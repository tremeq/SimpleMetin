package me.simplemetin.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.PlayerStatsManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PlaceholderProvider extends PlaceholderExpansion {

    private final SimpleMetin plugin;

    public PlaceholderProvider(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "simplemetin";
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

        // %simplemetin_damage%
        if (params.equalsIgnoreCase("damage")) {
            if (player == null) return "0";
            int damage = plugin.getPlayerStatsManager().getDamage(player.getUniqueId());
            return String.valueOf(damage);
        }

        // %simplemetin_top_name_N% (e.g. %simplemetin_top_name_1%)
        if (params.startsWith("top_name_")) {
            return handleTopName(params.substring(9));
        }

        // %simplemetin_top_value_N% (e.g. %simplemetin_top_value_1%)
        if (params.startsWith("top_value_")) {
            return handleTopValue(params.substring(10));
        }

        return null;
    }

    private String handleTopName(String posStr) {
        int position = parsePosition(posStr);
        if (position < 1) return "";

        List<PlayerStatsManager.LeaderboardEntry> top = plugin.getPlayerStatsManager().getCachedLeaderboard();
        if (position > top.size()) return "-";

        OfflinePlayer p = Bukkit.getOfflinePlayer(top.get(position - 1).uuid());
        String name = p.getName();
        return name != null ? name : "Unknown";
    }

    private String handleTopValue(String posStr) {
        int position = parsePosition(posStr);
        if (position < 1) return "0";

        List<PlayerStatsManager.LeaderboardEntry> top = plugin.getPlayerStatsManager().getCachedLeaderboard();
        if (position > top.size()) return "0";

        return String.valueOf(top.get(position - 1).value());
    }

    private int parsePosition(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
