package ru.blacksmith.v2.managers;

import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class IngotManager {

    public static int count(Player player, String iaId) {
        if (iaId == null || iaId.isEmpty()) return 0;
        int total = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            CustomStack cs = CustomStack.byItemStack(item);
            if (cs != null && cs.getNamespacedID().equals(iaId)) total += item.getAmount();
        }
        return total;
    }

    public static boolean remove(Player player, String iaId, int amount) {
        if (count(player, iaId) < amount) return false;
        int toRemove = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && toRemove > 0; i++) {
            if (contents[i] == null) continue;
            CustomStack cs = CustomStack.byItemStack(contents[i]);
            if (cs == null || !cs.getNamespacedID().equals(iaId)) continue;
            if (contents[i].getAmount() <= toRemove) {
                toRemove -= contents[i].getAmount();
                contents[i] = null;
            } else {
                contents[i].setAmount(contents[i].getAmount() - toRemove);
                toRemove = 0;
            }
        }
        player.getInventory().setContents(contents);
        return true;
    }

    public static void give(Player player, String iaId) {
        CustomStack cs = CustomStack.getInstance(iaId);
        if (cs != null) player.getInventory().addItem(cs.getItemStack());
    }

    /** ItemsAdder ID предмета в руке, null если не IA-предмет */
    public static String getIdInHand(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return null;
        CustomStack cs = CustomStack.byItemStack(item);
        return cs != null ? cs.getNamespacedID() : null;
    }

    /** Иконка предмета по IA ID, null если не найден */
    public static ItemStack getIcon(String iaId) {
        if (iaId == null || iaId.isEmpty()) return null;
        CustomStack cs = CustomStack.getInstance(iaId);
        return cs != null ? cs.getItemStack().clone() : null;
    }
}
