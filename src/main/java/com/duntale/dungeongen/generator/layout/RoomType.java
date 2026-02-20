package com.duntale.dungeongen.generator.layout;

/**
 * Classification of dungeon rooms controlling gameplay pacing and
 * decoration choices.
 *
 * @since 1.0.0
 */
public enum RoomType {
    /** Starting room where players enter the dungeon. */
    ENTRANCE,
    /** Standard fight encounter room. */
    COMBAT,
    /** Safe area with no spawns — allows players to breathe. */
    SAFE,
    /** Contains loot chests or reward items. */
    LOOT,
    /** Final boss encounter room — largest, most decorated. */
    BOSS,
    /** Central hub connecting multiple paths. */
    HUB,
    /** Terminal room with an optional secret. */
    DEAD_END,
    /** Small transitional room at corridor junctions. */
    CORRIDOR_JUNCTION
}
