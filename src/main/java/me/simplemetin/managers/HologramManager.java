package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import me.simplemetin.models.CrystalData;
import me.simplemetin.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Crystal holograms as a single TextDisplay entity (all lines in one entity, no ArmorStands):
 * cheaper for the server and the client, with configurable view range, background, shadow and see-through.
 */
public class HologramManager {
    public static final String HOLOGRAM_TAG = "simplemetin_hologram";

    private static final double DEFAULT_HEIGHT = 2.3;

    private final SimpleMetin plugin;
    private final Map<String, TextDisplay> holograms = new ConcurrentHashMap<>();
    /** Last raw text per hologram: the entity is only updated (and a packet sent) when the text changes. */
    private final Map<String, String> lastText = new ConcurrentHashMap<>();

    public HologramManager(SimpleMetin plugin) {
        this.plugin = plugin;
    }

    private ConfigurationSection config(CrystalData data) {
        return plugin.getConfig().getConfigurationSection("crystals." + data.getConfigId() + ".hologram");
    }

    public void createHologram(CrystalData data) {
        var config = config(data);
        if (config == null || !config.getBoolean("enabled", true)) return;

        var text = crystalText(data, config);
        if (text.isEmpty()) return;

        spawn(data, config, text);
    }

    public void updateHologram(CrystalData data) {
        var display = holograms.get(data.getId());
        if (display == null) return;

        var config = config(data);
        if (config == null) return;

        setText(data.getId(), display, crystalText(data, config));
    }

    public void updateRespawnHologram(CrystalData data) {
        var remaining = Math.max(0, (data.getRespawnTime() - System.currentTimeMillis()) / 1000);
        var text = plugin.getMessages().raw("respawn-countdown", "time", remaining);
        if (text.isEmpty()) return;

        var display = holograms.get(data.getId());
        if (display == null) {
            var config = config(data);
            if (config != null && !config.getBoolean("enabled", true)) return;
            spawn(data, config, text);
        } else {
            setText(data.getId(), display, text);
        }
    }

    public void removeHologram(CrystalData data) {
        lastText.remove(data.getId());
        var display = holograms.remove(data.getId());
        if (display != null && !display.isDead()) {
            display.remove();
        }
    }

    public boolean isTracked(UUID entityUuid) {
        for (var display : holograms.values()) {
            if (display.getUniqueId().equals(entityUuid)) return true;
        }
        return false;
    }

    public TextDisplay getHologram(String crystalId) {
        return holograms.get(crystalId);
    }

    private String crystalText(CrystalData data, ConfigurationSection config) {
        var lines = config.getStringList("lines");
        return String.join("\n", lines)
                .replace("%hp%", String.valueOf(data.getCurrentHp()))
                .replace("%max_hp%", String.valueOf(data.getMaxHp()))
                .replace("%id%", data.getId());
    }

    private void setText(String id, TextDisplay display, String text) {
        if (text.equals(lastText.get(id))) return;
        lastText.put(id, text);
        display.text(TextUtils.parse(text));
    }

    private void spawn(CrystalData data, ConfigurationSection config, String text) {
        if (data.getWorldName() == null) return;
        var world = Bukkit.getWorld(data.getWorldName());
        if (world == null || !world.isChunkLoaded(data.getChunkX(), data.getChunkZ())) return;

        removeHologram(data);

        double height = config != null ? config.getDouble("height", DEFAULT_HEIGHT) : DEFAULT_HEIGHT;
        double range = config != null ? config.getDouble("range", 0) : 0;
        var background = config != null ? config.getString("background", "#00000000") : "#00000000";
        boolean shadow = config == null || config.getBoolean("shadow", true);
        boolean seeThrough = config != null && config.getBoolean("see-through", false);

        var location = data.getLocation().clone().add(0, height, 0);
        var display = world.spawn(location, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(HOLOGRAM_TAG);
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(shadow);
            d.setSeeThrough(seeThrough);
            applyBackground(d, background);
            if (range > 0) {
                // View range is a multiplier of 64 blocks
                d.setViewRange((float) (range / 64.0));
            }
            d.text(TextUtils.parse(text));
        });

        holograms.put(data.getId(), display);
        lastText.put(data.getId(), text);
    }

    private void applyBackground(TextDisplay display, String background) {
        if (background == null || background.equalsIgnoreCase("default")) {
            display.setDefaultBackground(true);
            return;
        }
        display.setDefaultBackground(false);
        try {
            var hex = background.startsWith("#") ? background.substring(1) : background;
            long argb = Long.parseLong(hex, 16);
            if (hex.length() <= 6) argb |= 0xFF000000L; // RRGGBB = opaque
            display.setBackgroundColor(Color.fromARGB((int) argb));
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid hologram background '" + background + "', use #AARRGGBB or 'default'");
        }
    }
}
