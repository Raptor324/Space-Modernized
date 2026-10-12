package com.space_m.machine.misc;

import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.dim.SpaceDimensions;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ereignisse R7-A (Original {@code ModEventHandler} und {@code WorldProviderCelestial/WorldProviderOrbit}):
 * <ul>
 *   <li>"sneaky sneaky space furnace" ({@code onBlockPlaced}): jeder von einem Spieler gesetzte Vanilla-Ofen wird sofort
 *       durch {@code space_m:furnace} ersetzt (gleiche Ausrichtung, Name/Daten des Vanilla-Blockentities uebernommen) -
 *       in allen Dimensionen wie im Original (auf der Erde atmet er immer).</li>
 *   <li>Respawn ({@code getRespawnDimension}): steht das Bett in einer Space-Dimension (Himmelskoerper ausser Kerbin oder
 *       Orbit) an einer Stelle ohne atembare Atmosphaere, wird der Spieler stattdessen in der Oberwelt wiederbelebt.
 *       Port: beim Tod wird der Bett-Spawnpunkt in diesem Fall auf die Oberwelt zurueckgesetzt (in 1.20/1.21 gibt es nur
 *       einen Spawnpunkt; ohne Bett wie im Original ohnehin Oberwelt). Explodierende Betten verhindert hier der
 *       Dimensionstyp ({@code bed_works = true}) statt {@code attemptingSleep}.</li>
 * </ul>
 */
public final class MiscEvents {

    private MiscEvents() {}

    public static void init() {
        BlockEvent.PLACE.register(MiscEvents::onBlockPlaced);
        EntityEvent.LIVING_DEATH.register(MiscEvents::onDeath);
    }

    private static EventResult onBlockPlaced(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity placer) {
        if (level == null || level.isClientSide) return EventResult.pass();

        // sneaky sneaky space furnace
        BlockState now = level.getBlockState(pos);
        if (now.is(Blocks.FURNACE)) {
            BlockEntity old = level.getBlockEntity(pos);
            //? if < 1.21.1 {
            CompoundTag data = old != null ? old.saveWithoutMetadata() : null;
            //?} else {
            /*CompoundTag data = old != null ? old.saveWithoutMetadata(level.registryAccess()) : null;
            *///?}

            level.setBlock(pos, MiscBlocks.FURNACE.get().defaultBlockState()
                    .setValue(AbstractFurnaceBlock.FACING, now.getValue(AbstractFurnaceBlock.FACING)), 2);

            BlockEntity fresh = level.getBlockEntity(pos);
            if (fresh != null && data != null) {
                //? if < 1.21.1 {
                fresh.load(data);
                //?} else {
                /*fresh.loadWithComponents(data, level.registryAccess());
                *///?}
                fresh.setChanged();
            }
        }

        return EventResult.pass();
    }

    private static EventResult onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof ServerPlayer player) || player.level().isClientSide) return EventResult.pass();

        ResourceKey<Level> dim = player.getRespawnDimension();
        BlockPos bed = player.getRespawnPosition();

        // If no bed, respawn in overworld (Vanilla)
        if (bed == null || dim == Level.OVERWORLD || !isSpaceDimension(dim)) return EventResult.pass();

        ServerLevel world = player.server.getLevel(dim);
        if (world == null) return EventResult.pass();

        // If the bed location has no breathable atmosphere, respawn in overworld
        CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(world, bed);
        if (!ChunkAtmosphereManager.proxy.canBreathe(atmosphere)) {
            player.setRespawnPosition(Level.OVERWORLD, null, 0.0F, false, false);
        }

        return EventResult.pass();
    }

    /** Original {@code provider instanceof WorldProviderCelestial || instanceof WorldProviderOrbit} (ohne Oberwelt). */
    public static boolean isSpaceDimension(ResourceKey<Level> dim) {
        if (dim == Level.OVERWORLD) return false;
        if (dim == SpaceDimensions.ORBIT) return true;
        return CelestialBody.getBodyOrNull(dim) != null;
    }
}
