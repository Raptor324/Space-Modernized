package com.space_m.platform;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Schwerkraft je Himmelskoerper (SPACE_PORT.md 2d).
 *
 * <p>1.20.1-Forge: Attribut {@code ForgeMod.ENTITY_GRAVITY} (Basis 0.08). 1.21.1: Vanilla {@code Attributes.GRAVITY}
 * (Basis 0.08). Beide Versionen: ein transienter Modifikator {@code MULTIPLY_TOTAL}/{@code ADD_MULTIPLIED_TOTAL}
 * mit fester Kennung, Wert {@code faktor - 1}. Fallschaden (1.20.1 ohne Attribut) regelt der
 * {@code LivingFallEvent}-Handler beider Loader ({@code com.space_m.gravity.CelestialGravity}).
 */
public final class GravityHooks {
    /** Vanilla-Grundwert der Schwerkraft (Bloecke/Tick^2). */
    public static final double BASE = 0.08D;
    /** Normschwere des Originals (Kerbin = Erde), m/s^2 - {@code AstronomyUtil}/{@code CelestialBody.getSurfaceGravity}. */
    public static final double STANDARD_GRAVITY = 9.81D;
    /** Normschwere in NTM-Einheiten ({@code AstronomyUtil.STANDARD_GRAVITY} = 1.6, d.h. 0.08 Bloecke/Tick^2 = g/20). */
    public static final double NTM_STANDARD_GRAVITY = 1.6D;

    private static final UUID MODIFIER_UUID = UUID.fromString("4f1c3d0e-5a1b-4e8e-9a51-73a1c0b9d8e2");
    private static final String MODIFIER_NAME = "space_m:celestial_gravity";

    private GravityHooks() {}

    @Nullable
    private static AttributeInstance attribute(LivingEntity entity) {
        //? if forge {
        return entity.getAttribute(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
        //?} else {
        /*return entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.GRAVITY);
        *///?}
    }

    /** Aktuelle Schwerkraft (Attributwert) der Entity; {@link #BASE}, wenn sie kein Schwerkraft-Attribut hat. */
    public static double get(LivingEntity entity) {
        AttributeInstance inst = attribute(entity);
        return inst == null ? BASE : inst.getValue();
    }

    /** Setzt die Schwerkraft auf {@code factor * Normal} (1 = Erde/Kerbin, 0 = schwerelos). */
    public static void setFactor(LivingEntity entity, double factor) {
        AttributeInstance inst = attribute(entity);
        if (inst == null) return;
        removeModifier(inst);
        if (factor == 1.0D) return;
        //? if forge {
        inst.addTransientModifier(new AttributeModifier(MODIFIER_UUID, MODIFIER_NAME, factor - 1.0D,
                AttributeModifier.Operation.MULTIPLY_TOTAL));
        //?} else {
        /*inst.addTransientModifier(new AttributeModifier(net.minecraft.resources.ResourceLocation.parse(MODIFIER_NAME),
                factor - 1.0D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        *///?}
    }

    /**
     * Wie {@link #setFactor}, aendert das Attribut aber nur, wenn sich der Faktor wirklich aendert (kein Neu-Anlegen
     * je Tick -> keine Attribut-Sync-Pakete je Tick). Fuer den Schwerkraft-Tick aller Lebewesen.
     */
    public static void applyFactor(LivingEntity entity, double factor) {
        AttributeInstance inst = attribute(entity);
        if (inst == null) return;
        AttributeModifier current = getModifier(inst);
        if (factor == 1.0D) {
            if (current != null) removeModifier(inst);
            return;
        }
        //? if forge {
        if (current != null && current.getAmount() == factor - 1.0D) return;
        //?} else {
        /*if (current != null && current.amount() == factor - 1.0D) return;
        *///?}
        setFactor(entity, factor);
    }

    /** Schwerkraft in NTM-Einheiten (Original {@code CelestialBody.getGravity}, 1.6 = Normal) als Attribut anwenden. */
    public static void applyNtmGravity(LivingEntity entity, double ntmGravity) {
        applyFactor(entity, ntmGravity / NTM_STANDARD_GRAVITY);
    }

    @Nullable
    private static AttributeModifier getModifier(AttributeInstance inst) {
        //? if forge {
        return inst.getModifier(MODIFIER_UUID);
        //?} else {
        /*return inst.getModifier(net.minecraft.resources.ResourceLocation.parse(MODIFIER_NAME));
        *///?}
    }

    /** Schwerkraft eines Koerpers in m/s^2 (Original-Einheit) anwenden. */
    public static void setSurfaceGravity(LivingEntity entity, double gravityMs2) {
        setFactor(entity, gravityMs2 / STANDARD_GRAVITY);
    }

    /** Entfernt den Space-Modifikator (zurueck auf Normalschwerkraft). */
    public static void clear(LivingEntity entity) {
        AttributeInstance inst = attribute(entity);
        if (inst != null) removeModifier(inst);
    }

    private static void removeModifier(AttributeInstance inst) {
        //? if forge {
        inst.removeModifier(MODIFIER_UUID);
        //?} else {
        /*inst.removeModifier(net.minecraft.resources.ResourceLocation.parse(MODIFIER_NAME));
        *///?}
    }
}
