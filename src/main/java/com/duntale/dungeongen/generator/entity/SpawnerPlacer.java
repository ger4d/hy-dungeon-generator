package com.duntale.dungeongen.generator.entity;

import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.config.asset.SpawnPoolEntry;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.SpawnEntry;
import com.duntale.dungeongen.model.SpawnerDefinition;
import com.duntale.dungeongen.model.SpawnerType;
import com.duntale.dungeongen.model.SpawnerVariant;
import com.duntale.dungeongen.model.TriggerConfig;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Places {@link SpawnerDefinition} instances into COMBAT and BOSS rooms.
 * Spawners are clustered by room size, with spawn pools resolved from a
 * flat theme pool filtered by floor-level eligibility and variant tags.
 *
 * <p>Elite spawns are distributed across combat rooms via a floor-wide
 * budget derived from {@link DungeonSettingsConfig} elite ratio settings.</p>
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

    private record VariantPools(
        @Nonnull List<SpawnEntry> normalPool,
        @Nonnull List<SpawnEntry> elitePool,
        @Nonnull List<SpawnEntry> bossPool
    ) {}

    private record PlacementContext(
        @Nonnull BlockGrid grid,
        @Nonnull DungeonSettingsConfig settings,
        int floorLevel,
        int levelVariance,
        @Nonnull VariantPools pools
    ) {}

    private record CombatRoomPlan(
        @Nonnull Room room,
        int totalCount,
        @Nonnull TriggerConfig trigger,
        @Nonnull List<int[]> clusterCenters
    ) {}

    /**
     * Create a new spawner placer.
     *
     * @param seed         RNG seed for deterministic placement
     * @param pacingConfig pacing configuration (reserved for future use)
     * @since 1.1.0
     */
    public SpawnerPlacer(long seed, @Nonnull PacingConfig pacingConfig) {
        this.random = new Random(seed);
        this.pacingConfig = pacingConfig;
    }

    /**
     * Generate {@link SpawnerDefinition} instances for all COMBAT and BOSS rooms
     * in the dungeon graph. The theme's flat spawn pool is filtered by floor-level
     * eligibility and variant tags. Elite spawns are distributed across combat
     * rooms via a floor-wide budget.
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
        DungeonSettingsConfig settings = DungeonSettingsConfig.getDefault();
        DungeonThemeConfig themeConfig = DungeonThemeConfig.get(paletteName);

        if (themeConfig == null) {
            LOGGER.atWarning().log("[DungeonGen] No theme config for palette '%s' — skipping spawner placement", paletteName);
            return spawners;
        }

        PlacementContext context = createPlacementContext(grid, themeConfig, settings, floorLevel);
        List<CombatRoomPlan> combatPlans = buildCombatRoomPlans(graph, settings);
        int nextId = appendCombatSpawners(spawners, combatPlans, context, 0);
        appendBossSpawners(spawners, graph, context, nextId);
        return spawners;
    }

    @Nonnull
    private PlacementContext createPlacementContext(@Nonnull BlockGrid grid,
                                                    @Nonnull DungeonThemeConfig themeConfig,
                                                    @Nonnull DungeonSettingsConfig settings,
                                                    int floorLevel) {
        int levelVariance = themeConfig.getLevelVariance();
        SpawnPoolEntry[] pool = themeConfig.getSpawnPool();
        VariantPools pools = new VariantPools(
            filterPool(pool, floorLevel, SpawnerVariant.NORMAL),
            filterPool(pool, floorLevel, SpawnerVariant.ELITE),
            filterPool(pool, floorLevel, SpawnerVariant.BOSS)
        );
        return new PlacementContext(grid, settings, floorLevel, levelVariance, pools);
    }

    @Nonnull
    private List<CombatRoomPlan> buildCombatRoomPlans(@Nonnull DungeonGraph graph,
                                                      @Nonnull DungeonSettingsConfig settings) {
        List<CombatRoomPlan> combatPlans = new ArrayList<>();
        for (Room room : graph.getRooms()) {
            if (room.getType() != RoomType.COMBAT) {
                continue;
            }

            int totalCount = calculateSpawnCount(room, settings);
            double radius = Math.max(room.getWidth(), room.getDepth()) / 2.0;
            int area = room.getWidth() * room.getDepth();
            combatPlans.add(new CombatRoomPlan(
                room,
                totalCount,
                TriggerConfig.proximity(radius),
                computeClusterCenters(room, area)
            ));
        }
        return combatPlans;
    }

    private int appendCombatSpawners(@Nonnull List<SpawnerDefinition> spawners,
                                     @Nonnull List<CombatRoomPlan> combatPlans,
                                     @Nonnull PlacementContext context,
                                     int nextId) {
        int[] eliteCounts = allocateCombatEliteCounts(combatPlans, context);
        for (int i = 0; i < combatPlans.size(); i++) {
            CombatRoomPlan plan = combatPlans.get(i);
            int eliteCount = eliteCounts[i];
            int normalCount = plan.totalCount() - eliteCount;
            nextId = appendCombatRoomSpawners(spawners, plan, eliteCount, normalCount, context, nextId);
        }
        return nextId;
    }

    private int[] allocateCombatEliteCounts(@Nonnull List<CombatRoomPlan> combatPlans,
                                            @Nonnull PlacementContext context) {
        int totalCombatSpawns = 0;
        for (CombatRoomPlan plan : combatPlans) {
            totalCombatSpawns += plan.totalCount();
        }

        int eliteBudget = computeEliteBudget(totalCombatSpawns, context);
        int[] eliteCapacity = computeEliteCapacities(combatPlans, context);
        int totalCapacity = 0;
        for (int capacity : eliteCapacity) {
            totalCapacity += capacity;
        }
        eliteBudget = Math.min(eliteBudget, totalCapacity);

        return distributeEliteBudget(combatPlans, eliteCapacity, eliteBudget, context.settings());
    }

    private int computeEliteBudget(int totalCombatSpawns,
                                   @Nonnull PlacementContext context) {
        double eliteRatio = computeEliteRatio(context.floorLevel(), context.settings());
        double expectedBudget = eliteRatio * totalCombatSpawns;
        int eliteBudget = (int) Math.floor(expectedBudget);
        double fractionalBudget = expectedBudget - eliteBudget;
        if (fractionalBudget > 0 && random.nextDouble() < fractionalBudget) {
            eliteBudget++;
        }
        return eliteBudget;
    }

    @Nonnull
    private int[] computeEliteCapacities(@Nonnull List<CombatRoomPlan> combatPlans,
                                         @Nonnull PlacementContext context) {
        int[] eliteCapacity = new int[combatPlans.size()];
        if (context.pools().elitePool().isEmpty()) {
            return eliteCapacity;
        }

        for (int i = 0; i < combatPlans.size(); i++) {
            eliteCapacity[i] = Math.min(combatPlans.get(i).totalCount(), context.settings().getMaxElitesPerCombatRoom());
        }
        return eliteCapacity;
    }

    @Nonnull
    private int[] distributeEliteBudget(@Nonnull List<CombatRoomPlan> combatPlans,
                                        @Nonnull int[] eliteCapacity,
                                        int eliteBudget,
                                        @Nonnull DungeonSettingsConfig settings) {
        int[] eliteCounts = new int[combatPlans.size()];
        List<Integer> eligibleIndices = new ArrayList<>();
        double[] roomWeights = new double[combatPlans.size()];
        double weightExponent = settings.getEliteRoomWeightExponent();

        for (int i = 0; i < combatPlans.size(); i++) {
            if (eliteCapacity[i] <= 0) {
                continue;
            }

            eligibleIndices.add(i);
            roomWeights[i] = Math.pow(combatPlans.get(i).totalCount(), weightExponent);
        }

        int remaining = eliteBudget;
        while (remaining > 0 && !eligibleIndices.isEmpty()) {
            int pick = pickWeightedRoomIndex(eligibleIndices, roomWeights);
            eliteCounts[pick]++;
            if (eliteCounts[pick] >= eliteCapacity[pick]) {
                eligibleIndices.remove(Integer.valueOf(pick));
            }
            remaining--;
        }

        return eliteCounts;
    }

    private int pickWeightedRoomIndex(@Nonnull List<Integer> eligibleIndices,
                                      @Nonnull double[] roomWeights) {
        double totalWeight = 0;
        for (int idx : eligibleIndices) {
            totalWeight += roomWeights[idx];
        }

        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;
        int pick = eligibleIndices.getLast();
        for (int idx : eligibleIndices) {
            cumulative += roomWeights[idx];
            if (roll <= cumulative) {
                pick = idx;
                break;
            }
        }
        return pick;
    }

    private int appendCombatRoomSpawners(@Nonnull List<SpawnerDefinition> spawners,
                                         @Nonnull CombatRoomPlan plan,
                                         int eliteCount,
                                         int normalCount,
                                         @Nonnull PlacementContext context,
                                         int nextId) {
        if (context.pools().normalPool().isEmpty() && context.pools().elitePool().isEmpty()) {
            LOGGER.atWarning().log("[DungeonGen] No normal or elite pool for combat room %d on floor %d — skipping",
                plan.room().getId(), context.floorLevel());
            return nextId;
        }

        int elitePerCluster = eliteCount / plan.clusterCenters().size();
        int eliteRemainder = eliteCount % plan.clusterCenters().size();
        int normalPerCluster = normalCount / plan.clusterCenters().size();
        int normalRemainder = normalCount % plan.clusterCenters().size();

        for (int i = 0; i < plan.clusterCenters().size(); i++) {
            int[] clusterCenter = plan.clusterCenters().get(i);
            int cx = clusterCenter[0];
            int cz = clusterCenter[1];
            int cy = plan.room().getY() + 1;
            int clusterElites = elitePerCluster + (i < eliteRemainder ? 1 : 0);
            int clusterNormals = normalPerCluster + (i < normalRemainder ? 1 : 0);

            nextId = appendVariantSpawner(
                spawners,
                plan.room(),
                plan.trigger(),
                cx,
                cy,
                cz,
                clusterElites,
                context.pools().elitePool(),
                SpawnerVariant.ELITE,
                context,
                nextId
            );
            nextId = appendVariantSpawner(
                spawners,
                plan.room(),
                plan.trigger(),
                cx,
                cy,
                cz,
                clusterNormals,
                context.pools().normalPool(),
                SpawnerVariant.NORMAL,
                context,
                nextId
            );

            if (clusterElites == 0 && clusterNormals == 0) {
                LOGGER.atWarning().log("[DungeonGen] Cluster %d in room %d has zero spawns — skipping", i, plan.room().getId());
            }
        }

        return nextId;
    }

    private void appendBossSpawners(@Nonnull List<SpawnerDefinition> spawners,
                                    @Nonnull DungeonGraph graph,
                                    @Nonnull PlacementContext context,
                                    int nextId) {
        for (Room room : graph.getRooms()) {
            if (room.getType() != RoomType.BOSS) {
                continue;
            }
            nextId = appendBossRoomSpawners(spawners, room, context, nextId);
        }
    }

    private int appendBossRoomSpawners(@Nonnull List<SpawnerDefinition> spawners,
                                       @Nonnull Room room,
                                       @Nonnull PlacementContext context,
                                       int nextId) {
        if (context.pools().bossPool().isEmpty()) {
            LOGGER.atWarning().log("[DungeonGen] Empty boss pool for room %d on floor %d — skipping",
                room.getId(), context.floorLevel());
            return nextId;
        }

        int totalCount = calculateSpawnCount(room, context.settings());
        double radius = Math.max(room.getWidth(), room.getDepth()) / 2.0;
        TriggerConfig trigger = TriggerConfig.proximity(radius);
        int bossY = room.getY() + 1;
        List<Vec3i> bossOffsets = computeSpawnOffsets(
            context.grid(),
            room.centerX(),
            bossY,
            room.centerZ(),
            1,
            room,
            context.settings()
        );

        if (bossOffsets.isEmpty()) {
            LOGGER.atWarning().log("[DungeonGen] No valid spawn offsets for boss room %d — skipping", room.getId());
            return nextId;
        }

        spawners.add(new SpawnerDefinition(
            nextId++,
            room.centerX(),
            bossY,
            room.centerZ(),
            room.getId(),
            SpawnerType.FIXED,
            trigger,
            context.pools().bossPool(),
            1,
            bossOffsets,
            SpawnerVariant.BOSS,
            context.floorLevel(),
            context.levelVariance()
        ));

        int minionCount = totalCount - 1;
        if (minionCount <= 0) {
            return nextId;
        }

        int eliteMinionCount = context.pools().elitePool().isEmpty()
            ? 0
            : Math.min(context.settings().getBossRoomEliteMax(), minionCount);
        int normalMinionCount = minionCount - eliteMinionCount;
        int minionX = room.getX() + room.getWidth() / 3;
        int minionZ = room.getZ() + room.getDepth() / 3;

        nextId = appendVariantSpawner(
            spawners,
            room,
            trigger,
            minionX,
            bossY,
            minionZ,
            eliteMinionCount,
            context.pools().elitePool(),
            SpawnerVariant.ELITE,
            context,
            nextId
        );
        return appendVariantSpawner(
            spawners,
            room,
            trigger,
            minionX,
            bossY,
            minionZ,
            normalMinionCount,
            context.pools().normalPool(),
            SpawnerVariant.NORMAL,
            context,
            nextId
        );
    }

    private int appendVariantSpawner(@Nonnull List<SpawnerDefinition> spawners,
                                     @Nonnull Room room,
                                     @Nonnull TriggerConfig trigger,
                                     int centerX,
                                     int centerY,
                                     int centerZ,
                                     int count,
                                     @Nonnull List<SpawnEntry> pool,
                                     @Nonnull SpawnerVariant variant,
                                     @Nonnull PlacementContext context,
                                     int nextId) {
        if (count <= 0 || pool.isEmpty()) {
            return nextId;
        }

        List<Vec3i> offsets = computeSpawnOffsets(
            context.grid(),
            centerX,
            centerY,
            centerZ,
            count,
            room,
            context.settings()
        );
        if (offsets.isEmpty()) {
            return nextId;
        }

        spawners.add(new SpawnerDefinition(
            nextId++,
            centerX,
            centerY,
            centerZ,
            room.getId(),
            SpawnerType.FIXED,
            trigger,
            pool,
            count,
            offsets,
            variant,
            context.floorLevel(),
            context.levelVariance()
        ));
        return nextId;
    }

    // ============================================
    // Elite ratio (sigmoid model)
    // ============================================

    /**
     * Compute the elite ratio for a given floor level using a normalized sigmoid curve.
     *
     * <p>Formula:
     * <ul>
     *   <li>{@code sigmoid(f) = 1 / (1 + exp(-steepness * (f - midpoint)))}</li>
     *   <li>{@code normalizedSigmoid(f) = (sigmoid(f) - sigmoid(1)) / (sigmoid(60) - sigmoid(1))}, clamped [0,1]</li>
     *   <li>{@code eliteRatio(f) = eliteRatioMin + (eliteRatioMax - eliteRatioMin) * normalizedSigmoid(f)}</li>
     * </ul>
     *
     * @param floorLevel the current dungeon floor level
     * @param settings   dungeon settings containing sigmoid parameters
     * @return the elite ratio for this floor level
     */
    private static double computeEliteRatio(int floorLevel,
                                             @Nonnull DungeonSettingsConfig settings) {
        double steepness = settings.getEliteRatioSteepness();
        double midpoint = settings.getEliteRatioMidpoint();
        double sigFloor = sigmoid(floorLevel, steepness, midpoint);
        double sigMin = sigmoid(1, steepness, midpoint);
        double sigMax = sigmoid(60, steepness, midpoint);
        double denom = sigMax - sigMin;

        double normalizedProgress = (denom == 0.0) ? 0.0 : Math.clamp((sigFloor - sigMin) / denom, 0.0, 1.0);
        return settings.getEliteRatioMin() + (settings.getEliteRatioMax() - settings.getEliteRatioMin()) * normalizedProgress;
    }

    private static double sigmoid(double f, double steepness, double midpoint) {
        return 1.0 / (1.0 + Math.exp(-steepness * (f - midpoint)));
    }

    // ============================================
    // Spawn count calculation
    // ============================================

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
    // Spawn pool filtering
    // ============================================

    /**
     * Filter the theme's flat spawn pool by floor eligibility and variant.
     *
     * @param pool       raw pool entries from the theme config
     * @param floorLevel the current dungeon floor level
     * @param variant    the required spawner variant
     * @return filtered and converted list of {@link SpawnEntry} instances
     */
    @Nonnull
    private List<SpawnEntry> filterPool(@Nonnull SpawnPoolEntry[] pool,
                                        int floorLevel,
                                        @Nonnull SpawnerVariant variant) {
        List<SpawnEntry> result = new ArrayList<>();
        for (SpawnPoolEntry entry : pool) {
            if (!entry.isEligibleForFloor(floorLevel)) continue;
            if (!entry.allowsVariant(variant)) continue;
            result.add(new SpawnEntry(entry.getNpcRole(), entry.getWeight()));
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
            centers.add(new int[]{cx, room.centerZ()});
        } else if (area <= MEDIUM_ROOM_AREA) {
            centers.add(new int[]{cx, room.getZ() + room.getDepth() / 3});
            centers.add(new int[]{cx, room.getZ() + 2 * room.getDepth() / 3});
        } else {
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
     * @param grid     the voxel grid for validation
     * @param centerX  spawner center X
     * @param centerY  spawner center Y (floor level)
     * @param centerZ  spawner center Z
     * @param count    desired number of spawn positions
     * @param room     the owning room (for interior bounds)
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

        if (offsets.isEmpty() && isValidSpawnPosition(grid, centerX, centerY, centerZ)) {
            offsets.add(Vec3i.ZERO);
        }

        return offsets;
    }

    /**
     * Check if a position is a valid spawn location: air at the position
     * and a solid (non-fluid) block directly below.
     */
    private boolean isValidSpawnPosition(@Nonnull BlockGrid grid, int x, int y, int z) {
        return grid.isAir(x, y, z) && grid.isBlock(x, y - 1, z);
    }
}
