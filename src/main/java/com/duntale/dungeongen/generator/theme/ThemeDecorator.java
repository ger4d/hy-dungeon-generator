package com.duntale.dungeongen.generator.theme;

import com.duntale.dungeongen.config.ThemeConfig;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
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
        decayPass.applyRubble(grid, palette, themeConfig.decayFactor());
        decayPass.applyFlooding(grid, palette, themeConfig.floodingFactor());
    }

    /**
     * Replace generic fill blocks with themed materials based on their
     * structural position (floor, ceiling, wall, or interior).
     */
    private void applyMaterials(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette) {
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                for (int z = 0; z < grid.getDepth(); z++) {
                    if (grid.isAir(x, y, z)) continue;

                    boolean airAbove = grid.isAir(x, y + 1, z);
                    boolean airBelow = grid.isAir(x, y - 1, z);
                    boolean airSide = grid.isAir(x - 1, y, z) || grid.isAir(x + 1, y, z) ||
                                      grid.isAir(x, y, z - 1) || grid.isAir(x, y, z + 1);

                    if (airAbove && !airBelow) {
                        // Floor block
                        grid.set(x, y, z, palette.getFloor());
                    } else if (airBelow && !airAbove) {
                        // Ceiling block
                        grid.set(x, y, z, palette.getCeiling());
                    } else if (airSide) {
                        // Wall block — occasionally use secondary wall
                        String wall = (random.nextInt(5) == 0)
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
     * and depth are both at least 8 blocks.
     */
    private void addPillars(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                            @Nonnull BlockPalette palette) {
        for (Room room : graph.getRooms()) {
            if (room.getWidth() < 8 || room.getDepth() < 8) continue;

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
