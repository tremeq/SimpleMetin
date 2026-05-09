package me.simplemetin.models;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public record DropItem(Material material, int amount, double chance, String name, List<String> lore) {

    public ItemStack createItemStack() {
        var item = new ItemStack(material, amount);

        if (name != null || (lore != null && !lore.isEmpty())) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (name != null) {
                    meta.setDisplayName(colorize(name));
                }
                if (lore != null && !lore.isEmpty()) {
                    meta.setLore(lore.stream().map(this::colorize).toList());
                }
                item.setItemMeta(meta);
            }
        }

        return item;
    }

    private String colorize(String text) {
        return text.replace('&', '§');
    }
}
