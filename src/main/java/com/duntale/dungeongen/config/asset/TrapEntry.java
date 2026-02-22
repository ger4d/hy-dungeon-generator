package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;

/**
 * Codec-backed trap configuration embedded in a {@link DungeonThemeConfig}.
 * Defines the block IDs for regular traps, wall spikes, and floor traps.
 *
 * @since 1.3.0
 */
public class TrapEntry {

    public static final BuilderCodec<TrapEntry> CODEC;

    protected String[] regularTraps = {
        "Survival_Trap_Snapjaw",
        "Survival_Trap_Spike_Iron",
        "Survival_Trap_Spike_Wood"
    };
    protected String[] wallSpikeTraps = {
        "Survival_Trap_Spike_Iron",
        "Survival_Trap_Spike_Wood"
    };
    protected String[] floorTraps = {
        "Trap_Ancient_Platform",
        "Trap_Ice",
        "Trap_Slate",
        "Survival_Trap_Grass"
    };

    public TrapEntry() {}

    static {
        CODEC = BuilderCodec.builder(TrapEntry.class, TrapEntry::new)
            .append(new KeyedCodec<>("RegularTraps", Codec.STRING_ARRAY),
                (e, v) -> e.regularTraps = v, e -> e.regularTraps).add()
            .append(new KeyedCodec<>("WallSpikeTraps", Codec.STRING_ARRAY),
                (e, v) -> e.wallSpikeTraps = v, e -> e.wallSpikeTraps).add()
            .append(new KeyedCodec<>("FloorTraps", Codec.STRING_ARRAY),
                (e, v) -> e.floorTraps = v, e -> e.floorTraps).add()
            .build();
    }

    // ============================================
    // Getters
    // ============================================

    @Nonnull public String[] getRegularTraps() { return regularTraps; }
    @Nonnull public String[] getWallSpikeTraps() { return wallSpikeTraps; }
    @Nonnull public String[] getFloorTraps() { return floorTraps; }
}
