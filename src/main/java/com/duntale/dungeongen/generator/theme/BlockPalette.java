package com.duntale.dungeongen.generator.theme;

import com.duntale.dungeongen.config.asset.DungeonThemeConfig;

import javax.annotation.Nonnull;

/**
 * Themed block palette mapping structural roles (wall, floor, ceiling, etc.)
 * to concrete block IDs from the Hytale asset registry.
 * <p>
 * Each palette also carries decay / overgrowth / rubble / fluid variants
 * for environmental storytelling passes.
 *
 * @since 1.0.0
 */
public class BlockPalette {

    private final String name;
    private final String primaryWall;
    private final String secondaryWall;
    private final String floor;
    private final String ceiling;
    private final String pillarBase;
    private final String pillarMiddle;
    private final String stairs;
    private final String slab;
    private final String[] decayVariants;
    private final String[] overgrowthBlocks;
    private final String[] rubbleBlocks;
    private final String fluidBlock;
    private final String accentBlock;

    /**
     * Create a new block palette.
     *
     * @param name             palette name
     * @param primaryWall      primary wall block ID
     * @param secondaryWall    secondary / accent wall block ID
     * @param floor            floor block ID
     * @param ceiling          ceiling block ID
     * @param pillarBase       pillar base block ID
     * @param pillarMiddle     pillar middle block ID
     * @param stairs           stairs block ID
     * @param slab             slab / half-block ID
     * @param decayVariants    mossy / cracked wall variants
     * @param overgrowthBlocks vine / moss / root blocks
     * @param rubbleBlocks     rubble / debris blocks
     * @param fluidBlock       fluid source block ID (water, lava, etc.)
     * @param accentBlock      accent block (gold trim, ore, etc.)
     */
    public BlockPalette(@Nonnull String name,
                        @Nonnull String primaryWall,
                        @Nonnull String secondaryWall,
                        @Nonnull String floor,
                        @Nonnull String ceiling,
                        @Nonnull String pillarBase,
                        @Nonnull String pillarMiddle,
                        @Nonnull String stairs,
                        @Nonnull String slab,
                        @Nonnull String[] decayVariants,
                        @Nonnull String[] overgrowthBlocks,
                        @Nonnull String[] rubbleBlocks,
                        @Nonnull String fluidBlock,
                        @Nonnull String accentBlock) {
        this.name = name;
        this.primaryWall = primaryWall;
        this.secondaryWall = secondaryWall;
        this.floor = floor;
        this.ceiling = ceiling;
        this.pillarBase = pillarBase;
        this.pillarMiddle = pillarMiddle;
        this.stairs = stairs;
        this.slab = slab;
        this.decayVariants = decayVariants;
        this.overgrowthBlocks = overgrowthBlocks;
        this.rubbleBlocks = rubbleBlocks;
        this.fluidBlock = fluidBlock;
        this.accentBlock = accentBlock;
    }

    // ============================================
    // Asset-backed factory
    // ============================================

    /**
     * Look up a palette by name from the asset-backed {@link DungeonThemeConfig}.
     * Falls back to the crypt theme if the requested theme is not loaded.
     *
     * @param name the palette name (e.g. "crypt", "volcanic")
     * @return the matching palette
     */
    @Nonnull
    public static BlockPalette fromName(@Nonnull String name) {
        DungeonThemeConfig config = DungeonThemeConfig.get(name);
        if (config != null) {
            return config.toPalette();
        }
        // Fallback: try default crypt theme from assets
        DungeonThemeConfig fallback = DungeonThemeConfig.get("crypt");
        if (fallback != null) {
            return fallback.toPalette();
        }
        // Last-resort hardcoded default (only if assets not yet loaded)
        return new BlockPalette(
            "crypt",
            "Rock_Stone_Brick", "Rock_Basalt_Brick",
            "Rock_Stone_Cobble", "Rock_Stone_Brick",
            "Rock_Stone_Brick_Pillar_Base", "Rock_Stone_Brick_Pillar_Middle",
            "Rock_Stone_Brick_Stairs", "Rock_Stone_Brick_Half",
            new String[]{"Rock_Stone_Brick_Mossy", "Rock_Stone_Cobble_Mossy", "Rock_Stone_Mossy"},
            new String[]{"Plant_Vine_Hanging", "Plant_Vine_Wall", "Plant_Moss_Cave_Green"},
            new String[]{"Rubble_Stone", "Rubble_Stone_Medium", "Rubble_Stone_Mossy"},
            "Fluid_Water", "Rock_Basalt_Brick"
        );
    }

    // ============================================
    // Getters
    // ============================================

    /** @return the palette name. */
    @Nonnull public String getName() { return name; }

    /** @return the primary wall block ID. */
    @Nonnull public String getPrimaryWall() { return primaryWall; }

    /** @return the secondary wall block ID. */
    @Nonnull public String getSecondaryWall() { return secondaryWall; }

    /** @return the floor block ID. */
    @Nonnull public String getFloor() { return floor; }

    /** @return the ceiling block ID. */
    @Nonnull public String getCeiling() { return ceiling; }

    /** @return the pillar base block ID. */
    @Nonnull public String getPillarBase() { return pillarBase; }

    /** @return the pillar middle block ID. */
    @Nonnull public String getPillarMiddle() { return pillarMiddle; }

    /** @return the stairs block ID. */
    @Nonnull public String getStairs() { return stairs; }

    /** @return the slab / half-block ID. */
    @Nonnull public String getSlab() { return slab; }

    /** @return decay variant block IDs (mossy / cracked). */
    @Nonnull public String[] getDecayVariants() { return decayVariants; }

    /** @return overgrowth block IDs (vine / moss / root). */
    @Nonnull public String[] getOvergrowthBlocks() { return overgrowthBlocks; }

    /** @return rubble block IDs. */
    @Nonnull public String[] getRubbleBlocks() { return rubbleBlocks; }

    /** @return the fluid source block ID. */
    @Nonnull public String getFluidBlock() { return fluidBlock; }

    /** @return the accent block ID. */
    @Nonnull public String getAccentBlock() { return accentBlock; }
}
