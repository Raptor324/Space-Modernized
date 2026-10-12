package com.space_m.orbit.propulsion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.SolarSystem;
import com.space_m.orbit.IPropulsion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsamer Unterbau der fluidbetriebenen Stationstriebwerke (Xenon, HTR3, HTR-F4), entspricht dem in allen drei
 * Originalen gleichen Teil ({@code TileEntityMachineBase implements IPropulsion, IFluidStandardReceiver}): Tanks,
 * {@code isOn}, {@code fuelCost}, Anmeldung bei der Station ({@code hasRegistered}), Animationszeit
 * ({@code speed/time/lastTime}) und Soundzaehler ({@code soundtime}). Werte (Schub, Tankgroesse, Strompreis,
 * Vorwaerm-/Abkuehlzeit) setzen die Unterklassen 1:1.
 */
public abstract class StationThrusterBlockEntity extends BaseMachineBlockEntity implements IPropulsion, IFluidStandardReceiverMK2 {

    public FluidTank[] tanks;

    public boolean isOn;
    protected float speed;
    public double lastTime;
    public double time;
    public float soundtime;

    protected int fuelCost;

    protected final PropulsionUtil.Registration registration = new PropulsionUtil.Registration();

    protected StationThrusterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, long maxPower) {
        super(type, pos, state, 0, maxPower, maxPower);
    }

    public Direction dir() {
        return PropulsionUtil.dir(getBlockState());
    }

    /** Original {@code isFacingPrograde()}. */
    public abstract boolean isFacingPrograde();

    /** Original {@code POWER_COST_MULTIPLIER} (0 = kein Strom noetig). */
    protected long powerCostMultiplier() {
        return 0;
    }

    /** Anmeldung (Original: erster Server-Tick, {@code isOn = false}). */
    protected void tickRegistration() {
        if (registration.tick(this, isFacingPrograde())) {
            isOn = false;
        }
    }

    /** Original {@code soundtime}-Zaehler auf dem Server mit Start-/Stoppgeraeusch. */
    protected void tickSound(net.minecraft.sounds.SoundEvent start, float startVol, net.minecraft.sounds.SoundEvent stop, float stopVol, int max) {
        if (level == null) return;
        if (isOn) {
            soundtime++;

            if (soundtime == 1) {
                level.playSound(null, worldPosition, start, net.minecraft.sounds.SoundSource.BLOCKS, startVol, 1F);
            } else if (soundtime > max) {
                soundtime = max;
            }
        } else {
            soundtime--;

            if (soundtime == max - 1) {
                level.playSound(null, worldPosition, stop, net.minecraft.sounds.SoundSource.BLOCKS, stopVol, 1F);
            } else if (soundtime <= 0) {
                soundtime = 0;
            }
        }
    }

    /** Client: {@code speed} +-0.05 (0..1), danach {@code lastTime = time; time += speed}. */
    protected void tickAnimation() {
        if (isOn) {
            speed += 0.05D;
            if (speed > 1) speed = 1;
        } else {
            speed -= 0.05D;
            if (speed < 0) speed = 0;
        }
    }

    protected void advanceTime() {
        lastTime = time;
        time += speed;
    }

    // ── IPropulsion ──────────────────────────────────────────────────────────────────────────

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    /** Original {@code canPerformBurn}: Treibstoff nach {@code SolarSystem.getFuelCost(deltaV, shipMass, ISP des Tanks 0)}. */
    @Override
    public boolean canPerformBurn(int shipMass, double deltaV) {
        int isp = PropulsionUtil.isp(tanks[0]);

        fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, isp);

        if (getEnergyStored() < (long) fuelCost * powerCostMultiplier()) return false;

        for (FluidTank tank : tanks) {
            if (tank.getFill() < fuelCost) return false;
        }

        return true;
    }

    @Override
    public void addErrors(java.util.List<Component> errors) {
        if (powerCostMultiplier() > 0 && getEnergyStored() < (long) fuelCost * powerCostMultiplier()) {
            PropulsionUtil.powerError(errors, (long) fuelCost * powerCostMultiplier());
        }

        for (FluidTank tank : tanks) {
            if (tank.getFill() < fuelCost) {
                PropulsionUtil.fuelError(errors, fuelCost);
            }
        }
    }

    /** Original {@code startBurn}: einschalten, Strom und Treibstoff abziehen, Vorwaermzeit zurueck. */
    @Override
    public int startBurn() {
        isOn = true;
        if (powerCostMultiplier() > 0) setEnergyStored(getEnergyStored() - (long) fuelCost * powerCostMultiplier());
        for (FluidTank tank : tanks) {
            tank.setFill(tank.getFill() - fuelCost);
        }
        setChanged();
        return burnTime();
    }

    @Override
    public int endBurn() {
        isOn = false;
        setChanged();
        return burnTime();
    }

    /** Original-Rueckgabe von {@code startBurn}/{@code endBurn}. */
    protected abstract int burnTime();

    public int getFuelCost() {
        return fuelCost;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) registration.remove(this);
    }

    // ── Fluide ───────────────────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT (Original writeToNBT "on"/"power"/"t<i>", Sync zusaetzlich fuelCost/soundtime) ──────

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("on", isOn);
        nbt.putLong("power", getEnergyStored());
        nbt.putInt("fuelCost", fuelCost);
        nbt.putFloat("soundtime", soundtime);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        isOn = nbt.getBoolean("on");
        fuelCost = nbt.getInt("fuelCost");
        soundtime = nbt.getFloat("soundtime");
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
    }

    // ── kein Inventar, keine GUI ─────────────────────────────────────────────────────────────

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }
    @Override public Component getDisplayName() { return getDefaultName(); }
}
