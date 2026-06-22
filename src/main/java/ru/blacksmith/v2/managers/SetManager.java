package ru.blacksmith.v2.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.SetData;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Каждый сет хранится в отдельном файле: plugins/BlacksmithV2/sets/<id>.yml
 */
public class SetManager {

    private final BlacksmithPlugin plugin;
    private File setsDir;
    private File shopsFile;
    private final Map<String, SetData> sets = new LinkedHashMap<>();
    private final Map<String, String> shopNames = new LinkedHashMap<>(); // shopId → display name
    private final Map<String, Map<Integer, String>> shopDecorations = new LinkedHashMap<>(); // shopId → slot → material

    public SetManager(BlacksmithPlugin plugin) { this.plugin = plugin; }

    public void load() {
        sets.clear();
        shopNames.clear();
        setsDir = new File(plugin.getDataFolder(), "sets");
        if (!setsDir.exists()) setsDir.mkdirs();

        shopDecorations.clear();
        shopsFile = new File(plugin.getDataFolder(), "shops.yml");
        if (shopsFile.exists()) {
            YamlConfiguration sc = YamlConfiguration.loadConfiguration(shopsFile);
            var sec = sc.getConfigurationSection("shops");
            if (sec != null) {
                for (String id : sec.getKeys(false)) {
                    shopNames.put(id, sc.getString("shops." + id + ".name", id));
                    var decorSec = sc.getConfigurationSection("shops." + id + ".decorations");
                    if (decorSec != null) {
                        Map<Integer, String> decors = new LinkedHashMap<>();
                        for (String key : decorSec.getKeys(false)) {
                            try {
                                String mat = decorSec.getString(key);
                                if (mat != null) decors.put(Integer.parseInt(key), mat);
                            } catch (NumberFormatException ignored) {}
                        }
                        shopDecorations.put(id, decors);
                    }
                }
            }
        }

        File[] files = setsDir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        // Sort by filename for consistent order
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File f : files) {
            String id = f.getName().replace(".yml", "");
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(f);
            SetData set = new SetData(id);
            set.loadFrom(cfg, "set");
            sets.put(id, set);
        }
        plugin.getLogger().info("Загружено сетов: " + sets.size());
    }

    public void saveSet(SetData set) {
        File f = new File(setsDir, set.getId() + ".yml");
        YamlConfiguration cfg = new YamlConfiguration();
        set.saveTo(cfg, "set");
        try { cfg.save(f); } catch (IOException e) { e.printStackTrace(); }
    }

    public void deleteSet(String id) {
        sets.remove(id);
        File f = new File(setsDir, id + ".yml");
        if (f.exists()) f.delete();
    }

    public SetData createSet(String id) {
        return createSet(id, "default");
    }

    public SetData createSet(String id, String shopId) {
        SetData set = new SetData(id);
        set.setShopId(shopId);
        sets.put(id, set);
        saveSet(set);
        return set;
    }

    // ---- Shop methods ----

    public List<String> getShopIds() {
        Set<String> ids = new LinkedHashSet<>(shopNames.keySet());
        sets.values().stream().map(SetData::getShopId).forEach(ids::add);
        return ids.stream().sorted().collect(Collectors.toList());
    }

    public List<SetData> getSetsByShop(String shopId) {
        return sets.values().stream()
                .filter(s -> s.getShopId().equals(shopId))
                .collect(Collectors.toList());
    }

    public String getShopName(String shopId) {
        return shopNames.getOrDefault(shopId, shopId);
    }

    public void setShopName(String shopId, String name) {
        shopNames.put(shopId, name);
        saveShops();
    }

    public void deleteShop(String shopId) {
        shopNames.remove(shopId);
        saveShops();
    }

    public void saveShops() {
        YamlConfiguration sc = new YamlConfiguration();
        Set<String> allIds = new LinkedHashSet<>(shopNames.keySet());
        sets.values().stream().map(SetData::getShopId).forEach(allIds::add);
        for (String id : allIds) {
            sc.set("shops." + id + ".name", shopNames.getOrDefault(id, id));
            Map<Integer, String> decors = shopDecorations.get(id);
            if (decors != null && !decors.isEmpty()) {
                for (var e : decors.entrySet())
                    sc.set("shops." + id + ".decorations." + e.getKey(), e.getValue());
            }
        }
        try { sc.save(shopsFile); } catch (IOException e) { e.printStackTrace(); }
    }

    // ---- Main menu decorations ----

    public Map<Integer, String> getMainMenuDecorations(String shopId) {
        return shopDecorations.computeIfAbsent(shopId, k -> new LinkedHashMap<>());
    }

    public void setMainMenuDecoration(String shopId, int slot, String material) {
        getMainMenuDecorations(shopId).put(slot, material);
        saveShops();
    }

    public void removeMainMenuDecoration(String shopId, int slot) {
        getMainMenuDecorations(shopId).remove(slot);
        saveShops();
    }

    public Map<String, SetData> getSets() { return sets; }
    public SetData getSet(String id) { return sets.get(id); }
    public Collection<SetData> getAllSets() { return sets.values(); }
}
