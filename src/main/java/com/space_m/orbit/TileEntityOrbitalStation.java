package com.space_m.orbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.fluid.SpaceFluids;
import com.space_m.fluid.trait.FT_Rocket;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;
import com.space_m.rocket.entity.RocketTravelHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityOrbitalStation}: Andockport einer Raumstation (Hauptport = Kern {@code orbital_station}
 * oder Zusatzport {@code orbital_station_port}).
 *
 * <p>Pro Tick (nur im Orbit): Station unter dem Port holen, der Kern laesst sie {@link OrbitalStation#update laufen},
 * Port anmelden. Angedockte wiederverwendbare Kapsel ({@code rp_pod_20}): Hydrazin-Tank auf den Bedarf zum Abstieg
 * (bzw. 500 mB fuer Station -> Station) vergroessern, an 12 Anschlusszellen Hydrazin beziehen, genug Treibstoff ->
 * {@code NEEDSFUEL} -> {@code AWAITING}/{@code LANDED}. Beim Abdocken ({@code UNDOCKING}) wird der Verbrauch abgezogen.
 * 16 Lagerplaetze fuer eingelagerte Raketen/Laufwerke.
 *
 * <p>Abweichungen (technisch): Sync ueber das Block-Update-NBT statt {@code networkPackNT(512)} (Stationsdaten selbst
 * ueber {@link StationSyncPacket}); die angedockte Rakete wird nur beim Abbau des Ports gedroppt (Original
 * {@code invalidate} lief nur beim Abbau, modern auch beim Entladen des Chunks).
 * Original-Klasse: TileEntityOrbitalStation
 */
public class TileEntityOrbitalStation extends BaseMachineBlockEntity
        implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2, IDockingPort, RocketTravelHooks.RocketPort {

    public static final int SLOT_COUNT = 16;

    @Nullable private OrbitalStation station;
    @Nullable private EntityRideableRocket docked;

    private final FluidTank[] tanks;

    public boolean isReserved = false;

    // Client synced state information
    public boolean hasDocked = false;
    public boolean hasRider = false;
    public boolean needsFuel = false;
    public boolean hasFuel = false;

    public float rot;
    public float prevRot;

    private int lastSyncHash;

    public TileEntityOrbitalStation(BlockPos pos, BlockState state) {
        super(OrbitBlocks.ORBITAL_STATION_BE.get(), pos, state, SLOT_COUNT, 0L, 0L);
        tanks = new FluidTank[1];
        tanks[0] = new FluidTank(SpaceFluids.HYDRAZINE.fluid(), 16_000);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.orbitalStation");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return null; // kein GUI (Original ohne IGUIProvider)
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    // ── Tick ───────────────────────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStation be) {
        be.updateEntity(level);
    }

    private void updateEntity(Level level) {
        if (!CelestialBody.inOrbit(level)) return;

        if (!level.isClientSide) {
            // Station TEs handle syncing information about the current orbital parameters to players on the station
            station = OrbitalStation.getStationFromPosition(worldPosition.getX(), worldPosition.getZ());

            if (isCore()) station.update(level);
            station.addPort(this);

            if (docked != null && docked.isReusable()) {
                int fillRequirement = getFillRequirement(false); // Use higher fill requirement for tank sizing

                // Update tank sizes based on fuel requirement, preserving existing fills
                for (FluidTank tank : tanks) {
                    tank.changeTankSize(Math.max(fillRequirement, tank.getFill()));
                }

                // Connections
                for (ConPos pos : getConPos()) {
                    for (FluidTank tank : tanks) {
                        if (tank.getTankType() == com.hbm_m.inventory.fluid.ModFluids.NONE.getSource()) continue;
                        trySubscribe(tank.getTankType(), level, pos.pos(), pos.dir());
                    }
                }
            }

            if (docked != null && (docked.isRemoved() || docked.getState() == RocketState.UNDOCKING)) {
                // Drain tanks only upon successful undocking, just in case the pod gets stashed before the player can launch (in multi-player stations)
                if (!docked.isRemoved() && docked.isReusable()) {
                    boolean toOrbit = docked.getTarget().inOrbit();
                    for (FluidTank tank : tanks) tank.changeTankSize(Math.max(0, tank.getFill() - getFillRequirement(toOrbit)));
                }

                undockRocket();
            }

            // if we have enough fuel, transition to ready to launch
            if (docked != null && docked.isReusable()) {
                boolean hasFuel = hasSufficientFuel(docked.getTarget().inOrbit());

                if (hasFuel && docked.getState() == RocketState.NEEDSFUEL) {
                    docked.setState(!docked.navDrive.isEmpty() ? RocketState.AWAITING : RocketState.LANDED);
                } else if (!hasFuel && docked.getState() != RocketState.NEEDSFUEL) {
                    docked.setState(RocketState.NEEDSFUEL);
                }
            }

            hasDocked = docked != null;
            hasRider = hasDocked && !docked.getPassengers().isEmpty();
            needsFuel = hasDocked && docked.isReusable();
            hasFuel = needsFuel && hasSufficientFuel(docked.getTarget().inOrbit());

            if (hasDocked) isReserved = false;

            // networkPackNT(STATION_SIZE / 2): nur bei Aenderung bzw. jede Sekunde
            int hash = syncHash();
            if (hash != lastSyncHash || level.getGameTime() % 20 == 0) {
                lastSyncHash = hash;
                setChanged();
                sendUpdateToClient();
            }
        } else {
            prevRot = rot;
            if (hasDocked) {
                rot += 2.25F;
                if (rot > 90) rot = 90;
            } else {
                rot -= 2.25F;
                if (rot < 0) rot = 0;
            }
        }
    }

    private int syncHash() {
        int h = (hasDocked ? 1 : 0) | (hasRider ? 2 : 0) | (needsFuel ? 4 : 0) | (hasFuel ? 8 : 0);
        for (FluidTank tank : tanks) h = h * 31 + tank.getFill() * 7 + tank.getMaxFill();
        for (int i = 0; i < inventory.getSlots(); i++) h = h * 31 + inventory.getStackInSlot(i).getItem().hashCode();
        return h;
    }

    /** Original {@code invalidate()} beim Abbau: Port abmelden, angedockte Rakete fallen lassen. */
    public void onBroken() {
        if (level != null && !level.isClientSide) {
            if (station != null && !isCore()) station.removePort(this);
            if (docked != null) docked.dropNDie(null);
            docked = null;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide && station != null && !isCore()) station.removePort(this);
    }

    public boolean isCore() {
        return getBlockState().getBlock() == OrbitBlocks.ORBITAL_STATION.get();
    }

    public record ConPos(BlockPos pos, Direction dir) {}

    /** Original {@code getConPos} (Richtungen 1:1, auch die vertauschten NORTH/SOUTH der z-Seiten). */
    public ConPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new ConPos[] {
            new ConPos(new BlockPos(x - 1, y + 1, z + 3), Direction.NORTH),
            new ConPos(new BlockPos(x + 0, y + 1, z + 3), Direction.NORTH),
            new ConPos(new BlockPos(x + 1, y + 1, z + 3), Direction.NORTH),

            new ConPos(new BlockPos(x - 1, y + 1, z - 3), Direction.SOUTH),
            new ConPos(new BlockPos(x + 0, y + 1, z - 3), Direction.SOUTH),
            new ConPos(new BlockPos(x + 1, y + 1, z - 3), Direction.SOUTH),

            new ConPos(new BlockPos(x + 3, y + 1, z - 1), Direction.EAST),
            new ConPos(new BlockPos(x + 3, y + 1, z + 0), Direction.EAST),
            new ConPos(new BlockPos(x + 3, y + 1, z + 1), Direction.EAST),

            new ConPos(new BlockPos(x - 3, y + 1, z - 1), Direction.WEST),
            new ConPos(new BlockPos(x - 3, y + 1, z + 0), Direction.WEST),
            new ConPos(new BlockPos(x - 3, y + 1, z + 1), Direction.WEST),
        };
    }

    // ── Andocken ───────────────────────────────────────────────────────────────────────────────

    public void enterCapsule(Player player) {
        if (docked == null || !docked.getPassengers().isEmpty()) return;
        docked.interact(player, net.minecraft.world.InteractionHand.MAIN_HAND);
    }

    @Override
    public void dockRocket(EntityRideableRocket rocket) {
        despawnRocket();

        docked = rocket;
    }

    public void undockRocket() {
        docked = null;
    }

    @Override
    public void despawnRocket() {
        if (docked != null) {
            Stack<ItemStack> itemsToStuff = new Stack<>();

            RocketStruct rocket = docked.getRocket();
            if (rocket.stages.size() > 0) {
                itemsToStuff.push(ItemCustomRocket.build(docked.getRocket(), true));
            } else {
                itemsToStuff.push(new ItemStack(rocket.capsule.part));
            }

            if (!docked.navDrive.isEmpty()) itemsToStuff.push(docked.navDrive.copy());

            for (int i = 0; i < inventory.getSlots(); i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, itemsToStuff.pop());
                    if (itemsToStuff.empty()) break;
                }
            }

            docked.discard();
            docked = null;
        }
    }

    @Override
    public void reservePort() {
        isReserved = true;
    }

    @Override
    public BlockPos pos() {
        return worldPosition;
    }

    public void spawnRocket(ItemStack stack) {
        if (level == null) return;
        EntityRideableRocket rocket = EntityRideableRocket.create(level, worldPosition.getX() + 0.5F, worldPosition.getY() + 1.5F, worldPosition.getZ() + 0.5F, stack);
        rocket.setPos(rocket.getX(), rocket.getY() - rocket.getBbHeight(), rocket.getZ());
        rocket.setState(rocket.isReusable() ? RocketState.NEEDSFUEL : RocketState.LANDED);
        level.addFreshEntity(rocket);

        dockRocket(rocket);
    }

    @Nullable
    public EntityRideableRocket getDocked() {
        return docked;
    }

    public boolean hasStoredItems() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) return true;
        }

        return false;
    }

    public void giveStoredItems(Player player) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ItemStack copy = stack.copy();
                if (!player.getInventory().add(copy)) {
                    player.drop(copy, false);
                }
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        player.inventoryMenu.broadcastChanges();
        setChanged();
    }

    /** Fuer das Blick-Overlay (Original {@code slots}, per Sync nur die Item-IDs). */
    public List<ItemStack> clientSlots() {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.getSlots(); i++) list.add(inventory.getStackInSlot(i));
        return list;
    }

    public boolean hasSufficientFuel(boolean toOrbit) {
        int fillRequirement = getFillRequirement(toOrbit);
        for (FluidTank tank : tanks) {
            if (tank.getFill() < fillRequirement) return false;
        }

        return true;
    }

    public int getFillRequirement(boolean toOrbit) {
        if (toOrbit) return 500; // Transferring between stations is much cheaper

        int mass = docked != null ? docked.getRocket().getLaunchMass() : 4_000;

        FT_Rocket fuelTrait = FluidType.getTrait(tanks[0].getTankType(), FT_Rocket.class);
        int thrust = fuelTrait != null ? (int) fuelTrait.getThrust() : 600_000;
        int isp = fuelTrait != null ? fuelTrait.getISP() : 300;

        CelestialBody orbiting = station != null ? station.orbiting : CelestialBody.getBody("kerbin");
        return SolarSystem.getCostBetween(orbiting, orbiting, mass, thrust, isp, false, true);
    }

    // ── NBT / Sync ─────────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
        nbt.putBoolean("hasDocked", hasDocked);
        nbt.putBoolean("hasRider", hasRider);
        nbt.putBoolean("needsFuel", needsFuel);
        nbt.putBoolean("hasFuel", hasFuel);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);

        // Migrate old hydrogen tanks
        tanks[0].setTankType(SpaceFluids.HYDRAZINE.fluid());

        hasDocked = nbt.getBoolean("hasDocked");
        hasRider = nbt.getBoolean("hasRider");
        needsFuel = nbt.getBoolean("needsFuel");
        hasFuel = nbt.getBoolean("hasFuel");
    }

    @Nullable private AABB bb = null;

    @Override
    public AABB getRenderBoundingBox() {
        if (bb == null) {
            bb = new AABB(worldPosition.getX() - 2, worldPosition.getY() - 2, worldPosition.getZ() - 2,
                    worldPosition.getX() + 3, worldPosition.getY() + 2, worldPosition.getZ() + 3);
        }

        return bb;
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Nur fuer Tests: Tank. */
    public FluidTank tank() { return tanks[0]; }
}
