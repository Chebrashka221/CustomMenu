package ru.blacksmith.v2.managers;

import org.bukkit.configuration.file.YamlConfiguration;
import ru.blacksmith.v2.BlacksmithPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NpcBindingManager {

    private final BlacksmithPlugin plugin;
    private final File file;
    private final Map<String, String> bindings = new HashMap<>(); // npcName → shopId
    private final Map<UUID, String> pendingBinds = new HashMap<>(); // playerUUID → shopId (ожидает нажатия на NPC)

    public NpcBindingManager(BlacksmithPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "npc_bindings.yml");
    }

    public void load() {
        bindings.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        var sec = cfg.getConfigurationSection("bindings");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            String val = sec.getString(key);
            if (val != null) bindings.put(key, val);
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (var e : bindings.entrySet()) cfg.set("bindings." + e.getKey(), e.getValue());
        try { cfg.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Ошибка сохранения npc_bindings.yml: " + e.getMessage()); }
    }

    public void bind(String npcName, String shopId) {
        bindings.put(npcName, shopId);
        save();
    }

    public void unbind(String npcName) {
        bindings.remove(npcName);
        save();
    }

    public String getBinding(String npcName) { return bindings.get(npcName); }

    public Map<String, String> getAll() { return Map.copyOf(bindings); }

    // ---- Режим привязки: нажми на NPC ----

    public void setPendingBind(UUID uuid, String shopId) {
        pendingBinds.put(uuid, shopId);
    }

    public String getPendingBind(UUID uuid) {
        return pendingBinds.get(uuid);
    }

    public void clearPendingBind(UUID uuid) {
        pendingBinds.remove(uuid);
    }
}
