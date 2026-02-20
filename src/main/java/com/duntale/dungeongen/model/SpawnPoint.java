package com.duntale.dungeongen.model;

/**
 * A planned entity spawn location in the dungeon.
 *
 * @param x            world-relative X
 * @param y            world-relative Y (floor level)
 * @param z            world-relative Z
 * @param spawnerTable spawner table ID (e.g. "Zone1_Undead_Tier1")
 * @param tier         difficulty tier 1–3
 * @since 1.0.0
 */
public record SpawnPoint(int x, int y, int z, String spawnerTable, int tier) {}
