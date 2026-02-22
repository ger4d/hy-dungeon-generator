package com.duntale.dungeongen.config.asset;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Codec-backed prop rule entry embedded in a {@link DungeonThemeConfig}.
 * Defines a single decorative prop with placement rules.
 *
 * @since 1.3.0
 */
public class PropRuleEntry {

    public static final BuilderCodec<PropRuleEntry> CODEC;
    public static final ArrayCodec<PropRuleEntry> ARRAY_CODEC;

    protected String blockId = "";
    protected String placement = "FLOOR";
    protected double spawnChance = 0.1;
    protected int maxPerRoom = 1;
    protected String[] allowedRoomTypes = new String[0];
    protected int yOffset = 0;

    public PropRuleEntry() {}

    static {
        CODEC = BuilderCodec.builder(PropRuleEntry.class, PropRuleEntry::new)
            .append(new KeyedCodec<>("BlockId", Codec.STRING),
                (e, v) -> e.blockId = v, e -> e.blockId).add()
            .append(new KeyedCodec<>("Placement", Codec.STRING),
                (e, v) -> e.placement = v, e -> e.placement).add()
            .append(new KeyedCodec<>("SpawnChance", Codec.DOUBLE),
                (e, v) -> e.spawnChance = v, e -> e.spawnChance).add()
            .append(new KeyedCodec<>("MaxPerRoom", Codec.INTEGER),
                (e, v) -> e.maxPerRoom = v, e -> e.maxPerRoom).add()
            .append(new KeyedCodec<>("AllowedRoomTypes", Codec.STRING_ARRAY),
                (e, v) -> e.allowedRoomTypes = v, e -> e.allowedRoomTypes).add()
            .append(new KeyedCodec<>("YOffset", Codec.INTEGER),
                (e, v) -> e.yOffset = v, e -> e.yOffset).add()
            .build();
        ARRAY_CODEC = new ArrayCodec<>(CODEC, PropRuleEntry[]::new);
    }

    // ============================================
    // Getters
    // ============================================

    @Nonnull public String getBlockId() { return blockId; }
    @Nonnull public String getPlacement() { return placement; }
    public double getSpawnChance() { return spawnChance; }
    public int getMaxPerRoom() { return maxPerRoom; }
    @Nonnull public String[] getAllowedRoomTypes() { return allowedRoomTypes; }
    public int getYOffset() { return yOffset; }
}
