package com.space_m.machine.dyson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.api.item.ItemHandlerAccess;
import com.hbm_m.item.ISatChip;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.platform.ModItemStackHandler;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Dyson;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.machine.misc.R7MachineBlock;
import com.space_m.sound.SpaceSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityDysonLauncher}: schiesst Dyson-Schwarmmitglieder ({@code swarm_member}) in den Schwarm
 * der Chip-Frequenz ({@code CBT_Dyson.launch}). 20 000 000 HE Speicher; Hochlauf 132 Ticks (Overdrive 38) bei
 * {@code MAX_POWER / Hochlaufzeit} HE je Tick, danach bis zu 4 Mitglieder je Schuss, Auslauf 68 Ticks (Overdrive 12).
 * Geraeusche {@code misc.spincharge} (Tonhoehe 0.25 bzw. 1.0) und zweimal {@code misc.spinshot} (Lautstaerke nach
 * Atmosphaerendruck {@code min((1-(1-p)^3)*16, 4)}), Partikel "spinlaunch" (Anzahl {@code min(20, p*80)}).
 * Strom ueber die fuenf Seitenzellen ({@code getConPos}: -rot*3 neben {@code -dir*0..4}); Mitglieder werden einzeln aus
 * einem Inventar hinter der Ladezelle ({@code -dir*9}) nachgeladen, solange weniger als 4 im Slot liegen.
 * Client: Drehung ({@code rotation/speed}), Nutzlastflug ({@code payloadTicks}).
 * Original-Klasse: TileEntityDysonLauncher
 */
public class TileEntityDysonLauncher extends BaseMachineBlockEntity {

    public int swarmId;
    public int swarmCount;

    public static final long MAX_POWER = 20_000_000;

    private static final int MEMBERS_PER_LAUNCH = 4;

    // SHAKE IT LIKE IT'S HEAT, OVERDRIVE
    public boolean sunsetOverdrive = false;

    public boolean isOperating;
    public boolean isSpinningDown;
    public int operatingTime;

    public float rotation;
    public float lastRotation;
    public float speed;

    public int payloadTicks;

    public int satCount;

    public TileEntityDysonLauncher(BlockPos pos, BlockState state) {
        super(DysonBlockEntities.LAUNCHER.get(), pos, state, 2, MAX_POWER, MAX_POWER);
    }

    /** Original {@code getInventoryStackLimit() = 4}. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 4;
            }
        };
    }

    public long power() {
        return getEnergyStored();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDysonLauncher be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level, pos, state);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
        ensureNetworkInitialized();
        Direction facing = R7MachineBlock.dir(state);
        tryLoad(pos.offset(-facing.getStepX() * 9, 0, -facing.getStepZ() * 9), facing.getOpposite()); // getInvPos

        ItemStack members = inventory.getStackInSlot(0);
        swarmId = ISatChip.getFreqS(inventory.getStackInSlot(1));
        swarmCount = CBT_Dyson.count(level, swarmId);

        isOperating = !isSpinningDown && getEnergyStored() >= getPowerPerTick() && !members.isEmpty()
                && members.getItem() == SpaceMaterialItems.SWARM_MEMBER.get() && swarmId > 0;

        if (isSpinningDown) {
            operatingTime++;

            if (operatingTime > getSpinDownTime()) {
                isSpinningDown = false;
                operatingTime = 0;
            }
        } else if (isOperating) {
            if (operatingTime == 0) {
                float pitch = sunsetOverdrive ? 1.0F : 0.25F;
                level.playSound(null, pos.getX(), pos.getY() + 8, pos.getZ(), SpaceSounds.MISC_SPINCHARGE.get(), SoundSource.BLOCKS, 1.5F, pitch);
            }

            operatingTime++;
            setEnergyStored(getEnergyStored() - getPowerPerTick());

            if (operatingTime > getSpinUpTime()) {
                int toLaunch = Math.min(members.getCount(), MEMBERS_PER_LAUNCH);
                CBT_Dyson.launch(level, swarmId, toLaunch);

                CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
                double pressure = atmosphere != null ? atmosphere.getPressure() : 0;
                double scaledPressure = 1.0 - Math.pow(1.0 - pressure, 3);

                float volume = Math.min((float) scaledPressure * 16.0F, 4.0F);

                Direction dir = R7MachineBlock.dir(state);
                Direction rot = R7MachineBlock.rot(dir);

                double sx = pos.getX() + rot.getStepX() * 6, sy = pos.getY() + 8, sz = pos.getZ() + rot.getStepZ() * 6;
                level.playSound(null, sx, sy, sz, SpaceSounds.MISC_SPINSHOT.get(), SoundSource.BLOCKS, volume, 0.9F + level.random.nextFloat() * 0.3F);
                level.playSound(null, sx, sy, sz, SpaceSounds.MISC_SPINSHOT.get(), SoundSource.BLOCKS, volume, 1F + level.random.nextFloat() * 0.3F);

                int count = Math.min(20, (int) (pressure * 80));

                double posX = pos.getX() + rot.getStepX() * 9;
                double posY = pos.getY() + 12;
                double posZ = pos.getZ() + rot.getStepZ() * 9;

                CompoundTag data = new CompoundTag();
                data.putInt("count", count);
                data.putString("type", "spinlaunch");
                data.putFloat("scale", 3);
                data.putDouble("moX", dir.getStepX() * 10);
                data.putDouble("moY", 10);
                data.putDouble("moZ", dir.getStepZ() * 10);
                data.putInt("maxAge", 10 + count / 2 + level.random.nextInt(5));
                IParticleCreator.sendPacket(level, posX, posY, posZ, 150, data);

                members.shrink(toLaunch);
                inventory.setStackInSlot(0, members.isEmpty() ? ItemStack.EMPTY : members);

                operatingTime = 0;
                isSpinningDown = true;
            }
        } else {
            operatingTime = 0;
        }

        setChanged();
        sendUpdateToClient(); // networkPackNT(250)
    }

    private void clientTick() {
        float acceleration = sunsetOverdrive ? 2.5F : 0.75F;
        float deceleration = sunsetOverdrive ? 15.0F : 3.0F;
        float resetSpeed = sunsetOverdrive ? 30.0F : 8.0F;

        if (isOperating) {
            speed += acceleration;
            if (speed > 90) speed = 90;
        } else if (speed > 0.1F) {
            speed -= deceleration;
            if (speed < resetSpeed) speed = resetSpeed;
        }

        lastRotation = rotation;
        if (!isOperating && speed <= resetSpeed && rotation > 360 - resetSpeed * 1.5) {
            lastRotation -= 360;
            rotation = 0;
            speed = 0;
        } else {
            rotation += speed;
        }

        if (rotation >= 360) {
            rotation -= 360;
            lastRotation -= 360;
        }

        if (isSpinningDown) {
            payloadTicks++;
        } else {
            payloadTicks = 0;
        }
    }

    private int getSpinUpTime() { return sunsetOverdrive ? 38 : 132; }
    private int getSpinDownTime() { return sunsetOverdrive ? 12 : 68; }
    private long getPowerPerTick() { return MAX_POWER / getSpinUpTime(); }

    /** Original {@code getConPos}: {@code -dir*i - rot*3} (i = 0..4), Richtung {@code rot.getOpposite()}. */
    public BlockPos[] getConPos() {
        Direction dir = R7MachineBlock.dir(getBlockState());
        Direction rot = R7MachineBlock.rot(dir);
        BlockPos[] out = new BlockPos[5];
        for (int i = 0; i < 5; i++) {
            out[i] = worldPosition.offset(-dir.getStepX() * i - rot.getStepX() * 3, 0, -dir.getStepZ() * i - rot.getStepZ() * 3);
        }
        return out;
    }

    /** Stromanschluss: die fuenf Anschlusszellen ({@code makeExtra} bei {@code -dir*i - rot*2}), deren Nachbarn {@link #getConPos} sind. */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        Direction dir = R7MachineBlock.dir(getBlockState());
        Direction rot = R7MachineBlock.rot(dir);
        BlockPos[] out = new BlockPos[5];
        for (int i = 0; i < 5; i++) {
            out[i] = worldPosition.offset(-dir.getStepX() * i - rot.getStepX() * 2, 0, -dir.getStepZ() * i - rot.getStepZ() * 2);
        }
        return out;
    }

    /**
     * Original {@code tryLoad}: ist der Slot nicht voll (unter 4), wird genau ein {@code swarm_member} aus dem Inventar
     * entnommen (Seitenregeln des Nachbarn ueber dessen Item-Handler der angegebenen Seite).
     */
    private void tryLoad(BlockPos pos, Direction side) {
        ItemStack slot0 = inventory.getStackInSlot(0);
        if (!slot0.isEmpty() && slot0.getCount() >= MEMBERS_PER_LAUNCH) return;
        if (level == null || !level.isLoaded(pos)) return;

        var inv = ItemHandlerAccess.getItemHandler(level, pos, side);
        if (inv == null) return;

        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == SpaceMaterialItems.SWARM_MEMBER.get()) {
                ItemStack removed = inv.extractItem(i, 1, false);
                if (removed.isEmpty()) continue;
                if (slot0.isEmpty()) {
                    inventory.setStackInSlot(0, removed);
                } else {
                    slot0.grow(1);
                    inventory.setStackInSlot(0, slot0);
                }
                break;
            }
        }
    }

    // ==================== NBT / Sync ====================

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("spinDown", isSpinningDown);
        nbt.putInt("time", operatingTime);
        nbt.putBoolean("overdrive", sunsetOverdrive);
        // networkPackNT
        nbt.putInt("s_swarmId", swarmId);
        nbt.putBoolean("s_operating", isOperating);
        nbt.putInt("s_swarmCount", swarmCount);
        nbt.putInt("s_satCount", inventory.getStackInSlot(0).getCount());
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        isSpinningDown = nbt.getBoolean("spinDown");
        operatingTime = nbt.getInt("time");
        sunsetOverdrive = nbt.getBoolean("overdrive");
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag nbt) {
        super.applyClientUpdate(nbt);
        swarmId = nbt.getInt("s_swarmId");
        isOperating = nbt.getBoolean("s_operating");
        swarmCount = nbt.getInt("s_swarmCount");
        satCount = nbt.getInt("s_satCount");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack itemStack) {
        if (slot == 0) return itemStack.getItem() == SpaceMaterialItems.SWARM_MEMBER.get();
        return false;
    }

    /** Original {@code getName() = "container.machineDysonLauncher"}. */
    @Override protected Component getDefaultName() { return Component.translatable("container.machineDysonLauncher"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-100..x+100, y..y+100, z-100..z+100. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 100, worldPosition.getY(), worldPosition.getZ() - 100,
                worldPosition.getX() + 100, worldPosition.getY() + 100, worldPosition.getZ() + 100);
    }
}
