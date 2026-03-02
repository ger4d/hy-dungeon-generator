package com.duntale.dungeongen.model;

import com.duntale.dungeongen.config.Vec3i;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Complete definition of an enemy spawner, produced at generation time
 * and consumed at runtime by the ECS spawner system.
 *
 * @param id            unique spawner ID within the blueprint
 * @param x             world-relative X position
 * @param y             world-relative Y position (floor level)
 * @param z             world-relative Z position
 * @param roomId        owning room ID in the DungeonGraph
 * @param type          spawner lifecycle type
 * @param trigger       how the spawner activates
 * @param spawnPool     weighted pool of NPC roles to pick from
 * @param totalCount    total NPCs to spawn (for FIXED)
 * @param spawnOffsets  pre-validated spawn positions relative to spawner center
 * @param isBoss        true for boss spawners
 * @since 1.1.0
 */
public record SpawnerDefinition(
    int id,
    int x, int y, int z,
    int roomId,
    @Nonnull SpawnerType type,
    @Nonnull TriggerConfig trigger,
    @Nonnull List<SpawnEntry> spawnPool,
    int totalCount,
    @Nonnull List<Vec3i> spawnOffsets,
    boolean isBoss
) {}
