package com.duntale.dungeongen.generator;

import com.duntale.dungeongen.generator.voxel.BlockGrid;
import com.duntale.dungeongen.model.DungeonBlueprint;

import javax.annotation.Nonnull;

/**
 * Preview-safe outputs from a dungeon generation run.
 *
 * @param result    generation summary and runtime definitions
 * @param blockGrid final block and fluid voxel grid
 * @param blueprint final dungeon blueprint metadata
 * @since 1.6.0
 */
public record GenerationArtifacts(
        @Nonnull GenerationResult result,
        @Nonnull BlockGrid blockGrid,
        @Nonnull DungeonBlueprint blueprint
) {}