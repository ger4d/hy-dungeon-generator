package com.duntale.dungeongen.model;

/**
 * Blueprint-level definition of a merchant spawn point.
 *
 * @param x          X position relative to dungeon origin
 * @param y          Y position relative to dungeon origin
 * @param z          Z position relative to dungeon origin
 * @param floorLevel the dungeon floor level (for pricing/catalog)
 * @since 1.3.0
 */
public record MerchantDefinition(int x, int y, int z, int floorLevel) {}
