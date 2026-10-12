package com.space_m.orbit.propulsion;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fusion.IFusionPowerReceiver;
import com.hbm_m.api.fusion.PlasmaNetwork;
import com.hbm_m.api.fusion.PlasmaNetworkProvider;
import com.hbm_m.api.network.GenNode;
import com.hbm_m.api.network.NodeDirPos;
import com.hbm_m.api.network.NodeNet;
import com.hbm_m.api.network.UniNodespace;
import com.hbm_m.blockentity.machines.albion.CooledMachineBlockEntity;
import com.hbm_m.util.BobMathUtil;
import com.space_m.celestial.SolarSystem;
import com.space_m.orbit.IPropulsion;
import com.space_m.sound.SpaceSounds;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineHTRNeo} (HTR-F4 NEO): Fusionstriebwerk am Plasmanetz des Fusionstorus.
 * Haengt als {@link IFusionPowerReceiver} am Plasma-Knoten {@code -rot*10} (Verbindung {@code -rot*11}), braucht vollen
 * Speicher (200 MHE, wird beim Brennen geleert), Plasmaenergie {@code >= fuelCost} (ISP fest 250) und einen laufenden
 * Kuehlkreislauf (kaltes Perfluormethyl, Ziel unter 123 K; Basis {@code TileEntityCooledBase}). Schub 1 600 000 000,
 * 180 Ticks. {@code IBlockSealable} am TE ist im Original wirkungslos ({@code AtmosphereBlob} fragt nur
 * {@code block instanceof IBlockSealable}) und entfaellt daher. Anders als die anderen Triebwerke ohne Orbit-Abfrage
 * im Tick (1:1).
 * Original-Klasse: TileEntityMachineHTRNeo
 */
public class TileEntityMachineHTRNeo extends CooledMachineBlockEntity implements IPropulsion, IFusionPowerReceiver, NodeNet.ILoadedEntry {

    // i smushed these together because i need you so bad
    @Nullable protected GenNode<PlasmaNetwork> plasmaNode;

    public long plasmaEnergy;
    public long plasmaEnergySync;

    public static final long maxPower = 200_000_000L;

    public static final int COOLANT_USE = 50;

    public float rotor;
    public float prevRotor;
    public float rotorSpeed;
    public float soundtime;

    private final PropulsionUtil.Registration registration = new PropulsionUtil.Registration();
    public boolean isOn;
    public int fuelCost;
    public float thrustAmount;

    public float plasmaR;
    public float plasmaG;
    public float plasmaB;

    public TileEntityMachineHTRNeo(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.HTRF4_NEO.get(), pos, state, 0, maxPower, maxPower);
    }

    public Direction dir() {
        return PropulsionUtil.dir(getBlockState());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineHTRNeo be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level);
        } else {
            be.clientTick();
        }
    }

    private void serverTick(ServerLevel level) {
        if (registration.tick(this, isFacingPrograde())) {
            isOn = false;
        }

        for (NodeDirPos pos : getConPos()) {
            trySubscribe(level, pos.getX(), pos.getY(), pos.getZ(), pos.getDir());
            trySubscribe(coolantTanks[0].getTankType(), level, pos.getPos(), pos.getDir());
            tryProvide(coolantTanks[1], level, pos.getPos(), pos.getDir());
        }

        plasmaEnergySync = plasmaEnergy;

        temperature += TEMP_PASSIVE_HEATING;
        if (temperature > KELVIN + 20) temperature = KELVIN + 20;

        if (temperature > TEMPERATURE_TARGET) {
            int cyclesTemp = (int) Math.ceil((Math.min(temperature - TEMPERATURE_TARGET, TEMP_CHANGE_MAX)) / TEMP_CHANGE_PER_MB);
            int cyclesCool = coolantTanks[0].getFill();
            int cyclesHot = coolantTanks[1].getMaxFill() - coolantTanks[1].getFill();
            int cycles = BobMathUtil.min(cyclesTemp, cyclesCool, cyclesHot);

            coolantTanks[0].setFill(coolantTanks[0].getFill() - cycles);
            coolantTanks[1].setFill(coolantTanks[1].getFill() + cycles);
            temperature -= TEMP_CHANGE_PER_MB * cycles;
        }

        if (isOn) {
            soundtime++;

            if (soundtime == 1) {
                level.playSound(null, worldPosition, SpaceSounds.MISC_HTRFSTART.get(), SoundSource.BLOCKS, 1.5F, 1F);
            } else if (soundtime > 30) {
                soundtime = 30;
            }
        } else {
            soundtime--;

            if (soundtime == 29) {
                level.playSound(null, worldPosition, SpaceSounds.MISC_HTRSTOP.get(), SoundSource.BLOCKS, 2.0F, 1F);
            } else if (soundtime <= 0) {
                soundtime = 0;
            }
        }

        // nodespace setup
        if (plasmaNode == null || plasmaNode.expired) {
            Direction dir = PropulsionUtil.rot(dir());
            BlockPos nodePos = worldPosition.offset(dir.getStepX() * -10, 0, dir.getStepZ() * -10);
            plasmaNode = UniNodespace.getNode(level, nodePos, PlasmaNetworkProvider.THE_PROVIDER);

            if (plasmaNode == null) {
                plasmaNode = new GenNode<>(PlasmaNetworkProvider.THE_PROVIDER, nodePos)
                        .setConnections(new NodeDirPos(worldPosition.offset(dir.getStepX() * -11, 0, dir.getStepZ() * -11), dir));

                UniNodespace.createNode(level, plasmaNode);
            }
        }

        if (plasmaNode != null && plasmaNode.hasValidNet()) plasmaNode.net.addReceiver(this);

        setChanged();
        sendUpdateToClient(); // networkPackNT(200)
        plasmaEnergy = 0;
    }

    private void clientTick() {
        if (getEnergyStored() >= maxPower || isOn) rotorSpeed += 0.125F;
        else rotorSpeed -= 0.125F;

        rotorSpeed = Mth.clamp(rotorSpeed, 0F, 15F);

        prevRotor = rotor;
        rotor += rotorSpeed;

        if (rotor >= 360F) {
            rotor -= 360F;
            prevRotor -= 360F;
        }

        if (isOn) {
            thrustAmount += 0.01D;
            if (thrustAmount > 1) thrustAmount = 1;
        } else {
            thrustAmount -= 0.01D;
            if (thrustAmount < 0) thrustAmount = 0;
        }

        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.orbit.propulsion.client.PropulsionClientEffects.htrNeoTick(this));
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (level instanceof ServerLevel serverLevel) {
            registration.remove(this);
            if (plasmaNode != null) UniNodespace.destroyNode(serverLevel, plasmaNode);
        } else if (level != null && level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.orbit.propulsion.client.PropulsionClientEffects.stop(this));
        }
    }

    @Override
    public boolean receivesFusionPower() {
        return true;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        plasmaEnergy = fusionPower;
        // genuinely useful method refactor
        plasmaR = r;
        plasmaG = g;
        plasmaB = b;
    }

    /** Original {@code getConPos}: vier Kuehl-/Stromanschluesse zwei Ebenen tiefer ({@code -rot*5|+rot*1, +-dir*3}). */
    public NodeDirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = PropulsionUtil.rot(dir);
        BlockPos p = worldPosition;

        return new NodeDirPos[] {
                new NodeDirPos(p.offset(-rot.getStepX() * 5 - dir.getStepX() * 3, -2, -rot.getStepZ() * 5 - dir.getStepZ() * 3), dir.getOpposite()),
                new NodeDirPos(p.offset(-rot.getStepX() * -1 - dir.getStepX() * 3, -2, -rot.getStepZ() * -1 - dir.getStepZ() * 3), dir.getOpposite()),
                new NodeDirPos(p.offset(-rot.getStepX() * 5 - dir.getStepX() * -3, -2, -rot.getStepZ() * 5 - dir.getStepZ() * -3), dir),
                new NodeDirPos(p.offset(-rot.getStepX() * -1 - dir.getStepX() * -3, -2, -rot.getStepZ() * -1 - dir.getStepZ() * -3), dir),
        };
    }

    // ── IPropulsion ──────────────────────────────────────────────────────────────────────────

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public boolean canPerformBurn(int shipMass, double deltaV) {
        fuelCost = SolarSystem.getFuelCost(deltaV, shipMass, 250); // i think this engine *itself* would have a base ISP..?

        if (plasmaEnergySync < fuelCost) return false;
        if (getEnergyStored() < maxPower) return false;
        if (!isCool()) return false;

        return true;
    }

    @Override
    public void addErrors(List<Component> errors) {
        if (plasmaEnergySync < fuelCost) {
            errors.add(Component.literal("Insufficient plasma energy: needs " + BobMathUtil.getShortNumber(fuelCost) + " TU").withStyle(ChatFormatting.RED));
        }

        if (getEnergyStored() < maxPower) {
            errors.add(Component.literal("Insufficient power").withStyle(ChatFormatting.RED));
        }

        if (!isCool()) {
            errors.add(Component.literal("Coolant loop not operational!").withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public float getThrust() {
        return 1_600_000_000.0F;
    }

    @Override
    public int startBurn() {
        isOn = true;
        setEnergyStored(0);
        setChanged();
        return 180;
    }

    @Override
    public int endBurn() {
        isOn = false;
        setChanged();
        return 180;
    }

    public boolean isFacingPrograde() {
        return dir() == Direction.SOUTH;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── NBT / Sync ───────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);

        nbt.putLong("plasma", plasmaEnergySync);
        nbt.putBoolean("on", isOn);
        nbt.putInt("fuelCost", fuelCost);
        nbt.putFloat("soundtime", soundtime);

        nbt.putFloat("plasmaR", plasmaR);
        nbt.putFloat("plasmaG", plasmaG);
        nbt.putFloat("plasmaB", plasmaB);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);

        plasmaEnergy = nbt.getLong("plasma");
        isOn = nbt.getBoolean("on");
        fuelCost = nbt.getInt("fuelCost");
        soundtime = nbt.getFloat("soundtime");

        plasmaR = nbt.getFloat("plasmaR");
        plasmaG = nbt.getFloat("plasmaG");
        plasmaB = nbt.getFloat("plasmaB");
    }

    // ── sonstiges ────────────────────────────────────────────────────────────────────────────

    @Override protected Component getDefaultName() { return Component.translatable("container.htrfneo"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-11..x+12, y-2..y+3, z-11..z+12. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 11, worldPosition.getY() - 2, worldPosition.getZ() - 11,
                worldPosition.getX() + 12, worldPosition.getY() + 3, worldPosition.getZ() + 12);
    }
}
