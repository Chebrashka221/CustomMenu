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

import java.util.List;

/**
 * Настройки конкретного сета:
 *
 *  [0] Название     [1] Уровень      [2] Слот в меню
 *  [3] Иконка сета  [4] Слиток       [5] Название слитка
 *  [6] Заголовок GUI[7] Строк GUI
 *  [8] ← Назад
 *
 *  [18] Предметы сета (список)
 *  [26] + Добавить предмет
 */
public class SetOptionsMenu {

    public static final String TITLE_PREFIX = "§8Сет: ";

    private final BlacksmithPlugin plugin;
    private final SetData set;

    public SetOptionsMenu(BlacksmithPlugin plugin, SetData set) {
        this.plugin = plugin;
        this.set = set;
    }

    /** Открыть на странице 0 (по умолчанию) */
    public void open(Player player) { open(player, 0); }

    public void open(Player player, int page) {
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_PREFIX + set.getId());

        for (int i = 0; i < 54; i++) inv.setItem(i, GuiUtil.filler());

        // ---- Строка 1: настройки сета ----
        inv.setItem(0, GuiUtil.makeItem(Material.NAME_TAG, "&fНазвание",
                "&7Текущее: §r" + GuiUtil.color(set.getName()),
                "", "&eНажмите чтобы изменить"));
        inv.setItem(1, GuiUtil.makeItem(Material.EXPERIENCE_BOTTLE, "&fМин. уровень",
                "&7Текущий: &e" + set.getRequiredLevel(),
                "", "&eНажмите чтобы изменить"));
        inv.setItem(2, GuiUtil.makeItem(Material.COMPASS, "&fСлот в главном меню",
                "&7Текущий: &e" + set.getMenuSlot() + " &7(0–53)",
                "", "&eНажмите чтобы изменить"));

        ItemStack iconPreview = IngotManager.getIcon(set.getIconItemsAdderId());
        if (iconPreview == null) iconPreview = GuiUtil.tryMaterialOf(set.getIconMaterial(), Material.CHEST);
        setLore(iconPreview, "&fИконка сета",
                "&7IA ID: &f" + nvl(set.getIconItemsAdderId()),
                "&7Материал: &f" + set.getIconMaterial(),
                "", "&eЛКМ &7— из руки (IA)", "&aПКМ &7— материал вручную");
        inv.setItem(3, iconPreview);

        ItemStack ingotPreview = IngotManager.getIcon(set.getIngotItemsAdderId());
        if (ingotPreview == null) ingotPreview = new ItemStack(Material.BRICK);
        setLore(ingotPreview, "&fСлиток (ресурс для крафта)",
                "&7IA ID: &f" + nvl(set.getIngotItemsAdderId()),
                "", "&eНажмите &7— взять из руки");
        inv.setItem(4, ingotPreview);

        inv.setItem(5, GuiUtil.makeItem(Material.WRITABLE_BOOK, "&fНазвание слитка",
                "&7Текущее: &f" + set.getIngotName(),
                "", "&eНажмите чтобы изменить"));
        inv.setItem(6, GuiUtil.makeItem(Material.OAK_SIGN, "&fЗаголовок GUI сета",
                "&7Текущий: §r" + GuiUtil.color(set.getMenuTitle()),
                "", "&eНажмите чтобы изменить"));
        inv.setItem(7, GuiUtil.makeItem(Material.COMPARATOR, "&fСтрок в GUI",
                "&7Текущее: &e" + set.getMenuRows() + " &7(1–6)",
                "", "&eЛКМ &7— +1", "&cПКМ &7— -1"));
        inv.setItem(8, GuiUtil.back());

        // ---- Строка 2: инструменты ----
        inv.setItem(9, GuiUtil.makeItem(Material.MAP, "&fРедактор раскладки",
                "&7Расположение предметов в меню",
                "", "&eНажмите чтобы открыть"));
        inv.setItem(10, GuiUtil.makeItem(Material.BLUE_STAINED_GLASS_PANE, "&fПресет декора",
                "&7Готовые пресеты оформления",
                "", "&eНажмите чтобы открыть"));
        inv.setItem(11, GuiUtil.makeItem(Material.ENDER_CHEST, "&fМагазин",
                "&7Текущий: &e" + plugin.getSetManager().getShopName(set.getShopId()),
                "&7ID: &f" + set.getShopId(),
                "", "&eНажмите чтобы изменить"));

        // ---- Строки 3–5: предметы сета (27 на страницу, слоты 18–44) ----
        var items = new java.util.ArrayList<>(set.getItems().values());
        int totalItems = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / 27.0));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int start = safePage * 27;

        for (int i = 0; i < 27; i++) {
            int idx = start + i;
            if (idx >= totalItems) break;
            var item = items.get(idx);
            ItemStack icon = IngotManager.getIcon(item.getItemsAdderId());
            if (icon == null && item.hasExecutableItem())
                icon = plugin.getExecutableItemsManager().getIcon(item.getExecutableItemsId());
            if (icon == null && item.hasVanillaItem()) {
                org.bukkit.Material mat = org.bukkit.Material.matchMaterial(item.getMaterial());
                if (mat != null) icon = new ItemStack(mat);
            }
            if (icon == null) icon = new ItemStack(Material.PAPER);
            setLore(icon, "&f" + item.getName(),
                    "&7ID: &f" + item.getId(),
                    "&7Цена: &e" + (int) item.getPrice() + "$",
                    "&7Слитков: &e" + item.getIngots(),
                    "&7Слот: &e" + item.getSlot(),
                    "", "&eЛКМ &7— настройки", "&cПКМ &7— удалить");
            inv.setItem(18 + i, icon);
        }

        // ---- Строка 6: навигация + кнопка добавить ----
        if (safePage > 0)
            inv.setItem(45, GuiUtil.makeItem(Material.ARROW, "&f← Предыдущая стр.",
                    "&7Страница &e" + safePage + " &7из &e" + totalPages));
        inv.setItem(46, GuiUtil.makeItem(Material.PAPER, "&7Страница &e" + (safePage + 1) + " &7/ &e" + totalPages,
                "&7Предметов: &e" + totalItems));
        if (safePage < totalPages - 1)
            inv.setItem(49, GuiUtil.makeItem(Material.ARROW, "&fСледующая стр. →",
                    "&7Страница &e" + (safePage + 2) + " &7из &e" + totalPages));
        inv.setItem(48, GuiUtil.makeItem(Material.LIME_DYE, "&a+ Добавить предмет",
                "&eЛКМ &7— добавить IA предмет из руки",
                "&aПКМ &7— ввести ID вручную"));

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot, boolean right, int page) {
        switch (slot) {
            case 0 -> ChatInput.prompt(plugin, player, "Введите название сета (поддерживаются &-цвета):", input -> {
                set.setName(input);
                plugin.getSetManager().saveSet(set);
                player.sendMessage(GuiUtil.color("&aНазвание изменено: " + input));
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            });
            case 1 -> ChatInput.prompt(plugin, player, "Введите минимальный уровень (число):", input -> {
                try {
                    set.setRequiredLevel(Integer.parseInt(input));
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage(GuiUtil.color("&aУровень установлен: &e" + input));
                } catch (NumberFormatException e) {
                    player.sendMessage(GuiUtil.color("&cНужно число!"));
                }
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            });
            case 2 -> ChatInput.prompt(plugin, player, "Введите слот в главном меню (0–53):", input -> {
                try {
                    set.setMenuSlot(Integer.parseInt(input));
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage(GuiUtil.color("&aСлот установлен: &e" + input));
                } catch (NumberFormatException e) {
                    player.sendMessage(GuiUtil.color("&cНужно число!"));
                }
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            });
            case 3 -> {
                if (right) {
                    ChatInput.prompt(plugin, player, "Введите название материала (например DIAMOND_HELMET):", input -> {
                        set.setIconMaterial(input.toUpperCase());
                        set.setIconItemsAdderId(null);
                        plugin.getSetManager().saveSet(set);
                        player.sendMessage(GuiUtil.color("&aМатериал иконки: &e" + input.toUpperCase()));
                        plugin.getMenuListener().openEditorSetPage(player, set, page);
                    });
                } else {
                    String id = IngotManager.getIdInHand(player);
                    if (id == null) { player.sendMessage(GuiUtil.color("&cДержите ItemsAdder предмет в руке!")); return; }
                    set.setIconItemsAdderId(id);
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage(GuiUtil.color("&aИконка сета: &e" + id));
                    plugin.getMenuListener().openEditorSetPage(player, set, page);
                }
            }
            case 4 -> {
                String id = IngotManager.getIdInHand(player);
                if (id == null) { player.sendMessage(GuiUtil.color("&cДержите ItemsAdder предмет (слиток) в руке!")); return; }
                set.setIngotItemsAdderId(id);
                plugin.getSetManager().saveSet(set);
                player.sendMessage(GuiUtil.color("&aСлиток: &e" + id));
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            }
            case 5 -> ChatInput.prompt(plugin, player, "Введите название слитка:", input -> {
                set.setIngotName(input);
                plugin.getSetManager().saveSet(set);
                player.sendMessage(GuiUtil.color("&aНазвание слитка: &f" + input));
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            });
            case 6 -> ChatInput.prompt(plugin, player, "Введите заголовок GUI сета (поддерживаются &-цвета):", input -> {
                set.setMenuTitle(input);
                plugin.getSetManager().saveSet(set);
                player.sendMessage(GuiUtil.color("&aЗаголовок: §r" + GuiUtil.color(input)));
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            });
            case 7 -> {
                int rows = set.getMenuRows() + (right ? -1 : 1);
                rows = Math.max(1, Math.min(6, rows));
                set.setMenuRows(rows);
                plugin.getSetManager().saveSet(set);
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            }
            case 8 -> plugin.getMenuListener().openEditorList(player, set.getShopId());
            case 11 -> ChatInput.prompt(plugin, player, "Введите ID магазина (латиница, без пробелов):", input -> {
                String newShopId = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                set.setShopId(newShopId);
                plugin.getSetManager().saveSet(set);
                plugin.getSetManager().saveShops();
                player.sendMessage(GuiUtil.color("&aМагазин изменён: &e" + newShopId));
                plugin.getMenuListener().openEditorSetPage(player, set, page);
            });
        }
    }

    private void setLore(ItemStack item, String name, String... lore) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(GuiUtil.color(name));
        meta.setLore(java.util.Arrays.stream(lore).map(GuiUtil::color).toList());
        item.setItemMeta(meta);
    }

    private String nvl(String s) { return s == null || s.isEmpty() ? "§7не задан" : s; }
}
