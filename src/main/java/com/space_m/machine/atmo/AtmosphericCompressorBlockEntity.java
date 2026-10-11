package com.space_m.machine.atmo;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
 * 1:1 NTM Space {@code TileEntityAtmosphericCompressor}: saugt die Koerper-Atmosphaere an.
 *
 * <p>Jeden Tick (Server): ausserhalb des Orbits und ausserhalb abgedichteter Raeume gilt die Atmosphaere des Koerpers.
 * Hat sie Druck und enthaelt das eingestellte Gas nicht (mehr), wird auf ihr Hauptgas umgestellt; ohne Atmosphaere ist
 * der Tank {@code NONE}. Mit {@code power >= maxPower/50} (20 000 HE von 1 000 000), gueltigem Gas und Platz fuer 200 mB:
 * Tank +200 mB, Energie -20 000 HE und 200 mB werden der Atmosphaere entnommen ({@code FT_Gaseous.capture}). Ausgabe an
 * die acht Sockelnachbarn. Client: Luefter beschleunigt mit 0,2/Tick bis 30, bremst ohne Strom mit 0,1.
 *
 * <p>Abweichung (technisch noetig): Atmosphaeren-Gase, die noch nicht als Fluid registriert sind (Space-Fluide, R4),
 * verhalten sich wie {@code NONE} - der Kompressor laeuft dort erst mit R4.
 * Original-Klasse: TileEntityAtmosphericCompressor
 */
public class AtmosphericCompressorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2 {

    public static final long MAX_POWER = 1_000_000L;

    public final FluidTank tank = new FluidTank(ModFluids.AIR.getSource(), 50_000);

    public float rot;
    public float prevRot;
    private float rotSpeed;

    public AtmosphericCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ATMOSPHERIC_VENT.get(), pos, state, 0, MAX_POWER, MAX_POWER);
    }

    /** Original {@code hasPower}: {@code power >= getMaxPower() / 50}. */
    public boolean hasPower() {
        return getEnergyStored() >= MAX_POWER / 50;
    }

    private ResourceLocation tankId() {
        return AtmoFluids.id(tank.getTankType());
    }

    private void setTankType(ResourceLocation id) {
        Fluid f = AtmoFluids.fluid(id);
        if (f != tank.getTankType()) tank.setTankType(f);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphericCompressorBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        ensureNetworkInitialized();

        // Original updateConnections: trySubscribe (Strom) + tryProvide(tank) an getConPos
        for (AtmoConnections.DirPos dp : AtmoConnections.conPos(pos, state)) {
            tryProvide(tank, level, dp.pos(), dp.dir());
        }

        // Extractors will not work indoors (or in space oops)
        CBT_Atmosphere atmosphere = !CelestialBody.inOrbit(level) && !AtmoBlobs.hasAtmosphere(level, pos)
                ? CelestialBody.getTrait(level, CBT_Atmosphere.class)
                : null;

        if (atmosphere != null && atmosphere.getPressure() > 0.0D) {
            // If the atmosphere doesn't contain the fluid we're sucking up, pick a new one
            if (!atmosphere.hasFluid(tankId())) {
                setTankType(atmosphere.getMainFluid());
            }
        } else {
            setTankType(AtmoFluids.id(AtmoFluids.none()));
        }

        if (hasPower() && !AtmoFluids.isNone(tank.getTankType()) && tank.getFill() + 200 <= tank.getMaxFill()) {
            tank.setFill(tank.getFill() + 200);
            setEnergyStored(getEnergyStored() - MAX_POWER / 50);

            CelestialBody.captureGas(level, tankId(), 200);
        }

        setChanged();
        sendUpdateToClient(); // networkPackNT(50)
    }

    private void clientTick() {
        float maxSpeed = 30F;

        if (hasPower()) {
            rotSpeed += 0.2F;
            if (rotSpeed > maxSpeed) rotSpeed = maxSpeed;
        } else {
            rotSpeed -= 0.1F;
            if (rotSpeed < 0) rotSpeed = 0;
        }

        prevRot = rot;
        rot += rotSpeed;

        if (rot >= 360) {
            rot -= 360;
            prevRot -= 360;
        }
    }

    /** Original {@code cycleGas}: naechstes Gas der (absteigend sortierten) Atmosphaere, unter 0,0001 atm zurueck zum ersten. */
    public void cycleGas() {
        if (level == null) return;
        CBT_Atmosphere atmosphere = !AtmoBlobs.hasAtmosphere(level, worldPosition)
                ? CelestialBody.getTrait(level, CBT_Atmosphere.class)
                : null;

        if (atmosphere == null) return;

        atmosphere.sortDescending();

        ResourceLocation currentFluid = tankId();

        for (int i = 0; i < atmosphere.fluids.size(); i++) {
            if (atmosphere.fluids.get(i).fluid.equals(currentFluid)) {
                int targetIndex = i + 1;
                if (targetIndex >= atmosphere.fluids.size()) targetIndex = 0;
                if (atmosphere.fluids.get(targetIndex).pressure <= 0.0001) targetIndex = 0;

                setTankType(atmosphere.fluids.get(targetIndex).fluid);
                break;
            }
        }
    }

    /** Original {@code switchGas}: nur Gase, die die Atmosphaere enthaelt; true bei Wechsel. */
    public boolean switchGas(Fluid targetFluid) {
        if (level == null) return false;
        if (targetFluid == tank.getTankType()) return false;

        CBT_Atmosphere atmosphere = !AtmoBlobs.hasAtmosphere(level, worldPosition)
                ? CelestialBody.getTrait(level, CBT_Atmosphere.class)
                : null;

        if (atmosphere == null) return false;

        ResourceLocation target = AtmoFluids.id(targetFluid);
        for (CBT_Atmosphere.FluidEntry entry : atmosphere.fluids) {
            if (entry.fluid.equals(target)) {
                tank.setTankType(targetFluid);
                return true;
            }
        }

        return false;
    }

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ==================== NBT (Original-Schluessel "water") ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "water");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "water");
    }

    // ==================== sonstiges ====================

    /** Original {@code getName() = "container.air"}. */
    @Override protected Component getDefaultName() { return Component.translatable("block.space_m.machine_atmo_vent"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+10, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 10, worldPosition.getZ() + 2);
    }
}
