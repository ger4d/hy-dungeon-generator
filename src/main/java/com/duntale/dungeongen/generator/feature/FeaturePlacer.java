package com.duntale.dungeongen.generator.feature;

import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

/**
 * Places dungeon features (pillars, water pools, lava pools, traps,
 * and secret walls) into the voxel grid after initial carving and
 * before theme decoration.
 *
 * <p>Each feature type is gated by its corresponding config value;
 * values &le; 0.01 disable the feature entirely.</p>
 *
 * @since 1.1.0
 */
public class FeaturePlacer {

    private static final String PILLAR_BLOCK = "Rock_Stone_Brick";
    private static final String WATER_BLOCK = "Fluid_Water";
    private static final String LAVA_BLOCK = "Fluid_Lava";
    private static final String TRAP_BLOCK = "Survival_Trap_Spike_Wood_Large";

    private final Random random;

    /**
     * Create a new feature placer.
     *
     * @param seed random seed for deterministic feature distribution
     */
    public FeaturePlacer(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Place all configured features into the block grid.
     *
     * @param grid   the voxel grid (post-carve, pre-theme)
     * @param graph  the dungeon layout graph
     * @param config layout configuration controlling feature densities
     */
    public void placeFeatures(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                              @Nonnull LayoutConfig config) {
        placePillars(grid, graph, config);
        placeWaterPools(grid, graph, config);
        placeLavaPools(grid, graph, config);
        placeTraps(grid, graph, config);
        placeSecretWalls(grid, graph, config);
    }

    // ============================================
    // Pillars
    // ============================================

    private void placePillars(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                              @Nonnull LayoutConfig config) {
        if (config.pillarFrequency() <= 0.01) return;

        for (Room room : graph.getRooms()) {
            if (room.getWidth() < 6 || room.getDepth() < 6) continue;

            int spacing = Math.max(3, (int) (6.0 - config.pillarFrequency() * 4.0));

            for (int dx = 2; dx < room.getWidth() - 2; dx += spacing) {
                for (int dz = 2; dz < room.getDepth() - 2; dz += spacing) {
                    int px = room.getX() + dx;
                    int pz = room.getZ() + dz;

                    if (random.nextDouble() >= config.pillarFrequency()) continue;
                    if (!grid.isAir(px, room.getY() + 1, pz)) continue;

                    // Place pillar column from above floor to below ceiling
                    for (int y = room.getY() + 1; y < room.getY() + room.getHeight() - 1; y++) {
                        grid.set(px, y, pz, PILLAR_BLOCK);
                    }
                }
            }
        }
    }

    // ============================================
    // Water pools
    // ============================================

    private void placeWaterPools(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                 @Nonnull LayoutConfig config) {
        if (config.waterFrequency() <= 0.01) return;

        for (Room room : graph.getRooms()) {
            if (room.isEntrance()) continue;
            if (random.nextDouble() >= config.waterFrequency()) continue;

            int poolW = Math.max(2, room.getWidth() / 3);
            int poolD = Math.max(2, room.getDepth() / 3);
            int startX = room.getX() + room.getWidth() / 2 - poolW / 2;
            int startZ = room.getZ() + room.getDepth() / 2 - poolD / 2;

            for (int dx = 0; dx < poolW; dx++) {
                for (int dz = 0; dz < poolD; dz++) {
                    int px = startX + dx;
                    int pz = startZ + dz;
                    // Replace floor block with water (flush pool)
                    if (grid.isSolid(px, room.getY(), pz)) {
                        grid.set(px, room.getY(), pz, WATER_BLOCK);
                    }
                }
            }
        }
    }

    // ============================================
    // Lava pools
    // ============================================

    private void placeLavaPools(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                @Nonnull LayoutConfig config) {
        if (config.lavaFrequency() <= 0.01) return;

        for (Room room : graph.getRooms()) {
            if (room.isEntrance()) continue;
            if (random.nextDouble() >= config.lavaFrequency()) continue;

            int poolW = Math.max(2, room.getWidth() / 4);
            int poolD = Math.max(2, room.getDepth() / 4);
            int startX = room.getX() + room.getWidth() / 2 - poolW / 2;
            int startZ = room.getZ() + room.getDepth() / 2 - poolD / 2;

            for (int dx = 0; dx < poolW; dx++) {
                for (int dz = 0; dz < poolD; dz++) {
                    int px = startX + dx;
                    int pz = startZ + dz;
                    if (grid.isSolid(px, room.getY(), pz)) {
                        grid.set(px, room.getY(), pz, LAVA_BLOCK);
                    }
                }
            }
        }
    }

    // ============================================
    // Traps
    // ============================================

    private void placeTraps(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                            @Nonnull LayoutConfig config) {
        if (config.trapDensity() <= 0.01) return;

        List<Room> rooms = graph.getRooms();

        // Iterate all floor-level cells; place traps in corridor space
        // (solid floor at Y=0 with air above, outside any room bounding box).
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int z = 0; z < grid.getDepth(); z++) {
                if (!grid.isSolid(x, 0, z)) continue;
                if (!grid.isAir(x, 1, z)) continue;
                if (isInsideRoom(x, z, rooms)) continue;

                if (random.nextDouble() < config.trapDensity() * 0.05) {
                    grid.set(x, 0, z, TRAP_BLOCK);
                }
            }
        }
    }

    /**
     * Check whether an XZ position falls inside any room bounding box.
     */
    private boolean isInsideRoom(int x, int z, @Nonnull List<Room> rooms) {
        for (Room room : rooms) {
            if (x >= room.getX() && x < room.getX() + room.getWidth()
                && z >= room.getZ() && z < room.getZ() + room.getDepth()) {
                return true;
            }
        }
        return false;
    }

    // ============================================
    // Secret walls
    // ============================================

    private void placeSecretWalls(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                  @Nonnull LayoutConfig config) {
        if (config.secretWallChance() <= 0.01) return;

        for (int x = 1; x < grid.getWidth() - 1; x++) {
            for (int z = 1; z < grid.getDepth() - 1; z++) {
                for (int y = 1; y < grid.getHeight() - 1; y++) {
                    if (grid.isAir(x, y, z)) continue;

                    // Check if wall separates two air volumes on opposite sides (X or Z)
                    boolean passageX = grid.isAir(x - 1, y, z) && grid.isAir(x + 1, y, z);
                    boolean passageZ = grid.isAir(x, y, z - 1) && grid.isAir(x, y, z + 1);

                    if ((passageX || passageZ)
                        && random.nextDouble() < config.secretWallChance() * 0.1) {
                        grid.set(x, y, z, null); // Remove wall → secret passage
                    }
                }
            }
        }
    }
}
