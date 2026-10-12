package com.space_m.bomb;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.bomb.NukeBaseBlockEntity;
import com.hbm_m.item.ModItems;
import com.hbm_m.sound.HbmSoundsNT;
import com.space_m.item.SpaceMaterialItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityAntimatter} ({@code TileEntityMachineBase(8)}, Timer 18000 Ticks).
 * <ul>
 *   <li>Geladen = Slot 2 {@code pellet_antimatter} und Slots 0/1 {@code particle_lead} + Slots 3/4 {@code ingot_hafnium}.</li>
 *   <li>Scharf ({@code started}): Timer zaehlt, jede Sekunde {@code weapon.fstbmbPing} (5.0/1.0), bei 0 Explosion; faellt ein
 *       Teil heraus, bricht der Countdown ab.</li>
 *   <li>GUI ({@code AuxButtonPacket} -> {@code NBTControlPacket}): meta 0 = Start ({@code weapon.fstbmbStart}), meta 1 = Timer
 *       in Sekunden.</li>
 *   <li>Explosion: Slots leeren, Block ohne Drop entfernen, {@code EntityBalefire.antimatter()} Radius 150 an der Blockmitte,
 *       {@code EntityNukeTorex.startFacAnti(x+0.5, y+5, z+0.5, 1000)}.</li>
 * </ul>
 * Original-Klasse: TileEntityAntimatter
 * Original-Klasse: EntityAntimatter (im Original leere Datei)
 */
public class TileEntityAntimatter extends NukeBaseBlockEntity implements IControlReceiver {

    public static final int SLOTS = 8;

    public boolean loaded;
    public boolean started;
    public int timer;

    public TileEntityAntimatter(BlockPos pos, BlockState state) {
        super(SpaceBombs.ANTIMATTER_BE.get(), pos, state, SLOTS);
        timer = 18000;
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable("container.nukeAntimatter");
    }

    /** Original {@code updateEntity} (nur Server). */
    public void serverTick() {
        if (level == null || level.isClientSide) return;

        boolean wasLoaded = this.loaded;
        boolean hadCore = this.lastCore;
        this.loaded = isLoaded();
        this.lastCore = hasAmatCore();

        if (!this.loaded) {
            started = false;
        }

        if (started) {
            timer--;

            if (timer % 20 == 0)
                level.playSound(null, worldPosition, HbmSoundsNT.get("hbm:weapon.fstbmbPing"), SoundSource.BLOCKS, 5.0F, 1.0F);
        }

        if (timer <= 0) {
            explode();
            return;
        }

        // Original networkPackNT(250): Timer/Status an die Clients (Renderer zeigt Kern + Strahlen nur mit Kern)
        if (started || loaded != wasLoaded || lastCore != hadCore) sync();
    }

    private boolean lastCore;

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** Original {@code handleButtonPacket(value, meta)}. */
    public void handleButtonPacket(int value, int meta) {
        if (level == null) return;

        if (meta == 0 && this.isLoaded()) {
            level.playSound(null, worldPosition, HbmSoundsNT.get("hbm:weapon.fstbmbStart"), SoundSource.BLOCKS, 5.0F, 1.0F);
            started = true;
        }

        if (meta == 1)
            timer = value * 20;

        sync();
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        handleButtonPacket(data.getInt("value"), data.getInt("meta"));
    }

    public boolean isLoaded() {
        return hasAmatCore() && hasRest();
    }

    public boolean hasAmatCore() {
        return slots.get(2).is(ModItems.PELLET_ANTIMATTER.get());
    }

    public boolean hasRest() {
        return getRest() > 0;
    }

    public int getRest() {
        if (slots.get(1).is(ModItems.PARTICLE_LEAD.get()) && slots.get(0).is(ModItems.PARTICLE_LEAD.get())
                && slots.get(3).is(SpaceMaterialItems.INGOT_HAFNIUM.get()) && slots.get(4).is(SpaceMaterialItems.INGOT_HAFNIUM.get())) {
            return 1;
        }
        return 0;
    }

    /** Original {@code explode()}. */
    public void explode() {
        if (level == null) return;

        clearContent();

        // worldObj.func_147480_a(x, y, z, false) = Block ohne Drop zerstoeren
        level.destroyBlock(worldPosition, false);

        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        EntityBalefireAntimatter bf = new EntityBalefireAntimatter(SpaceBombs.BALEFIRE_ANTIMATTER.get(), level);
        bf.setPos(x, y, z);
        bf.destructionRange = 150;
        level.addFreshEntity(bf);
        EntityBalefireAntimatter.startFacAnti(level, worldPosition.getX() + 0.5, worldPosition.getY() + 5, worldPosition.getZ() + 0.5, 1000);
    }

    public String getMinutes() {
        String mins = "" + (timer / 1200);
        if (mins.length() == 1) mins = "0" + mins;
        return mins;
    }

    public String getSeconds() {
        String mins = "" + ((timer / 20) % 60);
        if (mins.length() == 1) mins = "0" + mins;
        return mins;
    }

    /** Original {@code TileEntityMachineBase}: Stapelgrenze 64, im GUI jeder Gegenstand, kein Trichterzugriff. */
    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction direction) {
        return new int[0];
    }

    @Override
    public boolean isReady() {
        return isLoaded();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerNukeAntimatter(id, inventory, this);
    }

    /** Original {@code getRenderBoundingBox}: {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return com.hbm_m.platform.RenderBounds.INFINITE;
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.started = tag.getBoolean("started");
        this.timer = tag.getInt("timer");
        this.loaded = tag.getBoolean("loaded");
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("started", started);
        tag.putInt("timer", timer);
        tag.putBoolean("loaded", loaded);
    }
}
