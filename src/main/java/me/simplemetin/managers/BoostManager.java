package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BoostManager {
    private final SimpleMetin plugin;
    private final File boostsFile;
    private final Map<UUID, PersonalBoost> personalBoosts = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> personalBossBars = new ConcurrentHashMap<>();
    private volatile GlobalBoost globalBoost = null;
    private BossBar globalBossBar = null;
    private BukkitTask updateTask;

    public BoostManager(SimpleMetin plugin) {
        this.plugin = plugin;
        this.boostsFile = new File(plugin.getDataFolder(), "boosts.yml");
        loadBoosts();
    }

    /** Starts the task that updates boost BossBars and announces expired boosts. */
    public void start() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        long interval = Math.max(1, plugin.getConfig().getLong("boosts.bossbar.update-interval", 20));
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void activateGlobalBoost(double multiplier, long durationSeconds) {
        long durationMs = durationSeconds * 1000L;
        long expiresAt = System.currentTimeMillis() + durationMs;
        this.globalBoost = new GlobalBoost(multiplier, expiresAt, durationMs);
        plugin.getLogger().info("Global boost activated: " + multiplier + "x for " + durationSeconds + " seconds");
    }

    public void activatePersonalBoost(Player player, double multiplier, long durationSeconds) {
        long durationMs = durationSeconds * 1000L;
        long expiresAt = System.currentTimeMillis() + durationMs;
        personalBoosts.put(player.getUniqueId(), new PersonalBoost(multiplier, expiresAt, durationMs));
        plugin.getLogger().info("Personal boost activated for " + player.getName() + ": " + multiplier + "x for " + durationSeconds + " seconds");
    }

    public void removePersonalBoost(UUID playerUuid) {
        personalBoosts.remove(playerUuid);
    }

    public void removeGlobalBoost() {
        globalBoost = null;
    }

    /**
     * Combined drop-chance multiplier.
     * stacking-enabled=false: the higher of global/personal.
     * stacking-enabled=true: stacking-mode 1.0 adds the bonuses (2x + 2x = 3x), 2.0 multiplies them (2x * 2x = 4x).
     */
    public double getTotalMultiplier(Player player) {
        var global = getGlobalBoost();
        var personal = getPersonalBoost(player.getUniqueId());

        double g = global != null ? global.multiplier : 1.0;
        double p = personal != null ? personal.multiplier : 1.0;

        if (!plugin.getConfig().getBoolean("boosts.personal.stacking-enabled", true)) {
            return Math.max(g, p);
        }

        double stackingMode = plugin.getConfig().getDouble("boosts.global.stacking-mode", 1.0);
        if (stackingMode >= 2.0) {
            return g * p;
        }
        return 1.0 + (g - 1.0) + (p - 1.0);
    }

    // Expired boosts are not removed here: tick() removes them so it can announce the expiry

    public GlobalBoost getGlobalBoost() {
        var boost = globalBoost;
        return boost != null && !boost.isExpired() ? boost : null;
    }

    public PersonalBoost getPersonalBoost(UUID playerUuid) {
        var boost = personalBoosts.get(playerUuid);
        return boost != null && !boost.isExpired() ? boost : null;
    }

    public long getGlobalBoostTimeLeft() {
        var boost = getGlobalBoost();
        return boost == null ? 0 : Math.max(0, (boost.expiresAt - System.currentTimeMillis()) / 1000);
    }

    public long getPersonalBoostTimeLeft(UUID playerUuid) {
        var boost = getPersonalBoost(playerUuid);
        return boost == null ? 0 : Math.max(0, (boost.expiresAt - System.currentTimeMillis()) / 1000);
    }

    private void tick() {
        expireBoosts();

        if (plugin.getConfig().getBoolean("boosts.bossbar.enabled", true)) {
            updateGlobalBossBar();
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePersonalBossBar(player);
            }
        } else {
            removeAllBossBars();
        }
    }

    private void expireBoosts() {
        var global = globalBoost;
        if (global != null && global.isExpired()) {
            globalBoost = null;
            plugin.getMessages().broadcast("global-boost-expired");
        }

        for (var entry : personalBoosts.entrySet()) {
            if (!entry.getValue().isExpired()) continue;
            personalBoosts.remove(entry.getKey());

            // Players who were offline when it expired get no message
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                plugin.getMessages().send(player, "boost-expired");
            }
        }
    }

    private void updateGlobalBossBar() {
        var boost = getGlobalBoost();
        if (!plugin.getConfig().getBoolean("boosts.bossbar.global.enabled", true) || boost == null) {
            if (globalBossBar != null) {
                globalBossBar.removeAll();
                globalBossBar = null;
            }
            return;
        }

        if (globalBossBar == null) {
            globalBossBar = createBossBar("global", BarColor.GREEN);
        }

        long timeLeftMs = boost.expiresAt - System.currentTimeMillis();
        globalBossBar.setTitle(plugin.getMessages().legacy("bossbar-boost-global",
                "multiplier", formatMultiplier(boost.multiplier), "time", formatTime(timeLeftMs / 1000)));
        globalBossBar.setProgress(progress(timeLeftMs, boost.totalDurationMs));

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!globalBossBar.getPlayers().contains(player)) {
                globalBossBar.addPlayer(player);
            }
        }
    }

    private void updatePersonalBossBar(Player player) {
        var boost = getPersonalBoost(player.getUniqueId());
        if (!plugin.getConfig().getBoolean("boosts.bossbar.personal.enabled", true) || boost == null) {
            BossBar bar = personalBossBars.remove(player.getUniqueId());
            if (bar != null) {
                bar.removeAll();
            }
            return;
        }

        BossBar bar = personalBossBars.computeIfAbsent(player.getUniqueId(), uuid -> createBossBar("personal", BarColor.YELLOW));

        long timeLeftMs = boost.expiresAt - System.currentTimeMillis();
        bar.setTitle(plugin.getMessages().legacy("bossbar-boost-personal",
                "multiplier", formatMultiplier(boost.multiplier), "time", formatTime(timeLeftMs / 1000)));
        bar.setProgress(progress(timeLeftMs, boost.totalDurationMs));

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private BossBar createBossBar(String type, BarColor defaultColor) {
        String colorStr = plugin.getConfig().getString("boosts.bossbar." + type + ".color", defaultColor.name());
        String styleStr = plugin.getConfig().getString("boosts.bossbar." + type + ".style", "SOLID");

        BarColor color;
        try {
            color = BarColor.valueOf(colorStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            color = defaultColor;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(styleStr.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            style = BarStyle.SOLID;
        }

        BossBar bar = Bukkit.createBossBar("", color, style);
        bar.setVisible(true);
        return bar;
    }

    private void removeAllBossBars() {
        if (globalBossBar != null) {
            globalBossBar.removeAll();
            globalBossBar = null;
        }
        for (BossBar bar : personalBossBars.values()) {
            bar.removeAll();
        }
        personalBossBars.clear();
    }

    private static double progress(long timeLeftMs, long totalMs) {
        if (totalMs <= 0) return 0.0;
        return Math.max(0.0, Math.min(1.0, (double) timeLeftMs / totalMs));
    }

    // ── Persistence (boosts.yml) ────────────────────────────────

    private void loadBoosts() {
        if (!boostsFile.exists()) return;

        var config = YamlConfiguration.loadConfiguration(boostsFile);
        long now = System.currentTimeMillis();

        if (config.contains("global")) {
            var boost = new GlobalBoost(config.getDouble("global.multiplier"), config.getLong("global.expires-at"),
                    config.getLong("global.total-duration"));
            if (boost.expiresAt > now) {
                globalBoost = boost;
            }
        }

        var personal = config.getConfigurationSection("personal");
        if (personal != null) {
            for (var key : personal.getKeys(false)) {
                try {
                    var boost = new PersonalBoost(personal.getDouble(key + ".multiplier"),
                            personal.getLong(key + ".expires-at"), personal.getLong(key + ".total-duration"));
                    if (boost.expiresAt > now) {
                        personalBoosts.put(UUID.fromString(key), boost);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
    }

    public void saveBoosts() {
        var config = new YamlConfiguration();

        var global = getGlobalBoost();
        if (global != null) {
            config.set("global.multiplier", global.multiplier);
            config.set("global.expires-at", global.expiresAt);
            config.set("global.total-duration", global.totalDurationMs);
        }

        for (var entry : personalBoosts.entrySet()) {
            var boost = entry.getValue();
            if (boost.isExpired()) continue;
            var path = "personal." + entry.getKey();
            config.set(path + ".multiplier", boost.multiplier);
            config.set(path + ".expires-at", boost.expiresAt);
            config.set(path + ".total-duration", boost.totalDurationMs);
        }

        try {
            config.save(boostsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save boosts.yml: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        removeAllBossBars();
        saveBoosts();
    }

    // ── Formatting ──────────────────────────────────────────────

    /** Always uses a dot ("2.0"), independent of the server locale. */
    public static String formatMultiplier(double multiplier) {
        return String.format(Locale.ROOT, "%.1f", multiplier);
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + secs + "s";
        } else if (minutes > 0) {
            return minutes + "m " + secs + "s";
        } else {
            return secs + "s";
        }
    }

    public static class GlobalBoost {
        public final double multiplier;
        public final long expiresAt;
        public final long totalDurationMs;

        public GlobalBoost(double multiplier, long expiresAt, long totalDurationMs) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
            this.totalDurationMs = totalDurationMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }

    public static class PersonalBoost {
        public final double multiplier;
        public final long expiresAt;
        public final long totalDurationMs;

        public PersonalBoost(double multiplier, long expiresAt, long totalDurationMs) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
            this.totalDurationMs = totalDurationMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
