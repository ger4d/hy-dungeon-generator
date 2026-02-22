package com.duntale.dungeongen.assembly;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.config.asset.DungeonSettingsConfig;
import com.duntale.dungeongen.model.BlockEntry;
import com.duntale.dungeongen.model.DungeonBlueprint;
import com.duntale.dungeongen.util.BlockResolver;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

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

    private final BlockResolver blockResolver;
    private final DungeonSettingsConfig settings;

    /**
     * Create a new world assembler.
     *
     * @param blockResolver the block resolver for string→int conversion
     */
    public WorldAssembler(@Nonnull BlockResolver blockResolver) {
        this.blockResolver = blockResolver;
        this.settings = DungeonSettingsConfig.getDefault();
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
                int end = Math.min(start + settings.getBlocksPerBatch(), blocks.size());

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
     * Fluid blocks (IDs starting with "Fluid_") are placed via the
     * {@link FluidSection} API instead of {@code setBlock} so they
     * behave as proper flowing fluids.
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

        String blockId = entry.blockId();

        // Fluid blocks use a separate layer
        if (blockId.startsWith("Fluid_")) {
            placeFluid(world, chunk, blockId, worldX, worldY, worldZ);
            return;
        }

        if (entry.rotation() != 0) {
            // Place with explicit rotation (yaw from rotation index)
            Rotation yaw = Rotation.values()[entry.rotation() % 4];
            chunk.placeBlock(worldX, worldY, worldZ, blockId,
                yaw, Rotation.None, Rotation.None, 0);
        } else {
            chunk.setBlock(worldX, worldY, worldZ, blockId);
        }
    }

    /**
     * Place a fluid block via the {@link FluidSection} API.
     *
     * <p>Hytale uses a two-fluid-type system:
     * <ul>
     *   <li><b>Source fluid</b> (e.g. {@code Water_Source}) — has {@code CanDemote: false},
     *       so it persists forever. Spreads the flowing variant automatically.</li>
     *   <li><b>Spread/flowing fluid</b> (e.g. {@code Water}) — has {@code CanDemote: true}
     *       and disappears if not adjacent to a source or higher-level neighbor.</li>
     * </ul>
     *
     * <p>We must place the <b>source</b> variant. Resolution order for e.g. {@code "Fluid_Water"}:
     * <ol>
     *   <li>{@code "Water_Source"} (the actual source fluid)</li>
     *   <li>{@code "Fluid_Water"} (literal lookup — unlikely to match)</li>
     *   <li>{@code "Water"} (spread fluid — last resort, will still demote)</li>
     * </ol>
     */
    private void placeFluid(@Nonnull World world,
                            @Nonnull WorldChunk chunk,
                            @Nonnull String blockId,
                            int x, int y, int z) {
        // Strip "Fluid_" prefix to get the base name (e.g. "Fluid_Water" → "Water")
        String baseName = blockId.startsWith("Fluid_")
            ? blockId.substring("Fluid_".length())
            : blockId;

        // Try source variant first (e.g. "Water_Source") — this is the non-demoting fluid
        Fluid fluid = Fluid.getAssetMap().getAsset(baseName + "_Source");
        if (fluid == null) {
            // Try the raw block ID (e.g. "Fluid_Water")
            fluid = Fluid.getAssetMap().getAsset(blockId);
        }
        if (fluid == null) {
            // Try the base name directly (e.g. "Water" — the spread fluid)
            fluid = Fluid.getAssetMap().getAsset(baseName);
        }
        if (fluid == null) {
            LOGGER.atWarning().log("[DungeonGen] Unknown fluid: %s — falling back to setBlock", blockId);
            chunk.setBlock(x, y, z, blockId);
            return;
        }

        try {
            // Ensure a solid block exists below the fluid so players don't fall through
            if (y > 0 && chunk.getBlock(x, y - 1, z) == 0) {
                chunk.setBlock(x, y - 1, z, settings.getFluidFallbackBlock());
            }

            byte level = (byte) fluid.getMaxFluidLevel();
            Store<ChunkStore> store = world.getChunkStore().getStore();
            ChunkColumn column = store.getComponent(chunk.getReference(), ChunkColumn.getComponentType());
            Ref<ChunkStore> section = column.getSection(ChunkUtil.chunkCoordinate(y));
            FluidSection fluidSection = store.ensureAndGetComponent(section, FluidSection.getComponentType());
            fluidSection.setFluid(x, y, z, fluid, level);

            chunk.setTicking(x, y, z, true);
            chunk.markNeedsSaving();

            LOGGER.atFine().log("[DungeonGen] Placed fluid %s (level=%d) at (%d,%d,%d)",
                fluid.getId(), level, x, y, z);
        } catch (Exception e) {
            LOGGER.atWarning().log("[DungeonGen] Failed to place fluid %s at (%d,%d,%d): %s",
                blockId, x, y, z, e.getMessage());
        }
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
