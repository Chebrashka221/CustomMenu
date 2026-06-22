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

import java.util.Arrays;
import java.util.List;

/**
 * Уровень 1 — выбор сета для зависимости.
 * Показывает все сеты. Клик → открывает список предметов выбранного сета.
 */
public class RequirementPickerMenu {

    public static final String TITLE = "§8Выбери сет для зависимости";

    private final BlacksmithPlugin plugin;
    private final SetData editSet;   // сет, в котором редактируем предмет
    private final ItemData editItem; // предмет, которому ставим зависимость

    public RequirementPickerMenu(BlacksmithPlugin plugin, SetData editSet, ItemData editItem) {
        this.plugin   = plugin;
        this.editSet  = editSet;
        this.editItem = editItem;
    }

    public List<SetData> getSets() {
        return List.copyOf(plugin.getSetManager().getAllSets());
    }

    public void open(Player player) {
        List<SetData> sets = getSets();

        int needed = sets.size() + 2; // +2: "убрать" + "назад"
        int rows   = Math.max(3, (int) Math.ceil(needed / 9.0));
        rows = Math.min(rows, 6);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(null, size, TITLE);
        for (int i = 0; i < size; i++) inv.setItem(i, GuiUtil.filler());

        // Слот 0 — убрать зависимость
        String cur = editItem.hasRequirement() ? editItem.getRequiresPrev() : "§7нет";
        inv.setItem(0, GuiUtil.makeItem(Material.RED_DYE, "&cУбрать зависимость",
                "&7Текущая: &f" + cur,
                "",
                "&7Предмет станет доступен без условий"));

        // Слоты 1+ — все сеты
        for (int i = 0; i < sets.size() && (i + 1) < size - 1; i++) {
            SetData s = sets.get(i);
            ItemStack icon = IngotManager.getIcon(s.getIconItemsAdderId());
            if (icon == null) icon = GuiUtil.tryMaterialOf(s.getIconMaterial(), Material.CHEST);

            // Проверяем: текущая зависимость принадлежит этому сету?
            boolean fromThis = editItem.hasRequirement()
                    && editItem.getRequiresPrev().startsWith(s.getId() + ".");

            var meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(GuiUtil.color((fromThis ? "&a✔ " : "&f") + s.getName()));
                meta.setLore(Arrays.asList(
                        GuiUtil.color("&7ID: &e" + s.getId()),
                        GuiUtil.color("&7Предметов: &e" + s.getItems().size()),
                        GuiUtil.color("&7Мин. уровень: &e" + s.getRequiredLevel()),
                        "",
                        GuiUtil.color("&eНажмите &7— выбрать предмет из этого сета")
                ));
                icon.setItemMeta(meta);
            }
            inv.setItem(i + 1, icon);
        }

        // Последний слот — назад
        inv.setItem(size - 1, GuiUtil.back());
        player.openInventory(inv);
    }

    /** Возвращает сет по слоту (slot 1 → sets[0] и т.д.), null если не сет. */
    public SetData getSetAtSlot(int slot) {
        if (slot < 1) return null;
        List<SetData> sets = getSets();
        int idx = slot - 1;
        return idx < sets.size() ? sets.get(idx) : null;
    }
}
