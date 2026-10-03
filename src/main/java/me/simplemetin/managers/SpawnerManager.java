package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.models.CrystalData;
import me.simplemetin.models.IntRange;
import me.simplemetin.utils.ConfigUpdater;
import me.simplemetin.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

/**
 * Automatic crystal spawns defined in spawners.yml: random safe places in an area or fixed points,
 * weighted crystal types, intervals, per-spawner and global limits, lifetime and announcements.
 */
public class SpawnerManager {

    public enum Mode { RANDOM, POINTS }

    public static final class Spawner {
        final String id;
        boolean enabled;
        Mode mode;
        String world;
        boolean circle;
        double centerX, centerZ, radius, minX, minZ, maxX, maxZ;
        int minY, maxY;
        boolean avoidLiquids;
        double minDistance;
        final LinkedHashMap<String, Integer> types = new LinkedHashMap<>();
        final List<String> points = new ArrayList<>();
        IntRange interval;
        int maxAlive;
        long lifetimeSeconds;
        boolean spawnOnStart;
        boolean announceSpawn, announceDespawn, announceDestroy;
        int warningSeconds;
        double announceRange;
        String sound;

        // Runtime state
        long nextSpawnAt;
        boolean warned;
        String plannedType;
        boolean searching;

        Spawner(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public boolean isEnabled() { return enabled; }
        public Mode getMode() { return mode; }
        public int getMaxAlive() { return maxAlive; }
        public long getNextSpawnAt() { return nextSpawnAt; }
        public List<String> getPoints() { return Collections.unmodifiableList(points); }
    }

    private static final int RANDOM_ATTEMPTS = 12;

    private final SimpleMetin plugin;
    private final CrystalManager crystalManager;
    private final Random random = new Random();
    private final Map<String, Spawner> spawners = new LinkedHashMap<>();
    private File file;
    private YamlConfiguration yaml = new YamlConfiguration();
    private BukkitTask task;
    private volatile boolean stopped;

    public SpawnerManager(SimpleMetin plugin, CrystalManager crystalManager) {
        this.plugin = plugin;
        this.crystalManager = crystalManager;
    }

    // ── Loading ─────────────────────────────────────────────────

    public void load() {
        file = new File(plugin.getDataFolder(), "spawners.yml");
        try {
            ConfigUpdater.saveIfMissing("spawners.yml", file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create spawners.yml: " + e.getMessage());
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        loadFrom(yaml);
    }

    /** Parses spawner definitions; runtime timers of spawners that still exist are kept. */
    public void loadFrom(YamlConfiguration source) {
        this.yaml = source;
        var previous = new HashMap<>(spawners);
        spawners.clear();

        var section = source.getConfigurationSection("spawners");
        if (section == null) return;

        long now = System.currentTimeMillis();
        for (var id : section.getKeys(false)) {
            var s = section.getConfigurationSection(id);
            if (s == null) continue;
            var spawner = parse(id, s);
            if (spawner == null) continue;

            var old = previous.get(id);
            if (old != null && old.nextSpawnAt > 0) {
                spawner.nextSpawnAt = old.nextSpawnAt;
                spawner.warned = old.warned;
                spawner.plannedType = old.plannedType;
            } else {
                spawner.nextSpawnAt = spawner.spawnOnStart ? now : now + spawner.interval.roll(random) * 1000L;
            }
            spawners.put(id, spawner);
        }
    }

    private Spawner parse(String id, ConfigurationSection s) {
        var sp = new Spawner(id);
        sp.enabled = s.getBoolean("enabled", true);
        sp.mode = "points".equalsIgnoreCase(s.getString("mode", "random")) ? Mode.POINTS : Mode.RANDOM;
        sp.world = s.getString("world", "world");

        var area = s.getConfigurationSection("area");
        if (area != null) {
            sp.circle = !"rectangle".equalsIgnoreCase(area.getString("shape", "circle"));
            sp.centerX = area.getDouble("center-x", 0);
            sp.centerZ = area.getDouble("center-z", 0);
            sp.radius = Math.max(0, area.getDouble("radius", 100));
            sp.minX = Math.min(area.getDouble("min-x", -100), area.getDouble("max-x", 100));
            sp.maxX = Math.max(area.getDouble("min-x", -100), area.getDouble("max-x", 100));
            sp.minZ = Math.min(area.getDouble("min-z", -100), area.getDouble("max-z", 100));
            sp.maxZ = Math.max(area.getDouble("min-z", -100), area.getDouble("max-z", 100));
            sp.minY = area.getInt("min-y", -64);
            sp.maxY = area.getInt("max-y", 320);
        } else {
            sp.circle = true;
            sp.radius = 100;
            sp.minY = -64;
            sp.maxY = 320;
        }
        sp.avoidLiquids = s.getBoolean("avoid-liquids", true);
        sp.minDistance = Math.max(0, s.getDouble("min-distance", 0));

        var types = s.getConfigurationSection("types");
        if (types != null) {
            for (var type : types.getKeys(false)) {
                int weight = types.getInt(type, 1);
                if (weight > 0) sp.types.put(type, weight);
            }
        }
        if (sp.types.isEmpty()) {
            plugin.getLogger().warning("Spawner '" + id + "' has no crystal types - skipped");
            return null;
        }

        sp.points.addAll(s.getStringList("points"));
        sp.interval = IntRange.parse(s.get("interval"), IntRange.of(600));
        if (sp.interval.min() < 1) sp.interval = new IntRange(1, Math.max(1, sp.interval.max()));
        sp.maxAlive = Math.max(0, s.getInt("max-alive", 1));
        sp.lifetimeSeconds = Math.max(0, s.getLong("lifetime", 0));
        sp.spawnOnStart = s.getBoolean("spawn-on-start", false);
        sp.announceSpawn = s.getBoolean("announce.spawn", true);
        sp.announceDespawn = s.getBoolean("announce.despawn", true);
        sp.announceDestroy = s.getBoolean("announce.destroy", true);
        sp.warningSeconds = Math.max(0, s.getInt("announce.warning", 0));
        sp.announceRange = Math.max(0, s.getDouble("announce.range", 0));
        sp.sound = s.getString("announce.sound", "");
        return sp;
    }

    public void start() {
        stopped = false;
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void shutdown() {
        stopped = true;
        if (task != null) task.cancel();
    }

    public Collection<Spawner> getSpawners() {
        return spawners.values();
    }

    public Spawner getSpawner(String id) {
        return spawners.get(id);
    }

    // ── Tick ────────────────────────────────────────────────────

    public void tick() {
        if (stopped) return;
        long now = System.currentTimeMillis();
        expireCrystals(now);

        if (!plugin.getConfig().getBoolean("auto-spawn.enabled", true)) return;
        if (Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("auto-spawn.min-players-online", 1)) return;

        for (var sp : spawners.values()) {
            if (!sp.enabled || sp.searching) continue;

            if (!sp.warned && sp.warningSeconds > 0 && now >= sp.nextSpawnAt - sp.warningSeconds * 1000L
                    && now < sp.nextSpawnAt && hasRoom(sp)) {
                sp.plannedType = pickType(sp);
                sp.warned = true;
                if (sp.plannedType != null) {
                    long seconds = Math.max(1, (sp.nextSpawnAt - now + 999) / 1000);
                    announce(sp, null, "spawner-warning", "spawner", sp.id,
                            "display_name", typeDisplayName(sp.plannedType), "type", sp.plannedType,
                            "time", formatTime(seconds));
                }
            }

            if (now < sp.nextSpawnAt) continue;

            var type = sp.plannedType != null ? sp.plannedType : pickType(sp);
            sp.nextSpawnAt = now + sp.interval.roll(random) * 1000L;
            sp.warned = false;
            sp.plannedType = null;

            if (type != null && hasRoom(sp)) {
                spawnFrom(sp, type, null, true);
            }
        }
    }

    private void expireCrystals(long now) {
        for (var data : new ArrayList<>(crystalManager.getAllCrystals())) {
            if (data.getExpiresAt() <= 0 || now < data.getExpiresAt() || data.isDestroyed()) continue;

            var sp = data.getSpawnerId() != null ? spawners.get(data.getSpawnerId()) : null;
            var displayName = crystalManager.getDisplayName(data);
            var location = data.getLocation();
            crystalManager.removeCrystal(data.getId());
            plugin.getLogger().info("Crystal " + data.getId() + " vanished (lifetime over)");

            if (sp == null || sp.announceDespawn) {
                announce(sp, location, "spawner-despawned", locationPlaceholders(data, displayName, sp));
            }
        }
    }

    private boolean hasRoom(Spawner sp) {
        if (crystalManager.countSpawnerCrystals(sp.id) >= sp.maxAlive) return false;
        int globalMax = plugin.getConfig().getInt("auto-spawn.max-alive-total", 0);
        return globalMax <= 0 || crystalManager.countSpawnerCrystals(null) < globalMax;
    }

    /** Weighted random crystal type among types that exist in config.yml. */
    String pickType(Spawner sp) {
        var available = new LinkedHashMap<String, Integer>();
        for (var entry : sp.types.entrySet()) {
            if (plugin.getConfig().getConfigurationSection("crystals." + entry.getKey()) != null) {
                available.put(entry.getKey(), entry.getValue());
            }
        }
        if (available.isEmpty()) {
            plugin.getLogger().warning("Spawner '" + sp.id + "': none of its crystal types exist in config.yml");
            return null;
        }
        int total = available.values().stream().mapToInt(Integer::intValue).sum();
        int target = random.nextInt(total);
        for (var entry : available.entrySet()) {
            target -= entry.getValue();
            if (target < 0) return entry.getKey();
        }
        return available.keySet().iterator().next();
    }

    // ── Spawning ────────────────────────────────────────────────

    /**
     * Spawns one crystal from the spawner (timer and limits are the caller's business).
     * The callback receives the crystal, or null if no valid location was found.
     */
    public void spawnFrom(Spawner sp, String type, Consumer<CrystalData> callback) {
        spawnFrom(sp, type, callback, false);
    }

    private void spawnFrom(Spawner sp, String type, Consumer<CrystalData> callback, boolean automatic) {
        if (stopped || spawners.get(sp.id) != sp || sp.searching) {
            if (callback != null) callback.accept(null);
            return;
        }
        if (type == null) type = pickType(sp);
        if (type == null) {
            if (callback != null) callback.accept(null);
            return;
        }
        final String chosenType = type;

        Consumer<Location> place = location -> {
            sp.searching = false;
            // Chunk loading may finish after reload/shutdown, or after another spawner fills the limit.
            if (stopped || spawners.get(sp.id) != sp || (automatic && (!sp.enabled
                    || !plugin.getConfig().getBoolean("auto-spawn.enabled", true)
                    || Bukkit.getOnlinePlayers().size() < plugin.getConfig().getInt("auto-spawn.min-players-online", 1)
                    || !hasRoom(sp)))) {
                if (callback != null) callback.accept(null);
                return;
            }
            if (location == null) {
                plugin.getLogger().warning("Spawner '" + sp.id + "' could not find a valid location");
                if (callback != null) callback.accept(null);
                return;
            }
            var data = crystalManager.spawnCrystal(crystalManager.generateId(chosenType), chosenType, location, null);
            if (data == null) {
                if (callback != null) callback.accept(null);
                return;
            }
            data.setSpawnerId(sp.id);
            if (sp.lifetimeSeconds > 0) {
                data.setExpiresAt(System.currentTimeMillis() + sp.lifetimeSeconds * 1000L);
            }
            if (sp.announceSpawn) {
                announce(sp, location, "spawner-spawned", locationPlaceholders(data, crystalManager.getDisplayName(data), sp));
                playSound(sp, location);
            }
            if (callback != null) callback.accept(data);
        };

        sp.searching = true;
        if (sp.mode == Mode.POINTS) {
            place.accept(pickPoint(sp));
        } else {
            var world = Bukkit.getWorld(sp.world);
            if (world == null) {
                plugin.getLogger().warning("Spawner '" + sp.id + "': world '" + sp.world + "' is not loaded");
                place.accept(null);
                return;
            }
            findRandomLocation(sp, world, RANDOM_ATTEMPTS, place);
        }
    }

    private Location pickPoint(Spawner sp) {
        var free = new ArrayList<Location>();
        for (var point : sp.points) {
            var location = parsePoint(point);
            if (location != null && isFarEnough(location, Math.max(1.5, sp.minDistance))) {
                free.add(location);
            }
        }
        return free.isEmpty() ? null : free.get(random.nextInt(free.size()));
    }

    /** Picks random columns until a safe one is found; chunks are loaded asynchronously, checks run on the main thread. */
    private void findRandomLocation(Spawner sp, World world, int attemptsLeft, Consumer<Location> done) {
        if (attemptsLeft <= 0) {
            done.accept(null);
            return;
        }
        int x, z;
        if (sp.circle) {
            double r = sp.radius * Math.sqrt(random.nextDouble());
            double angle = random.nextDouble() * Math.PI * 2;
            x = (int) Math.floor(sp.centerX + r * Math.cos(angle));
            z = (int) Math.floor(sp.centerZ + r * Math.sin(angle));
        } else {
            x = (int) Math.floor(sp.minX + random.nextDouble() * (sp.maxX - sp.minX));
            z = (int) Math.floor(sp.minZ + random.nextDouble() * (sp.maxZ - sp.minZ));
        }

        world.getChunkAtAsync(x >> 4, z >> 4).whenComplete((chunk, error) -> {
            if (stopped) return;
            Runnable finish = () -> {
                if (stopped || spawners.get(sp.id) != sp || error != null) {
                    done.accept(null);
                    return;
                }
                var location = checkGround(sp, world, x, z);
                if (location != null) {
                    done.accept(location);
                } else {
                    findRandomLocation(sp, world, attemptsLeft - 1, done);
                }
            };
            if (Bukkit.isPrimaryThread()) finish.run();
            else Bukkit.getScheduler().runTask(plugin, finish);
        });
    }

    Location checkGround(Spawner sp, World world, int x, int z) {
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (y < sp.minY || y > sp.maxY) return null;

        var ground = world.getBlockAt(x, y, z);
        if (ground.isLiquid() && sp.avoidLiquids) return null;
        if (!ground.isSolid()) return null;

        var above = world.getBlockAt(x, y + 1, z);
        var above2 = world.getBlockAt(x, y + 2, z);
        if (!above.isPassable() || !above2.isPassable() || above.isLiquid() || above2.isLiquid()) return null;

        var location = new Location(world, x + 0.5, y + 1, z + 0.5);
        return isFarEnough(location, sp.minDistance) ? location : null;
    }

    private boolean isFarEnough(Location location, double minDistance) {
        if (minDistance <= 0) return true;
        return crystalManager.getNearest(location, minDistance) == null;
    }

    public void onCrystalDestroyed(CrystalData data, Player killer) {
        var sp = spawners.get(data.getSpawnerId());
        if (sp != null && !sp.announceDestroy) return;
        var placeholders = new ArrayList<>(Arrays.asList(locationPlaceholders(data, crystalManager.getDisplayName(data), sp)));
        placeholders.add("player");
        placeholders.add(killer.getName());
        announce(sp, data.getLocation(), "spawner-destroyed", placeholders.toArray());
    }

    // ── Points ──────────────────────────────────────────────────

    /** Adds a point and saves spawners.yml; returns the 1-based index. */
    public int addPoint(Spawner sp, Location location) throws IOException {
        var point = String.format(Locale.ROOT, "%s %.1f %.1f %.1f", location.getWorld().getName(),
                location.getBlockX() + 0.5, (double) location.getBlockY(), location.getBlockZ() + 0.5);
        sp.points.add(point);
        savePoints(sp);
        return sp.points.size();
    }

    public boolean removePoint(Spawner sp, int index) throws IOException {
        if (index < 1 || index > sp.points.size()) return false;
        sp.points.remove(index - 1);
        savePoints(sp);
        return true;
    }

    private void savePoints(Spawner sp) throws IOException {
        yaml.set("spawners." + sp.id + ".points", new ArrayList<>(sp.points));
        if (file != null) yaml.save(file);
    }

    /** "world x y z" -> location; null if malformed or the world is not loaded. */
    public static Location parsePoint(String point) {
        var parts = point.trim().split("\\s+");
        if (parts.length < 4) return null;
        var world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ── Announcements ───────────────────────────────────────────

    private Object[] locationPlaceholders(CrystalData data, String displayName, Spawner sp) {
        var l = data.getLocation();
        return new Object[]{"spawner", sp != null ? sp.id : "-", "display_name", displayName, "type", data.getConfigId(),
                "id", data.getId(), "world", data.getWorldName(), "x", l.getBlockX(), "y", l.getBlockY(), "z", l.getBlockZ()};
    }

    private String typeDisplayName(String type) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + type);
        return config != null ? config.getString("display-name", type) : type;
    }

    /** Sends to everyone (range 0) or to players within the spawner's announce range of the location, plus the console. */
    private void announce(Spawner sp, Location location, String key, Object... placeholders) {
        var text = plugin.getMessages().raw(key, placeholders);
        if (text.isEmpty()) return;
        var component = TextUtils.parse(text);

        for (var player : recipients(sp, location)) {
            player.sendMessage(component);
        }
        CommandSender console = Bukkit.getConsoleSender();
        if (console != null) console.sendMessage(component);
    }

    private Collection<? extends Player> recipients(Spawner sp, Location location) {
        if (sp == null || sp.announceRange <= 0 || location == null || location.getWorld() == null) {
            return Bukkit.getOnlinePlayers();
        }
        var list = new ArrayList<Player>();
        double rangeSq = sp.announceRange * sp.announceRange;
        for (var player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= rangeSq) list.add(player);
        }
        return list;
    }

    private void playSound(Spawner sp, Location location) {
        if (sp.sound == null || sp.sound.isEmpty()) return;
        Sound sound;
        try {
            sound = Sound.valueOf(sp.sound.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Spawner '" + sp.id + "': invalid sound " + sp.sound);
            return;
        }
        for (var player : recipients(sp, location)) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public static String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
