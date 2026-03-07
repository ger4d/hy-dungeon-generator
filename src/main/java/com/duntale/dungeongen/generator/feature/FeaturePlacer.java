package com.duntale.dungeongen.generator.feature;

import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.config.asset.TrapEntry;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.DungeonBlueprint;
import com.duntale.dungeongen.model.MerchantDefinition;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
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

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

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
     * @param grid        the voxel grid (post-carve, pre-theme)
     * @param graph       the dungeon layout graph
     * @param config      layout configuration controlling feature densities
     * @param paletteName the theme palette name (determines pool/trap blocks)
     */
    public void placeFeatures(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                              @Nonnull LayoutConfig config, @Nonnull String paletteName) {
        DungeonThemeConfig themeConfig = DungeonThemeConfig.get(paletteName);
        String waterBlock = themeConfig != null ? themeConfig.getPalette().getFluidBlock() : "Fluid_Water";
        String lavaBlock = themeConfig != null ? themeConfig.getPalette().getSecondaryFluidBlock() : "Fluid_Lava";

        placeWaterPools(grid, graph, config, waterBlock);
        placeLavaPools(grid, graph, config, lavaBlock);
        placeTraps(grid, graph, config, paletteName);
    }

    // ============================================
    // Water pools
    // ============================================

    private void placeWaterPools(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                 @Nonnull LayoutConfig config, @Nonnull String waterBlock) {
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
                    if (grid.isBlock(px, room.getY(), pz)) {
                        grid.set(px, room.getY(), pz, waterBlock);
                    }
                }
            }
        }
    }

    // ============================================
    // Lava pools
    // ============================================

    private void placeLavaPools(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                                @Nonnull LayoutConfig config, @Nonnull String lavaBlock) {
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
                    if (grid.isBlock(px, room.getY(), pz)) {
                        grid.set(px, room.getY(), pz, lavaBlock);
                    }
                }
            }
        }
    }

    // ============================================
    // Traps
    // ============================================

    private void placeTraps(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                            @Nonnull LayoutConfig config, @Nonnull String paletteName) {
        if (config.trapDensity() <= 0.01) return;

        DungeonSettingsConfig settings = DungeonSettingsConfig.getDefault();
        double densityMultiplier = settings.getTrapDensityMultiplier();
        double floorTrapChance = settings.getFloorTrapChance();
        double wallSpikeChance = settings.getWallSpikeChance();

        // Load trap blocks from theme config
        DungeonThemeConfig themeConfig = DungeonThemeConfig.get(paletteName);
        TrapEntry trapEntry = themeConfig != null ? themeConfig.getTraps() : new TrapEntry();
        String[] regularTraps = trapEntry.getRegularTraps();
        String[] wallSpikeTraps = trapEntry.getWallSpikeTraps();
        String[] floorTraps = trapEntry.getFloorTraps();

        for (int x = 0; x < grid.getWidth(); x++) {
            for (int z = 0; z < grid.getDepth(); z++) {
                if (!grid.isBlock(x, 0, z)) continue;
                if (!grid.isAir(x, 1, z)) continue;

                if (random.nextDouble() >= config.trapDensity() * densityMultiplier) continue;

                // Floor traps (breakable platforms) — only when floorTraps is enabled.
                // Replace the floor block at Y=0 so players fall through.
                if (config.floorTraps() && floorTraps.length > 0 && random.nextDouble() < floorTrapChance) {
                    grid.set(x, 0, z, floorTraps[random.nextInt(floorTraps.length)]);
                    continue;
                }

                // Regular traps — placed above the floor at Y=1
                boolean hasAdjacentWall = grid.isBlock(x, 1, z - 1) || grid.isBlock(x, 1, z + 1)
                                       || grid.isBlock(x - 1, 1, z) || grid.isBlock(x + 1, 1, z);

                // Wall-mounted spike traps (can go up to floorY+2) next to walls
                if (hasAdjacentWall && wallSpikeTraps.length > 0 && random.nextDouble() < wallSpikeChance) {
                    String trap = wallSpikeTraps[random.nextInt(wallSpikeTraps.length)];
                    int wallY = 1 + random.nextInt(2); // Y=1 or Y=2
                    if (wallY < grid.getHeight() && grid.isAir(x, wallY, z)) {
                        int rotation = wallRotation(grid, x, wallY, z);
                        grid.set(x, wallY, z, trap, rotation);
                    }
                } else if (regularTraps.length > 0) {
                    // Floor-level regular trap at Y=1
                    String trap = regularTraps[random.nextInt(regularTraps.length)];
                    LOGGER.atInfo().log("[DungeonGen] Placing trap %s at (%d, %d, %d)", trap, x, 1, z);
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
        if (grid.isBlock(x, y, z - 1)) return 0;
        if (grid.isBlock(x - 1, y, z)) return 1;
        if (grid.isBlock(x, y, z + 1)) return 2;
        if (grid.isBlock(x + 1, y, z)) return 3;
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

    // ============================================
    // Merchants (near fluids)
    // ============================================

    /**
     * Place merchant definitions in rooms that contain fluid (water or lava).
     * Each eligible room has a {@code config.merchantSpawnChance()} probability
     * of receiving a merchant, positioned on solid ground adjacent to a fluid block.
     *
     * @param grid       the voxel grid (post-feature placement)
     * @param graph      the dungeon layout graph
     * @param config     layout config controlling merchant spawn chance
     * @param blueprint  the blueprint to register merchants into
     * @param floorLevel the dungeon floor level for the merchant definition
     */
    public void placeMerchants(@Nonnull BlockGrid grid, @Nonnull DungeonGraph graph,
                               @Nonnull LayoutConfig config, @Nonnull DungeonBlueprint blueprint,
                               int floorLevel) {
        if (config.merchantSpawnChance() <= 0.01) return;

        for (Room room : graph.getRooms()) {
            if (room.isEntrance()) continue;
            if (!roomHasFluid(grid, room)) continue;
            if (random.nextDouble() >= config.merchantSpawnChance()) continue;

            int[] pos = findMerchantPosition(grid, room);
            if (pos != null) {
                blueprint.addMerchant(new MerchantDefinition(pos[0], pos[1], pos[2], floorLevel));
                LOGGER.atInfo().log("[DungeonGen] Placed merchant at (%d, %d, %d) in room %d",
                    pos[0], pos[1], pos[2], room.getId());
                return; // Only one merchant per dungeon for now; remove this to allow multiple merchants in different rooms
            }
        }
    }

    /**
     * Check whether a room contains any fluid block at the floor level.
     */
    private boolean roomHasFluid(@Nonnull BlockGrid grid, @Nonnull Room room) {
        int y = room.getY();
        for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
            for (int z = room.getZ(); z < room.getZ() + room.getDepth(); z++) {
                if (grid.isFluid(x, y, z)) return true;
            }
        }
        return false;
    }

    /**
     * Find a valid merchant position adjacent to fluid in the given room.
     * Valid = air at (x, y+1, z) and solid/block at (x, y, z), with at least
     * one cardinal neighbor being fluid at floor level.
     *
     * @return {@code int[]{x, y+1, z}} or {@code null} if no valid position found
     */
    @Nullable
    private int[] findMerchantPosition(@Nonnull BlockGrid grid, @Nonnull Room room) {
        int y = room.getY();
        int cx = room.centerX();
        int cz = room.centerZ();

        // Collect candidate positions, pick the one closest to room center
        List<int[]> candidates = new ArrayList<>();
        for (int x = room.getX(); x < room.getX() + room.getWidth(); x++) {
            for (int z = room.getZ(); z < room.getZ() + room.getDepth(); z++) {
                if (!grid.isBlock(x, y, z)) continue;
                if (!grid.isAir(x, y + 1, z)) continue;

                // Check if any cardinal neighbor at floor level is fluid
                boolean adjacentFluid = grid.isFluid(x - 1, y, z) || grid.isFluid(x + 1, y, z)
                                     || grid.isFluid(x, y, z - 1) || grid.isFluid(x, y, z + 1);
                if (adjacentFluid) {
                    candidates.add(new int[]{x, y + 1, z});
                }
            }
        }

        if (candidates.isEmpty()) return null;

        // Pick candidate closest to room center
        candidates.sort((a, b) -> {
            int distA = Math.abs(a[0] - cx) + Math.abs(a[2] - cz);
            int distB = Math.abs(b[0] - cx) + Math.abs(b[2] - cz);
            return Integer.compare(distA, distB);
        });
        return candidates.getFirst();
    }
}
