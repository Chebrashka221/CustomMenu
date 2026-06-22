package ru.blacksmith.v2.managers;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.entity.Player;
import ru.blacksmith.v2.BlacksmithPlugin;

public class PlayerPointsManager {

    private final PlayerPointsAPI api;
    private final boolean enabled;

    public PlayerPointsManager(BlacksmithPlugin plugin) {
        var pp = plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
        if (pp instanceof PlayerPoints playerPoints) {
            this.api     = playerPoints.getAPI();
            this.enabled = true;
            plugin.getLogger().info("PlayerPoints найден — донат-валюта включена.");
        } else {
            this.api     = null;
            this.enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    public int getPoints(Player player) {
        if (!enabled) return 0;
        return api.look(player.getUniqueId());
    }

    public boolean has(Player player, int amount) {
        if (amount <= 0) return true;
        if (!enabled) return false;
        return api.look(player.getUniqueId()) >= amount;
    }

    public boolean take(Player player, int amount) {
        if (amount <= 0) return true;
        if (!enabled) return false;
        return api.take(player.getUniqueId(), amount);
    }
}
