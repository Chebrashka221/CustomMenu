package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.ItemData;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.GuiUtil;
import ru.blacksmith.v2.managers.DataManager;
import ru.blacksmith.v2.managers.IngotManager;

import java.util.ArrayList;
import java.util.List;

public class ShopMenu {

    private final BlacksmithPlugin plugin;

    public ShopMenu(BlacksmithPlugin plugin) { this.plugin = plugin; }

    // ---- Главное меню ----

    public void openMain(Player player, String shopId) {
        String shopName = plugin.getSetManager().getShopName(shopId);
        Inventory inv = Bukkit.createInventory(null, 54, GuiUtil.color(shopName));
        // Декорации (первыми, сеты их перекрывают)
        for (var e : plugin.getSetManager().getMainMenuDecorations(shopId).entrySet()) {
            int dSlot = e.getKey();
            if (dSlot < 0 || dSlot >= 54) continue;
            Material mat = Material.matchMaterial(e.getValue());
            if (mat != null) {
                var dItem = new ItemStack(mat);
                var dMeta = dItem.getItemMeta();
                if (dMeta != null) { dMeta.setDisplayName(" "); dItem.setItemMeta(dMeta); }
                inv.setItem(dSlot, dItem);
            }
        }
        for (SetData set : plugin.getSetManager().getSetsByShop(shopId)) {
            int slot = set.getMenuSlot();
            if (slot < 0 || slot >= 54) continue;
            inv.setItem(slot, buildSetIcon(player, set));
        }
        player.openInventory(inv);
    }

    // Показываем список магазинов игроку (когда их несколько и /cm без аргументов)
    public void openShopSelector(Player player) {
        java.util.List<String> shops = plugin.getSetManager().getShopIds();
        int size = Math.max(9, Math.min(54, ((shops.size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(null, size, "§8Выберите магазин");
        for (int i = 0; i < size; i++) inv.setItem(i, GuiUtil.filler());
        for (int i = 0; i < shops.size() && i < size; i++) {
            String shopId = shops.get(i);
            String shopName = plugin.getSetManager().getShopName(shopId);
            int setCount = plugin.getSetManager().getSetsByShop(shopId).size();
            inv.setItem(i, GuiUtil.makeItem(Material.CHEST,
                    GuiUtil.color(shopName),
                    "§7Предметов: §e" + setCount,
                    "", "§eНажмите чтобы открыть"));
        }
        player.openInventory(inv);
    }

    private ItemStack buildSetIcon(Player player, SetData set) {
        ItemStack icon = IngotManager.getIcon(set.getIconItemsAdderId());
        if (icon == null) icon = GuiUtil.tryMaterialOf(set.getIconMaterial(), Material.CHEST);

        int level  = plugin.getLevelManager().getLevel(player);
        boolean locked = level < set.getRequiredLevel();

        var meta = icon.getItemMeta();
        if (meta == null) return icon;
        meta.setDisplayName(GuiUtil.color(set.getName()) + (locked ? " §c[Заблокировано]" : ""));
        List<String> lore = new ArrayList<>();
        lore.add("§7Уровень: §e" + set.getRequiredLevel() + "+");
        lore.add("§7Ресурс: §f" + set.getIngotName());
        lore.add("§7Предметов: §f" + set.getItems().size());
        if (locked) {
            lore.add("");
            lore.add("§cВаш уровень: §e" + level);
        } else {
            lore.add("");
            lore.add("§eНажмите чтобы открыть");
        }
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    // ---- Меню предметов сета ----

    public void openSet(Player player, SetData set) {
        int size = Math.max(1, Math.min(6, set.getMenuRows())) * 9;
        Inventory inv = Bukkit.createInventory(null, size, GuiUtil.color(set.getMenuTitle()));
        // Декорации (перед предметами, предметы их перекрывают)
        for (var e : set.getDecorations().entrySet()) {
            int dSlot = e.getKey();
            if (dSlot < 0 || dSlot >= size - 1) continue;
            Material mat = Material.matchMaterial(e.getValue());
            if (mat != null) {
                var dItem = new ItemStack(mat);
                var dMeta = dItem.getItemMeta();
                if (dMeta != null) { dMeta.setDisplayName(" "); dItem.setItemMeta(dMeta); }
                inv.setItem(dSlot, dItem);
            }
        }
        for (ItemData item : set.getItems().values()) {
            int slot = item.getSlot();
            if (slot < 0 || slot >= size) continue;
            inv.setItem(slot, buildItemIcon(player, set, item));
        }
        inv.setItem(size - 1, GuiUtil.back());
        player.openInventory(inv);
    }

    /** Цена предмета с учётом прироста после покупок */
    public double getEffectivePrice(Player player, SetData set, ItemData item) {
        double base = item.getPrice();
        if (!item.hasPriceIncrease() || base <= 0) return base;
        int count;
        if ("global".equals(item.getPriceIncreaseMode())) {
            count = plugin.getDataManager().getGlobalBuyCount(set.getId(), item.getId(), item.getResetHours());
        } else {
            count = plugin.getDataManager().getBuyCount(
                    player.getUniqueId(), set.getId(), item.getId(), item.getResetHours());
        }
        if (count <= 0) return base;
        if ("fixed".equals(item.getPriceIncreaseType())) {
            return base + item.getPriceIncreaseFixed() * count;
        }
        return base * Math.pow(1.0 + item.getPriceIncreasePercent() / 100.0, count);
    }

    private ItemStack buildItemIcon(Player player, SetData set, ItemData item) {
        DataManager data = plugin.getDataManager();

        int buyCount    = data.getBuyCount(player.getUniqueId(), set.getId(), item.getId(), item.getResetHours());
        int maxBuys     = item.getMaxBuys();
        boolean canBuy  = data.canBuy(player.getUniqueId(), set.getId(), item);
        boolean finalBought = maxBuys > 0 && buyCount >= maxBuys;

        // Иконка: EI → ItemsAdder → ванильный материал → бумага
        ItemStack icon = null;
        if (item.hasExecutableItem()) {
            icon = plugin.getExecutableItemsManager().getIcon(item.getExecutableItemsId());
        }
        if (icon == null) icon = IngotManager.getIcon(item.getItemsAdderId());
        if (icon == null && item.hasVanillaItem()) {
            Material mat = Material.matchMaterial(item.getMaterial());
            if (mat != null) icon = new ItemStack(mat);
        }
        if (icon == null) icon = new ItemStack(Material.PAPER);

        var meta = icon.getItemMeta();
        if (meta == null) return icon;

        double effectivePrice = getEffectivePrice(player, set, item);

        boolean meetsLevel  = plugin.getLevelManager().getLevel(player) >= set.getRequiredLevel();
        boolean meetsMoney  = plugin.getEconomyManager().has(player, effectivePrice);
        boolean meetsIngots = IngotManager.count(player, set.getIngotItemsAdderId()) >= item.getIngots();
        boolean meetsPoints = plugin.getPlayerPointsManager().has(player, item.getPointsPrice());
        boolean meetsPrev   = true;
        String  prevName    = null;

        if (item.hasRequirement()) {
            String[] parts = item.getRequiresPrev().split("\\.", 2);
            if (parts.length == 2) {
                SetData ps = plugin.getSetManager().getSet(parts[0]);
                int prevResetHours = 0;
                if (ps != null) {
                    ItemData pi = ps.getItem(parts[1]);
                    if (pi != null) {
                        prevName = pi.getName();
                        prevResetHours = pi.getResetHours();
                    } else {
                        prevName = item.getRequiresPrev();
                    }
                }
                meetsPrev = data.hasBought(player.getUniqueId(), parts[0], parts[1], prevResetHours);
            }
        }

        // Название
        if (finalBought)
            meta.setDisplayName("§a✔ " + item.getName());
        else if (!meetsLevel || !meetsPrev)
            meta.setDisplayName("§c✖ " + item.getName());
        else
            meta.setDisplayName("§f" + item.getName());

        List<String> lore = new ArrayList<>();

        // Описание товара (если задано)
        if (!item.getDescription().isEmpty()) {
            for (String line : item.getDescription()) {
                lore.add(ru.blacksmith.v2.editor.GuiUtil.color(line));
            }
            lore.add("");
        } else {
            lore.add("");
        }

        // Статус покупок
        if (finalBought && maxBuys == 1) {
            lore.add("§a§lКУПЛЕНО");
        } else if (finalBought) {
            lore.add("§a§lКУПЛЕНО §8(§e" + buyCount + "§8/§e" + maxBuys + "§8)");
        } else if (maxBuys > 1 && buyCount > 0) {
            lore.add("§7Куплено: §e" + buyCount + "§7/§e" + maxBuys);
        } else if (maxBuys == 0 && buyCount > 0) {
            lore.add("§7Куплено: §e" + buyCount + " §7раз");
        }

        if (!finalBought) {
            if (item.getPrice() > 0) {
                String priceStr = "§7Цена: " + (meetsMoney ? "§a" : "§c") + (int) Math.ceil(effectivePrice) + "$";
                if (item.hasPriceIncrease()) {
                    String prefix = "global".equals(item.getPriceIncreaseMode()) ? "§8(глоб. +" : "§8(+";
                    String suffix = "fixed".equals(item.getPriceIncreaseType())
                            ? (int) item.getPriceIncreaseFixed() + "$/п.)"
                            : item.getPriceIncreasePercent() + "%/п.)";
                    priceStr += " " + prefix + suffix;
                }
                lore.add(priceStr);
            }
            if (item.getPointsPrice() > 0) {
                int havePoints = plugin.getPlayerPointsManager().getPoints(player);
                lore.add("§7Донат-поинты: " + (meetsPoints ? "§a" : "§c") + havePoints + "§7/§e" + item.getPointsPrice());
            }
            if (item.getIngots() > 0) {
                int have = IngotManager.count(player, set.getIngotItemsAdderId());
                lore.add("§7" + set.getIngotName() + ": " + (meetsIngots ? "§a" : "§c") + have + "§7/§e" + item.getIngots());
            }
            if (item.hasRequirement() && prevName != null)
                lore.add("§7Требует: " + (meetsPrev ? "§a" : "§c") + prevName);
            lore.add("");
            if (!meetsLevel)
                lore.add("§cТребуется уровень §e" + set.getRequiredLevel());
            else if (!meetsPrev)
                lore.add("§cСначала купите: §e" + (prevName != null ? prevName : "предыдущий предмет"));
            else if (!meetsMoney || !meetsPoints || !meetsIngots)
                lore.add("§cНедостаточно ресурсов");
            else
                lore.add("§eНажмите для покупки");
        }

        // Таймер сброса
        long resetMs = data.getResetMillisRemaining(player.getUniqueId(), set.getId(), item);
        if (resetMs > 0) {
            lore.add("§7Сброс через: §e" + formatTime(resetMs));
        }

        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    // ---- Покупка ----

    public void tryBuy(Player player, SetData set, ItemData item) {
        DataManager data = plugin.getDataManager();

        if (!data.canBuy(player.getUniqueId(), set.getId(), item)) {
            int maxBuys = item.getMaxBuys();
            long resetMs = data.getResetMillisRemaining(player.getUniqueId(), set.getId(), item);
            if (resetMs > 0)
                player.sendMessage("§eЛимит покупок достигнут. Сброс через §f" + formatTime(resetMs));
            else
                player.sendMessage("§eВы уже купили §f" + item.getName() +
                        (maxBuys > 1 ? " §e(" + maxBuys + " раз)" : ""));
            return;
        }
        if (plugin.getLevelManager().getLevel(player) < set.getRequiredLevel()) {
            player.sendMessage("§cТребуется уровень §e" + set.getRequiredLevel());
            return;
        }
        if (item.hasRequirement()) {
            String[] parts = item.getRequiresPrev().split("\\.", 2);
            if (parts.length == 2) {
                SetData ps = plugin.getSetManager().getSet(parts[0]);
                String prevName = item.getRequiresPrev();
                int prevResetHours = 0;
                if (ps != null) {
                    ItemData pi = ps.getItem(parts[1]);
                    if (pi != null) { prevName = pi.getName(); prevResetHours = pi.getResetHours(); }
                }
                if (!data.hasBought(player.getUniqueId(), parts[0], parts[1], prevResetHours)) {
                    player.sendMessage("§cСначала купите: §e" + prevName);
                    return;
                }
            }
        }
        double effectivePrice = getEffectivePrice(player, set, item);

        if (effectivePrice > 0 && !plugin.getEconomyManager().has(player, effectivePrice)) {
            player.sendMessage("§cНедостаточно денег! Нужно §e" + (int) Math.ceil(effectivePrice) + "$");
            return;
        }
        if (item.getPointsPrice() > 0 && !plugin.getPlayerPointsManager().has(player, item.getPointsPrice())) {
            player.sendMessage("§cНедостаточно донат-поинтов! Нужно §e" + item.getPointsPrice());
            return;
        }
        if (item.getIngots() > 0 && IngotManager.count(player, set.getIngotItemsAdderId()) < item.getIngots()) {
            player.sendMessage("§cНедостаточно §e" + set.getIngotName() + "§c! Нужно §e" + item.getIngots() + " шт.");
            return;
        }

        if (effectivePrice > 0) plugin.getEconomyManager().take(player, effectivePrice);
        if (item.getPointsPrice() > 0) plugin.getPlayerPointsManager().take(player, item.getPointsPrice());
        if (item.getIngots() > 0) IngotManager.remove(player, set.getIngotItemsAdderId(), item.getIngots());

        // Выдача предмета: ExecutableItems → ItemsAdder → ванильный материал
        if (item.hasExecutableItem()) {
            boolean given = plugin.getExecutableItemsManager().give(player, item.getExecutableItemsId(), item.getAmount());
            if (!given) {
                player.sendMessage("§c[Ошибка] Предмет §e" + item.getExecutableItemsId()
                        + " §cне найден в ExecutableItems!");
                plugin.getLogger().warning("EI предмет '" + item.getExecutableItemsId()
                        + "' не найден для " + player.getName());
            }
        } else if (item.getItemsAdderId() != null && !item.getItemsAdderId().isEmpty()) {
            IngotManager.give(player, item.getItemsAdderId());
        } else if (item.hasVanillaItem()) {
            org.bukkit.Material mat = org.bukkit.Material.matchMaterial(item.getMaterial());
            if (mat != null) player.getInventory().addItem(new org.bukkit.inventory.ItemStack(mat, item.getAmount()));
        }

        // Команды при покупке
        for (String cmd : item.getCommands()) {
            String resolved = cmd.replace("{player}", player.getName());
            plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), resolved);
        }

        data.recordBuy(player.getUniqueId(), set.getId(), item.getId());

        // Увеличиваем глобальный счётчик, если включено глобальное повышение цены
        if (item.hasPriceIncrease() && "global".equals(item.getPriceIncreaseMode())) {
            data.incrementGlobalBuyCount(set.getId(), item.getId());
        }

        int newCount = data.getBuyCount(player.getUniqueId(), set.getId(), item.getId(), item.getResetHours());
        int maxBuys = item.getMaxBuys();
        String countInfo = maxBuys > 0 ? " §8(" + newCount + "/" + maxBuys + ")" : "";
        player.sendMessage("§aВы купили §e" + item.getName() + "§a!" + countInfo);

        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getMenuListener().openShopSet(player, set));
    }

    // ---- Util ----

    private static String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (hours > 0) return hours + "ч " + minutes + "м";
        return Math.max(1, minutes) + "м";
    }
}
