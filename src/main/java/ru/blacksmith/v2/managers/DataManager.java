package ru.blacksmith.v2.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.ItemData;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataManager {

    private final BlacksmithPlugin plugin;
    private File file;
    private YamlConfiguration cfg;

    // uuid → "setId.itemId" → [count, firstBuyTimestampMs]
    private final Map<UUID, Map<String, long[]>> cache = new HashMap<>();

    // Глобальный счётчик: "setId.itemId" → [totalCount, firstBuyTimestampMs]
    private final Map<String, long[]> globalCache = new HashMap<>();

    public DataManager(BlacksmithPlugin plugin) { this.plugin = plugin; }

    public void load() {
        file = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!file.exists()) { try { file.createNewFile(); } catch (IOException e) { e.printStackTrace(); } }
        cfg = YamlConfiguration.loadConfiguration(file);
        cache.clear();
        globalCache.clear();

        // Загрузка глобальных счётчиков
        var globalSec = cfg.getConfigurationSection("global");
        if (globalSec != null) {
            for (String key : globalSec.getKeys(false)) {
                var s = globalSec.getConfigurationSection(key);
                if (s != null) {
                    long count = s.getLong("count", 0);
                    long time  = s.getLong("time", 0);
                    if (count > 0) globalCache.put(key, new long[]{count, time});
                }
            }
        }

        var playersSec = cfg.getConfigurationSection("players");
        if (playersSec == null) return;

        for (String uuidStr : playersSec.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidStr);
                Map<String, long[]> buys = new HashMap<>();
                var playerSec = playersSec.getConfigurationSection(uuidStr);
                if (playerSec == null) continue;

                for (String setId : playerSec.getKeys(false)) {
                    var setSec = playerSec.getConfigurationSection(setId);
                    if (setSec == null) continue;
                    for (String itemId : setSec.getKeys(false)) {
                        String key = setId + "." + itemId;
                        if (setSec.isBoolean(itemId)) {
                            // Миграция со старого формата (boolean true → count 1)
                            if (setSec.getBoolean(itemId)) buys.put(key, new long[]{1, 0});
                        } else if (setSec.isConfigurationSection(itemId)) {
                            var itemSec = setSec.getConfigurationSection(itemId);
                            long count = itemSec.getLong("count", 0);
                            long time  = itemSec.getLong("time", 0);
                            if (count > 0) buys.put(key, new long[]{count, time});
                        }
                    }
                }
                if (!buys.isEmpty()) cache.put(uuid, buys);
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        cfg.set("global", null);
        for (var e : globalCache.entrySet()) {
            long[] d = e.getValue();
            if (d[0] <= 0) continue;
            String base = "global." + e.getKey();
            cfg.set(base + ".count", d[0]);
            cfg.set(base + ".time", d[1]);
        }
        cfg.set("players", null);
        for (var e : cache.entrySet()) {
            for (var se : e.getValue().entrySet()) {
                long[] data = se.getValue();
                if (data[0] <= 0) continue;
                String[] parts = se.getKey().split("\\.", 2);
                if (parts.length != 2) continue;
                String base = "players." + e.getKey() + "." + parts[0] + "." + parts[1];
                cfg.set(base + ".count", data[0]);
                cfg.set(base + ".time", data[1]);
            }
        }
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    // ---- Buy checks ----

    /**
     * Количество покупок в текущем цикле (с учётом сброса таймера).
     */
    public int getBuyCount(UUID uuid, String setId, String itemId, int resetHours) {
        return effectiveCount(uuid, setId + "." + itemId, resetHours);
    }

    /**
     * True если игрок может купить предмет прямо сейчас
     * (учитывает maxBuys и resetHours из ItemData).
     */
    public boolean canBuy(UUID uuid, String setId, ItemData item) {
        if (item.getMaxBuys() == 0) return true;
        return effectiveCount(uuid, setId + "." + item.getId(), item.getResetHours()) < item.getMaxBuys();
    }

    /**
     * Возвращает true если предмет был куплен хотя бы 1 раз (для проверки зависимостей).
     * Учитывает resetHours переданного предмета — если таймер сброса истёк, считается не купленным.
     * Перегрузка без resetHours — никогда не сбрасывается (зависимости обычно постоянны).
     */
    public boolean hasBought(UUID uuid, String setId, String itemId) {
        Map<String, long[]> playerBuys = cache.get(uuid);
        if (playerBuys == null) return false;
        long[] data = playerBuys.get(setId + "." + itemId);
        return data != null && data[0] > 0;
    }

    /** Перегрузка с учётом сброса — используется если зависимый предмет имеет resetHours. */
    public boolean hasBought(UUID uuid, String setId, String itemId, int resetHours) {
        if (resetHours <= 0) return hasBought(uuid, setId, itemId);
        return effectiveCount(uuid, setId + "." + itemId, resetHours) > 0;
    }

    /**
     * Сколько миллисекунд осталось до сброса. -1 если сброс не настроен или
     * счётчик ещё не начался.
     */
    public long getResetMillisRemaining(UUID uuid, String setId, ItemData item) {
        if (item.getResetHours() <= 0) return -1;
        Map<String, long[]> playerBuys = cache.get(uuid);
        if (playerBuys == null) return -1;
        long[] data = playerBuys.get(setId + "." + item.getId());
        if (data == null || data[0] == 0 || data[1] == 0) return -1;
        long resetAt  = data[1] + (long) item.getResetHours() * 3_600_000L;
        long remaining = resetAt - System.currentTimeMillis();
        return remaining > 0 ? remaining : -1;
    }

    // ---- Recording ----

    /**
     * Зафиксировать одну покупку.
     */
    public void recordBuy(UUID uuid, String setId, String itemId) {
        String key = setId + "." + itemId;
        Map<String, long[]> playerBuys = cache.computeIfAbsent(uuid, k -> new HashMap<>());
        long[] data = playerBuys.computeIfAbsent(key, k -> new long[]{0, 0});
        if (data[0] == 0) data[1] = System.currentTimeMillis();
        data[0]++;
        persistEntry(uuid, setId, itemId, data);
    }

    /** Для команды /cmadmin give */
    public void setBought(UUID uuid, String setId, String itemId) {
        recordBuy(uuid, setId, itemId);
    }

    // ---- Resets ----

    public void resetPlayer(UUID uuid) {
        cache.remove(uuid);
        cfg.set("players." + uuid, null);
        safeSave();
    }

    public void resetPlayerSet(UUID uuid, String setId) {
        if (cache.containsKey(uuid))
            cache.get(uuid).entrySet().removeIf(e -> e.getKey().startsWith(setId + "."));
        cfg.set("players." + uuid + "." + setId, null);
        safeSave();
    }

    public void resetPlayerItem(UUID uuid, String setId, String itemId) {
        if (cache.containsKey(uuid)) cache.get(uuid).remove(setId + "." + itemId);
        cfg.set("players." + uuid + "." + setId + "." + itemId, null);
        safeSave();
    }

    // ---- Global price-increase counters ----

    /**
     * Сколько раз предмет куплен всеми игроками (для global режима повышения цены).
     * Учитывает resetHours — если сброс прошёл, вернёт 0.
     */
    public int getGlobalBuyCount(String setId, String itemId, int resetHours) {
        String key = setId + "." + itemId;
        long[] d = globalCache.get(key);
        if (d == null || d[0] == 0) return 0;
        if (resetHours > 0 && d[1] > 0) {
            long elapsed = System.currentTimeMillis() - d[1];
            if (elapsed >= (long) resetHours * 3_600_000L) {
                globalCache.remove(key);
                return 0;
            }
        }
        return (int) d[0];
    }

    /** Увеличить глобальный счётчик покупок (вызывается из ShopMenu при global-режиме). */
    public void incrementGlobalBuyCount(String setId, String itemId) {
        String key = setId + "." + itemId;
        long[] d = globalCache.computeIfAbsent(key, k -> new long[]{0, 0});
        if (d[0] == 0) d[1] = System.currentTimeMillis();
        d[0]++;
        cfg.set("global." + key + ".count", d[0]);
        cfg.set("global." + key + ".time", d[1]);
        safeSave();
    }

    // ---- Internal ----

    private int effectiveCount(UUID uuid, String key, int resetHours) {
        Map<String, long[]> playerBuys = cache.get(uuid);
        if (playerBuys == null) return 0;
        long[] data = playerBuys.get(key);
        if (data == null || data[0] == 0) return 0;
        if (resetHours > 0 && data[1] > 0) {
            long elapsed = System.currentTimeMillis() - data[1];
            if (elapsed >= (long) resetHours * 3_600_000L) {
                data[0] = 0;
                data[1] = 0;
                return 0;
            }
        }
        return (int) data[0];
    }

    private void persistEntry(UUID uuid, String setId, String itemId, long[] data) {
        String base = "players." + uuid + "." + setId + "." + itemId;
        cfg.set(base + ".count", data[0]);
        cfg.set(base + ".time", data[1]);
        safeSave();
    }

    private void safeSave() {
        try { cfg.save(file); } catch (IOException e) { e.printStackTrace(); }
    }
}
