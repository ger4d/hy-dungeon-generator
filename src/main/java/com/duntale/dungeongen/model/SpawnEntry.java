package com.duntale.dungeongen.model;

import javax.annotation.Nonnull;

/**
 * A weighted entry in a spawner's NPC pool. NPC levels are determined
 * at runtime from the spawner's floor level and level variance, not
 * from per-entry ranges.
 *
 * <p>The theme JSON's {@code MinFloor}/{@code MaxFloor} fields control
 * <strong>floor eligibility</strong> (which floors this NPC can appear on),
 * not the NPC's actual combat level.</p>
 *
 * @param npcRole Hytale NPC role name (e.g. "Skeleton_Soldier")
 * @param weight  relative weight for weighted random selection
 * @since 1.1.0
 */
public record SpawnEntry(
    @Nonnull String npcRole,
    double weight
) {}
