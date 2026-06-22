package ru.blacksmith.v2.editor;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Закрывает меню, просит игрока написать в чат, вызывает callback.
 * Использование:
 *   ChatInput.prompt(plugin, player, "&eВведите название:", input -> { ... });
 */
public class ChatInput implements Listener {

    private static ChatInput instance;
    private final Map<UUID, Consumer<String>> waiting = new HashMap<>();
    private final Plugin plugin;

    private ChatInput(Plugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void init(Plugin plugin) {
        if (instance == null) instance = new ChatInput(plugin);
    }

    public static void prompt(Plugin plugin, Player player, String prompt, Consumer<String> callback) {
        instance.waiting.put(player.getUniqueId(), callback);
        player.closeInventory();
        player.sendMessage(GuiUtil.color("&8▶ &f" + prompt));
        player.sendMessage(GuiUtil.color("&7(введите &cотмена &7чтобы отменить)"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        if (!waiting.containsKey(uuid)) return;
        e.setCancelled(true);
        String input = e.getMessage().trim();
        Consumer<String> callback = waiting.remove(uuid);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!input.equalsIgnoreCase("отмена") && !input.equalsIgnoreCase("cancel")) {
                callback.accept(input);
            } else {
                e.getPlayer().sendMessage(GuiUtil.color("&cОтменено."));
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        waiting.remove(e.getPlayer().getUniqueId());
    }

    public static boolean isWaiting(UUID uuid) {
        return instance != null && instance.waiting.containsKey(uuid);
    }
}
