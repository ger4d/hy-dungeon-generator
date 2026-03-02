package com.duntale.dungeongen.model;

/**
 * Classification of spawner lifecycle behavior.
 *
 * @since 1.1.0
 */
public enum SpawnerType {
    /** Spawns a finite total, then permanently deactivates. */
    FIXED,
    /** Keeps spawning until manually disabled (future). */
    RECURRENT
}
