package com.duntale.dungeongen.generator.voxel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Classifies what kind of content occupies a {@link BlockGrid} cell.
 *
 * <p>Every known block ID used in dungeon generation is registered in a
 * static map. A lookup via {@link #of(String)} returns the category for
 * any registered block; unknown IDs default to {@link #BLOCK}.</p>
 *
 * <p>This distinction lets downstream passes make accurate placement
 * decisions — e.g. wall vines require a structural {@link #BLOCK}
 * neighbour, rubble needs a {@link #BLOCK} floor underneath, and
 * nothing should be placed on top of {@link #FLUID}.</p>
 *
 * @since 1.2.0
 */
public enum BlockCategory {

    /** Empty cell — no content. Equivalent to {@code null} in the grid. */
    AIR,

    /**
     * Structural block — stone, brick, floor tiles, ceiling slabs,
     * pillars, decay variants, etc. Provides physical support for
     * wall-mounted assets and counts as a walkable surface.
     */
    BLOCK,

    /**
     * Fluid — water, lava, poison, slime. Occupies a cell;
     * nothing should be placed on top of or adjacent to fluids.
     */
    FLUID,

    /**
     * Light source — wall torches, floor braziers, ceiling lanterns,
     * standing torches. Does <em>not</em> provide structural support.
     */
    LIGHT,

    /**
     * Trap — floor traps, wall spike traps, snap-jaws. Does
     * <em>not</em> provide structural support.
     */
    TRAP,

    /**
     * Wall decoration — vines, banners, spider webs. Requires a
     * structural {@link #BLOCK} on the attached face and correct
     * rotation.
     */
    WALL_DECO,

    /**
     * Floor decoration — moss, rubble, mushrooms, bone piles.
     * Requires a structural {@link #BLOCK} directly below.
     */
    FLOOR_DECO,

    /**
     * Miscellaneous asset — furniture, chests, coffins, statues,
     * crates, barrels, bookshelves, etc. Does not fit the other
     * non-block categories.
     */
    MISC;

    // ================================================================
    // Static registry: blockId → BlockCategory
    // ================================================================

    private static final Map<String, BlockCategory> REGISTRY = new HashMap<>();

    static {
        // ---- Fluids ----
        register(FLUID,
            "Fluid_Water", "Fluid_Lava", "Fluid_Poison", "Fluid_Slime"
        );

        // ---- Traps ----
        register(TRAP,
            "Survival_Trap_Snapjaw",
            "Survival_Trap_Spike_Iron", "Survival_Trap_Spike_Wood",
            "Survival_Trap_Spike_Wood_Large",
            "Trap_Ancient_Platform", "Trap_Ice", "Trap_Slate",
            "Survival_Trap_Grass"
        );

        // ---- Wall decorations (require BLOCK on attached face) ----
        register(WALL_DECO,
            "Plant_Vine_Wall", "Plant_Vine_Wall_Dead",
            "Deco_SpiderWeb",
            "Furniture_Human_Ruins_Banner"
        );

        // ---- Floor decorations (require BLOCK below) ----
        register(FLOOR_DECO,
            "Plant_Moss_Cave_Green",
            "Plant_Vine_Hanging", "Plant_Vine_Green_Hanging",
            "Rubble_Stone", "Rubble_Stone_Medium", "Rubble_Stone_Mossy",
            "Rubble_Volcanic", "Rubble_Volcanic_Medium",
            "Rubble_Basalt", "Rubble_Basalt_Medium",
            "Deco_Bone_Skulls", "Deco_Bone_Pile",
            "Deco_Scarak_Eggsacks",
            "Plant_Crop_Mushroom_Block_Blue",
            "Plant_Crop_Mushroom_Block_Purple",
            "Plant_Crop_Mushroom_Block_Red",
            "Deco_Iron_Stack"
        );

        // ---- Lights ----
        register(LIGHT,
            "Wood_Torch_Wall",
            "Furniture_Crude_Torch",
            "Furniture_Crude_Brazier",
            "Furniture_Human_Ruins_Torch",
            "Furniture_Human_Ruins_Lantern_Ceiling",
            "Furniture_Feran_Torch", "Furniture_Feran_Torch_Tall",
            "Furniture_Desert_Torch",
            "Furniture_Scarak_Hive_Lamp",
            "Furniture_Ancient_Candle",
            "Furniture_Dungeon_Earth_Brazier",
            "Furniture_Royal_Magic_Potion_Glow",
            "Forniture_Jungle_Brazier",
            "Forniture_Human_Ruins_Brazier",
            "Forniture_Temple_Dark_Brazier",
            "Deco_Lantern_Ceiling",
            "Plant_Crop_Mushroom_Glowing_Purple",
            "Plant_Crop_Mushroom_Glowing_Blue"
        );

        // ---- Misc assets (furniture, chests, coffins, etc.) ----
        register(MISC,
            "Furniture_Ancient_Pot", "Furniture_Ancient_Barrel",
            "Furniture_Ancient_Coffin", "Furniture_Ancient_Chest_Small",
            "Furniture_Ancient_Statue", "Furniture_Ancient_Table",
            "Furniture_Ancient_Crate", "Furniture_Ancient_Bookshelf",
            "Furniture_Crude_Chest_Small", "Furniture_Crude_Chest_Large",
            "Furniture_Dungeon_Chest_Epic", "Furniture_Dungeon_Chest_Epic_Large",
            "Furniture_Jungle_Chest_Small",
            "Furniture_Temple_Scarak_Chest_Small", "Furniture_Temple_Scarak_Pot",
            "Furniture_Temple_Dark_Statue", "Furniture_Temple_Dark_Coffin",
            "Furniture_Temple_Dark_Chest_Large", "Furniture_Temple_Dark_Pot",
            "Furniture_Royal_Magic_Table", "Furniture_Royal_Magic_Pot",
            "Bench_Furnace", "Bench_Armory",
            "Deco_Hive",
            "Deco_Iron_Chains_Vertical",
            "Rock_Crystal_Blue_Large", "Rock_Crystal_Purple_Large",
            "Wood_Darkwood_Beam"
        );

        // All structural blocks (Rock_*, Soil_*, Plant_Moss_Block_*,
        // Ore_*, Wood_Darkwood_Planks, etc.) are NOT registered.
        // They default to BLOCK via the of() method.
    }

    /**
     * Register one or more block IDs under the given category.
     */
    private static void register(@Nonnull BlockCategory category, @Nonnull String... blockIds) {
        for (String id : blockIds) {
            REGISTRY.put(id, category);
        }
    }

    // ================================================================
    // Lookup
    // ================================================================

    /**
     * Look up the category for a given block ID.
     *
     * @param blockId the block type ID, or {@code null}
     * @return the category; {@link #AIR} for {@code null},
     *         the registered category if known, or {@link #BLOCK} as default
     */
    @Nonnull
    public static BlockCategory of(@Nullable String blockId) {
        if (blockId == null) return AIR;
        return REGISTRY.getOrDefault(blockId, BLOCK);
    }

    /**
     * Whether this category represents a structural block that provides
     * physical support (only {@link #BLOCK}).
     *
     * @return {@code true} if this is a structural block
     */
    public boolean isStructural() {
        return this == BLOCK;
    }
}
