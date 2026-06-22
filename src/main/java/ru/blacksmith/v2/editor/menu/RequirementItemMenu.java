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
 * Уровень 2 — выбор конкретного предмета внутри выбранного сета.
 * Клик → устанавливает зависимость и возвращает в ItemOptionsMenu.
 */
public class RequirementItemMenu {

    public static final String TITLE_PREFIX = "§8Предметы: ";

    private final BlacksmithPlugin plugin;
    private final SetData editSet;     // сет, в котором редактируем
    private final ItemData editItem;   // предмет, которому ставим зависимость
    private final SetData pickedSet;   // сет, из которого выбираем предмет

    public RequirementItemMenu(BlacksmithPlugin plugin, SetData editSet,
                               ItemData editItem, SetData pickedSet) {
        this.plugin     = plugin;
        this.editSet    = editSet;
        this.editItem   = editItem;
        this.pickedSet  = pickedSet;
    }

    public List<ItemData> getItems() {
        return List.copyOf(pickedSet.getItems().values());
    }

    public void open(Player player) {
        List<ItemData> items = getItems();

        int needed = items.size() + 2; // +2: "← назад к сетам" + "назад"
        int rows   = Math.max(3, (int) Math.ceil(needed / 9.0));
        rows = Math.min(rows, 6);
        int size = rows * 9;

        Inventory inv = Bukkit.createInventory(null, size,
                TITLE_PREFIX + pickedSet.getName());
        for (int i = 0; i < size; i++) inv.setItem(i, GuiUtil.filler());

        // Слот 0 — назад к выбору сета
        inv.setItem(0, GuiUtil.makeItem(Material.ARROW, "&7← &fВыбор сета",
                "&7Вернуться к списку сетов"));

        // Слоты 1+ — предметы выбранного сета
        for (int i = 0; i < items.size() && (i + 1) < size - 1; i++) {
            ItemData it = items.get(i);
            ItemStack icon = IngotManager.getIcon(it.getItemsAdderId());
            if (icon == null) icon = new ItemStack(Material.PAPER);

            String depKey = pickedSet.getId() + "." + it.getId();
            boolean isCurrent = depKey.equals(editItem.getRequiresPrev());

            var meta = icon.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(GuiUtil.color((isCurrent ? "&a✔ " : "&f") + it.getName()));
                meta.setLore(Arrays.asList(
                        GuiUtil.color("&7ID: &e" + it.getId()),
                        GuiUtil.color("&7Зависимость: &f" + depKey),
                        "",
                        GuiUtil.color(isCurrent ? "&aУже выбрана" : "&eНажмите чтобы выбрать")
                ));
                icon.setItemMeta(meta);
            }
            inv.setItem(i + 1, icon);
        }

        // Последний слот — назад в ItemOptionsMenu
        inv.setItem(size - 1, GuiUtil.back());
        player.openInventory(inv);
    }

    /** Возвращает предмет по слоту (slot 1 → items[0] и т.д.), null если не предмет. */
    public ItemData getItemAtSlot(int slot) {
        if (slot < 1) return null;
        List<ItemData> items = getItems();
        int idx = slot - 1;
        return idx < items.size() ? items.get(idx) : null;
    }
}
