package com.duntale.dungeongen.util;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves string block-type keys (e.g. {@code "Rock_Stone_Brick"}) to the
 * integer block IDs used by the Hytale chunk system.
 *
 * <p>Resolved IDs are cached so that repeated lookups during world assembly
 * are O(1).</p>
 *
 * @since 1.0.0
 */
public class BlockResolver {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private final Map<String, Integer> cache = new ConcurrentHashMap<>();

    /**
     * Resolve a block-type key to its integer ID.
     *
     * @param blockTypeKey the asset key (e.g. {@code "Rock_Stone_Brick"})
     * @return the integer block ID, or {@code -1} if the key is unknown
     */
    public int resolve(@Nonnull String blockTypeKey) {
        return cache.computeIfAbsent(blockTypeKey, key -> {
            BlockTypeAssetMap<String, BlockType> assetMap = BlockType.getAssetMap();
            int index = assetMap.getIndex(key);
            if (index == Integer.MIN_VALUE) {
                LOGGER.atWarning().log("[DungeonGen] Unknown block type key: %s", key);
                return -1;
            }
            return index;
        });
    }

    /**
     * Resolve a block-type key to the {@link BlockType} asset object.
     *
     * @param blockTypeKey the asset key
     * @return the block type object, or {@code null} if unknown
     */
    @Nullable
    public BlockType resolveType(@Nonnull String blockTypeKey) {
        BlockType type = BlockType.fromString(blockTypeKey);
        if (type == null || type.isUnknown()) {
            LOGGER.atWarning().log("[DungeonGen] Unknown BlockType: %s", blockTypeKey);
            return null;
        }
        return type;
    }

    /**
     * Check whether a block-type key is valid (exists in the asset map).
     *
     * @param blockTypeKey the key to validate
     * @return {@code true} if the key resolves to a valid block ID
     */
    public boolean isValid(@Nonnull String blockTypeKey) {
        return resolve(blockTypeKey) != -1;
    }

    /**
     * Clear the ID cache.
     */
    public void clearCache() {
        cache.clear();
    }
}
