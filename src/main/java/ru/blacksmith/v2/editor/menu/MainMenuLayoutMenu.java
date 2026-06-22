package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.GuiUtil;
import ru.blacksmith.v2.managers.IngotManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainMenuLayoutMenu {

    private final BlacksmithPlugin plugin;
    private final String shopId;

    public MainMenuLayoutMenu(BlacksmithPlugin plugin, String shopId) {
        this.plugin = plugin;
        this.shopId = shopId;
    }

    public void open(Player player) {
        player.openInventory(buildInventory());
    }

    public Inventory buildInventory() {
        String shopName = plugin.getSetManager().getShopName(shopId);
        Inventory inv = Bukkit.createInventory(null, 54,
                "§8[" + shopName + "] Редактор раскладки");

        ItemStack empty = GuiUtil.makeItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§eПустой слот", "§7ЛКМ §8— назначить сет", "§7Shift+ЛКМ §8— декор");
        for (int i = 0; i < 54; i++) inv.setItem(i, empty);
        inv.setItem(53, GuiUtil.makeItem(Material.BARRIER, "§c← Назад к списку меню"));

        // Декорации главного меню
        for (var e : plugin.getSetManager().getMainMenuDecorations(shopId).entrySet()) {
            int dSlot = e.getKey();
            if (dSlot < 0 || dSlot >= 53) continue;
            Material mat = Material.matchMaterial(e.getValue());
            if (mat == null) continue;
            var dItem = new ItemStack(mat);
            var dMeta = dItem.getItemMeta();
            if (dMeta != null) {
                dMeta.setDisplayName("§7Декор §8(" + e.getValue() + "§8)");
                dMeta.setLore(java.util.Arrays.asList("", "§eЛКМ §7— изменить цвет", "§cПКМ §7— убрать"));
                dItem.setItemMeta(dMeta);
            }
            inv.setItem(dSlot, dItem);
        }

        // Сеты (перекрывают декор)
        for (SetData set : plugin.getSetManager().getSetsByShop(shopId)) {
            int slot = set.getMenuSlot();
            if (slot < 0 || slot >= 53) continue;
            inv.setItem(slot, buildSetIcon(set));
        }

        return inv;
    }

    private ItemStack buildSetIcon(SetData set) {
        ItemStack icon = IngotManager.getIcon(set.getIconItemsAdderId());
        if (icon == null) icon = GuiUtil.tryMaterialOf(set.getIconMaterial(), Material.CHEST);
        var meta = icon.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(GuiUtil.color(set.getName()));
            meta.setLore(java.util.Arrays.asList(
                    "§7ID: §f" + set.getId(),
                    "§7Предметов: §f" + set.getItems().size(),
                    "",
                    "§eЛКМ §7— настройки сета",
                    "§cПКМ §7— убрать из этого слота"));
            icon.setItemMeta(meta);
        }
        return icon;
    }

    public String getShopId() { return shopId; }

    public void handleClick(Player player, int slot, boolean right, boolean shift) {
        if (slot == 53) {
            plugin.getMenuListener().openEditorMenusList(player, shopId);
            return;
        }

        // Проверяем: сет на этом слоте?
        SetData existing = null;
        for (SetData set : plugin.getSetManager().getSetsByShop(shopId)) {
            if (set.getMenuSlot() == slot) { existing = set; break; }
        }

        if (existing != null) {
            if (right) {
                existing.setMenuSlot(-1);
                plugin.getSetManager().saveSet(existing);
                player.sendMessage(GuiUtil.color("§eSет §f" + existing.getName() + " §eубран из слота."));
                plugin.getMenuListener().openMainMenuLayout(player, shopId);
            } else {
                plugin.getMenuListener().openEditorSet(player, existing);
            }
            return;
        }

        // Проверяем: декор на этом слоте?
        Map<Integer, String> decors = plugin.getSetManager().getMainMenuDecorations(shopId);
        if (decors.containsKey(slot)) {
            if (right) {
                plugin.getSetManager().removeMainMenuDecoration(shopId, slot);
                player.sendMessage(GuiUtil.color("§eДекор убран со слота §f" + slot + "§e."));
                plugin.getMenuListener().openMainMenuLayout(player, shopId);
            } else {
                plugin.getMenuListener().openMainMenuDecorPicker(player, shopId, slot);
            }
            return;
        }

        // Пустой слот: Shift+ЛКМ → декор, ЛКМ → назначить сет
        if (shift) {
            plugin.getMenuListener().openMainMenuDecorPicker(player, shopId, slot);
        } else {
            plugin.getMenuListener().openMainMenuSlotPick(player, shopId, slot);
        }
    }

    public Inventory buildSetPickerInventory() {
        List<SetData> sets = plugin.getSetManager().getSetsByShop(shopId);
        int size = Math.max(9, Math.min(54, ((sets.size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(null, size, "§8Выберите сет для слота");
        for (int i = 0; i < size; i++) inv.setItem(i, GuiUtil.filler());

        for (int i = 0; i < sets.size() && i < size; i++) {
            SetData set = sets.get(i);
            ItemStack icon = IngotManager.getIcon(set.getIconItemsAdderId());
            if (icon == null) icon = GuiUtil.tryMaterialOf(set.getIconMaterial(), Material.CHEST);
            var meta = icon.getItemMeta();
            if (meta != null) {
                int curSlot = set.getMenuSlot();
                String slotInfo = (curSlot >= 0 && curSlot < 53) ? "§7Текущий слот: §e" + curSlot : "§7Не назначен";
                meta.setDisplayName(GuiUtil.color(set.getName()));
                meta.setLore(java.util.Arrays.asList(
                        "§7ID: §f" + set.getId(),
                        slotInfo,
                        "",
                        "§eНажмите §7— назначить на выбранный слот"));
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
        }

        return inv;
    }

    public SetData getSetAtPickerSlot(int slot) {
        List<SetData> sets = new ArrayList<>(plugin.getSetManager().getSetsByShop(shopId));
        return (slot >= 0 && slot < sets.size()) ? sets.get(slot) : null;
    }
}
