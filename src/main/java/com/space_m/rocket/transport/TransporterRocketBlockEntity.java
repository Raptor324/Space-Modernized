package com.space_m.rocket.transport;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.util.ParticleUtil;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.fluid.trait.FT_Rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityTransporterRocket}: Raketen-Transporter-Rampe. 16 Item-Plaetze (8 senden, 8
 * empfangen), 8 Tanks a 128 000 mB (4 senden, 4 empfangen), 2 Treibstofftanks a 64 000 mB (Wasserstoff/Sauerstoff,
 * per Identifikator aenderbar). Eine Rakete pendelt zwischen den beiden Rampen: nur die Rampe mit Rakete kann senden,
 * und nur wenn die Gegenseite ihre Rakete fertig gestartet hat ({@code launchTicks >= 100}).
 *
 * <p>Startbedingung ({@link #canSend}): Rakete steht ({@code launchTicks <= -20}), Masse >= Schwelle
 * (2^(Schwelle-1), Schieberegler 0..10), Treibstoff mit {@code FT_Rocket} (Tank 8, sonst 9) und in beiden Tanks
 * mindestens {@code min(64000, SolarSystem.getCostBetween(von, nach, Masse*100, Schub, ISP, Orbit, Orbit))}.
 * Nach dem Senden wird mit der tatsaechlich gesendeten Masse abgerechnet und die Rakete wechselt die Seite.
 * Client: Gasflamme unter der aufsteigenden/landenden Rakete, die ersten 10 Ticks Schockwelle.
 * Original-Klasse: TileEntityTransporterRocket
 */
public class TransporterRocketBlockEntity extends TransporterBlockEntityBase {

    public boolean hasRocket = true;
    public int launchTicks = 0;

    public int threshold = 0;

    private static final int MASS_MULT = 100;

    public TransporterRocketBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.TRANSPORTER_ROCKET.get(), pos, state, 16, 8, 128_000, 0, 2, 64_000);

        tanks[8].setTankType(ModFluids.HYDROGEN.getSource());
        tanks[9].setTankType(ModFluids.OXYGEN.getSource());
    }

    @Override
    protected void serverTick(net.minecraft.server.level.ServerLevel level) {
        super.serverTick(level);

        // If our transporter state sync is incorrect, fix whichever one gets updated first
        if (linkedTransporter instanceof TransporterRocketBlockEntity other) {
            if (hasRocket == other.hasRocket) {
                hasRocket = !hasRocket;
            }
        }

        updateLaunchTicks();
    }

    @Override
    protected void clientTick() {
        updateLaunchTicks();

        if (level != null && launchTicks > 0 && launchTicks < 100) {
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY(), z = worldPosition.getZ() + 0.5;
            ParticleUtil.spawnGasFlame(level, x, y + 0.5 + launchTicks, z, 0.0, -1.0, 0.0);

            if (launchTicks < 10) {
                TransportClientEffects.shock(level, x, y, z, 1 + level.random.nextInt(3), 1 + level.random.nextGaussian());
            }
        }
    }

    private void updateLaunchTicks() {
        launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);
    }

    public int getThreshold() {
        return threshold == 0 ? 0 : (int) Math.pow(2, threshold - 1);
    }

    @Nullable
    private FT_Rocket fuelStats() {
        FT_Rocket fuelStats = FluidType.getTrait(tanks[8].getTankType(), FT_Rocket.class);
        if (fuelStats == null) fuelStats = FluidType.getTrait(tanks[9].getTankType(), FT_Rocket.class);
        return fuelStats;
    }

    private int sendCost(TransporterBlockEntityBase linked, int mass, FT_Rocket fuelStats) {
        CelestialBody.Target from = CelestialBody.getTarget(level, worldPosition.getX(), worldPosition.getZ());
        CelestialBody.Target to = CelestialBody.getTarget(linked.getLevel(), linked.getBlockPos().getX(), linked.getBlockPos().getZ());

        return Math.min(64_000, SolarSystem.getCostBetween(from.body, to.body, mass * MASS_MULT, (int) fuelStats.getThrust(),
                fuelStats.getISP(), from.inOrbit, to.inOrbit));
    }

    // Check that we have enough fuel to send to our destination
    @Override
    protected boolean canSend(TransporterBlockEntityBase linkedTransporter) {
        if (launchTicks > -20) return false;
        if (!(linkedTransporter instanceof TransporterRocketBlockEntity other) || other.launchTicks < 100) return false;
        if (!hasRocket) return false;

        int mass = itemCount();
        if (mass < getThreshold()) return false;

        FT_Rocket fuelStats = fuelStats();
        if (fuelStats == null) return false;

        int sendCost = sendCost(linkedTransporter, mass, fuelStats);

        return tanks[8].getFill() >= sendCost && tanks[9].getFill() >= sendCost;
    }

    @Override
    protected void hasSent(TransporterBlockEntityBase linkedTransporter, int quantitySent) {
        // Recalculate send cost from what was actually successfully sent
        FT_Rocket fuelStats = fuelStats();
        if (fuelStats != null) {
            int sendCost = sendCost(linkedTransporter, quantitySent, fuelStats);

            tanks[8].setFill(tanks[8].getFill() - sendCost);
            tanks[9].setFill(tanks[9].getFill() - sendCost);
        }

        hasRocket = false;
        ((TransporterRocketBlockEntity) linkedTransporter).hasRocket = true;
    }

    @Override
    protected void hasConnected(TransporterBlockEntityBase linkedTransporter) {
        hasRocket = true;
        if (linkedTransporter instanceof TransporterRocketBlockEntity other) other.hasRocket = false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("container.transporter"); // Original getName() ohne Lang-Eintrag
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.space_m.transporter_rocket");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TransporterRocketMenu(id, inv, this);
    }

    /** Original {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }

    @Override
    protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putBoolean("rocket", hasRocket);
        nbt.putInt("threshold", threshold);
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        hasRocket = nbt.getBoolean("rocket");
        threshold = nbt.getInt("threshold");
    }

    // ==================== Anschluesse (Original DirPos, dir = Blickrichtung, rot = dir.getRotation(UP)) ====================

    private Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    private static BlockPos at(BlockPos p, Direction dir, int d, Direction rot, int r, int up) {
        return p.offset(dir.getStepX() * d + rot.getStepX() * r, up, dir.getStepZ() * d + rot.getStepZ() * r);
    }

    @Override
    protected DirPos[] getConPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;

        return new DirPos[] {
            new DirPos(at(p, dir, 2, rot, -1, 0), dir.getOpposite()),
            new DirPos(at(p, dir, 2, rot, 1, 0), dir.getOpposite()),
            new DirPos(at(p, dir, -2, rot, -1, 0), dir),
            new DirPos(at(p, dir, -2, rot, 1, 0), dir),

            new DirPos(at(p, dir, 1, rot, -1, 1), Direction.UP),
            new DirPos(at(p, dir, 1, rot, 1, 1), Direction.UP),
            new DirPos(at(p, dir, -1, rot, -1, 1), Direction.UP),
            new DirPos(at(p, dir, -1, rot, 1, 1), Direction.UP),
        };
    }

    @Override
    protected DirPos[] getTankPos() {
        Direction dir = dir();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;

        return new DirPos[] {
            new DirPos(at(p, dir, 1, rot, -3, 0), rot),
            new DirPos(at(p, dir, -1, rot, -3, 0), rot),
        };
    }

    @Override
    protected DirPos[] getInsertPos() {
        Direction dir = dir();
        return new DirPos[] { new DirPos(at(worldPosition, dir, -2, dir, 0, 0), dir) };
    }

    @Override
    protected DirPos[] getExtractPos() {
        Direction dir = dir();
        return new DirPos[] { new DirPos(at(worldPosition, dir, 2, dir, 0, 0), dir.getOpposite()) };
    }

    @Override
    public void receiveControl(CompoundTag nbt) {
        super.receiveControl(nbt);
        if (nbt.contains("threshold")) {
            threshold = nbt.getInt("threshold");
            setChanged();
        }
    }
}
