package com.duntale.dungeongen.model;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A weighted entry in a spawner's NPC pool. Has min/max level range
 * and optional floor-level gates for progressive difficulty.
 *
 * @param npcRole       Hytale NPC role name (e.g. "Skeleton_Soldier")
 * @param minLevel      minimum NPC level (inclusive)
 * @param maxLevel      maximum NPC level (inclusive)
 * @param weight        relative weight for weighted random selection
 * @param minFloorLevel minimum dungeon floor for eligibility (null = no minimum)
 * @param maxFloorLevel maximum dungeon floor for eligibility (null = no maximum)
 * @since 1.1.0
 */
public record SpawnEntry(
    @Nonnull String npcRole,
    int minLevel,
    int maxLevel,
    double weight,
    @Nullable Integer minFloorLevel,
    @Nullable Integer maxFloorLevel
) {
    /**
     * Convenience constructor with no floor restriction.
     *
     * @param npcRole  Hytale NPC role name
     * @param minLevel minimum NPC level (inclusive)
     * @param maxLevel maximum NPC level (inclusive)
     * @param weight   relative weight for weighted random selection
     */
    public SpawnEntry(@Nonnull String npcRole, int minLevel, int maxLevel, double weight) {
        this(npcRole, minLevel, maxLevel, weight, null, null);
    }

    /**
     * Check if this entry is eligible for the given floor level.
     *
     * @param floorLevel the current dungeon floor
     * @return true if this entry can appear on the given floor
     */
    public boolean isEligibleForFloor(int floorLevel) {
        if (minFloorLevel != null && floorLevel < minFloorLevel) return false;
        return maxFloorLevel == null || floorLevel <= maxFloorLevel;
    }
}
