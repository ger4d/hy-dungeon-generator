package com.duntale.dungeongen.generator;

import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.ThemeConfig;
import com.duntale.dungeongen.config.Vec3i;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GenerationArtifactsTest {

    @Test
    void generateArtifactsReturnsPreviewOutputsAndPreservesGenerateSummary() {
        GenerationOrchestrator orchestrator = new GenerationOrchestrator(null);
        try {
            DungeonConfig config = testConfig();

            GenerationArtifacts artifacts = orchestrator.generateArtifacts(config).join();
            GenerationResult generated = orchestrator.generate(config).join();

            assertNotNull(artifacts.result());
            assertNotNull(artifacts.blockGrid());
            assertNotNull(artifacts.blueprint());
            assertEquals(generated.seed(), artifacts.result().seed());
            assertEquals(generated.rooms(), artifacts.result().rooms());
            assertEquals(generated.corridors(), artifacts.result().corridors());
            assertEquals(generated.totalBlocks(), artifacts.result().totalBlocks());
            assertEquals(generated.spawners(), artifacts.result().spawners());
            assertEquals(generated.merchants(), artifacts.result().merchants());
            assertEquals(generated.chests(), artifacts.result().chests());
            assertEquals(artifacts.result().totalBlocks(), artifacts.blueprint().getTotalBlocks());
        } finally {
            orchestrator.shutdown();
        }
    }

    private static DungeonConfig testConfig() {
        LayoutConfig layout = new LayoutConfig(
                32, 32, 8,
                0.45, 4, 10, 8,
                "rectangular", 0.1,
                2, 0.2, 0.1, false, 0.3,
                0.05, 0.02, 0.0, 0.02, false, 0.01,
                0.0,
                "edge", 0.6,
                0.2, 3, true, 0.05,
                0.0,
                false, true, true,
                0.4
        );
        return new DungeonConfig(
                "artifacts-contract",
                null,
                "default",
                Vec3i.ZERO,
                layout,
                new ThemeConfig("crypt", 0.2, 0.0, 0.0),
                PacingConfig.defaults(),
                false,
                1
        );
    }
}