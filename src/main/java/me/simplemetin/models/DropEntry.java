package me.simplemetin.models;

import me.simplemetin.utils.TextUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * One reward: an item (material or custom item from items.yml) or a console command.
 * chance is used by independent drops, weight by pools.
 */
public record DropEntry(String key, double chance, int weight, IntRange amount,
                        Material material, String customItem, String name, List<String> lore,
                        Map<String, Integer> enchantments, int customModelData, String command) {

    public boolean isCommand() {
        return command != null;
    }

    public DropEntry withChance(double newChance) {
        return new DropEntry(key, newChance, weight, amount, material, customItem, name, lore,
                enchantments, customModelData, command);
    }

    /**
     * Builds the item stack with the given amount.
     *
     * @param customItems resolves custom-item keys (items.yml); returns null for unknown keys
     * @return null if the item cannot be created (unknown custom item)
     */
    public ItemStack createItem(int count, Function<String, ItemStack> customItems, Logger logger) {
        ItemStack item;
        if (customItem != null) {
            var template = customItems.apply(customItem);
            if (template == null) {
                logger.warning("Drop '" + key + "': custom item '" + customItem + "' not found in items.yml");
                return null;
            }
            item = template.clone();
            item.setAmount(Math.max(1, count));
        } else {
            item = new ItemStack(material, Math.max(1, count));
        }

        boolean customise = name != null || (lore != null && !lore.isEmpty())
                || (enchantments != null && !enchantments.isEmpty()) || customModelData > 0;
        if (!customise) return item;

        var meta = item.getItemMeta();
        if (meta == null) return item;

        if (name != null) {
            meta.displayName(TextUtils.parseItemText(name));
        }
        if (lore != null && !lore.isEmpty()) {
            meta.lore(lore.stream().map(TextUtils::parseItemText).toList());
        }
        if (enchantments != null) {
            for (var entry : enchantments.entrySet()) {
                var enchantment = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(entry.getKey().toLowerCase()));
                if (enchantment == null) {
                    logger.warning("Drop '" + key + "': unknown enchantment '" + entry.getKey() + "'");
                    continue;
                }
                meta.addEnchant(enchantment, entry.getValue(), true);
            }
        }
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }
        item.setItemMeta(meta);
        return item;
    }
}
