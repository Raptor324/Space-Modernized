package com.space_m.machine.atmo;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Polluting;
import com.hbm_m.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityAtmosphericEmitter}: blaest Gas in die Atmosphaere des Koerpers.
 *
 * <p>Server: alle 20 Ticks Abo des Tanktyps (und Strom) an den acht Sockelnachbarn. Ist Gas im Tank (50 000 mB):
 * {@code toSpill = max(fill/2, 1)}; mit {@code power >= toSpill*10} (Puffer 250 000 HE) explodiert Antimaterie
 * ({@code FT_Amat}, Staerke 10, brennend), sonst wird {@code toSpill} entnommen, als Verschmutzung ({@code SPILL})
 * gemeldet und in die Atmosphaere abgegeben ({@code FT_Gaseous.release}), Energie {@code - toSpill*10}.
 *
 * <p>Client: laeuft er ({@code power >= max(fill/2,1)*10}), dreht der Luefter 45 Grad/Tick, der Elektromotor-Loop
 * ({@code hbm:block.motor}, Lautstaerke 0,25, Reichweite 10) spielt und bei Tankinhalt steigt eine Rauchsaeule
 * ({@code "tower"}-Partikel in Tankfarbe) aus der Sockelmitte 2,5 Bloecke hoch, wenn der Spieler naeher als 100 ist.
 * Original-Klasse: TileEntityAtmosphericEmitter
 */
public class AtmosphericEmitterBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2 {

    public static final long MAX_POWER = 250_000L;

    public final FluidTank tank = new FluidTank(AtmoFluids.none(), 50_000);

    public float fan = 0;
    public float prevFan = 0;

    public AtmosphericEmitterBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ATMO_EMITTER.get(), pos, state, 0, MAX_POWER, MAX_POWER);
    }

    /** Laeuft der Emitter (Original-Bedingung im Client-Zweig). */
    public boolean isRunning() {
        return getEnergyStored() >= Math.max(tank.getFill() / 2, 1) * 10L;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphericEmitterBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getGameTime() % 20 == 0) {
            ensureNetworkInitialized();
            for (AtmoConnections.DirPos dp : AtmoConnections.conPos(pos, state)) {
                trySubscribe(tank.getTankType(), level, dp.pos(), dp.dir());
            }
        }

        // networkPackNT(50)
        sendUpdateToClient();

        if (tank.getFill() > 0) {
            int toSpill = Math.max(tank.getFill() / 2, 1);

            if (getEnergyStored() >= toSpill * 10L) {
                Fluid type = tank.getTankType();
                if (FluidType.hasTrait(type, FT_Amat.class)) {
                    level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10F, true, Level.ExplosionInteraction.BLOCK);
                    return;
                }

                tank.setFill(tank.getFill() - toSpill);

                FT_Polluting.pollute(level, pos, type, FluidReleaseType.SPILL, toSpill);
                CelestialBody.releaseGas(level, AtmoFluids.id(type), toSpill);

                setEnergyStored(getEnergyStored() - toSpill * 10L);
                setChanged();
            }
        }
    }

    private void clientTick() {
        prevFan = fan;

        boolean running = isRunning();
        if (running) {
            fan += 45;

            if (fan >= 360) {
                fan -= 360;
                prevFan -= 360;
            }
        }

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.machine.atmo.client.AtmoClientEffects.emitterTick(this, running));
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.machine.atmo.client.AtmoClientEffects.emitterStop(this));
        }
    }

    // ==================== Fluid ====================

    /** Original {@code IFluidStandardReceiverMK2}: nur der eingestellte Typ wird angenommen ({@code NONE} nimmt nichts). */
    @Override
    public long getDemand(Fluid fluid, int pressure) {
        if (AtmoFluids.isNone(tank.getTankType())) return 0;
        if (tank.getPressure() != pressure || !VanillaFluidEquivalence.sameSubstance(tank.getTankType(), fluid)) return 0;
        return tank.getMaxFill() - tank.getFill();
    }

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return true;
    }

    // ==================== NBT (Original-Schluessel "t") ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "t");
    }

    // ==================== sonstiges ====================

    @Override protected Component getDefaultName() { return Component.translatable("block.space_m.machine_atmo_emitter"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+3, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
    }
}
