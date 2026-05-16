package com.duntale.dungeongen.assembly;

import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.ThemeConfig;
import com.duntale.dungeongen.generator.GenerationArtifacts;
import com.duntale.dungeongen.generator.GenerationOrchestrator;
import com.duntale.dungeongen.generator.GenerationResult;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;

/**
 * Facade over {@link GenerationOrchestrator} for the standalone generate workflow.
 *
 * <p>Separates the UI layer from the orchestrator by exposing two intent-specific
 * entry points: preview artifact generation and full world assembly. Both force
 * the appropriate {@code assemble} flag so callers cannot accidentally mix
 * preview and assembly modes.</p>
 *
 * @since 1.0.3
 */
public class DungeonAssemblyService {

    private final GenerationOrchestrator orchestrator;

    /**
     * Create a new assembly service backed by the given orchestrator.
     *
     * @param orchestrator the underlying generation orchestrator
     */
    public DungeonAssemblyService(@Nonnull GenerationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    /**
     * Generate preview-safe artifacts (block grid + blueprint) without placing
     * blocks in the world. Intended for in-page preview rendering.
     *
     * @param config the dungeon configuration (assemble flag is ignored)
     * @return future resolving to the generation artifacts with assembly disabled
     */
    @Nonnull
    public CompletableFuture<GenerationArtifacts> generatePreviewArtifacts(@Nonnull DungeonConfig config) {
        return orchestrator.generateArtifacts(withAssemble(config, false));
    }

    /**
     * Generate a dungeon and assemble it into the configured target world.
     *
     * @param config the dungeon configuration; {@code config.assemble()} must be {@code true}
     * @return future resolving to the generation result after assembly completes
     * @throws IllegalArgumentException if {@code config.assemble()} is {@code false}
     */
    @Nonnull
    public CompletableFuture<GenerationResult> generateAndAssemble(@Nonnull DungeonConfig config) {
        if (!config.assemble()) {
            throw new IllegalArgumentException("generateAndAssemble requires config.assemble() == true");
        }
        return orchestrator.generate(config);
    }

    /**
     * Returns the orchestrator backing this service.
     *
     * @return the orchestrator
     */
    @Nonnull
    public GenerationOrchestrator getOrchestrator() {
        return orchestrator;
    }

    @Nonnull
    private static DungeonConfig withAssemble(@Nonnull DungeonConfig config, boolean assemble) {
        if (config.assemble() == assemble) {
            return config;
        }
        LayoutConfig layout = config.layout();
        ThemeConfig theme = config.theme();
        PacingConfig pacing = config.pacing();
        return new DungeonConfig(
                config.seed(),
                config.preset(),
                config.worldName(),
                config.origin(),
                layout,
                theme,
                pacing,
                assemble,
                config.floorLevel()
        );
    }
}
