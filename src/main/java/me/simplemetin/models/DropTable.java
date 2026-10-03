package me.simplemetin.models;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Rewards of one moment (hit or destruction) of a crystal type:
 * independent entries (&lt;prefix&gt;-drops, &lt;prefix&gt;-commands) each rolled on their own chance, and
 * weighted pools (&lt;prefix&gt;-pools) that pick a number of entries by weight.
 */
public final class DropTable {

    public record Pool(String key, double chance, IntRange rolls, boolean unique, List<DropEntry> entries) {
    }

    /** What a roll produced: items to give and commands (with %player% still in them) to run. */
    public record Result(List<ItemStack> items, List<String> commands) {
        public static Result empty() {
            return new Result(List.of(), List.of());
        }
    }

    private static final DropTable EMPTY = new DropTable(List.of(), List.of());

    private final List<DropEntry> independent;
    private final List<Pool> pools;

    public DropTable(List<DropEntry> independent, List<Pool> pools) {
        this.independent = List.copyOf(independent);
        this.pools = List.copyOf(pools);
    }

    public static DropTable empty() {
        return EMPTY;
    }

    public List<DropEntry> getIndependent() {
        return independent;
    }

    public List<Pool> getPools() {
        return pools;
    }

    public boolean isEmpty() {
        return independent.isEmpty() && pools.isEmpty();
    }

    /** Loads "&lt;prefix&gt;-drops", "&lt;prefix&gt;-commands" and "&lt;prefix&gt;-pools" from a crystal type section. */
    public static DropTable load(ConfigurationSection crystal, String prefix, Logger logger) {
        if (crystal == null) return EMPTY;

        var independent = new ArrayList<DropEntry>();
        loadEntries(crystal.getConfigurationSection(prefix + "-drops"), independent, logger);
        loadEntries(crystal.getConfigurationSection(prefix + "-commands"), independent, logger);

        var pools = new ArrayList<Pool>();
        var poolsSection = crystal.getConfigurationSection(prefix + "-pools");
        if (poolsSection != null) {
            for (var key : poolsSection.getKeys(false)) {
                var section = poolsSection.getConfigurationSection(key);
                if (section == null) continue;
                var entries = new ArrayList<DropEntry>();
                loadEntries(section.getConfigurationSection("entries"), entries, logger);
                entries.removeIf(e -> e.weight() <= 0);
                if (entries.isEmpty()) {
                    logger.warning("Drop pool '" + key + "' has no entries with weight > 0");
                    continue;
                }
                pools.add(new Pool(key, section.getDouble("chance", 100.0),
                        IntRange.parse(section.get("rolls"), IntRange.of(1)),
                        section.getBoolean("unique", false), entries));
            }
        }
        return new DropTable(independent, pools);
    }

    private static void loadEntries(ConfigurationSection section, List<DropEntry> out, Logger logger) {
        if (section == null) return;
        for (var key : section.getKeys(false)) {
            var entry = section.getConfigurationSection(key);
            if (entry == null) continue;
            var parsed = parseEntry(key, entry, logger);
            if (parsed != null) out.add(parsed);
        }
    }

    static DropEntry parseEntry(String key, ConfigurationSection s, Logger logger) {
        var command = s.getString("command");
        var customItem = s.getString("custom-item");
        Material material = null;

        if (command == null && customItem == null) {
            var itemName = s.getString("item", "STONE");
            material = Material.matchMaterial(itemName);
            if (material == null || !material.isItem()) {
                logger.warning("Invalid drop item '" + itemName + "' in entry '" + key + "'");
                return null;
            }
        }

        var enchantments = new LinkedHashMap<String, Integer>();
        var enchSection = s.getConfigurationSection("enchantments");
        if (enchSection != null) {
            for (var ench : enchSection.getKeys(false)) {
                enchantments.put(ench, enchSection.getInt(ench, 1));
            }
        }

        return new DropEntry(key, s.getDouble("chance", 100.0), s.getInt("weight", 1),
                IntRange.parse(s.get("amount"), IntRange.of(1)), material, customItem,
                s.getString("name"), s.getStringList("lore"), enchantments,
                s.getInt("custom-model-data", 0), command);
    }

    /**
     * Rolls the table.
     *
     * @param boost drop-chance multiplier (boosts); applied to entry chances and pool chances, capped at 100%
     */
    public Result roll(Random random, double boost, Function<String, ItemStack> customItems, Logger logger) {
        if (isEmpty()) return Result.empty();
        double multiplier = Math.max(1.0, boost);
        var items = new ArrayList<ItemStack>();
        var commands = new ArrayList<String>();

        for (var entry : independent) {
            if (random.nextDouble() * 100 < Math.min(100.0, entry.chance() * multiplier)) {
                give(entry, random, items, commands, customItems, logger);
            }
        }

        for (var pool : pools) {
            if (random.nextDouble() * 100 >= Math.min(100.0, pool.chance() * multiplier)) continue;

            var candidates = new ArrayList<>(pool.entries());
            int rolls = pool.rolls().roll(random);
            for (int i = 0; i < rolls && !candidates.isEmpty(); i++) {
                var picked = pickWeighted(candidates, random);
                give(picked, random, items, commands, customItems, logger);
                if (pool.unique()) candidates.remove(picked);
            }
        }
        return new Result(items, commands);
    }

    private static void give(DropEntry entry, Random random, List<ItemStack> items, List<String> commands,
                             Function<String, ItemStack> customItems, Logger logger) {
        if (entry.isCommand()) {
            commands.add(entry.command());
            return;
        }
        var item = entry.createItem(entry.amount().roll(random), customItems, logger);
        if (item != null) items.add(item);
    }

    static DropEntry pickWeighted(List<DropEntry> entries, Random random) {
        int total = entries.stream().mapToInt(DropEntry::weight).sum();
        int target = random.nextInt(total);
        for (var entry : entries) {
            target -= entry.weight();
            if (target < 0) return entry;
        }
        return entries.get(entries.size() - 1);
    }
}
