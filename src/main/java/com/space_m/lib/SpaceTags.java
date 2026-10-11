package com.space_m.lib;

import com.space_m.SpaceMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tags des Addons, die Original-Schnittstellen ersetzen (SPACE_PORT.md 2c, "Tags statt Interfaces"):
 * HBM-Modernized muss die Space-Klassen nicht kennen, Inhalte melden sich per Datapack an.
 */
public final class SpaceTags {
    private SpaceTags() {}

    /** Ersetzt {@code IBlockSealable}: Bloecke, die einen Atmosphaeren-Raum abdichten. */
    public static final TagKey<Block> SEALABLE = TagKey.create(Registries.BLOCK, SpaceMod.id("sealable"));

    /** R2-A: Bloecke mit voller Box, die trotzdem nie abdichten (Original {@code IBlockSealable.isSealed = false}). */
    public static final TagKey<Block> UNSEALABLE = TagKey.create(Registries.BLOCK, SpaceMod.id("unsealable"));

    /** R2-A: zusaetzliche Pflanzen, die Atemluft brauchen (Original {@code IPlantable}; Vanilla-Pflanzen per Klasse). */
    public static final TagKey<Block> BREATHING_PLANTS = TagKey.create(Registries.BLOCK, SpaceMod.id("breathing_plants"));

    /** R2-A: Brust-Einsaetze mit Korrosionsschutz (Original {@code ItemModInsert.corrosionProtection}, insert_cmb). */
    public static final TagKey<Item> CORROSION_PROTECTION = TagKey.create(Registries.ITEM, SpaceMod.id("corrosion_protection"));

    /** Ersetzt {@code ArmorFSB.canSeal}: Ruestungsteile, die zusammen einen dichten Anzug bilden. */
    public static final TagKey<Item> SEALED_ARMOR = TagKey.create(Registries.ITEM, SpaceMod.id("sealed_armor"));

    /** Atemluft-Versorgung (Original {@code ItemModOxy}, {@code oxy_plss}, {@code oxy_pinwheel} ...). */
    public static final TagKey<Item> OXYGEN_SUPPLY = TagKey.create(Registries.ITEM, SpaceMod.id("oxygen_supply"));

    /** Ersetzt {@code ISuffocationImmune}: Entities, die ohne Atmosphaere nicht ersticken. */
    public static final TagKey<EntityType<?>> SUFFOCATION_IMMUNE = TagKey.create(Registries.ENTITY_TYPE, SpaceMod.id("suffocation_immune"));
}
