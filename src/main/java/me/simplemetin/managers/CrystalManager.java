package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.data.DataHandler;
import me.simplemetin.events.MetinCrystalDestroyedEvent;
import me.simplemetin.models.CrystalData;
import me.simplemetin.models.CrystalType;
import me.simplemetin.models.DropTable;
import me.simplemetin.utils.DropUtils;
import me.simplemetin.utils.ParticleUtils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class CrystalManager {
    public static final String CRYSTAL_TAG = "simplemetin";

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    private final SimpleMetin plugin;
    private final HologramManager hologramManager;
    private final DataHandler dataHandler;
    private final Map<String, CrystalData> crystals = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityToCrystal = new ConcurrentHashMap<>();
    private final Map<String, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private final Set<String> missingConfigWarned = ConcurrentHashMap.newKeySet();
    /** Parsed drop tables per "configId:hit|death", cleared on reload and when drops are edited. */
    private final Map<String, DropTable> dropTables = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private BukkitTask updateTask;
    private BukkitTask saveTask;

    public CrystalManager(SimpleMetin plugin, HologramManager hologramManager, DataHandler dataHandler) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.dataHandler = dataHandler;
    }

    // ── IDs ─────────────────────────────────────────────────────

    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id).matches();
    }

    public static boolean isLegacyUuidId(String id) {
        return id != null && UUID_PATTERN.matcher(id).matches();
    }

    public boolean isIdTaken(String id) {
        return crystals.containsKey(id) || dataHandler.getPendingIds().contains(id);
    }

    /** Short type name used in IDs: "metin_common" -> "common". */
    public static String shortTypeName(String configId) {
        var name = configId.toLowerCase(Locale.ROOT);
        if (name.startsWith("metin_") && name.length() > 6) name = name.substring(6);
        name = name.replaceAll("[^a-z0-9_-]", "_");
        return name.length() > 26 ? name.substring(0, 26) : name;
    }

    /** First free "&lt;type&gt;-&lt;n&gt;" ID, e.g. common-1, common-2... */
    public String generateId(String configId) {
        var base = shortTypeName(configId);
        for (int n = 1; ; n++) {
            var id = base + "-" + n;
            if (!isIdTaken(id)) return id;
        }
    }

    // ── Spawning ────────────────────────────────────────────────

    public CrystalData spawnCrystal(String id, String configId, Location location, String overrideName) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + configId);
        if (config == null) {
            plugin.getLogger().warning("Crystal config not found: " + configId);
            return null;
        }

        var typeStr = config.getString("type", "one-time");
        var type = typeStr.equalsIgnoreCase("respawn") ? CrystalType.RESPAWN : CrystalType.ONE_TIME;
        var maxHp = config.getInt("max-hp", 100);

        var data = new CrystalData(id, configId, location, type, maxHp, overrideName);

        crystals.put(id, data);
        spawnEntity(data);

        plugin.getLogger().info("Spawned crystal: " + id + " (type: " + configId + ") at " +
                                data.getWorldName() + " " + location.getBlockX() + "," +
                                location.getBlockY() + "," + location.getBlockZ());
        return data;
    }

    private void spawnEntity(CrystalData data) {
        if (plugin.getConfig().getBoolean("settings.bossbar-enabled", true)) {
            createBossBar(data);
        }
        spawnWorldEntities(data);
    }

    /**
     * Spawns the crystal entity and its hologram if the chunk is loaded; otherwise this happens on chunk load.
     * Both are non-persistent, so they are never written to the chunk and cannot be duplicated by a restart or crash.
     */
    private void spawnWorldEntities(CrystalData data) {
        var world = getLoadedWorld(data);
        if (world == null) return;

        despawnWorldEntities(data);

        var crystal = world.spawn(data.getLocation(), EnderCrystal.class, c -> {
            c.setShowingBottom(false);
            c.setInvulnerable(false);
            c.setPersistent(false);
            c.addScoreboardTag(CRYSTAL_TAG);
            c.addScoreboardTag("metin_" + data.getConfigId());
        });

        data.setEntity(crystal);
        entityToCrystal.put(crystal.getUniqueId(), data.getId());

        hologramManager.createHologram(data);
    }

    private void despawnWorldEntities(CrystalData data) {
        var entity = data.getEntity();
        if (entity != null) {
            entityToCrystal.remove(entity.getUniqueId());
            if (!entity.isDead()) {
                entity.remove();
            }
            data.setEntity(null);
        }
        hologramManager.removeHologram(data);
    }

    /** Returns the crystal's world if it and the crystal's chunk are loaded, otherwise null. */
    private World getLoadedWorld(CrystalData data) {
        if (data.getWorldName() == null) return null;
        var world = Bukkit.getWorld(data.getWorldName());
        if (world == null || !world.isChunkLoaded(data.getChunkX(), data.getChunkZ())) return null;
        return world;
    }

    private boolean isInChunk(CrystalData data, Chunk chunk) {
        return chunk.getWorld().getName().equals(data.getWorldName())
                && chunk.getX() == data.getChunkX()
                && chunk.getZ() == data.getChunkZ();
    }

    private boolean hasLiveEntity(CrystalData data) {
        return data.getEntity() != null && data.getEntity().isValid();
    }

    // ── Chunk lifecycle ─────────────────────────────────────────

    public void handleChunkLoad(Chunk chunk) {
        var worldName = chunk.getWorld().getName();
        int x = chunk.getX();
        int z = chunk.getZ();
        // One tick later: the chunk is fully loaded and may already have been unloaded again
        Bukkit.getScheduler().runTask(plugin, () -> spawnMissingEntities(worldName, x, z));
    }

    public void spawnMissingEntities(String worldName, int chunkX, int chunkZ) {
        for (var data : crystals.values()) {
            if (data.isDestroyed() || hasLiveEntity(data)) continue;
            if (!worldName.equals(data.getWorldName()) || data.getChunkX() != chunkX || data.getChunkZ() != chunkZ) continue;
            spawnWorldEntities(data);
        }
    }

    public void handleChunkUnload(Chunk chunk) {
        for (var data : crystals.values()) {
            if (isInChunk(data, chunk)) {
                despawnWorldEntities(data);
            }
        }
    }

    /**
     * Removes metin crystals and holograms that are not tracked by the plugin: copies left in the world by
     * versions that saved them with the chunk, and ArmorStand holograms of versions before TextDisplay holograms.
     * Returns the number of removed entities.
     */
    public int removeOrphans(Collection<? extends Entity> entities) {
        int removed = 0;
        for (var entity : entities) {
            if (entity.isDead()) continue;

            boolean orphan = false;
            if (entity instanceof EnderCrystal && entity.getScoreboardTags().contains(CRYSTAL_TAG)) {
                orphan = !entityToCrystal.containsKey(entity.getUniqueId());
            } else if (entity instanceof TextDisplay && entity.getScoreboardTags().contains(HologramManager.HOLOGRAM_TAG)) {
                orphan = !hologramManager.isTracked(entity.getUniqueId());
            } else if (entity instanceof ArmorStand stand) {
                // Holograms are TextDisplays now: every metin ArmorStand is a leftover
                orphan = stand.getScoreboardTags().contains(HologramManager.HOLOGRAM_TAG) || isLegacyHologram(stand);
            }

            if (orphan) {
                entity.remove();
                removed++;
            }
        }
        if (removed > 0) {
            plugin.getLogger().info("Removed " + removed + " orphaned metin entities");
        }
        return removed;
    }

    /** Holograms from the first versions had no tag: invisible marker stands right above a metin location. */
    private boolean isLegacyHologram(ArmorStand stand) {
        if (!stand.isMarker() || stand.isVisible() || !stand.isCustomNameVisible()) return false;

        var loc = stand.getLocation();
        if (loc.getWorld() == null) return false;
        var worldName = loc.getWorld().getName();
        for (var data : crystals.values()) {
            var c = data.getLocation();
            if (!worldName.equals(data.getWorldName())) continue;
            double dy = loc.getY() - c.getY();
            if (Math.abs(loc.getX() - c.getX()) < 0.01 && Math.abs(loc.getZ() - c.getZ()) < 0.01 && dy > 1.0 && dy < 2.6) {
                return true;
            }
        }
        return false;
    }

    public void removeCrystal(String id) {
        var data = crystals.remove(id);
        if (data == null) return;

        despawnWorldEntities(data);
        removeBossBar(id);

        plugin.getLogger().info("Removed crystal: " + id);
    }

    /**
     * Config section of the crystal type. If the type was removed from config.yml the crystal keeps working
     * with defaults (no drops) instead of silently ignoring hits; the admin is warned once per type.
     */
    private ConfigurationSection getCrystalConfig(CrystalData data) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        if (config != null) return config;

        if (missingConfigWarned.add(data.getConfigId())) {
            plugin.getLogger().warning("Crystal type '" + data.getConfigId() + "' is missing in config.yml - crystals of this type use defaults and have no drops");
        }
        return new MemoryConfiguration();
    }

    /** Display name of a crystal: the override from /metin spawn or the type's display-name. */
    public String getDisplayName(CrystalData data) {
        if (data.getOverrideName() != null) return data.getOverrideName();
        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        return config != null ? config.getString("display-name", data.getConfigId()) : data.getConfigId();
    }

    public DropTable getDropTable(String configId, String moment) {
        return dropTables.computeIfAbsent(configId + ":" + moment, key -> DropTable.load(
                plugin.getConfig().getConfigurationSection("crystals." + configId), moment, plugin.getLogger()));
    }

    /** Drops cached drop tables (after editing drops in config.yml from a command). */
    public void clearDropCache() {
        dropTables.clear();
    }

    private boolean actionBarEnabled() {
        return plugin.getConfig().getBoolean("settings.actionbar-enabled", true);
    }

    public void clearCooldown(UUID playerUuid) {
        playerCooldowns.remove(playerUuid);
    }

    // ── Damage & destruction ────────────────────────────────────

    public void handleDamage(EnderCrystal crystal, Player damager) {
        var id = entityToCrystal.get(crystal.getUniqueId());
        if (id == null) return;

        var data = crystals.get(id);
        if (data == null || data.isDestroyed()) return;

        var messages = plugin.getMessages();
        var cooldownSeconds = plugin.getConfig().getDouble("settings.hit-cooldown", 1.0);
        var now = System.currentTimeMillis();
        var cooldownMillis = (long) (cooldownSeconds * 1000);

        var lastHit = playerCooldowns.get(damager.getUniqueId());
        if (lastHit != null) {
            var timeLeft = (lastHit + cooldownMillis) - now;
            if (timeLeft > 0) {
                if (actionBarEnabled()) {
                    messages.actionBar(damager, "hit-cooldown", "time", String.format(Locale.ROOT, "%.1f", timeLeft / 1000.0));
                }
                return;
            }
        }

        playerCooldowns.put(damager.getUniqueId(), now);

        var config = getCrystalConfig(data);

        // Strength below 1 (e.g. edited players.yml) would make the crystal unkillable while still rolling hit-drops
        var strength = Math.max(1, plugin.getPlayerStatsManager().getDamage(damager.getUniqueId()));
        var dealt = data.damage(strength);

        // Track stats (only damage actually dealt, no overkill)
        var stats = plugin.getStatsManager().getOrCreateStats(damager.getUniqueId());
        stats.addDamage(dealt);
        stats.updateLastSeen();

        if (actionBarEnabled() && config.getBoolean("show-actionbar", true)) {
            messages.actionBar(damager, "crystal-hit", "hp", data.getCurrentHp(), "max_hp", data.getMaxHp(),
                    "damage", dealt, "display_name", getDisplayName(data));
        }

        hologramManager.updateHologram(data);
        updateBossBar(data);

        var hit = rollRewards(data, "hit", damager);
        if (hit.itemsReceived() > 0) stats.addItemsReceived(hit.itemsReceived());
        if (hit.money() > 0) stats.addMoneyEarned(hit.money());

        if (data.isDead()) {
            handleDestruction(data, damager);
        }
    }

    private record Rewards(List<ItemStack> items, List<String> commands, int itemsReceived, long money) {
    }

    private Rewards rollRewards(CrystalData data, String moment, Player player) {
        var table = getDropTable(data.getConfigId(), moment);
        if (table.isEmpty()) return new Rewards(List.of(), List.of(), 0, 0);

        double boost = plugin.getBoostManager().getTotalMultiplier(player);
        var customItems = plugin.getCustomItemManager();
        var result = table.roll(random, boost, key -> customItems != null ? customItems.get(key) : null, plugin.getLogger());

        int received = DropUtils.giveItems(result.items(), data.getLocation(), player, plugin);
        long money = DropUtils.runCommands(result.commands(), player);
        var ranCommands = result.commands().stream().map(c -> c.replace("%player%", player.getName())).toList();
        return new Rewards(result.items(), ranCommands, received, money);
    }

    private void handleDestruction(CrystalData data, Player killer) {
        data.setDestroyed(true);

        var config = getCrystalConfig(data);

        // Track stats
        var stats = plugin.getStatsManager().getOrCreateStats(killer.getUniqueId());
        stats.addCrystalDestroyed(data.getConfigId());
        stats.updateLastSeen();

        var death = rollRewards(data, "death", killer);
        if (death.itemsReceived() > 0) stats.addItemsReceived(death.itemsReceived());
        if (death.money() > 0) stats.addMoneyEarned(death.money());

        playDeathEffects(data, config);

        plugin.getMessages().send(killer, "crystal-destroyed", "display_name", getDisplayName(data),
                "id", data.getId(), "player", killer.getName());

        var event = new MetinCrystalDestroyedEvent(data, killer, death.items(), death.commands());
        Bukkit.getPluginManager().callEvent(event);

        despawnWorldEntities(data);
        removeBossBar(data.getId());

        if (data.getSpawnerId() != null) {
            // Spawners create new crystals themselves: their crystals never respawn in place
            crystals.remove(data.getId());
            var spawners = plugin.getSpawnerManager();
            if (spawners != null) spawners.onCrystalDestroyed(data, killer);
            plugin.getLogger().info("Spawner crystal " + data.getId() + " destroyed");
        } else if (data.getType() == CrystalType.RESPAWN) {
            var respawnSeconds = config.getInt("respawn-time", 300);
            data.setRespawnTime(System.currentTimeMillis() + (respawnSeconds * 1000L));
            plugin.getLogger().info("Crystal " + data.getId() + " will respawn in " + respawnSeconds + " seconds");
        } else {
            crystals.remove(data.getId());
            plugin.getLogger().info("One-time crystal " + data.getId() + " has been permanently destroyed");
        }
    }

    private void playDeathEffects(CrystalData data, ConfigurationSection config) {
        var effectsSection = config.getConfigurationSection("death-effects");
        if (effectsSection == null) return;

        var location = data.getLocation();
        var world = location.getWorld();
        if (world == null) return;

        var particleStr = effectsSection.getString("particle", "EXPLOSION_EMITTER");
        var particle = ParticleUtils.resolve(particleStr);
        if (particle == null) {
            plugin.getLogger().warning("Invalid particle type: " + particleStr);
        } else {
            try {
                world.spawnParticle(particle, location, 50, 1, 1, 1, 0.1);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Particle " + particleStr + " needs extra data and cannot be used as a death effect");
            }
        }

        var soundStr = effectsSection.getString("sound", "ENTITY_GENERIC_EXPLODE");
        try {
            var sound = Sound.valueOf(soundStr);
            var volume = (float) effectsSection.getDouble("volume", 1.0);
            var pitch = (float) effectsSection.getDouble("pitch", 1.0);
            world.playSound(location, sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid sound type: " + soundStr);
        }
    }

    // ── Boss bars ───────────────────────────────────────────────

    private String bossBarTitle(CrystalData data) {
        return plugin.getMessages().legacy("bossbar-crystal", "display_name", getDisplayName(data),
                "hp", data.getCurrentHp(), "max_hp", data.getMaxHp(), "id", data.getId());
    }

    private void createBossBar(CrystalData data) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        if (config == null || !config.getBoolean("show-bossbar", true)) return;

        removeBossBar(data.getId());

        var bossBar = Bukkit.createBossBar(bossBarTitle(data), BarColor.RED, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        bossBars.put(data.getId(), bossBar);
    }

    private void updateBossBar(CrystalData data) {
        var bossBar = bossBars.get(data.getId());
        if (bossBar == null) return;

        bossBar.setTitle(bossBarTitle(data));
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) data.getCurrentHp() / data.getMaxHp())));
    }

    private void removeBossBar(String id) {
        var bossBar = bossBars.remove(id);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    /** Re-applies config.yml to existing crystals and restarts the tasks with the new intervals (/metin reload). */
    public void reload() {
        missingConfigWarned.clear();
        clearDropCache();
        startUpdateTask();
        startAutoSave();

        boolean bossBarsEnabled = plugin.getConfig().getBoolean("settings.bossbar-enabled", true);
        for (var data : crystals.values()) {
            var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
            if (config != null) {
                data.setMaxHp(config.getInt("max-hp", data.getMaxHp()));
            }

            removeBossBar(data.getId());
            hologramManager.removeHologram(data);
            if (!data.isDestroyed()) {
                if (bossBarsEnabled) {
                    createBossBar(data);
                    updateBossBar(data);
                }
                // Countdown holograms of destroyed crystals are recreated by the update task
                if (getLoadedWorld(data) != null) {
                    hologramManager.createHologram(data);
                }
            }
        }
    }

    public void startUpdateTask() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        var interval = Math.max(1, plugin.getConfig().getLong("settings.hologram-update-interval", 20));
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            var now = System.currentTimeMillis();

            for (var data : crystals.values()) {
                if (data.isDestroyed() && data.getType() == CrystalType.RESPAWN) {
                    if (now >= data.getRespawnTime()) {
                        respawnCrystal(data);
                    } else {
                        hologramManager.updateRespawnHologram(data);
                    }
                } else if (!data.isDestroyed()) {
                    // Self-heal: entity removed by something that bypasses damage events (/kill, other plugins)
                    if (!hasLiveEntity(data) && getLoadedWorld(data) != null) {
                        spawnWorldEntities(data);
                    }
                    hologramManager.updateHologram(data);
                    updateBossBarPlayers(data);
                }
            }
        }, interval, interval);
    }

    private void updateBossBarPlayers(CrystalData data) {
        var bossBar = bossBars.get(data.getId());
        if (bossBar == null) return;

        var range = plugin.getConfig().getDouble("settings.bossbar-range", 30.0);
        var world = getLoadedWorld(data);
        if (world == null) return;
        var location = data.getLocation();

        var currentPlayers = new HashSet<>(bossBar.getPlayers());

        for (var player : world.getPlayers()) {
            if (player.getLocation().distance(location) <= range) {
                if (!currentPlayers.contains(player)) {
                    bossBar.addPlayer(player);
                }
                currentPlayers.remove(player);
            }
        }

        for (var player : currentPlayers) {
            bossBar.removePlayer(player);
        }
    }

    private void respawnCrystal(CrystalData data) {
        hologramManager.removeHologram(data);

        data.setCurrentHp(data.getMaxHp());
        data.setDestroyed(false);
        data.setRespawnTime(0);

        spawnEntity(data);

        plugin.getLogger().info("Crystal " + data.getId() + " has respawned");
    }

    public void startAutoSave() {
        if (saveTask != null) {
            saveTask.cancel();
        }
        var interval = Math.max(1, plugin.getConfig().getLong("settings.save-interval", 300)) * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            dataHandler.saveData();
            // stats.yml used to be written only on shutdown, so a crash lost the whole session
            plugin.getStatsManager().saveAllStats();
            plugin.getBoostManager().saveBoosts();
        }, interval, interval);
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
        }
        if (saveTask != null) {
            saveTask.cancel();
        }

        for (var bossBar : bossBars.values()) {
            bossBar.removeAll();
        }
        bossBars.clear();

        for (var data : crystals.values()) {
            despawnWorldEntities(data);
        }
    }

    // ── Lookup ──────────────────────────────────────────────────

    public CrystalData getCrystalById(String id) {
        return id == null ? null : crystals.get(id.toLowerCase(Locale.ROOT));
    }

    public CrystalData getCrystalByEntity(UUID entityUuid) {
        var id = entityToCrystal.get(entityUuid);
        return id != null ? crystals.get(id) : null;
    }

    public Collection<CrystalData> getAllCrystals() {
        return crystals.values();
    }

    /** Crystals sorted by ID in natural order (common-2 before common-10). */
    public List<CrystalData> getSortedCrystals() {
        var list = new ArrayList<>(crystals.values());
        list.sort(Comparator.comparing(CrystalData::getId, CrystalManager::compareIds));
        return list;
    }

    static int compareIds(String a, String b) {
        int dashA = a.lastIndexOf('-');
        int dashB = b.lastIndexOf('-');
        if (dashA > 0 && dashB > 0 && a.substring(0, dashA).equals(b.substring(0, dashB))) {
            try {
                return Integer.compare(Integer.parseInt(a.substring(dashA + 1)), Integer.parseInt(b.substring(dashB + 1)));
            } catch (NumberFormatException ignored) {
            }
        }
        return a.compareTo(b);
    }

    /** Nearest crystal in the same world within range, or null. */
    public CrystalData getNearest(Location location, double range) {
        if (location.getWorld() == null) return null;
        var worldName = location.getWorld().getName();
        CrystalData best = null;
        double bestDistance = range * range;
        for (var data : crystals.values()) {
            if (!worldName.equals(data.getWorldName())) continue;
            double d = data.getLocation().distanceSquared(location);
            if (d <= bestDistance) {
                best = data;
                bestDistance = d;
            }
        }
        return best;
    }

    /** Number of live (not destroyed) crystals created by the spawner; null = all spawners. */
    public int countSpawnerCrystals(String spawnerId) {
        int count = 0;
        for (var data : crystals.values()) {
            if (data.getSpawnerId() == null || data.isDestroyed()) continue;
            if (spawnerId == null || spawnerId.equals(data.getSpawnerId())) count++;
        }
        return count;
    }

    /**
     * Marks a crystal as destroyed, cleaning up its entity, hologram and bossbar.
     * Used by DataHandler when loading destroyed crystals from data.yml.
     */
    public void markCrystalDestroyed(String id) {
        var data = crystals.get(id);
        if (data == null) return;

        data.setDestroyed(true);

        despawnWorldEntities(data);
        removeBossBar(id);
    }

    public void manualRespawn(String id) {
        var data = getCrystalById(id);
        if (data != null && data.isDestroyed()) {
            respawnCrystal(data);
        }
    }
}
