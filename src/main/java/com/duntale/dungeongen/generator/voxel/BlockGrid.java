package com.duntale.dungeongen.generator.voxel;

import com.duntale.dungeongen.model.BlockEntry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * A 3D array used to build dungeon voxel data before converting to a
 * flat list of {@link BlockEntry} records. Cells hold a string block ID
 * ({@code null} means air).
 *
 * @since 1.0.0
 */
public class BlockGrid {

    private final int width;
    private final int height;
    private final int depth;
    private final String[][][] blocks;
    private final int[][][] rotations;
    private int blockCount;

    /**
     * Create a new grid filled with air (all cells {@code null}).
     *
     * @param width  X dimension
     * @param height Y dimension
     * @param depth  Z dimension
     */
    public BlockGrid(int width, int height, int depth) {
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.blocks = new String[width][height][depth];
        this.rotations = new int[width][height][depth];
        this.blockCount = 0;
    }

    /**
     * Check whether a block can be placed at the given position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return {@code true} if the position is within bounds
     */
    public boolean canPlace(int x, int y, int z) {
        return x >= 0 && x < width && y >= 0 && y < height && z >= 0 && z < depth;
    }

    /**
     * Set a block at the given position with rotation.
     * Out-of-bounds writes are silently ignored.
     *
     * @param x        X coordinate
     * @param y        Y coordinate
     * @param z        Z coordinate
     * @param blockId  block type ID, or {@code null} for air
     * @param rotation rotation index
     */
    public void set(int x, int y, int z, @Nullable String blockId, int rotation) {
        if (!canPlace(x, y, z)) return;
        if (blocks[x][y][z] == null && blockId != null) blockCount++;
        if (blocks[x][y][z] != null && blockId == null) blockCount--;
        blocks[x][y][z] = blockId;
        rotations[x][y][z] = rotation;
    }

    /**
     * Set a block at the given position with default rotation (0).
     *
     * @param x       X coordinate
     * @param y       Y coordinate
     * @param z       Z coordinate
     * @param blockId block type ID, or {@code null} for air
     */
    public void set(int x, int y, int z, @Nullable String blockId) {
        set(x, y, z, blockId, 0);
    }

    /**
     * Get the block ID at the given position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return the block ID, or {@code null} if air or out of bounds
     */
    @Nullable
    public String get(int x, int y, int z) {
        if (!canPlace(x, y, z)) return null;
        return blocks[x][y][z];
    }

    /**
     * Get the rotation index at the given position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return the rotation index, or 0 if out of bounds
     */
    public int getRotation(int x, int y, int z) {
        if (!canPlace(x, y, z)) return 0;
        return rotations[x][y][z];
    }

    /**
     * Check whether a position is air (null block ID or out of bounds).
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return {@code true} if the position is air
     */
    public boolean isAir(int x, int y, int z) {
        return get(x, y, z) == null;
    }

    /**
     * Check whether a position holds a solid (non-null) block.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return {@code true} if the position is solid
     */
    public boolean isSolid(int x, int y, int z) {
        return get(x, y, z) != null;
    }

    /**
     * Get the {@link BlockCategory} for the cell at the given position.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return the category; {@link BlockCategory#AIR} for empty/OOB cells
     */
    @Nonnull
    public BlockCategory getCategory(int x, int y, int z) {
        String id = get(x, y, z);
        return id == null ? BlockCategory.AIR : BlockCategory.of(id);
    }

    /**
     * Check whether a position holds a structural block — one that
     * provides physical support for wall-mounted or floor-placed assets.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return {@code true} if the cell is a structural block
     */
    public boolean isBlock(int x, int y, int z) {
        return getCategory(x, y, z) == BlockCategory.BLOCK;
    }

    /**
     * Check whether a position holds a fluid block.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return {@code true} if the position is a fluid block
     */
    public boolean isFluid(int x, int y, int z) {
        return getCategory(x, y, z) == BlockCategory.FLUID;
    }

    /**
     * Check whether a solid block at this position is exposed — i.e. at
     * least one face is adjacent to air or the grid edge.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return {@code true} if the block is solid and exposed
     */
    public boolean isExposed(int x, int y, int z) {
        return isSolid(x, y, z) && (
            isAir(x - 1, y, z) || isAir(x + 1, y, z) ||
            isAir(x, y - 1, z) || isAir(x, y + 1, z) ||
            isAir(x, y, z - 1) || isAir(x, y, z + 1)
        );
    }

    /**
     * Convert the grid to a flat list of {@link BlockEntry} records,
     * skipping air cells.
     *
     * @return list of block entries
     */
    @Nonnull
    public List<BlockEntry> toBlockEntries() {
        List<BlockEntry> entries = new ArrayList<>(blockCount);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    if (blocks[x][y][z] != null) {
                        entries.add(new BlockEntry(x, y, z, blocks[x][y][z], rotations[x][y][z]));
                    }
                }
            }
        }
        return entries;
    }

    /** @return X dimension. */
    public int getWidth() { return width; }

    /** @return Y dimension. */
    public int getHeight() { return height; }

    /** @return Z dimension. */
    public int getDepth() { return depth; }

    /** @return total number of non-air blocks. */
    public int getBlockCount() { return blockCount; }
}
