package com.duntale.dungeongen.rest;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemArmor;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemQuality;
import com.hypixel.hytale.server.core.asset.type.item.config.ItemWeapon;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.ResistanceModifier;
import com.hypixel.hytale.server.core.modules.entitystats.asset.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.RootInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.DamageEntityInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.combat.DamageCalculator;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderInfo;
import com.hypixel.hytale.server.npc.asset.builder.BuilderManager;
import com.hypixel.hytale.server.npc.asset.builder.BuilderSupport;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.role.Role;
import com.hypixel.hytale.server.npc.role.builders.BuilderRole;
import com.hypixel.hytale.server.npc.role.support.RoleStats;
import com.hypixel.hytale.server.npc.util.expression.ExecutionContext;
import com.hypixel.hytale.server.npc.util.expression.Scope;
import com.hypixel.hytale.server.spawning.ISpawnableWithModel;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.Predicate;

/**
 * Builds read-only JSON datasets from the live Hytale runtime asset APIs.
 */
public class BalanceAssetExportService {

    private static final int SCHEMA_VERSION = 3;
    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 60;
    private static final double SELL_RATIO = 0.80;
    private static final long MIN_BUY_PRICE = 25L;
    private static final double SCORE_TO_GOLD_SCALE = 10.0;
    private static final double SCORE_TO_GOLD_EXPONENT = 1.4;
    private static final double ARMOR_RESIST_SCORE_WEIGHT = 3.0;
    private static final double ARMOR_HEALTH_SCORE_WEIGHT = 0.9;
    private static final double QUALITY_FALLBACK_EXPONENT = 0.5;
    private static final float MIDPOINT = 30.0f;
    private static final float STEEPNESS = 0.12f;
    private static final float WEAPON_K = 6.0f;
    private static final float ARMOR_K = 4.0f;
    private static final float MAX_ARMOR_DR = 0.65f;

    @Nullable
    private static final Field DAMAGE_CALCULATOR_FIELD;

    static {
        Field damageCalculatorField = null;
        try {
            damageCalculatorField = DamageEntityInteraction.class.getDeclaredField("damageCalculator");
            damageCalculatorField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException ignored) {
            // Graceful degradation: weapon damage extraction falls back to zero.
        }
        DAMAGE_CALCULATOR_FIELD = damageCalculatorField;
    }

    /**
     * Builds a compact summary of the exported balancing dataset.
     *
     * @return dataset summary JSON
     */
    @Nonnull
    public JsonObject buildSummary() {
        List<Map.Entry<String, Item>> allItems = sortedItems(item -> true);
        List<Map.Entry<String, Item>> weapons = sortedItems(item -> item.getWeapon() != null);
        List<Map.Entry<String, Item>> armor = sortedItems(item -> item.getArmor() != null);
        List<BuilderInfo> npcBuilders = sortedNpcBuilders();

        long validNpcCount = npcBuilders.stream().filter(BuilderInfo::isValid).count();
        long deprecatedNpcCount = npcBuilders.stream().filter(info -> info.getBuilder().isDeprecated()).count();
        long roleBuilderCount = npcBuilders.stream().filter(info -> info.getBuilder() instanceof BuilderRole).count();
        long merchantEligibleWeapons = weapons.stream().filter(entry -> isMerchantEligibleWeapon(entry.getKey(), entry.getValue())).count();
        long merchantEligibleArmor = armor.stream().filter(entry -> isMerchantEligibleArmor(entry.getKey(), entry.getValue())).count();

        JsonObject summary = new JsonObject();
        summary.addProperty("schemaVersion", SCHEMA_VERSION);
        summary.addProperty("generatedAt", Instant.now().toString());
        summary.addProperty("curveLevelMin", MIN_LEVEL);
        summary.addProperty("curveLevelMax", MAX_LEVEL);
        summary.addProperty("items", allItems.size());
        summary.addProperty("weapons", weapons.size());
        summary.addProperty("armor", armor.size());
        summary.addProperty("merchantEligibleWeapons", merchantEligibleWeapons);
        summary.addProperty("merchantEligibleArmor", merchantEligibleArmor);
        summary.addProperty("npcs", npcBuilders.size());
        summary.addProperty("validNpcs", validNpcCount);
        summary.addProperty("deprecatedNpcs", deprecatedNpcCount);
        summary.addProperty("roleBackedNpcs", roleBuilderCount);
        return summary;
    }

    /**
     * Builds the full balancing dataset.
     *
     * @return combined dataset JSON
     */
    @Nonnull
    public JsonObject buildBalanceDataset() {
        JsonObject dataset = new JsonObject();
        dataset.addProperty("schemaVersion", SCHEMA_VERSION);
        dataset.addProperty("generatedAt", Instant.now().toString());
        dataset.addProperty("curveLevelMin", MIN_LEVEL);
        dataset.addProperty("curveLevelMax", MAX_LEVEL);
        dataset.add("summary", buildSummary());
        dataset.add("weapons", buildWeapons());
        dataset.add("armor", buildArmor());
        dataset.add("npcs", buildNpcs());
        return dataset;
    }

    /**
     * Builds the exported weapon list.
     *
     * @return weapon array JSON
     */
    @Nonnull
    public JsonArray buildWeapons() {
        JsonArray weapons = new JsonArray();
        for (Map.Entry<String, Item> entry : sortedItems(item -> item.getWeapon() != null)) {
            weapons.add(toWeaponJson(entry.getKey(), entry.getValue()));
        }
        return weapons;
    }

    /**
     * Builds the exported armor list.
     *
     * @return armor array JSON
     */
    @Nonnull
    public JsonArray buildArmor() {
        JsonArray armor = new JsonArray();
        for (Map.Entry<String, Item> entry : sortedItems(item -> item.getArmor() != null)) {
            armor.add(toArmorJson(entry.getKey(), entry.getValue()));
        }
        return armor;
    }

    /**
     * Builds the exported NPC builder list.
     *
     * @return NPC builder array JSON
     */
    @Nonnull
    public JsonArray buildNpcs() {
        JsonArray npcs = new JsonArray();
        for (BuilderInfo builderInfo : sortedNpcBuilders()) {
            npcs.add(toNpcJson(builderInfo));
        }
        return npcs;
    }

    @Nonnull
    private static List<Map.Entry<String, Item>> sortedItems(@Nonnull Predicate<Item> filter) {
        return Item.getAssetMap()
                .getAssetMap()
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() != null)
                .filter(entry -> filter.test(entry.getValue()))
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, Item> entry) -> entry.getValue().getItemLevel())
                        .thenComparingInt(entry -> entry.getValue().getQualityIndex())
                        .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Nonnull
    private static List<BuilderInfo> sortedNpcBuilders() {
        BuilderManager builderManager = NPCPlugin.get().getBuilderManager();
        return builderManager.getAllBuilders()
                .values()
                .stream()
                .filter(BuilderInfo::isValidated)
                .filter(info -> info.getBuilder().isSpawnable())
                .sorted(Comparator.comparing(BuilderInfo::getKeyName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Nonnull
    private static JsonObject toWeaponJson(@Nonnull String id, @Nonnull Item item) {
        ItemQuality quality = resolveQuality(item);
        String qualityId = quality != null ? quality.getId() : null;
        int baseLevel = resolvePricingLevel(item.getItemLevel());
        String family = inferWeaponFamily(id);
        float baseDamage = extractWeaponDamage(item);
        List<String> exclusionReasons = buildWeaponExclusionReasons(id, qualityId);

        JsonObject json = baseItemJson(id, item, quality, exclusionReasons);
        ItemWeapon weapon = item.getWeapon();
        addString(json, "weaponFamily", family);
        json.addProperty("pricingBaseLevel", baseLevel);
        json.addProperty("baseDamage", round4(baseDamage));
        json.add("merchantScoreByLevel", buildDoubleCurve(level -> computeWeaponScore(item.getItemLevel(), qualityId, family, baseDamage, level)));
        json.add("scaledDamageByLevel", buildDoubleCurve(level -> computeScaledWeaponDamage(baseDamage, level)));
        json.add("merchantBuyPriceByLevel", buildLongCurve(level -> computeWeaponBuyPrice(item.getItemLevel(), qualityId, family, baseDamage, level)));
        json.add("merchantSellPriceByLevel", buildLongCurve(level -> computeWeaponSellPrice(item.getItemLevel(), qualityId, family, baseDamage, level)));
        json.addProperty("merchantBaseBuyPrice", computeWeaponBuyPrice(item.getItemLevel(), qualityId, family, baseDamage, baseLevel));
        json.addProperty("merchantBaseSellPrice", computeWeaponSellPrice(item.getItemLevel(), qualityId, family, baseDamage, baseLevel));
        json.addProperty("weaponStatModifierGroups", sizeOf(weapon.getStatModifiers()));
        json.addProperty("weaponStatModifierCount", countStaticModifiers(weapon.getStatModifiers()));
        json.addProperty("entityStatsToClearCount", lengthOf(weapon.getEntityStatsToClear()));
        return json;
    }

    @Nonnull
    private static JsonObject toArmorJson(@Nonnull String id, @Nonnull Item item) {
        ItemQuality quality = resolveQuality(item);
        String qualityId = quality != null ? quality.getId() : null;
        int baseLevel = resolvePricingLevel(item.getItemLevel());

        ItemArmor armor = item.getArmor();
        float physResist = extractResistance(armor, "Physical");
        float projResist = extractResistance(armor, "Projectile");
        int healthBonus = extractHealthBonus(armor);
        String slot = armor.getArmorSlot() != null ? armor.getArmorSlot().name() : null;
        List<String> exclusionReasons = buildArmorExclusionReasons(id, qualityId);

        JsonObject json = baseItemJson(id, item, quality, exclusionReasons);
        addString(json, "armorSlot", slot);
        json.addProperty("pricingBaseLevel", baseLevel);
        json.addProperty("physResist", round4(physResist));
        json.addProperty("projResist", round4(projResist));
        json.addProperty("healthBonus", healthBonus);
        json.add("armorScoreByLevel", buildDoubleCurve(level -> computeArmorScore(item.getItemLevel(), qualityId, slot, physResist, projResist, healthBonus, level)));
        json.add("avgResistPercentByLevel", buildDoubleCurve(level -> computeAverageResistPercent(physResist, projResist, level)));
        json.add("physDrByLevel", buildDoubleCurve(level -> computeArmorDr(physResist, level)));
        json.add("projDrByLevel", buildDoubleCurve(level -> computeArmorDr(projResist, level)));
        json.add("merchantBuyPriceByLevel", buildLongCurve(level -> computeArmorBuyPrice(item.getItemLevel(), qualityId, slot, physResist, projResist, healthBonus, level)));
        json.add("merchantSellPriceByLevel", buildLongCurve(level -> computeArmorSellPrice(item.getItemLevel(), qualityId, slot, physResist, projResist, healthBonus, level)));
        json.addProperty("merchantBaseBuyPrice", computeArmorBuyPrice(item.getItemLevel(), qualityId, slot, physResist, projResist, healthBonus, baseLevel));
        json.addProperty("merchantBaseSellPrice", computeArmorSellPrice(item.getItemLevel(), qualityId, slot, physResist, projResist, healthBonus, baseLevel));
        if (armor.getArmorSlot() != null) {
            json.addProperty("armorSlot", armor.getArmorSlot().name());
        }
        json.addProperty("baseDamageResistance", armor.getBaseDamageResistance());
        json.addProperty("armorStatModifierGroups", sizeOf(armor.getStatModifiers()));
        json.addProperty("armorStatModifierCount", countStaticModifiers(armor.getStatModifiers()));
        json.addProperty("damageResistanceGroups", sizeOf(armor.getDamageResistanceValues()));
        json.addProperty("damageEnhancementGroups", sizeOf(armor.getDamageEnhancementValues()));
        json.addProperty("damageClassEnhancementGroups", sizeOf(armor.getDamageClassEnhancement()));
        json.addProperty("regeneratingGroups", sizeOf(armor.getRegeneratingValues()));
        return json;
    }

    @Nonnull
    private static JsonObject baseItemJson(@Nonnull String id,
                                           @Nonnull Item item,
                                           @Nullable ItemQuality quality,
                                           @Nonnull List<String> exclusionReasons) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("itemLevel", item.getItemLevel());
        json.addProperty("qualityIndex", item.getQualityIndex());
        json.addProperty("curveLevelMin", MIN_LEVEL);
        json.addProperty("curveLevelMax", MAX_LEVEL);
        json.addProperty("maxStack", item.getMaxStack());
        json.addProperty("maxDurability", item.getMaxDurability());
        json.addProperty("icon", item.getIcon());
        json.addProperty("assetPack", Item.getAssetMap().getAssetPack(id));
        addPath(json, "path", Item.getAssetMap().getPath(id));
        if (quality != null) {
            addString(json, "qualityId", quality.getId());
            json.addProperty("qualityValue", quality.getQualityValue());
        }
        json.addProperty("merchantEligible", exclusionReasons.isEmpty());
        json.add("merchantExclusionReasons", toJsonArray(exclusionReasons));
        json.add("categories", toJsonArray(item.getCategories()));
        addString(json, "subCategory", item.getSubCategory());
        return json;
    }

    @Nonnull
    private static JsonObject toNpcJson(@Nonnull BuilderInfo builderInfo) {
        Builder<?> builder = builderInfo.getBuilder();

        JsonObject json = new JsonObject();
        json.addProperty("index", builderInfo.getIndex());
        json.addProperty("keyName", builderInfo.getKeyName());
        json.addProperty("valid", builderInfo.isValid());
        json.addProperty("deprecated", builder.isDeprecated());
        json.addProperty("spawnable", builder.isSpawnable());
        json.addProperty("builderClass", builder.getClass().getSimpleName());
        json.addProperty("builderCategory", builder.category().getSimpleName());
        addString(json, "typeName", builder.getTypeName());
        addString(json, "descriptorState", builder.getBuilderDescriptorState() == null ? null : builder.getBuilderDescriptorState().name());
        addPath(json, "path", builderInfo.getPath());

        appendSpawnableMetadata(json, builder);
        appendRoleMetadata(json, builderInfo, builder);
        return json;
    }

    private static void appendSpawnableMetadata(@Nonnull JsonObject json, @Nonnull Builder<?> builder) {
        if (!(builder instanceof ISpawnableWithModel spawnable)) {
            return;
        }

        try {
            ExecutionContext infoContext = new ExecutionContext();
            infoContext.setScope(spawnable.createExecutionScope());
            Scope modifierScope = spawnable.createModifierScope(infoContext);

            addString(json, "memoriesCategory", spawnable.getMemoriesCategory(infoContext, modifierScope));
            addString(json, "memoriesNameOverride", spawnable.getMemoriesNameOverride(infoContext, modifierScope));
            addString(json, "nameTranslationKey", spawnable.getNameTranslationKey(infoContext, modifierScope));
            json.addProperty("memory", spawnable.isMemory(infoContext, modifierScope));
            json.addProperty("breathesInAir", spawnable.breathesInAir(infoContext, modifierScope));
            json.addProperty("breathesInWater", spawnable.breathesInWater(infoContext, modifierScope));
        } catch (RuntimeException ignored) {
            json.addProperty("spawnableMetadataError", true);
        }

        try {
            ExecutionContext modelContext = new ExecutionContext(builder.getBuilderParameters().createScope());
            addString(json, "spawnModel", spawnable.getSpawnModelName(modelContext, spawnable.createModifierScope(modelContext)));
        } catch (RuntimeException ignored) {
            json.addProperty("spawnModelError", true);
        }
    }

    private static void appendRoleMetadata(@Nonnull JsonObject json,
                                           @Nonnull BuilderInfo builderInfo,
                                           @Nonnull Builder<?> builder) {
        if (builder.category() != Role.class) {
            return;
        }

        if (builder instanceof BuilderRole roleBuilder) {
            try {
                ExecutionContext context = new ExecutionContext(builder.getBuilderParameters().createScope());
                BuilderSupport support = new BuilderSupport(
                        NPCPlugin.get().getBuilderManager(),
                        new NPCEntity(),
                        EntityStore.REGISTRY.newHolder(),
                        context,
                        roleBuilder,
                        new RoleStats());

                json.addProperty("maxHealth", roleBuilder.getMaxHealth(support));
                addString(json, "roleNameTranslationKey", roleBuilder.getNameTranslationKey(support));
                addString(json, "appearance", roleBuilder.getAppearance(support));
                addString(json, "dropListId", roleBuilder.getDropListId(support));
                json.add("hotbarItems", toJsonArray(roleBuilder.getHotbarItems(support)));
                json.add("offHandItems", toJsonArray(roleBuilder.getOffHandItems(support)));
                json.add("armorItems", toJsonArray(roleBuilder.getArmor()));
            } catch (RuntimeException ignored) {
                json.addProperty("roleMetadataError", true);
            }
        }

        Integer resolvedBaseHp = resolveRoleBaseHp(builderInfo);
        if (resolvedBaseHp != null) {
            json.addProperty("maxHealth", resolvedBaseHp);
            addString(json, "tier", classifyNpcTier(resolvedBaseHp));
        }
    }

    @Nullable
    private static Integer resolveRoleBaseHp(@Nonnull BuilderInfo builderInfo) {
        try {
            NPCPlugin npcPlugin = NPCPlugin.get();
            int roleIndex = npcPlugin.getIndex(builderInfo.getKeyName());
            if (roleIndex < 0) {
                return null;
            }

            BuilderInfo preparedBuilderInfo = npcPlugin.prepareRoleBuilderInfo(roleIndex);
            @SuppressWarnings("unchecked")
            Builder<Role> typedRoleBuilder = (Builder<Role>) preparedBuilderInfo.getBuilder();
            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            BuilderSupport builderSupport = new BuilderSupport(
                    npcPlugin.getBuilderManager(),
                    new NPCEntity(),
                    holder,
                    new ExecutionContext(),
                    typedRoleBuilder,
                    new RoleStats());
            Role role = NPCPlugin.buildRole(typedRoleBuilder, preparedBuilderInfo, builderSupport, roleIndex);
            return role != null ? role.getInitialMaxHealth() : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @Nonnull
    private static String classifyNpcTier(int baseHp) {
        if (baseHp <= 36) {
            return "Fodder";
        }
        if (baseHp <= 74) {
            return "Standard";
        }
        if (baseHp <= 150) {
            return "Tough";
        }
        if (baseHp <= 350) {
            return "Elite";
        }
        return "Boss";
    }

    @Nullable
    private static ItemQuality resolveQuality(@Nonnull Item item) {
        int qualityIndex = item.getQualityIndex();
        if (qualityIndex <= 0) {
            return null;
        }
        return ItemQuality.getAssetMap().getAsset(qualityIndex);
    }

    @Nonnull
    private static List<String> buildWeaponExclusionReasons(@Nonnull String id, @Nullable String qualityId) {
        List<String> reasons = new ArrayList<>();
        if (isExcludedQuality(qualityId)) {
            reasons.add("quality:" + qualityId);
        }
        if (isNpcItem(id)) {
            reasons.add("npcOnly");
        }
        if (isAmmoLike(id)) {
            reasons.add("ammoOrProjectile");
        }
        return reasons;
    }

    @Nonnull
    private static List<String> buildArmorExclusionReasons(@Nonnull String id, @Nullable String qualityId) {
        List<String> reasons = new ArrayList<>();
        if (isExcludedQuality(qualityId)) {
            reasons.add("quality:" + qualityId);
        }
        if (isNpcItem(id)) {
            reasons.add("npcOnly");
        }
        return reasons;
    }

    private static boolean isMerchantEligibleWeapon(@Nonnull String id, @Nonnull Item item) {
        ItemQuality quality = resolveQuality(item);
        String qualityId = quality != null ? quality.getId() : null;
        return buildWeaponExclusionReasons(id, qualityId).isEmpty();
    }

    private static boolean isMerchantEligibleArmor(@Nonnull String id, @Nonnull Item item) {
        ItemQuality quality = resolveQuality(item);
        String qualityId = quality != null ? quality.getId() : null;
        return buildArmorExclusionReasons(id, qualityId).isEmpty();
    }

    private static boolean isExcludedQuality(@Nullable String qualityId) {
        return "Template".equals(qualityId)
                || "Developer".equals(qualityId)
                || "Technical".equals(qualityId)
                || "Debug".equals(qualityId)
                || "QA".equals(qualityId);
    }

    private static boolean isNpcItem(@Nonnull String id) {
        String lowered = id.toLowerCase();
        return lowered.endsWith("_npc") || lowered.contains("_npc_");
    }

    private static boolean isAmmoLike(@Nonnull String id) {
        String lowered = id.toLowerCase();
        return lowered.contains("arrow") || lowered.contains("ammo");
    }

    @Nullable
    private static String inferWeaponFamily(@Nonnull String id) {
        String[] parts = id.split("_");
        if (parts.length >= 2 && "Weapon".equals(parts[0])) {
            return parts[1];
        }
        return parts.length >= 1 ? parts[0] : null;
    }

    private static float extractWeaponDamage(@Nonnull Item item) {
        if (DAMAGE_CALCULATOR_FIELD == null) {
            return 0f;
        }

        Map<String, String> interactionVars = item.getInteractionVars();
        if (interactionVars == null || interactionVars.isEmpty()) {
            return 0f;
        }

        List<Float> damages = new ArrayList<>();
        for (String rootId : interactionVars.values()) {
            RootInteraction root = RootInteraction.getAssetMap().getAsset(rootId);
            if (root == null || root.getInteractionIds() == null) {
                continue;
            }

            for (String childId : root.getInteractionIds()) {
                Interaction interaction = Interaction.getAssetMap().getAsset(childId);
                if (!(interaction instanceof DamageEntityInteraction damageInteraction)) {
                    continue;
                }

                try {
                    DamageCalculator calculator = (DamageCalculator) DAMAGE_CALCULATOR_FIELD.get(damageInteraction);
                    if (calculator == null) {
                        continue;
                    }

                    Object2FloatMap<DamageCause> damageMap = calculator.calculateDamage(1.0);
                    if (damageMap == null || damageMap.isEmpty()) {
                        continue;
                    }

                    float total = 0f;
                    for (Object2FloatMap.Entry<DamageCause> entry : damageMap.object2FloatEntrySet()) {
                        if (entry.getFloatValue() > 0f) {
                            total += entry.getFloatValue();
                        }
                    }
                    if (total > 0f) {
                        damages.add(total);
                    }
                } catch (IllegalAccessException ignored) {
                    // Graceful degradation: this weapon will export with baseDamage=0.
                }
            }
        }

        if (damages.isEmpty()) {
            return 0f;
        }

        float totalDamage = 0f;
        for (float damage : damages) {
            totalDamage += damage;
        }
        return totalDamage / damages.size();
    }

    private static float extractResistance(@Nonnull ItemArmor armor, @Nonnull String causeId) {
        Map<DamageCause, ResistanceModifier[]> resistances = armor.getDamageResistanceValues();
        DamageCause cause = DamageCause.getAssetMap().getAsset(causeId);
        if (resistances == null || cause == null) {
            return 0f;
        }

        ResistanceModifier[] modifiers = resistances.get(cause);
        if (modifiers == null) {
            return 0f;
        }

        float total = 0f;
        for (ResistanceModifier modifier : modifiers) {
            total += (float) modifier.getAmount();
        }
        return total;
    }

    private static int extractHealthBonus(@Nonnull ItemArmor armor) {
        Int2ObjectMap<StaticModifier[]> statModifiers = armor.getStatModifiers();
        if (statModifiers == null) {
            return 0;
        }

        int healthIndex = EntityStatType.getAssetMap().getIndex("Health");
        if (healthIndex < 0) {
            return 0;
        }

        StaticModifier[] healthModifiers = statModifiers.get(healthIndex);
        if (healthModifiers == null) {
            return 0;
        }

        int total = 0;
        for (StaticModifier modifier : healthModifiers) {
            total += (int) modifier.getAmount();
        }
        return total;
    }

    private static int resolvePricingLevel(int requestedLevel) {
        int boundedLevel = requestedLevel > 0 ? requestedLevel : MIN_LEVEL;
        return Math.clamp(boundedLevel, MIN_LEVEL, MAX_LEVEL);
    }

    private static long computeWeaponBuyPrice(int itemLevel,
                                              @Nullable String qualityId,
                                              @Nullable String family,
                                              float baseDamage,
                                              int requestedLevel) {
        double score = computeWeaponScore(itemLevel, qualityId, family, baseDamage, requestedLevel);
        return computeBuyPrice(score);
    }

    private static long computeWeaponSellPrice(int itemLevel,
                                               @Nullable String qualityId,
                                               @Nullable String family,
                                               float baseDamage,
                                               int requestedLevel) {
        return (long) Math.floor(computeWeaponBuyPrice(itemLevel, qualityId, family, baseDamage, requestedLevel) * SELL_RATIO);
    }

    private static long computeArmorBuyPrice(int itemLevel,
                                             @Nullable String qualityId,
                                             @Nullable String slot,
                                             float physResist,
                                             float projResist,
                                             int healthBonus,
                                             int requestedLevel) {
        double score = computeArmorScore(itemLevel, qualityId, slot, physResist, projResist, healthBonus, requestedLevel);
        return computeBuyPrice(score);
    }

    private static long computeArmorSellPrice(int itemLevel,
                                              @Nullable String qualityId,
                                              @Nullable String slot,
                                              float physResist,
                                              float projResist,
                                              int healthBonus,
                                              int requestedLevel) {
        return (long) Math.floor(computeArmorBuyPrice(itemLevel, qualityId, slot, physResist, projResist, healthBonus, requestedLevel) * SELL_RATIO);
    }

    private static long computeBuyPrice(double score) {
        double boundedScore = Math.max(1.0, score);
        long computed = Math.round(Math.pow(boundedScore, SCORE_TO_GOLD_EXPONENT) * SCORE_TO_GOLD_SCALE);
        return Math.max(MIN_BUY_PRICE, computed);
    }

    private static double computeWeaponScore(int itemLevel,
                                             @Nullable String qualityId,
                                             @Nullable String family,
                                             float baseDamage,
                                             int requestedLevel) {
        int level = resolvePricingLevel(requestedLevel > 0 ? requestedLevel : itemLevel);
        if (baseDamage > 0f) {
            return baseDamage * weaponMult(level);
        }

        return computeFallbackTierScore(itemLevel, qualityId) * zeroStatWeaponFamilyMultiplier(family);
    }

    private static double computeArmorScore(int itemLevel,
                                            @Nullable String qualityId,
                                            @Nullable String slot,
                                            float physResist,
                                            float projResist,
                                            int healthBonus,
                                            int requestedLevel) {
        int level = resolvePricingLevel(requestedLevel > 0 ? requestedLevel : itemLevel);
        double physDr = physResist > 0f ? computeArmorDr(physResist, level) : 0.0;
        double projDr = projResist > 0f ? computeArmorDr(projResist, level) : 0.0;

        int resistSources = 0;
        if (physDr > 0.0) {
            resistSources++;
        }
        if (projDr > 0.0) {
            resistSources++;
        }

        double avgResistPercent = resistSources > 0 ? ((physDr + projDr) / resistSources) * 100.0 : 0.0;
        double healthScore = Math.max(0, healthBonus) * ARMOR_HEALTH_SCORE_WEIGHT;
        double score = avgResistPercent * ARMOR_RESIST_SCORE_WEIGHT + healthScore;
        if (score > 0.0) {
            return score;
        }

        return computeFallbackTierScore(itemLevel, qualityId) * armorFallbackSlotMultiplier(slot);
    }

    private static double computeScaledWeaponDamage(float baseDamage, int requestedLevel) {
        if (baseDamage <= 0f) {
            return 0.0;
        }
        return baseDamage * weaponMult(requestedLevel);
    }

    private static double computeAverageResistPercent(float physResist, float projResist, int requestedLevel) {
        double physDr = physResist > 0f ? computeArmorDr(physResist, requestedLevel) : 0.0;
        double projDr = projResist > 0f ? computeArmorDr(projResist, requestedLevel) : 0.0;

        int resistSources = 0;
        if (physDr > 0.0) {
            resistSources++;
        }
        if (projDr > 0.0) {
            resistSources++;
        }

        if (resistSources == 0) {
            return 0.0;
        }
        return ((physDr + projDr) / resistSources) * 100.0;
    }

    private static double computeArmorDr(float baseResist, int requestedLevel) {
        int level = resolvePricingLevel(requestedLevel);
        float resistMultiplier = Math.max(1.0f + (ARMOR_K - 1.0f) * sigmoid(level), 1.0f);
        float damageReduction = baseResist * resistMultiplier;
        return Math.min(damageReduction, MAX_ARMOR_DR);
    }

    private static double weaponMult(int requestedLevel) {
        int level = resolvePricingLevel(requestedLevel);
        return 1.0f + WEAPON_K * sigmoid(level);
    }

    private static float sigmoid(int requestedLevel) {
        int level = Math.clamp(requestedLevel, MIN_LEVEL, MAX_LEVEL);
        float raw = rawSigmoid(level);
        float minRaw = rawSigmoid(MIN_LEVEL);
        float maxRaw = rawSigmoid(MAX_LEVEL);
        float denominator = maxRaw - minRaw;
        if (denominator <= 0f) {
            return 0f;
        }
        return Math.max(0f, Math.min((raw - minRaw) / denominator, 1f));
    }

    private static float rawSigmoid(int level) {
        return 1.0f / (1.0f + (float) Math.exp(-STEEPNESS * (level - MIDPOINT)));
    }

    private static double computeFallbackTierScore(int itemLevel, @Nullable String qualityId) {
        int boundedLevel = Math.max(1, itemLevel);
        double qualityFactor = Math.pow(qualityCoefficient(qualityId), QUALITY_FALLBACK_EXPONENT);
        return boundedLevel * qualityFactor;
    }

    private static double qualityCoefficient(@Nullable String qualityId) {
        if (qualityId == null) {
            return 1.0;
        }
        return switch (qualityId) {
            case "Common" -> 1.0;
            case "Uncommon" -> 1.5;
            case "Rare" -> 2.5;
            case "Epic" -> 5.0;
            case "Legendary" -> 15.0;
            default -> 1.0;
        };
    }

    private static double zeroStatWeaponFamilyMultiplier(@Nullable String family) {
        if (family == null) {
            return 1.0;
        }
        return switch (family) {
            case "Bow", "Shortbow", "Crossbow", "Handgun", "Rifle" -> 1.20;
            case "Staff", "Wand", "Spellbook" -> 1.15;
            case "Shield" -> 1.05;
            case "Torch", "Fire" -> 0.90;
            default -> 1.0;
        };
    }

    private static double armorFallbackSlotMultiplier(@Nullable String slot) {
        if (slot == null) {
            return 1.0;
        }
        return switch (slot) {
            case "Chest" -> 1.0;
            case "Legs" -> 0.85;
            case "Head" -> 0.70;
            case "Hands" -> 0.55;
            case "Feet" -> 0.55;
            default -> 1.0;
        };
    }

    @Nonnull
    private static JsonArray buildDoubleCurve(@Nonnull IntToDoubleFunction curveFunction) {
        JsonArray curve = new JsonArray();
        for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {
            curve.add(round4(curveFunction.applyAsDouble(level)));
        }
        return curve;
    }

    @Nonnull
    private static JsonArray buildLongCurve(@Nonnull IntToLongFunction curveFunction) {
        JsonArray curve = new JsonArray();
        for (int level = MIN_LEVEL; level <= MAX_LEVEL; level++) {
            curve.add(curveFunction.applyAsLong(level));
        }
        return curve;
    }

    @Nonnull
    private static JsonArray toJsonArray(@Nullable String[] values) {
        JsonArray array = new JsonArray();
        if (values == null) {
            return array;
        }

        for (String value : values) {
            if (value != null) {
                array.add(value);
            }
        }
        return array;
    }

    @Nonnull
    private static JsonArray toJsonArray(@Nonnull List<String> values) {
        JsonArray array = new JsonArray();
        for (String value : values) {
            if (value != null) {
                array.add(value);
            }
        }
        return array;
    }

    private static void addString(@Nonnull JsonObject json, @Nonnull String key, @Nullable String value) {
        if (value != null && !value.isBlank()) {
            json.addProperty(key, value);
        }
    }

    private static void addPath(@Nonnull JsonObject json, @Nonnull String key, @Nullable Path path) {
        if (path != null) {
            json.addProperty(key, path.toString());
        }
    }

    private static int sizeOf(@Nullable Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    private static int sizeOf(@Nullable Int2ObjectMap<?> map) {
        return map == null ? 0 : map.size();
    }

    private static int lengthOf(@Nullable int[] values) {
        return values == null ? 0 : values.length;
    }

    private static int countStaticModifiers(@Nullable Int2ObjectMap<StaticModifier[]> modifiers) {
        if (modifiers == null) {
            return 0;
        }

        int count = 0;
        for (StaticModifier[] group : modifiers.values()) {
            count += group == null ? 0 : group.length;
        }
        return count;
    }

    private static double round4(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}