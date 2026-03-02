package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Codec-backed spawn pool entry embedded in a {@link DungeonThemeConfig}.
 * Defines a single NPC role with level range, weight, and optional floor gates.
 *
 * @since 1.1.0
 */
public class SpawnPoolEntry {

    public static final BuilderCodec<SpawnPoolEntry> CODEC;
    public static final ArrayCodec<SpawnPoolEntry> ARRAY_CODEC;

    protected String npcRole = "";
    protected int minLevel = 1;
    protected int maxLevel = 1;
    protected double weight = 1.0;
    protected int minFloor = -1;   // -1 means no restriction
    protected int maxFloor = -1;   // -1 means no restriction

    public SpawnPoolEntry() {}

    static {
        CODEC = BuilderCodec.builder(SpawnPoolEntry.class, SpawnPoolEntry::new)
            .append(new KeyedCodec<>("NpcRole", Codec.STRING),
                (e, v) -> e.npcRole = v, e -> e.npcRole).add()
            .append(new KeyedCodec<>("MinLevel", Codec.INTEGER),
                (e, v) -> e.minLevel = v, e -> e.minLevel).add()
            .append(new KeyedCodec<>("MaxLevel", Codec.INTEGER),
                (e, v) -> e.maxLevel = v, e -> e.maxLevel).add()
            .append(new KeyedCodec<>("Weight", Codec.DOUBLE),
                (e, v) -> e.weight = v, e -> e.weight).add()
            .append(new KeyedCodec<>("MinFloor", Codec.INTEGER),
                (e, v) -> e.minFloor = v, e -> e.minFloor).add()
            .append(new KeyedCodec<>("MaxFloor", Codec.INTEGER),
                (e, v) -> e.maxFloor = v, e -> e.maxFloor).add()
            .build();
        ARRAY_CODEC = new ArrayCodec<>(CODEC, SpawnPoolEntry[]::new);
    }

    // ============================================
    // Getters
    // ============================================

    /**
     * @return the NPC role identifier.
     * @since 1.1.0
     */
    @Nonnull public String getNpcRole() { return npcRole; }
    /**
     * @return the minimum NPC level.
     * @since 1.1.0
     */
    public int getMinLevel() { return minLevel; }
    /**
     * @return the maximum NPC level.
     * @since 1.1.0
     */
    public int getMaxLevel() { return maxLevel; }
    /**
     * @return the spawn weight for weighted random selection.
     * @since 1.1.0
     */
    public double getWeight() { return weight; }
    /**
     * @return minimum floor level, or -1 if no restriction.
     * @since 1.1.0
     */
    public int getMinFloor() { return minFloor; }
    /**
     * @return maximum floor level, or -1 if no restriction.
     * @since 1.1.0
     */
    public int getMaxFloor() { return maxFloor; }

    /**
     * Check if this entry is eligible for the given floor level.
     * Uses {@code minLevel}/{@code maxLevel} as floor eligibility gates:
     * the NPC can only spawn on floors where {@code minLevel <= floorLevel <= maxLevel}.
     *
     * @param floorLevel the current dungeon floor
     * @return true if this entry can be used on the given floor
     * @since 1.1.0
     */
    public boolean isEligibleForFloor(int floorLevel) {
        if (floorLevel < minLevel) return false;
        return floorLevel <= maxLevel;
    }
}
