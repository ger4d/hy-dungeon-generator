package com.duntale.dungeongen.preview;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.generator.GenerationArtifacts;
import com.duntale.dungeongen.generator.GenerationResult;
import com.duntale.dungeongen.generator.layout.DungeonGraph;
import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.ChestDefinition;
import com.duntale.dungeongen.model.ChestTier;
import com.duntale.dungeongen.model.DungeonBlueprint;
import com.duntale.dungeongen.model.MerchantDefinition;
import com.duntale.dungeongen.model.SpawnEntry;
import com.duntale.dungeongen.model.SpawnerDefinition;
import com.duntale.dungeongen.model.SpawnerType;
import com.duntale.dungeongen.model.SpawnerVariant;
import com.duntale.dungeongen.model.TriggerConfig;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonPreviewPacketFactoryTest {

    @Test
    void createPacketEmitsBlocksAndFluidsButNoEntityMarkers() {
        DungeonPreviewPacketFactory factory = new DungeonPreviewPacketFactory(
                DungeonPreviewPacketFactoryTest::resolveBlock,
                ignored -> 21,
                ignored -> (byte) 1
        );
        GenerationArtifacts artifacts = artifactsWithSpawnerAndMerchant();

        BuilderToolPrefabPreview packet = factory.createPacket(artifacts);

        assertEquals(23, packet.tilt);
        assertEquals(27, packet.spinSpeed);
        assertEquals(100, packet.previewScale);
        assertNotNull(packet.blocksChange);
        assertEquals(2, packet.blocksChange.length);
        assertTrue(List.of(packet.blocksChange).stream().anyMatch(change -> change.block == 11));
        assertTrue(List.of(packet.blocksChange).stream().anyMatch(change -> change.block == 12));
        assertNotNull(packet.fluidsChange);
        assertEquals(1, packet.fluidsChange.length);
        assertEquals(21, packet.fluidsChange[0].fluidId);
        assertEquals(1, packet.fluidsChange[0].fluidLevel);
        assertNotNull(packet.entityChanges);
        assertEquals(0, packet.entityChanges.length,
                "Standalone dungeon-gen preview must omit spawner/merchant entity markers");
    }

    @Test
    void blockRotationFromGridIsPreservedInPacket() {
        DungeonPreviewPacketFactory factory = new DungeonPreviewPacketFactory(
                DungeonPreviewPacketFactoryTest::resolveBlock,
                ignored -> -1,
                ignored -> (byte) 0
        );

        BlockGrid grid = new BlockGrid(4, 4, 4);
        grid.set(1, 1, 1, "Rock_Stone_Brick", 2);
        DungeonBlueprint blueprint = new DungeonBlueprint("rot-test", new DungeonGraph());

        BuilderToolPrefabPreview packet = factory.createPacket(emptyArtifacts(grid, blueprint));

        assertNotNull(packet.blocksChange);
        assertEquals(1, packet.blocksChange.length);
        assertEquals(11, packet.blocksChange[0].block);
        assertEquals(2, packet.blocksChange[0].rotation);
    }

    @Test
    void emptyGridProducesEmptyButValidPacket() {
        DungeonPreviewPacketFactory factory = new DungeonPreviewPacketFactory(
                DungeonPreviewPacketFactoryTest::resolveBlock,
                ignored -> -1,
                ignored -> (byte) 0
        );

        BlockGrid grid = new BlockGrid(4, 4, 4);
        DungeonBlueprint blueprint = new DungeonBlueprint("empty", new DungeonGraph());
        BuilderToolPrefabPreview packet = factory.createPacket(emptyArtifacts(grid, blueprint));

        assertNotNull(packet.blocksChange);
        assertEquals(0, packet.blocksChange.length);
        assertNotNull(packet.fluidsChange);
        assertEquals(0, packet.fluidsChange.length);
        assertNotNull(packet.entityChanges);
        assertEquals(0, packet.entityChanges.length);
    }

    @Test
    void unresolvedBlockIdsAreSkipped() {
        DungeonPreviewPacketFactory factory = new DungeonPreviewPacketFactory(
                blockId -> -1,
                ignored -> -1,
                ignored -> (byte) 0
        );

        BlockGrid grid = new BlockGrid(4, 4, 4);
        grid.set(1, 1, 1, "Unknown_Block");
        DungeonBlueprint blueprint = new DungeonBlueprint("unresolved", new DungeonGraph());
        BuilderToolPrefabPreview packet = factory.createPacket(emptyArtifacts(grid, blueprint));

        assertNotNull(packet.blocksChange);
        assertEquals(0, packet.blocksChange.length);
    }

    @Nonnull
    private static GenerationArtifacts artifactsWithSpawnerAndMerchant() {
        BlockGrid grid = new BlockGrid(4, 4, 4);
        grid.set(1, 1, 1, "Rock_Stone_Brick");
        grid.set(2, 1, 1, "Fluid_Water");
        grid.set(3, 1, 1, "Furniture_Crude_Chest_Small");

        SpawnerDefinition spawner = new SpawnerDefinition(
                1,
                1, 2, 1,
                1,
                SpawnerType.FIXED,
                TriggerConfig.proximity(5.0),
                List.of(new SpawnEntry("Skeleton_Soldier", 1.0)),
                1,
                List.of(new Vec3i(1, 2, 1)),
                SpawnerVariant.NORMAL,
                1,
                0
        );
        MerchantDefinition merchant = new MerchantDefinition(2, 2, 2, 1);
        ChestDefinition chest = new ChestDefinition(3, 1, 1, ChestTier.REGULAR, "Furniture_Crude_Chest_Small");
        DungeonBlueprint blueprint = new DungeonBlueprint("preview-test", new DungeonGraph());
        blueprint.addSpawner(spawner);
        blueprint.addMerchant(merchant);
        GenerationResult result = new GenerationResult(
                "preview-test",
                1, 0,
                grid.getBlockCount(),
                1, List.of(spawner),
                1, List.of(merchant),
                1, List.of(chest),
                new Vec3i(1, 2, 1),
                new Vec3i(2, 2, 2),
                1L, 0L, null
        );
        return new GenerationArtifacts(result, grid, blueprint);
    }

    @Nonnull
    private static GenerationArtifacts emptyArtifacts(@Nonnull BlockGrid grid,
                                                      @Nonnull DungeonBlueprint blueprint) {
        GenerationResult result = new GenerationResult(
                "empty",
                0, 0,
                grid.getBlockCount(),
                0, List.of(),
                0, List.of(),
                0, List.of(),
                null, null,
                0L, 0L, null
        );
        return new GenerationArtifacts(result, grid, blueprint);
    }

    private static int resolveBlock(String blockId) {
        return switch (blockId) {
            case "Rock_Stone_Brick" -> 11;
            case "Furniture_Crude_Chest_Small" -> 12;
            default -> -1;
        };
    }
}
