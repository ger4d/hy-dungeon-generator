package com.duntale.dungeongen.generator;

import com.duntale.dungeongen.assembly.WorldAssembler;
import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.generator.entity.SpawnPointPlacer;
import com.duntale.dungeongen.generator.feature.FeaturePlacer;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.layout.LayoutGenerator;
import com.duntale.dungeongen.generator.lighting.LightPlacer;
import com.duntale.dungeongen.generator.props.PropPlacer;
import com.duntale.dungeongen.generator.theme.ThemeDecorator;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.generator.voxel.VoxelCarver;
import com.duntale.dungeongen.model.DungeonBlueprint;
import com.duntale.dungeongen.model.SpawnPoint;
import com.duntale.dungeongen.util.BlockResolver;
import com.hypixel.hytale.logger.HytaleLogger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the full dungeon generation pipeline on a dedicated thread pool.
 *
 * <p><b>Pipeline stages:</b></p>
 * <ol>
 *   <li><b>Layout</b> — BSP room/corridor graph ({@link LayoutGenerator})</li>
 *   <li><b>Voxel Carving</b> — fill-then-carve into {@link BlockGrid} ({@link VoxelCarver})</li>
 *   <li><b>Theme Decoration</b> — materials, pillars, decay ({@link ThemeDecorator})</li>
 *   <li><b>Props</b> — furniture, chests, cobwebs ({@link PropPlacer})</li>
 *   <li><b>Lights</b> — torches, lanterns, braziers ({@link LightPlacer})</li>
 *   <li><b>Spawns</b> — enemy spawn points ({@link SpawnPointPlacer})</li>
 *   <li><b>Assembly</b> — place into world (optional, {@link WorldAssembler})</li>
 * </ol>
 *
 * <p>All generation work runs off the server's world thread. Only the assembly
 * step dispatches back to the world thread via {@code World.execute()}.</p>
 *
 * @since 1.0.0
 */
public class GenerationOrchestrator {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final ExecutorService executor;
    private final WorldAssembler worldAssembler;

    /**
     * Create an orchestrator with a 2-thread worker pool and a world assembler
     * backed by the given block resolver.
     *
     * @param blockResolver the resolver for string block IDs (may be {@code null}
     *                      if assembly is never requested)
     */
    public GenerationOrchestrator(@Nullable BlockResolver blockResolver) {
        this.executor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "DungeonGen-Worker");
            t.setDaemon(true);
            return t;
        });
        this.worldAssembler = blockResolver != null ? new WorldAssembler(blockResolver) : null;
    }

    /**
     * Asynchronously generate a dungeon from the given configuration.
     *
     * <p>If {@code config.assemble()} is {@code true}, the generated blueprint
     * will be placed into the target world before the future completes.</p>
     *
     * @param config the dungeon generation config
     * @return a future that completes with generation statistics
     */
    @Nonnull
    public CompletableFuture<GenerationResult> generate(@Nonnull DungeonConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            long seed = config.seed() != null ? config.seed().hashCode() : System.nanoTime();
            String seedStr = config.seed() != null ? config.seed() : String.valueOf(seed);

            // Apply complexity scaling
            LayoutConfig layout = config.layout();
            double c = Math.max(0.0, Math.min(1.0, layout.complexity()));
            double cScale = 0.3 + c * 1.4; // range [0.3, 1.7]
            layout = new LayoutConfig(
                layout.width(), layout.depth(), layout.height(),
                layout.roomDensity(),
                layout.minRoomSize(), layout.maxRoomSize(),
                Math.max(3, (int) (layout.maxRooms() * cScale)),
                layout.roomShape(), layout.irregularity(),
                layout.corridorWidth(),
                Math.max(0.0, Math.min(1.0, layout.branchChance() * cScale)),
                Math.max(0.0, Math.min(1.0, layout.loopChance() * cScale)),
                layout.windingCorridors(), layout.windingFactor(),
                layout.pillarFrequency(), layout.waterFrequency(), layout.lavaFrequency(),
                Math.max(0.0, Math.min(0.5, layout.trapDensity() * cScale)),
                layout.floorTraps(),
                Math.max(0.0, Math.min(0.3, layout.secretWallChance() * cScale)),
                layout.entrancePlacement(), layout.exitDistance(),
                Math.max(0.0, Math.min(1.0, layout.enemyDensity() * cScale)),
                layout.maxEnemiesPerRoom(), layout.bossRoom(), layout.ambushChance(),
                layout.erosion(),
                layout.removeCeiling(), layout.flatFloor(), layout.solidFill(),
                layout.complexity()
            );

            LOGGER.atInfo().log("[DungeonGen] Starting generation with seed=%d, layout=%dx%d, maxRooms=%d, density=%.2f",
                seed, layout.width(), layout.depth(),
                layout.maxRooms(), layout.roomDensity());

            // Phase 1: Layout generation
            LayoutGenerator layoutGen = new LayoutGenerator(seed, layout);
            DungeonGraph graph = layoutGen.generate();

            LOGGER.atInfo().log("[DungeonGen] Layout: %d rooms, %d corridors, connected=%b",
                graph.getRooms().size(), graph.getCorridors().size(), graph.isFullyConnected());

            // Phase 2: Voxel carving
            String palette = config.theme().palette();
            VoxelCarver carver = new VoxelCarver(seed,
                layout.width(), layout.height(), layout.depth());
            BlockGrid grid = carver.carve(graph, "Rock_Stone_Brick", layout.removeCeiling(), layout.solidFill());

            LOGGER.atInfo().log("[DungeonGen] Voxel carve complete: %d blocks", grid.getBlockCount());

            // Phase 2b: Feature placement (pillars, water, lava, traps, secret walls)
            FeaturePlacer featurePlacer = new FeaturePlacer(seed + 5);
            featurePlacer.placeFeatures(grid, graph, layout);

            // Phase 2c: Erosion
            // carver.applyErosion(grid, layout.erosion());

            LOGGER.atInfo().log("[DungeonGen] Features + erosion applied");

            // Phase 3: Theme decoration
            ThemeDecorator decorator = new ThemeDecorator(seed + 1, config.theme());
            decorator.applyTheme(grid, graph, layout.removeCeiling());

            LOGGER.atInfo().log("[DungeonGen] Theme applied: %s (decay=%.2f)",
                palette, config.theme().decayFactor());

            // Phase 4: Prop placement
            PropPlacer propPlacer = new PropPlacer(seed + 2);
            propPlacer.placeProps(grid, graph, palette, layout.removeCeiling());

            // Phase 5: Light placement
            LightPlacer lightPlacer = new LightPlacer(seed + 3);
            lightPlacer.placeLights(grid, graph, palette, layout.removeCeiling());

            // Phase 6: Spawn point placement
            SpawnPointPlacer spawnPlacer = new SpawnPointPlacer(seed + 4, config.pacing());
            List<SpawnPoint> spawnPoints = spawnPlacer.placeSpawnPoints(grid, graph, palette);

            LOGGER.atInfo().log("[DungeonGen] Props/lights/spawns placed: %d spawn points",
                spawnPoints.size());

            // Build the blueprint
            DungeonBlueprint blueprint = new DungeonBlueprint(seedStr, graph);
            grid.toBlockEntries().forEach(blueprint::addBlock);
            spawnPoints.forEach(blueprint::addSpawnPoint);

            long genElapsed = System.currentTimeMillis() - start;

            LOGGER.atInfo().log("[DungeonGen] Generation complete: %d blocks in %d ms",
                blueprint.getTotalBlocks(), genElapsed);

            // Phase 7: World assembly (optional)
            long assemblyMs = 0;
            String assemblyError = null;
            if (config.assemble()) {
                if (worldAssembler == null) {
                    assemblyError = "Assembly requested but no BlockResolver available";
                    LOGGER.atWarning().log("[DungeonGen] %s", assemblyError);
                } else {
                    try {
                        assemblyMs = worldAssembler.assemble(blueprint, config.worldName(), config.origin())
                            .join(); // block until assembly completes on world thread
                    } catch (Exception e) {
                        assemblyError = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        LOGGER.atSevere().log("[DungeonGen] Assembly failed: %s", assemblyError);
                    }
                }
            }

            return new GenerationResult(
                seedStr,
                graph.getRooms().size(),
                graph.getCorridors().size(),
                blueprint.getTotalBlocks(),
                genElapsed,
                assemblyMs,
                assemblyError
            );
        }, executor);
    }

    /**
     * Shut down the worker thread pool. Pending generations will attempt
     * to complete, but no new work will be accepted.
     */
    public void shutdown() {
        executor.shutdown();
        LOGGER.atInfo().log("[DungeonGen] Generation orchestrator shut down");
    }
}
