package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Categorized overgrowth blocks for a dungeon theme palette.
 * Each array contains block IDs appropriate for a specific structural position.
 *
 * @since 1.3.0
 */
public class OvergrowthEntry {

    public static final BuilderCodec<OvergrowthEntry> CODEC;

    protected String[] floor = new String[0];
    protected String[] wall = new String[0];
    protected String[] ceiling = new String[0];

    public OvergrowthEntry() {}

    static {
        CODEC = BuilderCodec.builder(OvergrowthEntry.class, OvergrowthEntry::new)
            .append(new KeyedCodec<>("Floor", Codec.STRING_ARRAY),
                (e, v) -> e.floor = v, e -> e.floor).add()
            .append(new KeyedCodec<>("Wall", Codec.STRING_ARRAY),
                (e, v) -> e.wall = v, e -> e.wall).add()
            .append(new KeyedCodec<>("Ceiling", Codec.STRING_ARRAY),
                (e, v) -> e.ceiling = v, e -> e.ceiling).add()
            .build();
    }

    /** @return block IDs placed on floor surfaces (solid below). */
    @Nonnull public String[] getFloor() { return floor; }

    /** @return block IDs placed on wall surfaces (solid to the side). */
    @Nonnull public String[] getWall() { return wall; }

    /** @return block IDs placed on ceiling surfaces (solid above). */
    @Nonnull public String[] getCeiling() { return ceiling; }

    /** @return true if all arrays are empty. */
    public boolean isEmpty() {
        return floor.length == 0 && wall.length == 0 && ceiling.length == 0;
    }
}
