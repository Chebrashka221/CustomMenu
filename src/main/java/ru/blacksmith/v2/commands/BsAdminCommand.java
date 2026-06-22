package ru.blacksmith.v2.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.SetData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BsAdminCommand implements CommandExecutor, TabCompleter {

    private final BlacksmithPlugin plugin;

    public BsAdminCommand(BlacksmithPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("custommenu.admin")) {
            sender.sendMessage("§cУ вас нет прав для использования этой команды.");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "editor" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cЭта подкоманда только для игроков!");
                    return true;
                }
                plugin.getMenuListener().openEditorShopList(player);
            }

            case "editormenus" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cЭта подкоманда только для игроков!");
                    return true;
                }
                plugin.getMenuListener().openEditorShopListMenus(player);
            }

            case "list" -> {
                sender.sendMessage("§8§m══════════════════════════════════");
                sender.sendMessage("§6Список сетов CustomMenus:");
                if (plugin.getSetManager().getSets().isEmpty()) {
                    sender.sendMessage("§7Сетов пока нет. Создайте через §e/cmadmin editor");
                } else {
                    for (SetData set : plugin.getSetManager().getAllSets()) {
                        sender.sendMessage("§7• §e" + set.getId()
                                + " §8│ §f" + set.getName()
                                + " §8│ §7предметов: §e" + set.getItems().size()
                                + " §8│ §7команда: §a/cm " + set.getId());
                    }
                }
                sender.sendMessage("§8§m══════════════════════════════════");
            }

            case "give" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cНеверное использование!");
                    sender.sendMessage("§7Правильно: §e/cmadmin give <игрок> <сет> <предмет>");
                    sender.sendMessage("§7Пример:   §e/cmadmin give Steve blacksmith helmet");
                    sender.sendMessage("§7ID сетов: §e/cmadmin list");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cне найден или не в сети.");
                    return true;
                }
                if (plugin.getSetManager().getSet(args[2]) == null) {
                    sender.sendMessage("§cСет §e" + args[2] + " §cне найден. Проверьте §e/cmadmin list");
                    return true;
                }
                plugin.getDataManager().setBought(target.getUniqueId(), args[2], args[3]);
                sender.sendMessage("§a✔ Выдан статус §e" + args[2] + "." + args[3]
                        + " §aигроку §e" + target.getName());
            }

            case "reset" -> {
                if (args.length < 2) {
                    sender.sendMessage("§cНеверное использование!");
                    sender.sendMessage("§7Правильно: §e/cmadmin reset <игрок> [сет] [предмет]");
                    sender.sendMessage("§7Примеры:");
                    sender.sendMessage("§e  /cmadmin reset Steve §7— сбросить всё");
                    sender.sendMessage("§e  /cmadmin reset Steve blacksmith §7— сбросить сет");
                    sender.sendMessage("§e  /cmadmin reset Steve blacksmith helmet §7— сбросить предмет");
                    return true;
                }
                UUID uuid = resolveUUID(args[1]);
                if (uuid == null) {
                    sender.sendMessage("§cИгрок §e" + args[1] + " §cне найден.");
                    return true;
                }
                if (args.length == 2)      plugin.getDataManager().resetPlayer(uuid);
                else if (args.length == 3) plugin.getDataManager().resetPlayerSet(uuid, args[2]);
                else                       plugin.getDataManager().resetPlayerItem(uuid, args[2], args[3]);
                sender.sendMessage("§a✔ Прогресс игрока §e" + args[1] + " §aсброшен.");
            }

            case "bindnpc" -> {
                // /cmadmin bindnpc <shopId>  — затем правой кнопкой нажать на NPC
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("§cЭта подкоманда только для игроков!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: §e/cmadmin bindnpc <ID магазина>");
                    sender.sendMessage("§7Список магазинов: §e/cmadmin listshops");
                    return true;
                }
                String shopId = args[1];
                if (!plugin.getSetManager().getShopIds().contains(shopId)) {
                    sender.sendMessage("§cМагазин §e" + shopId + " §cне найден.");
                    sender.sendMessage("§7Список магазинов: §e/cmadmin listshops");
                    return true;
                }
                plugin.getNpcBindingManager().setPendingBind(player.getUniqueId(), shopId);
                player.sendMessage("§aТеперь §eправой кнопкой §aнажми на NPC которого хочешь привязать к §e"
                        + plugin.getSetManager().getShopName(shopId) + " §8(§e" + shopId + "§8)");
                player.sendMessage("§7У тебя 30 секунд. Напиши §eотмена §7чтобы отменить.");
                // Таймаут — отмена через 30 секунд
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    if (plugin.getNpcBindingManager().getPendingBind(player.getUniqueId()) != null) {
                        plugin.getNpcBindingManager().clearPendingBind(player.getUniqueId());
                        player.sendMessage("§cВремя вышло. Привязка NPC отменена.");
                    }
                }, 600L);
            }

            case "unbindnpc" -> {
                // /cmadmin unbindnpc <npcName>
                if (args.length < 2) {
                    sender.sendMessage("§cИспользование: §e/cmadmin unbindnpc <имяNPC>");
                    return true;
                }
                String npcName = args[1];
                if (plugin.getNpcBindingManager().getBinding(npcName) == null) {
                    sender.sendMessage("§cNPC §e" + npcName + " §cне был привязан.");
                    return true;
                }
                plugin.getNpcBindingManager().unbind(npcName);
                sender.sendMessage("§a✔ Привязка NPC §e" + npcName + " §aудалена.");
            }

            case "listshops" -> {
                sender.sendMessage("§8§m══════════════════════════════════");
                sender.sendMessage("§6Магазины CustomMenus:");
                var shopIds = plugin.getSetManager().getShopIds();
                if (shopIds.isEmpty()) {
                    sender.sendMessage("§7Нет магазинов. Создайте через §e/cmadmin editor");
                } else {
                    for (String sid : shopIds) {
                        int cnt = plugin.getSetManager().getSetsByShop(sid).size();
                        sender.sendMessage("§7• §e" + sid
                                + " §8│ §f" + plugin.getSetManager().getShopName(sid)
                                + " §8│ §7сетов: §e" + cnt);
                    }
                }
                sender.sendMessage("§8§m══════════════════════════════════");
            }

            case "listnpc" -> {
                var all = plugin.getNpcBindingManager().getAll();
                sender.sendMessage("§8§m══════════════════════════════════");
                sender.sendMessage("§6NPC-привязки CustomMenus:");
                if (all.isEmpty()) {
                    sender.sendMessage("§7Нет привязок. Добавьте через §e/cmadmin bindnpc");
                } else {
                    all.forEach((npc, target) ->
                        sender.sendMessage("§7• §e" + npc + " §8→ §f" + target));
                }
                sender.sendMessage("§8§m══════════════════════════════════");
            }

            case "help" -> sendHelp(sender);

            default -> {
                sender.sendMessage("§cНеизвестная подкоманда: §e" + args[0]);
                sender.sendMessage("§7Используйте §e/cmadmin help §7для списка команд.");
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("custommenu.admin")) return List.of();

        List<String> result = new ArrayList<>();
        String prefix = args.length > 0 ? args[args.length - 1].toLowerCase() : "";

        if (args.length == 1) {
            return filterPrefix(List.of("editor", "editormenus", "list", "listshops", "give", "reset",
                    "bindnpc", "unbindnpc", "listnpc", "help"), prefix);
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("bindnpc") && args.length == 2) {
            return filterPrefix(plugin.getSetManager().getShopIds(), prefix);
        }

        if (sub.equals("unbindnpc") && args.length == 2) {
            return filterPrefix(new ArrayList<>(plugin.getNpcBindingManager().getAll().keySet()), prefix);
        }

        if (sub.equals("give") || sub.equals("reset")) {
            if (args.length == 2) {
                // Список онлайн-игроков
                Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(n -> n.toLowerCase().startsWith(prefix))
                        .forEach(result::add);
                return result;
            }
            if (args.length == 3) {
                // Список сетов
                return filterPrefix(
                        plugin.getSetManager().getAllSets().stream()
                                .map(SetData::getId).toList(), prefix);
            }
            if (args.length == 4 && sub.equals("give")) {
                // Предметы выбранного сета
                SetData set = plugin.getSetManager().getSet(args[2]);
                if (set != null) {
                    return filterPrefix(new ArrayList<>(set.getItems().keySet()), prefix);
                }
            }
            if (args.length == 4 && sub.equals("reset")) {
                SetData set = plugin.getSetManager().getSet(args[2]);
                if (set != null) {
                    return filterPrefix(new ArrayList<>(set.getItems().keySet()), prefix);
                }
            }
        }

        return result;
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        return list.stream().filter(s -> s.toLowerCase().startsWith(prefix)).toList();
    }

    private UUID resolveUUID(String name) {
        Player p = Bukkit.getPlayer(name);
        if (p != null) return p.getUniqueId();
        var off = Bukkit.getOfflinePlayer(name);
        return off.hasPlayedBefore() ? off.getUniqueId() : null;
    }

    private void sendHelp(CommandSender s) {
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§6CustomMenus §7— Команды администратора");
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§e/cmadmin editor");
        s.sendMessage("§7  Открыть in-game редактор сетов и предметов");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin editormenus");
        s.sendMessage("§7  Редактировать внешний вид меню (расположение предметов/сетов)");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin list");
        s.sendMessage("§7  Показать все сеты, их ID и количество предметов");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin give §b<игрок> <сет> <предмет>");
        s.sendMessage("§7  Выдать игроку статус «куплено» для предмета");
        s.sendMessage("§7  Пример: §e/cmadmin give Steve blacksmith helmet");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin reset §b<игрок> §7[сет] [предмет]");
        s.sendMessage("§7  Сбросить прогресс. Без аргументов — сбросить всё.");
        s.sendMessage("§7  Пример: §e/cmadmin reset Steve");
        s.sendMessage("§7  Пример: §e/cmadmin reset Steve blacksmith helmet");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin bindnpc §b<ID магазина>");
        s.sendMessage("§7  Затем правой кнопкой нажми на нужного NPC.");
        s.sendMessage("§7  Пример: §e/cmadmin bindnpc blacksmith §7→ нажать на NPC");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin unbindnpc §b<имяNPC>");
        s.sendMessage("§7  Удалить привязку NPC");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin listnpc");
        s.sendMessage("§7  Показать все привязки NPC → меню");
        s.sendMessage("");
        s.sendMessage("§e/cmadmin help §7— показать эту справку");
        s.sendMessage("§8§m══════════════════════════════════");
        s.sendMessage("§7Открыть магазин игроку: §e/cm §7или §e/cm <ID сета>");
        s.sendMessage("§8§m══════════════════════════════════");
    }
}
