package com.space_m.lifesupport;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.atmosphere.AtmosphereBlob;
import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityAirScrubber} (Block {@code air_scrubber}, 1x1x2): sucht alle 5 Ticks die Luftpumpe
 * des Raums, in dem er steht (Blob-Wurzel), meldet sich dort an und nimmt deren Abgas als CO2 auf (Tank 16 000 mB,
 * 10 HE je mB, arbeitet ab > 200 HE, Puffer 10 000 HE). CO2 geht an alle sechs Nachbarn. Luefter dreht bis 30 Grad/Tick.
 * Original-Klasse: TileEntityAirScrubber
 */
public class TileEntityAirScrubber extends BaseMachineBlockEntity implements IFluidStandardSenderMK2 {

    public static final long MAX_POWER = 10000;

    private TileEntityAirPump pump;
    public final FluidTank tank;

    public float rot;
    public float prevRot;
    private float rotSpeed;

    public TileEntityAirScrubber(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.AIR_SCRUBBER.get(), pos, state, 0, MAX_POWER, MAX_POWER);
        tank = new FluidTank(ModFluids.CARBONDIOXIDE.getSource(), 16_000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityAirScrubber be) {
        be.updateEntity();
    }

    public void updateEntity() {
        if (!level.isClientSide) {
            ensureNetworkInitialized();

            if (canOperate()) {
                // Fetch a new pump to scrub CO2 from
                if (level.getGameTime() % 5 == 0 && (pump == null || pump.getFluidPressure() == 0 || !pump.registerScrubber(this))) {
                    pump = null;

                    List<AtmosphereBlob> blobs = ChunkAtmosphereManager.proxy.getBlobs(level, worldPosition);

                    for (AtmosphereBlob blob : blobs) {
                        if (blob != null) {
                            BlockPos pos = blob.getRootPosition();
                            BlockEntity te = level.getBlockEntity(pos);
                            if (te instanceof TileEntityAirPump airPump) {
                                pump = airPump;
                                if (!pump.registerScrubber(this)) {
                                    pump = null;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            for (Direction dir : Direction.values()) {
                if (tank.getFill() > 0) tryProvide(tank, level, worldPosition.relative(dir), dir);
            }

            // Original networkPackNT(20)
            setChanged();
            sendUpdateToClient();
        } else {
            float maxSpeed = 30F;

            if (canOperate()) {
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
    }

    public boolean canOperate() {
        return getEnergyStored() > 200;
    }

    public int scrub(int amount) {
        if (!canOperate()) return 0;
        int add = Math.min(tank.getMaxFill() - tank.getFill(), amount);
        tank.setFill(tank.getFill() + add);
        setEnergyStored(getEnergyStored() - add * 10L);
        return add;
    }

    /** Original {@code getPower()}. */
    public long getPower() {
        return getEnergyStored();
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        tank.writeToNBT(nbt, "t");
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        tank.readFromNBT(nbt, "t");
    }

    @Override
    public FluidTank[] getAllTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tank };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
    }

    // ── BaseMachineBlockEntity (kein GUI, Original getName "container.airScrubber") ──

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.airScrubber");
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
