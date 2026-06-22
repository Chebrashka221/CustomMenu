package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.GuiUtil;
import ru.blacksmith.v2.managers.IngotManager;

import java.util.List;

public class EditorMenusListMenu {

    private final BlacksmithPlugin plugin;
    private final String shopId;

    public EditorMenusListMenu(BlacksmithPlugin plugin, String shopId) {
        this.plugin = plugin;
        this.shopId = shopId;
    }

    public void open(org.bukkit.entity.Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        List<SetData> sets = plugin.getSetManager().getSetsByShop(shopId);
        String shopName = plugin.getSetManager().getShopName(shopId);

        Inventory inv = Bukkit.createInventory(null, 27, "§8Редактор меню: " + shopName);
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        inv.setItem(0, GuiUtil.makeItem(Material.CHEST, "&fГлавное меню магазина",
                "&7Настроить расположение сетов",
                "&7в главном 54-слотовом меню",
                "", "&eНажмите чтобы открыть"));

        inv.setItem(8, GuiUtil.back());

        for (int i = 0; i < sets.size() && i < 25; i++) {
            SetData set = sets.get(i);
            int uiSlot = (i < 7) ? (i + 1) : (i + 2);

            ItemStack icon = IngotManager.getIcon(set.getIconItemsAdderId());
            if (icon == null) icon = GuiUtil.tryMaterialOf(set.getIconMaterial(), Material.CHEST);
            var meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(GuiUtil.color(set.getName()));
                meta.setLore(java.util.Arrays.asList(
                        "§7ID: §f" + set.getId(),
                        "§7Предметов: §f" + set.getItems().size(),
                        "§7Строк: §e" + set.getMenuRows(),
                        "",
                        "§eНажмите §7— редактор раскладки"));
                icon.setItemMeta(meta);
            }
            inv.setItem(uiSlot, icon);
        }

        return inv;
    }

    public String getShopId() { return shopId; }

    public SetData getSetAtSlot(int uiSlot) {
        if (uiSlot == 0 || uiSlot == 8) return null;
        List<SetData> sets = plugin.getSetManager().getSetsByShop(shopId);
        int setIdx = (uiSlot <= 7) ? (uiSlot - 1) : (uiSlot - 2);
        if (setIdx < 0 || setIdx >= sets.size()) return null;
        return sets.get(setIdx);
    }
}
