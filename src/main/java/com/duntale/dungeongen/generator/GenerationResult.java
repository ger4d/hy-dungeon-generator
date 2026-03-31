package com.duntale.dungeongen.generator;

import com.duntale.dungeongen.config.Vec3i;
import com.duntale.dungeongen.model.MerchantDefinition;
import com.duntale.dungeongen.model.SpawnerDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Immutable result returned by {@link GenerationOrchestrator} after a
 * dungeon generation completes. Contains summary statistics about the
 * generated dungeon.
 *
 * @param seed                the seed used for generation
 * @param rooms               number of rooms generated
 * @param corridors           number of corridors generated
 * @param totalBlocks         total voxel blocks in the blueprint (0 until voxel carver is implemented)
 * @param spawners            number of spawner definitions placed
 * @param spawnerDefinitions  the spawner definitions for creating ECS spawner entities
 * @param merchants           number of merchant definitions placed
 * @param merchantDefinitions the merchant definitions for creating merchant NPC entities
 * @param entrancePosition    dungeon-origin-relative standing position for the entrance room,
 *                            or {@code null} if no entrance room was assigned
 * @param exitPosition        dungeon-origin-relative standing position for the boss/exit room,
 *                            or {@code null} if no boss/exit room was assigned
 * @param generationTimeMs    wall-clock time for the generation pipeline
 * @param assemblyTimeMs      wall-clock time for world assembly (0 if assemble=false)
 * @param assemblyError       {@code null} if assembly succeeded or was not requested; error message otherwise
 * @since 1.0.0
 */
public record GenerationResult(
    @Nonnull String seed,
    int rooms,
    int corridors,
    int totalBlocks,
    int spawners,
    @Nonnull List<SpawnerDefinition> spawnerDefinitions,
    int merchants,
    @Nonnull List<MerchantDefinition> merchantDefinitions,
    @Nullable Vec3i entrancePosition,
    @Nullable Vec3i exitPosition,
    long generationTimeMs,
    long assemblyTimeMs,
    @Nullable String assemblyError
) {

    /**
     * Serialize this result to a JSON string suitable for the REST response.
     *
     * @return JSON representation
     */
    @Nonnull
    public String toJson() {
        String status = assemblyError != null ? "error" : "ok";
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"status\": \"").append(status).append("\",");
        sb.append("\"seed\": \"").append(seed).append("\",");
        sb.append("\"entrancePosition\": ");
        appendVec3iJson(sb, entrancePosition);
        sb.append(",");
        sb.append("\"exitPosition\": ");
        appendVec3iJson(sb, exitPosition);
        sb.append(",");
        sb.append("\"stats\": {");
        sb.append("\"rooms\": ").append(rooms).append(",");
        sb.append("\"corridors\": ").append(corridors).append(",");
        sb.append("\"totalBlocks\": ").append(totalBlocks).append(",");
        sb.append("\"spawners\": ").append(spawners).append(",");
        sb.append("\"merchants\": ").append(merchants).append(",");
        sb.append("\"generationTimeMs\": ").append(generationTimeMs).append(",");
        sb.append("\"assemblyTimeMs\": ").append(assemblyTimeMs);
        sb.append("}");
        if (assemblyError != null) {
            sb.append(",\"assemblyError\": \"");
            sb.append(assemblyError.replace("\"", "\\\""));
            sb.append("\"");
        }
        sb.append("}");
        return sb.toString();
    }

    private static void appendVec3iJson(@Nonnull StringBuilder sb, @Nullable Vec3i position) {
        if (position == null) {
            sb.append("null");
            return;
        }

        sb.append("{");
        sb.append("\"x\": ").append(position.x()).append(",");
        sb.append("\"y\": ").append(position.y()).append(",");
        sb.append("\"z\": ").append(position.z());
        sb.append("}");
    }
}
