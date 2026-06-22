package ru.blacksmith.v2.listeners;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import ru.blacksmith.v2.BlacksmithPlugin;

public class FancyNpcListener implements Listener {

    private final BlacksmithPlugin plugin;

    public FancyNpcListener(BlacksmithPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        var entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Определяем имя NPC — пробуем несколько источников
        String npcName = resolveNpcName(entity);
        if (npcName == null || npcName.isEmpty()) return;

        // Если игрок в режиме привязки (написал /cmadmin bindnpc <shopId>) — перехватываем клик
        String pendingShopId = plugin.getNpcBindingManager().getPendingBind(player.getUniqueId());
        if (pendingShopId != null && player.hasPermission("custommenu.admin")) {
            event.setCancelled(true);
            plugin.getNpcBindingManager().clearPendingBind(player.getUniqueId());
            plugin.getNpcBindingManager().bind(npcName, pendingShopId);
            player.sendMessage("§a✔ NPC привязан! §7Захваченное имя: §e\"" + npcName + "\"");
            player.sendMessage("§aМагазин: §e"
                    + plugin.getSetManager().getShopName(pendingShopId)
                    + " §8(ID: §e" + pendingShopId + "§8)");
            player.sendMessage("§7Если имя неверное — используй §e/cmadmin listnpc §7для проверки.");
            return;
        }

        // Обычное открытие магазина
        String binding = plugin.getNpcBindingManager().getBinding(npcName);
        if (binding == null) {
            // Подсказка для администратора
            if (player.hasPermission("custommenu.admin")) {
                player.sendMessage("§7[CustomMenus] NPC §e\"" + npcName + "§e\" §7не привязан. "
                        + "Команда: §e/cmadmin bindnpc <ID> §7→ нажми на NPC");
            }
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("custommenu.use")) return;

        plugin.getServer().getScheduler().runTask(plugin, () ->
            plugin.getMenuListener().openShopMain(player, binding));
    }

    private String resolveNpcName(org.bukkit.entity.Entity entity) {
        // 1) Adventure component customName (для ArmorStand NPC и обычных entity)
        var customName = entity.customName();
        if (customName != null) {
            String name = PlainTextComponentSerializer.plainText().serialize(customName);
            if (!name.isEmpty()) return name;
        }
        // 2) Legacy string getCustomName (strip §-цвета)
        String legacyName = entity.getCustomName();
        if (legacyName != null && !legacyName.isEmpty()) {
            return legacyName.replaceAll("§[0-9a-fk-orA-FK-OR]", "").trim();
        }
        // 3) Для player-type NPC (FancyNpcs, Citizens и подобные) — getName()
        if (entity instanceof org.bukkit.entity.Player fakePlayer) {
            String name = fakePlayer.getName();
            if (name != null && !name.isEmpty()) return name;
        }
        // 4) Fallback для любого entity (работает для Villager-based NPC плагинов)
        return entity.getName();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getNpcBindingManager().clearPendingBind(event.getPlayer().getUniqueId());
    }
}
