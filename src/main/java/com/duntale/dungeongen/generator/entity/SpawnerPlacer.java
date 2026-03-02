package com.duntale.dungeongen.generator.entity;

import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.config.asset.SpawnPoolEntry;
import com.duntale.dungeongen.config.asset.SpawnPoolsEntry;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.SpawnEntry;
import com.duntale.dungeongen.model.SpawnerDefinition;
import com.duntale.dungeongen.model.SpawnerType;
import com.duntale.dungeongen.model.TriggerConfig;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Places {@link SpawnerDefinition} instances into COMBAT and BOSS rooms,
 * replacing the legacy {@code SpawnPointPlacer}. Spawners are clustered
 * by room size, with spawn pools resolved from the theme configuration
 * and filtered by floor-level eligibility.
 *
 * @since 1.1.0
 */
public class SpawnerPlacer {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Area threshold below which a room is considered small (1 cluster). */
    private static final int SMALL_ROOM_AREA = 60;
    /** Area threshold below which a room is considered medium (2 clusters). */
    private static final int MEDIUM_ROOM_AREA = 120;

    private final Random random;
    private final PacingConfig pacingConfig;

    /**
     * Create a new spawner placer.
     *
     * @param seed         RNG seed for deterministic placement
     * @param pacingConfig pacing configuration for difficulty scaling
     * @since 1.1.0
     */
    public SpawnerPlacer(long seed, @Nonnull PacingConfig pacingConfig) {
        this.random = new Random(seed);
        this.pacingConfig = pacingConfig;
    }

    /**
     * Generate {@link SpawnerDefinition} instances for all COMBAT and BOSS rooms
     * in the dungeon graph. Spawn pools are resolved from the theme's
     * {@link SpawnPoolsEntry} and filtered by floor-level eligibility.
     *
     * @param grid        the voxel grid (used to validate walkable positions)
     * @param graph       the dungeon layout graph
     * @param paletteName the theme palette name (determines spawn pool lookup)
     * @param floorLevel  the current dungeon floor level for pool filtering
     * @return list of spawner definitions placed in the dungeon
     * @since 1.1.0
     */
    @Nonnull
    public List<SpawnerDefinition> placeSpawners(@Nonnull BlockGrid grid,
                                                  @Nonnull DungeonGraph graph,
                                                  @Nonnull String paletteName,
                                                  int floorLevel) {
        List<SpawnerDefinition> spawners = new ArrayList<>();
        List<Integer> criticalPath = graph.getCriticalPath();
        DungeonSettingsConfig settings = DungeonSettingsConfig.getDefault();
        DungeonThemeConfig themeConfig = DungeonThemeConfig.get(paletteName);

        if (themeConfig == null) {
            LOGGER.atWarning().log("[DungeonGen] No theme config for palette '%s' — skipping spawner placement", paletteName);
            return spawners;
        }

        int nextId = 0;

        for (Room room : graph.getRooms()) {
            if (room.getType() != RoomType.COMBAT && room.getType() != RoomType.BOSS) continue;

            int tier = calculateTier(room, criticalPath, settings);
            int totalCount = calculateSpawnCount(room, settings);
            double radius = Math.max(room.getWidth(), room.getDepth()) / 2.0;
            TriggerConfig trigger = TriggerConfig.proximity(radius);

            if (room.getType() == RoomType.BOSS) {
                // Boss spawner
                List<SpawnEntry> bossPool = resolveSpawnPool(themeConfig, settings.getBossTier(), floorLevel);
                if (!bossPool.isEmpty()) {
                    List<Vec3i> bossOffsets = computeSpawnOffsets(grid, room.centerX(),
                        room.getY() + 1, room.centerZ(), 1, room, settings);
                    spawners.add(new SpawnerDefinition(nextId++, room.centerX(),
                        room.getY() + 1, room.centerZ(), room.getId(),
                        SpawnerType.FIXED, trigger, bossPool, 1, bossOffsets, true));
                } else {
                    LOGGER.atWarning().log("[DungeonGen] Empty boss pool for room %d on floor %d — skipping boss spawner",
                        room.getId(), floorLevel);
                }

                // Minion spawner (optional, if room large enough)
                if (totalCount > 1) {
                    List<SpawnEntry> minionPool = resolveSpawnPool(themeConfig, tier, floorLevel);
                    if (!minionPool.isEmpty()) {
                        int minionCount = totalCount - 1;
                        int mx = room.getX() + room.getWidth() / 3;
                        int mz = room.getZ() + room.getDepth() / 3;
                        List<Vec3i> minionOffsets = computeSpawnOffsets(grid, mx,
                            room.getY() + 1, mz, minionCount, room, settings);
                        spawners.add(new SpawnerDefinition(nextId++, mx,
                            room.getY() + 1, mz, room.getId(),
                            SpawnerType.FIXED, trigger, minionPool, minionCount,
                            minionOffsets, false));
                    }
                }
            } else {
                // Combat room — cluster by room size
                List<SpawnEntry> pool = resolveSpawnPool(themeConfig, tier, floorLevel);
                if (pool.isEmpty()) {
                    LOGGER.atWarning().log("[DungeonGen] Empty spawn pool for room %d (tier %d, floor %d) — skipping",
                        room.getId(), tier, floorLevel);
                    continue;
                }

                int area = room.getWidth() * room.getDepth();
                List<int[]> clusterCenters = computeClusterCenters(room, area);
                int countPerCluster = Math.max(1, totalCount / clusterCenters.size());
                int remainder = totalCount - countPerCluster * clusterCenters.size();

                for (int i = 0; i < clusterCenters.size(); i++) {
                    int cx = clusterCenters.get(i)[0];
                    int cz = clusterCenters.get(i)[1];
                    int cy = room.getY() + 1;
                    int count = countPerCluster + (i == 0 ? remainder : 0);
                    List<Vec3i> offsets = computeSpawnOffsets(grid, cx, cy, cz, count, room, settings);
                    spawners.add(new SpawnerDefinition(nextId++, cx, cy, cz,
                        room.getId(), SpawnerType.FIXED, trigger, pool, count,
                        offsets, false));
                }
            }
        }

        return spawners;
    }

    // ============================================
    // Tier & count calculation (preserved from SpawnPointPlacer)
    // ============================================

    /**
     * Calculate the difficulty tier for a room based on its position along
     * the critical path and the configured difficulty ramp.
     *
     * @param room         the room
     * @param criticalPath list of room IDs on the critical path
     * @param settings     dungeon settings
     * @return tier 1–3, or boss tier for BOSS rooms
     */
    private int calculateTier(@Nonnull Room room,
                              @Nonnull List<Integer> criticalPath,
                              @Nonnull DungeonSettingsConfig settings) {
        if (room.getType() == RoomType.BOSS) return settings.getBossTier();

        int pathIndex = criticalPath.indexOf(room.getId());
        if (pathIndex < 0) pathIndex = criticalPath.size() / 2;

        double progress = (double) pathIndex / Math.max(1, criticalPath.size() - 1);
        double adjustedProgress = progress * (settings.getDifficultyRampBase() + pacingConfig.difficultyRamp());

        if (adjustedProgress < settings.getTierThreshold1()) return 1;
        if (adjustedProgress < settings.getTierThreshold2()) return 2;
        return 3;
    }

    /**
     * Calculate how many NPCs should spawn in a room based on its area.
     *
     * @param room     the room
     * @param settings dungeon settings
     * @return spawn count
     */
    private int calculateSpawnCount(@Nonnull Room room,
                                    @Nonnull DungeonSettingsConfig settings) {
        int area = room.getWidth() * room.getDepth();
        if (room.getType() == RoomType.BOSS) {
            return settings.getBossSpawnBase() + area / settings.getBossSpawnAreaDivisor();
        }
        return settings.getCombatSpawnBase() + area / settings.getCombatSpawnAreaDivisor();
    }

    // ============================================
    // Spawn pool resolution
    // ============================================

    /**
     * Resolve the spawn pool for a given tier and floor level. Entries are
     * filtered by floor eligibility. If the resulting pool is empty, falls
     * back to the next-lower tier (3→2→1). Returns an empty list if no
     * eligible entries exist at any tier.
     *
     * @param themeConfig the theme configuration with spawn pools
     * @param tier        the desired difficulty tier (1–3, or 4+ for boss)
     * @param floorLevel  the current dungeon floor level
     * @return list of eligible {@link SpawnEntry} instances, possibly empty
     */
    @Nonnull
    private List<SpawnEntry> resolveSpawnPool(@Nonnull DungeonThemeConfig themeConfig,
                                              int tier,
                                              int floorLevel) {
        SpawnPoolsEntry pools = themeConfig.getSpawnPools();

        // Try the requested tier first
        List<SpawnEntry> result = convertPoolEntries(pools.getPoolForTier(tier), floorLevel);
        if (!result.isEmpty()) return result;

        // Fallback: try lower tiers (3→2→1)
        for (int fallback = Math.min(tier - 1, 3); fallback >= 1; fallback--) {
            result = convertPoolEntries(pools.getPoolForTier(fallback), floorLevel);
            if (!result.isEmpty()) return result;
        }

        return List.of();
    }

    /**
     * Filter and convert codec {@link SpawnPoolEntry} instances to model
     * {@link SpawnEntry} instances, keeping only those eligible for the
     * given floor level.
     *
     * @param entries    raw pool entries from the theme config
     * @param floorLevel the current dungeon floor level
     * @return filtered and converted list
     */
    @Nonnull
    private List<SpawnEntry> convertPoolEntries(@Nonnull SpawnPoolEntry[] entries,
                                                int floorLevel) {
        List<SpawnEntry> result = new ArrayList<>();
        for (SpawnPoolEntry entry : entries) {
            if (!entry.isEligibleForFloor(floorLevel)) continue;
            result.add(new SpawnEntry(
                entry.getNpcRole(),
                entry.getMinLevel(),
                entry.getMaxLevel(),
                entry.getWeight(),
                entry.getMinFloor() > 0 ? entry.getMinFloor() : null,
                entry.getMaxFloor() > 0 ? entry.getMaxFloor() : null
            ));
        }
        return result;
    }

    // ============================================
    // Clustering & offset computation
    // ============================================

    /**
     * Compute cluster center positions for a combat room based on its area.
     * <ul>
     *   <li>Small room (area &lt; 60): 1 spawner at room center</li>
     *   <li>Medium room (60–120): 2 spawners at 1/3 and 2/3 depth</li>
     *   <li>Large room (&gt; 120): 3 spawners spread across interior</li>
     * </ul>
     *
     * @param room the room
     * @param area the room's floor area (width × depth)
     * @return list of [x, z] cluster center coordinates
     */
    @Nonnull
    private List<int[]> computeClusterCenters(@Nonnull Room room, int area) {
        List<int[]> centers = new ArrayList<>();
        int cx = room.centerX();

        if (area < SMALL_ROOM_AREA) {
            // Small: 1 cluster at center
            centers.add(new int[]{cx, room.centerZ()});
        } else if (area <= MEDIUM_ROOM_AREA) {
            // Medium: 2 clusters at 1/3 and 2/3 depth
            centers.add(new int[]{cx, room.getZ() + room.getDepth() / 3});
            centers.add(new int[]{cx, room.getZ() + 2 * room.getDepth() / 3});
        } else {
            // Large: 3 clusters spread across interior
            centers.add(new int[]{room.getX() + room.getWidth() / 4, room.getZ() + room.getDepth() / 4});
            centers.add(new int[]{cx, room.centerZ()});
            centers.add(new int[]{room.getX() + 3 * room.getWidth() / 4, room.getZ() + 3 * room.getDepth() / 4});
        }

        return centers;
    }

    /**
     * Compute pre-validated spawn offset positions around a center point.
     * Attempts to find {@code count + 2} valid positions (air above solid
     * ground) within the room interior. Each offset is stored as a
     * {@link Vec3i} relative to the spawner center.
     *
     * @param grid    the voxel grid for validation
     * @param centerX spawner center X
     * @param centerY spawner center Y (floor level)
     * @param centerZ spawner center Z
     * @param count   desired number of spawn positions
     * @param room    the owning room (for interior bounds)
     * @param settings dungeon settings (for interior inset)
     * @return list of validated spawn offsets relative to (centerX, centerY, centerZ)
     */
    @Nonnull
    private List<Vec3i> computeSpawnOffsets(@Nonnull BlockGrid grid,
                                            int centerX, int centerY, int centerZ,
                                            int count,
                                            @Nonnull Room room,
                                            @Nonnull DungeonSettingsConfig settings) {
        List<Vec3i> offsets = new ArrayList<>();
        int target = count + 2;
        int inset = settings.getSpawnInteriorInset();
        int interiorMinX = room.getX() + inset;
        int interiorMaxX = room.getX() + room.getWidth() - inset - 1;
        int interiorMinZ = room.getZ() + inset;
        int interiorMaxZ = room.getZ() + room.getDepth() - inset - 1;

        // Always include the center if valid
        if (isValidSpawnPosition(grid, centerX, centerY, centerZ)) {
            offsets.add(Vec3i.ZERO);
        }

        int maxAttempts = target * settings.getSpawnAttemptMultiplier();
        for (int attempt = 0; attempt < maxAttempts && offsets.size() < target; attempt++) {
            int sx = interiorMinX + random.nextInt(Math.max(1, interiorMaxX - interiorMinX + 1));
            int sz = interiorMinZ + random.nextInt(Math.max(1, interiorMaxZ - interiorMinZ + 1));

            if (isValidSpawnPosition(grid, sx, centerY, sz)) {
                offsets.add(new Vec3i(sx - centerX, 0, sz - centerZ));
            }
        }

        // If we got nothing at all, add a zero offset as fallback
        if (offsets.isEmpty()) {
            offsets.add(Vec3i.ZERO);
        }

        return offsets;
    }

    /**
     * Check if a position is a valid spawn location: air at the position
     * and a solid block directly below.
     */
    private boolean isValidSpawnPosition(@Nonnull BlockGrid grid, int x, int y, int z) {
        return grid.isAir(x, y, z) && grid.isBlock(x, y - 1, z);
    }
}
