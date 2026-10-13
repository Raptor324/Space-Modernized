package com.space_m.rocket.transport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.config.SpaceConfig;
import com.space_m.sound.SpaceSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityCombatDropPod}: gelandete Kampf-Abwurfkapsel. 40 Ticks Verzoegerung, dann klappen
 * beide Luken auf ({@code hatchopen -> 90}, {@code hatchopen2 -> -90}, je 20 % pro Tick); der Server setzt die Nutzlast
 * ({@code amount}x die gespeicherte Entity) auf den Block, spielt {@code block.hatchOpen} (10 / 0,9) und leert sie.
 * Mit {@code 1.94_combatPodDespawn} verschwindet die Kapsel bei {@code Weltzeit % 1000 == 0}.
 *
 * <p>Abweichung (technisch noetig): die gespeicherte {@code UUID} wird je Kopie entfernt - moderne Welten weisen eine
 * zweite Entity mit gleicher UUID ab (1.7.10 nicht).
 * Original-Klasse: TileEntityCombatDropPod
 */
public class CombatDropPodBlockEntity extends BaseHbmBlockEntity {

    @Nullable public CompoundTag entityType;
    public int amount;
    public int color;
    public int delay = 40;
    /** Ticks seit dem Aussetzen der Nutzlast (fuer das Verschwinden nach combatPodDespawnSeconds). */
    public int emptyTicks;

    public double hatchopen;
    public double hatchopen2;
    public double prevHatchopen;
    public double prevHatchopen2;

    public CombatDropPodBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.COMBAT_DROP_POD.get(), pos, state);
    }

    public void setPayload(CompoundTag entityType, int amount, int color) {
        this.entityType = entityType;
        this.amount = amount;
        this.color = color;
        setChanged();
    }

    public int getColor() {
        return this.color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CombatDropPodBlockEntity be) {
        be.update(level);
    }

    private void update(Level level) {
        prevHatchopen = hatchopen;
        prevHatchopen2 = hatchopen2;

        if (delay > 0) {
            delay--;
            return;
        }

        if (delay == 0) {
            hatchopen += (90 - hatchopen) * 0.2;
            hatchopen2 += (-90 - hatchopen2) * 0.2;
        }

        if (entityType != null && amount > 0 && !level.isClientSide) {

            for (int i = 0; i < amount; i++) {
                CompoundTag copy = entityType.copy();
                copy.remove("UUID");
                Entity entity = EntityType.loadEntityRecursive(copy, level, e -> {
                    e.moveTo(worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, e.getYRot(), e.getXRot());
                    return e;
                });

                if (entity != null) {
                    level.addFreshEntity(entity);
                }
            }

            amount = 0;
            level.playSound(null, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    SpaceSounds.BLOCK_HATCHOPEN.get(), SoundSource.BLOCKS, 10.0F, 0.9F);
            setChanged();
            return;
        }

        // Abweichung (Nutzerwunsch): leere Kapsel verschwindet combatPodDespawnSeconds nach dem Aussetzen der Nutzlast
        // (Original: bei Weltzeit % 1000 == 0) - verhindert Lag durch hunderte Kapseln waehrend einer Invasion.
        if (SpaceConfig.get().combatPodDespawn && !level.isClientSide) {
            emptyTicks++;
            if (emptyTicks >= Math.max(1, SpaceConfig.get().combatPodDespawnSeconds) * 20) {
                level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** Original {@code getRenderBoundingBox}: x-3..x+4, y..y+8, z-3..z+4. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 3, worldPosition.getY(), worldPosition.getZ() - 3,
                worldPosition.getX() + 4, worldPosition.getY() + 8, worldPosition.getZ() + 4);
    }

    /** Original {@code getDescriptionPacket}: das komplette NBT an den Client. */
    public void sync() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        if (entityType != null)
            nbt.put("EntityType", entityType);

        nbt.putInt("amount", amount);
        nbt.putInt("emptyTicks", emptyTicks);
        nbt.putInt("color", color);
        nbt.putInt("delay", delay);
        nbt.putDouble("hatchopen", hatchopen);
        nbt.putDouble("hatchopen2", hatchopen2);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        entityType = nbt.getCompound("EntityType");
        amount = nbt.getInt("amount");
        color = nbt.getInt("color");
        delay = nbt.getInt("delay");
        hatchopen = nbt.getDouble("hatchopen");
        hatchopen2 = nbt.getDouble("hatchopen2");
        emptyTicks = nbt.getInt("emptyTicks");
    }
}
