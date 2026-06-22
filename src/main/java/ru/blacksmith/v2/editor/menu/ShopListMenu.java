package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.editor.GuiUtil;

import java.util.List;

/**
 * Список магазинов в редакторе.
 * Слоты 0-44: магазины
 * Слот 49: + Создать магазин
 */
public class ShopListMenu {

    public static final String TITLE = "§8⚒ §lМагазины";

    private final BlacksmithPlugin plugin;

    public ShopListMenu(BlacksmithPlugin plugin) { this.plugin = plugin; }

    public void open(Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        List<String> shopIds = plugin.getSetManager().getShopIds();
        Inventory inv = Bukkit.createInventory(null, 54, TITLE);
        for (int i = 0; i < 54; i++) inv.setItem(i, GuiUtil.filler());

        for (int i = 0; i < shopIds.size() && i < 45; i++) {
            inv.setItem(i, buildShopIcon(shopIds.get(i)));
        }

        inv.setItem(49, GuiUtil.makeItem(Material.LIME_DYE, "&a+ Создать магазин",
                "&7Создать новый независимый магазин"));
        return inv;
    }

    private ItemStack buildShopIcon(String shopId) {
        String name = plugin.getSetManager().getShopName(shopId);
        int setCount = plugin.getSetManager().getSetsByShop(shopId).size();
        return GuiUtil.makeItem(Material.CHEST,
                "&f" + name,
                "&7ID: &e" + shopId,
                "&7Сетов: &e" + setCount,
                "",
                "&eЛКМ &7— открыть редактор",
                "&cПКМ &7— удалить магазин (если пуст)");
    }

    public String getShopAtSlot(int slot) {
        List<String> shopIds = plugin.getSetManager().getShopIds();
        return (slot >= 0 && slot < Math.min(shopIds.size(), 45)) ? shopIds.get(slot) : null;
    }
}
