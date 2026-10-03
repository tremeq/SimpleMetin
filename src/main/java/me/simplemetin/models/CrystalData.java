package me.simplemetin.models;

import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;

import java.util.UUID;

public class CrystalData {
    private final String id;
    private final String configId;
    private final Location location;
    private final String worldName;
    private final CrystalType type;
    private int maxHp;
    private int currentHp;
    private EnderCrystal entity;
    private UUID entityUuid;
    private long respawnTime;
    private boolean isDestroyed;
    private String overrideName;
    /** Spawner that created this crystal (null = placed by an admin). */
    private String spawnerId;
    /** Epoch millis when an automatically spawned crystal vanishes if nobody destroys it (0 = never). */
    private long expiresAt;

    public CrystalData(String id, String configId, Location location, CrystalType type, int maxHp, String overrideName) {
        this.id = id;
        this.configId = configId;
        this.location = location;
        // Kept separately: Location#getWorld() throws once the world is unloaded
        this.worldName = location != null && location.getWorld() != null ? location.getWorld().getName() : null;
        this.type = type;
        this.maxHp = maxHp;
        this.currentHp = maxHp;
        this.respawnTime = 0;
        this.isDestroyed = false;
        this.overrideName = overrideName;
    }

    public String getId() {
        return id;
    }

    public String getConfigId() {
        return configId;
    }

    public Location getLocation() {
        return location;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getChunkX() {
        return location.getBlockX() >> 4;
    }

    public int getChunkZ() {
        return location.getBlockZ() >> 4;
    }

    public CrystalType getType() {
        return type;
    }

    public int getMaxHp() {
        return maxHp;
    }

    /** Changes max HP (config reload). A crystal at full HP stays full; otherwise HP is capped. */
    public void setMaxHp(int maxHp) {
        boolean wasFull = currentHp >= this.maxHp;
        this.maxHp = Math.max(1, maxHp);
        this.currentHp = wasFull ? this.maxHp : Math.min(currentHp, this.maxHp);
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(hp, maxHp));
    }

    /** Applies damage and returns how much HP was actually removed (no overkill, never heals). */
    public int damage(int amount) {
        if (amount <= 0) return 0;
        int dealt = Math.min(amount, currentHp);
        this.currentHp -= dealt;
        return dealt;
    }

    public boolean isDead() {
        return currentHp <= 0;
    }

    public EnderCrystal getEntity() {
        return entity;
    }

    public void setEntity(EnderCrystal entity) {
        this.entity = entity;
        if (entity != null) {
            this.entityUuid = entity.getUniqueId();
        }
    }

    public UUID getEntityUuid() {
        return entityUuid;
    }

    public long getRespawnTime() {
        return respawnTime;
    }

    public void setRespawnTime(long time) {
        this.respawnTime = time;
    }

    public boolean isDestroyed() {
        return isDestroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.isDestroyed = destroyed;
    }

    public String getOverrideName() {
        return overrideName;
    }

    public void setOverrideName(String name) {
        this.overrideName = name;
    }

    public String getSpawnerId() {
        return spawnerId;
    }

    public void setSpawnerId(String spawnerId) {
        this.spawnerId = spawnerId;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }
}
