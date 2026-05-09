package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoostManager {
    private final SimpleMetin plugin;
    private final Map<UUID, PersonalBoost> personalBoosts = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> personalBossBars = new ConcurrentHashMap<>();
    private GlobalBoost globalBoost = null;
    private BossBar globalBossBar = null;
    private BukkitTask updateTask;

    public BoostManager(SimpleMetin plugin) {
        this.plugin = plugin;
        startBossBarUpdateTask();
    }

    public void activateGlobalBoost(double multiplier, long durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        this.globalBoost = new GlobalBoost(multiplier, expiresAt);
        plugin.getLogger().info("Global boost activated: " + multiplier + "x for " + durationSeconds + " seconds");
    }

    public void activatePersonalBoost(Player player, double multiplier, long durationSeconds) {
        long expiresAt = System.currentTimeMillis() + (durationSeconds * 1000L);
        personalBoosts.put(player.getUniqueId(), new PersonalBoost(multiplier, expiresAt));
        plugin.getLogger().info("Personal boost activated for " + player.getName() + ": " + multiplier + "x for " + durationSeconds + " seconds");
    }

    public void removePersonalBoost(UUID playerUuid) {
        personalBoosts.remove(playerUuid);
    }

    public void removeGlobalBoost() {
        globalBoost = null;
    }

    public double getTotalMultiplier(Player player) {
        double multiplier = 1.0;

        // Global boost
        if (globalBoost != null) {
            if (System.currentTimeMillis() >= globalBoost.expiresAt) {
                globalBoost = null;
            } else {
                double globalMult = plugin.getConfig().getDouble("boosts.global.stacking-mode", 1.0) == 2.0 ?
                        globalBoost.multiplier : (globalBoost.multiplier - 1.0);
                multiplier += globalMult - 1.0;
            }
        }

        // Personal boost
        PersonalBoost personal = personalBoosts.get(player.getUniqueId());
        if (personal != null) {
            if (System.currentTimeMillis() >= personal.expiresAt) {
                personalBoosts.remove(player.getUniqueId());
            } else {
                boolean stackingEnabled = plugin.getConfig().getBoolean("boosts.personal.stacking-enabled", true);
                if (stackingEnabled) {
                    multiplier *= personal.multiplier;
                } else {
                    multiplier = Math.max(multiplier, personal.multiplier);
                }
            }
        }

        return multiplier;
    }

    public GlobalBoost getGlobalBoost() {
        if (globalBoost != null && System.currentTimeMillis() >= globalBoost.expiresAt) {
            globalBoost = null;
        }
        return globalBoost;
    }

    public PersonalBoost getPersonalBoost(UUID playerUuid) {
        PersonalBoost boost = personalBoosts.get(playerUuid);
        if (boost != null && System.currentTimeMillis() >= boost.expiresAt) {
            personalBoosts.remove(playerUuid);
            return null;
        }
        return boost;
    }

    public long getGlobalBoostTimeLeft() {
        if (globalBoost == null) return 0;
        long timeLeft = globalBoost.expiresAt - System.currentTimeMillis();
        return Math.max(0, timeLeft / 1000);
    }

    public long getPersonalBoostTimeLeft(UUID playerUuid) {
        PersonalBoost boost = personalBoosts.get(playerUuid);
        if (boost == null) return 0;
        long timeLeft = boost.expiresAt - System.currentTimeMillis();
        return Math.max(0, timeLeft / 1000);
    }

    private void startBossBarUpdateTask() {
        if (!plugin.getConfig().getBoolean("boosts.bossbar.enabled", true)) {
            return;
        }

        long interval = plugin.getConfig().getLong("boosts.bossbar.update-interval", 20);
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars, interval, interval);
    }

    private void updateBossBars() {
        // Update global boost bossbar
        updateGlobalBossBar();

        // Update personal boost bossbars
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePersonalBossBar(player);
        }
    }

    private void updateGlobalBossBar() {
        if (!plugin.getConfig().getBoolean("boosts.bossbar.global.enabled", true)) {
            if (globalBossBar != null) {
                globalBossBar.removeAll();
                globalBossBar = null;
            }
            return;
        }

        if (globalBoost != null && System.currentTimeMillis() < globalBoost.expiresAt) {
            if (globalBossBar == null) {
                createGlobalBossBar();
            }

            // Update title with current time
            long timeLeft = (globalBoost.expiresAt - System.currentTimeMillis()) / 1000;
            String title = plugin.getConfig().getString("boosts.bossbar.global.title",
                    "&6&l⭐ GLOBAL BOOST &6%multiplier%x &7- &6%time%");
            title = colorize(title
                    .replace("%multiplier%", String.format("%.1f", globalBoost.multiplier))
                    .replace("%time%", formatTime(timeLeft)));
            globalBossBar.setTitle(title);

            // Update progress
            double progress = (double) timeLeft / ((globalBoost.expiresAt - (System.currentTimeMillis() - timeLeft * 1000)) / 1000);
            globalBossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

            // Add all online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!globalBossBar.getPlayers().contains(player)) {
                    globalBossBar.addPlayer(player);
                }
            }
        } else {
            if (globalBossBar != null) {
                globalBossBar.removeAll();
                globalBossBar = null;
            }
            globalBoost = null;
        }
    }

    private void createGlobalBossBar() {
        String colorStr = plugin.getConfig().getString("boosts.bossbar.global.color", "GREEN");
        String styleStr = plugin.getConfig().getString("boosts.bossbar.global.style", "SOLID");

        BarColor color;
        try {
            color = BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BarColor.GREEN;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        globalBossBar = Bukkit.createBossBar("", color, style);
        globalBossBar.setVisible(true);
    }

    private void updatePersonalBossBar(Player player) {
        if (!plugin.getConfig().getBoolean("boosts.bossbar.personal.enabled", true)) {
            BossBar bar = personalBossBars.remove(player.getUniqueId());
            if (bar != null) {
                bar.removePlayer(player);
            }
            return;
        }

        PersonalBoost boost = personalBoosts.get(player.getUniqueId());

        if (boost != null && System.currentTimeMillis() < boost.expiresAt) {
            BossBar bar = personalBossBars.get(player.getUniqueId());
            if (bar == null) {
                bar = createPersonalBossBar();
                personalBossBars.put(player.getUniqueId(), bar);
            }

            // Update title
            long timeLeft = (boost.expiresAt - System.currentTimeMillis()) / 1000;
            String title = plugin.getConfig().getString("boosts.bossbar.personal.title",
                    "&e&l⚡ PERSONAL BOOST &e%multiplier%x &7- &e%time%");
            title = colorize(title
                    .replace("%multiplier%", String.format("%.1f", boost.multiplier))
                    .replace("%time%", formatTime(timeLeft)));
            bar.setTitle(title);

            // Update progress
            double totalDuration = (boost.expiresAt - (System.currentTimeMillis() - timeLeft * 1000)) / 1000.0;
            double progress = timeLeft / totalDuration;
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));

            // Add player
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }
        } else {
            BossBar bar = personalBossBars.remove(player.getUniqueId());
            if (bar != null) {
                bar.removePlayer(player);
            }
            personalBoosts.remove(player.getUniqueId());
        }
    }

    private BossBar createPersonalBossBar() {
        String colorStr = plugin.getConfig().getString("boosts.bossbar.personal.color", "YELLOW");
        String styleStr = plugin.getConfig().getString("boosts.bossbar.personal.style", "SOLID");

        BarColor color;
        try {
            color = BarColor.valueOf(colorStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            color = BarColor.YELLOW;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        BossBar bar = Bukkit.createBossBar("", color, style);
        bar.setVisible(true);
        return bar;
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }

        if (globalBossBar != null) {
            globalBossBar.removeAll();
        }

        for (BossBar bar : personalBossBars.values()) {
            bar.removeAll();
        }
        personalBossBars.clear();
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

    private String colorize(String text) {
        return text.replace('&', '§');
    }

    public static class GlobalBoost {
        public final double multiplier;
        public final long expiresAt;

        public GlobalBoost(double multiplier, long expiresAt) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
        }
    }

    public static class PersonalBoost {
        public final double multiplier;
        public final long expiresAt;

        public PersonalBoost(double multiplier, long expiresAt) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
        }
    }
}
