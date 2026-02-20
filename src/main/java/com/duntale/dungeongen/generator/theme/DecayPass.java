package com.duntale.dungeongen.generator.theme;

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
                    if (grid.isExposed(x, y, z) && random.nextDouble() < decayFactor) {
                        grid.set(x, y, z, variants[random.nextInt(variants.length)]);
                    }
                }
            }
        }
    }

    /**
     * Place overgrowth blocks (vines, moss, roots) on air cells that are
     * adjacent to a solid block.
     *
     * @param grid             the block grid to modify
     * @param palette          the block palette providing overgrowth blocks
     * @param overgrowthFactor density factor (0–1); actual placement rate
     *                         is scaled to {@code overgrowthFactor * 0.3}
     */
    public void applyOvergrowth(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette,
                                double overgrowthFactor) {
        if (overgrowthFactor <= 0 || palette.getOvergrowthBlocks().length == 0) return;
        String[] blocks = palette.getOvergrowthBlocks();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight(); y++) {
                for (int z = 0; z < grid.getDepth(); z++) {
                    if (grid.isAir(x, y, z) && hasAdjacentSolid(grid, x, y, z)) {
                        if (random.nextDouble() < overgrowthFactor * 0.3) {
                            grid.set(x, y, z, blocks[random.nextInt(blocks.length)]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Scatter rubble blocks on floor surfaces (air blocks with a solid
     * block directly below).
     *
     * @param grid        the block grid to modify
     * @param palette     the block palette providing rubble blocks
     * @param decayFactor overall decay factor; actual rubble rate is
     *                    {@code decayFactor * 0.1}
     */
    public void applyRubble(@Nonnull BlockGrid grid, @Nonnull BlockPalette palette,
                            double decayFactor) {
        if (decayFactor <= 0 || palette.getRubbleBlocks().length == 0) return;
        String[] rubble = palette.getRubbleBlocks();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 1; y < grid.getHeight(); y++) {
                for (int z = 0; z < grid.getDepth(); z++) {
                    if (grid.isAir(x, y, z) && grid.isSolid(x, y - 1, z)) {
                        if (random.nextDouble() < decayFactor * 0.1) {
                            grid.set(x, y, z, rubble[random.nextInt(rubble.length)]);
                        }
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
                for (int y = 1; y < grid.getHeight() / 3; y++) { // only lowest third
                    if (grid.isAir(x, y, z) && grid.isSolid(x, y - 1, z)) {
                        if (random.nextDouble() < floodingFactor) {
                            grid.set(x, y, z, palette.getFluidBlock());
                        }
                    }
                }
            }
        }
    }

    private boolean hasAdjacentSolid(@Nonnull BlockGrid grid, int x, int y, int z) {
        return grid.isSolid(x - 1, y, z) || grid.isSolid(x + 1, y, z) ||
               grid.isSolid(x, y - 1, z) || grid.isSolid(x, y + 1, z) ||
               grid.isSolid(x, y, z - 1) || grid.isSolid(x, y, z + 1);
    }
}
