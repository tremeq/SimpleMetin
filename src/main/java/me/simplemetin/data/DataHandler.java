package me.simplemetin.data;

import me.simplemetin.SimpleMetin;
import me.simplemetin.models.CrystalType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class DataHandler {
    private final SimpleMetin plugin;
    private final File dataFile;
    private YamlConfiguration dataConfig;

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
        var crystalsSection = dataConfig.getConfigurationSection("crystals");

        if (crystalsSection == null) {
            plugin.getLogger().info("No crystals found in data.yml");
            return;
        }

        for (var id : crystalsSection.getKeys(false)) {
            var section = crystalsSection.getConfigurationSection(id);
            if (section == null) continue;

            try {
                var configId = section.getString("config-id");
                var worldName = section.getString("location.world");
                var x = section.getDouble("location.x");
                var y = section.getDouble("location.y");
                var z = section.getDouble("location.z");
                var world = Bukkit.getWorld(worldName);

                if (world == null) {
                    plugin.getLogger().warning("World not found for crystal " + id + ": " + worldName);
                    continue;
                }

                var location = new Location(world, x, y, z);
                var overrideName = section.getString("override-name");

                plugin.getCrystalManager().spawnCrystal(id, configId, location, overrideName);

                var data = plugin.getCrystalManager().getCrystalById(id);
                if (data != null) {
                    data.setCurrentHp(section.getInt("current-hp", data.getMaxHp()));
                    data.setDestroyed(section.getBoolean("is-destroyed", false));

                    if (data.isDestroyed() && data.getType() == CrystalType.RESPAWN) {
                        data.setRespawnTime(section.getLong("respawn-time", 0));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading crystal " + id + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded " + plugin.getCrystalManager().getAllCrystals().size() + " crystals from data.yml");
    }

    public void saveData() {
        dataConfig = new YamlConfiguration();

        for (var data : plugin.getCrystalManager().getAllCrystals()) {
            var path = "crystals." + data.getId();

            dataConfig.set(path + ".config-id", data.getConfigId());
            dataConfig.set(path + ".location.world", data.getLocation().getWorld().getName());
            dataConfig.set(path + ".location.x", data.getLocation().getX());
            dataConfig.set(path + ".location.y", data.getLocation().getY());
            dataConfig.set(path + ".location.z", data.getLocation().getZ());
            dataConfig.set(path + ".current-hp", data.getCurrentHp());
            dataConfig.set(path + ".is-destroyed", data.isDestroyed());

            if (data.getOverrideName() != null) {
                dataConfig.set(path + ".override-name", data.getOverrideName());
            }

            if (data.isDestroyed() && data.getType() == CrystalType.RESPAWN) {
                dataConfig.set(path + ".respawn-time", data.getRespawnTime());
            }
        }

        try {
            dataConfig.save(dataFile);
            plugin.getLogger().info("Saved " + plugin.getCrystalManager().getAllCrystals().size() + " crystals to data.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save data.yml: " + e.getMessage());
        }
    }
}
