package com.duntale.dungeongen.assembly;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.model.BlockEntry;
import com.duntale.dungeongen.model.DungeonBlueprint;
import com.duntale.dungeongen.util.BlockResolver;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Places a {@link DungeonBlueprint} into the Hytale world by setting blocks
 * on the world thread in batches to avoid freezing the server.
 *
 * <p>All {@code setBlock} calls are dispatched to the target {@link World}'s
 * executor (world thread) via {@link World#execute(Runnable)}. Blocks are
 * placed in configurable batches per tick to stay within a reasonable tick
 * budget.</p>
 *
 * @since 1.0.0
 */
public class WorldAssembler {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    /** Maximum blocks placed per world-thread dispatch. */
    private static final int BLOCKS_PER_BATCH = 1000;

    private final BlockResolver blockResolver;

    /**
     * Create a new world assembler.
     *
     * @param blockResolver the block resolver for string→int conversion
     */
    public WorldAssembler(@Nonnull BlockResolver blockResolver) {
        this.blockResolver = blockResolver;
    }

    /**
     * Assemble the blueprint into the specified world at the given origin.
     *
     * <p>The returned future completes with the wall-clock assembly time in
     * milliseconds once every block has been placed.</p>
     *
     * @param blueprint the dungeon blueprint to place
     * @param worldName the target world name
     * @param origin    the world-space origin offset
     * @return future completing with assembly time in milliseconds
     */
    @Nonnull
    public CompletableFuture<Long> assemble(@Nonnull DungeonBlueprint blueprint,
                                            @Nonnull String worldName,
                                            @Nonnull Vec3i origin) {
        CompletableFuture<Long> future = new CompletableFuture<>();
        long startTime = System.currentTimeMillis();

        World world = resolveWorld(worldName);
        if (world == null) {
            future.completeExceptionally(
                new IllegalStateException("World not found: " + worldName));
            return future;
        }

        List<BlockEntry> blocks = blueprint.getBlocks();
        if (blocks.isEmpty()) {
            future.complete(0L);
            return future;
        }

        LOGGER.atInfo().log("[DungeonGen] Assembling %d blocks in world '%s' at (%d, %d, %d)",
            blocks.size(), worldName, origin.x(), origin.y(), origin.z());

        AtomicInteger index = new AtomicInteger(0);

        // Schedule the first batch on the world thread
        scheduleBatch(world, blocks, origin, index, startTime, future);

        return future;
    }

    /**
     * Schedule a batch of block placements on the world thread.
     * After each batch, if more blocks remain, another batch is scheduled.
     */
    private void scheduleBatch(@Nonnull World world,
                               @Nonnull List<BlockEntry> blocks,
                               @Nonnull Vec3i origin,
                               @Nonnull AtomicInteger index,
                               long startTime,
                               @Nonnull CompletableFuture<Long> future) {
        world.execute(() -> {
            try {
                int start = index.get();
                int end = Math.min(start + BLOCKS_PER_BATCH, blocks.size());

                for (int i = start; i < end; i++) {
                    BlockEntry entry = blocks.get(i);
                    placeBlock(world, entry, origin);
                }

                index.set(end);

                if (end < blocks.size()) {
                    // More blocks remain — schedule the next batch
                    scheduleBatch(world, blocks, origin, index, startTime, future);
                } else {
                    // All blocks placed
                    long elapsed = System.currentTimeMillis() - startTime;
                    LOGGER.atInfo().log("[DungeonGen] Assembly complete: %d blocks in %d ms",
                        blocks.size(), elapsed);
                    future.complete(elapsed);
                }
            } catch (Exception e) {
                LOGGER.atSevere().log("[DungeonGen] Assembly failed at block %d: %s",
                    index.get(), e.getMessage());
                future.completeExceptionally(e);
            }
        });
    }

    /**
     * Place a single block into the world at the origin-adjusted position.
     */
    private void placeBlock(@Nonnull World world,
                            @Nonnull BlockEntry entry,
                            @Nonnull Vec3i origin) {
        int worldX = origin.x() + entry.x();
        int worldY = origin.y() + entry.y();
        int worldZ = origin.z() + entry.z();

        long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
        WorldChunk chunk = world.getChunk(chunkIndex);
        if (chunk == null) {
            // Chunk not loaded — skip this block silently
            return;
        }

        // Use the string-based setBlock which resolves via the asset map internally
        chunk.setBlock(worldX, worldY, worldZ, entry.blockId());
    }

    /**
     * Resolve a world by name.
     *
     * @param worldName the world name, or "default" for the default world
     * @return the world, or {@code null} if not found
     */
    @Nullable
    private World resolveWorld(@Nonnull String worldName) {
        if ("default".equalsIgnoreCase(worldName)) {
            return Universe.get().getDefaultWorld();
        }
        return Universe.get().getWorld(worldName);
    }
}
