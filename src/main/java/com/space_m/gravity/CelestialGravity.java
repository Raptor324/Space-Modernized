package com.space_m.gravity;

import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.mixin.LivingEntityAccessor;
import com.space_m.platform.GravityHooks;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Chicken;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.EventPriority;
//?} elif neoforge {
/*import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
*///?}

/**
 * Schwerkraft je Himmelskoerper, 1:1 NTM Space {@code ModEventHandler}:
 * <ul>
 *   <li>{@code onLivingUpdate} (Client und Server): Lebewesen ausser {@code EntityFlying} und fliegenden Spielern
 *       fallen mit {@link CelestialBody#getGravity} (NTM-Einheiten, 1.6 = 0.08 Bloecke/Tick^2). Nichts tun bei
 *       1.5..1.7, im Wasser und in den ersten 20 Ticks; Mindestwert 0.2; Hochschwerkraft (&gt; 1.7) nur beim Fallen,
 *       damit man noch einen Block hochspringen kann; Huehner nie. Schwerelos (0, Orbit): Spieler steigt/sinkt mit
 *       Springen/Schleichen um 0.01, Huhn steht, Daempfung 0.91.</li>
 *   <li>{@code onEntityFall}: Fallhoehe bei g &lt; 0.3 = 0, bei g &lt; 1.5 mal {@code g / 1.6} (vor HBM-Ruestungen).</li>
 *   <li>{@code onWorldTick} (Orbit-Dimension): Items schweben ({@code motionY = 0.04}, horizontal * 0.9).</li>
 * </ul>
 * Projektile, Fahrzeuge und Items auf Planeten bekommen im Original keine Koerperschwerkraft (nur Lebewesen und
 * Orbit-Items) - hier ebenso.
 *
 * <p>Umsetzung: Das Original ersetzt in {@code motionY} die Vanilla-Schwerkraft des Vorticks durch {@code g/20}. Hier
 * traegt der Schwerkraft-Modifikator ({@link GravityHooks}, 1.20.1 {@code ForgeMod.ENTITY_GRAVITY}, 1.21.1
 * {@code Attributes.GRAVITY}) genau {@code g/20} - gleiche Fallkurve, Sprunghoehe und Fallgeschwindigkeit, ohne
 * Vortick-Versatz, und Vanilla-Nebenwirkungen (Sanfter Fall, Elytra, Fluessigkeiten) bleiben korrekt. Nur die
 * Schwerelosigkeit (Daempfung/Steuerung) braucht zusaetzlich den Bewegungseingriff des Originals.
 *
 * <p>Client-Vorhersage: der Tick laeuft auf beiden Seiten. Der Client setzt den Modifikator nur fuer selbst
 * simulierte Entities (eigener Spieler, gesteuertes Reittier), vor seiner eigenen {@code travel}-Rechnung - kein
 * Ruckeln. Der Server setzt ihn fuer alle Lebewesen (Mobs rechnet er selbst; beim Spieler bleibt der Wert stabil,
 * Attribut-Sync nur bei echten Wechseln wie Dimension/Wasser, da {@link GravityHooks#applyFactor} unveraenderte
 * Werte nicht neu anlegt).
 */
public final class CelestialGravity {

    private static boolean registered;

    private CelestialGravity() {}

    public static void register() {
        if (registered) return;
        registered = true;

        //? if forge {
        MinecraftForge.EVENT_BUS.addListener(CelestialGravity::onLivingTick);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, CelestialGravity::onFall);
        //?} elif neoforge {
        /*NeoForge.EVENT_BUS.addListener(CelestialGravity::onEntityTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, CelestialGravity::onFall);
        *///?}

        TickEvent.SERVER_LEVEL_PRE.register(CelestialGravity::floatOrbitItems);
    }

    //? if forge {
    private static void onLivingTick(LivingEvent.LivingTickEvent event) {
        tick(event.getEntity());
    }
    //?} elif neoforge {
    /*private static void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof LivingEntity living) tick(living);
    }
    *///?}

    private static void onFall(LivingFallEvent event) {
        event.setDistance(fallDistance(event.getEntity(), event.getDistance()));
    }

    /** true, wenn diese Seite die Bewegung rechnet (Server: alles ausser Spielern; Client: eigener Spieler/Reittier). */
    public static boolean simulatesMovement(LivingEntity entity) {
        if (!entity.level().isClientSide) return !(entity instanceof Player);
        return (entity instanceof Player player && player.isLocalPlayer()) || entity.isControlledByLocalInstance();
    }

    /** Original {@code ModEventHandler.onLivingUpdate}, Schwerkraft-Teil. */
    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide && !simulatesMovement(entity)) return;
        tick(entity, CelestialBody.getGravity(entity));
    }

    /** Wie {@link #tick(LivingEntity)} mit vorgegebener Schwerkraft (NTM-Einheiten; Tests ohne Koerper-Dimension). */
    public static void tick(LivingEntity entity, float gravity) {
        Level level = entity.level();
        boolean simulated = simulatesMovement(entity);
        if (level.isClientSide && !simulated) return;

        boolean isFlying = entity instanceof Player player && player.getAbilities().flying;
        if (isFlying || entity instanceof FlyingMob) {
            GravityHooks.applyFactor(entity, 1.0D);
            return;
        }

        if (gravity == 0) {
            GravityHooks.applyFactor(entity, 0.0D);
            if (simulated && !entity.isNoGravity()) floatWeightless(entity);
        } else if (!entity.isInWater() && entity.tickCount > 20 && (gravity < 1.5F || gravity > 1.7F)) {
            // If gravity is basically the same as normal, do nothing
            // Also do nothing in water, or if we've been alive less than a second (so we don't glitch into the ground)

            // Minimum gravity to prevent floating bug
            if (gravity < 0.2F) gravity = 0.2F;

            // On high gravity planets, only apply falling speed when descending, so we can still jump up single blocks.
            // Server-Spieler rechnet der Client: dort stabil den Fallwert halten (kein Attribut-Sync je Sprung).
            boolean descending = !simulated || entity.getDeltaMovement().y < 0;
            if ((gravity < 1.5F || descending) && !(entity instanceof Chicken)) {
                GravityHooks.applyNtmGravity(entity, gravity);
            } else {
                GravityHooks.applyFactor(entity, 1.0D);
            }
        } else {
            GravityHooks.applyFactor(entity, 1.0D);
        }
    }

    /**
     * Schwerelosigkeit (Original {@code gravity == 0}): Vanilla-Daempfung 0.98 zuruecknehmen, Spieler steuert mit
     * Schleichen/Springen, Huhn steht still, Daempfung 0.91. Die Schwerkraft selbst ist ueber das Attribut 0.
     */
    private static void floatWeightless(LivingEntity entity) {
        Vec3 m = entity.getDeltaMovement();
        double motionY = m.y / 0.98F;

        if (entity instanceof Player player) {
            if (player.isShiftKeyDown()) motionY -= 0.01F;
            if (((LivingEntityAccessor) player).space_m$isJumping()) motionY += 0.01F;
        } else if (entity instanceof Chicken) {
            motionY = 0;
        }

        motionY *= 0.91F;
        entity.setDeltaMovement(m.x, motionY, m.z);
    }

    /** Original {@code ModEventHandler.onEntityFall}: Fallhoehe auf leichten Koerpern verringern. */
    public static float fallDistance(LivingEntity entity, float distance) {
        float gravity = CelestialBody.getGravity(entity);

        // Reduce fall damage on low gravity bodies
        if (gravity < 0.3F) {
            return 0;
        } else if (gravity < 1.5F) {
            return distance * (gravity / AstronomyUtil.STANDARD_GRAVITY);
        }
        return distance;
    }

    /** Original {@code ModEventHandler.onWorldTick} (Phase START, Orbit-Dimension): Items schweben. */
    public static void floatOrbitItems(ServerLevel level) {
        if (!CelestialBody.inOrbit(level)) return;
        for (Entity e : level.getAllEntities()) floatItem(e);
    }

    /** Ein Item schweben lassen (auch Client-Vorhersage, {@code CelestialGravityClient}). */
    public static void floatItem(Entity e) {
        if (e instanceof ItemEntity item) {
            Vec3 m = item.getDeltaMovement();
            item.setDeltaMovement(m.x * 0.9D, 0.03999999910593033D, m.z * 0.9D); // when entity gravity is applied, this becomes exactly 0
        }
    }
}
