package com.duntale.dungeongen.generator.theme;

import com.duntale.dungeongen.config.ThemeConfig;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.voxel.BlockCategory;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Orchestrates all theme application to a carved {@link BlockGrid}:
 * material assignment, architectural details (pillars), and environmental
 * passes (decay, overgrowth, rubble, flooding).
 *
 * @since 1.0.0
 */
public class ThemeDecorator {

    private final Random random;
    private final ThemeConfig themeConfig;

    /**
     * Create a new theme decorator.
     *
     * @param seed        random seed for deterministic decoration
     * @param themeConfig the theme configuration
     */
    public ThemeDecorator(long seed, @Nonnull ThemeConfig themeConfig) {
        this.random = new Random(seed);
        this.themeConfig = themeConfig;
    }

    /**
     * Apply the full theme to a carved block grid.
     * <ol>
     *   <li>Replace fill blocks with themed walls / floor / ceiling.</li>
     *   <li>Add pillars in large rooms.</li>
     *   <li>Apply decay pass.</li>
     *   <li>Apply overgrowth pass.</li>
     *   <li>Apply rubble pass.</li>
     *   <li>Apply flooding pass.</li>
     * </ol>
     *
     * @param grid  the carved block grid to decorate
     * @param graph the dungeon layout graph (used for room metadata)
     */
    public void applyTheme(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph, boolean removeCeiling) {
        BlockPalette palette = BlockPalette.fromName(themeConfig.palette());

        // Phase 1: Material assignment — replace generic fill with themed blocks
        applyMaterials(grid, palette);

        // Phase 2: Architectural details — pillars in large rooms
        addPillars(grid, graph, palette);

        // Phase 3: Environmental passes
        DecayPass decayPass = new DecayPass(random.nextLong());
        decayPass.applyDecay(grid, palette, themeConfig.decayFactor());
        decayPass.applyOvergrowth(grid, palette, themeConfig.overgrowthFactor(), removeCeiling);
        decayPass.applyRubble(grid, palette, themeConfig.decayFactor(), removeCeiling);
        decayPass.applyFlooding(grid, palette, themeConfig.floodingFactor());
    }

    /**
     * Replace generic fill blocks with themed materials based on their
     * structural position (floor, ceiling, wall, or interior).
     */
    private void applyMaterials(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette) {
        // Load secondary wall chance from theme config asset (default 0.2 = 20%)
        double secondaryWallChance = 0.2;
        DungeonThemeConfig themeAsset = DungeonThemeConfig.get(themeConfig.palette());
        if (themeAsset != null) {
            secondaryWallChance = themeAsset.getSecondaryWallChance();
        }

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                for (int z = 0; z < grid.getDepth(); z++) {
                    // Only re-theme structural blocks — skip fluids, traps,
                    // and any other non-BLOCK content placed by FeaturePlacer.
                    if (!grid.isBlock(x, y, z)) continue;

                    // boolean airAbove = grid.isAir(x, y + 1, z);
                    // Treat out-of-bounds below (y == 0) as solid — the grid
                    // bottom is structurally equivalent to bedrock.
                    // boolean airBelow = y > 0 && grid.isAir(x, y - 1, z);
                    boolean airSide = grid.isAir(x - 1, y, z) || grid.isAir(x + 1, y, z) ||
                                      grid.isAir(x, y, z - 1) || grid.isAir(x, y, z + 1);

                    // if (!airAbove && !airBelow) {
                    if (y == 0) {
                        // Floor block
                        grid.set(x, y, z, palette.getFloor());
                    // } else if (airBelow && !airAbove) {
                    } else if (y == grid.getHeight() - 1) {
                        // Ceiling block
                        grid.set(x, y, z, palette.getCeiling());
                    } else if (airSide) {
                        // Wall block — occasionally use secondary wall
                        String wall = (random.nextDouble() < secondaryWallChance)
                            ? palette.getSecondaryWall()
                            : palette.getPrimaryWall();
                        grid.set(x, y, z, wall);
                    }
                    // else: interior block stays as original fill
                }
            }
        }
    }

    /**
     * Place decorative pillars near the corners of rooms whose width
     * and depth meet the configured minimum size.
     */
    private void addPillars(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                            @Nonnull BlockPalette palette) {
        DungeonSettingsConfig settings = DungeonSettingsConfig.getDefault();
        int minRoomSize = settings.getPillarMinRoomSize();
        double skipChance = settings.getPillarSkipChance();

        for (Room room : graph.getRooms()) {
            if (room.getWidth() < minRoomSize || room.getDepth() < minRoomSize) continue;

            if (random.nextDouble() < skipChance) continue;

            // Place pillars 2 blocks in from each corner of the room interior
            int[][] pillarOffsets = {{2, 2}, {2, -3}, {-3, 2}, {-3, -3}};

            for (int[] offset : pillarOffsets) {
                int px = room.getX() + (offset[0] >= 0 ? offset[0] : room.getWidth() + offset[0]);
                int pz = room.getZ() + (offset[1] >= 0 ? offset[1] : room.getDepth() + offset[1]);

                // Place pillar base at floor + 1
                int floorY = room.getY() + 1;
                grid.set(px, floorY, pz, palette.getPillarBase());

                // Fill pillar middle
                for (int y = floorY + 1; y < room.getY() + room.getHeight() - 2; y++) {
                    grid.set(px, y, pz, palette.getPillarMiddle());
                }
            }
        }
    }
}
