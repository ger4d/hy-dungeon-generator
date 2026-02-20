package com.duntale.dungeongen.model;

/**
 * A single block to be placed in the world.
 *
 * <p>Uses a string block ID (e.g. {@code "Rock_Stone_Brick"}) rather than
 * a numeric runtime ID so that the generation pipeline remains independent
 * of the Hytale Server API. Resolution to integer IDs happens at assembly
 * time via {@code BlockResolver}.
 *
 * @param x        world-relative X
 * @param y        world-relative Y
 * @param z        world-relative Z
 * @param blockId  the string block type ID (e.g. "Rock_Stone_Brick")
 * @param rotation rotation index (0 = default)
 * @since 1.0.0
 */
public record BlockEntry(int x, int y, int z, String blockId, int rotation) {}
