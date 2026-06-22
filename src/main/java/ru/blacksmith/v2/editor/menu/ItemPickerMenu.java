package ru.blacksmith.v2.editor.menu;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.editor.GuiUtil;

import java.util.function.Consumer;

/**
 * Открывает меню с одним слотом.
 * Игрок кладёт предмет → закрывает → колбэк получает ItemsAdder ID.
 */
public class ItemPickerMenu {

    public static final String TITLE = "§8Положи предмет в центр";

    /**
     * @param onPick  колбэк с ItemsAdder ID (или null если предмет не IA)
     * @param onClose колбэк если игрок закрыл без предмета
     */
    public static void open(Player player, Consumer<String> onPick, Runnable onClose) {
        Inventory inv = Bukkit.createInventory(null, 27, TITLE);

        // Заполняем всё кроме центрального слота (13)
        for (int i = 0; i < 27; i++) {
            if (i != 13) inv.setItem(i, GuiUtil.filler());
        }

        // Подсказка
        inv.setItem(4, GuiUtil.makeItem(Material.PAPER, "§eКак использовать",
                "§7Положи ItemsAdder предмет",
                "§7в центральный слот,",
                "§7затем закрой меню (Esc)"));

        player.openInventory(inv);

        // Слушаем закрытие
        var plugin = ru.blacksmith.v2.BlacksmithPlugin.getInstance();
        plugin.getServer().getPluginManager().registerEvents(
            new org.bukkit.event.Listener() {
                @org.bukkit.event.EventHandler
                public void onClose(InventoryCloseEvent e) {
                    if (!e.getPlayer().equals(player)) return;
                    if (!e.getView().title().equals(
                            net.kyori.adventure.text.Component.text(TITLE))) {
                        org.bukkit.event.HandlerList.unregisterAll(this);
                        return;
                    }

                    org.bukkit.event.HandlerList.unregisterAll(this);

                    ItemStack item = e.getInventory().getItem(13);
                    if (item == null || item.getType() == Material.AIR) {
                        onClose.run();
                        return;
                    }

                    CustomStack cs = CustomStack.byItemStack(item);
                    if (cs == null) {
                        player.sendMessage("§cЭтот предмет не является ItemsAdder предметом!");
                        onClose.run();
                        return;
                    }

                    onPick.accept(cs.getNamespacedID());
                }
            }, plugin
        );
    }
}
