package ru.blacksmith.v2;

import org.bukkit.plugin.java.JavaPlugin;
import ru.blacksmith.v2.commands.BsAdminCommand;
import ru.blacksmith.v2.commands.BsCommand;
import ru.blacksmith.v2.editor.ChatInput;
import ru.blacksmith.v2.listeners.FancyNpcListener;
import ru.blacksmith.v2.listeners.MenuListener;
import ru.blacksmith.v2.managers.*;

public class BlacksmithPlugin extends JavaPlugin {

    private static BlacksmithPlugin instance;

    private SetManager setManager;
    private DataManager dataManager;
    private EconomyManager economyManager;
    private LevelManager levelManager;
    private MenuListener menuListener;
    private NpcBindingManager npcBindingManager;
    private PlayerPointsManager playerPointsManager;
    private ExecutableItemsManager executableItemsManager;

    @Override
    public void onEnable() {
        instance = this;

        getDataFolder().mkdirs();
        saveDefaultConfig();

        this.setManager           = new SetManager(this);
        this.dataManager          = new DataManager(this);
        this.economyManager       = new EconomyManager(this);
        this.levelManager         = new LevelManager();
        this.menuListener         = new MenuListener(this);
        this.npcBindingManager       = new NpcBindingManager(this);
        this.playerPointsManager     = new PlayerPointsManager(this);
        this.executableItemsManager  = new ExecutableItemsManager(this);

        setManager.load();
        dataManager.load();
        npcBindingManager.load();

        ChatInput.init(this);

        BsCommand cmCmd = new BsCommand(this);
        getCommand("cm").setExecutor(cmCmd);
        getCommand("cm").setTabCompleter(cmCmd);

        BsAdminCommand cmAdminCmd = new BsAdminCommand(this);
        getCommand("cmadmin").setExecutor(cmAdminCmd);
        getCommand("cmadmin").setTabCompleter(cmAdminCmd);

        getServer().getPluginManager().registerEvents(menuListener, this);
        getServer().getPluginManager().registerEvents(new FancyNpcListener(this), this);

        getLogger().info("CustomMenus запущен! Сетов: " + setManager.getSets().size());
    }

    @Override
    public void onDisable() {
        if (dataManager != null) dataManager.save();
        getLogger().info("CustomMenus остановлен.");
    }

    public static BlacksmithPlugin getInstance() { return instance; }
    public SetManager getSetManager()            { return setManager; }
    public DataManager getDataManager()          { return dataManager; }
    public EconomyManager getEconomyManager()    { return economyManager; }
    public LevelManager getLevelManager()        { return levelManager; }
    public MenuListener getMenuListener()        { return menuListener; }
    public NpcBindingManager getNpcBindingManager()             { return npcBindingManager; }
    public PlayerPointsManager getPlayerPointsManager()         { return playerPointsManager; }
    public ExecutableItemsManager getExecutableItemsManager()   { return executableItemsManager; }
}
