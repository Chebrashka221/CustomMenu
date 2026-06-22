package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.ChatInput;
import ru.blacksmith.v2.editor.GuiUtil;
import ru.blacksmith.v2.managers.IngotManager;

import java.util.ArrayList;
import java.util.List;

public class SetListMenu {

    public static final String TITLE_PREFIX = "§8⚒ §lСеты: ";

    private final BlacksmithPlugin plugin;
    private final String shopId;

    public SetListMenu(BlacksmithPlugin plugin, String shopId) {
        this.plugin = plugin;
        this.shopId = shopId;
    }

    public void open(Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        List<SetData> sets = plugin.getSetManager().getSetsByShop(shopId);
        String shopName = plugin.getSetManager().getShopName(shopId);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + shopName);

        for (int i = 0; i < 54; i++) inv.setItem(i, GuiUtil.filler());

        inv.setItem(49, GuiUtil.makeItem(Material.LIME_DYE, "&a+ Создать новый сет",
                "&7Нажмите чтобы создать новый сет"));

        inv.setItem(45, GuiUtil.back());

        for (int i = 0; i < sets.size() && i < 45; i++) {
            inv.setItem(i, buildSetIcon(sets.get(i)));
        }

        return inv;
    }

    private ItemStack buildSetIcon(SetData set) {
        ItemStack icon = IngotManager.getIcon(set.getIconItemsAdderId());
        if (icon == null) icon = GuiUtil.tryMaterialOf(set.getIconMaterial(), Material.CHEST);

        var meta = icon.getItemMeta();
        if (meta == null) return icon;

        meta.setDisplayName(GuiUtil.color(set.getName()));
        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + set.getId());
        lore.add("§7Уровень: §e" + set.getRequiredLevel() + "+");
        lore.add("§7Предметов: §f" + set.getItems().size());
        lore.add("§7Слиток: §f" + set.getIngotName());
        lore.add("");
        lore.add("§eЛКМ §7— открыть настройки");
        lore.add("§cПКМ §7— удалить сет");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    public String getShopId() { return shopId; }

    public SetData getSetAtSlot(int slot) {
        List<SetData> sets = plugin.getSetManager().getSetsByShop(shopId);
        return (slot >= 0 && slot < Math.min(sets.size(), 45)) ? sets.get(slot) : null;
    }
}
