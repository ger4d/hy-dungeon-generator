package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Codec-backed palette data embedded in a {@link DungeonThemeConfig}.
 * Maps structural roles (wall, floor, ceiling, etc.) to block IDs.
 *
 * @since 1.3.0
 */
public class PaletteEntry {

    public static final BuilderCodec<PaletteEntry> CODEC;

    protected String primaryWall = "Rock_Stone_Brick";
    protected String secondaryWall = "Rock_Stone_Brick";
    protected String floor = "Rock_Stone_Cobble";
    protected String ceiling = "Rock_Stone_Brick";
    protected String pillarBase = "Rock_Stone_Brick";
    protected String pillarMiddle = "Rock_Stone_Brick";
    protected String stairs = "Rock_Stone_Brick";
    protected String slab = "Rock_Stone_Brick";
    protected String[] decayVariants = new String[0];
    protected String[] overgrowthBlocks = new String[0];
    protected String[] rubbleBlocks = new String[0];
    protected String fluidBlock = "Fluid_Water";
    protected String secondaryFluidBlock = "Fluid_Lava";
    @Nullable protected String accentBlock = null;

    public PaletteEntry() {}

    static {
        CODEC = BuilderCodec.builder(PaletteEntry.class, PaletteEntry::new)
            .append(new KeyedCodec<>("PrimaryWall", Codec.STRING),
                (e, v) -> e.primaryWall = v, e -> e.primaryWall).add()
            .append(new KeyedCodec<>("SecondaryWall", Codec.STRING),
                (e, v) -> e.secondaryWall = v, e -> e.secondaryWall).add()
            .append(new KeyedCodec<>("Floor", Codec.STRING),
                (e, v) -> e.floor = v, e -> e.floor).add()
            .append(new KeyedCodec<>("Ceiling", Codec.STRING),
                (e, v) -> e.ceiling = v, e -> e.ceiling).add()
            .append(new KeyedCodec<>("PillarBase", Codec.STRING),
                (e, v) -> e.pillarBase = v, e -> e.pillarBase).add()
            .append(new KeyedCodec<>("PillarMiddle", Codec.STRING),
                (e, v) -> e.pillarMiddle = v, e -> e.pillarMiddle).add()
            .append(new KeyedCodec<>("Stairs", Codec.STRING),
                (e, v) -> e.stairs = v, e -> e.stairs).add()
            .append(new KeyedCodec<>("Slab", Codec.STRING),
                (e, v) -> e.slab = v, e -> e.slab).add()
            .append(new KeyedCodec<>("DecayVariants", Codec.STRING_ARRAY),
                (e, v) -> e.decayVariants = v, e -> e.decayVariants).add()
            .append(new KeyedCodec<>("OvergrowthBlocks", Codec.STRING_ARRAY),
                (e, v) -> e.overgrowthBlocks = v, e -> e.overgrowthBlocks).add()
            .append(new KeyedCodec<>("RubbleBlocks", Codec.STRING_ARRAY),
                (e, v) -> e.rubbleBlocks = v, e -> e.rubbleBlocks).add()
            .append(new KeyedCodec<>("FluidBlock", Codec.STRING),
                (e, v) -> e.fluidBlock = v, e -> e.fluidBlock).add()
            .append(new KeyedCodec<>("SecondaryFluidBlock", Codec.STRING),
                (e, v) -> e.secondaryFluidBlock = v, e -> e.secondaryFluidBlock).add()
            .append(new KeyedCodec<>("AccentBlock", Codec.STRING),
                (e, v) -> e.accentBlock = v, e -> e.accentBlock).add()
            .build();
    }

    // ============================================
    // Getters
    // ============================================

    @Nonnull public String getPrimaryWall() { return primaryWall; }
    @Nonnull public String getSecondaryWall() { return secondaryWall; }
    @Nonnull public String getFloor() { return floor; }
    @Nonnull public String getCeiling() { return ceiling; }
    @Nonnull public String getPillarBase() { return pillarBase; }
    @Nonnull public String getPillarMiddle() { return pillarMiddle; }
    @Nonnull public String getStairs() { return stairs; }
    @Nonnull public String getSlab() { return slab; }
    @Nonnull public String[] getDecayVariants() { return decayVariants; }
    @Nonnull public String[] getOvergrowthBlocks() { return overgrowthBlocks; }
    @Nonnull public String[] getRubbleBlocks() { return rubbleBlocks; }
    @Nonnull public String getFluidBlock() { return fluidBlock; }
    @Nonnull public String getSecondaryFluidBlock() { return secondaryFluidBlock; }
    @Nullable public String getAccentBlock() { return accentBlock; }
}
