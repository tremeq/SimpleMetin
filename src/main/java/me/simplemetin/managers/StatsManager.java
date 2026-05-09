package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatsManager {
    private final SimpleMetin plugin;
    private final File statsFile;
    private final File leaderboardFile;
    private final File readableStatsFile;
    private YamlConfiguration statsConfig;
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private BukkitTask leaderboardUpdateTask;

    public StatsManager(SimpleMetin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        this.leaderboardFile = new File(plugin.getDataFolder(), "leaderboards.yml");
        this.readableStatsFile = new File(plugin.getDataFolder(), "statistics.yml");

        if (!statsFile.exists()) {
            try {
                statsFile.getParentFile().mkdirs();
                statsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create stats.yml: " + e.getMessage());
            }
        }

        this.statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        loadAllStats();
        startLeaderboardUpdateTask();
    }

    public PlayerStats getOrCreateStats(UUID playerUuid) {
        return playerStats.computeIfAbsent(playerUuid, PlayerStats::new);
    }

    public PlayerStats getStats(UUID playerUuid) {
        return playerStats.get(playerUuid);
    }

    public Collection<PlayerStats> getAllStats() {
        return playerStats.values();
    }

    public List<PlayerStats> getTopCrystalsDestroyed(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Integer.compare(b.getCrystalsDestroyed(), a.getCrystalsDestroyed()))
                .limit(limit)
                .toList();
    }

    public List<PlayerStats> getTopDamage(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalDamageDealt(), a.getTotalDamageDealt()))
                .limit(limit)
                .toList();
    }

    public List<PlayerStats> getTopItemsReceived(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Integer.compare(b.getItemsReceived(), a.getItemsReceived()))
                .limit(limit)
                .toList();
    }

    public List<PlayerStats> getTopMoneyEarned(int limit) {
        return playerStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getMoneyEarned(), a.getMoneyEarned()))
                .limit(limit)
                .toList();
    }

    public void loadAllStats() {
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        var playersSection = statsConfig.getConfigurationSection("players");

        if (playersSection == null) {
            plugin.getLogger().info("No stats found in stats.yml");
            return;
        }

        for (var uuidStr : playersSection.getKeys(false)) {
            try {
                var uuid = UUID.fromString(uuidStr);
                var section = playersSection.getConfigurationSection(uuidStr);
                if (section == null) continue;

                var stats = new PlayerStats(uuid);
                stats.setCrystalsDestroyed(section.getInt("crystals-destroyed", 0));
                stats.setTotalDamageDealt(section.getLong("total-damage", 0));
                stats.setItemsReceived(section.getInt("items-received", 0));
                stats.setMoneyEarned(section.getLong("money-earned", 0));
                stats.setLastSeen(section.getLong("last-seen", System.currentTimeMillis()));

                var typesSection = section.getConfigurationSection("crystal-types");
                if (typesSection != null) {
                    for (var type : typesSection.getKeys(false)) {
                        stats.setCrystalTypeDestroyed(type, typesSection.getInt(type, 0));
                    }
                }

                playerStats.put(uuid, stats);
            } catch (Exception e) {
                plugin.getLogger().warning("Error loading stats for " + uuidStr + ": " + e.getMessage());
            }
        }

        plugin.getLogger().info("Loaded stats for " + playerStats.size() + " players");
    }

    public void saveAllStats() {
        statsConfig = new YamlConfiguration();

        for (var stats : playerStats.values()) {
            var path = "players." + stats.getPlayerUuid().toString();

            statsConfig.set(path + ".crystals-destroyed", stats.getCrystalsDestroyed());
            statsConfig.set(path + ".total-damage", stats.getTotalDamageDealt());
            statsConfig.set(path + ".items-received", stats.getItemsReceived());
            statsConfig.set(path + ".money-earned", stats.getMoneyEarned());
            statsConfig.set(path + ".last-seen", stats.getLastSeen());

            for (var entry : stats.getAllCrystalTypes().entrySet()) {
                statsConfig.set(path + ".crystal-types." + entry.getKey(), entry.getValue());
            }
        }

        try {
            statsConfig.save(statsFile);
            plugin.getLogger().info("Saved stats for " + playerStats.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save stats.yml: " + e.getMessage());
        }
    }

    public void saveLeaderboards() {
        YamlConfiguration leaderboard = new YamlConfiguration();

        // Header
        leaderboard.set("info.last-updated", new Date().toString());
        leaderboard.set("info.total-players", playerStats.size());

        // TOP 100 - Crystals Destroyed
        saveTopToConfig(leaderboard, "top-destroyed", getTopCrystalsDestroyed(100),
            stats -> stats.getCrystalsDestroyed());

        // TOP 100 - Damage
        saveTopToConfig(leaderboard, "top-damage", getTopDamage(100),
            stats -> stats.getTotalDamageDealt());

        // TOP 100 - Items
        saveTopToConfig(leaderboard, "top-items", getTopItemsReceived(100),
            stats -> stats.getItemsReceived());

        // TOP 100 - Money
        saveTopToConfig(leaderboard, "top-money", getTopMoneyEarned(100),
            stats -> stats.getMoneyEarned());

        try {
            leaderboard.save(leaderboardFile);
            plugin.getLogger().info("Saved leaderboards to leaderboards.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save leaderboards.yml: " + e.getMessage());
        }
    }

    private void saveTopToConfig(YamlConfiguration config, String path, List<PlayerStats> topPlayers,
                                   java.util.function.Function<PlayerStats, Number> valueExtractor) {
        int position = 1;
        for (PlayerStats stats : topPlayers) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(stats.getPlayerUuid());
            String playerName = player.getName() != null ? player.getName() : "Unknown";
            Number value = valueExtractor.apply(stats);

            config.set(path + "." + position + ".player", playerName);
            config.set(path + "." + position + ".uuid", stats.getPlayerUuid().toString());
            config.set(path + "." + position + ".value", value);
            position++;
        }
    }

    public void saveReadableStatistics() {
        YamlConfiguration readable = new YamlConfiguration();

        // Header
        readable.set("info.last-updated", new Date().toString());
        readable.set("info.total-players", playerStats.size());
        readable.set("info.description", "All player statistics - sorted by name");

        // Sort players by name for easier reading
        List<PlayerStats> sortedPlayers = playerStats.values().stream()
                .sorted((a, b) -> {
                    OfflinePlayer playerA = Bukkit.getOfflinePlayer(a.getPlayerUuid());
                    OfflinePlayer playerB = Bukkit.getOfflinePlayer(b.getPlayerUuid());
                    String nameA = playerA.getName() != null ? playerA.getName() : "Unknown";
                    String nameB = playerB.getName() != null ? playerB.getName() : "Unknown";
                    return nameA.compareToIgnoreCase(nameB);
                })
                .toList();

        // Save all player stats
        for (PlayerStats stats : sortedPlayers) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(stats.getPlayerUuid());
            String playerName = player.getName() != null ? player.getName() : "Unknown";
            String path = "players." + playerName;

            readable.set(path + ".uuid", stats.getPlayerUuid().toString());
            readable.set(path + ".crystals-destroyed", stats.getCrystalsDestroyed());
            readable.set(path + ".total-damage", stats.getTotalDamageDealt());
            readable.set(path + ".items-received", stats.getItemsReceived());
            readable.set(path + ".money-earned", stats.getMoneyEarned());
            readable.set(path + ".last-seen", new Date(stats.getLastSeen()).toString());

            // Crystal types breakdown
            if (!stats.getAllCrystalTypes().isEmpty()) {
                for (var entry : stats.getAllCrystalTypes().entrySet()) {
                    readable.set(path + ".crystal-types." + entry.getKey(), entry.getValue());
                }
            }
        }

        try {
            readable.save(readableStatsFile);
            plugin.getLogger().info("Saved readable statistics to statistics.yml");
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save statistics.yml: " + e.getMessage());
        }
    }

    private void startLeaderboardUpdateTask() {
        long interval = plugin.getConfig().getLong("settings.leaderboard-update-interval", 6000); // 5 min default

        leaderboardUpdateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            saveLeaderboards();
            saveReadableStatistics();
        }, interval, interval);

        // Save immediately on start
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            saveLeaderboards();
            saveReadableStatistics();
        });
    }

    public void shutdown() {
        if (leaderboardUpdateTask != null) {
            leaderboardUpdateTask.cancel();
        }
        saveLeaderboards(); // Final save
        saveReadableStatistics(); // Final save
    }
}
