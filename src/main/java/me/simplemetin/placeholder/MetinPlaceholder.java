package me.simplemetin.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.simplemetin.SimpleMetin;
import me.simplemetin.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MetinPlaceholder extends PlaceholderExpansion {
    private final SimpleMetin plugin;

    public MetinPlaceholder(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "metin";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        // Handle TOP placeholders first (don't require player stats)
        if (params.contains("_top_")) {
            return handleTopPlaceholder(params);
        }

        // Handle boost placeholders (don't require player stats)
        if (params.startsWith("boost_") || params.startsWith("personal_boost_") || params.startsWith("global_boost_")) {
            return handleBoostPlaceholder(player, params);
        }

        // For other placeholders, we need player stats
        var stats = plugin.getStatsManager().getStats(player.getUniqueId());
        if (stats == null) {
            // Return 0 for stat placeholders if player has no stats yet
            if (params.startsWith("type_")) {
                return "0";
            }
            switch (params.toLowerCase()) {
                case "crystals_destroyed", "total_damage", "items_received", "money_earned":
                    return "0";
                default:
                    return "";
            }
        }

        // Player stats placeholders
        switch (params.toLowerCase()) {
            case "crystals_destroyed":
                return String.valueOf(stats.getCrystalsDestroyed());

            case "total_damage":
                return String.valueOf(stats.getTotalDamageDealt());

            case "items_received":
                return String.valueOf(stats.getItemsReceived());

            case "money_earned":
                return String.valueOf(stats.getMoneyEarned());
        }

        // Crystal type stats: %metin_type_<crystal_id>%
        if (params.startsWith("type_")) {
            String crystalType = params.substring(5);
            return String.valueOf(stats.getCrystalTypeDestroyed(crystalType));
        }

        return null;
    }

    private String handleBoostPlaceholder(OfflinePlayer player, String params) {
        switch (params.toLowerCase()) {
            case "boost_multiplier":
                if (!player.isOnline()) return "1.0";
                double mult = plugin.getBoostManager().getTotalMultiplier(player.getPlayer());
                return String.format("%.1f", mult);

            case "boost_active":
                if (!player.isOnline()) return "false";
                double multiplier = plugin.getBoostManager().getTotalMultiplier(player.getPlayer());
                return multiplier > 1.0 ? "true" : "false";

            case "personal_boost_time":
                long personalTime = plugin.getBoostManager().getPersonalBoostTimeLeft(player.getUniqueId());
                return formatTime(personalTime);

            case "global_boost_time":
                long globalTime = plugin.getBoostManager().getGlobalBoostTimeLeft();
                return formatTime(globalTime);

            case "personal_boost_multiplier":
                var personalBoost = plugin.getBoostManager().getPersonalBoost(player.getUniqueId());
                return personalBoost != null ? String.format("%.1f", personalBoost.multiplier) : "1.0";

            case "global_boost_multiplier":
                var globalBoost = plugin.getBoostManager().getGlobalBoost();
                return globalBoost != null ? String.format("%.1f", globalBoost.multiplier) : "1.0";

            default:
                return "";
        }
    }

    private String handleTopPlaceholder(String params) {
        // Parse: destroyed_top_1_name -> type=destroyed, position=1, isName=true
        String[] parts = params.split("_top_");
        if (parts.length != 2) return "";

        String statType = parts[0]; // destroyed, damage, items, money
        String positionPart = parts[1]; // 1_name, 1, etc.

        boolean isName = positionPart.endsWith("_name");
        String posStr = isName ? positionPart.substring(0, positionPart.length() - 5) : positionPart;

        int position;
        try {
            position = Integer.parseInt(posStr);
        } catch (NumberFormatException e) {
            return "";
        }

        // Get appropriate top list based on stat type
        List<PlayerStats> topPlayers = switch (statType) {
            case "destroyed" -> plugin.getStatsManager().getTopCrystalsDestroyed(position);
            case "damage" -> plugin.getStatsManager().getTopDamage(position);
            case "items" -> plugin.getStatsManager().getTopItemsReceived(position);
            case "money" -> plugin.getStatsManager().getTopMoneyEarned(position);
            default -> List.of();
        };

        if (position < 1 || position > topPlayers.size()) {
            return isName ? "-" : "0";
        }

        PlayerStats stats = topPlayers.get(position - 1);

        if (isName) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(stats.getPlayerUuid());
            String name = player.getName();
            return name != null ? name : "Unknown";
        } else {
            return switch (statType) {
                case "destroyed" -> String.valueOf(stats.getCrystalsDestroyed());
                case "damage" -> String.valueOf(stats.getTotalDamageDealt());
                case "items" -> String.valueOf(stats.getItemsReceived());
                case "money" -> String.valueOf(stats.getMoneyEarned());
                default -> "0";
            };
        }
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }
}
