package com.duntale.dungeongen.command;

import com.duntale.dungeongen.DungeonGenPlugin;
import com.duntale.dungeongen.assembly.DungeonAssemblyService;
import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.GenerationResult;
import com.duntale.dungeongen.preview.DungeonPreviewPacketFactory;
import com.duntale.dungeongen.util.JsonParser;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Interactive UI page for configuring and triggering dungeon generation.
 *
 * <p>Two-pane layout: a scrollable parameter form on the left, and a live
 * {@link com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview}
 * preview on the right that refreshes (debounced) as the player changes form
 * values. Saved values are persisted in {@code generate-config.json} within the
 * plugin's data directory.</p>
 *
 * <p>Clicking "Generate Dungeon" always assembles the dungeon directly into
 * the player's current world. The {@code Center X/Y/Z} coordinates are
 * treated as the centre of the {@code width × height × depth} volume; that
 * volume is cleared before block placement. The standalone flow does not
 * create spawner ECS entities, merchant NPCs, or fill chest loot.</p>
 *
 * @since 1.0.3
 */
public class DungeonGeneratePage extends InteractiveCustomUIPage<DungeonGeneratePage.GenerateEventData> {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String CONFIG_FILENAME = "generate-config.json";
    private static final long PREVIEW_DEBOUNCE_MS = 750L;
    private static final String DEFAULT_SEED = "0";

    private final World world;
    private final DungeonPreviewPacketFactory previewPacketFactory = new DungeonPreviewPacketFactory();
    private final AtomicLong previewRequestSequence = new AtomicLong();
    private long lastAppliedPreviewRequestSequence = 0L;

    /**
     * Create a new dungeon generation page.
     *
     * @param playerRef the player opening the page
     * @param world     the world the player is in (used for dispatching results back)
     */
    public DungeonGeneratePage(@Nonnull PlayerRef playerRef, @Nonnull World world) {
        super(playerRef, CustomPageLifetime.CanDismiss, GenerateEventData.CODEC);
        this.world = world;
    }

    // ============================================
    // Build
    // ============================================

    @Override
    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder events, @Nonnull Store<EntityStore> store) {
        cmd.append("Pages/Generate/DungeonGeneratePage.ui");

        Map<String, Object> saved = loadSavedConfig();
        populateForm(cmd, ref, store, saved);

        events.addEventBinding(CustomUIEventBindingType.Activating, "#GenerateButton",
                buildFieldBinding("Generate"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#SeedRollButton",
                buildFieldBinding("Roll"));
        addPreviewEventBindings(events);

        GenerateEventData initial = buildInitialEventData(ref, store, saved);
        schedulePreview(initial, false);
    }

    private void populateForm(@Nonnull UICommandBuilder cmd,
                              @Nonnull Ref<EntityStore> ref,
                              @Nonnull Store<EntityStore> store,
                              @Nonnull Map<String, Object> saved) {
        DungeonConfig dd = DungeonConfig.withDefaults();

        if (saved.containsKey("originX")) {
            cmd.set("#OriginX.Value", JsonParser.toInt(saved.get("originX")));
            cmd.set("#OriginY.Value", JsonParser.toInt(saved.get("originY")));
            cmd.set("#OriginZ.Value", JsonParser.toInt(saved.get("originZ")));
        } else {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            Vector3d pos = transform.getPosition();
            cmd.set("#OriginX.Value", (int) pos.x);
            cmd.set("#OriginY.Value", (int) pos.y);
            cmd.set("#OriginZ.Value", (int) pos.z);
        }

        String savedSeed = JsonParser.toStringOrNull(saved.get("seed"));
        cmd.set("#Seed.Value", savedSeed != null && !savedSeed.isBlank() ? savedSeed : DEFAULT_SEED);

        cmd.set("#FloorLevel.Value", savedInt(saved, "floorLevel", dd.floorLevel()));
        cmd.set("#Width.Value", savedInt(saved, "width", dd.layout().width()));
        cmd.set("#Depth.Value", savedInt(saved, "depth", dd.layout().depth()));
        cmd.set("#Height.Value", savedInt(saved, "height", dd.layout().height()));
        cmd.set("#MaxRooms.Value", savedInt(saved, "maxRooms", dd.layout().maxRooms()));
        cmd.set("#MinRoomSize.Value", savedInt(saved, "minRoomSize", dd.layout().minRoomSize()));
        cmd.set("#MaxRoomSize.Value", savedInt(saved, "maxRoomSize", dd.layout().maxRoomSize()));
        cmd.set("#CorridorWidth.Value", savedInt(saved, "corridorWidth", dd.layout().corridorWidth()));
        cmd.set("#MaxEnemiesPerRoom.Value", savedInt(saved, "maxEnemiesPerRoom", dd.layout().maxEnemiesPerRoom()));

        cmd.set("#RoomDensity.Value", savedFloat(saved, "roomDensity", (float) dd.layout().roomDensity()));
        cmd.set("#Complexity.Value", savedFloat(saved, "complexity", (float) dd.layout().complexity()));
        cmd.set("#RoomShape.Value", savedStr(saved, "roomShape", dd.layout().roomShape()));
        cmd.set("#Irregularity.Value", savedFloat(saved, "irregularity", (float) dd.layout().irregularity()));
        cmd.set("#BranchChance.Value", savedFloat(saved, "branchChance", (float) dd.layout().branchChance()));
        cmd.set("#LoopChance.Value", savedFloat(saved, "loopChance", (float) dd.layout().loopChance()));
        cmd.set("#WindingFactor.Value", savedFloat(saved, "windingFactor", (float) dd.layout().windingFactor()));
        cmd.set("#PillarFreq.Value", savedFloat(saved, "pillarFreq", (float) dd.layout().pillarFrequency()));
        cmd.set("#WaterFreq.Value", savedFloat(saved, "waterFreq", (float) dd.layout().waterFrequency()));
        cmd.set("#LavaFreq.Value", savedFloat(saved, "lavaFreq", (float) dd.layout().lavaFrequency()));
        cmd.set("#TrapDensity.Value", savedFloat(saved, "trapDensity", (float) dd.layout().trapDensity()));
        cmd.set("#SecretWallChance.Value", savedFloat(saved, "secretWallChance", (float) dd.layout().secretWallChance()));
        cmd.set("#MerchantChance.Value", savedFloat(saved, "merchantSpawnChance", (float) dd.layout().merchantSpawnChance()));
        cmd.set("#EntrancePlacement.Value", savedStr(saved, "entrancePlacement", dd.layout().entrancePlacement()));
        cmd.set("#ExitDistance.Value", savedFloat(saved, "exitDistance", (float) dd.layout().exitDistance()));
        cmd.set("#EnemyDensity.Value", savedFloat(saved, "enemyDensity", (float) dd.layout().enemyDensity()));
        cmd.set("#AmbushChance.Value", savedFloat(saved, "ambushChance", (float) dd.layout().ambushChance()));
        cmd.set("#Erosion.Value", savedFloat(saved, "erosion", (float) dd.layout().erosion()));
        cmd.set("#Palette.Value", savedStr(saved, "palette", DungeonGenerateConfigMapper.defaultPalette()));
        cmd.set("#DecayFactor.Value", savedFloat(saved, "decayFactor", (float) dd.theme().decayFactor()));
        cmd.set("#OvergrowthFactor.Value", savedFloat(saved, "overgrowthFactor", (float) dd.theme().overgrowthFactor()));
        cmd.set("#FloodingFactor.Value", savedFloat(saved, "floodingFactor", (float) dd.theme().floodingFactor()));
        cmd.set("#BreatheRoomFreq.Value", savedFloat(saved, "breatheRoomFreq", (float) dd.pacing().breatheRoomFrequency()));
        cmd.set("#DifficultyRamp.Value", savedFloat(saved, "difficultyRamp", (float) dd.pacing().difficultyRamp()));

        if (saved.containsKey("windingCorridors")) {
            cmd.set("#WindingCheck #CheckBox.Value", JsonParser.toBoolean(saved.get("windingCorridors")));
        }
        if (saved.containsKey("floorTraps")) {
            cmd.set("#FloorTrapsCheck #CheckBox.Value", JsonParser.toBoolean(saved.get("floorTraps")));
        }
        if (saved.containsKey("bossRoom")) {
            cmd.set("#BossRoomCheck #CheckBox.Value", JsonParser.toBoolean(saved.get("bossRoom")));
        }
        if (saved.containsKey("removeCeiling")) {
            cmd.set("#RemoveCeilingCheck #CheckBox.Value", JsonParser.toBoolean(saved.get("removeCeiling")));
        }
        if (saved.containsKey("flatFloor")) {
            cmd.set("#FlatFloorCheck #CheckBox.Value", JsonParser.toBoolean(saved.get("flatFloor")));
        }
        if (saved.containsKey("solidFill")) {
            cmd.set("#SolidFillCheck #CheckBox.Value", JsonParser.toBoolean(saved.get("solidFill")));
        }
    }

    @Nonnull
    private GenerateEventData buildInitialEventData(@Nonnull Ref<EntityStore> ref,
                                                    @Nonnull Store<EntityStore> store,
                                                    @Nonnull Map<String, Object> saved) {
        GenerateEventData d = new GenerateEventData();
        DungeonConfig dd = DungeonConfig.withDefaults();

        if (saved.containsKey("originX")) {
            d.originX = JsonParser.toInt(saved.get("originX"));
            d.originY = JsonParser.toInt(saved.get("originY"));
            d.originZ = JsonParser.toInt(saved.get("originZ"));
        } else {
            TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
            Vector3d pos = transform.getPosition();
            d.originX = (int) pos.x;
            d.originY = (int) pos.y;
            d.originZ = (int) pos.z;
        }

        String seedRaw = savedStr(saved, "seed", DEFAULT_SEED);
        d.seed = seedRaw.isBlank() ? DEFAULT_SEED : seedRaw;
        d.floorLevel = savedInt(saved, "floorLevel", dd.floorLevel());
        d.width = savedInt(saved, "width", dd.layout().width());
        d.depth = savedInt(saved, "depth", dd.layout().depth());
        d.height = savedInt(saved, "height", dd.layout().height());
        d.maxRooms = savedInt(saved, "maxRooms", dd.layout().maxRooms());
        d.minRoomSize = savedInt(saved, "minRoomSize", dd.layout().minRoomSize());
        d.maxRoomSize = savedInt(saved, "maxRoomSize", dd.layout().maxRoomSize());
        d.corridorWidth = savedInt(saved, "corridorWidth", dd.layout().corridorWidth());
        d.maxEnemiesPerRoom = savedInt(saved, "maxEnemiesPerRoom", dd.layout().maxEnemiesPerRoom());
        d.roomDensity = savedFloat(saved, "roomDensity", (float) dd.layout().roomDensity());
        d.complexity = savedFloat(saved, "complexity", (float) dd.layout().complexity());
        d.roomShape = savedStr(saved, "roomShape", dd.layout().roomShape());
        d.irregularity = savedFloat(saved, "irregularity", (float) dd.layout().irregularity());
        d.branchChance = savedFloat(saved, "branchChance", (float) dd.layout().branchChance());
        d.loopChance = savedFloat(saved, "loopChance", (float) dd.layout().loopChance());
        d.windingFactor = savedFloat(saved, "windingFactor", (float) dd.layout().windingFactor());
        d.pillarFreq = savedFloat(saved, "pillarFreq", (float) dd.layout().pillarFrequency());
        d.waterFreq = savedFloat(saved, "waterFreq", (float) dd.layout().waterFrequency());
        d.lavaFreq = savedFloat(saved, "lavaFreq", (float) dd.layout().lavaFrequency());
        d.trapDensity = savedFloat(saved, "trapDensity", (float) dd.layout().trapDensity());
        d.secretWallChance = savedFloat(saved, "secretWallChance", (float) dd.layout().secretWallChance());
        d.merchantSpawnChance = savedFloat(saved, "merchantSpawnChance", (float) dd.layout().merchantSpawnChance());
        d.entrancePlacement = savedStr(saved, "entrancePlacement", dd.layout().entrancePlacement());
        d.exitDistance = savedFloat(saved, "exitDistance", (float) dd.layout().exitDistance());
        d.enemyDensity = savedFloat(saved, "enemyDensity", (float) dd.layout().enemyDensity());
        d.ambushChance = savedFloat(saved, "ambushChance", (float) dd.layout().ambushChance());
        d.erosion = savedFloat(saved, "erosion", (float) dd.layout().erosion());
        d.palette = savedStr(saved, "palette", DungeonGenerateConfigMapper.defaultPalette());
        d.decayFactor = savedFloat(saved, "decayFactor", (float) dd.theme().decayFactor());
        d.overgrowthFactor = savedFloat(saved, "overgrowthFactor", (float) dd.theme().overgrowthFactor());
        d.floodingFactor = savedFloat(saved, "floodingFactor", (float) dd.theme().floodingFactor());
        d.breatheRoomFreq = savedFloat(saved, "breatheRoomFreq", (float) dd.pacing().breatheRoomFrequency());
        d.difficultyRamp = savedFloat(saved, "difficultyRamp", (float) dd.pacing().difficultyRamp());

        d.windingCorridors = saved.containsKey("windingCorridors") ? JsonParser.toBoolean(saved.get("windingCorridors")) : dd.layout().windingCorridors();
        d.floorTraps = saved.containsKey("floorTraps") ? JsonParser.toBoolean(saved.get("floorTraps")) : dd.layout().floorTraps();
        d.bossRoom = saved.containsKey("bossRoom") ? JsonParser.toBoolean(saved.get("bossRoom")) : dd.layout().bossRoom();
        d.removeCeiling = saved.containsKey("removeCeiling") ? JsonParser.toBoolean(saved.get("removeCeiling")) : dd.layout().removeCeiling();
        d.flatFloor = saved.containsKey("flatFloor") ? JsonParser.toBoolean(saved.get("flatFloor")) : dd.layout().flatFloor();
        d.solidFill = saved.containsKey("solidFill") ? JsonParser.toBoolean(saved.get("solidFill")) : dd.layout().solidFill();
        return d;
    }

    // ============================================
    // Event Handling
    // ============================================

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store,
                                @Nonnull GenerateEventData data) {
        LOGGER.atInfo().log("[DungeonGen] event action=%s width=%s seed=%s palette=%s",
                data.action, data.width, data.seed, data.palette);
        if ("Generate".equals(data.action)) {
            handleGenerate(ref, store, data);
            return;
        }
        if ("Roll".equals(data.action)) {
            handleRoll(data);
            return;
        }
        if ("Preview".equals(data.action)) {
            schedulePreview(data, true);
        }
    }

    private void handleRoll(@Nonnull GenerateEventData data) {
        String newSeed = generateRandomSeed();
        data.seed = newSeed;
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#Seed.Value", newSeed);
        sendPageUpdate(cmd);
        schedulePreview(data, false);
    }

    @Nonnull
    private static String generateRandomSeed() {
        long value = java.util.concurrent.ThreadLocalRandom.current().nextLong(0L, 1_000_000_000L);
        return Long.toString(value);
    }

    private void handleGenerate(@Nonnull Ref<EntityStore> ref,
                                @Nonnull Store<EntityStore> store,
                                @Nonnull GenerateEventData d) {
        DungeonAssemblyService assembly = DungeonGenPlugin.get().getAssemblyService();
        if (assembly == null) {
            playerRef.sendMessage(Message.raw("[DungeonGen] Generator not available yet").color("#FF5555"));
            updateStatus("Error - generator unavailable", false);
            return;
        }

        DungeonConfig config;
        try {
            config = DungeonGenerateConfigMapper.toConfig(d);
        } catch (Exception e) {
            playerRef.sendMessage(Message.raw("[DungeonGen] Invalid config: " + e.getMessage()).color("#FF5555"));
            LOGGER.atWarning().log("[DungeonGen] Config parse error: %s", e.getMessage());
            return;
        }

        saveConfig(d);

        Vec3i center = config.origin();
        Vec3i corner = centerToCorner(center, config.layout());
        DungeonConfig assemblyConfig = withOriginAndWorld(config, corner, world.getName());

        updateStatus("Clearing target volume...");
        playerRef.sendMessage(Message.raw(String.format(
                "[DungeonGen] Clearing %dx%dx%d at center (%d, %d, %d) in '%s'...",
                assemblyConfig.layout().width(), assemblyConfig.layout().height(), assemblyConfig.layout().depth(),
                center.x(), center.y(), center.z(), world.getName()
        )).color("#FFD700"));

        clearAssemblyVolume(corner, assemblyConfig.layout())
                .thenCompose(ignored -> {
                    world.execute(() -> updateStatus("Generating dungeon..."));
                    return assembly.generateAndAssemble(assemblyConfig);
                })
                .thenAccept(result -> world.execute(() -> enterAssembledDungeon(ref, store, assemblyConfig, result)))
                .exceptionally(e -> {
                    world.execute(() -> handleAssemblyFailure(e));
                    return null;
                });
    }

    private void enterAssembledDungeon(@Nonnull Ref<EntityStore> ref,
                                       @Nonnull Store<EntityStore> store,
                                       @Nonnull DungeonConfig config,
                                       @Nonnull GenerationResult result) {
        if (result.assemblyError() != null) {
            handleAssemblyFailure(new IllegalStateException(result.assemblyError()));
            return;
        }

        sendGenerationSummary(result);

        if (!ref.isValid()) {
            return;
        }

        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent != null) {
            playerComponent.getPageManager().setPage(ref, store, Page.None);
        }

        Vec3i entranceLocal = result.entrancePosition();
        Vec3i origin = config.origin();
        Vec3i target = entranceLocal != null
                ? new Vec3i(origin.x() + entranceLocal.x(),
                            origin.y() + entranceLocal.y(),
                            origin.z() + entranceLocal.z())
                : origin;

        try {
            store.addComponent(
                    ref,
                    Teleport.getComponentType(),
                    Teleport.createForPlayer(world, toPlayerTransform(target))
            );
            updateStatus(String.format("Ready - teleported to (%d, %d, %d)", target.x(), target.y(), target.z()), false);
        } catch (Exception e) {
            String msg = extractErrorMessage(e);
            LOGGER.atSevere().log("[DungeonGen] Failed to teleport to entrance: %s", msg);
            playerRef.sendMessage(Message.raw("[DungeonGen] Failed to teleport: " + msg).color("#FF5555"));
            updateStatus("Error - see chat", false);
        }
    }

    private void handleAssemblyFailure(@Nonnull Throwable throwable) {
        String msg = extractErrorMessage(throwable);
        playerRef.sendMessage(Message.raw("[DungeonGen] Assembly failed: " + msg).color("#FF5555"));
        LOGGER.atSevere().log("[DungeonGen] Assembly failed: %s", msg);
        updateStatus("Error - see chat", false);
    }

    private void sendGenerationSummary(@Nonnull GenerationResult result) {
        String summary = String.format(
                "[DungeonGen] Done! %d rooms, %d corridors, %d blocks, %d spawners, %d merchants, %d chests - gen %dms, asm %dms",
                result.rooms(), result.corridors(), result.totalBlocks(), result.spawners(),
                result.merchants(), result.chests(), result.generationTimeMs(), result.assemblyTimeMs()
        );
        String color = result.assemblyError() == null ? "#55FF55" : "#FFAA00";
        playerRef.sendMessage(Message.raw(summary).color(color));

        if (result.assemblyError() != null) {
            playerRef.sendMessage(Message.raw("[DungeonGen] Assembly error: " + result.assemblyError()).color("#FF5555"));
        }

        updateStatus(String.format("Done - %d rooms, %dms", result.rooms(), result.generationTimeMs()), false);
    }

    @Nonnull
    private static Vec3i centerToCorner(@Nonnull Vec3i center, @Nonnull LayoutConfig layout) {
        return new Vec3i(
                center.x() - layout.width() / 2,
                center.y() - layout.height() / 2,
                center.z() - layout.depth() / 2
        );
    }

    @Nonnull
    private static DungeonConfig withOriginAndWorld(@Nonnull DungeonConfig config,
                                                    @Nonnull Vec3i origin,
                                                    @Nonnull String worldName) {
        return new DungeonConfig(
                config.seed(),
                config.preset(),
                worldName,
                origin,
                config.layout(),
                config.theme(),
                config.pacing(),
                true,
                config.floorLevel()
        );
    }

    // ============================================
    // Cleanup
    // ============================================

    @Nonnull
    private CompletableFuture<Void> clearAssemblyVolume(@Nonnull Vec3i corner, @Nonnull LayoutConfig layout) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger yIndex = new AtomicInteger(0);
        clearVolumeYSlice(corner, layout.width(), layout.height(), layout.depth(), yIndex, future);
        return future;
    }

    private void clearVolumeYSlice(@Nonnull Vec3i corner, int width, int height, int depth,
                                   @Nonnull AtomicInteger yIndex,
                                   @Nonnull CompletableFuture<Void> future) {
        world.execute(() -> {
            try {
                int y = yIndex.getAndIncrement();
                if (y >= height) {
                    future.complete(null);
                    return;
                }
                int worldY = corner.y() + y;
                Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
                for (int dx = 0; dx < width; dx++) {
                    int worldX = corner.x() + dx;
                    for (int dz = 0; dz < depth; dz++) {
                        int worldZ = corner.z() + dz;
                        long chunkIndex = ChunkUtil.indexChunkFromBlock(worldX, worldZ);
                        WorldChunk chunk = world.getChunk(chunkIndex);
                        if (chunk == null) {
                            continue;
                        }
                        chunk.setBlock(worldX, worldY, worldZ, 0);
                        clearFluidAt(chunkStore, chunk, worldX, worldY, worldZ);
                    }
                }
                clearVolumeYSlice(corner, width, height, depth, yIndex, future);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
    }

    private static void clearFluidAt(@Nonnull Store<ChunkStore> store, @Nonnull WorldChunk chunk,
                                     int x, int y, int z) {
        try {
            ChunkColumn column = store.getComponent(chunk.getReference(), ChunkColumn.getComponentType());
            if (column == null) {
                return;
            }
            Ref<ChunkStore> section = column.getSection(ChunkUtil.chunkCoordinate(y));
            if (section == null || !section.isValid()) {
                return;
            }
            FluidSection fluidSection = store.getComponent(section, FluidSection.getComponentType());
            if (fluidSection == null) {
                return;
            }
            fluidSection.setFluid(x, y, z, 0, (byte) 0);
        } catch (Exception ignored) {
            // best-effort: missing fluid section is fine
        }
    }

    // ============================================
    // Preview
    // ============================================

    private void schedulePreview(@Nonnull GenerateEventData data, boolean debounce) {
        DungeonAssemblyService assembly = DungeonGenPlugin.get().getAssemblyService();
        if (assembly == null) {
            updatePreviewStatus("Preview unavailable: generator not ready");
            return;
        }

        long requestSequence = previewRequestSequence.incrementAndGet();
        DungeonConfig previewConfig;
        try {
            previewConfig = toPreviewArtifactsConfig(DungeonGenerateConfigMapper.toConfig(data));
        } catch (RuntimeException e) {
            updatePreviewStatus("Preview unavailable: " + extractErrorMessage(e));
            return;
        }

        long delayMs = debounce ? PREVIEW_DEBOUNCE_MS : 0L;
        LOGGER.atInfo().log("[DungeonGen] schedulePreview seq=%d delay=%dms width=%d palette=%s",
                requestSequence, delayMs, previewConfig.layout().width(), previewConfig.theme().palette());
        Executor delayedExecutor = CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS);
        delayedExecutor.execute(() -> {
            long current = previewRequestSequence.get();
            if (requestSequence != current) {
                LOGGER.atInfo().log("[DungeonGen] preview seq=%d superseded by seq=%d", requestSequence, current);
                return;
            }
            try {
                LOGGER.atInfo().log("[DungeonGen] preview seq=%d firing generation", requestSequence);
                assembly.generatePreviewArtifacts(previewConfig)
                        .thenApply(previewPacketFactory::createPacket)
                        .whenComplete((packet, error) -> world.execute(() ->
                                applyPreviewResult(requestSequence, packet, error)));
            } catch (RuntimeException e) {
                world.execute(() -> applyPreviewFailure(requestSequence, e));
            }
        });
    }

    private void applyPreviewResult(long requestSequence,
                                    @Nullable BuilderToolPrefabPreview packet,
                                    @Nullable Throwable error) {
        if (requestSequence != previewRequestSequence.get()) {
            return;
        }
        if (error != null) {
            applyPreviewFailure(requestSequence, error);
            return;
        }
        if (packet == null || playerRef.getReference() == null) {
            return;
        }

        DungeonPreviewPacketFactory.applyTintFromPlayerPosition(packet, playerRef);
        int blockChanges = packet.blocksChange == null ? 0 : packet.blocksChange.length;
        int fluidChanges = packet.fluidsChange == null ? 0 : packet.fluidsChange.length;
        writePreviewPacket(packet);
        lastAppliedPreviewRequestSequence = requestSequence;
        if (blockChanges == 0 && fluidChanges == 0) {
            updatePreviewStatus("Preview empty: no renderable blocks");
            return;
        }
        updatePreviewStatus(String.format("Preview updated: %d blocks, %d fluids", blockChanges, fluidChanges));
    }

    private void applyPreviewFailure(long requestSequence, @Nonnull Throwable error) {
        if (requestSequence != previewRequestSequence.get()) {
            return;
        }
        updatePreviewStatus("Preview unavailable: " + extractErrorMessage(error));
    }

    private void writePreviewPacket(@Nonnull BuilderToolPrefabPreview packet) {
        if (lastAppliedPreviewRequestSequence != 0L) {
            playerRef.getPacketHandler().write(new BuilderToolPrefabPreview());
        }
        playerRef.getPacketHandler().write(packet);
    }

    @Nonnull
    private static DungeonConfig toPreviewArtifactsConfig(@Nonnull DungeonConfig config) {
        return new DungeonConfig(
                config.seed(),
                config.preset(),
                "dungeon-gen-ui-preview",
                Vec3i.ZERO,
                config.layout(),
                config.theme(),
                config.pacing(),
                false,
                config.floorLevel()
        );
    }

    private void updatePreviewStatus(@Nonnull String text) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#PreviewStatusLabel.Text", text);
        sendPageUpdate(cmd);
    }

    private void sendPageUpdate(@Nonnull UICommandBuilder cmd) {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }
        player.getPageManager().updateCustomPage(new CustomPage(
                getClass().getName(),
                false,
                false,
                lifetime,
                cmd.getCommands(),
                UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY
        ));
    }

    @Override
    public void onDismiss(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store) {
        previewRequestSequence.incrementAndGet();
        if (playerRef.getReference() != null) {
            playerRef.getPacketHandler().write(new BuilderToolPrefabPreview());
        }
        lastAppliedPreviewRequestSequence = 0L;
    }

    // ============================================
    // Status text
    // ============================================

    private void updateStatus(@Nonnull String text) {
        updateStatus(text, true);
    }

    private void updateStatus(@Nonnull String text, boolean showSpinner) {
        UICommandBuilder cmd = new UICommandBuilder();
        cmd.set("#StatusGroup.Visible", true);
        cmd.set("#StatusGroup #StatusLabel.Text", text);
        cmd.set("#StatusGroup #StatusSpinner.Visible", showSpinner);
        sendPageUpdate(cmd);
    }

    // ============================================
    // Helpers
    // ============================================

    @Nonnull
    private static Transform toPlayerTransform(@Nonnull Vec3i position) {
        return new Transform(position.x() + 0.5D, position.y(), position.z() + 0.5D);
    }

    @Nonnull
    private static String extractErrorMessage(@Nonnull Throwable throwable) {
        Throwable current = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        while (current.getCause() != null && current.getMessage() == null) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null || message.isBlank() ? current.getClass().getSimpleName() : message;
    }

    // ============================================
    // Saved config (JSON persistence)
    // ============================================

    @Nonnull
    private static Map<String, Object> loadSavedConfig() {
        Path configPath = DungeonGenPlugin.get().getDataDirectory().resolve(CONFIG_FILENAME);
        if (!Files.exists(configPath)) {
            return Map.of();
        }
        try {
            String json = Files.readString(configPath);
            Map<String, Object> result = JsonParser.parseObject(json);
            return result != null ? result : Map.of();
        } catch (Exception e) {
            LOGGER.atWarning().log("[DungeonGen] Failed to load saved config: %s", e.getMessage());
            return Map.of();
        }
    }

    private static void saveConfig(@Nonnull GenerateEventData d) {
        try {
            Path dir = DungeonGenPlugin.get().getDataDirectory();
            Files.createDirectories(dir);
            Path configPath = dir.resolve(CONFIG_FILENAME);
            Files.writeString(configPath, buildConfigJson(d));
        } catch (Exception e) {
            LOGGER.atWarning().log("[DungeonGen] Failed to save config: %s", e.getMessage());
        }
    }

    @Nonnull
    private static String buildConfigJson(@Nonnull GenerateEventData d) {
        StringBuilder sb = new StringBuilder(768);
        sb.append("{\n");
        appendStr(sb, "seed", d.seed);
        appendInt(sb, "originX", d.originX);
        appendInt(sb, "originY", d.originY);
        appendInt(sb, "originZ", d.originZ);
        appendInt(sb, "floorLevel", d.floorLevel);
        appendInt(sb, "width", d.width);
        appendInt(sb, "depth", d.depth);
        appendInt(sb, "height", d.height);
        appendInt(sb, "maxRooms", d.maxRooms);
        appendFloat(sb, "roomDensity", d.roomDensity);
        appendFloat(sb, "complexity", d.complexity);
        appendInt(sb, "minRoomSize", d.minRoomSize);
        appendInt(sb, "maxRoomSize", d.maxRoomSize);
        appendStr(sb, "roomShape", d.roomShape);
        appendFloat(sb, "irregularity", d.irregularity);
        appendInt(sb, "corridorWidth", d.corridorWidth);
        appendFloat(sb, "branchChance", d.branchChance);
        appendFloat(sb, "loopChance", d.loopChance);
        appendBool(sb, "windingCorridors", d.windingCorridors);
        appendFloat(sb, "windingFactor", d.windingFactor);
        appendFloat(sb, "pillarFreq", d.pillarFreq);
        appendFloat(sb, "waterFreq", d.waterFreq);
        appendFloat(sb, "lavaFreq", d.lavaFreq);
        appendFloat(sb, "trapDensity", d.trapDensity);
        appendFloat(sb, "secretWallChance", d.secretWallChance);
        appendFloat(sb, "merchantSpawnChance", d.merchantSpawnChance);
        appendBool(sb, "floorTraps", d.floorTraps);
        appendStr(sb, "entrancePlacement", d.entrancePlacement);
        appendFloat(sb, "exitDistance", d.exitDistance);
        appendFloat(sb, "enemyDensity", d.enemyDensity);
        appendInt(sb, "maxEnemiesPerRoom", d.maxEnemiesPerRoom);
        appendFloat(sb, "ambushChance", d.ambushChance);
        appendBool(sb, "bossRoom", d.bossRoom);
        appendFloat(sb, "erosion", d.erosion);
        appendBool(sb, "removeCeiling", d.removeCeiling);
        appendBool(sb, "flatFloor", d.flatFloor);
        appendBool(sb, "solidFill", d.solidFill);
        appendStr(sb, "palette", d.palette);
        appendFloat(sb, "decayFactor", d.decayFactor);
        appendFloat(sb, "overgrowthFactor", d.overgrowthFactor);
        appendFloat(sb, "floodingFactor", d.floodingFactor);
        appendFloat(sb, "breatheRoomFreq", d.breatheRoomFreq);
        sb.append("  \"difficultyRamp\": ");
        sb.append(d.difficultyRamp != null ? d.difficultyRamp.toString() : "null");
        sb.append('\n').append('}');
        return sb.toString();
    }

    private static void appendStr(@Nonnull StringBuilder sb, @Nonnull String key, @Nullable String value) {
        sb.append("  \"").append(key).append("\": ");
        if (value != null) {
            sb.append('"').append(escapeJson(value)).append('"');
        } else {
            sb.append("null");
        }
        sb.append(",\n");
    }

    private static void appendInt(@Nonnull StringBuilder sb, @Nonnull String key, @Nullable Integer value) {
        sb.append("  \"").append(key).append("\": ");
        sb.append(value != null ? value.toString() : "null");
        sb.append(",\n");
    }

    private static void appendBool(@Nonnull StringBuilder sb, @Nonnull String key, @Nullable Boolean value) {
        sb.append("  \"").append(key).append("\": ");
        sb.append(value != null ? value.toString() : "null");
        sb.append(",\n");
    }

    private static void appendFloat(@Nonnull StringBuilder sb, @Nonnull String key, @Nullable Float value) {
        sb.append("  \"").append(key).append("\": ");
        sb.append(value != null ? value.toString() : "null");
        sb.append(",\n");
    }

    @Nonnull
    private static String escapeJson(@Nonnull String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    @Nonnull
    private static String savedStr(@Nonnull Map<String, Object> saved, @Nonnull String key,
                                   @Nonnull String fallback) {
        String val = JsonParser.toStringOrNull(saved.get(key));
        return val != null ? val : fallback;
    }

    private static int savedInt(@Nonnull Map<String, Object> saved, @Nonnull String key, int fallback) {
        if (!saved.containsKey(key)) return fallback;
        try {
            return JsonParser.toInt(saved.get(key));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static float savedFloat(@Nonnull Map<String, Object> saved, @Nonnull String key, float fallback) {
        Object val = saved.get(key);
        if (val == null) return fallback;
        if (val instanceof Number n) return n.floatValue();
        try {
            return Float.parseFloat(val.toString());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ============================================
    // Event Binding
    // ============================================

    private static final List<String> PREVIEW_VALUE_BINDING_SELECTORS = List.of(
            "#Seed",
            "#OriginX", "#OriginY", "#OriginZ", "#FloorLevel",
            "#Width", "#Depth", "#Height",
            "#MaxRooms", "#RoomDensity", "#Complexity", "#MinRoomSize", "#MaxRoomSize", "#RoomShape", "#Irregularity",
            "#CorridorWidth", "#BranchChance", "#LoopChance", "#WindingCheck #CheckBox", "#WindingFactor",
            "#PillarFreq", "#WaterFreq", "#LavaFreq", "#TrapDensity", "#FloorTrapsCheck #CheckBox", "#SecretWallChance", "#MerchantChance",
            "#EntrancePlacement", "#ExitDistance",
            "#EnemyDensity", "#MaxEnemiesPerRoom", "#AmbushChance", "#BossRoomCheck #CheckBox",
            "#Erosion", "#RemoveCeilingCheck #CheckBox", "#FlatFloorCheck #CheckBox", "#SolidFillCheck #CheckBox",
            "#Palette", "#DecayFactor", "#OvergrowthFactor", "#FloodingFactor",
            "#BreatheRoomFreq", "#DifficultyRamp"
    );

    private static void addPreviewEventBindings(@Nonnull UIEventBuilder events) {
        EventData binding = buildFieldBinding("Preview");
        for (String selector : PREVIEW_VALUE_BINDING_SELECTORS) {
            events.addEventBinding(CustomUIEventBindingType.ValueChanged, selector, binding, false);
        }
    }

    @Nonnull
    private static EventData buildFieldBinding(@Nonnull String action) {
        return new EventData()
                .append("Action", action)
                .append("@Seed", "#Seed.Value")
                .append("@OriginX", "#OriginX.Value")
                .append("@OriginY", "#OriginY.Value")
                .append("@OriginZ", "#OriginZ.Value")
                .append("@FloorLevel", "#FloorLevel.Value")
                .append("@Width", "#Width.Value")
                .append("@Depth", "#Depth.Value")
                .append("@Height", "#Height.Value")
                .append("@MaxRooms", "#MaxRooms.Value")
                .append("@RoomDensity", "#RoomDensity.Value")
                .append("@Complexity", "#Complexity.Value")
                .append("@MinRoomSize", "#MinRoomSize.Value")
                .append("@MaxRoomSize", "#MaxRoomSize.Value")
                .append("@RoomShape", "#RoomShape.Value")
                .append("@Irregularity", "#Irregularity.Value")
                .append("@CorridorWidth", "#CorridorWidth.Value")
                .append("@BranchChance", "#BranchChance.Value")
                .append("@LoopChance", "#LoopChance.Value")
                .append("@Winding", "#WindingCheck #CheckBox.Value")
                .append("@WindingFactor", "#WindingFactor.Value")
                .append("@PillarFreq", "#PillarFreq.Value")
                .append("@WaterFreq", "#WaterFreq.Value")
                .append("@LavaFreq", "#LavaFreq.Value")
                .append("@TrapDensity", "#TrapDensity.Value")
                .append("@SecretWallChance", "#SecretWallChance.Value")
                .append("@MerchantChance", "#MerchantChance.Value")
                .append("@FloorTraps", "#FloorTrapsCheck #CheckBox.Value")
                .append("@EntrancePlacement", "#EntrancePlacement.Value")
                .append("@ExitDistance", "#ExitDistance.Value")
                .append("@EnemyDensity", "#EnemyDensity.Value")
                .append("@MaxEnemiesPerRoom", "#MaxEnemiesPerRoom.Value")
                .append("@AmbushChance", "#AmbushChance.Value")
                .append("@BossRoom", "#BossRoomCheck #CheckBox.Value")
                .append("@Erosion", "#Erosion.Value")
                .append("@RemoveCeiling", "#RemoveCeilingCheck #CheckBox.Value")
                .append("@FlatFloor", "#FlatFloorCheck #CheckBox.Value")
                .append("@SolidFill", "#SolidFillCheck #CheckBox.Value")
                .append("@Palette", "#Palette.Value")
                .append("@DecayFactor", "#DecayFactor.Value")
                .append("@OvergrowthFactor", "#OvergrowthFactor.Value")
                .append("@FloodingFactor", "#FloodingFactor.Value")
                .append("@BreatheRoomFreq", "#BreatheRoomFreq.Value")
                .append("@DifficultyRamp", "#DifficultyRamp.Value");
    }

    // ============================================
    // Event Data Codec
    // ============================================

    /**
     * Event data captured from the page form when the user interacts with
     * controls (Preview event) or clicks Generate.
     */
    public static class GenerateEventData {

        public static final BuilderCodec<GenerateEventData> CODEC = BuilderCodec.builder(
                        GenerateEventData.class, GenerateEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (e, v) -> e.action = v, e -> e.action)
                .add()
                .append(new KeyedCodec<>("@Seed", Codec.STRING), (e, v) -> e.seed = v, e -> e.seed)
                .add()
                .append(new KeyedCodec<>("@OriginX", Codec.INTEGER), (e, v) -> e.originX = v, e -> e.originX)
                .add()
                .append(new KeyedCodec<>("@OriginY", Codec.INTEGER), (e, v) -> e.originY = v, e -> e.originY)
                .add()
                .append(new KeyedCodec<>("@OriginZ", Codec.INTEGER), (e, v) -> e.originZ = v, e -> e.originZ)
                .add()
                .append(new KeyedCodec<>("@FloorLevel", Codec.INTEGER), (e, v) -> e.floorLevel = v, e -> e.floorLevel)
                .add()
                .append(new KeyedCodec<>("@Width", Codec.INTEGER), (e, v) -> e.width = v, e -> e.width)
                .add()
                .append(new KeyedCodec<>("@Depth", Codec.INTEGER), (e, v) -> e.depth = v, e -> e.depth)
                .add()
                .append(new KeyedCodec<>("@Height", Codec.INTEGER), (e, v) -> e.height = v, e -> e.height)
                .add()
                .append(new KeyedCodec<>("@MaxRooms", Codec.INTEGER), (e, v) -> e.maxRooms = v, e -> e.maxRooms)
                .add()
                .append(new KeyedCodec<>("@RoomDensity", Codec.FLOAT), (e, v) -> e.roomDensity = v, e -> e.roomDensity)
                .add()
                .append(new KeyedCodec<>("@Complexity", Codec.FLOAT), (e, v) -> e.complexity = v, e -> e.complexity)
                .add()
                .append(new KeyedCodec<>("@MinRoomSize", Codec.INTEGER), (e, v) -> e.minRoomSize = v, e -> e.minRoomSize)
                .add()
                .append(new KeyedCodec<>("@MaxRoomSize", Codec.INTEGER), (e, v) -> e.maxRoomSize = v, e -> e.maxRoomSize)
                .add()
                .append(new KeyedCodec<>("@RoomShape", Codec.STRING), (e, v) -> e.roomShape = v, e -> e.roomShape)
                .add()
                .append(new KeyedCodec<>("@Irregularity", Codec.FLOAT), (e, v) -> e.irregularity = v, e -> e.irregularity)
                .add()
                .append(new KeyedCodec<>("@CorridorWidth", Codec.INTEGER), (e, v) -> e.corridorWidth = v, e -> e.corridorWidth)
                .add()
                .append(new KeyedCodec<>("@BranchChance", Codec.FLOAT), (e, v) -> e.branchChance = v, e -> e.branchChance)
                .add()
                .append(new KeyedCodec<>("@LoopChance", Codec.FLOAT), (e, v) -> e.loopChance = v, e -> e.loopChance)
                .add()
                .append(new KeyedCodec<>("@Winding", Codec.BOOLEAN), (e, v) -> e.windingCorridors = v, e -> e.windingCorridors)
                .add()
                .append(new KeyedCodec<>("@WindingFactor", Codec.FLOAT), (e, v) -> e.windingFactor = v, e -> e.windingFactor)
                .add()
                .append(new KeyedCodec<>("@PillarFreq", Codec.FLOAT), (e, v) -> e.pillarFreq = v, e -> e.pillarFreq)
                .add()
                .append(new KeyedCodec<>("@WaterFreq", Codec.FLOAT), (e, v) -> e.waterFreq = v, e -> e.waterFreq)
                .add()
                .append(new KeyedCodec<>("@LavaFreq", Codec.FLOAT), (e, v) -> e.lavaFreq = v, e -> e.lavaFreq)
                .add()
                .append(new KeyedCodec<>("@TrapDensity", Codec.FLOAT), (e, v) -> e.trapDensity = v, e -> e.trapDensity)
                .add()
                .append(new KeyedCodec<>("@SecretWallChance", Codec.FLOAT), (e, v) -> e.secretWallChance = v, e -> e.secretWallChance)
                .add()
                .append(new KeyedCodec<>("@MerchantChance", Codec.FLOAT), (e, v) -> e.merchantSpawnChance = v, e -> e.merchantSpawnChance)
                .add()
                .append(new KeyedCodec<>("@FloorTraps", Codec.BOOLEAN), (e, v) -> e.floorTraps = v, e -> e.floorTraps)
                .add()
                .append(new KeyedCodec<>("@EntrancePlacement", Codec.STRING), (e, v) -> e.entrancePlacement = v, e -> e.entrancePlacement)
                .add()
                .append(new KeyedCodec<>("@ExitDistance", Codec.FLOAT), (e, v) -> e.exitDistance = v, e -> e.exitDistance)
                .add()
                .append(new KeyedCodec<>("@EnemyDensity", Codec.FLOAT), (e, v) -> e.enemyDensity = v, e -> e.enemyDensity)
                .add()
                .append(new KeyedCodec<>("@MaxEnemiesPerRoom", Codec.INTEGER), (e, v) -> e.maxEnemiesPerRoom = v, e -> e.maxEnemiesPerRoom)
                .add()
                .append(new KeyedCodec<>("@AmbushChance", Codec.FLOAT), (e, v) -> e.ambushChance = v, e -> e.ambushChance)
                .add()
                .append(new KeyedCodec<>("@BossRoom", Codec.BOOLEAN), (e, v) -> e.bossRoom = v, e -> e.bossRoom)
                .add()
                .append(new KeyedCodec<>("@Erosion", Codec.FLOAT), (e, v) -> e.erosion = v, e -> e.erosion)
                .add()
                .append(new KeyedCodec<>("@RemoveCeiling", Codec.BOOLEAN), (e, v) -> e.removeCeiling = v, e -> e.removeCeiling)
                .add()
                .append(new KeyedCodec<>("@FlatFloor", Codec.BOOLEAN), (e, v) -> e.flatFloor = v, e -> e.flatFloor)
                .add()
                .append(new KeyedCodec<>("@SolidFill", Codec.BOOLEAN), (e, v) -> e.solidFill = v, e -> e.solidFill)
                .add()
                .append(new KeyedCodec<>("@Palette", Codec.STRING), (e, v) -> e.palette = v, e -> e.palette)
                .add()
                .append(new KeyedCodec<>("@DecayFactor", Codec.FLOAT), (e, v) -> e.decayFactor = v, e -> e.decayFactor)
                .add()
                .append(new KeyedCodec<>("@OvergrowthFactor", Codec.FLOAT), (e, v) -> e.overgrowthFactor = v, e -> e.overgrowthFactor)
                .add()
                .append(new KeyedCodec<>("@FloodingFactor", Codec.FLOAT), (e, v) -> e.floodingFactor = v, e -> e.floodingFactor)
                .add()
                .append(new KeyedCodec<>("@BreatheRoomFreq", Codec.FLOAT), (e, v) -> e.breatheRoomFreq = v, e -> e.breatheRoomFreq)
                .add()
                .append(new KeyedCodec<>("@DifficultyRamp", Codec.FLOAT), (e, v) -> e.difficultyRamp = v, e -> e.difficultyRamp)
                .add()
                .build();

        public String action;
        public String seed;
        public Integer originX, originY, originZ;
        public Integer floorLevel;
        public Integer width, depth, height;
        public Integer maxRooms;
        public Float roomDensity, complexity;
        public Integer minRoomSize, maxRoomSize;
        public String roomShape;
        public Float irregularity;
        public Integer corridorWidth;
        public Float branchChance, loopChance;
        public Boolean windingCorridors;
        public Float windingFactor;
        public Float pillarFreq, waterFreq, lavaFreq;
        public Float trapDensity, secretWallChance, merchantSpawnChance;
        public Boolean floorTraps;
        public String entrancePlacement;
        public Float exitDistance;
        public Float enemyDensity;
        public Integer maxEnemiesPerRoom;
        public Float ambushChance;
        public Boolean bossRoom;
        public Float erosion;
        public Boolean removeCeiling, flatFloor, solidFill;
        public String palette;
        public Float decayFactor, overgrowthFactor, floodingFactor;
        public Float breatheRoomFreq, difficultyRamp;
    }
}
