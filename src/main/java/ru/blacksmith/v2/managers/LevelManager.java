package ru.blacksmith.v2.managers;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LevelManager {

    private Boolean hasPapi = null;

    public int getLevel(Player player) {
        if (isPapi()) {
            try {
                return Integer.parseInt(PlaceholderAPI.setPlaceholders(player, "%lc_level%").trim());
            } catch (Exception ignored) {}
        }
        return player.getLevel();
    }

    private boolean isPapi() {
        if (hasPapi == null) hasPapi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        return hasPapi;
    }
}
