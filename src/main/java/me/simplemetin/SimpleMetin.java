package me.simplemetin;

import me.simplemetin.commands.MetinCommand;
import me.simplemetin.data.DataHandler;
import me.simplemetin.listeners.BoostVoucherListener;
import me.simplemetin.listeners.CrystalListener;
import me.simplemetin.managers.BoostManager;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.managers.HologramManager;
import me.simplemetin.managers.StatsManager;
import me.simplemetin.placeholder.MetinPlaceholder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleMetin extends JavaPlugin {

    private CrystalManager crystalManager;
    private HologramManager hologramManager;
    private DataHandler dataHandler;
    private StatsManager statsManager;
    private BoostManager boostManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize managers
        this.dataHandler = new DataHandler(this);
        this.statsManager = new StatsManager(this);
        this.boostManager = new BoostManager(this);
        this.hologramManager = new HologramManager(this);
        this.crystalManager = new CrystalManager(this, hologramManager, dataHandler);

        // Load data
        dataHandler.loadData();

        // Register commands
        MetinCommand metinCommand = new MetinCommand(this, crystalManager);
        getCommand("metin").setExecutor(metinCommand);
        getCommand("metin").setTabCompleter(metinCommand);

        // Register listeners
        getServer().getPluginManager().registerEvents(new CrystalListener(this, crystalManager), this);
        getServer().getPluginManager().registerEvents(new BoostVoucherListener(this), this);

        // Start tasks
        crystalManager.startUpdateTask();
        crystalManager.startAutoSave();

        // Register PlaceholderAPI if available
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MetinPlaceholder(this).register();
            getLogger().info("PlaceholderAPI hooked successfully!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        getLogger().info("SimpleMetin v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
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
}
