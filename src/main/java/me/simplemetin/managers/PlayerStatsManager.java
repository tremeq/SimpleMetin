package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatsManager implements Listener {

    public static final int DEFAULT_DAMAGE = 10;
    /** Strength 0 would roll hit-drops forever without ever destroying the crystal. */
    public static final int MIN_DAMAGE = 1;

    private final SimpleMetin plugin;
    private final File playersFile;
    private YamlConfiguration playersConfig;
    private final Map<UUID, Integer> damageCache = new ConcurrentHashMap<>();
    private volatile List<LeaderboardEntry> cachedLeaderboard = List.of();
    private BukkitTask leaderboardTask;

    public PlayerStatsManager(SimpleMetin plugin) {
        this.plugin = plugin;
        this.playersFile = new File(plugin.getDataFolder(), "players.yml");
        ensureFileExists();
        this.playersConfig = YamlConfiguration.loadConfiguration(playersFile);

        for (Player player : Bukkit.getOnlinePlayers()) {
            loadPlayer(player.getUniqueId());
        }

        rebuildLeaderboard();

        this.leaderboardTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin, this::rebuildLeaderboard, 6000L, 6000L
        );
    }

    private void ensureFileExists() {
        if (!playersFile.exists()) {
            try {
                playersFile.getParentFile().mkdirs();
                playersFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create players.yml: " + e.getMessage());
            }
        }
    }

    // ── Event Handlers ──────────────────────────────────────────

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        Integer value = damageCache.remove(uuid);
        if (value != null) {
            persistPlayer(uuid, value);
        }
    }

    // ── Data Access ─────────────────────────────────────────────

    private void loadPlayer(UUID uuid) {
        if (isKnown(uuid)) {
            damageCache.put(uuid, readFromConfig(uuid));
        } else {
            damageCache.put(uuid, DEFAULT_DAMAGE);
            persistPlayer(uuid, DEFAULT_DAMAGE);
            plugin.getLogger().info("New player registered with " + DEFAULT_DAMAGE + " damage: " + uuid);
        }
    }

    // playersConfig is shared with the async leaderboard task and PAPI, so every access is synchronized

    /** True if the player has an entry in players.yml. */
    public synchronized boolean isKnown(UUID uuid) {
        return playersConfig.contains(uuid.toString());
    }

    private synchronized int readFromConfig(UUID uuid) {
        return playersConfig.getInt(uuid + ".damage", DEFAULT_DAMAGE);
    }

    private synchronized void persistPlayer(UUID uuid, int value) {
        playersConfig.set(uuid + ".damage", value);
        saveFile();
    }

    private void saveFile() {
        try {
            playersConfig.save(playersFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save players.yml: " + e.getMessage());
        }
    }

    /**
     * Returns the damage strength for a player.
     * Online players: read from cache. Offline: read from file.
     * Unknown players return DEFAULT_DAMAGE (10).
     */
    public int getDamage(UUID uuid) {
        Integer cached = damageCache.get(uuid);
        if (cached != null) return cached;
        return readFromConfig(uuid);
    }

    /**
     * Sets the damage strength (minimum {@link #MIN_DAMAGE}) and immediately persists to players.yml.
     * Works for offline players too; only online players are kept in the cache.
     */
    public void setDamage(UUID uuid, int value) {
        int safe = Math.max(MIN_DAMAGE, value);
        damageCache.computeIfPresent(uuid, (k, v) -> safe);
        persistPlayer(uuid, safe);
    }

    /**
     * Adds to the current damage strength (result clamped to {@link #MIN_DAMAGE}..Integer.MAX_VALUE) and persists.
     */
    public void addDamage(UUID uuid, int value) {
        long result = (long) getDamage(uuid) + value;
        setDamage(uuid, (int) Math.min(Integer.MAX_VALUE, Math.max(MIN_DAMAGE, result)));
    }

    /**
     * Resets damage strength to DEFAULT_DAMAGE (10) and persists.
     */
    public void resetDamage(UUID uuid) {
        setDamage(uuid, DEFAULT_DAMAGE);
    }

    // ── Leaderboard ─────────────────────────────────────────────

    /**
     * Rebuilds the cached top-10 leaderboard from the in-memory players.yml data.
     * Called asynchronously every 5 minutes (reading the file there could see a half-written save).
     */
    private void rebuildLeaderboard() {
        List<LeaderboardEntry> entries = new ArrayList<>();

        synchronized (this) {
            for (String key : playersConfig.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    int damage = playersConfig.getInt(key + ".damage", DEFAULT_DAMAGE);
                    entries.add(new LeaderboardEntry(uuid, damage));
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        entries.sort(Comparator.comparingInt(LeaderboardEntry::value).reversed());

        if (entries.size() > 10) {
            entries = new ArrayList<>(entries.subList(0, 10));
        }

        this.cachedLeaderboard = List.copyOf(entries);
    }

    /**
     * Returns the cached top-10 leaderboard (rebuilt every 5 minutes).
     */
    public List<LeaderboardEntry> getCachedLeaderboard() {
        return cachedLeaderboard;
    }

    // ── Lifecycle ───────────────────────────────────────────────

    /**
     * Saves all online players' data to players.yml.
     */
    public synchronized void saveAll() {
        for (var entry : damageCache.entrySet()) {
            playersConfig.set(entry.getKey() + ".damage", entry.getValue());
        }
        saveFile();
    }

    public void shutdown() {
        if (leaderboardTask != null) {
            leaderboardTask.cancel();
        }
        saveAll();
    }

    // ── Leaderboard Entry ───────────────────────────────────────

    public record LeaderboardEntry(UUID uuid, int value) {
    }
}
