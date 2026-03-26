package com.duntale.dungeongen.model;

/**
 * Mutually exclusive spawner variant. Determines how spawned NPCs are scaled
 * at runtime via {@code CombatScaling.NpcVariant}.
 *
 * @since 1.4.0
 */
public enum SpawnerVariant {
    NORMAL,
    ELITE,
    BOSS
}
