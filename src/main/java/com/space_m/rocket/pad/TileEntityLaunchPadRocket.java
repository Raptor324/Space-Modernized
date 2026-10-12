package com.space_m.rocket.pad;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.platform.RenderBounds;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.SpaceRocketItems;
import com.space_m.rocket.entity.EntityRideableRocket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code TileEntityLaunchPadRocket}: betankt und startet die Rakete aus der Montage.
 *
 * <p>Slots: 0 Rakete ({@code rocket_custom}), 1 Ziel-Laufwerk, 2 Batterie, 3/4 Kanister rein/raus (auch Festbrennstoff
 * {@code rocket_fuel}, je Stueck 250 kg). Bis zu {@code MAX_STAGES * 2} Tanks werden aus dem Treibstoffbedarf der
 * Rakete angelegt ({@link #updateStorageTanks}); war die Rakete schon betankt ({@code hasFuel}), werden die Tanks
 * gefuellt. Ab Raketenhoehe 9 waechst ein Versorgungsturm mit Plattform und Leiter ({@link LaunchPadRocketTowerBlock}).
 * Start (GUI "COMMIT TO LAUNCH"): Strom &gt;= 75 %, alle Tanks voll, Laufwerk verarbeitet, Rakete schafft das Ziel ->
 * {@link EntityRideableRocket} bei Y+3, alle Fuellungen und Slots 0/1 verbraucht, 75 % Strom abgezogen.
 *
 * <p>Abweichungen: OpenComputers-Schnittstelle entfaellt (kein OC auf 1.20); Leitern ueber eine Kletter-Bewegung im
 * BE-Tick statt {@code HbmPlayerProps.isOnLadder}; Turm aus eigenem Block (s. {@link LaunchPadRocketTowerBlock}).
 */
public class TileEntityLaunchPadRocket extends BaseMachineBlockEntity implements com.hbm_m.api.fluids.IFluidStandardReceiverMK2,
        com.hbm_m.api.tile.IControlReceiver {

    public static final int SLOT_ROCKET = 0;
    public static final int SLOT_DRIVE = 1;
    public static final int SLOT_BATTERY = 2;
    public static final int SLOT_IN = 3;
    public static final int SLOT_OUT = 4;
    public static final int SLOT_COUNT = 5;

    public static final long maxPower = 100_000;

    public static class SolidFuelTank {
        public int level = 0;
        public int max = 0;
    }
    public SolidFuelTank solidFuel = new SolidFuelTank();

    public FluidTank[] tanks;

    public boolean canSeeSky = true;
    @Nullable public RocketStruct rocket;
    public int height;

    /** Festbrennstoff-Schluessel in {@link RocketStruct#getFillRequirement()} (Original {@code Fluids.NONE}). */
    public static final Fluid SOLID = Fluids.EMPTY;

    public TileEntityLaunchPadRocket(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.LAUNCH_PAD_ROCKET.get(), pos, state, SLOT_COUNT, maxPower, maxPower);
        tanks = new FluidTank[RocketStruct.MAX_STAGES * 2]; // enough tanks for any combination of rocket stages
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTank(none(), 64_000);
    }

    private static Fluid none() {
        return ModFluids.NONE.getSource();
    }

    private static boolean isNone(Fluid f) {
        return f == null || f == none() || f == Fluids.EMPTY;
    }

    public long getPower() {
        return getEnergyStored();
    }

    // ── Ausrichtung (Original dir = Meta - offset, rot = dir.getRotation(UP)) ──────────────────

    public Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public Direction rot() {
        return dir().getClockWise();
    }

    /** {@code (x + dir*forward + rot*side, y + up, z + ...)}. */
    public BlockPos at(int forward, int up, int side) {
        return worldPosition.relative(dir(), forward).relative(rot(), side).above(up);
    }

    // ── Tick ───────────────────────────────────────────────────────────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityLaunchPadRocket be) {
        if (!level.isClientSide) {
            be.serverTick((ServerLevel) level);
        }
        be.tickLadders(level);
    }

    private void serverTick(ServerLevel level) {
        ensureNetworkInitialized();

        ItemStack driveStack = inventory.getStackInSlot(SLOT_DRIVE);
        if (!driveStack.isEmpty() && driveStack.getItem() instanceof ItemVOTVdrive) ItemVOTVdrive.getTarget(driveStack, level);

        // Setup tanks required for the current rocket
        updateTanks();

        // Connections
        if (level.getGameTime() % 20 == 0) {
            for (ConPos pos : getConPos()) {
                trySubscribe(level, pos.pos().getX(), pos.pos().getY(), pos.pos().getZ(), pos.dir());

                if (hasRocket()) {
                    for (FluidTank tank : tanks) {
                        if (isNone(tank.getTankType())) continue;
                        trySubscribe(tank.getTankType(), level, pos.pos(), pos.dir());
                    }
                }
            }
        }

        // Fills, note that the liquid input also takes solid fuel
        ItemStack battery = inventory.getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && battery.getItem() instanceof ItemCreativeBattery) setEnergyStored(maxPower);
        else chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = slotArray();
        for (FluidTank tank : tanks) tank.loadTank(SLOT_IN, SLOT_OUT, slots);
        writeBack(slots);

        ItemStack in = inventory.getStackInSlot(SLOT_IN);
        if (!in.isEmpty() && in.is(ModItems.ROCKET_FUEL.get()) && solidFuel.level < solidFuel.max) {
            inventory.extractItem(SLOT_IN, 1, false);
            solidFuel.level += 250;
            if (solidFuel.level > solidFuel.max) solidFuel.level = solidFuel.max;
        }

        rocket = ItemCustomRocket.get(inventory.getStackInSlot(SLOT_ROCKET));
        if (rocket != null) {
            double capsuleHeight = rocket.capsule != null ? rocket.capsule.height : 0;
            int newHeight = (int) Math.floor(rocket.getHeight() - capsuleHeight + 1);
            if (newHeight <= 8) newHeight = 0;

            if (newHeight != height) {
                // Check that the pad is entirely unobstructed
                canSeeSky = !isPadObstructed();

                if (canSeeSky) {
                    buildTower(level, newHeight);
                }

                height = newHeight;
            }
        }

        setChanged();
        sendUpdateToClient(); // networkPackNT(250)
    }

    /** Original: Turm (7 Saeulen) und begehbare Plattform auf Raketenhoehe mit Strukturbloecken fuellen. */
    private void buildTower(ServerLevel level, int newHeight) {
        int y = worldPosition.getY();
        int maxY = level.getMaxBuildHeight() - 1;
        BlockState up = LaunchPadRocketTowerBlock.state(Direction.UP);

        LaunchPadRocketTowerBlock.safeRem = true;
        try {
            // Build tower
            if (newHeight > height) {
                for (int oy = height + 3; oy < newHeight + 3; oy++) {
                    if (y + oy > maxY) break;
                    for (int[] c : TOWER) placeTower(level, at(c[0], oy, c[1]), up);
                }
            } else {
                for (int oy = height + 3; oy >= newHeight + 3; oy--) {
                    if (y + oy > maxY) continue;
                    for (int[] c : TOWER) clearTower(level, at(c[0], oy, c[1]));
                }
            }

            // Build standable platform after removing old platform
            if (height >= 8) {
                for (int[] c : PLATFORM) clearTower(level, at(c[0], height + 2, c[1]));
            }

            if (newHeight >= 8) {
                for (int[] c : PLATFORM) placeTower(level, at(c[0], newHeight + 2, c[1]), LaunchPadRocketTowerBlock.state(c[2] == 0 ? dir() : rot()));
            }
        } finally {
            LaunchPadRocketTowerBlock.safeRem = false;
        }
    }

    /** Turm-Saeulen {forward, side} (Original: -rot*2-dir*4/5/6, -rot*3/4-dir*4, -rot*3/4-dir*6). */
    private static final int[][] TOWER = {
            {-4, -2}, {-5, -2}, {-6, -2},
            {-4, -3}, {-4, -4},
            {-6, -3}, {-6, -4},
    };

    /** Plattform {forward, side, 0 = Meta dir / 1 = Meta rot}. */
    private static final int[][] PLATFORM = {
            {-3, -2, 0},
            {-1, -1, 0}, {-2, -1, 0}, {-3, -1, 0}, {-4, -1, 1}, {-5, -1, 1}, {-6, -1, 1},
            {-1, 0, 0}, {-2, 0, 0}, {-3, 0, 0}, {-4, 0, 1}, {-5, 0, 1}, {-6, 0, 1},
            {-1, 1, 0}, {-2, 1, 0}, {-3, 1, 0}, {-4, 1, 1}, {-5, 1, 1}, {-6, 1, 1},
    };

    private void placeTower(ServerLevel level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof LaunchPadRocketTowerBlock.TowerBlockEntity tower) tower.setCore(worldPosition);
    }

    private void clearTower(ServerLevel level, BlockPos pos) {
        // Original setBlockToAir: nur Turmbloecke dieser Rampe entfernen
        if (level.getBlockState(pos).getBlock() instanceof LaunchPadRocketTowerBlock) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    /** Beim Abbau der Rampe: alle Turm-/Plattformbloecke entfernen (Original: Dummies fielen mit dem Kern). */
    public void removeTower(Level level) {
        if (!(level instanceof ServerLevel server) || height <= 0) return;
        LaunchPadRocketTowerBlock.safeRem = true;
        try {
            for (int oy = 3; oy < height + 3; oy++) {
                for (int[] c : TOWER) clearTower(server, at(c[0], oy, c[1]));
            }
            if (height >= 8) {
                for (int[] c : PLATFORM) clearTower(server, at(c[0], height + 2, c[1]));
            }
        } finally {
            LaunchPadRocketTowerBlock.safeRem = false;
        }
    }

    /**
     * Original: {@code HbmPlayerProps.isOnLadder = true} fuer Spieler an der Seitenleiter (Sockel) und - ab Turmhoehe 8 -
     * an der Hauptleiter. Ersatz: Vanilla-Kletterbewegung ({@code EntityLivingBase.isOnLadder} 1.7.10).
     */
    private void tickLadders(Level level) {
        Direction dir = dir();
        Direction rot = rot();
        double x = worldPosition.getX();
        double y = worldPosition.getY();
        double z = worldPosition.getZ();

        AABB side = new AABB(x + 0.25, y, z + 0.25, x + 0.75, y + 3, z + 0.75)
                .move(-rot.getStepX() * 6.5 - dir.getStepX() * 5, 0, -rot.getStepZ() * 6.5 - dir.getStepZ() * 5);
        for (Player player : level.getEntitiesOfClass(Player.class, side)) climb(player);

        if (height >= 8) {
            AABB main = new AABB(x + 0.25, y + 3, z + 0.25, x + 0.75, y + 3 + height, z + 0.75)
                    .move(-rot.getStepX() * 2.5 - dir.getStepX() * 5, 0, -rot.getStepZ() * 2.5 - dir.getStepZ() * 5);
            for (Player player : level.getEntitiesOfClass(Player.class, main)) climb(player);
        }
    }

    /** 1.7.10 {@code moveEntityWithHeading} mit {@code isOnLadder}. */
    private static void climb(Player player) {
        if (player.getAbilities().flying || player.isSpectator()) return;
        Vec3 m = player.getDeltaMovement();
        double mx = Math.max(-0.15, Math.min(0.15, m.x));
        double mz = Math.max(-0.15, Math.min(0.15, m.z));
        double my = Math.max(m.y, -0.15);
        if (player.isShiftKeyDown() && my < 0) my = 0;
        if (player.horizontalCollision) my = 0.2;
        player.setDeltaMovement(mx, my, mz);
        player.fallDistance = 0;
    }

    private boolean isPadObstructed() {
        for (int ox = 0; ox <= 0; ox++) {
            for (int oz = 0; oz <= 0; oz++) {
                // Original canBlockSeeTheSky(x, y + 2, z): die (nicht lichtundurchlaessigen) Dummies zaehlten dort nicht;
                // hier ueber dem Sockel pruefen, die HBM-Teilbloecke schatten den Himmel ab
                if (level != null && !level.canSeeSky(worldPosition.offset(ox, 3, oz))) {
                    return true;
                }
            }
        }

        return false;
    }

    public record ConPos(BlockPos pos, Direction dir) {}

    @Nullable private ConPos[] conPos;

    public ConPos[] getConPos() {
        if (conPos == null) {
            conPos = new ConPos[13]; // 12 + 1 inputs

            Direction out = dir().getOpposite();
            int i = 0;

            for (int or = 1; or < 5; or++) {
                for (int oy = 0; oy < 3; oy++) {
                    conPos[i++] = new ConPos(at(-8, oy, -or), out);
                }
            }
            conPos[i++] = new ConPos(at(-8, 0, 3), out);
        }

        return conPos;
    }

    // ── Start ──────────────────────────────────────────────────────────────────────────────────

    public void launch(@Nullable Player player) {
        if (!canLaunch() || level == null) return;

        EntityRideableRocket rocket = EntityRideableRocket.create(level, worldPosition.getX() + 0.5F, worldPosition.getY() + 3.0F, worldPosition.getZ() + 0.5F, inventory.getStackInSlot(SLOT_ROCKET))
                .withProgram(inventory.getStackInSlot(SLOT_DRIVE)).launchedBy(player);
        level.addFreshEntity(rocket);

        // Deplete all fills
        for (int i = 0; i < tanks.length; i++) tanks[i] = new FluidTank(none(), 64_000);
        solidFuel.level = solidFuel.max = 0;

        setEnergyStored(getEnergyStored() - (long) (maxPower * 0.75));

        inventory.setStackInSlot(SLOT_ROCKET, ItemStack.EMPTY);
        inventory.setStackInSlot(SLOT_DRIVE, ItemStack.EMPTY);
        setChanged();
    }

    private boolean hasRocket() {
        return ItemCustomRocket.get(inventory.getStackInSlot(SLOT_ROCKET)) != null;
    }

    private boolean hasDrive() {
        ItemStack stack = inventory.getStackInSlot(SLOT_DRIVE);
        return !stack.isEmpty() && stack.getItem() instanceof ItemVOTVdrive;
    }

    private boolean areTanksFull() {
        for (FluidTank tank : tanks) if (!isNone(tank.getTankType()) && tank.getFill() < tank.getMaxFill()) return false;
        if (solidFuel.level < solidFuel.max) return false;
        return true;
    }

    private boolean canReachDestination() {
        ItemStack drive = inventory.getStackInSlot(SLOT_DRIVE);

        // Check that the drive is processed
        if (!ItemVOTVdrive.getProcessed(drive)) {
            return false;
        }

        RocketStruct rocket = ItemCustomRocket.get(inventory.getStackInSlot(SLOT_ROCKET));
        if (rocket == null || level == null) return false;

        SolarSystem.Body target = ItemVOTVdrive.getDestination(drive).body();
        if (target == SolarSystem.Body.ORBIT && !isCapsule(rocket) && !isStationCore(rocket))
            return false;

        CelestialBody.Target from = CelestialBody.getTarget(level, worldPosition.getX(), worldPosition.getZ());
        ItemVOTVdrive.Target to = ItemVOTVdrive.getTarget(drive, level);

        if (!to.isValid() && !isStationCore(rocket)) return false;
        if (to.isValid() && isStationCore(rocket)) return false;
        if (to.body() == null) return false;

        // Check if the stage can make the journey
        return rocket.hasSufficientFuel(from.body, to.body(), from.inOrbit, to.inOrbit());
    }

    private static boolean isCapsule(RocketStruct rocket) {
        return rocket.capsule != null && rocket.capsule.part == SpaceRocketItems.RP_CAPSULE_20.get();
    }

    private static boolean isStationCore(RocketStruct rocket) {
        return rocket.capsule != null && rocket.capsule.part == SpaceRocketItems.RP_STATION_CORE_20.get();
    }

    public boolean canLaunch() {
        return hasRocket() && hasDrive() && getEnergyStored() >= maxPower * 0.75 && areTanksFull() && canReachDestination();
    }

    private void updateTanks() {
        if (!hasRocket()) return;

        ItemStack stack = inventory.getStackInSlot(SLOT_ROCKET);
        RocketStruct rocket = ItemCustomRocket.get(stack);

        // If the rocket is already fueled, unmark it and fill the tanks
        boolean hasFuel = ItemCustomRocket.hasFuel(stack);
        if (hasFuel) ItemCustomRocket.setFuel(stack, false);

        updateStorageTanks(rocket, tanks, solidFuel, hasFuel);
    }

    public static void updateStorageTanks(RocketStruct rocket, FluidTank[] tanks, SolidFuelTank solidFuel, boolean hasFuel) {
        Map<Fluid, Integer> fuels = rocket.getFillRequirement();

        // Remove solid fuels (listed as NONE fluid) from tank updates
        if (fuels.containsKey(SOLID)) {
            solidFuel.max = fuels.get(SOLID);
            if (hasFuel) solidFuel.level = solidFuel.max;
            fuels.remove(SOLID);
        } else {
            solidFuel.max = 0;
        }

        // Check to see if any of the current tanks already fulfil fuelling requirements
        List<FluidTank> keepTanks = new ArrayList<>();
        for (FluidTank tank : tanks) {
            if (fuels.containsKey(tank.getTankType())) {
                tank.changeTankSize(fuels.get(tank.getTankType()));
                keepTanks.add(tank);
                fuels.remove(tank.getTankType());
            }
        }

        // Add new tanks
        for (Entry<Fluid, Integer> entry : fuels.entrySet()) {
            keepTanks.add(new FluidTank(entry.getKey(), entry.getValue()));
        }

        // Fill tanks if rocket had fuel
        if (hasFuel) {
            for (FluidTank tank : keepTanks) {
                tank.setFill(tank.getMaxFill());
            }
        }

        // Sort and fill the tank array to place NONE at the end (Original: absteigende FluidType-ID, NONE = 0)
        keepTanks.sort((a, b) -> fluidId(b.getTankType()) - fluidId(a.getTankType()));
        while (keepTanks.size() < RocketStruct.MAX_STAGES * 2) {
            keepTanks.add(new FluidTank(none(), 64_000));
        }

        for (int i = 0; i < tanks.length; i++) {
            tanks[i] = keepTanks.get(i);
        }
    }

    private static int fluidId(Fluid f) {
        if (isNone(f)) return -1;
        return BuiltInRegistries.FLUID.getId(f);
    }

    // ── Fehlerliste (GUI) ──────────────────────────────────────────────────────────────────────

    public static void findTankIssues(List<Component> issues, FluidTank[] tanks, SolidFuelTank solidFuel) {
        for (FluidTank tank : tanks) {
            if (isNone(tank.getTankType())) continue;
            int fill = tank.getFill();
            int maxFill = tank.getMaxFill();
            String tankName = FluidType.forFluid(tank.getTankType()).getLocalizedName().getString();
            if (tankName.contains(" ")) {
                String[] split = tankName.split(" ");
                tankName = split[split.length - 1];
            }
            if (fill < maxFill) {
                issues.add(Component.literal(fill + "/" + maxFill + "mB " + tankName).withStyle(ChatFormatting.YELLOW));
            } else {
                issues.add(Component.literal(fill + "/" + maxFill + "mB " + tankName).withStyle(ChatFormatting.GREEN));
            }
        }

        if (solidFuel.max > 0) {
            if (solidFuel.level < solidFuel.max) {
                issues.add(Component.literal(solidFuel.level + "/" + solidFuel.max + "kg Solid Fuel").withStyle(ChatFormatting.YELLOW));
            } else {
                issues.add(Component.literal(solidFuel.level + "/" + solidFuel.max + "kg Solid Fuel").withStyle(ChatFormatting.GREEN));
            }
        }
    }

    public static boolean findDriveIssues(List<Component> issues, RocketStruct rocket, ItemStack drive) {
        if (drive.isEmpty() || !(drive.getItem() instanceof ItemVOTVdrive)) {
            issues.add(Component.literal("No destination drive installed").withStyle(ChatFormatting.YELLOW));
            return true;
        }

        if (!ItemVOTVdrive.getProcessed(drive)) {
            issues.add(Component.literal("Destination drive needs processing").withStyle(ChatFormatting.RED));
            return true;
        }

        SolarSystem.Body target = ItemVOTVdrive.getDestination(drive).body();
        if (target == SolarSystem.Body.ORBIT && !isCapsule(rocket) && !isStationCore(rocket)) {
            issues.add(Component.literal("Satellite target must be a planet").withStyle(ChatFormatting.RED));
            return true;
        }

        return false;
    }

    public static void findTravelIssues(List<Component> issues, RocketStruct rocket, CelestialBody.Target from, ItemVOTVdrive.Target to) {
        if (to.inOrbit() && !to.isValid() && !isStationCore(rocket)) {
            issues.add(Component.literal("Station not yet launched").withStyle(ChatFormatting.RED));
        }

        if (to.inOrbit() && to.isValid() && isStationCore(rocket)) {
            issues.add(Component.literal("Station already launched").withStyle(ChatFormatting.RED));
        }

        if (to.body() == null || !rocket.hasSufficientFuel(from.body, to.body(), from.inOrbit, to.inOrbit())) {
            issues.add(Component.literal("Rocket can't reach destination").withStyle(ChatFormatting.RED));
        }
    }

    public List<Component> findIssues() {
        List<Component> issues = new ArrayList<>();

        if (!hasRocket() || level == null) return issues;

        // Check that the rocket is fully fueled and capable of leaving our starting planet
        RocketStruct rocket = ItemCustomRocket.get(inventory.getStackInSlot(SLOT_ROCKET));

        if (!canSeeSky) {
            issues.add(Component.literal("Pad is obstructed").withStyle(ChatFormatting.RED));
        }

        if (getEnergyStored() < maxPower * 0.75) {
            issues.add(Component.literal("Insufficient power").withStyle(ChatFormatting.RED));
        }

        findTankIssues(issues, tanks, solidFuel);
        if (findDriveIssues(issues, rocket, inventory.getStackInSlot(SLOT_DRIVE))) return issues;

        // Check that the rocket is actually capable of reaching our destination
        CelestialBody.Target from = CelestialBody.getTarget(level, worldPosition.getX(), worldPosition.getZ());
        ItemVOTVdrive.Target to = ItemVOTVdrive.getTarget(inventory.getStackInSlot(SLOT_DRIVE), level);

        findTravelIssues(issues, rocket, from, to);

        return issues;
    }

    // ── Inventar ───────────────────────────────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int index, @NotNull ItemStack stack) {
        if (stack.isEmpty()) return true;
        if (index == SLOT_ROCKET && !(stack.getItem() instanceof ItemCustomRocket)) return false;
        if (index == SLOT_DRIVE && !(stack.getItem() instanceof ItemVOTVdrive)) return false;
        if (index == SLOT_BATTERY && !isEnergyProviderItem(stack) && !(stack.getItem() instanceof ItemCreativeBattery)) return false;
        if (index == SLOT_IN && !stack.is(ModItems.ROCKET_FUEL.get())) return false;
        return true;
    }

    /** Fuer die GUI-Slots (Original SlotNonRetarded fragt isItemValidForSlot). */
    public boolean isItemValid(int index, ItemStack stack) {
        return isItemValidForSlot(index, stack);
    }

    private ItemStack[] slotArray() {
        ItemStack[] slots = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) slots[i] = inventory.getStackInSlot(i);
        return slots;
    }

    private void writeBack(ItemStack[] slots) {
        for (int i : new int[] { SLOT_IN, SLOT_OUT }) {
            if (inventory.getStackInSlot(i) != slots[i]) inventory.setStackInSlot(i, slots[i]);
        }
    }

    // ── Steuerung ──────────────────────────────────────────────────────────────────────────────

    @Override
    public boolean hasPermission(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D) <= 1024;
    }

    @Override
    public void receiveControl(CompoundTag data) { }

    @Override
    public void receiveControl(Player player, CompoundTag data) {
        if (data.getBoolean("launch")) {
            launch(player);
        }
    }

    // ── BaseMachineBlockEntity ─────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.launchPadRocket");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inv, Player player) {
        return new ContainerLaunchPadRocket(containerId, inv, this);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return RenderBounds.INFINITE; // hi martin ;)
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("power", getEnergyStored());
        nbt.putInt("solid", solidFuel.level);
        nbt.putInt("maxSolid", solidFuel.max);
        nbt.putInt("height", height);
        nbt.putBoolean("sky", canSeeSky);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
        // Original serialize(): Rakete fuer Client-GUI/Renderer
        if (rocket != null) {
            CompoundTag r = new CompoundTag();
            rocket.writeToNBT(r);
            nbt.put("rocketSync", r);
        }
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        solidFuel.level = nbt.getInt("solid");
        solidFuel.max = nbt.getInt("maxSolid");
        height = nbt.getInt("height");
        canSeeSky = !nbt.contains("sky") || nbt.getBoolean("sky");
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
        rocket = nbt.contains("rocketSync") ? RocketStruct.readFromNBT(nbt.getCompound("rocketSync")) : null;
    }

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getReceivingTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }
}
