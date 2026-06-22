package ru.blacksmith.v2.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.ItemData;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.ChatInput;
import ru.blacksmith.v2.editor.GuiUtil;
import ru.blacksmith.v2.editor.menu.*;
import ru.blacksmith.v2.managers.IngotManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MenuListener implements Listener {

    public enum MenuType {
        SHOP_MAIN, SHOP_SET,
        EDITOR_LIST, EDITOR_SET, EDITOR_ITEM, EDITOR_CONFIRM, EDITOR_LAYOUT, EDITOR_BULK_ADD,
        EDITOR_REQ_PICKER, EDITOR_REQ_ITEM,
        EDITOR_SLOT_MENU, EDITOR_SLOT_ASSIGN,
        EDITOR_MENUS_LIST, EDITOR_MAIN_LAYOUT, EDITOR_MAIN_SLOT_PICK, EDITOR_MAIN_DECOR_PICK,
        EDITOR_DECOR_PICK, EDITOR_DECOR_PRESET,
        SHOP_SELECTOR, EDITOR_SHOP_LIST
    }

    private record OpenMenu(MenuType type, Object context) {}

    private final BlacksmithPlugin plugin;
    private final Map<UUID, OpenMenu> open = new HashMap<>();
    private final Map<UUID, Boolean> ignoreClose = new HashMap<>();

    private final ShopMenu shopMenu;

    public MenuListener(BlacksmithPlugin plugin) {
        this.plugin = plugin;
        this.shopMenu = new ShopMenu(plugin);
    }

    // ---- Open helpers ----

    public void openShopMain(Player player, String shopId) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.SHOP_MAIN, shopId));
        shopMenu.openMain(player, shopId);
    }

    public void openShopSelector(Player player) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.SHOP_SELECTOR, null));
        shopMenu.openShopSelector(player);
    }

    public void openShopSet(Player player, SetData set) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.SHOP_SET, set));
        shopMenu.openSet(player, set);
    }

    public void openEditorList(Player player, String shopId) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_LIST, shopId));
        new SetListMenu(plugin, shopId).open(player);
    }

    public void openEditorShopList(Player player) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_SHOP_LIST, "editor"));
        new ShopListMenu(plugin).open(player);
    }

    public void openEditorShopListMenus(Player player) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_SHOP_LIST, "menus"));
        new ShopListMenu(plugin).open(player);
    }

    public void openEditorSet(Player player, SetData set) {
        openEditorSetPage(player, set, 0);
    }

    public void openEditorSetPage(Player player, SetData set, int page) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_SET, new Object[]{set, page}));
        new SetOptionsMenu(plugin, set).open(player, page);
    }

    public void openEditorItem(Player player, SetData set, ItemData item) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_ITEM, new Object[]{set, item}));
        new ItemOptionsMenu(plugin, set, item).open(player);
    }

    public void openConfirm(Player player, ConfirmMenu menu) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_CONFIRM, menu));
        menu.open(player);
    }

    public void openLayoutEditor(Player player, SetData set) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_LAYOUT, new Object[]{set, false}));
        new SetLayoutMenu(plugin, set).open(player);
    }

    /** Открыть layout editor из пути editormenus — back/ESC возвращают в menus list */
    public void openLayoutEditorFromMenus(Player player, SetData set) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_LAYOUT, new Object[]{set, true}));
        new SetLayoutMenu(plugin, set).open(player);
    }

    /** Переоткрыть layout editor, сохранив текущий режим (из SetLayoutMenu) */
    public void reopenCurrentLayoutEditor(Player player, SetData set) {
        OpenMenu cur = open.get(player.getUniqueId());
        boolean fromMenus = cur != null
                && cur.type() == MenuType.EDITOR_LAYOUT
                && cur.context() instanceof Object[] ctx
                && Boolean.TRUE.equals(ctx[1]);
        if (fromMenus) openLayoutEditorFromMenus(player, set);
        else openLayoutEditor(player, set);
    }

    public void openBulkAdd(Player player, SetData set) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_BULK_ADD, set));
        new BulkAddMenu(plugin, set).open(player);
    }

    public void openRequirementPicker(Player player, SetData editSet, ItemData editItem) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_REQ_PICKER, new Object[]{editSet, editItem}));
        new RequirementPickerMenu(plugin, editSet, editItem).open(player);
    }

    public void openRequirementItemPicker(Player player, SetData editSet, ItemData editItem, SetData pickedSet) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_REQ_ITEM, new Object[]{editSet, editItem, pickedSet}));
        new RequirementItemMenu(plugin, editSet, editItem, pickedSet).open(player);
    }

    public void openSlotMenu(Player player, SetData set, int targetSlot) {
        OpenMenu cur = open.get(player.getUniqueId());
        boolean fromMenus = false;
        if (cur != null) {
            if (cur.type() == MenuType.EDITOR_LAYOUT && cur.context() instanceof Object[] ctx && ctx.length > 1)
                fromMenus = Boolean.TRUE.equals(ctx[1]);
            else if (cur.type() == MenuType.EDITOR_SLOT_ASSIGN && cur.context() instanceof Object[] ctx && ctx.length > 2)
                fromMenus = Boolean.TRUE.equals(ctx[2]);
        }
        openSlotMenuInternal(player, set, targetSlot, fromMenus);
    }

    private void openSlotMenuInternal(Player player, SetData set, int targetSlot, boolean fromMenus) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_SLOT_MENU, new Object[]{set, targetSlot, fromMenus}));
        player.openInventory(new SetLayoutMenu(plugin, set).buildSlotMenuInventory(targetSlot));
    }

    public void openSlotAssign(Player player, SetData set, int targetSlot) {
        OpenMenu cur = open.get(player.getUniqueId());
        boolean fromMenus = cur != null
                && cur.type() == MenuType.EDITOR_SLOT_MENU
                && cur.context() instanceof Object[] ctx
                && ctx.length > 2
                && Boolean.TRUE.equals(ctx[2]);
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_SLOT_ASSIGN, new Object[]{set, targetSlot, fromMenus}));
        player.openInventory(new SetLayoutMenu(plugin, set).buildAssignExistingInventory());
    }

    public void openEditorMenusList(Player player, String shopId) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_MENUS_LIST, shopId));
        new EditorMenusListMenu(plugin, shopId).open(player);
    }

    public void openMainMenuLayout(Player player, String shopId) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_MAIN_LAYOUT, shopId));
        new MainMenuLayoutMenu(plugin, shopId).open(player);
    }

    public void openMainMenuSlotPick(Player player, String shopId, int targetSlot) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_MAIN_SLOT_PICK, new Object[]{shopId, targetSlot}));
        player.openInventory(new MainMenuLayoutMenu(plugin, shopId).buildSetPickerInventory());
    }

    public void openMainMenuDecorPicker(Player player, String shopId, int targetSlot) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_MAIN_DECOR_PICK, new Object[]{shopId, targetSlot}));
        player.openInventory(new DecorPickerMenu().buildInventory());
    }

    public void openDecorPicker(Player player, SetData set, int targetSlot) {
        OpenMenu cur = open.get(player.getUniqueId());
        boolean fromMenus = false;
        if (cur != null) {
            if (cur.type() == MenuType.EDITOR_LAYOUT && cur.context() instanceof Object[] ctx && ctx.length > 1)
                fromMenus = Boolean.TRUE.equals(ctx[1]);
            else if (cur.type() == MenuType.EDITOR_SLOT_MENU && cur.context() instanceof Object[] ctx && ctx.length > 2)
                fromMenus = Boolean.TRUE.equals(ctx[2]);
        }
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_DECOR_PICK, new Object[]{set, targetSlot, fromMenus}));
        player.openInventory(new DecorPickerMenu().buildInventory());
    }

    public void openDecorPreset(Player player, SetData set) {
        markIgnoreClose(player);
        open.put(player.getUniqueId(), new OpenMenu(MenuType.EDITOR_DECOR_PRESET, set));
        player.openInventory(new DecorPresetMenu().buildInventory());
    }

    /** Вызвать перед открытием стороннего меню (ItemPickerMenu и т.п.) */
    public void markNextOpenAsIgnore(Player player) {
        markIgnoreClose(player);
        // Убираем запись — пока picker открыт, мы не в нашем меню
        // При возврате openEditorItem восстановит запись
        open.remove(player.getUniqueId());
    }

    private void markIgnoreClose(Player player) {
        ignoreClose.put(player.getUniqueId(), true);
    }

    // ---- Click handler ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        OpenMenu current = open.get(player.getUniqueId());
        if (current == null) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        int slot = e.getRawSlot();

        // Для BulkAddMenu обрабатываем клики по нижнему инвентарю (инвентарь игрока)
        if (current.type() == MenuType.EDITOR_BULK_ADD && slot >= e.getView().getTopInventory().getSize()) {
            SetData bulkSet = (SetData) current.context();
            new BulkAddMenu(plugin, bulkSet).handleBottomClick(player, e.getCurrentItem());
            return;
        }

        if (slot >= e.getView().getTopInventory().getSize()) return;

        boolean right  = e.getClick() == ClickType.RIGHT;
        boolean middle = e.getClick() == ClickType.MIDDLE;
        boolean shift  = e.getClick().isShiftClick();

        switch (current.type()) {

            case SHOP_SELECTOR -> {
                List<String> shopIds = plugin.getSetManager().getShopIds();
                if (slot >= 0 && slot < shopIds.size()) {
                    openShopMain(player, shopIds.get(slot));
                }
            }

            case SHOP_MAIN -> {
                String shopId = (String) current.context();
                for (SetData set : plugin.getSetManager().getSetsByShop(shopId)) {
                    if (set.getMenuSlot() == slot) {
                        if (plugin.getLevelManager().getLevel(player) < set.getRequiredLevel()) {
                            player.sendMessage("§cТребуется уровень §e" + set.getRequiredLevel());
                            return;
                        }
                        openShopSet(player, set);
                        return;
                    }
                }
            }

            case SHOP_SET -> {
                SetData set = (SetData) current.context();
                int size = Math.max(1, Math.min(6, set.getMenuRows())) * 9;
                if (slot == size - 1) { openShopMain(player, set.getShopId()); return; }
                // Используем ПОСЛЕДНИЙ предмет на слоте — именно он рендерится игроку
                ItemData target = null;
                for (ItemData item : set.getItems().values()) {
                    if (item.getSlot() == slot) target = item;
                }
                if (target != null) shopMenu.tryBuy(player, set, target);
            }

            case EDITOR_SHOP_LIST -> {
                String shopListMode = current.context() instanceof String s ? s : "editor";
                ShopListMenu slm = new ShopListMenu(plugin);
                if (slot == 49) {
                    ru.blacksmith.v2.editor.ChatInput.prompt(plugin, player,
                            "Введите ID нового магазина (латиница, без пробелов):", input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        ru.blacksmith.v2.editor.ChatInput.prompt(plugin, player,
                                "Введите название магазина (поддерживаются &-цвета):", name -> {
                            plugin.getSetManager().setShopName(id, name);
                            player.sendMessage(GuiUtil.color("&aМагазин &e" + id + " &aсоздан!"));
                            if (shopListMode.equals("menus")) openEditorShopListMenus(player);
                            else openEditorShopList(player);
                        });
                    });
                } else {
                    String shopId = slm.getShopAtSlot(slot);
                    if (shopId != null) {
                        if (right) {
                            if (plugin.getSetManager().getSetsByShop(shopId).isEmpty()) {
                                plugin.getSetManager().deleteShop(shopId);
                                player.sendMessage(GuiUtil.color("&cМагазин &e" + shopId + " &cудалён."));
                                if (shopListMode.equals("menus")) openEditorShopListMenus(player);
                                else openEditorShopList(player);
                            } else {
                                player.sendMessage(GuiUtil.color("&cНельзя удалить — в магазине есть сеты!"));
                            }
                        } else {
                            if (shopListMode.equals("menus")) openEditorMenusList(player, shopId);
                            else openEditorList(player, shopId);
                        }
                    }
                }
            }

            case EDITOR_LIST -> {
                String shopId = (String) current.context();
                handleEditorList(player, shopId, slot, right);
            }

            case EDITOR_SET -> {
                Object[] setCtx = (Object[]) current.context();
                SetData set = (SetData) setCtx[0];
                int page = (Integer) setCtx[1];
                handleEditorSet(player, set, slot, right, page);
            }

            case EDITOR_ITEM -> {
                Object[] ctx = (Object[]) current.context();
                SetData set   = (SetData) ctx[0];
                ItemData item = (ItemData) ctx[1];
                new ItemOptionsMenu(plugin, set, item).handleClick(player, slot, right, middle);
            }

            case EDITOR_CONFIRM -> {
                ConfirmMenu confirm = (ConfirmMenu) current.context();
                if (slot == 11) confirm.getOnConfirm().run();
                else if (slot == 15) confirm.getOnCancel().run();
            }

            case EDITOR_LAYOUT -> {
                Object[] lCtx = (Object[]) current.context();
                SetData set = (SetData) lCtx[0];
                boolean fromMenus = Boolean.TRUE.equals(lCtx[1]);
                int layoutSize = Math.max(1, Math.min(6, set.getMenuRows())) * 9;
                // Перехватываем кнопку "Назад" до делегирования в SetLayoutMenu
                if (slot == layoutSize - 1) {
                    if (fromMenus) openEditorMenusList(player, set.getShopId());
                    else openEditorSet(player, set);
                    return;
                }
                new SetLayoutMenu(plugin, set).handleClick(player, slot, right);
            }

            case EDITOR_BULK_ADD -> {
                // slot 8 = "Готово →", остальные верхние слоты = filler
                if (slot == 8) {
                    SetData set = (SetData) current.context();
                    openEditorSet(player, set);
                }
            }

            case EDITOR_REQ_PICKER -> {
                Object[] ctx    = (Object[]) current.context();
                SetData editSet = (SetData)  ctx[0];
                ItemData target = (ItemData) ctx[1];
                int invSize = e.getView().getTopInventory().getSize();

                if (slot == 0) {
                    target.setRequiresPrev(null);
                    plugin.getSetManager().saveSet(editSet);
                    player.sendMessage(GuiUtil.color("§aЗависимость убрана."));
                    openEditorItem(player, editSet, target);
                    return;
                }
                if (slot == invSize - 1) {
                    openEditorItem(player, editSet, target);
                    return;
                }
                // Клик по сету → уровень 2 (предметы этого сета)
                SetData pickedSet = new RequirementPickerMenu(plugin, editSet, target).getSetAtSlot(slot);
                if (pickedSet != null) {
                    openRequirementItemPicker(player, editSet, target, pickedSet);
                }
            }

            case EDITOR_REQ_ITEM -> {
                Object[] ctx     = (Object[]) current.context();
                SetData editSet  = (SetData)  ctx[0];
                ItemData target  = (ItemData) ctx[1];
                SetData pickedSet = (SetData) ctx[2];
                int invSize = e.getView().getTopInventory().getSize();

                if (slot == 0) {
                    // ← Назад к выбору сета
                    openRequirementPicker(player, editSet, target);
                    return;
                }
                if (slot == invSize - 1) {
                    // Назад в ItemOptionsMenu
                    openEditorItem(player, editSet, target);
                    return;
                }
                // Клик по предмету → устанавливаем зависимость
                ItemData picked = new RequirementItemMenu(plugin, editSet, target, pickedSet).getItemAtSlot(slot);
                if (picked != null) {
                    String req = pickedSet.getId() + "." + picked.getId();
                    target.setRequiresPrev(req);
                    plugin.getSetManager().saveSet(editSet);
                    player.sendMessage(GuiUtil.color("§aЗависимость: §e" + req));
                    openEditorItem(player, editSet, target);
                }
            }

            case EDITOR_SLOT_MENU -> {
                Object[] ctx   = (Object[]) current.context();
                SetData set    = (SetData)  ctx[0];
                int targetSlot = (Integer)  ctx[1];
                boolean smFromMenus = ctx.length > 2 && Boolean.TRUE.equals(ctx[2]);

                if (slot == 10) {
                    ChatInput.prompt(plugin, player,
                            "Введите ID нового предмета (например: helmet):", input -> {
                        String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                        if (set.getItem(id) != null) {
                            player.sendMessage(GuiUtil.color("§cПредмет §e" + id + " §cуже существует!"));
                            openSlotMenuInternal(player, set, targetSlot, smFromMenus);
                            return;
                        }
                        ItemData newItem = new ItemData(id);
                        newItem.setSlot(targetSlot);
                        set.putItem(newItem);
                        plugin.getSetManager().saveSet(set);
                        player.sendMessage(GuiUtil.color("§aПредмет §e" + id + " §aсоздан на слоте §e" + targetSlot));
                        openEditorItem(player, set, newItem);
                    });
                } else if (slot == 12) {
                    if (!new SetLayoutMenu(plugin, set).getUnassignedItems().isEmpty()) {
                        openSlotAssign(player, set, targetSlot);
                    }
                } else if (slot == 14) {
                    openDecorPicker(player, set, targetSlot);
                } else if (slot == 16) {
                    if (smFromMenus) openLayoutEditorFromMenus(player, set);
                    else openLayoutEditor(player, set);
                }
            }

            case EDITOR_SLOT_ASSIGN -> {
                Object[] ctx   = (Object[]) current.context();
                SetData set    = (SetData)  ctx[0];
                int targetSlot = (Integer)  ctx[1];
                boolean saFromMenus = ctx.length > 2 && Boolean.TRUE.equals(ctx[2]);
                List<ItemData> items = new SetLayoutMenu(plugin, set).getUnassignedItems();

                if (slot < items.size()) {
                    ItemData chosen = items.get(slot);
                    chosen.setSlot(targetSlot);
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage(GuiUtil.color("§aПредмет §e" + chosen.getName() +
                            " §aназначен на слот §e" + targetSlot));
                    if (saFromMenus) openLayoutEditorFromMenus(player, set);
                    else openLayoutEditor(player, set);
                }
            }

            case EDITOR_MENUS_LIST -> {
                String shopId = (String) current.context();
                EditorMenusListMenu eml = new EditorMenusListMenu(plugin, shopId);
                if (slot == 0) {
                    openMainMenuLayout(player, shopId);
                } else if (slot == 8) {
                    // Назад к списку магазинов (режим menus)
                    openEditorShopListMenus(player);
                } else {
                    SetData set = eml.getSetAtSlot(slot);
                    if (set != null) openLayoutEditorFromMenus(player, set);
                }
            }

            case EDITOR_MAIN_LAYOUT -> {
                String shopId = (String) current.context();
                new MainMenuLayoutMenu(plugin, shopId).handleClick(player, slot, right, shift);
            }

            case EDITOR_MAIN_DECOR_PICK -> {
                Object[] ctx2  = (Object[]) current.context();
                String shopId  = (String) ctx2[0];
                int targetSlot = (Integer) ctx2[1];
                if (slot == 26) {
                    openMainMenuLayout(player, shopId);
                    return;
                }
                if (slot >= 0 && slot < DecorPickerMenu.GLASS_PANES.length) {
                    String mat = DecorPickerMenu.GLASS_PANES[slot].name();
                    plugin.getSetManager().setMainMenuDecoration(shopId, targetSlot, mat);
                    player.sendMessage(GuiUtil.color("§aДекор §e" + mat + " §aпоставлен на слот §e" + targetSlot));
                    openMainMenuLayout(player, shopId);
                }
            }

            case EDITOR_MAIN_SLOT_PICK -> {
                Object[] ctx2  = (Object[]) current.context();
                String shopId  = (String) ctx2[0];
                int targetSlot = (Integer) ctx2[1];
                MainMenuLayoutMenu mlm = new MainMenuLayoutMenu(plugin, shopId);
                SetData chosen = mlm.getSetAtPickerSlot(slot);
                if (chosen != null) {
                    int oldSlot = chosen.getMenuSlot();
                    if (oldSlot >= 0 && oldSlot < 53) chosen.setMenuSlot(-1);
                    chosen.setMenuSlot(targetSlot);
                    plugin.getSetManager().saveSet(chosen);
                    player.sendMessage(GuiUtil.color("§aСет §e" + chosen.getName() +
                            " §aназначен на слот §e" + targetSlot));
                    openMainMenuLayout(player, shopId);
                }
            }

            case EDITOR_DECOR_PICK -> {
                Object[] ctx   = (Object[]) current.context();
                SetData set    = (SetData) ctx[0];
                int targetSlot = (Integer) ctx[1];
                boolean dpFromMenus = ctx.length > 2 && Boolean.TRUE.equals(ctx[2]);

                if (slot == 26) {
                    if (dpFromMenus) openLayoutEditorFromMenus(player, set);
                    else openLayoutEditor(player, set);
                    return;
                }
                if (slot >= 0 && slot < DecorPickerMenu.GLASS_PANES.length) {
                    String mat = DecorPickerMenu.GLASS_PANES[slot].name();
                    set.setDecoration(targetSlot, mat);
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage(GuiUtil.color("§aДекор §e" + mat + " §aпоставлен на слот §e" + targetSlot));
                    if (dpFromMenus) openLayoutEditorFromMenus(player, set);
                    else openLayoutEditor(player, set);
                }
            }

            case EDITOR_DECOR_PRESET -> {
                SetData set = (SetData) current.context();
                if (slot == 8) {
                    openEditorSet(player, set);
                    return;
                }
                String presetId = DecorPresetMenu.getPresetId(slot);
                if (presetId != null) {
                    DecorPresetMenu.apply(set, presetId);
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage(GuiUtil.color("§aПресет декора применён."));
                    openEditorSet(player, set);
                }
            }
        }
    }

    // ---- Editor List ----

    private void handleEditorList(Player player, String shopId, int slot, boolean right) {
        // Слот 45 = назад к списку магазинов
        if (slot == 45) {
            openEditorShopList(player);
            return;
        }

        if (slot == 49) {
            ru.blacksmith.v2.editor.ChatInput.prompt(plugin, player,
                    "Введите ID нового сета (латиница, без пробелов):", input -> {
                String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                if (plugin.getSetManager().getSet(id) != null) {
                    player.sendMessage("§cСет §e" + id + " §cуже существует!");
                    openEditorList(player, shopId);
                    return;
                }
                SetData newSet = plugin.getSetManager().createSet(id, shopId);
                player.sendMessage("§aСет §e" + id + " §aсоздан!");
                openEditorSet(player, newSet);
            });
            return;
        }

        SetListMenu slm = new SetListMenu(plugin, shopId);
        SetData set = slm.getSetAtSlot(slot);
        if (set == null) return;

        if (right) {
            openConfirm(player, new ConfirmMenu(plugin,
                    "§cУдалить сет §e" + set.getName() + "§c?",
                    () -> {
                        plugin.getSetManager().deleteSet(set.getId());
                        player.sendMessage("§cСет §e" + set.getId() + " §cудалён.");
                        openEditorList(player, shopId);
                    },
                    () -> openEditorList(player, shopId)));
        } else {
            openEditorSet(player, set);
        }
    }

    // ---- Editor Set ----

    private void handleEditorSet(Player player, SetData set, int slot, boolean right, int page) {
        if (slot == 8) { openEditorList(player, set.getShopId()); return; }
        if (slot == 9) { openLayoutEditor(player, set); return; }
        if (slot == 10) { openDecorPreset(player, set); return; }

        // + Добавить (слот 48)
        if (slot == 48) {
            if (!right) {
                openBulkAdd(player, set);
            } else {
                ru.blacksmith.v2.editor.ChatInput.prompt(plugin, player,
                        "Введите ID нового предмета (например: helmet, sword):", input -> {
                    String id = input.toLowerCase().replaceAll("[^a-z0-9_]", "_");
                    if (set.getItem(id) != null) {
                        player.sendMessage("§cПредмет §e" + id + " §cуже есть в этом сете!");
                        openEditorSetPage(player, set, page);
                        return;
                    }
                    ItemData newItem = new ItemData(id);
                    set.putItem(newItem);
                    plugin.getSetManager().saveSet(set);
                    player.sendMessage("§aПредмет §e" + id + " §aдобавлен!");
                    openEditorItem(player, set, newItem);
                });
            }
            return;
        }

        // Пагинация: слот 45 = назад, слот 49 = вперёд
        int totalItems = set.getItems().size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / 27.0));
        if (slot == 45 && page > 0) { openEditorSetPage(player, set, page - 1); return; }
        if (slot == 49 && page < totalPages - 1) { openEditorSetPage(player, set, page + 1); return; }

        // Предметы (слоты 18–44, 27 на страницу)
        if (slot >= 18 && slot <= 44) {
            int idx = page * 27 + (slot - 18);
            var items = new java.util.ArrayList<>(set.getItems().values());
            if (idx < items.size()) {
                ItemData item = items.get(idx);
                if (right) {
                    openConfirm(player, new ConfirmMenu(plugin,
                            "§cУдалить предмет §e" + item.getName() + "§c?",
                            () -> {
                                set.removeItem(item.getId());
                                plugin.getSetManager().saveSet(set);
                                player.sendMessage("§cПредмет §e" + item.getId() + " §cудалён.");
                                openEditorSetPage(player, set, page);
                            },
                            () -> openEditorSetPage(player, set, page)));
                } else {
                    openEditorItem(player, set, item);
                }
                return;
            }
        }

        new SetOptionsMenu(plugin, set).handleClick(player, slot, right, page);
    }

    // ---- Close / Quit ----

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (ignoreClose.remove(uuid) != null) return;
        OpenMenu menu = open.remove(uuid);
        if (menu == null) return;
        Player p = (Player) e.getPlayer();
        // При закрытии BulkAddMenu (ESC) — возвращаем в SetOptionsMenu
        if (menu.type() == MenuType.EDITOR_BULK_ADD) {
            plugin.getServer().getScheduler().runTask(plugin,
                () -> openEditorSet(p, (SetData) menu.context()));
        }
        // При закрытии RequirementPickerMenu (ESC) — возвращаем в ItemOptionsMenu
        if (menu.type() == MenuType.EDITOR_REQ_PICKER) {
            Object[] ctx = (Object[]) menu.context();
            plugin.getServer().getScheduler().runTask(plugin,
                () -> openEditorItem(p, (SetData) ctx[0], (ItemData) ctx[1]));
        }
        // При закрытии RequirementItemMenu (ESC) — возвращаем к выбору сета
        if (menu.type() == MenuType.EDITOR_REQ_ITEM) {
            Object[] ctx = (Object[]) menu.context();
            plugin.getServer().getScheduler().runTask(plugin,
                () -> openRequirementPicker(p, (SetData) ctx[0], (ItemData) ctx[1]));
        }
        // ESC из layout editor → назад в зависимости от режима
        if (menu.type() == MenuType.EDITOR_LAYOUT) {
            Object[] ctx = (Object[]) menu.context();
            SetData lSet = (SetData) ctx[0];
            boolean lFromMenus = Boolean.TRUE.equals(ctx[1]);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (lFromMenus) openEditorMenusList(p, lSet.getShopId());
                else openEditorSet(p, lSet);
            });
        }
        // При закрытии меню выбора действия для слота (ESC) — назад в layout editor
        // Если закрытие вызвано ChatInput.prompt — ничего не открываем: callback сам откроет нужное меню
        if (menu.type() == MenuType.EDITOR_SLOT_MENU) {
            if (!ChatInput.isWaiting(uuid)) {
                Object[] ctx = (Object[]) menu.context();
                SetData smSet = (SetData) ctx[0];
                boolean smFromMenus = ctx.length > 2 && Boolean.TRUE.equals(ctx[2]);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    if (smFromMenus) openLayoutEditorFromMenus(p, smSet);
                    else openLayoutEditor(p, smSet);
                });
            }
        }
        // При закрытии меню выбора существующего предмета (ESC) — назад в меню действия для слота
        if (menu.type() == MenuType.EDITOR_SLOT_ASSIGN) {
            Object[] ctx = (Object[]) menu.context();
            SetData saSet = (SetData) ctx[0];
            int saTargetSlot = (Integer) ctx[1];
            boolean saFromMenus = ctx.length > 2 && Boolean.TRUE.equals(ctx[2]);
            plugin.getServer().getScheduler().runTask(plugin,
                () -> openSlotMenuInternal(p, saSet, saTargetSlot, saFromMenus));
        }
        // ESC из редактора главного меню → список всех меню
        if (menu.type() == MenuType.EDITOR_MAIN_LAYOUT) {
            String shopId = (String) menu.context();
            plugin.getServer().getScheduler().runTask(plugin, () -> openEditorMenusList(p, shopId));
        }
        // ESC из выбора сета → назад в редактор главного меню
        if (menu.type() == MenuType.EDITOR_MAIN_SLOT_PICK) {
            Object[] ctx2 = (Object[]) menu.context();
            String shopId = (String) ctx2[0];
            plugin.getServer().getScheduler().runTask(plugin, () -> openMainMenuLayout(p, shopId));
        }
        // ESC из выбора декора главного меню → назад в редактор главного меню
        if (menu.type() == MenuType.EDITOR_MAIN_DECOR_PICK) {
            Object[] ctx2 = (Object[]) menu.context();
            String shopId = (String) ctx2[0];
            plugin.getServer().getScheduler().runTask(plugin, () -> openMainMenuLayout(p, shopId));
        }
        // ESC из выбора цвета декора → назад в layout editor
        if (menu.type() == MenuType.EDITOR_DECOR_PICK) {
            Object[] ctx = (Object[]) menu.context();
            SetData dpSet = (SetData) ctx[0];
            boolean dpFromMenus = ctx.length > 2 && Boolean.TRUE.equals(ctx[2]);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (dpFromMenus) openLayoutEditorFromMenus(p, dpSet);
                else openLayoutEditor(p, dpSet);
            });
        }
        // ESC из меню пресетов декора → назад в настройки сета
        if (menu.type() == MenuType.EDITOR_DECOR_PRESET) {
            plugin.getServer().getScheduler().runTask(plugin,
                () -> openEditorSet(p, (SetData) menu.context()));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        open.remove(uuid);
        ignoreClose.remove(uuid);
    }

    public ShopMenu getShopMenu() { return shopMenu; }
}
