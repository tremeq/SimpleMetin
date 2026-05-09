package me.simplemetin.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerStats {
    private final UUID playerUuid;
    private int crystalsDestroyed;
    private long totalDamageDealt;
    private int itemsReceived;
    private long moneyEarned;
    private final Map<String, Integer> crystalTypeDestroyed;
    private long lastSeen;

    public PlayerStats(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.crystalsDestroyed = 0;
        this.totalDamageDealt = 0;
        this.itemsReceived = 0;
        this.moneyEarned = 0;
        this.crystalTypeDestroyed = new HashMap<>();
        this.lastSeen = System.currentTimeMillis();
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public int getCrystalsDestroyed() {
        return crystalsDestroyed;
    }

    public void addCrystalDestroyed(String crystalType) {
        this.crystalsDestroyed++;
        this.crystalTypeDestroyed.put(crystalType,
            this.crystalTypeDestroyed.getOrDefault(crystalType, 0) + 1);
    }

    public long getTotalDamageDealt() {
        return totalDamageDealt;
    }

    public void addDamage(int damage) {
        this.totalDamageDealt += damage;
    }

    public int getItemsReceived() {
        return itemsReceived;
    }

    public void addItemsReceived(int count) {
        this.itemsReceived += count;
    }

    public long getMoneyEarned() {
        return moneyEarned;
    }

    public void addMoneyEarned(long amount) {
        this.moneyEarned += amount;
    }

    public int getCrystalTypeDestroyed(String type) {
        return crystalTypeDestroyed.getOrDefault(type, 0);
    }

    public Map<String, Integer> getAllCrystalTypes() {
        return new HashMap<>(crystalTypeDestroyed);
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    public void setCrystalsDestroyed(int crystalsDestroyed) {
        this.crystalsDestroyed = crystalsDestroyed;
    }

    public void setTotalDamageDealt(long totalDamageDealt) {
        this.totalDamageDealt = totalDamageDealt;
    }

    public void setItemsReceived(int itemsReceived) {
        this.itemsReceived = itemsReceived;
    }

    public void setMoneyEarned(long moneyEarned) {
        this.moneyEarned = moneyEarned;
    }

    public void setCrystalTypeDestroyed(String type, int count) {
        this.crystalTypeDestroyed.put(type, count);
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
