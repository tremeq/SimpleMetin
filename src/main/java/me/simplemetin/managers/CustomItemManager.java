package me.simplemetin.managers;

import me.simplemetin.SimpleMetin;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Items saved from the hand with /metin item save &lt;key&gt; (items.yml), used in drops as custom-item: &lt;key&gt;.
 * Stored with Paper's byte serialization, so any NBT/components (custom plugins' items too) survive exactly.
 */
public class CustomItemManager {

    private static final String KEY_PATTERN = "[a-z0-9_-]{1,32}";

    private final SimpleMetin plugin;
    private final File file;
    private final Map<String, ItemStack> items = new ConcurrentHashMap<>();

    public CustomItemManager(SimpleMetin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "items.yml");
        load();
    }

    public void load() {
        items.clear();
        if (!file.exists()) return;

        var config = YamlConfiguration.loadConfiguration(file);
        var section = config.getConfigurationSection("items");
        if (section == null) return;

        for (var key : section.getKeys(false)) {
            var data = section.getString(key + ".data");
            if (data == null) continue;
            try {
                items.put(key, ItemStack.deserializeBytes(Base64.getDecoder().decode(data)));
            } catch (RuntimeException e) {
                plugin.getLogger().warning("Could not load custom item '" + key + "' from items.yml: " + e.getMessage());
            }
        }
    }

    public static boolean isValidKey(String key) {
        return key != null && key.matches(KEY_PATTERN);
    }

    /** Returns a copy of the item, or null if the key is unknown. */
    public ItemStack get(String key) {
        var item = items.get(key);
        return item == null ? null : item.clone();
    }

    public Set<String> getKeys() {
        return new TreeSet<>(items.keySet());
    }

    public void save(String key, ItemStack item) throws IOException {
        var copy = item.clone();
        copy.setAmount(1);
        items.put(key, copy);
        write();
    }

    public boolean remove(String key) throws IOException {
        if (items.remove(key) == null) return false;
        write();
        return true;
    }

    /** First free "&lt;base&gt;_&lt;n&gt;" key. */
    public String nextFreeKey(String base) {
        var clean = base.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
        for (int i = 1; ; i++) {
            var key = clean + "_" + i;
            if (!items.containsKey(key)) return key;
        }
    }

    private void write() throws IOException {
        var config = new YamlConfiguration();
        config.options().setHeader(List.of(
                "Custom items for drops - managed with /metin item save|remove|give|list",
                "Use them in config.yml as:  custom-item: <key>",
                "'data' is the exact item (all NBT/components); 'material' and 'name' are only for reading."));
        for (var entry : new TreeMap<>(items).entrySet()) {
            var path = "items." + entry.getKey();
            var item = entry.getValue();
            config.set(path + ".material", item.getType().name());
            var meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                config.set(path + ".name", net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                        .plainText().serialize(Objects.requireNonNull(meta.displayName())));
            }
            config.set(path + ".data", Base64.getEncoder().encodeToString(item.serializeAsBytes()));
        }
        config.save(file);
    }
}
