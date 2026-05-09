package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.data.DataHandler;
import me.simplemetin.events.MetinCrystalDestroyedEvent;
import me.simplemetin.models.CrystalData;
import me.simplemetin.models.CrystalType;
import me.simplemetin.models.DropCommand;
import me.simplemetin.models.DropItem;
import me.simplemetin.utils.DropUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CrystalManager {
    private final SimpleMetin plugin;
    private final HologramManager hologramManager;
    private final DataHandler dataHandler;
    private final Map<String, CrystalData> crystals = new ConcurrentHashMap<>();
    private final Map<UUID, String> entityToCrystal = new ConcurrentHashMap<>();
    private final Map<String, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private BukkitTask updateTask;
    private BukkitTask saveTask;

    public CrystalManager(SimpleMetin plugin, HologramManager hologramManager, DataHandler dataHandler) {
        this.plugin = plugin;
        this.hologramManager = hologramManager;
        this.dataHandler = dataHandler;
    }

    public void spawnCrystal(String id, String configId, Location location, String overrideName) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + configId);
        if (config == null) {
            plugin.getLogger().warning("Crystal config not found: " + configId);
            return;
        }

        var typeStr = config.getString("type", "one-time");
        var type = typeStr.equalsIgnoreCase("respawn") ? CrystalType.RESPAWN : CrystalType.ONE_TIME;
        var maxHp = config.getInt("max-hp", 100);

        var data = new CrystalData(id, configId, location, type, maxHp, overrideName);

        spawnEntity(data);
        crystals.put(id, data);

        plugin.getLogger().info("Spawned crystal: " + id + " (type: " + configId + ") at " +
                                location.getWorld().getName() + " " + location.getBlockX() + "," +
                                location.getBlockY() + "," + location.getBlockZ());
    }

    private void spawnEntity(CrystalData data) {
        var world = data.getLocation().getWorld();
        if (world == null) return;

        var crystal = world.spawn(data.getLocation(), EnderCrystal.class, c -> {
            c.setShowingBottom(false);
            c.setInvulnerable(false);
        });

        data.setEntity(crystal);
        entityToCrystal.put(crystal.getUniqueId(), data.getId());

        hologramManager.createHologram(data);

        if (plugin.getConfig().getBoolean("settings.bossbar-enabled", true)) {
            createBossBar(data);
        }
    }

    public void removeCrystal(String id) {
        var data = crystals.remove(id);
        if (data == null) return;

        if (data.getEntity() != null && !data.getEntity().isDead()) {
            entityToCrystal.remove(data.getEntity().getUniqueId());
            data.getEntity().remove();
        }

        hologramManager.removeHologram(data);
        removeBossBar(id);

        plugin.getLogger().info("Removed crystal: " + id);
    }

    public void handleDamage(EnderCrystal crystal, Player damager) {
        var id = entityToCrystal.get(crystal.getUniqueId());
        if (id == null) return;

        var data = crystals.get(id);
        if (data == null || data.isDestroyed()) return;

        var cooldownSeconds = plugin.getConfig().getDouble("settings.hit-cooldown", 1.0);
        var now = System.currentTimeMillis();
        var cooldownMillis = (long) (cooldownSeconds * 1000);

        if (playerCooldowns.containsKey(damager.getUniqueId())) {
            var lastHit = playerCooldowns.get(damager.getUniqueId());
            var timeLeft = (lastHit + cooldownMillis) - now;

            if (timeLeft > 0) {
                String message = plugin.getConfig().getString("messages.hit-cooldown", "&cYou must wait &e%time%s &cbefore attacking again!");
                if (message != null && !message.isEmpty()) {
                    message = message.replace("%time%", String.format("%.1f", timeLeft / 1000.0));
                    damager.sendActionBar(net.kyori.adventure.text.Component.text(colorize(message)));
                }
                return;
            }
        }

        playerCooldowns.put(damager.getUniqueId(), now);

        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        if (config == null) return;

        var damagePerHit = config.getInt("damage-per-hit", 1);
        data.damage(damagePerHit);

        // Track stats
        var stats = plugin.getStatsManager().getOrCreateStats(damager.getUniqueId());
        stats.addDamage(damagePerHit);
        stats.updateLastSeen();

        if (config.getBoolean("show-actionbar", true)) {
            String message = plugin.getConfig().getString("messages.crystal-hit", "&7Crystal HP: &c%hp%&7/&c%max_hp%");
            if (message != null && !message.isEmpty()) {
                message = message.replace("%hp%", String.valueOf(data.getCurrentHp()))
                        .replace("%max_hp%", String.valueOf(data.getMaxHp()));
                damager.sendActionBar(net.kyori.adventure.text.Component.text(colorize(message)));
            }
        }

        hologramManager.updateHologram(data);
        updateBossBar(data);

        var hitDrops = loadDropItems(config.getConfigurationSection("hit-drops"));
        // Apply boost multiplier to drops
        var boostedDrops = applyBoostToDrops(hitDrops, damager);
        int itemsReceived = DropUtils.processDrops(boostedDrops, data.getLocation(), damager, plugin);

        // Track items received
        if (itemsReceived > 0) {
            stats.addItemsReceived(itemsReceived);
        }

        var hitCommands = loadDropCommands(config.getConfigurationSection("hit-commands"));
        long moneyEarned = DropUtils.executeCommands(hitCommands, damager);

        // Track money earned from commands
        if (moneyEarned > 0) {
            stats.addMoneyEarned(moneyEarned);
        }

        if (data.isDead()) {
            handleDestruction(data, damager);
        }
    }

    private void handleDestruction(CrystalData data, Player killer) {
        data.setDestroyed(true);

        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        if (config == null) return;

        // Track stats
        var stats = plugin.getStatsManager().getOrCreateStats(killer.getUniqueId());
        stats.addCrystalDestroyed(data.getConfigId());
        stats.updateLastSeen();

        var deathDrops = loadDropItems(config.getConfigurationSection("death-drops"));
        var boostedDeathDrops = applyBoostToDrops(deathDrops, killer);
        int deathItemsReceived = DropUtils.processDrops(boostedDeathDrops, data.getLocation(), killer, plugin);

        // Track items received from death drops
        if (deathItemsReceived > 0) {
            stats.addItemsReceived(deathItemsReceived);
        }

        var deathCommands = loadDropCommands(config.getConfigurationSection("death-commands"));
        long deathMoneyEarned = DropUtils.executeCommands(deathCommands, killer);

        // Track money earned from death commands
        if (deathMoneyEarned > 0) {
            stats.addMoneyEarned(deathMoneyEarned);
        }

        playDeathEffects(data, config);

        String displayName = data.getOverrideName() != null ? data.getOverrideName() :
                           config.getString("display-name", data.getConfigId());
        String message = plugin.getConfig().getString("messages.crystal-destroyed", "&e%display_name% &ahas been destroyed!");
        if (message != null && !message.isEmpty()) {
            message = message.replace("%display_name%", colorize(displayName));
            String prefix = plugin.getConfig().getString("messages.prefix", "");
            killer.sendMessage(colorize(prefix + message));
        }

        var event = new MetinCrystalDestroyedEvent(data, killer, new ArrayList<>(deathDrops));
        Bukkit.getPluginManager().callEvent(event);

        if (data.getEntity() != null && !data.getEntity().isDead()) {
            entityToCrystal.remove(data.getEntity().getUniqueId());
            data.getEntity().remove();
        }
        hologramManager.removeHologram(data);
        removeBossBar(data.getId());

        if (data.getType() == CrystalType.RESPAWN) {
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

        var particleStr = effectsSection.getString("particle", "EXPLOSION_HUGE");
        try {
            var particle = Particle.valueOf(particleStr);
            world.spawnParticle(particle, location, 50, 1, 1, 1, 0.1);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid particle type: " + particleStr);
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

    private List<DropItem> loadDropItems(ConfigurationSection section) {
        if (section == null) return Collections.emptyList();

        var drops = new ArrayList<DropItem>();
        for (var key : section.getKeys(false)) {
            var dropSection = section.getConfigurationSection(key);
            if (dropSection == null) continue;

            try {
                var material = org.bukkit.Material.valueOf(dropSection.getString("item", "STONE"));
                var amount = dropSection.getInt("amount", 1);
                var chance = dropSection.getDouble("chance", 100.0);
                var name = dropSection.getString("name");
                var lore = dropSection.getStringList("lore");

                drops.add(new DropItem(material, amount, chance, name, lore));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid drop item: " + key);
            }
        }
        return drops;
    }

    private List<DropCommand> loadDropCommands(ConfigurationSection section) {
        if (section == null) return Collections.emptyList();

        var commands = new ArrayList<DropCommand>();
        for (var key : section.getKeys(false)) {
            var cmdSection = section.getConfigurationSection(key);
            if (cmdSection == null) continue;

            var command = cmdSection.getString("command");
            var chance = cmdSection.getDouble("chance", 100.0);

            if (command != null) {
                commands.add(new DropCommand(command, chance));
            }
        }
        return commands;
    }

    private void createBossBar(CrystalData data) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        if (config == null || !config.getBoolean("show-bossbar", true)) return;

        removeBossBar(data.getId());

        String displayName = data.getOverrideName() != null ? data.getOverrideName() :
                           config.getString("display-name", data.getConfigId());
        var bossBar = Bukkit.createBossBar(
                colorize(displayName + " &7- &c" + data.getCurrentHp() + "&7/&c" + data.getMaxHp()),
                BarColor.RED,
                BarStyle.SOLID
        );
        bossBar.setProgress(1.0);
        bossBars.put(data.getId(), bossBar);
    }

    private void updateBossBar(CrystalData data) {
        var bossBar = bossBars.get(data.getId());
        if (bossBar == null) return;

        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId());
        if (config == null) return;

        String displayName = data.getOverrideName() != null ? data.getOverrideName() :
                           config.getString("display-name", data.getConfigId());
        bossBar.setTitle(colorize(displayName + " &7- &c" + data.getCurrentHp() + "&7/&c" + data.getMaxHp()));
        bossBar.setProgress(Math.max(0.0, Math.min(1.0, (double) data.getCurrentHp() / data.getMaxHp())));
    }

    private void removeBossBar(String id) {
        var bossBar = bossBars.remove(id);
        if (bossBar != null) {
            bossBar.removeAll();
        }
    }

    public void startUpdateTask() {
        var interval = plugin.getConfig().getLong("settings.hologram-update-interval", 20);
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
        var location = data.getLocation();
        var world = location.getWorld();
        if (world == null) return;

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
        var interval = plugin.getConfig().getLong("settings.save-interval", 300) * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, dataHandler::saveData, interval, interval);
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
            if (data.getEntity() != null && !data.getEntity().isDead()) {
                data.getEntity().remove();
            }
            hologramManager.removeHologram(data);
        }
    }

    public CrystalData getCrystalById(String id) {
        return crystals.get(id);
    }

    public CrystalData getCrystalByEntity(UUID entityUuid) {
        var id = entityToCrystal.get(entityUuid);
        return id != null ? crystals.get(id) : null;
    }

    public Collection<CrystalData> getAllCrystals() {
        return crystals.values();
    }

    public void manualRespawn(String id) {
        var data = crystals.get(id);
        if (data != null && data.isDestroyed()) {
            respawnCrystal(data);
        }
    }

    private List<DropItem> applyBoostToDrops(List<DropItem> drops, Player player) {
        if (drops == null || drops.isEmpty()) return drops;

        double multiplier = plugin.getBoostManager().getTotalMultiplier(player);
        if (multiplier <= 1.0) return drops;

        // Multiply chances by the boost multiplier
        return drops.stream()
                .map(drop -> new DropItem(
                        drop.material(),
                        drop.amount(),
                        Math.min(100.0, drop.chance() * multiplier), // Cap at 100%
                        drop.name(),
                        drop.lore()
                ))
                .toList();
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
