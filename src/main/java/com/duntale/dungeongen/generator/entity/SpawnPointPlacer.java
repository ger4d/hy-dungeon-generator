package com.duntale.dungeongen.generator.entity;

import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.config.asset.DungeonThemeConfig;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.Room;
import com.duntale.dungeongen.generator.layout.RoomType;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.SpawnPoint;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Determines where enemies spawn based on room types, critical-path
 * position, and the {@link PacingConfig} difficulty ramp.
 *
 * @since 1.0.0
 */
public class SpawnPointPlacer {

    private final Random random;
    private final PacingConfig pacingConfig;

    /**
     * Create a new spawn point placer.
     *
     * @param seed         RNG seed for deterministic placement
     * @param pacingConfig pacing configuration for difficulty scaling
     */
    public SpawnPointPlacer(long seed, @Nonnull PacingConfig pacingConfig) {
        this.random = new Random(seed);
        this.pacingConfig = pacingConfig;
    }

    /**
     * Generate spawn points for the dungeon.
     * Only {@link RoomType#COMBAT} and {@link RoomType#BOSS} rooms receive spawn points.
     * Tier scales with position along the critical path.
     *
     * @param grid        the voxel grid (used to validate walkable positions)
     * @param graph       the dungeon layout graph
     * @param paletteName the theme palette name (determines spawner table prefix)
     * @return list of spawn points placed in the dungeon
     */
    @Nonnull
    public List<SpawnPoint> placeSpawnPoints(@Nonnull BlockGrid grid,
                                             @Nonnull DungeonGraph graph,
                                             @Nonnull String paletteName) {
        List<SpawnPoint> spawnPoints = new ArrayList<>();
        List<Integer> criticalPath = graph.getCriticalPath();
        String spawnerPrefix = getSpawnerPrefix(paletteName);
        DungeonSettingsConfig settings = DungeonSettingsConfig.getDefault();

        for (Room room : graph.getRooms()) {
            if (room.getType() != RoomType.COMBAT && room.getType() != RoomType.BOSS) continue;

            int tier = calculateTier(room, criticalPath, settings);
            int spawnCount = calculateSpawnCount(room, settings);
            String spawnerTable = spawnerPrefix + "_Tier" + tier;

            int placed = 0;
            int inset = settings.getSpawnInteriorInset();
            int interiorMinX = room.getX() + inset;
            int interiorMaxX = room.getX() + room.getWidth() - inset - 1;
            int interiorMinZ = room.getZ() + inset;
            int interiorMaxZ = room.getZ() + room.getDepth() - inset - 1;
            int floorY = room.getY() + 1;

            for (int attempt = 0; attempt < spawnCount * settings.getSpawnAttemptMultiplier() && placed < spawnCount; attempt++) {
                int sx = interiorMinX + random.nextInt(Math.max(1, interiorMaxX - interiorMinX + 1));
                int sz = interiorMinZ + random.nextInt(Math.max(1, interiorMaxZ - interiorMinZ + 1));

                if (grid.isAir(sx, floorY, sz) && grid.isBlock(sx, floorY - 1, sz)) {
                    spawnPoints.add(new SpawnPoint(sx, floorY, sz, spawnerTable, tier));
                    placed++;
                }
            }
        }

        return spawnPoints;
    }

    private int calculateTier(Room room, List<Integer> criticalPath, DungeonSettingsConfig settings) {
        if (room.getType() == RoomType.BOSS) return settings.getBossTier();

        int pathIndex = criticalPath.indexOf(room.getId());
        if (pathIndex < 0) pathIndex = criticalPath.size() / 2;

        double progress = (double) pathIndex / Math.max(1, criticalPath.size() - 1);
        double adjustedProgress = progress * (settings.getDifficultyRampBase() + pacingConfig.difficultyRamp());

        if (adjustedProgress < settings.getTierThreshold1()) return 1;
        if (adjustedProgress < settings.getTierThreshold2()) return 2;
        return 3;
    }

    private int calculateSpawnCount(Room room, DungeonSettingsConfig settings) {
        int area = room.getWidth() * room.getDepth();
        if (room.getType() == RoomType.BOSS) {
            return settings.getBossSpawnBase() + area / settings.getBossSpawnAreaDivisor();
        }
        return settings.getCombatSpawnBase() + area / settings.getCombatSpawnAreaDivisor();
    }

    /**
     * Map palette themes to spawner table prefixes from asset config.
     *
     * @param paletteName the palette name
     * @return the spawner table prefix for that theme
     */
    @Nonnull
    private String getSpawnerPrefix(@Nonnull String paletteName) {
        DungeonThemeConfig config = DungeonThemeConfig.get(paletteName);
        if (config != null) {
            return config.getSpawnerPrefix();
        }
        // Fallback if assets not loaded
        return "Zone1_Undead";
    }
}
