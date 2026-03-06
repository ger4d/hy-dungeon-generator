package com.duntale.dungeongen.generator.theme;

import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
import java.util.Random;

/**
 * Environmental storytelling pass that adds decay, overgrowth, rubble,
 * and flooding to a carved {@link BlockGrid}.
 *
 * @since 1.0.0
 */
public class DecayPass {

    private final Random random;

    /**
     * Create a new decay pass.
     *
     * @param seed random seed for deterministic results
     */
    public DecayPass(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Replace a proportion of exposed wall blocks with decay variants
     * (mossy, cracked, etc.).
     *
     * @param grid        the block grid to modify
     * @param palette     the block palette providing decay variants
     * @param decayFactor proportion of exposed blocks to replace (0–1)
     */
    public void applyDecay(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette,
                           double decayFactor) {
        if (decayFactor <= 0 || palette.getDecayVariants().length == 0) return;
        String[] variants = palette.getDecayVariants();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                for (int z = 0; z < grid.getDepth(); z++) {
                    if (grid.isBlock(x, y, z) &&
                        grid.isExposed(x, y, z) &&
                        random.nextDouble() < decayFactor) {
                        grid.set(x, y, z, variants[random.nextInt(variants.length)]);
                    }
                }
            }
        }
    }

    /**
     * Place overgrowth blocks on air cells based on structural position:
     * <ul>
     *   <li>Floor blocks (solid below): moss, rubble-like plants</li>
     *   <li>Wall blocks (solid to the side): wall vines</li>
     *   <li>Ceiling blocks (solid above): hanging vines — skipped when
     *       {@code removeCeiling} is {@code true}</li>
     * </ul>
     *
     * @param grid             the block grid to modify
     * @param palette          the block palette providing overgrowth blocks
     * @param overgrowthFactor density factor (0–1); actual placement rate
     *                         is scaled to {@code overgrowthFactor * 0.3}
     * @param removeCeiling    whether ceilings have been stripped
     */
    public void applyOvergrowth(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette,
                                double overgrowthFactor, boolean removeCeiling) {
        if (overgrowthFactor <= 0 || !palette.hasOvergrowth()) return;

        String[] floorBlocks = palette.getOvergrowthFloor();
        String[] wallBlocks = palette.getOvergrowthWall();
        String[] ceilingBlocks = palette.getOvergrowthCeiling();

        int maxY = removeCeiling ? grid.getHeight() - 2 : grid.getHeight();
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < maxY; y++) {
                for (int z = 0; z < grid.getDepth(); z++) {
                    if (!grid.isAir(x, y, z)) continue;
                    if (random.nextDouble() >= overgrowthFactor * DungeonSettingsConfig.getDefault().getOvergrowthMultiplier()) continue;

                    // Never place overgrowth on or adjacent to fluid
                    if (grid.isFluid(x, y - 1, z)) continue;

                    boolean solidBelow = grid.isBlock(x, y - 1, z);
                    boolean solidAbove = grid.isBlock(x, y + 1, z);
                    boolean solidSide = grid.isBlock(x - 1, y, z) || grid.isBlock(x + 1, y, z)
                                     || grid.isBlock(x, y, z - 1) || grid.isBlock(x, y, z + 1);

                    if (!solidBelow && !solidAbove && !solidSide) continue;

                    // Pick a block from the appropriate category
                    String block = null;
                    if (solidBelow && floorBlocks.length > 0) {
                        block = floorBlocks[random.nextInt(floorBlocks.length)];
                    } else if (solidSide && wallBlocks.length > 0) {
                        block = wallBlocks[random.nextInt(wallBlocks.length)];
                    } else if (solidAbove && !removeCeiling && ceilingBlocks.length > 0) {
                        block = ceilingBlocks[random.nextInt(ceilingBlocks.length)];
                    }

                    if (block != null) {
                        if (solidSide && wallBlocks.length > 0 && block.equals(wallBlocks[0]) || isWallMounted(block, wallBlocks)) {
                            int rotation = wallRotation(grid, x, y, z);
                            grid.set(x, y, z, block, rotation);
                        } else {
                            grid.set(x, y, z, block);
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a block is from the wall overgrowth category (needs rotation).
     */
    private static boolean isWallMounted(@Nonnull String block, @Nonnull String[] wallBlocks) {
        for (String wb : wallBlocks) {
            if (wb.equals(block)) return true;
        }
        return false;
    }

    /**
     * Determine the yaw rotation index for a wall-mounted block based on
     * which adjacent face is solid.
     * <ul>
     *   <li>0 = attached to north wall (-Z)</li>
     *   <li>1 = attached to west wall (-X)</li>
     *   <li>2 = attached to south wall (+Z)</li>
     *   <li>3 = attached to east wall (+X)</li>
     * </ul>
     *
     * @return rotation index (0–3), or 0 if no clear wall
     */
    static int wallRotation(@Nonnull BlockGrid grid, int x, int y, int z) {
        if (grid.isBlock(x, y, z - 1)) return 0; // north
        if (grid.isBlock(x - 1, y, z)) return 1; // west
        if (grid.isBlock(x, y, z + 1)) return 2; // south
        if (grid.isBlock(x + 1, y, z)) return 3; // east
        return 0;
    }

    /**
     * Scatter rubble blocks on floor surfaces (air blocks with a solid
     * block directly below).
     *
     * @param grid          the block grid to modify
     * @param palette       the block palette providing rubble blocks
     * @param decayFactor   overall decay factor; actual rubble rate is
     *                      {@code decayFactor * 0.1}
     * @param removeCeiling whether ceilings have been stripped; when
     *                      {@code true}, the top two rows are skipped
     *                      to avoid floating rubble on the exposed surface
     */
    public void applyRubble(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette,
                            double decayFactor, boolean removeCeiling) {
        if (decayFactor <= 0 || palette.getRubbleBlocks().length == 0) return;
        String[] rubble = palette.getRubbleBlocks();

        int maxY = removeCeiling ? grid.getHeight() - 2 : grid.getHeight();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int z = 0; z < grid.getDepth(); z++) {
                // Only place rubble at the lowest floor surface in each column
                for (int y = 1; y < maxY; y++) {
                    if (grid.isAir(x, y, z) && grid.isBlock(x, y - 1, z)) {
                        if (random.nextDouble() < decayFactor * DungeonSettingsConfig.getDefault().getRubbleMultiplier()) {
                            grid.set(x, y, z, rubble[random.nextInt(rubble.length)]);
                        }
                        break; // Only the first (lowest) floor surface per column
                    }
                }
            }
        }
    }

    /**
     * Flood the lowest floor areas with the palette's fluid block.
     * Only the bottom third of the grid is considered.
     *
     * @param grid           the block grid to modify
     * @param palette        the block palette providing the fluid block
     * @param floodingFactor proportion of eligible air cells to flood (0–1)
     */
    public void applyFlooding(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette,
                              double floodingFactor) {
        if (floodingFactor <= 0 || palette.getFluidBlock() == null) return;

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int z = 0; z < grid.getDepth(); z++) {
                for (int y = 1; y < (int)(grid.getHeight() * DungeonSettingsConfig.getDefault().getFloodScanFraction()); y++) { // only lowest portion
                    if (grid.isAir(x, y, z) && grid.isSolid(x, y - 1, z)) {
                        if (random.nextDouble() < floodingFactor) {
                            grid.set(x, y, z, palette.getFluidBlock());
                        }
                    }
                }
            }
        }
    }
}
