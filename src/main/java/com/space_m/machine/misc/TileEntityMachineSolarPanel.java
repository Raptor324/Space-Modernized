package com.space_m.machine.misc;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.blockentity.BaseMachineBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineSolarPanel}: Sonnenstaerke {@code sun = Himmelslicht - skylightSubtracted - 11}
 * (1..4); bei {@code sun > 0} und freiem Himmel ueber dem Kern {@code ceil(sun * 25 * getSunPower)} HE je Tick
 * ("Balanced around 100he/t on Earth"), Speicher {@code maxpwr} = 1000 (NBT {@code power}/{@code maxpwr}), Abgabe an alle
 * sechs Nachbarn des Kerns. {@code getSunPower} = {@code SpaceApi.solarIntensity} (Addon: Koerper + Finsternis + Orbit).
 * Original-Klasse: TileEntityMachineSolarPanel
 */
public class TileEntityMachineSolarPanel extends BaseMachineBlockEntity {

    private long maxpwr = 1_000;

    public TileEntityMachineSolarPanel(BlockPos pos, BlockState state) {
        super(MiscBlockEntities.SOLAR_PANEL.get(), pos, state, 0, 1_000L, 0L, Long.MAX_VALUE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineSolarPanel be) {
        if (level.isClientSide) return;

        // Sun power ranges from 1-4
        int sun = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() - 11;

        be.ensureNetworkInitialized(); // tryProvide an alle sechs Nachbarn

        if (sun <= 0 || !level.canSeeSky(pos.above())) {
            return;
        }

        long power = be.getEnergyStored() + be.getOutput(sun);

        if (power > be.maxpwr) power = be.maxpwr;
        be.setEnergyStored(power);
        be.setChanged();
    }

    // Balanced around 100he/t on Earth
    public long getOutput(int sun) {
        return Mth.ceil(sun * 25 * SpaceApi.solarIntensity(level, worldPosition));
    }

    @Override
    public boolean canReceive() {
        return false;
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("power", getEnergyStored());
        nbt.putLong("maxpwr", maxpwr);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        if (nbt.contains("maxpwr")) this.maxpwr = nbt.getLong("maxpwr");
        if (this.maxpwr <= 0) this.maxpwr = 1_000;
        setEnergyCapacity(maxpwr);
        if (nbt.contains("power")) setEnergyStored(nbt.getLong("power"));
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Override protected Component getDefaultName() { return Component.translatable("block.space_m.machine_solar"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox() = INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
}
