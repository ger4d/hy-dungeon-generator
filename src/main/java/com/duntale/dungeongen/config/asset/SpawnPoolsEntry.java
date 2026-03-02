package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Container for all spawn pool tiers within a theme. Parsed from the
 * {@code "SpawnPools"} section of a theme JSON.
 *
 * @since 1.1.0
 */
public class SpawnPoolsEntry {

    public static final BuilderCodec<SpawnPoolsEntry> CODEC;

    protected SpawnPoolEntry[] tier1 = new SpawnPoolEntry[0];
    protected SpawnPoolEntry[] tier2 = new SpawnPoolEntry[0];
    protected SpawnPoolEntry[] tier3 = new SpawnPoolEntry[0];
    protected SpawnPoolEntry[] boss = new SpawnPoolEntry[0];

    public SpawnPoolsEntry() {}

    static {
        CODEC = BuilderCodec.builder(SpawnPoolsEntry.class, SpawnPoolsEntry::new)
            .append(new KeyedCodec<>("Tier1", SpawnPoolEntry.ARRAY_CODEC),
                (e, v) -> e.tier1 = v, e -> e.tier1).add()
            .append(new KeyedCodec<>("Tier2", SpawnPoolEntry.ARRAY_CODEC),
                (e, v) -> e.tier2 = v, e -> e.tier2).add()
            .append(new KeyedCodec<>("Tier3", SpawnPoolEntry.ARRAY_CODEC),
                (e, v) -> e.tier3 = v, e -> e.tier3).add()
            .append(new KeyedCodec<>("Boss", SpawnPoolEntry.ARRAY_CODEC),
                (e, v) -> e.boss = v, e -> e.boss).add()
            .build();
    }

    /**
     * Get the spawn pool for the given tier (1-3) or boss (any other value).
     *
     * @param tier the difficulty tier
     * @return the spawn pool entries for that tier
     * @since 1.1.0
     */
    @Nonnull
    public SpawnPoolEntry[] getPoolForTier(int tier) {
        return switch (tier) {
            case 1 -> tier1;
            case 2 -> tier2;
            case 3 -> tier3;
            default -> boss;
        };
    }

    /**
     * @return the tier 1 spawn pool entries.
     * @since 1.1.0
     */
    @Nonnull public SpawnPoolEntry[] getTier1() { return tier1; }
    /**
     * @return the tier 2 spawn pool entries.
     * @since 1.1.0
     */
    @Nonnull public SpawnPoolEntry[] getTier2() { return tier2; }
    /**
     * @return the tier 3 spawn pool entries.
     * @since 1.1.0
     */
    @Nonnull public SpawnPoolEntry[] getTier3() { return tier3; }
    /**
     * @return the boss spawn pool entries.
     * @since 1.1.0
     */
    @Nonnull public SpawnPoolEntry[] getBoss() { return boss; }
}
