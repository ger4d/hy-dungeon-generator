package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Codec-backed light configuration embedded in a {@link DungeonThemeConfig}.
 * Defines the block IDs for wall, ceiling, floor, and accent lights.
 *
 * @since 1.3.0
 */
public class LightEntry {

    public static final BuilderCodec<LightEntry> CODEC;

    protected String wallLight = "Wood_Torch_Wall";
    @Nullable protected String ceilingLight = null;
    @Nullable protected String floorLightBlock = null;
    protected boolean floorLightTall = false;
    @Nullable protected String accentFloorLight = null;

    public LightEntry() {}

    static {
        CODEC = BuilderCodec.builder(LightEntry.class, LightEntry::new)
            .append(new KeyedCodec<>("WallLight", Codec.STRING),
                (e, v) -> e.wallLight = v, e -> e.wallLight).add()
            .append(new KeyedCodec<>("CeilingLight", Codec.STRING),
                (e, v) -> e.ceilingLight = v, e -> e.ceilingLight).add()
            .append(new KeyedCodec<>("FloorLightBlock", Codec.STRING),
                (e, v) -> e.floorLightBlock = v, e -> e.floorLightBlock).add()
            .append(new KeyedCodec<>("FloorLightTall", Codec.BOOLEAN),
                (e, v) -> e.floorLightTall = v, e -> e.floorLightTall).add()
            .append(new KeyedCodec<>("AccentFloorLight", Codec.STRING),
                (e, v) -> e.accentFloorLight = v, e -> e.accentFloorLight).add()
            .build();
    }

    // ============================================
    // Getters
    // ============================================

    @Nonnull public String getWallLight() { return wallLight; }
    @Nullable public String getCeilingLight() { return ceilingLight; }
    @Nullable public String getFloorLightBlock() { return floorLightBlock; }
    public boolean isFloorLightTall() { return floorLightTall; }
    @Nullable public String getAccentFloorLight() { return accentFloorLight; }
}
