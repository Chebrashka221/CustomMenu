package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.editor.GuiUtil;

import java.util.List;

/**
 * Меню выбора цвета стеклянной панели для декорации слота.
 * Слоты 0–15: 16 цветов, слот 26: Назад.
 */
public class DecorPickerMenu {

    public static final Material[] GLASS_PANES = {
        Material.WHITE_STAINED_GLASS_PANE,
        Material.ORANGE_STAINED_GLASS_PANE,
        Material.MAGENTA_STAINED_GLASS_PANE,
        Material.LIGHT_BLUE_STAINED_GLASS_PANE,
        Material.YELLOW_STAINED_GLASS_PANE,
        Material.LIME_STAINED_GLASS_PANE,
        Material.PINK_STAINED_GLASS_PANE,
        Material.GRAY_STAINED_GLASS_PANE,
        Material.LIGHT_GRAY_STAINED_GLASS_PANE,
        Material.CYAN_STAINED_GLASS_PANE,
        Material.PURPLE_STAINED_GLASS_PANE,
        Material.BLUE_STAINED_GLASS_PANE,
        Material.BROWN_STAINED_GLASS_PANE,
        Material.GREEN_STAINED_GLASS_PANE,
        Material.RED_STAINED_GLASS_PANE,
        Material.BLACK_STAINED_GLASS_PANE
    };

    private static final String[] NAMES = {
        "Белый", "Оранжевый", "Пурпурный", "Голубой",
        "Жёлтый", "Лаймовый", "Розовый", "Серый",
        "Светло-серый", "Бирюзовый", "Фиолетовый", "Синий",
        "Коричневый", "Зелёный", "Красный", "Чёрный"
    };

    public Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Выберите цвет декора");
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        for (int i = 0; i < GLASS_PANES.length; i++) {
            ItemStack pane = new ItemStack(GLASS_PANES[i]);
            var meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName("§f" + NAMES[i]);
                meta.setLore(List.of("", "§eНажмите §7— поставить декор"));
                pane.setItemMeta(meta);
            }
            inv.setItem(i, pane);
        }

        inv.setItem(26, GuiUtil.makeItem(Material.ARROW, "§7← Назад"));
        return inv;
    }
}
