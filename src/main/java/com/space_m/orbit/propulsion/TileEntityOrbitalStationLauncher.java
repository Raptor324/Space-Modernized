package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ISatChip;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.RenderBounds;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.orbit.IDockingPort;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.SpaceRocketItems;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;
import com.space_m.rocket.pad.TileEntityLaunchPadRocket;
import com.space_m.rocket.pad.TileEntityLaunchPadRocket.SolidFuelTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityOrbitalStationLauncher}: Raketenbau und -start direkt im Orbit (verhaelt sich "fast wie
 * ein Port", meldet sich aber nicht als Dockziel an).
 *
 * <p>Slots: 0 Ziel-Laufwerk, 1/2 Treibstoff rein/raus (auch Festbrennstoff {@code rocket_fuel}, je 250 kg), 3 Kapsel,
 * 4..18 je Stufe Rumpf/Beine/Triebwerk, 19..28 je Stufe Start-/Ziel-Laufwerk (Geisterslots, beim Abbau nicht
 * fallengelassen). Stapelgrenze 8. Jeden Tick (nur im Orbit) wird aus den Slots die Rakete gebaut (leere Stufen rutschen
 * nach oben), die Tanks werden nach dem Treibstoffbedarf angelegt ({@code TileEntityLaunchPadRocket.updateStorageTanks}),
 * alle 20 Ticks an den zwoelf Seitenanschluessen ({@code y+1}, je drei je Seite bei Abstand 3) angefordert. Start
 * ("launch" ueber {@code NBTControlPacket}): Rakete gueltig, Laufwerk, alle Tanks voll, Ziel erreichbar -> Rakete
 * {@code y + 1.5 - Hoehe} gespawnt und angedockt, Fuellungen/Slots verbraucht. Angedockte Rakete: Rechtsklick steigt ein.
 * Client: Andockarme drehen ({@code rot} +-2,25 bis 90).
 * Original-Klasse: TileEntityOrbitalStationLauncher
 */
public class TileEntityOrbitalStationLauncher extends BaseMachineBlockEntity implements IControlReceiver, IFluidStandardReceiverMK2, IDockingPort {

    public static final int SLOT_DRIVE = 0;
    public static final int SLOT_IN = 1;
    public static final int SLOT_OUT = 2;
    public static final int SLOT_CAPSULE = 3;
    /** launch: drive + fuel in + fuel out; construction: capsule + stages + program drives */
    public static final int SLOT_COUNT = 1 + 1 + 1 + 1 + RocketStruct.MAX_STAGES * 3 + RocketStruct.MAX_STAGES * 2;

    public RocketStruct rocket = new RocketStruct();

    @Nullable private EntityRideableRocket docked;

    public FluidTank[] tanks;
    public SolidFuelTank solidFuel = new SolidFuelTank();

    public float rot;
    public float prevRot;

    public int currentStage;

    public boolean isBreaking;

    // Client synced state information
    public boolean hasDocked = false;
    public boolean hasRider = false;

    public TileEntityOrbitalStationLauncher(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.ORBITAL_STATION_LAUNCHER.get(), pos, state, SLOT_COUNT, 0, 0);

        tanks = new FluidTank[RocketStruct.MAX_STAGES * 2]; // enough tanks for any combination of rocket stages
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTank(none(), 64_000);
    }

    private static Fluid none() {
        return ModFluids.NONE.getSource();
    }

    private static boolean isNone(Fluid f) {
        return f == none() || f == net.minecraft.world.level.material.Fluids.EMPTY;
    }

    /** Original {@code getInventoryStackLimit() = 8}. */
    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isItemValidForSlot(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 8;
            }
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.orbitalStationLauncher");
    }

    private ItemStack slot(int i) {
        return inventory.getStackInSlot(i);
    }

    public ItemStack fromDrive(int stage) {
        return slot(SLOT_COUNT - (RocketStruct.MAX_STAGES - stage) * 2);
    }

    public ItemStack toDrive(int stage) {
        return slot(SLOT_COUNT - (RocketStruct.MAX_STAGES - stage) * 2 + 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStationLauncher be) {
        if (!CelestialBody.inOrbit(level)) return;

        if (!level.isClientSide) {
            be.serverTick(level);
        } else {
            be.prevRot = be.rot;
            if (be.hasDocked) {
                be.rot += 2.25F;
                if (be.rot > 90) be.rot = 90;
            } else {
                be.rot -= 2.25F;
                if (be.rot < 0) be.rot = 0;
            }
        }
    }

    private void serverTick(Level level) {
        // This TE acts almost entirely like a port, except doesn't register itself so nothing actually tries to dock here

        // ROCKET CONSTRUCTION //
        // Setup the constructed rocket
        ItemStack fromStack = fromDrive(currentStage);
        ItemStack toStack = toDrive(currentStage);

        // updates the orbital station information and syncs it to the client, if necessary
        ItemVOTVdrive.getTarget(slot(SLOT_DRIVE), level);
        ItemVOTVdrive.getTarget(fromStack, level);
        ItemVOTVdrive.getTarget(toStack, level);

        rocket = new RocketStruct(slot(SLOT_CAPSULE));
        if (!slot(SLOT_CAPSULE).isEmpty() && slot(SLOT_CAPSULE).getItem() instanceof ISatChip) {
            rocket.satFreq = ISatChip.getFreqS(slot(SLOT_CAPSULE));
        }
        for (int i = 4; i < RocketStruct.MAX_STAGES * 3 + 3; i += 3) {
            if (slot(i).isEmpty() && slot(i + 1).isEmpty() && slot(i + 2).isEmpty()) {
                // Check for later stages and shift them up into empty stages
                if (i + 3 < RocketStruct.MAX_STAGES * 3 && (!slot(i + 3).isEmpty() || !slot(i + 4).isEmpty() || !slot(i + 5).isEmpty())) {
                    inventory.setStackInSlot(i, slot(i + 3));
                    inventory.setStackInSlot(i + 1, slot(i + 4));
                    inventory.setStackInSlot(i + 2, slot(i + 5));
                    inventory.setStackInSlot(i + 3, ItemStack.EMPTY);
                    inventory.setStackInSlot(i + 4, ItemStack.EMPTY);
                    inventory.setStackInSlot(i + 5, ItemStack.EMPTY);
                } else {
                    break;
                }
            }
            rocket.addStage(slot(i), slot(i + 1), slot(i + 2));
        }

        // ROCKET LAUNCHING
        updateTanks();

        // Connections
        if (level.getGameTime() % 20 == 0) {
            for (PropulsionUtil.DirPos pos : getConPos()) {
                if (rocket.validate()) {
                    for (FluidTank tank : tanks) {
                        if (isNone(tank.getTankType())) continue;
                        trySubscribe(tank.getTankType(), level, pos.pos(), pos.dir());
                    }
                }
            }
        }

        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = slot(i);
        for (FluidTank tank : tanks) tank.loadTank(SLOT_IN, SLOT_OUT, slots);
        for (int i : new int[] { SLOT_IN, SLOT_OUT }) {
            if (inventory.getStackInSlot(i) != slots[i]) inventory.setStackInSlot(i, slots[i]);
        }

        if (!slot(SLOT_IN).isEmpty() && slot(SLOT_IN).is(ModItems.ROCKET_FUEL.get()) && solidFuel.level < solidFuel.max) {
            inventory.extractItem(SLOT_IN, 1, false);
            solidFuel.level += 250;
            if (solidFuel.level > solidFuel.max) solidFuel.level = solidFuel.max;
        }

        if (docked != null && (docked.isRemoved() || docked.getState() == RocketState.UNDOCKING)) {
            undockRocket();
        }

        hasDocked = docked != null;
        hasRider = hasDocked && !docked.getPassengers().isEmpty();

        setChanged();
        sendUpdateToClient(); // networkPackNT(250)
    }

    public void enterCapsule(Player player) {
        if (docked == null || !docked.getPassengers().isEmpty()) return;
        docked.interact(player, InteractionHand.MAIN_HAND);
    }

    @Override
    public void dockRocket(EntityRideableRocket rocket) {
        docked = rocket;
    }

    public void undockRocket() {
        docked = null;
    }

    private boolean hasDrive() {
        return !slot(SLOT_DRIVE).isEmpty() && slot(SLOT_DRIVE).getItem() instanceof ItemVOTVdrive;
    }

    private boolean areTanksFull() {
        for (FluidTank tank : tanks) if (!isNone(tank.getTankType()) && tank.getFill() < tank.getMaxFill()) return false;
        if (solidFuel.level < solidFuel.max) return false;
        return true;
    }

    private boolean isCapsule() {
        return rocket.capsule != null && rocket.capsule.part == SpaceRocketItems.RP_CAPSULE_20.get();
    }

    private boolean isStationCore() {
        return rocket.capsule != null && rocket.capsule.part == SpaceRocketItems.RP_STATION_CORE_20.get();
    }

    private boolean canReachDestination() {
        if (level == null) return false;

        // Check that the drive is processed
        if (!ItemVOTVdrive.getProcessed(slot(SLOT_DRIVE))) {
            return false;
        }

        SolarSystem.Body target = ItemVOTVdrive.getDestination(slot(SLOT_DRIVE)).body();
        if (target == SolarSystem.Body.ORBIT && !isCapsule() && !isStationCore())
            return false;

        CelestialBody.Target from = CelestialBody.getTarget(level, worldPosition.getX(), worldPosition.getZ());
        ItemVOTVdrive.Target to = ItemVOTVdrive.getTarget(slot(SLOT_DRIVE), level);

        if (!to.isValid() && !isStationCore()) return false;
        if (to.isValid() && isStationCore()) return false;
        if (to.body() == null) return false;

        // Check if the stage can make the journey
        return rocket.hasSufficientFuel(from.body, to.body(), from.inOrbit, to.inOrbit());
    }

    public boolean canLaunch() {
        return rocket.validate() && hasDrive() && areTanksFull() && canReachDestination();
    }

    public void launch(@Nullable Player player) {
        if (!canLaunch() || level == null) return;

        ItemStack stack = ItemCustomRocket.build(rocket);

        EntityRideableRocket entity = EntityRideableRocket.create(level, worldPosition.getX() + 0.5F, worldPosition.getY() + 1.5F, worldPosition.getZ() + 0.5F, stack)
                .withProgram(slot(SLOT_DRIVE)).launchedBy(player);
        entity.setPos(entity.getX(), entity.getY() - entity.getBbHeight(), entity.getZ());
        level.addFreshEntity(entity);

        // Deplete all fills
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTank(none(), 64_000);
        solidFuel.level = solidFuel.max = 0;

        inventory.setStackInSlot(SLOT_DRIVE, ItemStack.EMPTY);
        for (int i = SLOT_CAPSULE; i < SLOT_COUNT; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);

        dockRocket(entity);
        setChanged();
    }

    /** Original {@code getSizeInventory}: beim Abbau ({@code isBreaking}) ohne die Laufwerk-Geisterslots. */
    @Override
    public void dropInventoryContents() {
        if (level == null) return;
        int size = isBreaking ? SLOT_COUNT - RocketStruct.MAX_STAGES * 2 : SLOT_COUNT;
        net.minecraft.world.SimpleContainer c = new net.minecraft.world.SimpleContainer(size);
        for (int i = 0; i < size; i++) c.setItem(i, inventory.getStackInSlot(i));
        net.minecraft.world.Containers.dropContents(level, worldPosition, c);
    }

    @Override
    protected boolean isItemValidForSlot(int index, ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (index == SLOT_DRIVE && !(stack.getItem() instanceof ItemVOTVdrive)) return false;
        if (index == SLOT_IN && !stack.is(ModItems.ROCKET_FUEL.get())) return false;
        return true;
    }

    /** Fuer die GUI-Slots (Original SlotNonRetarded/Slot fragen isItemValidForSlot). */
    public boolean isItemValid(int index, ItemStack stack) {
        return isItemValidForSlot(index, stack);
    }

    private void updateTanks() {
        if (!rocket.validate()) return;
        TileEntityLaunchPadRocket.updateStorageTanks(rocket, tanks, solidFuel, false);
    }

    /** Original {@code getConPos}: je drei Felder in Abstand 3 auf allen vier Seiten, eine Ebene hoeher. */
    public PropulsionUtil.DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new PropulsionUtil.DirPos[] {
                new PropulsionUtil.DirPos(p.offset(-1, 1, 3), Direction.NORTH),
                new PropulsionUtil.DirPos(p.offset(0, 1, 3), Direction.NORTH),
                new PropulsionUtil.DirPos(p.offset(1, 1, 3), Direction.NORTH),

                new PropulsionUtil.DirPos(p.offset(-1, 1, -3), Direction.SOUTH),
                new PropulsionUtil.DirPos(p.offset(0, 1, -3), Direction.SOUTH),
                new PropulsionUtil.DirPos(p.offset(1, 1, -3), Direction.SOUTH),

                new PropulsionUtil.DirPos(p.offset(3, 1, -1), Direction.EAST),
                new PropulsionUtil.DirPos(p.offset(3, 1, 0), Direction.EAST),
                new PropulsionUtil.DirPos(p.offset(3, 1, 1), Direction.EAST),

                new PropulsionUtil.DirPos(p.offset(-3, 1, -1), Direction.WEST),
                new PropulsionUtil.DirPos(p.offset(-3, 1, 0), Direction.WEST),
                new PropulsionUtil.DirPos(p.offset(-3, 1, 1), Direction.WEST),
        };
    }

    public List<Component> findIssues() {
        List<Component> issues = new ArrayList<>();

        if (!rocket.validate() || level == null) return issues;

        TileEntityLaunchPadRocket.findTankIssues(issues, tanks, solidFuel);
        if (TileEntityLaunchPadRocket.findDriveIssues(issues, rocket, slot(SLOT_DRIVE))) return issues;

        // Check that the rocket is actually capable of reaching our destination
        CelestialBody.Target from = CelestialBody.getTarget(level, worldPosition.getX(), worldPosition.getZ());
        ItemVOTVdrive.Target to = ItemVOTVdrive.getTarget(slot(SLOT_DRIVE), level);

        TileEntityLaunchPadRocket.findTravelIssues(issues, rocket, from, to);

        return issues;
    }

    public void setCurrentStage(int stage) {
        currentStage = stage;
    }

    // ── NBT / Sync ───────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putInt("solid", solidFuel.level);
        nbt.putInt("maxSolid", solidFuel.max);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);

        // Original serialize(): Rakete + Andockzustand fuer GUI/Renderer
        CompoundTag r = new CompoundTag();
        rocket.writeToNBT(r);
        nbt.put("rocketSync", r);
        nbt.putBoolean("hasDocked", hasDocked);
        nbt.putBoolean("hasRider", hasRider);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        solidFuel.level = nbt.getInt("solid");
        solidFuel.max = nbt.getInt("maxSolid");
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
        rocket = nbt.contains("rocketSync") ? RocketStruct.readFromNBT(nbt.getCompound("rocketSync")) : new RocketStruct();
        hasDocked = nbt.getBoolean("hasDocked");
        hasRider = nbt.getBoolean("hasRider");
    }

    // ── Steuerung ────────────────────────────────────────────────────────────────────────────

    /** Original {@code isUseableByPlayer}: Blockentity steht noch, Spieler innerhalb von 8 Bloecken. */
    @Override
    public boolean hasPermission(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) { }

    @Override
    public void receiveControl(Player player, CompoundTag data) {
        if (data.getBoolean("launch")) {
            launch(player);
        }
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerOrbitalStationLauncher(id, inv, this);
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    /** Original {@code INFINITE_EXTENT_AABB}. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE;
    }
}
