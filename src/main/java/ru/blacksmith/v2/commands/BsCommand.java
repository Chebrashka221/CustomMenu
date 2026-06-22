package ru.blacksmith.v2.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.blacksmith.v2.BlacksmithPlugin;

import java.util.List;

public class BsCommand implements CommandExecutor, TabCompleter {

    private final BlacksmithPlugin plugin;

    public BsCommand(BlacksmithPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cЭта команда только для игроков!");
            return true;
        }
        if (!player.hasPermission("custommenu.use")) {
            player.sendMessage("§cУ вас нет прав для использования этой команды.");
            return true;
        }
        if (plugin.getConfig().getBoolean("npc-only", false)
                && !player.hasPermission("custommenu.admin")) {
            player.sendMessage("§cМагазин доступен только через NPC.");
            return true;
        }

        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("help")) {
                sendHelp(player);
                return true;
            }
            // /cm <shopId> — открыть конкретный магазин
            String shopId = args[0].toLowerCase();
            if (!plugin.getSetManager().getShopIds().contains(shopId)) {
                player.sendMessage("§cМагазин §e" + shopId + " §cне найден.");
                return true;
            }
            plugin.getMenuListener().openShopMain(player, shopId);
        } else {
            List<String> shops = plugin.getSetManager().getShopIds();
            if (shops.isEmpty()) {
                player.sendMessage("§cМагазинов пока нет.");
                return true;
            }
            if (shops.size() == 1) {
                plugin.getMenuListener().openShopMain(player, shops.get(0));
            } else {
                plugin.getMenuListener().openShopSelector(player);
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            List<String> options = new java.util.ArrayList<>();
            options.add("help");
            plugin.getSetManager().getShopIds().stream()
                    .filter(id -> id.startsWith(prefix))
                    .forEach(options::add);
            return options.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§6CustomMenus §7— Команды магазина");
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§e/cm §7— открыть главное меню магазина");
        s.sendMessage("§e/cm <ID сета> §7— открыть конкретный сет напрямую");
        s.sendMessage("§e/cm help §7— показать эту справку");
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§7Список доступных сетов: §e/cmadmin list");
        s.sendMessage("§8§m══════════════════════════════════");
    }
}
