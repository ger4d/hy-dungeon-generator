package com.duntale.dungeongen.config.asset;

import com.duntale.dungeongen.model.SpawnerVariant;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.EnumSet;
import java.util.Set;

/**
 * Codec-backed spawn pool entry embedded in a {@link DungeonThemeConfig}.
 * Defines a single NPC role with floor range, weight, and allowed variants.
 *
 * @since 1.1.0
 */
public class SpawnPoolEntry {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<SpawnPoolEntry> CODEC;
    public static final ArrayCodec<SpawnPoolEntry> ARRAY_CODEC;

    protected String npcRole = "";
    protected int minFloor = 1;
    protected int maxFloor = 1;
    protected double weight = 1.0;
    protected Set<SpawnerVariant> allowedVariants = EnumSet.of(SpawnerVariant.NORMAL);

    public SpawnPoolEntry() {}

    static {
        CODEC = BuilderCodec.builder(SpawnPoolEntry.class, SpawnPoolEntry::new)
            .append(new KeyedCodec<>("NpcRole", Codec.STRING),
                (e, v) -> e.npcRole = v, e -> e.npcRole).add()
            .append(new KeyedCodec<>("MinFloor", Codec.INTEGER),
                (e, v) -> e.minFloor = v, e -> e.minFloor).add()
            .append(new KeyedCodec<>("MaxFloor", Codec.INTEGER),
                (e, v) -> e.maxFloor = v, e -> e.maxFloor).add()
            .append(new KeyedCodec<>("Weight", Codec.DOUBLE),
                (e, v) -> e.weight = v, e -> e.weight).add()
            .append(new KeyedCodec<>("Variants", Codec.STRING_ARRAY),
                (e, v) -> e.allowedVariants = parseVariants(v),
                e -> variantsToStrings(e.allowedVariants)).add()
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
     * @return the minimum floor level for eligibility.
     * @since 1.4.0
     */
    public int getMinFloor() { return minFloor; }

    /**
     * @return the maximum floor level for eligibility.
     * @since 1.4.0
     */
    public int getMaxFloor() { return maxFloor; }

    /**
     * @return the spawn weight for weighted random selection.
     * @since 1.1.0
     */
    public double getWeight() { return weight; }

    /**
     * @return the set of allowed spawner variants.
     * @since 1.4.0
     */
    @Nonnull public Set<SpawnerVariant> getAllowedVariants() { return allowedVariants; }

    /**
     * Check if this entry allows the given spawner variant.
     *
     * @param variant the variant to check
     * @return true if this entry can produce the given variant
     * @since 1.4.0
     */
    public boolean allowsVariant(@Nonnull SpawnerVariant variant) {
        return allowedVariants.contains(variant);
    }

    /**
     * Parse raw variant name strings into an {@link EnumSet}.
     * Invalid names are silently skipped.
     */
    @Nonnull
    private static Set<SpawnerVariant> parseVariants(@Nonnull String[] raw) {
        EnumSet<SpawnerVariant> set = EnumSet.noneOf(SpawnerVariant.class);
        for (String v : raw) {
            try {
                set.add(SpawnerVariant.valueOf(v));
            } catch (IllegalArgumentException e) {
                LOGGER.atWarning().log("[DungeonGen] Unknown variant '%s' in SpawnPoolEntry — skipping", v);
            }
        }
        return set;
    }

    /**
     * Convert a variant set back to a string array for codec serialization.
     */
    @Nonnull
    private static String[] variantsToStrings(@Nonnull Set<SpawnerVariant> set) {
        String[] result = new String[set.size()];
        int i = 0;
        for (SpawnerVariant v : set) {
            result[i++] = v.name();
        }
        return result;
    }

    /**
     * Check if this entry is eligible for the given floor level.
     *
     * @param floorLevel the current dungeon floor
     * @return true if this entry can be used on the given floor
     * @since 1.1.0
     */
    public boolean isEligibleForFloor(int floorLevel) {
        if (floorLevel < minFloor) return false;
        return floorLevel <= maxFloor;
    }
}
