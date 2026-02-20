package com.duntale.dungeongen.generator.theme;

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
    // Static factory methods — one per theme
    // ============================================

    /** @return the crypt palette (stone brick, mossy decay, water). */
    @Nonnull
    public static BlockPalette crypt() {
        return new BlockPalette(
            "crypt",
            "Rock_Stone_Brick",
            "Rock_Basalt_Brick",
            "Rock_Stone_Cobble",
            "Rock_Stone_Brick",
            "Rock_Stone_Brick_Pillar_Base",
            "Rock_Stone_Brick_Pillar_Middle",
            "Rock_Stone_Brick_Stairs",
            "Rock_Stone_Brick_Half",
            new String[]{"Rock_Stone_Brick_Mossy", "Rock_Stone_Cobble_Mossy", "Rock_Stone_Mossy"},
            new String[]{"Plant_Vine_Hanging", "Plant_Vine_Wall", "Plant_Moss_Cave_Green"},
            new String[]{"Rubble_Stone", "Rubble_Stone_Medium", "Rubble_Stone_Mossy"},
            "Fluid_Water",
            "Rock_Basalt_Brick"
        );
    }

    /** @return the volcanic palette (volcanic brick, lava, gold ore). */
    @Nonnull
    public static BlockPalette volcanic() {
        return new BlockPalette(
            "volcanic",
            "Rock_Volcanic_Brick",
            "Rock_Volcanic_Cobble",
            "Rock_Volcanic",
            "Rock_Volcanic_Brick",
            "Rock_Volcanic_Brick_Pillar_Base",
            "Rock_Volcanic_Brick_Pillar_Middle",
            "Rock_Volcanic_Brick",
            "Rock_Volcanic_Brick",
            new String[]{"Rock_Volcanic_Cracked_Incandescent", "Rock_Volcanic_Cracked_Lava"},
            new String[0],
            new String[]{"Rubble_Volcanic", "Rubble_Volcanic_Medium"},
            "Fluid_Lava",
            "Ore_Gold_Volcanic"
        );
    }

    /** @return the arcane palette (runic brick, crystals, water). */
    @Nonnull
    public static BlockPalette arcane() {
        return new BlockPalette(
            "arcane",
            "Rock_Runic_Blue_Brick",
            "Rock_Runic_Teal_Brick",
            "Rock_Runic_Cobble",
            "Rock_Runic_Brick",
            "Rock_Runic_Blue_Brick_Pillar_Base",
            "Rock_Runic_Cobble_Pillar_Middle",
            "Rock_Runic_Brick",
            "Rock_Runic_Brick",
            new String[]{"Rock_Runic_Dark_Brick"},
            new String[]{"Rock_Crystal_Blue_Large", "Rock_Crystal_Purple_Large"},
            new String[0],
            "Fluid_Water",
            "Rock_Runic_Blue_Brick_Pipe_Short"
        );
    }

    /** @return the mine palette (raw stone, wood beams, iron ore). */
    @Nonnull
    public static BlockPalette mine() {
        return new BlockPalette(
            "mine",
            "Rock_Stone",
            "Rock_Slate",
            "Soil_Gravel",
            "Rock_Stone",
            "Wood_Darkwood_Beam",
            "Wood_Darkwood_Beam",
            "Rock_Stone_Cobble",
            "Wood_Darkwood_Planks",
            new String[]{"Rock_Stone_Cobble_Mossy", "Soil_Gravel_Mossy"},
            new String[]{"Plant_Vine_Hanging", "Plant_Moss_Cave_Green"},
            new String[]{"Rubble_Stone", "Rubble_Stone_Medium"},
            "Fluid_Water",
            "Ore_Iron_Stone"
        );
    }

    /** @return the mushroom palette (moss blocks, mushroom trunks, slime). */
    @Nonnull
    public static BlockPalette mushroom() {
        return new BlockPalette(
            "mushroom",
            "Plant_Moss_Block_Green",
            "Plant_Moss_Block_Blue",
            "Plant_Moss_Cave_Green",
            "Plant_Moss_Block_Green_Dark",
            "Plant_Crop_Mushroom_Block_Blue_Trunk",
            "Plant_Crop_Mushroom_Block_Blue_Trunk",
            "Plant_Moss_Block_Green",
            "Plant_Moss_Block_Green",
            new String[]{"Plant_Moss_Block_Red", "Plant_Moss_Block_Yellow"},
            new String[]{"Plant_Vine_Hanging", "Plant_Vine_Green_Hanging"},
            new String[0],
            "Fluid_Slime",
            "Plant_Crop_Mushroom_Glowing_Blue"
        );
    }

    /** @return the hive palette (hive brick, corrupted, slime). */
    @Nonnull
    public static BlockPalette hive() {
        return new BlockPalette(
            "hive",
            "Soil_Hive_Brick",
            "Soil_Hive_Corrupted_Brick",
            "Soil_Hive",
            "Soil_Hive_Brick",
            "Soil_Hive_Brick_Beam",
            "Soil_Hive_Brick_Beam",
            "Soil_Hive_Brick",
            "Soil_Hive_Brick_Smooth",
            new String[]{"Soil_Hive_Corrupted", "Soil_Hive_Corrupted_Brick"},
            new String[]{"Deco_Scarak_Eggsacks"},
            new String[0],
            "Fluid_Slime",
            "Deco_Hive"
        );
    }

    /** @return the temple dark palette (basalt brick, dead vines, poison). */
    @Nonnull
    public static BlockPalette templeDark() {
        return new BlockPalette(
            "temple_dark",
            "Rock_Basalt_Brick",
            "Rock_Shale_Brick",
            "Rock_Basalt_Cobble",
            "Rock_Basalt_Brick",
            "Rock_Basalt_Brick_Pillar_Base",
            "Rock_Basalt_Brick_Pillar_Middle",
            "Rock_Basalt_Brick",
            "Rock_Basalt_Brick",
            new String[]{"Rock_Shale_Cobble"},
            new String[]{"Plant_Vine_Wall_Dead", "Deco_SpiderWeb"},
            new String[]{"Rubble_Basalt", "Rubble_Basalt_Medium"},
            "Fluid_Poison",
            "Rock_Shale_Brick"
        );
    }

    /**
     * Look up a palette by name.
     *
     * @param name the palette name (e.g. "crypt", "volcanic")
     * @return the matching palette, defaulting to {@link #crypt()} if unknown
     */
    @Nonnull
    public static BlockPalette fromName(@Nonnull String name) {
        return switch (name) {
            case "crypt" -> crypt();
            case "volcanic" -> volcanic();
            case "arcane" -> arcane();
            case "mine" -> mine();
            case "mushroom" -> mushroom();
            case "hive" -> hive();
            case "temple_dark" -> templeDark();
            default -> crypt();
        };
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
