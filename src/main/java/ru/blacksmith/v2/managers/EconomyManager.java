package ru.blacksmith.v2.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.blacksmith.v2.BlacksmithPlugin;

public class EconomyManager {

    private Economy economy;
    private final BlacksmithPlugin plugin;

    public EconomyManager(BlacksmithPlugin plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault не найден!");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("Провайдер экономики не найден!");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Экономика: " + economy.getName());
    }

    public boolean has(Player p, double amount) {
        return economy != null && economy.has(p, amount);
    }

    public boolean take(Player p, double amount) {
        return economy != null && economy.withdrawPlayer(p, amount).transactionSuccess();
    }

    public double balance(Player p) {
        return economy == null ? 0 : economy.getBalance(p);
    }
}
