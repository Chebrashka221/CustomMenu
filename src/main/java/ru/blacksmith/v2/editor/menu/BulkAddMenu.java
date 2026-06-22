package ru.blacksmith.v2.editor.menu;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.ItemData;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.GuiUtil;

/**
 * Меню быстрого добавления предметов в сет.
 * Игрок кликает по любому IA предмету в своём инвентаре (нижняя часть) — предмет добавляется.
 * Никакого выхода из меню не нужно.
 */
public class BulkAddMenu {

    public static final String TITLE = "§8Кликни по предмету в инвентаре";

    private final BlacksmithPlugin plugin;
    private final SetData set;

    public BulkAddMenu(BlacksmithPlugin plugin, SetData set) {
        this.plugin = plugin;
        this.set = set;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        inv.setItem(0, GuiUtil.makeItem(Material.PAPER, "&eКак добавить предметы",
                "&7Кликни по любому предмету",
                "&7в своём инвентаре §eснизу§7.",
                "&7Поддерживаются IA и ванильные предметы!"));

        refreshCounter(inv);

        inv.setItem(8, GuiUtil.makeItem(Material.ARROW, "&aГотово →",
                "&7Вернуться к настройкам сета"));

        player.openInventory(inv);
    }

    /** Клик по предмету в нижнем инвентаре (инвентарь игрока). */
    public void handleBottomClick(Player player, ItemStack clicked) {
        if (clicked == null || clicked.getType() == Material.AIR) return;

        CustomStack cs = CustomStack.byItemStack(clicked);

        String itemId;
        String displayName;
        ItemData newItem;

        if (cs != null) {
            // ItemsAdder предмет
            String iaId = cs.getNamespacedID();
            itemId = iaId.contains(":") ? iaId.substring(iaId.indexOf(':') + 1) : iaId;
            itemId = itemId.replaceAll("[^a-z0-9_]", "_").toLowerCase();
            itemId = uniqueId(itemId);

            displayName = itemId;
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                displayName = clicked.getItemMeta().getDisplayName();
            }

            newItem = new ItemData(itemId);
            newItem.setItemsAdderId(iaId);
            newItem.setName(displayName);
            newItem.setSlot(-1);

            player.sendMessage(GuiUtil.color("&aДобавлен: &e" + itemId + " &7(IA: " + iaId + ")"));
        } else {
            // Ванильный предмет
            String matName = clicked.getType().name().toLowerCase();
            itemId = uniqueId(matName);

            displayName = matName;
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()) {
                displayName = clicked.getItemMeta().getDisplayName();
            }

            newItem = new ItemData(itemId);
            newItem.setMaterial(clicked.getType().name());
            newItem.setAmount(clicked.getAmount());
            newItem.setName(displayName);
            newItem.setSlot(-1);

            player.sendMessage(GuiUtil.color("&aДобавлен: &e" + itemId + " &7(ваниль: " + clicked.getType().name() + ")"));
        }

        set.putItem(newItem);
        plugin.getSetManager().saveSet(set);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1.2f);

        // Обновляем счётчик в открытом меню
        var topInv = player.getOpenInventory().getTopInventory();
        if (topInv != null) refreshCounter(topInv);
    }

    private String uniqueId(String base) {
        if (set.getItem(base) == null) return base;
        int n = 2;
        while (set.getItem(base + "_" + n) != null) n++;
        return base + "_" + n;
    }

    private void refreshCounter(Inventory inv) {
        inv.setItem(4, GuiUtil.makeItem(Material.LIME_STAINED_GLASS_PANE,
                "&aПредметов в сете: &e" + set.getItems().size(),
                "&7Нажми &aГотово → &7когда закончишь"));
    }
}
