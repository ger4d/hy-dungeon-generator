package com.duntale.dungeongen.model;

import com.duntale.dungeongen.generator.layout.DungeonGraph;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The final output of the dungeon generation pipeline. Contains every
 * block placement and spawn point needed for world assembly, together
 * with the layout graph that produced them.
 *
 * @since 1.0.0
 */
public class DungeonBlueprint {

    private final List<BlockEntry> blocks;
    private final List<SpawnPoint> spawnPoints;
    private final DungeonGraph graph;
    private final String seed;

    /**
     * Create a new empty blueprint.
     *
     * @param seed  the generation seed string
     * @param graph the layout graph that produced this blueprint
     */
    public DungeonBlueprint(@Nonnull String seed, @Nonnull DungeonGraph graph) {
        this.seed = seed;
        this.graph = graph;
        this.blocks = new ArrayList<>();
        this.spawnPoints = new ArrayList<>();
    }

    /**
     * Add a pre-built block entry.
     *
     * @param entry the block entry to add
     */
    public void addBlock(@Nonnull BlockEntry entry) {
        blocks.add(entry);
    }

    /**
     * Add a block entry from raw components.
     *
     * @param x        world-relative X
     * @param y        world-relative Y
     * @param z        world-relative Z
     * @param blockId  the string block type ID
     * @param rotation rotation index (0 = default)
     */
    public void addBlock(int x, int y, int z, @Nonnull String blockId, int rotation) {
        blocks.add(new BlockEntry(x, y, z, blockId, rotation));
    }

    /**
     * Register a spawn point in the blueprint.
     *
     * @param sp the spawn point to add
     */
    public void addSpawnPoint(@Nonnull SpawnPoint sp) {
        spawnPoints.add(sp);
    }

    /**
     * @return unmodifiable list of all block entries.
     */
    @Nonnull
    public List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /**
     * @return unmodifiable list of all spawn points.
     */
    @Nonnull
    public List<SpawnPoint> getSpawnPoints() {
        return Collections.unmodifiableList(spawnPoints);
    }

    /**
     * @return the layout graph backing this blueprint.
     */
    @Nonnull
    public DungeonGraph getGraph() {
        return graph;
    }

    /**
     * @return the generation seed string.
     */
    @Nonnull
    public String getSeed() {
        return seed;
    }

    /**
     * @return total number of block entries in this blueprint.
     */
    public int getTotalBlocks() {
        return blocks.size();
    }
}
