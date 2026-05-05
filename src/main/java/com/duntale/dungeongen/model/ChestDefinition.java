package com.duntale.dungeongen.model;

import javax.annotation.Nonnull;

/**
 * Blueprint-level definition of a chest or crate that should receive rolled loot at runtime.
 *
 * @param x       X position relative to dungeon origin
 * @param y       Y position relative to dungeon origin
 * @param z       Z position relative to dungeon origin
 * @param tier    reward tier that controls which loot table is rolled
 * @param blockId the placed chest block ID for diagnostics
 * @since 1.4.0
 */
public record ChestDefinition(int x, int y, int z, @Nonnull ChestTier tier, @Nonnull String blockId) {}