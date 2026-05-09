package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.models.CrystalData;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class HologramManager {
    private final SimpleMetin plugin;
    private final Map<String, List<ArmorStand>> holograms = new ConcurrentHashMap<>();

    public HologramManager(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    public void createHologram(CrystalData data) {
        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId() + ".hologram");
        if (config == null || !config.getBoolean("enabled", true)) return;

        var lines = config.getStringList("lines");
        if (lines.isEmpty()) return;

        var location = data.getLocation().clone().add(0, 2.5, 0);
        var stands = new ArrayList<ArmorStand>();

        for (int i = 0; i < lines.size(); i++) {
            var line = replacePlaceholders(lines.get(i), data);
            var standLoc = location.clone().subtract(0, i * 0.25, 0);
            var stand = createArmorStand(standLoc, line);
            stands.add(stand);
        }

        holograms.put(data.getId(), stands);
    }

    public void updateHologram(CrystalData data) {
        var stands = holograms.get(data.getId());
        if (stands == null) return;

        var config = plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId() + ".hologram");
        if (config == null) return;

        var lines = config.getStringList("lines");

        for (int i = 0; i < Math.min(lines.size(), stands.size()); i++) {
            var line = replacePlaceholders(lines.get(i), data);
            stands.get(i).setCustomName(colorize(line));
        }
    }

    public void updateRespawnHologram(CrystalData data) {
        var stands = holograms.get(data.getId());
        if (stands == null || stands.isEmpty()) {
            createRespawnHologram(data);
            return;
        }

        var remaining = (data.getRespawnTime() - System.currentTimeMillis()) / 1000;
        var message = plugin.getConfig().getString("messages.respawn-countdown", "&7Respawns in: &e%time%s")
                .replace("%time%", String.valueOf(Math.max(0, remaining)));

        stands.get(0).setCustomName(colorize(message));
    }

    private void createRespawnHologram(CrystalData data) {
        var location = data.getLocation().clone().add(0, 2.5, 0);
        var stands = new ArrayList<ArmorStand>();

        var remaining = (data.getRespawnTime() - System.currentTimeMillis()) / 1000;
        var message = plugin.getConfig().getString("messages.respawn-countdown", "&7Respawns in: &e%time%s")
                .replace("%time%", String.valueOf(Math.max(0, remaining)));

        var stand = createArmorStand(location, message);
        stands.add(stand);

        holograms.put(data.getId(), stands);
    }

    public void removeHologram(CrystalData data) {
        var stands = holograms.remove(data.getId());
        if (stands == null) return;

        for (var stand : stands) {
            if (!stand.isDead()) {
                stand.remove();
            }
        }
    }

    private ArmorStand createArmorStand(Location location, String text) {
        var world = location.getWorld();
        if (world == null) throw new IllegalStateException("World is null");

        return world.spawn(location, ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setCustomNameVisible(true);
            stand.setCustomName(colorize(text));
            stand.setMarker(true);
            stand.setInvulnerable(true);
        });
    }

    private String replacePlaceholders(String text, CrystalData data) {
        return text
                .replace("%hp%", String.valueOf(data.getCurrentHp()))
                .replace("%max_hp%", String.valueOf(data.getMaxHp()))
                .replace("%id%", data.getId());
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
