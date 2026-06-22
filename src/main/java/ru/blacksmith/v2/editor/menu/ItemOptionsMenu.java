package ru.blacksmith.v2.editor.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;
import ru.blacksmith.v2.data.ItemData;
import ru.blacksmith.v2.data.SetData;
import ru.blacksmith.v2.editor.ChatInput;
import ru.blacksmith.v2.editor.GuiUtil;
import ru.blacksmith.v2.managers.IngotManager;

import java.util.Arrays;

public class ItemOptionsMenu {

    private final BlacksmithPlugin plugin;
    private final SetData set;
    private final ItemData item;

    public ItemOptionsMenu(BlacksmithPlugin plugin, SetData set, ItemData item) {
        this.plugin = plugin;
        this.set = set;
        this.item = item;
    }

    public void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 27,
                "§8Предмет: §f" + set.getId() + "§8.§f" + item.getId());
        for (int i = 0; i < 27; i++) inv.setItem(i, GuiUtil.filler());

        // [0] Название
        inv.setItem(0, GuiUtil.makeItem(Material.NAME_TAG, "&fНазвание",
                "&7Текущее: §r" + GuiUtil.color(item.getName()),
                "", "&eНажмите чтобы изменить"));

        // [1] Предмет — читается из руки
        ItemStack icon = IngotManager.getIcon(item.getItemsAdderId());
        if (icon == null) icon = new ItemStack(Material.PAPER);
        setMeta(icon, "&fПредмет (иконка + выдаётся игроку)",
                "&7IA ID: &f" + nvl(item.getItemsAdderId()),
                "",
                "&7Возьмите IA предмет в руку,",
                "&eНажмите &7— назначить из руки");
        inv.setItem(1, icon);

        // [2] Цена
        inv.setItem(2, GuiUtil.makeItem(Material.GOLD_INGOT, "&fЦена",
                "&7Текущая: &e" + (int) item.getPrice() + "$",
                "", "&eНажмите чтобы изменить"));

        // [3] Слитки
        inv.setItem(3, GuiUtil.makeItem(Material.BRICK, "&fКол-во слитков",
                "&7Текущее: &e" + item.getIngots(),
                "", "&eЛКМ &7— +1",
                "&cПКМ &7— -1",
                "&6СКМ &7— ввести вручную"));

        // [4] Слот в GUI
        inv.setItem(4, GuiUtil.makeItem(Material.COMPASS, "&fСлот в GUI",
                "&7Текущий: &e" + item.getSlot(),
                "", "&eНажмите чтобы изменить"));

        // [5] Requires-prev
        String prevStr = item.hasRequirement() ? item.getRequiresPrev() : "§7нет";
        inv.setItem(5, GuiUtil.makeItem(Material.CHAIN, "&fЗависимость (требует предмет)",
                "&7Текущая: &f" + prevStr,
                "",
                "&eНажмите &7— выбрать из списка предметов"));

        // [6] Лимит покупок
        String maxBuysStr = item.getMaxBuys() == 0 ? "§aБезлимит"
                : (item.getMaxBuys() == 1 ? "§e1 раз (по умолчанию)" : "§e" + item.getMaxBuys() + " раз");
        inv.setItem(6, GuiUtil.makeItem(Material.GOLD_NUGGET, "&fЛимит покупок",
                "&7Текущий: " + maxBuysStr,
                "",
                "&eЛКМ &7— +1",
                "&cПКМ &7— -1",
                "&6СКМ &7— ввести вручную",
                "&7(0 = безлимит)"));

        // [7] Открыть визуальный редактор раскладки
        inv.setItem(7, GuiUtil.makeItem(Material.MAP, "&fРедактор раскладки меню",
                "&7Открыть меню сета как оно",
                "&7будет выглядеть для игрока",
                "", "&eНажмите чтобы открыть"));

        // [8] Назад
        inv.setItem(8, GuiUtil.back());

        // [9] Сброс лимита
        String resetStr = item.getResetHours() == 0 ? "§7Отключён"
                : "§eКаждые " + formatResetHours(item.getResetHours());
        inv.setItem(9, GuiUtil.makeItem(Material.CLOCK, "&fСброс лимита",
                "&7Текущий: " + resetStr,
                "",
                "&eЛКМ &7— +1 час",
                "&cПКМ &7— -1 час",
                "&6СКМ &7— ввести вручную",
                "&7(0 = никогда)"));

        // [12] Донат-поинты (PlayerPoints)
        boolean ppEnabled = plugin.getPlayerPointsManager().isEnabled();
        String ppStatus = !ppEnabled ? "§cPlayerPoints не установлен"
                : item.getPointsPrice() > 0 ? "§e" + item.getPointsPrice() + " поинтов" : "§7не требуются";
        inv.setItem(12, GuiUtil.makeItem(Material.NETHER_STAR, "&fДонат-поинты (PlayerPoints)",
                "&7Текущая цена: " + ppStatus,
                "",
                "&eЛКМ &7— ввести количество поинтов",
                "&cПКМ &7— убрать (0 поинтов)",
                "&7(0 = поинты не нужны)"));

        // [13] Повышение цены за покупку
        boolean isFixed13 = "fixed".equals(item.getPriceIncreaseType());
        String increaseStatus;
        if (isFixed13) {
            increaseStatus = item.getPriceIncreaseFixed() > 0
                    ? "§e+" + (int) item.getPriceIncreaseFixed() + "$ §7за каждую покупку"
                    : "§7отключено";
        } else {
            increaseStatus = item.getPriceIncreasePercent() > 0
                    ? "§e+" + item.getPriceIncreasePercent() + "% §7за каждую покупку"
                    : "§7отключено";
        }
        inv.setItem(13, GuiUtil.makeItem(Material.ARROW, "&fПрирост цены",
                "&7Статус: " + increaseStatus,
                "&7Тип: " + (isFixed13 ? "&aФИКС. СУММА" : "&eПРОЦЕНТ"),
                "",
                isFixed13
                    ? "&7Цена увеличивается на N$ после каждой покупки."
                    : "&7Цена умножается на (1 + %) после каждой покупки.",
                isFixed13
                    ? "&7Пример: 100$, +50$ → 150$ → 200$ → ..."
                    : "&7Пример: 100$, +10% → 110$ → 121$ → ...",
                "",
                "&eЛКМ &7— +1",
                "&cПКМ &7— -1",
                "&6СКМ &7— ввести вручную",
                "&7(0 = отключить)"));

        // [17] Тип прироста цены (процент / фиксированная сумма)
        inv.setItem(17, GuiUtil.makeItem(
                isFixed13 ? Material.EMERALD : Material.GOLD_INGOT,
                "&fТип прироста цены",
                isFixed13 ? "&aФИКСИРОВАННАЯ СУММА" : "&eПРОЦЕНТ",
                "",
                isFixed13 ? "&7Цена растёт на N$ за покупку" : "&7Цена умножается на (1 + %)% за покупку",
                "",
                "&eЛКМ &7— переключить тип"));

        // [14] Режим повышения цены (player / global)
        boolean isGlobal = "global".equals(item.getPriceIncreaseMode());
        inv.setItem(14, GuiUtil.makeItem(
                isGlobal ? Material.ENDER_PEARL : Material.PLAYER_HEAD,
                "&fРежим прироста: " + (isGlobal ? "&aГЛОБАЛЬНЫЙ" : "&eИГРОКА"),
                isGlobal
                        ? "&7Цена растёт для ВСЕХ после каждой покупки"
                        : "&7Цена растёт только у этого игрока",
                "",
                "&eЛКМ &7— переключить режим",
                "",
                isGlobal
                        ? "&8Сейчас: глобальный — 1 покупка любого игрока = +%"
                        : "&8Сейчас: личный — у каждого игрока своя цена"));

        // [15] ExecutableItems предмет
        boolean eiEnabled = plugin.getExecutableItemsManager().isEnabled();
        String eiStatus = !eiEnabled
                ? "§cExecutableItems не установлен"
                : item.hasExecutableItem() ? "§e" + item.getExecutableItemsId() : "§7не задан";
        inv.setItem(15, GuiUtil.makeItem(Material.TOTEM_OF_UNDYING, "&fExecutableItems предмет",
                "&7ID: " + eiStatus,
                "",
                "&7Предмет будет выдан через EI со всеми",
                "&7зачарами, эффектами и способностями.",
                "&7Приоритет: &eEI &7> ItemsAdder &7> Ванильный",
                "",
                "&eЛКМ &7— держите EI-предмет в руке = автодетект",
                "&eЛКМ &7— без предмета = ввести ID вручную",
                "&cПКМ &7— сбросить (убрать EI)"));

        // [10] Ванильный материал
        String matStr = item.hasVanillaItem()
                ? "§e" + item.getMaterial() + " §7x" + item.getAmount()
                : "§7не задан";
        inv.setItem(10, GuiUtil.makeItem(Material.BOOK, "&fВанильный предмет",
                "&7Материал: " + matStr,
                "&7(альтернатива ItemsAdder предмету)",
                "",
                "&eЛКМ &7— ввести материал (пример: BOOK, ENCHANTED_BOOK)",
                "&cПКМ &7— сбросить",
                "&6СКМ &7— изменить количество"));

        // [16] Описание товара
        var desc = item.getDescription();
        var descLore = new java.util.ArrayList<String>();
        descLore.add("&7Описание отображается в лоре товара.");
        descLore.add("&7Строк: &e" + desc.size());
        for (int i = 0; i < Math.min(desc.size(), 5); i++)
            descLore.add("  &8" + (i + 1) + ". &7" + desc.get(i));
        if (desc.size() > 5) descLore.add("  &8... и ещё " + (desc.size() - 5));
        descLore.add("");
        descLore.add("&eЛКМ &7— добавить строку (&-цвета поддерживаются)");
        descLore.add("&cПКМ &7— удалить последнюю строку");
        descLore.add("&4СКМ &7— очистить всё");
        inv.setItem(16, GuiUtil.makeItem(Material.WRITABLE_BOOK, "&fОписание товара",
                descLore.stream().map(GuiUtil::color).toArray(String[]::new)));

        // [11] Команды при покупке
        var cmds = item.getCommands();
        var cmdLore = new java.util.ArrayList<String>();
        cmdLore.add("&7Команды (от консоли), {player} = ник");
        cmdLore.add("&7Текущих: &e" + cmds.size());
        for (int i = 0; i < Math.min(cmds.size(), 5); i++)
            cmdLore.add("  &8" + (i + 1) + ". &7" + cmds.get(i));
        if (cmds.size() > 5) cmdLore.add("  &8... и ещё " + (cmds.size() - 5));
        cmdLore.add("");
        cmdLore.add("&eЛКМ &7— добавить команду");
        cmdLore.add("&cПКМ &7— удалить последнюю");
        cmdLore.add("&4СКМ &7— очистить все");
        inv.setItem(11, GuiUtil.makeItem(Material.COMMAND_BLOCK, "&fКоманды при покупке",
                cmdLore.stream().map(GuiUtil::color).toArray(String[]::new)));

        player.openInventory(inv);
    }

    public void handleClick(Player player, int slot, boolean right, boolean middle) {
        switch (slot) {
            case 0 -> ChatInput.prompt(plugin, player, "Введите название предмета:", input -> {
                item.setName(input);
                save(player, "Название: &f" + input);
            });

            case 1 -> {
                String id = IngotManager.getIdInHand(player);
                if (id == null) {
                    player.sendMessage(GuiUtil.color("&cВозьмите ItemsAdder предмет в руку и нажмите снова!"));
                    return;
                }
                item.setItemsAdderId(id);
                save(player, "Предмет: &e" + id);
            }

            case 2 -> ChatInput.prompt(plugin, player, "Введите цену (число):", input -> {
                try {
                    item.setPrice(Double.parseDouble(input));
                    save(player, "Цена: &e" + input + "$");
                } catch (NumberFormatException e) {
                    player.sendMessage(GuiUtil.color("&cНужно число!"));
                    plugin.getMenuListener().openEditorItem(player, set, item);
                }
            });

            case 3 -> {
                if (middle) {
                    ChatInput.prompt(plugin, player, "Введите кол-во слитков:", input -> {
                        try {
                            item.setIngots(Math.max(0, Integer.parseInt(input)));
                            save(player, "Слитков: &e" + item.getIngots());
                        } catch (NumberFormatException e) {
                            player.sendMessage(GuiUtil.color("&cНужно число!"));
                            plugin.getMenuListener().openEditorItem(player, set, item);
                        }
                    });
                } else {
                    item.setIngots(Math.max(0, item.getIngots() + (right ? -1 : 1)));
                    plugin.getSetManager().saveSet(set);
                    plugin.getMenuListener().openEditorItem(player, set, item);
                }
            }

            case 4 -> ChatInput.prompt(plugin, player, "Введите слот в GUI (число):", input -> {
                try {
                    item.setSlot(Integer.parseInt(input));
                    save(player, "Слот: &e" + input);
                } catch (NumberFormatException e) {
                    player.sendMessage(GuiUtil.color("&cНужно число!"));
                    plugin.getMenuListener().openEditorItem(player, set, item);
                }
            });

            case 5 -> plugin.getMenuListener().openRequirementPicker(player, set, item);

            case 6 -> {
                if (middle) {
                    ChatInput.prompt(plugin, player, "Введите лимит покупок (0 = безлимит):", input -> {
                        try {
                            item.setMaxBuys(Math.max(0, Integer.parseInt(input)));
                            save(player, "Лимит: " + (item.getMaxBuys() == 0 ? "безлимит" : item.getMaxBuys() + " раз"));
                        } catch (NumberFormatException e) {
                            player.sendMessage(GuiUtil.color("&cНужно число!"));
                            plugin.getMenuListener().openEditorItem(player, set, item);
                        }
                    });
                } else {
                    item.setMaxBuys(Math.max(0, item.getMaxBuys() + (right ? -1 : 1)));
                    plugin.getSetManager().saveSet(set);
                    plugin.getMenuListener().openEditorItem(player, set, item);
                }
            }

            case 7 -> plugin.getMenuListener().openLayoutEditor(player, set);

            case 8 -> plugin.getMenuListener().openEditorSet(player, set);

            case 10 -> {
                if (middle) {
                    ChatInput.prompt(plugin, player, "Введите количество (число):", input -> {
                        try {
                            item.setAmount(Integer.parseInt(input));
                            save(player, "Количество: &e" + item.getAmount());
                        } catch (NumberFormatException e) {
                            player.sendMessage(GuiUtil.color("&cНужно число!"));
                            plugin.getMenuListener().openEditorItem(player, set, item);
                        }
                    });
                } else if (right) {
                    item.setMaterial(null);
                    save(player, "Ванильный предмет сброшен.");
                } else {
                    ChatInput.prompt(plugin, player,
                            "Введите материал (пример: BOOK, ENCHANTED_BOOK, POTION):", input -> {
                        item.setMaterial(input.trim());
                        save(player, "Материал: &e" + item.getMaterial());
                    });
                }
            }

            case 12 -> {
                if (right) {
                    item.setPointsPrice(0);
                    save(player, "Донат-поинты сброшены.");
                } else {
                    ChatInput.prompt(plugin, player, "Введите количество донат-поинтов (0 = не нужны):", input -> {
                        try {
                            item.setPointsPrice(Integer.parseInt(input));
                            save(player, "Донат-поинты: &e" + item.getPointsPrice());
                        } catch (NumberFormatException e) {
                            player.sendMessage(GuiUtil.color("&cНужно число!"));
                            plugin.getMenuListener().openEditorItem(player, set, item);
                        }
                    });
                }
            }

            case 13 -> {
                boolean isFixed = "fixed".equals(item.getPriceIncreaseType());
                if (middle) {
                    if (isFixed) {
                        ChatInput.prompt(plugin, player, "Введите фикс. прирост в $ (0 = отключить):", input -> {
                            try {
                                item.setPriceIncreaseFixed(Math.max(0, Double.parseDouble(input)));
                                save(player, "Прирост цены: &e+" + (int) item.getPriceIncreaseFixed() + "$");
                            } catch (NumberFormatException e) {
                                player.sendMessage(GuiUtil.color("&cНужно число!"));
                                plugin.getMenuListener().openEditorItem(player, set, item);
                            }
                        });
                    } else {
                        ChatInput.prompt(plugin, player, "Введите прирост цены в % (0 = отключить):", input -> {
                            try {
                                item.setPriceIncreasePercent(Math.max(0, Integer.parseInt(input)));
                                save(player, "Прирост цены: &e+" + item.getPriceIncreasePercent() + "%");
                            } catch (NumberFormatException e) {
                                player.sendMessage(GuiUtil.color("&cНужно целое число!"));
                                plugin.getMenuListener().openEditorItem(player, set, item);
                            }
                        });
                    }
                } else {
                    if (isFixed) {
                        item.setPriceIncreaseFixed(Math.max(0, item.getPriceIncreaseFixed() + (right ? -1 : 1)));
                    } else {
                        item.setPriceIncreasePercent(Math.max(0, item.getPriceIncreasePercent() + (right ? -1 : 1)));
                    }
                    plugin.getSetManager().saveSet(set);
                    plugin.getMenuListener().openEditorItem(player, set, item);
                }
            }

            case 17 -> {
                item.setPriceIncreaseType("fixed".equals(item.getPriceIncreaseType()) ? "percent" : "fixed");
                save(player, "Тип прироста: &e" + ("fixed".equals(item.getPriceIncreaseType()) ? "Фиксированная сумма" : "Процент"));
            }

            case 14 -> {
                // Переключить player ↔ global
                item.setPriceIncreaseMode("global".equals(item.getPriceIncreaseMode()) ? "player" : "global");
                save(player, "Режим: &e" + ("global".equals(item.getPriceIncreaseMode()) ? "Глобальный" : "Игрока"));
            }

            case 15 -> {
                if (right) {
                    item.setExecutableItemsId(null);
                    save(player, "ExecutableItems ID сброшен.");
                } else {
                    // ЛКМ: сначала пробуем взять из руки, если не EI-предмет — открываем чат
                    String eiId = plugin.getExecutableItemsManager().getIdInHand(player);
                    if (eiId != null) {
                        item.setExecutableItemsId(eiId);
                        save(player, "EI предмет: &e" + eiId);
                    } else {
                        ChatInput.prompt(plugin, player,
                                "Введите ID ExecutableItems предмета (имя файла без .yml):", input -> {
                            item.setExecutableItemsId(input.trim());
                            save(player, "EI предмет: &e" + item.getExecutableItemsId());
                        });
                    }
                }
            }

            case 16 -> {
                if (middle) {
                    item.clearDescription();
                    save(player, "Описание очищено.");
                } else if (right) {
                    item.removeLastDescriptionLine();
                    save(player, "Последняя строка описания удалена.");
                } else {
                    ChatInput.prompt(plugin, player,
                            "Введите строку описания (&-цвета поддерживаются):", input -> {
                        item.addDescriptionLine(input);
                        save(player, "Строка добавлена: &r" + input);
                    });
                }
            }

            case 11 -> {
                if (middle) {
                    item.clearCommands();
                    save(player, "Команды очищены.");
                } else if (right) {
                    item.removeLastCommand();
                    save(player, "Последняя команда удалена.");
                } else {
                    ChatInput.prompt(plugin, player,
                            "Введите команду (без /, {player} = ник игрока):", input -> {
                        item.addCommand(input.trim());
                        save(player, "Команда добавлена: &f" + input.trim());
                    });
                }
            }

            case 9 -> {
                if (middle) {
                    ChatInput.prompt(plugin, player, "Введите кол-во часов до сброса (0 = никогда):", input -> {
                        try {
                            item.setResetHours(Math.max(0, Integer.parseInt(input)));
                            save(player, "Сброс: " + (item.getResetHours() == 0 ? "отключён" : "каждые " + item.getResetHours() + " ч."));
                        } catch (NumberFormatException e) {
                            player.sendMessage(GuiUtil.color("&cНужно число!"));
                            plugin.getMenuListener().openEditorItem(player, set, item);
                        }
                    });
                } else {
                    item.setResetHours(Math.max(0, item.getResetHours() + (right ? -1 : 1)));
                    plugin.getSetManager().saveSet(set);
                    plugin.getMenuListener().openEditorItem(player, set, item);
                }
            }
        }
    }

    private void save(Player player, String msg) {
        plugin.getSetManager().saveSet(set);
        player.sendMessage(GuiUtil.color("&a" + msg));
        plugin.getMenuListener().openEditorItem(player, set, item);
    }

    private void setMeta(ItemStack item, String name, String... lore) {
        var meta = item.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(GuiUtil.color(name));
        meta.setLore(Arrays.stream(lore).map(GuiUtil::color).toList());
        item.setItemMeta(meta);
    }

    private String nvl(String s) { return s == null || s.isEmpty() ? "§7не задан" : s; }

    private static String formatResetHours(int hours) {
        if (hours % 24 == 0) return (hours / 24) + " дн.";
        return hours + " ч.";
    }
}
