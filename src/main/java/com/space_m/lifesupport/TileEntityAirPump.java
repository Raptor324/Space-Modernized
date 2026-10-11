package com.space_m.lifesupport;

import java.util.Random;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.atmosphere.AtmosphereBlob;
import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.atmosphere.IAtmosphereProvider;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code TileEntityAirPump} (Block {@code air_vent}): fuellt einen abgedichteten Raum (Atmosphaeren-Blob,
 * Radius 256) aus seinem Gastank (16 000 mB, Standard Sauerstoff, per Fluid-Identifier jedes Gas). Ab 20 mB im Tank
 * meldet er sich beim Atmosphaeren-Kern an, fuellt den Blob mit 1 mB je Block (max. 20 mB/Tick) und rieselt danach
 * zufaellig 1 mB nach (Abgas fuer Luftwascher). Ohne Blob (Raum undicht) entweicht 20 mB/Tick ins Vakuum.
 * Druck im Raum = airFill / Blobgroesse * 0.2 atm. Ein Luftwascher im Raum gibt verbrauchte Luft als Bonus zurueck.
 * Original-Klasse: TileEntityAirPump
 * <p>Nicht portiert: OpenComputers-Komponente {@code ntm_atmospheric_vent} (OC gibt es auf 1.20.1/1.21.1 nicht).
 */
public class TileEntityAirPump extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2, IAtmosphereProvider {

    private int onTicks = 0;
    private boolean registered = false;

    // Bubble registration wait, so it has time to fill out the bubble before running (for recovering fill in a chunk that just loaded)
    private int recovering = 0;

    // Tile initialisation wait, for systems that run too fast and finish bubbling before chunks have time to load
    private int registerWait = 10;

    private AtmosphereBlob currentBlob;
    private int airFill = 0; // How many blocks are filled with air in the blob
    private int wasteFill = 0; // How many air blocks worth of waste gas has been produced
    private int bonusFill = 0; // How much waste gas has been returned, is consumed before the tank

    public final FluidTank tank;

    private final Random rand = new Random();

    // Used for synchronizing printable info
    public CBT_Atmosphere currentAtmosphere;

    private TileEntityAirScrubber scrubber;

    public TileEntityAirPump(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.AIR_VENT.get(), pos, state, 0, 0L, 0L, 0L);
        tank = new FluidTank(ModFluids.OXYGEN.getSource(), 16000);
    }

    @Override
    public Level getWorld() {
        return level;
    }

    public void spawnParticles() {

        if (level.getGameTime() % 2 == 0) {
            CompoundTag data = new CompoundTag();
            data.putString("type", "tower");
            data.putFloat("lift", 0.1F);
            data.putFloat("base", 0.3F);
            data.putFloat("max", 1F);
            data.putInt("life", 20 + level.random.nextInt(20));
            data.putInt("color", CelestialFluids.color(getFluidType()));

            data.putDouble("posX", worldPosition.getX() + 0.5 + level.random.nextDouble() - 0.5);
            data.putDouble("posZ", worldPosition.getZ() + 0.5 + level.random.nextDouble() - 0.5);
            data.putDouble("posY", worldPosition.getY() + 1);

            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityAirPump be) {
        be.updateEntity();
    }

    public void updateEntity() {
        if (!level.isClientSide) {
            if (onTicks > 0) onTicks--;
            if (registerWait > 0) registerWait--;

            if (tank.getFill() + bonusFill >= 20) {
                onTicks = 20;

                if (registerWait > 0) {
                    // do nothing
                } else if (!registered) {
                    ChunkAtmosphereManager.proxy.registerAtmosphere(this);
                    registered = true;

                    if (airFill > 1) {
                        recovering = 100;
                    }
                } else if (recovering > 0) {
                    recovering--;
                    if (currentBlob != null) {
                        recovering = 0;
                    }
                } else {
                    if (currentBlob != null) {
                        int size = currentBlob.getBlobSize();
                        if (size != 0) {
                            if (airFill > size) airFill = size;
                            if (wasteFill > size) wasteFill = size;

                            // Fill the blob from the tank, 1mB per block
                            int toFill = Math.min(size - airFill, 20);
                            airFill += toFill;

                            // Fill to the brim, and then trickle randomly afterwards
                            if (toFill > 0) {
                                consumeFromTank(toFill);
                            } else if (rand.nextBoolean()) {
                                consumeFromTank(1);
                                wasteFill++;
                            }

                            // Fill scrubbers with waste gas
                            if (scrubber != null && scrubber.isLoaded() && !scrubber.isRemoved()) {
                                int toScrub = Mth.clamp(wasteFill, 0, 20);
                                int scrubbed = scrubber.scrub(toScrub);
                                wasteFill -= scrubbed;
                            }
                        } else {
                            currentBlob = null;
                        }
                    }

                    if (currentBlob == null) {
                        // Venting to vacuum
                        consumeFromTank(20);
                        airFill = 0;
                        wasteFill = 0;
                    }
                }
            } else {
                if (registered) {
                    ChunkAtmosphereManager.proxy.unregisterAtmosphere(this);
                    registered = false;
                    currentBlob = null;
                    airFill = 0;
                    wasteFill = 0;
                }
            }

            if (level.getGameTime() % 5 == 0) {
                currentAtmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(level, worldPosition);
            }

            subscribeToAllAround();

            // Original networkPackNT(100)
            setChanged();
            sendUpdateToClient();

        } else {
            if (onTicks > 0) {
                this.spawnParticles();
            }
        }
    }

    /** Original {@code subscribeToAllAround(tank.getTankType(), this)}: alle sechs Nachbarn. */
    private void subscribeToAllAround() {
        for (Direction dir : Direction.values()) {
            trySubscribe(tank.getTankType(), level, worldPosition.relative(dir), dir);
        }
    }

    private void consumeFromTank(int amount) {
        int bonusConsumption = Mth.clamp(amount, 0, bonusFill);
        bonusFill -= bonusConsumption;
        amount -= bonusConsumption;

        tank.setFill(tank.getFill() - amount);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (registered && level != null && !level.isClientSide) {
            ChunkAtmosphereManager.proxy.unregisterAtmosphere(this);
            registered = false;
        }
    }

    // ── NBT (Original readFromNBT/writeToNBT + serialize/deserialize fuer den Client) ──

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        tank.writeToNBT(nbt, "at");
        nbt.putInt("fill", airFill);
        nbt.putInt("waste", wasteFill);
        nbt.putInt("bonus", bonusFill);
        // nur Anzeige (Original serialize)
        nbt.putInt("onTicks", onTicks);
        if (currentAtmosphere != null) {
            CompoundTag atmo = new CompoundTag();
            currentAtmosphere.writeToNBT(atmo);
            nbt.put("atmosphere", atmo);
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        tank.readFromNBT(nbt, "at");
        airFill = nbt.getInt("fill");
        wasteFill = nbt.getInt("waste");
        bonusFill = nbt.getInt("bonus");
        onTicks = nbt.getInt("onTicks");
        if (nbt.contains("atmosphere")) {
            currentAtmosphere = new CBT_Atmosphere();
            currentAtmosphere.readFromNBT(nbt.getCompound("atmosphere"));
        } else {
            currentAtmosphere = null;
        }
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public int getMaxBlobRadius() {
        return 256;
    }

    @Override
    public BlockPos getRootPosition() {
        return worldPosition;
    }

    @Override
    public ResourceLocation getFluidType() {
        return BuiltInRegistries.FLUID.getKey(tank.getTankType());
    }

    @Override
    public double getFluidPressure() {
        if (currentBlob == null || currentBlob.getBlobSize() == 0) return 0;

        return ((double) airFill / (double) currentBlob.getBlobSize()) * 0.2;
    }

    public boolean hasSeal() {
        return airFill > 1;
    }

    @Override
    public void onBlobCreated(AtmosphereBlob blob) {
        currentBlob = blob;
    }

    @Override
    public void consume(int amount) {
        airFill -= amount;
        wasteFill += amount;
        if (airFill < 1) airFill = 1;
    }

    @Override
    public void produce(int amount) {
        int produced = Mth.clamp(amount, 0, wasteFill);
        bonusFill += produced;
        wasteFill -= produced;
    }

    public boolean isRecycling() {
        return bonusFill > 0;
    }

    public boolean registerScrubber(TileEntityAirScrubber scrubber) {
        if (!this.isLoaded() || this.isRemoved()) return false;
        if (tank.getTankType() != ModFluids.OXYGEN.getSource()) return false;
        if (this.scrubber == scrubber) return true;
        if (this.scrubber != null && this.scrubber.isLoaded() && !this.scrubber.isRemoved() && this.scrubber.canOperate()) return false;
        this.scrubber = scrubber;
        return true;
    }

    // ── Test-/Anzeigezugriff ──
    public int getAirFill() { return airFill; }
    public int getWasteFill() { return wasteFill; }
    public int getBonusFill() { return bonusFill; }
    public boolean isRegistered() { return registered; }
    @Nullable public AtmosphereBlob getCurrentBlob() { return currentBlob; }

    // ── BaseMachineBlockEntity (kein GUI, Original getName "container.atmosphereVent") ──

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.atmosphereVent");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
