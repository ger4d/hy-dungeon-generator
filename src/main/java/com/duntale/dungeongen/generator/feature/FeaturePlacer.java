package com.duntale.dungeongen.generator.feature;

import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.voxel.BlockGrid;

import javax.annotation.Nonnull;
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

    // Regular traps — placed above floor, never replace floor blocks
    private static final String[] REGULAR_TRAPS = {
        "Survival_Trap_Snapjaw",
        "Survival_Trap_Spike_Iron",
        "Survival_Trap_Spike_Wood"
    };
    /** Spike traps that can also be placed on walls (floorY+1 to floorY+2). */
    private static final String[] WALL_SPIKE_TRAPS = {
        "Survival_Trap_Spike_Iron",
        "Survival_Trap_Spike_Wood"
    };
    /** Breakable floor traps — replace the floor block so players fall through. */
    private static final String[] FLOOR_TRAPS = {
        "Trap_Ancient_Platform",
        "Trap_Ice",
        "Trap_Slate",
        "Survival_Trap_Grass"
    };

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

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int z = 0; z < grid.getDepth(); z++) {
                if (!grid.isSolid(x, 0, z)) continue;
                if (!grid.isAir(x, 1, z)) continue;

                if (random.nextDouble() >= config.trapDensity() * 0.05) continue;

                // Floor traps (breakable platforms) — only when floorTraps is enabled.
                // Replace the floor block at Y=0 so players fall through.
                if (config.floorTraps() && random.nextDouble() < 0.35) {
                    grid.set(x, 0, z, FLOOR_TRAPS[random.nextInt(FLOOR_TRAPS.length)]);
                    continue;
                }

                // Regular traps — placed above the floor at Y=1
                boolean hasAdjacentWall = grid.isSolid(x, 1, z - 1) || grid.isSolid(x, 1, z + 1)
                                       || grid.isSolid(x - 1, 1, z) || grid.isSolid(x + 1, 1, z);

                // Wall-mounted spike traps (can go up to floorY+2) next to walls
                if (hasAdjacentWall && random.nextDouble() < 0.4) {
                    String trap = WALL_SPIKE_TRAPS[random.nextInt(WALL_SPIKE_TRAPS.length)];
                    int wallY = 1 + random.nextInt(2); // Y=1 or Y=2
                    if (wallY < grid.getHeight() && grid.isAir(x, wallY, z)) {
                        int rotation = wallRotation(grid, x, wallY, z);
                        grid.set(x, wallY, z, trap, rotation);
                    }
                } else {
                    // Floor-level regular trap at Y=1
                    String trap = REGULAR_TRAPS[random.nextInt(REGULAR_TRAPS.length)];
                    grid.set(x, 1, z, trap);
                }
            }
        }
    }

    /**
     * Determine the yaw rotation index for a wall-adjacent block.
     * <ul>
     *   <li>0 = north wall (-Z)</li>
     *   <li>1 = west wall (-X)</li>
     *   <li>2 = south wall (+Z)</li>
     *   <li>3 = east wall (+X)</li>
     * </ul>
     */
    private int wallRotation(@Nonnull BlockGrid grid, int x, int y, int z) {
        if (grid.isSolid(x, y, z - 1)) return 0;
        if (grid.isSolid(x - 1, y, z)) return 1;
        if (grid.isSolid(x, y, z + 1)) return 2;
        if (grid.isSolid(x + 1, y, z)) return 3;
        return 0;
    }

    // ============================================
    // Secret walls
    // ============================================

    // TODO: Secret wall passages — currently disabled. Future implementation ideas:
    //  1. Raycast from both sides of the candidate wall to verify rooms/corridors
    //     exist on each side (not void/exterior).
    //  2. Verify floor continuity below the ray paths so players can walk through.
    //  3. Support double-block-thick walls — carve both blocks in the passage.
    //  4. Only pick walls that connect two disconnected areas or create shortcuts.
    private void placeSecretWalls(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                  @Nonnull LayoutConfig config) {
        // Disabled pending proper wall safety checks.
    }
}
