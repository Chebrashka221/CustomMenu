package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.editor.GuiUtil;

/**
 * Маленькое меню подтверждения удаления.
 * [11] = Да (зелёное стекло)
 * [13] = Вопрос
 * [15] = Нет (красное стекло)
 */
public class ConfirmMenu {

    public static final String TITLE_PREFIX = "§cПодтверждение: ";

    private final BlacksmithPlugin plugin;
    private final String question;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmMenu(BlacksmithPlugin plugin, String question, Runnable onConfirm, Runnable onCancel) {
        this.plugin = plugin;
        this.question = question;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + "удаление");
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        inv.setItem(11, GuiUtil.makeItem(Material.LIME_STAINED_GLASS_PANE, "&a✔ Да, удалить"));
        inv.setItem(13, GuiUtil.makeItem(Material.BARRIER, question));
        inv.setItem(15, GuiUtil.makeItem(Material.RED_STAINED_GLASS_PANE, "&c✖ Отмена"));

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot) {
        if (slot == 11) onConfirm.run();
        else if (slot == 15) onCancel.run();
    }

    public Runnable getOnConfirm() { return onConfirm; }
    public Runnable getOnCancel() { return onCancel; }
}
