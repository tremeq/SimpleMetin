package me.simplemetin.utils;

import org.bukkit.Particle;

import java.util.Locale;
import java.util.Map;

public final class ParticleUtils {

    /** Particle names renamed in 1.20.5; old configs keep working on servers without Paper's legacy mapping. */
    private static final Map<String, String> LEGACY_NAMES = Map.ofEntries(
            Map.entry("EXPLOSION_HUGE", "EXPLOSION_EMITTER"),
            Map.entry("EXPLOSION_LARGE", "EXPLOSION"),
            Map.entry("EXPLOSION_NORMAL", "POOF"),
            Map.entry("FIREWORKS_SPARK", "FIREWORK"),
            Map.entry("WATER_BUBBLE", "BUBBLE"),
            Map.entry("WATER_SPLASH", "SPLASH"),
            Map.entry("WATER_WAKE", "FISHING"),
            Map.entry("WATER_DROP", "RAIN"),
            Map.entry("SUSPENDED", "UNDERWATER"),
            Map.entry("SUSPENDED_DEPTH", "UNDERWATER"),
            Map.entry("CRIT_MAGIC", "ENCHANTED_HIT"),
            Map.entry("SMOKE_NORMAL", "SMOKE"),
            Map.entry("SMOKE_LARGE", "LARGE_SMOKE"),
            Map.entry("SPELL", "EFFECT"),
            Map.entry("SPELL_INSTANT", "INSTANT_EFFECT"),
            Map.entry("SPELL_WITCH", "WITCH"),
            Map.entry("DRIP_WATER", "DRIPPING_WATER"),
            Map.entry("DRIP_LAVA", "DRIPPING_LAVA"),
            Map.entry("VILLAGER_ANGRY", "ANGRY_VILLAGER"),
            Map.entry("VILLAGER_HAPPY", "HAPPY_VILLAGER"),
            Map.entry("TOWN_AURA", "MYCELIUM"),
            Map.entry("ENCHANTMENT_TABLE", "ENCHANT"),
            Map.entry("SNOWBALL", "ITEM_SNOWBALL"),
            Map.entry("SLIME", "ITEM_SLIME"),
            Map.entry("TOTEM", "TOTEM_OF_UNDYING")
    );

    private ParticleUtils() {
    }

    /** Resolves a particle by its current or pre-1.20.5 name; null if unknown. */
    public static Particle resolve(String name) {
        if (name == null) return null;
        var upper = name.toUpperCase(Locale.ROOT);
        try {
            return Particle.valueOf(upper);
        } catch (IllegalArgumentException ignored) {
        }
        var renamed = LEGACY_NAMES.get(upper);
        if (renamed == null) return null;
        try {
            return Particle.valueOf(renamed);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
