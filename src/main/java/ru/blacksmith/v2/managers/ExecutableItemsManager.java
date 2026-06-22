package ru.blacksmith.v2.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.blacksmith.v2.BlacksmithPlugin;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Интеграция с ExecutableItems (SSStudio) через reflection.
 * Не требует compile-time зависимости — работает с любой версией EI.
 */
public class ExecutableItemsManager {

    private final boolean enabled;
    private final BlacksmithPlugin plugin;

    public ExecutableItemsManager(BlacksmithPlugin plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getServer().getPluginManager().getPlugin("ExecutableItems") != null;
        if (enabled) plugin.getLogger().info("[CustomMenus] ExecutableItems найден — интеграция активна.");
    }

    public boolean isEnabled() { return enabled; }

    /**
     * Выдать ExecutableItems-предмет игроку.
     * @return true если выдан успешно, false если EI не установлен или ID не найден
     */
    public boolean give(Player player, String itemId, int amount) {
        ItemStack stack = buildStack(player, itemId, amount);
        if (stack == null) return false;
        player.getInventory().addItem(stack);
        return true;
    }

    /**
     * Получить иконку EI-предмета для отображения в GUI (без выдачи игроку).
     * @return ItemStack с иконкой или null если не найден
     */
    public ItemStack getIcon(String itemId) {
        return buildStack(null, itemId, 1);
    }

    /**
     * Получить ID ExecutableItems-предмета из руки игрока.
     * Стратегии (в порядке приоритета):
     *  1. PersistentDataContainer (прямое чтение — надёжнее всего)
     *  2. EI API через reflection (для версий, где PDC ключ отличается)
     * @return ID предмета или null если в руке не EI-предмет
     */
    public String getIdInHand(Player player) {
        if (!enabled) return null;
        org.bukkit.inventory.ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType() == org.bukkit.Material.AIR || !held.hasItemMeta()) return null;

        // Стратегия 1: читаем из PersistentDataContainer напрямую
        String fromPdc = readEiIdFromPdc(held);
        if (fromPdc != null) return fromPdc;

        // Стратегия 2: EI API через reflection
        return readEiIdViaApi(held);
    }

    private String readEiIdFromPdc(org.bukkit.inventory.ItemStack item) {
        var meta = item.getItemMeta();
        if (meta == null) return null;
        var pdc = meta.getPersistentDataContainer();
        // Перебираем известные namespace:key комбинации EI разных версий
        String[][] keys = {
            {"ssomar", "id"}, {"ssomar", "item_id"}, {"ssomar", "ei_id"},
            {"executableitems", "id"}, {"executableitems", "item_id"},
            {"ei", "id"}, {"score", "id"}
        };
        for (String[] pair : keys) {
            try {
                var nk = new org.bukkit.NamespacedKey(pair[0], pair[1]);
                if (pdc.has(nk, org.bukkit.persistence.PersistentDataType.STRING)) {
                    String val = pdc.get(nk, org.bukkit.persistence.PersistentDataType.STRING);
                    if (val != null && !val.isBlank()) return val;
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String readEiIdViaApi(org.bukkit.inventory.ItemStack held) {
        try {
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            Method getManager = apiClass.getMethod("getExecutableItemsManager");
            Object manager = getManager.invoke(null);

            // Перебираем методы менеджера, принимающие ItemStack и возвращающие Optional
            for (Method m : manager.getClass().getMethods()) {
                if (m.getParameterCount() != 1) continue;
                if (!m.getReturnType().equals(Optional.class) && !m.getReturnType().getSimpleName().equals("Optional")) continue;
                Class<?> paramType = m.getParameterTypes()[0];
                if (!paramType.isAssignableFrom(held.getClass())) continue;

                try {
                    Object result = m.invoke(manager, held);
                    if (!(result instanceof Optional<?> opt) || opt.isEmpty()) continue;
                    Object val = opt.get();

                    if (val instanceof String s && !s.isBlank()) return s;

                    // Optional<EI-объект> → getId()
                    for (Method gid : val.getClass().getMethods()) {
                        if (gid.getName().equals("getId") && gid.getParameterCount() == 0) {
                            Object id = gid.invoke(val);
                            if (id instanceof String s && !s.isBlank()) return s;
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            plugin.getLogger().warning("[CustomMenus] EI reflection: " + e.getMessage());
        }
        return null;
    }

    // ---- Internal ----

    private ItemStack buildStack(Player player, String itemId, int amount) {
        if (!enabled || itemId == null || itemId.isEmpty()) return null;
        try {
            // com.ssomar.score.api.executableitems.ExecutableItemsAPI.getExecutableItemsManager()
            Class<?> apiClass = Class.forName("com.ssomar.score.api.executableitems.ExecutableItemsAPI");
            Method getManager = apiClass.getMethod("getExecutableItemsManager");
            Object manager = getManager.invoke(null);

            // manager.getExecutableItem(String id) → Optional<?>
            Method getItem = manager.getClass().getMethod("getExecutableItem", String.class);
            @SuppressWarnings("unchecked")
            Optional<Object> optItem = (Optional<Object>) getItem.invoke(manager, itemId);
            if (optItem == null || optItem.isEmpty()) return null;

            Object eiObject = optItem.get();

            // Ищем метод buildItem(int amount, Optional<Player>)
            Method buildItem = findBuildItemMethod(eiObject.getClass());
            if (buildItem == null) return null;

            Optional<Player> playerOpt = player != null ? Optional.of(player) : Optional.empty();
            Object result = buildItem.invoke(eiObject, amount, playerOpt);
            if (result instanceof ItemStack is) return is;
            return null;
        } catch (ClassNotFoundException e) {
            // EI не установлен или другая структура классов
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("[CustomMenus] Ошибка EI API для «" + itemId + "»: " + e.getMessage());
            return null;
        }
    }

    private Method findBuildItemMethod(Class<?> cls) {
        // Ищем buildItem с 2 параметрами (int, Optional) по всей иерархии классов
        for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals("buildItem") && m.getParameterCount() == 2) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        // Fallback: ищем в интерфейсах
        for (Class<?> iface : cls.getInterfaces()) {
            for (Method m : iface.getMethods()) {
                if (m.getName().equals("buildItem") && m.getParameterCount() == 2) {
                    return m;
                }
            }
        }
        return null;
    }
}
