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
    private final List<SpawnerDefinition> spawners;
    private final List<MerchantDefinition> merchants;
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
        this.spawners = new ArrayList<>();
        this.merchants = new ArrayList<>();
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
     * Register a spawner definition in the blueprint.
     *
     * @param spawner the spawner definition to add
     * @since 1.1.0
     */
    public void addSpawner(@Nonnull SpawnerDefinition spawner) {
        spawners.add(spawner);
    }

    /**
     * Register a merchant definition in the blueprint.
     *
     * @param merchant the merchant definition to add
     * @since 1.3.0
     */
    public void addMerchant(@Nonnull MerchantDefinition merchant) {
        merchants.add(merchant);
    }

    /**
     * @return unmodifiable list of all block entries.
     */
    @Nonnull
    public List<BlockEntry> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    /**
     * @return unmodifiable list of all spawner definitions.
     * @since 1.1.0
     */
    @Nonnull
    public List<SpawnerDefinition> getSpawners() {
        return Collections.unmodifiableList(spawners);
    }

    /**
     * @return unmodifiable list of all merchant definitions.
     * @since 1.3.0
     */
    @Nonnull
    public List<MerchantDefinition> getMerchants() {
        return Collections.unmodifiableList(merchants);
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
