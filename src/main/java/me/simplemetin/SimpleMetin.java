package me.simplemetin;

import me.simplemetin.commands.MetinAdminCommand;
import me.simplemetin.commands.MetinCommand;
import me.simplemetin.data.DataHandler;
import me.simplemetin.listeners.BoostVoucherListener;
import me.simplemetin.listeners.CrystalListener;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.managers.CustomItemManager;
import me.simplemetin.managers.HologramManager;
import me.simplemetin.managers.PlayerStatsManager;
import me.simplemetin.managers.SpawnerManager;
import me.simplemetin.managers.StatsManager;
import me.simplemetin.messages.MessageManager;
import me.simplemetin.placeholder.MetinPlaceholder;
import me.simplemetin.placeholder.PlaceholderProvider;
import me.simplemetin.utils.ConfigUpdater;
import org.bukkit.Bukkit;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Set;

public final class SimpleMetin extends JavaPlugin {

    private CrystalManager crystalManager;
    private HologramManager hologramManager;
    private DataHandler dataHandler;
    private StatsManager statsManager;
    private BoostManager boostManager;
    private PlayerStatsManager playerStatsManager;
    private MessageManager messages;
    private CustomItemManager customItemManager;
    private SpawnerManager spawnerManager;

    @Override
    public void onEnable() {
        updateConfigFile();
        this.messages = new MessageManager(this);
        messages.load();
        this.customItemManager = new CustomItemManager(this);

        // Initialize managers
        this.dataHandler = new DataHandler(this);
        this.statsManager = new StatsManager(this);
        this.boostManager = new BoostManager(this);
        this.hologramManager = new HologramManager(this);
        this.crystalManager = new CrystalManager(this, hologramManager, dataHandler);
        this.spawnerManager = new SpawnerManager(this, crystalManager);
        spawnerManager.load();

        // Load data
        dataHandler.loadData();

        // Remove metin entities left in already loaded chunks (crash or older plugin versions);
        // chunks loaded later are cleaned by CrystalListener#onEntitiesLoad
        for (var world : Bukkit.getWorlds()) {
            crystalManager.removeOrphans(world.getEntities());
        }

        // Initialize player stats manager
        this.playerStatsManager = new PlayerStatsManager(this);

        // Register commands
        MetinCommand metinCommand = new MetinCommand(this, crystalManager);
        getCommand("metin").setExecutor(metinCommand);
        getCommand("metin").setTabCompleter(metinCommand);

        MetinAdminCommand adminCommand = new MetinAdminCommand(this);
        getCommand("metinadmin").setExecutor(adminCommand);
        getCommand("metinadmin").setTabCompleter(adminCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(new CrystalListener(this, crystalManager), this);
        getServer().getPluginManager().registerEvents(new BoostVoucherListener(this), this);
        getServer().getPluginManager().registerEvents(playerStatsManager, this);

        // Start tasks
        crystalManager.startUpdateTask();
        crystalManager.startAutoSave();
        boostManager.start();
        spawnerManager.start();

        // Register PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MetinPlaceholder(this).register();
            new PlaceholderProvider(this).register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        getLogger().info("SimpleMetin v" + getDescription().getVersion() + " has been enabled!");
    }

    /**
     * Creates config.yml if needed and adds options of this version that the file is missing (with comments).
     * The "crystals" section belongs to the admin: removed crystal types are not added back.
     */
    private void updateConfigFile() {
        saveDefaultConfig();
        try {
            var added = ConfigUpdater.update(new File(getDataFolder(), "config.yml"), "config.yml", Set.of("crystals"));
            if (!added.isEmpty()) {
                getLogger().info("Added " + added.size() + " new option(s) to config.yml: " + String.join(", ", added));
            }
        } catch (IOException e) {
            getLogger().warning("Could not update config.yml: " + e.getMessage());
        }
        reloadConfig();
    }

    /**
     * Without bundled defaults: every option is in the file anyway (see updateConfigFile), and defaults would make
     * crystal types the admin deleted from config.yml come back from the jar.
     */
    @Override
    public void reloadConfig() {
        super.reloadConfig();
        getConfig().setDefaults(new MemoryConfiguration());
    }

    /** /metin reload: config (with new options), messages, items, crystals, boosts, spawners. Returns restored pending crystals. */
    public int reloadAll() {
        updateConfigFile();
        messages.load();
        customItemManager.load();
        crystalManager.reload();
        statsManager.restartLeaderboardTask();
        boostManager.start();
        spawnerManager.load();
        return dataHandler.retryPending();
    }

    @Override
    public void onDisable() {
        if (spawnerManager != null) {
            spawnerManager.shutdown();
        }
        if (crystalManager != null) {
            crystalManager.shutdown();
        }
        if (dataHandler != null) {
            dataHandler.saveData();
        }
        if (statsManager != null) {
            statsManager.shutdown(); // This will save both stats and leaderboards
            statsManager.saveAllStats();
        }
        if (playerStatsManager != null) {
            playerStatsManager.shutdown();
        }
        if (boostManager != null) {
            boostManager.shutdown();
        }
        getLogger().info("SimpleMetin has been disabled!");
    }

    public CrystalManager getCrystalManager() {
        return crystalManager;
    }

    public HologramManager getHologramManager() {
        return hologramManager;
    }

    public DataHandler getDataHandler() {
        return dataHandler;
    }

    public StatsManager getStatsManager() {
        return statsManager;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    public PlayerStatsManager getPlayerStatsManager() {
        return playerStatsManager;
    }

    public MessageManager getMessages() {
        return messages;
    }

    public CustomItemManager getCustomItemManager() {
        return customItemManager;
    }

    public SpawnerManager getSpawnerManager() {
        return spawnerManager;
    }
}
