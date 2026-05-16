package com.duntale.dungeongen.command;

import com.duntale.dungeongen.config.DungeonConfig;
import com.duntale.dungeongen.config.LayoutConfig;
import com.duntale.dungeongen.config.PacingConfig;
import com.duntale.dungeongen.config.ThemeConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DungeonGenerateConfigMapperTest {

    @Test
    void emptyEventDataProducesFullDefaults() {
        DungeonGeneratePage.GenerateEventData empty = new DungeonGeneratePage.GenerateEventData();

        DungeonConfig config = DungeonGenerateConfigMapper.toConfig(empty);
        DungeonConfig defaults = DungeonConfig.withDefaults();
        LayoutConfig defaultLayout = LayoutConfig.defaults();
        ThemeConfig defaultTheme = ThemeConfig.defaults();
        PacingConfig defaultPacing = PacingConfig.defaults();

        assertEquals("0", config.seed());
        assertEquals(defaults.worldName(), config.worldName());
        assertEquals(defaults.origin(), config.origin());
        assertEquals(defaults.floorLevel(), config.floorLevel());
        assertTrue(config.assemble());
        assertEquals(defaultLayout, config.layout());
        assertEquals(defaultTheme, config.theme());
        assertEquals(defaultPacing, config.pacing());
    }

    @Test
    void allLayoutFieldsAreMapped() {
        DungeonGeneratePage.GenerateEventData d = new DungeonGeneratePage.GenerateEventData();
        d.width = 80;
        d.depth = 96;
        d.height = 20;
        d.roomDensity = 0.7F;
        d.minRoomSize = 5;
        d.maxRoomSize = 18;
        d.maxRooms = 24;
        d.roomShape = "circular";
        d.irregularity = 0.4F;
        d.corridorWidth = 3;
        d.branchChance = 0.5F;
        d.loopChance = 0.25F;
        d.windingCorridors = true;
        d.windingFactor = 0.6F;
        d.pillarFreq = 0.2F;
        d.waterFreq = 0.1F;
        d.lavaFreq = 0.05F;
        d.trapDensity = 0.15F;
        d.floorTraps = true;
        d.secretWallChance = 0.07F;
        d.merchantSpawnChance = 0.42F;
        d.entrancePlacement = "corner";
        d.exitDistance = 0.8F;
        d.enemyDensity = 0.55F;
        d.maxEnemiesPerRoom = 7;
        d.bossRoom = false;
        d.ambushChance = 0.12F;
        d.erosion = 0.2F;
        d.removeCeiling = true;
        d.flatFloor = false;
        d.solidFill = false;
        d.complexity = 0.85F;

        LayoutConfig layout = DungeonGenerateConfigMapper.toConfig(d).layout();
        assertEquals(80, layout.width());
        assertEquals(96, layout.depth());
        assertEquals(20, layout.height());
        assertEquals(0.7, layout.roomDensity(), 1e-5);
        assertEquals(5, layout.minRoomSize());
        assertEquals(18, layout.maxRoomSize());
        assertEquals(24, layout.maxRooms());
        assertEquals("circular", layout.roomShape());
        assertEquals(0.4, layout.irregularity(), 1e-5);
        assertEquals(3, layout.corridorWidth());
        assertEquals(0.5, layout.branchChance(), 1e-5);
        assertEquals(0.25, layout.loopChance(), 1e-5);
        assertTrue(layout.windingCorridors());
        assertEquals(0.6, layout.windingFactor(), 1e-5);
        assertEquals(0.2, layout.pillarFrequency(), 1e-5);
        assertEquals(0.1, layout.waterFrequency(), 1e-5);
        assertEquals(0.05, layout.lavaFrequency(), 1e-5);
        assertEquals(0.15, layout.trapDensity(), 1e-5);
        assertTrue(layout.floorTraps());
        assertEquals(0.07, layout.secretWallChance(), 1e-5);
        assertEquals(0.42, layout.merchantSpawnChance(), 1e-5);
        assertEquals("corner", layout.entrancePlacement());
        assertEquals(0.8, layout.exitDistance(), 1e-5);
        assertEquals(0.55, layout.enemyDensity(), 1e-5);
        assertEquals(7, layout.maxEnemiesPerRoom());
        assertFalse(layout.bossRoom());
        assertEquals(0.12, layout.ambushChance(), 1e-5);
        assertEquals(0.2, layout.erosion(), 1e-5);
        assertTrue(layout.removeCeiling());
        assertFalse(layout.flatFloor());
        assertFalse(layout.solidFill());
        assertEquals(0.85, layout.complexity(), 1e-5);
    }

    @Test
    void themeAndPacingFieldsAreMapped() {
        DungeonGeneratePage.GenerateEventData d = new DungeonGeneratePage.GenerateEventData();
        d.palette = "Volcanic";
        d.decayFactor = 0.4F;
        d.overgrowthFactor = 0.15F;
        d.floodingFactor = 0.05F;
        d.breatheRoomFreq = 0.42F;
        d.difficultyRamp = 0.66F;

        DungeonConfig config = DungeonGenerateConfigMapper.toConfig(d);
        assertEquals("volcanic", config.theme().palette());
        assertEquals(0.4, config.theme().decayFactor(), 1e-5);
        assertEquals(0.15, config.theme().overgrowthFactor(), 1e-5);
        assertEquals(0.05, config.theme().floodingFactor(), 1e-5);
        assertEquals(0.42, config.pacing().breatheRoomFrequency(), 1e-5);
        assertEquals(0.66, config.pacing().difficultyRamp(), 1e-5);
    }

    @Test
    void paletteIsNormalizedToLowercaseAndTempleDarkIsPreserved() {
        assertEquals("crypt", paletteFor("Crypt"));
        assertEquals("temple_dark", paletteFor("Temple_Dark"));
        assertEquals("temple_dark", paletteFor("temple_dark"));
        assertEquals("hive", paletteFor("HIVE"));
        assertEquals("mushroom", paletteFor("Mushroom"));
    }

    @Test
    void unknownPaletteFallsBackToDefault() {
        assertEquals(DungeonGenerateConfigMapper.defaultPalette(), paletteFor("not_a_real_palette"));
        assertEquals(DungeonGenerateConfigMapper.defaultPalette(), paletteFor(""));
        assertEquals(DungeonGenerateConfigMapper.defaultPalette(), paletteFor(null));
    }

    @Test
    void mapperAlwaysAssembles() {
        DungeonGeneratePage.GenerateEventData d = new DungeonGeneratePage.GenerateEventData();
        assertTrue(DungeonGenerateConfigMapper.toConfig(d).assemble());
    }

    @Test
    void blankSeedDefaultsToZero() {
        DungeonGeneratePage.GenerateEventData d = new DungeonGeneratePage.GenerateEventData();
        d.seed = "  ";
        assertEquals("0", DungeonGenerateConfigMapper.toConfig(d).seed());

        d.seed = null;
        assertEquals("0", DungeonGenerateConfigMapper.toConfig(d).seed());

        d.seed = "abc-42";
        assertEquals("abc-42", DungeonGenerateConfigMapper.toConfig(d).seed());
    }

    @Test
    void floorLevelDefaultsAndOverrides() {
        DungeonGeneratePage.GenerateEventData d = new DungeonGeneratePage.GenerateEventData();
        assertEquals(DungeonConfig.withDefaults().floorLevel(),
                DungeonGenerateConfigMapper.toConfig(d).floorLevel());

        d.floorLevel = 12;
        assertEquals(12, DungeonGenerateConfigMapper.toConfig(d).floorLevel());
    }

    private static String paletteFor(String raw) {
        DungeonGeneratePage.GenerateEventData d = new DungeonGeneratePage.GenerateEventData();
        d.palette = raw;
        return DungeonGenerateConfigMapper.toConfig(d).theme().palette();
    }
}
