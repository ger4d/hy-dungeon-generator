package com.duntale.dungeongen.model;

/**
 * How a spawner activates.
 *
 * @since 1.1.0
 */
public enum TriggerType {
    /** Activates immediately on creation. */
    ON_CREATE,
    /** Activates when a player enters the activation radius. */
    PROXIMITY,
    /** Activates after a timed delay (future). */
    TIMED,
    /** Activates when a player enters the room AABB (future). */
    ON_ROOM_ENTER,
    /** Activates when all room enemies are dead (future). */
    ON_ROOM_CLEAR
}
