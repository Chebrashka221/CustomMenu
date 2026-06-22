# CustomMenus

**[English](#english) | [Русский](#русский)**

---

## English

A Minecraft plugin for Paper/Purpur 1.21+ that lets you create custom shops with a full in-game GUI editor. No manual YAML editing — everything is configured through the inventory interface.

### Features

- **Multiple shops** — create any number of shops, each with its own item sets
- **Full in-game editor** — configure everything through GUI: names, prices, slots, decorations
- **ItemsAdder & Vanilla items** — items can be custom (IA) or plain vanilla Minecraft items
- **ExecutableItems support** — give items with custom enchants and abilities via EI
- **Item dependencies** — an item can only be purchased after buying another one
- **Dynamic price increase** — price grows after each purchase (by % or fixed amount), per-player or global mode
- **Purchase limits** — limit how many times an item can be bought, with optional timer reset
- **Visual layout editor** — drag-and-drop item placement inside the menu
- **Decorations** — colored glass and other blocks to decorate the interface
- **FancyNpcs integration** — bind a shop to an NPC
- **Multiple currencies** — money (Vault/Essentials), ingots (ItemsAdder), donate points (PlayerPoints)
- **Level requirement** — required level to unlock an item set (LevelCore + PlaceholderAPI)
- **Commands on purchase** — execute any console commands when a player buys an item

### Dependencies

#### Required
| Plugin | Version | Note |
|---|---|---|
| Paper / Purpur | 1.21+ | Server software |
| ItemsAdder | 3.6+ | Paid plugin |
| Vault | any | Economy bridge |
| PlaceholderAPI | 2.11+ | Placeholders |
| LevelCore | any | Player levels |

#### Optional
| Plugin | Description | Note |
|---|---|---|
| PlayerPoints | Donate points as extra currency | Free |
| ExecutableItems | Items with custom enchants & abilities | Paid |
| FancyNpcs | Bind shops to NPCs | Free |
| Essentials | Economy via Vault | Free |

> This plugin is designed to work alongside premium plugins such as **ItemsAdder** and **ExecutableItems**. Both are fully supported out of the box.

### Installation

1. Download `CustomMenus-2.0.0-shaded.jar` from [Releases](../../releases)
2. Place it in your server's `/plugins/` folder
3. Make sure all required dependencies are installed
4. Start the server — the plugin will create `plugins/CustomMenus/`
5. Configure your shop via `/cmadmin editor`

### Commands

| Command | Permission | Description |
|---|---|---|
| `/cm` | `custommenu.use` | Open the default shop |
| `/cm <shopId>` | `custommenu.use` | Open a specific shop |
| `/cmadmin editor` | `custommenu.admin` | Open the in-game editor |
| `/cmadmin list` | `custommenu.admin` | List all item sets |
| `/cmadmin give <player> <set> <item>` | `custommenu.admin` | Give an item to a player |
| `/cmadmin reset <player> <set> <item>` | `custommenu.admin` | Reset a player's purchases |

### Permissions

| Permission | Default | Description |
|---|---|---|
| `custommenu.use` | All players | Open the shop |
| `custommenu.admin` | OP | Access editor and admin commands |

### Configuration

`plugins/CustomMenus/config.yml`:

```yaml
# If true — /cm is blocked for regular players, shop opens through NPC only
npc-only: false
```

### Item delivery priority

When a purchase is made, the item is delivered in the following priority:

1. **ExecutableItems** — if `executableItemsId` is set
2. **ItemsAdder** — if `itemsAdderId` is set
3. **Vanilla** — if `material` is set

---

## Русский

Minecraft-плагин для Paper/Purpur 1.21+, который позволяет создавать кастомные магазины с полным редактором прямо в игре. Никаких правок YAML вручную — всё настраивается через интерфейс инвентаря.

### Возможности

- **Несколько магазинов** — создавай любое количество магазинов, каждый со своими сетами предметов
- **Полный in-game редактор** — всё настраивается через GUI: названия, цены, слоты, декорации
- **ItemsAdder + Ванильные предметы** — предметы могут быть как IA-кастомными, так и обычными ванильными
- **Поддержка ExecutableItems** — выдача предметов с зачарами и способностями через EI
- **Зависимости между предметами** — предмет можно купить только после покупки другого
- **Динамическое повышение цены** — цена растёт после каждой покупки (в % или фиксированной суммой), личный или глобальный режим
- **Лимит покупок** — ограничение на количество покупок с опциональным сбросом по таймеру
- **Визуальный редактор раскладки** — расстановка предметов в меню
- **Декорации** — цветные стёкла и другие блоки для украшения интерфейса
- **Интеграция с FancyNpcs** — привязка магазина к NPC
- **Несколько валют** — деньги (Vault/Essentials), слитки (ItemsAdder), донат-поинты (PlayerPoints)
- **Уровни** — требуемый уровень для открытия сета (LevelCore + PlaceholderAPI)
- **Команды при покупке** — выполнять любые консольные команды при покупке предмета

### Зависимости

#### Обязательные
| Плагин | Версия | Примечание |
|---|---|---|
| Paper / Purpur | 1.21+ | Серверное ядро |
| ItemsAdder | 3.6+ | Платный плагин |
| Vault | любая | Экономика |
| PlaceholderAPI | 2.11+ | Плейсхолдеры |
| LevelCore | любая | Уровни игроков |

#### Опциональные
| Плагин | Описание | Стоимость |
|---|---|---|
| PlayerPoints | Донат-поинты как доп. валюта | Бесплатно |
| ExecutableItems | Предметы с зачарами и способностями | Платный |
| FancyNpcs | Привязка магазина к NPC | Бесплатно |
| Essentials | Экономика через Vault | Бесплатно |

> Плагин разработан для совместной работы с платными плагинами **ItemsAdder** и **ExecutableItems**. Оба поддерживаются полностью из коробки.

### Установка

1. Скачай `CustomMenus-2.0.0-shaded.jar` из [Releases](../../releases)
2. Положи в папку `/plugins/` на сервере
3. Убедись что все обязательные зависимости установлены
4. Запусти сервер — плагин создаст папку `plugins/CustomMenus/`
5. Настрой магазин через команду `/cmadmin editor`

### Команды

| Команда | Право | Описание |
|---|---|---|
| `/cm` | `custommenu.use` | Открыть магазин по умолчанию |
| `/cm <shopId>` | `custommenu.use` | Открыть конкретный магазин |
| `/cmadmin editor` | `custommenu.admin` | Открыть in-game редактор |
| `/cmadmin list` | `custommenu.admin` | Список всех сетов |
| `/cmadmin give <игрок> <сет> <предмет>` | `custommenu.admin` | Выдать предмет игроку |
| `/cmadmin reset <игрок> <сет> <предмет>` | `custommenu.admin` | Сбросить покупки игрока |

### Права

| Право | По умолчанию | Описание |
|---|---|---|
| `custommenu.use` | Все игроки | Открытие магазина |
| `custommenu.admin` | OP | Доступ к редактору и админ-командам |

### Конфигурация

`plugins/CustomMenus/config.yml`:

```yaml
# Если true — /cm заблокирована для игроков, магазин открывается только через NPC
npc-only: false
```

### Приоритет выдачи предметов

При покупке предмет выдаётся по следующему приоритету:

1. **ExecutableItems** — если задан `executableItemsId`
2. **ItemsAdder** — если задан `itemsAdderId`
3. **Ванильный** — если задан `material`
