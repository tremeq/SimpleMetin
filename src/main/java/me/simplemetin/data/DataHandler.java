package me.simplemetin.data;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.CrystalManager;
import me.simplemetin.models.CrystalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DataHandler {
    private final SimpleMetin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;
    /**
     * Raw data.yml entries that could not be loaded yet (world not loaded, unknown config-id).
     * They are written back on every save so nothing is lost, and retried on world load / reload.
     */
    private final Map<String, Map<String, Object>> pending = new LinkedHashMap<>();
    private boolean renamed;

    public DataHandler(SimpleMetin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");

        if (!dataFile.exists()) {
            try {
                dataFile.getParentFile().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create data.yml: " + e.getMessage());
            }
        }

        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    public void loadData() {
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        pending.clear();
        renamed = false;
        var crystalsSection = dataConfig.getConfigurationSection("crystals");

        if (crystalsSection == null) {
            plugin.getLogger().info("No crystals found in data.yml");
            return;
        }

        // Entries with proper IDs first, so renamed UUID entries never take an ID that is already in the file
        var ids = new ArrayList<>(crystalsSection.getKeys(false));
        ids.sort(Comparator.comparing(CrystalManager::isLegacyUuidId));
        for (var id : ids) {
            var section = crystalsSection.getConfigurationSection(id);
            if (section == null) continue;

            if (!loadCrystal(id, section)) {
                pending.put(id, toMap(section));
            }
        }

        plugin.getLogger().info("Loaded " + plugin.getCrystalManager().getAllCrystals().size() + " crystals from data.yml"
                + (pending.isEmpty() ? "" : " (" + pending.size() + " kept for later: world or config-id not available)"));
        if (renamed) {
            // Write the new IDs right away so data.yml and the server agree even after a crash
            saveData();
        }
    }

    /** Retries entries that could not be loaded before. Returns the number of crystals loaded now. */
    public int retryPending() {
        int loaded = 0;
        for (var entry : new ArrayList<>(pending.entrySet())) {
            var section = new MemoryConfiguration().createSection("crystal", entry.getValue());
            if (loadCrystal(entry.getKey(), section)) {
                pending.remove(entry.getKey());
                loaded++;
            }
        }
        if (loaded > 0) {
            plugin.getLogger().info("Loaded " + loaded + " previously unavailable crystals");
        }
        return loaded;
    }

    public Set<String> getPendingIds() {
        return Collections.unmodifiableSet(pending.keySet());
    }

    private boolean loadCrystal(String id, ConfigurationSection section) {
        try {
            var configId = section.getString("config-id");
            var worldName = section.getString("location.world");
            var world = worldName != null ? Bukkit.getWorld(worldName) : null;

            if (world == null) {
                plugin.getLogger().warning("World not found for crystal " + id + ": " + worldName + " (kept in data.yml)");
                return false;
            }
            if (configId == null || plugin.getConfig().getConfigurationSection("crystals." + configId) == null) {
                plugin.getLogger().warning("Unknown config-id for crystal " + id + ": " + configId + " (kept in data.yml)");
                return false;
            }

            var location = new Location(world,
                    section.getDouble("location.x"),
                    section.getDouble("location.y"),
                    section.getDouble("location.z"));
            var overrideName = section.getString("override-name");

            // IDs used to be random UUIDs: give them the short "<type>-<n>" form (data is kept)
            var crystalId = id;
            if (CrystalManager.isLegacyUuidId(id) || !CrystalManager.isValidId(id)) {
                crystalId = plugin.getCrystalManager().generateId(configId);
                plugin.getLogger().info("Renamed crystal " + id + " -> " + crystalId);
                renamed = true;
            }

            var data = plugin.getCrystalManager().spawnCrystal(crystalId, configId, location, overrideName);
            if (data == null) return false;

            data.setSpawnerId(section.getString("spawner"));
            data.setExpiresAt(section.getLong("expires-at", 0));

            data.setCurrentHp(section.getInt("current-hp", data.getMaxHp()));
            if (section.getBoolean("is-destroyed", false)) {
                plugin.getCrystalManager().markCrystalDestroyed(crystalId);
                if (data.getType() == CrystalType.RESPAWN) {
                    data.setRespawnTime(section.getLong("respawn-time", 0));
                }
            }
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error loading crystal " + id + ": " + e.getMessage() + " (kept in data.yml)");
            return false;
        }
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();

        for (var data : plugin.getCrystalManager().getAllCrystals()) {
            var path = "crystals." + data.getId();

            dataConfig.set(path + ".config-id", data.getConfigId());
            dataConfig.set(path + ".location.world", data.getWorldName());
            dataConfig.set(path + ".location.x", data.getLocation().getX());
            dataConfig.set(path + ".location.y", data.getLocation().getY());
            dataConfig.set(path + ".location.z", data.getLocation().getZ());
            dataConfig.set(path + ".current-hp", data.getCurrentHp());
            dataConfig.set(path + ".is-destroyed", data.isDestroyed());

            if (data.getOverrideName() != null) {
                dataConfig.set(path + ".override-name", data.getOverrideName());
            }
            if (data.getSpawnerId() != null) {
                dataConfig.set(path + ".spawner", data.getSpawnerId());
            }
            if (data.getExpiresAt() > 0) {
                dataConfig.set(path + ".expires-at", data.getExpiresAt());
            }

            if (data.isDestroyed() && data.getType() == CrystalType.RESPAWN) {
                dataConfig.set(path + ".respawn-time", data.getRespawnTime());
            }
        }

        for (var entry : pending.entrySet()) {
            if (plugin.getCrystalManager().getCrystalById(entry.getKey()) == null) {
                dataConfig.createSection("crystals." + entry.getKey(), entry.getValue());
            }
        }

        try {
            dataConfig.save(dataFile);
            plugin.getLogger().info("Saved " + plugin.getCrystalManager().getAllCrystals().size() + " crystals to data.yml"
                    + (pending.isEmpty() ? "" : " (+" + pending.size() + " pending)"));
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }

    private static Map<String, Object> toMap(ConfigurationSection section) {
        var map = new LinkedHashMap<String, Object>();
        for (var key : section.getKeys(false)) {
            var value = section.get(key);
            map.put(key, value instanceof ConfigurationSection child ? toMap(child) : value);
        }
        return map;
    }
}
