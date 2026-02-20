package com.duntale.dungeongen.config;

import com.duntale.dungeongen.util.JsonParser;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Layout generation parameters controlling the dungeon's spatial extent,
 * room placement, corridor style, feature density, and enemy distribution.
 *
 * @param width              X grid extent (default: 64)
 * @param depth              Z grid extent (default: 64)
 * @param height             Y extent / wall height (default: 12)
 * @param roomDensity        0–1, scales effective room count (default: 0.45)
 * @param minRoomSize        minimum room dimension in cells (default: 4)
 * @param maxRoomSize        maximum room dimension in cells (default: 14)
 * @param maxRooms           maximum rooms per generation (default: 20)
 * @param roomShape          shape template: rectangular/circular/lshaped/cross/irregular (default: "rectangular")
 * @param irregularity       0–1, room edge nibbling amount (default: 0.2)
 * @param corridorWidth      corridor width in cells, 1–4 (default: 2)
 * @param branchChance       probability of corridor branching (default: 0.3)
 * @param loopChance         probability of loop corridors (default: 0.15)
 * @param windingCorridors   use winding random-walk corridors (default: false)
 * @param windingFactor      how much corridors wind, 0–1 (default: 0.5)
 * @param pillarFrequency    frequency of pillars in rooms (default: 0.1)
 * @param waterFrequency     frequency of water pools (default: 0.05)
 * @param lavaFrequency      frequency of lava pools (default: 0.0)
 * @param trapDensity        density of traps (default: 0.08)
 * @param secretWallChance   chance of secret walls (default: 0.05)
 * @param entrancePlacement  entrance placement strategy: edge/corner/center/random (default: "edge")
 * @param exitDistance        min normalised distance entrance→exit (default: 0.7)
 * @param enemyDensity       enemy density scaler (default: 0.4)
 * @param maxEnemiesPerRoom  max enemies per room (default: 5)
 * @param bossRoom           whether to designate a boss room (default: true)
 * @param ambushChance       probability of ambush event in a room (default: 0.1)
 * @param erosion            architectural erosion amount (default: 0.1)
 * @param removeCeiling      if true, ceiling blocks are removed for top-down visibility (default: false)
 * @param flatFloor          if true, all rooms share the same Y level (default: true)
 * @param solidFill          if true, exterior space is filled solid (default); if false, only room/corridor shells are kept (default: true)
 * @param complexity         global generation complexity scaler (default: 0.5)
 * @since 1.1.0
 */
public record LayoutConfig(
    // Size
    int width,
    int depth,
    int height,
    // Rooms
    double roomDensity,
    int minRoomSize,
    int maxRoomSize,
    int maxRooms,
    String roomShape,
    double irregularity,
    // Corridors
    int corridorWidth,
    double branchChance,
    double loopChance,
    boolean windingCorridors,
    double windingFactor,
    // Features
    double pillarFrequency,
    double waterFrequency,
    double lavaFrequency,
    double trapDensity,
    double secretWallChance,
    // Entrance / Exit
    String entrancePlacement,
    double exitDistance,
    // Enemies
    double enemyDensity,
    int maxEnemiesPerRoom,
    boolean bossRoom,
    double ambushChance,
    // Architecture
    double erosion,
    // View
    boolean removeCeiling,
    boolean flatFloor,
    boolean solidFill,
    // Generation
    double complexity
) {

    /**
     * Parse a {@code LayoutConfig} from a JSON map.
     *
     * @param json the JSON map with layout keys
     * @return the parsed config, falling back to defaults for missing keys
     */
    @Nonnull
    public static LayoutConfig fromJson(@Nonnull Map<String, Object> json) {
        LayoutConfig d = defaults();
        return new LayoutConfig(
            json.containsKey("width")             ? JsonParser.toInt(json.get("width"))             : d.width(),
            json.containsKey("depth")             ? JsonParser.toInt(json.get("depth"))             : d.depth(),
            json.containsKey("height")            ? JsonParser.toInt(json.get("height"))            : d.height(),
            json.containsKey("roomDensity")       ? JsonParser.toDouble(json.get("roomDensity"))    : d.roomDensity(),
            json.containsKey("minRoomSize")       ? JsonParser.toInt(json.get("minRoomSize"))       : d.minRoomSize(),
            json.containsKey("maxRoomSize")       ? JsonParser.toInt(json.get("maxRoomSize"))       : d.maxRoomSize(),
            json.containsKey("maxRooms")          ? JsonParser.toInt(json.get("maxRooms"))          : d.maxRooms(),
            json.containsKey("roomShape")         ? JsonParser.toStringOrNull(json.get("roomShape")): d.roomShape(),
            json.containsKey("irregularity")      ? JsonParser.toDouble(json.get("irregularity"))   : d.irregularity(),
            json.containsKey("corridorWidth")     ? JsonParser.toInt(json.get("corridorWidth"))     : d.corridorWidth(),
            json.containsKey("branchChance")      ? JsonParser.toDouble(json.get("branchChance"))   : d.branchChance(),
            json.containsKey("loopChance")        ? JsonParser.toDouble(json.get("loopChance"))     : d.loopChance(),
            json.containsKey("windingCorridors")  ? JsonParser.toBoolean(json.get("windingCorridors")) : d.windingCorridors(),
            json.containsKey("windingFactor")     ? JsonParser.toDouble(json.get("windingFactor"))  : d.windingFactor(),
            json.containsKey("pillarFrequency")   ? JsonParser.toDouble(json.get("pillarFrequency")): d.pillarFrequency(),
            json.containsKey("waterFrequency")    ? JsonParser.toDouble(json.get("waterFrequency")) : d.waterFrequency(),
            json.containsKey("lavaFrequency")     ? JsonParser.toDouble(json.get("lavaFrequency"))  : d.lavaFrequency(),
            json.containsKey("trapDensity")       ? JsonParser.toDouble(json.get("trapDensity"))    : d.trapDensity(),
            json.containsKey("secretWallChance")  ? JsonParser.toDouble(json.get("secretWallChance")): d.secretWallChance(),
            json.containsKey("entrancePlacement") ? JsonParser.toStringOrNull(json.get("entrancePlacement")) : d.entrancePlacement(),
            json.containsKey("exitDistance")      ? JsonParser.toDouble(json.get("exitDistance"))   : d.exitDistance(),
            json.containsKey("enemyDensity")      ? JsonParser.toDouble(json.get("enemyDensity"))   : d.enemyDensity(),
            json.containsKey("maxEnemiesPerRoom") ? JsonParser.toInt(json.get("maxEnemiesPerRoom")) : d.maxEnemiesPerRoom(),
            json.containsKey("bossRoom")          ? JsonParser.toBoolean(json.get("bossRoom"))      : d.bossRoom(),
            json.containsKey("ambushChance")      ? JsonParser.toDouble(json.get("ambushChance"))   : d.ambushChance(),
            json.containsKey("erosion")           ? JsonParser.toDouble(json.get("erosion"))        : d.erosion(),
            json.containsKey("removeCeiling")     ? JsonParser.toBoolean(json.get("removeCeiling")) : d.removeCeiling(),
            json.containsKey("flatFloor")         ? JsonParser.toBoolean(json.get("flatFloor"))     : d.flatFloor(),
            json.containsKey("solidFill")         ? JsonParser.toBoolean(json.get("solidFill"))     : d.solidFill(),
            json.containsKey("complexity")        ? JsonParser.toDouble(json.get("complexity"))     : d.complexity()
        );
    }

    /**
     * Return a {@code LayoutConfig} with sensible defaults.
     *
     * @return default layout config
     */
    @Nonnull
    public static LayoutConfig defaults() {
        return new LayoutConfig(
            64, 64, 12,                             // size
            0.45, 4, 14, 20,                        // rooms
            "rectangular", 0.2,                      // shape
            2, 0.3, 0.15, false, 0.5,               // corridors
            0.1, 0.05, 0.0, 0.08, 0.05,             // features
            "edge", 0.7,                             // entrance/exit
            0.4, 5, true, 0.1,                       // enemies
            0.1,                                     // architecture
            false, true, true,                       // view
            0.5                                      // generation
        );
    }
}
