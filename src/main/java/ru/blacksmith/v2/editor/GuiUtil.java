package ru.blacksmith.v2.editor;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;

public class GuiUtil {

    public static String color(String s) {
        if (s == null) return "";
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color(name));
        if (lore.length > 0)
            meta.setLore(Arrays.stream(lore).map(GuiUtil::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(color(name));
        meta.setLore(lore.stream().map(GuiUtil::color).toList());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler() {
        return makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
    }

    public static ItemStack back() {
        return makeItem(Material.ARROW, "&7← &fНазад");
    }

    public static ItemStack next() {
        return makeItem(Material.ARROW, "&fВперёд →");
    }

    public static ItemStack confirm() {
        return makeItem(Material.LIME_STAINED_GLASS_PANE, "&a✔ Подтвердить");
    }

    public static ItemStack cancel() {
        return makeItem(Material.RED_STAINED_GLASS_PANE, "&c✖ Отмена");
    }

    public static ItemStack tryMaterialOf(String name, Material fallback) {
        try { return new ItemStack(Material.valueOf(name)); }
        catch (Exception e) { return new ItemStack(fallback); }
    }
}
