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
import ru.blacksmith.v2.managers.IngotManager;

import java.util.ArrayList;
import java.util.List;

public class SetLayoutMenu {

    private final BlacksmithPlugin plugin;
    private final SetData set;

    public SetLayoutMenu(BlacksmithPlugin plugin, SetData set) {
        this.plugin = plugin;
        this.set = set;
    }

    public void open(Player player) {
        player.openInventory(buildInventory());
    }

    private Inventory buildInventory() {
        int rows = Math.max(1, Math.min(6, set.getMenuRows()));
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(null, size,
                GuiUtil.color("§8[Редактор] §r") + GuiUtil.color(set.getMenuTitle()));

        ItemStack emptySlot = GuiUtil.makeItem(Material.LIGHT_BLUE_STAINED_GLASS_PANE,
                "§eПустой слот", "§7Нажмите чтобы назначить предмет");
        for (int i = 0; i < size; i++) inv.setItem(i, emptySlot);

        inv.setItem(size - 1, GuiUtil.makeItem(Material.BARRIER,
                "§c← Назад к настройкам", "§7(этот слот зарезервирован)"));

        // Декорации
        for (var e : set.getDecorations().entrySet()) {
            int dSlot = e.getKey();
            if (dSlot < 0 || dSlot >= size - 1) continue;
            Material mat = Material.matchMaterial(e.getValue());
            if (mat == null) continue;
            var dItem = new ItemStack(mat);
            var dMeta = dItem.getItemMeta();
            if (dMeta != null) {
                dMeta.setDisplayName("§7Декор §8(" + e.getValue() + "§8)");
                dMeta.setLore(java.util.Arrays.asList(
                        "", "§eЛКМ §7— изменить цвет", "§cПКМ §7— убрать"));
                dItem.setItemMeta(dMeta);
            }
            inv.setItem(dSlot, dItem);
        }

        // Предметы (перекрывают декор на том же слоте)
        for (ItemData item : set.getItems().values()) {
            int slot = item.getSlot();
            if (slot < 0 || slot >= size || slot == size - 1) continue;
            inv.setItem(slot, buildItemIcon(item));
        }

        return inv;
    }

    private ItemStack buildItemIcon(ItemData item) {
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
        meta.setDisplayName("§f" + item.getName());
        List<String> lore = new ArrayList<>();
        lore.add("§7ID: §f" + item.getId());
        lore.add("§7Цена: §e" + (int) item.getPrice() + "$");
        lore.add("§7Слитков: §e" + item.getIngots());
        lore.add("");
        lore.add("§eЛКМ §7— настройки предмета");
        lore.add("§cПКМ §7— убрать с этого слота");
        meta.setLore(lore);
        icon.setItemMeta(meta);
        return icon;
    }

    public void handleClick(Player player, int slot, boolean right) {
        int size = Math.max(1, Math.min(6, set.getMenuRows())) * 9;

        if (slot == size - 1) {
            plugin.getMenuListener().openEditorSet(player, set);
            return;
        }

        ItemData existing = null;
        for (ItemData item : set.getItems().values()) {
            if (item.getSlot() == slot) { existing = item; break; }
        }

        if (existing != null) {
            if (right) {
                existing.setSlot(-1);
                plugin.getSetManager().saveSet(set);
                player.sendMessage(GuiUtil.color("§eПредмет §f" + existing.getName() + " §eубран со слота."));
                plugin.getMenuListener().reopenCurrentLayoutEditor(player, set);
            } else {
                plugin.getMenuListener().openEditorItem(player, set, existing);
            }
        } else if (set.getDecorations().containsKey(slot)) {
            // Декор: ЛКМ — сменить цвет, ПКМ — убрать
            if (right) {
                set.removeDecoration(slot);
                plugin.getSetManager().saveSet(set);
                player.sendMessage(GuiUtil.color("§eДекор убран со слота §f" + slot + "§e."));
                plugin.getMenuListener().reopenCurrentLayoutEditor(player, set);
            } else {
                plugin.getMenuListener().openDecorPicker(player, set, slot);
            }
        } else {
            plugin.getMenuListener().openSlotMenu(player, set, slot);
        }
    }

    /** Инвентарь "что назначить на слот?" — открывается через MenuListener.openSlotMenu */
    public Inventory buildSlotMenuInventory(int targetSlot) {
        int layoutSize = Math.max(1, Math.min(6, set.getMenuRows())) * 9;
        boolean hasUnassigned = set.getItems().values().stream()
                .anyMatch(i -> i.getSlot() < 0 || i.getSlot() >= layoutSize);

        Inventory inv = Bukkit.createInventory(null, 27, "§8Слот " + targetSlot + " — что назначить?");
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        inv.setItem(10, GuiUtil.makeItem(Material.LIME_DYE, "§a+ Создать новый предмет",
                "§7Создать новый предмет и",
                "§7назначить его на слот §e" + targetSlot));

        if (hasUnassigned) {
            inv.setItem(12, GuiUtil.makeItem(Material.YELLOW_DYE, "§eНазначить существующий",
                    "§7Выбрать предмет из списка"));
        }

        inv.setItem(14, GuiUtil.makeItem(Material.BLUE_STAINED_GLASS_PANE, "§9Поставить декор",
                "§7Разместить стеклянную панель",
                "§7как украшение меню"));

        inv.setItem(16, GuiUtil.makeItem(Material.ARROW, "§7← Назад"));

        return inv;
    }

    /** Инвентарь выбора существующего неназначенного предмета — открывается через MenuListener.openSlotAssign */
    public Inventory buildAssignExistingInventory() {
        List<ItemData> items = getUnassignedItems();
        int size2 = Math.max(9, Math.min(54, ((items.size() / 9) + 1) * 9));
        Inventory inv = Bukkit.createInventory(null, size2, "§8Выберите предмет");
        for (int i = 0; i < size2; i++) inv.setItem(i, GuiUtil.filler());

        for (int i = 0; i < items.size() && i < size2; i++) {
            ItemData item = items.get(i);
            ItemStack icon = IngotManager.getIcon(item.getItemsAdderId());
            if (icon == null) icon = new ItemStack(Material.PAPER);
            var meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + item.getName());
                meta.setLore(List.of("§7ID: §f" + item.getId(), "", "§eНажмите для назначения"));
                icon.setItemMeta(meta);
            }
            inv.setItem(i, icon);
        }

        return inv;
    }

    /** Предметы без назначенного слота — используется MenuListener для обработки клика */
    public List<ItemData> getUnassignedItems() {
        int layoutSize = Math.max(1, Math.min(6, set.getMenuRows())) * 9;
        return set.getItems().values().stream()
                .filter(i -> i.getSlot() < 0 || i.getSlot() >= layoutSize)
                .toList();
    }
}
