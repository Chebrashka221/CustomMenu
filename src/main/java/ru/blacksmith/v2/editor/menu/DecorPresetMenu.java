package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.GuiUtil;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Пресеты декораций для сетового меню.
 *
 * Слот 0: Без декора
 * Слот 1: Серая рамка
 * Слот 2: Тёмная рамка
 * Слот 3: Синяя рамка
 * Слот 4: Жёлтая рамка
 * Слот 5: Красная рамка
 * Слот 6: Фиолетовая рамка
 * Слот 8: Назад
 */
public class DecorPresetMenu {

    private record Preset(String id, String title, Material icon, String mat) {}

    private static final Preset[] PRESETS = {
        new Preset("none",   "§fБез декора",         Material.BARRIER,                  null),
        new Preset("gray",   "§7Серая рамка",         Material.GRAY_STAINED_GLASS_PANE,  "GRAY_STAINED_GLASS_PANE"),
        new Preset("black",  "§8Тёмная рамка",        Material.BLACK_STAINED_GLASS_PANE, "BLACK_STAINED_GLASS_PANE"),
        new Preset("blue",   "§9Синяя рамка",         Material.BLUE_STAINED_GLASS_PANE,  "BLUE_STAINED_GLASS_PANE"),
        new Preset("yellow", "§eЗолотая рамка",       Material.YELLOW_STAINED_GLASS_PANE,"YELLOW_STAINED_GLASS_PANE"),
        new Preset("red",    "§cКрасная рамка",       Material.RED_STAINED_GLASS_PANE,   "RED_STAINED_GLASS_PANE"),
        new Preset("purple", "§5Фиолетовая рамка",   Material.PURPLE_STAINED_GLASS_PANE,"PURPLE_STAINED_GLASS_PANE"),
    };

    public Inventory buildInventory() {
        Inventory inv = Bukkit.createInventory(null, 27, "§8Пресет декорации");
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        for (int i = 0; i < PRESETS.length; i++) {
            Preset p = PRESETS[i];
            inv.setItem(i, GuiUtil.makeItem(p.icon(), p.title(),
                    "§7Применить к меню этого сета",
                    "", "§eНажмите §7— применить"));
        }

        inv.setItem(8, GuiUtil.back());
        return inv;
    }

    /** Идентификатор пресета по UI-слоту, или null */
    public static String getPresetId(int slot) {
        if (slot >= 0 && slot < PRESETS.length) return PRESETS[slot].id();
        return null;
    }

    /** Применить пресет к сету */
    public static void apply(SetData set, String presetId) {
        set.clearDecorations();
        Preset preset = null;
        for (Preset p : PRESETS) if (p.id().equals(presetId)) { preset = p; break; }
        if (preset == null || preset.mat() == null) return; // "none" = просто очистить

        int rows    = Math.max(1, Math.min(6, set.getMenuRows()));
        int size    = rows * 9;
        int lastSlot = size - 1;

        Set<Integer> border = new LinkedHashSet<>();
        // Верхняя строка
        for (int i = 0; i < 9; i++) border.add(i);
        // Нижняя строка
        for (int i = size - 9; i < size; i++) border.add(i);
        // Левая и правая колонки (средние строки)
        for (int r = 1; r < rows - 1; r++) {
            border.add(r * 9);
            border.add(r * 9 + 8);
        }

        String mat = preset.mat();
        for (int s : border) {
            if (s == lastSlot || s < 0 || s >= size) continue;
            set.setDecoration(s, mat);
        }
    }
}
