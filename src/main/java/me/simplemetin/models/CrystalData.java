package me.simplemetin.models;

import org.bukkit.Location;
import org.bukkit.entity.EnderCrystal;

import java.util.UUID;

public class CrystalData {
    private final String id;
    private final String configId;
    private final Location location;
    private final CrystalType type;
    private final int maxHp;
    private int currentHp;
    private EnderCrystal entity;
    private UUID entityUuid;
    private long respawnTime;
    private boolean isDestroyed;
    private String overrideName;

    public CrystalData(String id, String configId, Location location, CrystalType type, int maxHp, String overrideName) {
        this.id = id;
        this.configId = configId;
        this.location = location;
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

    public CrystalType getType() {
        return type;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public int getCurrentHp() {
        return currentHp;
    }

    public void setCurrentHp(int hp) {
        this.currentHp = Math.max(0, Math.min(hp, maxHp));
    }

    public void damage(int amount) {
        this.currentHp = Math.max(0, currentHp - amount);
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
}
