package me.simplemetin.utils;

import me.simplemetin.SimpleMetin;
import me.simplemetin.managers.BoostManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class VoucherUtils {

    public static ItemStack createBoostVoucher(SimpleMetin plugin, double multiplier, long durationSeconds) {
        var config = plugin.getConfig();
        var materialStr = config.getString("boosts.personal.voucher.material", "PAPER");
        var material = Material.getMaterial(materialStr);
        if (material == null) material = Material.PAPER;

        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        if (meta == null) return item;

        // Name and lore come from the messages file (voucher-name, voucher-lore)
        var messages = plugin.getMessages();
        var multiplierText = BoostManager.formatMultiplier(multiplier);
        var durationText = TextUtils.formatDuration(durationSeconds);
        meta.displayName(TextUtils.parseItemText(messages.raw("voucher-name", "multiplier", multiplierText, "duration", durationText)));
        meta.lore(messages.rawList("voucher-lore", "multiplier", multiplierText, "duration", durationText).stream()
                .map(TextUtils::parseItemText)
                .toList());

        // Glow effect
        if (config.getBoolean("boosts.personal.voucher.glow", true)) {
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        // Custom model data
        int customModelData = config.getInt("boosts.personal.voucher.custom-model-data", 0);
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        // NBT data for identification
        var key = new NamespacedKey(plugin, "boost_voucher");
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "true");

        var multiplierKey = new NamespacedKey(plugin, "boost_multiplier");
        meta.getPersistentDataContainer().set(multiplierKey, PersistentDataType.DOUBLE, multiplier);

        var durationKey = new NamespacedKey(plugin, "boost_duration");
        meta.getPersistentDataContainer().set(durationKey, PersistentDataType.LONG, durationSeconds);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isBoostVoucher(Plugin plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        var meta = item.getItemMeta();
        var key = new NamespacedKey(plugin, "boost_voucher");
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public static double getVoucherMultiplier(Plugin plugin, ItemStack item) {
        if (!item.hasItemMeta()) return 1.0;
        var meta = item.getItemMeta();
        var key = new NamespacedKey(plugin, "boost_multiplier");
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.DOUBLE, 1.0);
    }

    public static long getVoucherDuration(Plugin plugin, ItemStack item) {
        if (!item.hasItemMeta()) return 0;
        var meta = item.getItemMeta();
        var key = new NamespacedKey(plugin, "boost_duration");
        return meta.getPersistentDataContainer().getOrDefault(key, PersistentDataType.LONG, 0L);
    }


}
