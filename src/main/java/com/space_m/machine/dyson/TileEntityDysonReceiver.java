package com.space_m.machine.dyson;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectStandard;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm_m.item.ISatChip;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.util.BobMathUtil;
import com.space_m.celestial.trait.CBT_Dyson;
import com.space_m.machine.misc.R7MachineBlock;
import com.space_m.satellite.SatelliteDysonRelay;
import com.space_m.satellite.SpaceSatellites;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityDysonReceiver}: empfaengt tagsueber (bzw. nachts, wenn ein Dyson-Relais auf der
 * Chip-Frequenz den Koerper umkreist) die Energie des Schwarms und schiesst sie als Strahl (Richtung = von der Front weg,
 * Hoehe y+1, Felder 9..23) auf einen Wandler ({@link IDysonConverter}, ab Feld 11, auch ueber Mehrblockteile); trifft er
 * etwas anderes Undurchsichtiges oder ein Blockentity, das nicht annimmt, wird es entfernt und explodiert (VNT Groesse 3,
 * Feuer). Lebewesen im Strahl werden mit {@code energy / 1000} Schaden gesprengt ({@link CustomDamageHandlerDyson}).
 * Schattierung: Hoehenkarte im 7x7-Umkreis hoeher als y+10 -> kein Empfang. Leistungskurve (Gompertz):
 * {@link #getEnergyOutput}. Client: Schleife {@code block.dysonBeam} (Lautstaerke 0.75, Reichweite 20, Tonhoehe 0.85),
 * Lichtfunken ueber der Schuessel.
 * Original-Klasse: TileEntityDysonReceiver
 */
public class TileEntityDysonReceiver extends BaseMachineBlockEntity {

    // Connects to a dyson swarm via ID, receiving energy during the day
    // also receives energy at night if a satellite relay is in orbit around the planet

    // The energy received is fired as a violently powerful beam,
    // converters can collect this beam and turn it into HE/TU or used for analysis, crafting, etc.

    public boolean isReceiving;
    public int swarmId;
    public int swarmCount;
    public int swarmConsumers;
    public int beamLength;

    public TileEntityDysonReceiver(BlockPos pos, BlockState state) {
        super(DysonBlockEntities.RECEIVER.get(), pos, state, 1, 0L, 0L);
    }

    // Sun luminosity is 4*10^26, which we can't represent in any Java integer primitive
    // therefore the upper bound for power generation is higher than a FEnSU, effectively
    // reality doesn't provide any interesting solutions that make the system engaging to use
    // so we're going to build our own power curve.
    // We need to encourage players to build large swarms, so single satellites must suck but together they produce enormous power
    // Gompertz is a funne name
    public static long getEnergyOutput(int swarmCount) {
        double adjustedDensity = (double) swarmCount / 1024.0D;
        long maxOutput = Long.MAX_VALUE / 10;
        double b = 32.0D;
        double c = 1.3D;
        double gompertz = Math.exp(-b * Math.exp(-c * adjustedDensity));
        return (long) (maxOutput * gompertz) / 20;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityDysonReceiver be) {
        Direction dir = R7MachineBlock.dir(state).getOpposite();
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level, pos, dir);
        } else {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.machine.dyson.client.DysonClientEffects.receiverTick(be, dir));
        }
    }

    private void serverTick(ServerLevel level, BlockPos pos, Direction dir) {
        int xCoord = pos.getX(), yCoord = pos.getY(), zCoord = pos.getZ();

        swarmId = ISatChip.getFreqS(inventory.getStackInSlot(0));

        Satellite sat = SpaceSatellites.getData(level, xCoord, zCoord).getSatFromFreq(swarmId);
        int sun = level.getBrightness(LightLayer.SKY, pos) - level.getSkyDarken() - 11;

        boolean occluded = false;
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, xCoord + x, zCoord + z) > yCoord + 10) {
                    occluded = true;
                    break;
                }
            }
        }

        swarmCount = CBT_Dyson.count(level, swarmId);
        swarmConsumers = CBT_Dyson.consumers(level, swarmId);

        isReceiving = (sat instanceof SatelliteDysonRelay || sun > 0) && swarmId > 0 && !occluded && swarmCount > 0 && swarmConsumers > 0;

        if (isReceiving) {
            long energyOutput = getEnergyOutput(swarmCount) / swarmConsumers;
            int maxLength = 24;

            beamLength = maxLength;
            for (int i = 9; i < maxLength; i++) {
                int x = xCoord + dir.getStepX() * i;
                int y = yCoord + 1;
                int z = zCoord + dir.getStepZ() * i;
                BlockPos at = new BlockPos(x, y, z);

                BlockState block = level.getBlockState(at);

                // two block gap minimum
                boolean detonate = true;
                BlockEntity te = null;
                if (i > 10) {
                    te = IDysonConverter.core(level, level.getBlockEntity(at));

                    IDysonConverter converter = IDysonConverter.of(te);
                    if (converter != null) {
                        detonate = !converter.provideEnergy(x, y, z, energyOutput);
                    }
                }

                if (block.isSolidRender(level, at) || te != null) {
                    if (detonate) {
                        level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

                        ExplosionVNT vnt = new ExplosionVNT(level, x, y, z, 3, null);
                        vnt.setBlockAllocator(new BlockAllocatorStandard());
                        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
                        vnt.setEntityProcessor(new EntityProcessorStandard().allowSelfDamage());
                        vnt.setPlayerProcessor(new PlayerProcessorStandard());
                        vnt.setSFX(new ExplosionEffectStandard());
                        vnt.explode();
                    }

                    beamLength = i;
                    break;
                }
            }

            double blx = Math.min(xCoord, xCoord + dir.getStepX() * beamLength) + 0.2;
            double bux = Math.max(xCoord, xCoord + dir.getStepX() * beamLength) + 0.8;
            double bly = Math.min(yCoord, 1 + yCoord + dir.getStepY() * beamLength) + 0.2;
            double buy = Math.max(yCoord, 1 + yCoord + dir.getStepY() * beamLength) + 0.8;
            double blz = Math.min(zCoord, zCoord + dir.getStepZ() * beamLength) + 0.2;
            double buz = Math.max(zCoord, zCoord + dir.getStepZ() * beamLength) + 0.8;

            List<LivingEntity> list = level.getEntitiesOfClass(LivingEntity.class, new AABB(blx, bly, blz, bux, buy, buz));

            for (LivingEntity entity : list) {
                ExplosionVNT vnt = new ExplosionVNT(level, entity.getX() - dir.getStepX(), entity.getY() + 1.5, entity.getZ() - dir.getStepZ(), 3, null);
                vnt.setBlockAllocator(new BlockAllocatorStandard());
                vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
                vnt.setEntityProcessor(new EntityProcessorStandard().withDamageMod(new CustomDamageHandlerDyson(energyOutput)));
                vnt.setPlayerProcessor(new PlayerProcessorStandard());
                vnt.setSFX(new ExplosionEffectStandard());
                vnt.explode();
            }
        }

        setChanged();
        sendUpdateToClient(); // networkPackNT(250)
    }

    // ==================== NBT / Sync ====================

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        // networkPackNT
        nbt.putBoolean("s_receiving", isReceiving);
        nbt.putInt("s_swarmId", swarmId);
        nbt.putInt("s_swarmCount", swarmCount);
        nbt.putInt("s_swarmConsumers", swarmConsumers);
        nbt.putInt("s_beamLength", beamLength);
    }

    @Override
    protected void applyClientUpdate(@NotNull CompoundTag nbt) {
        super.applyClientUpdate(nbt);
        isReceiving = nbt.getBoolean("s_receiving");
        swarmId = nbt.getInt("s_swarmId");
        swarmCount = nbt.getInt("s_swarmCount");
        swarmConsumers = nbt.getInt("s_swarmConsumers");
        beamLength = nbt.getInt("s_beamLength");
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> com.space_m.machine.dyson.client.DysonClientEffects.stop(this));
        }
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    /** Original {@code getName() = "container.machineDysonReceiver"}. */
    @Override protected Component getDefaultName() { return Component.translatable("container.machineDysonReceiver"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    /** Original {@code getRenderBoundingBox}: x-25..x+25, y..y+19, z-25..z+25. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 25, worldPosition.getY(), worldPosition.getZ() - 25,
                worldPosition.getX() + 25, worldPosition.getY() + 19, worldPosition.getZ() + 25);
    }

    /** Original {@code runTests}: Leistung je Schwarmgroesse (1, 2, 4 ... 4096) ins Log. */
    public static void runTests(org.slf4j.Logger logger) {
        for (int i = 1; i < 5000; i *= 2) {
            logger.info(i + " dyson swarm members produces: " + BobMathUtil.getShortNumber(getEnergyOutput(i) * 20) + "HE/s");
        }
    }
}
