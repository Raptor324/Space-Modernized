package com.space_m.orbit;

import com.hbm_m.interfaces.IMultiblockPart;
import com.space_m.SpaceMod;
import com.space_m.atmosphere.AtmosphereBlob;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.command.CommandStations;
import com.space_m.command.StationCommandAccess;
import com.space_m.dim.SpaceDimensions;
import com.space_m.fluid.SpaceFluids;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketStationHooks;
import com.space_m.rocket.SpaceRocketItems;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;
import com.space_m.rocket.entity.RocketTravelHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R6-A: Orbit-Dimension, Stationen als SavedData (Raster, Speichern, Transfer-Zustaende), Stationskern samt Ports,
 * Andocken einer Kapsel, Anbindung an Himmelskoerper/Laufwerk/Befehl.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OrbitGameTest {

    private OrbitGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static SolarSystemWorldSavedData data(GameTestHelper helper) {
        return SolarSystemWorldSavedData.get(helper.getLevel().getServer());
    }

    /** NeoForge-1.21.1-GameTestServer laedt keine Datapack-Dimensionen (s. DimensionGameTest): dann null. */
    private static ServerLevel orbit(GameTestHelper helper) {
        return helper.getLevel().getServer().getLevel(SpaceDimensions.ORBIT);
    }

    /** Orbit-Dimension: leer (flach ohne Schichten), Biom "Space", inOrbit, Schwerkraft 0. */
    @GameTest(template = "empty3x3x3", batch = "space_orbit")
    public static void orbitDimension(GameTestHelper helper) {
        ServerLevel orbit = orbit(helper);
        if (orbit == null) { helper.succeed(); return; }
        check(CelestialBody.inOrbit(orbit), "inOrbit(orbit)");
        BlockPos probe = new BlockPos(4096 * 3 + 7, 64, 4096 * 3 + 7);
        check(orbit.getBlockState(probe).isAir() && orbit.getBlockState(probe.atY(0)).isAir() && orbit.getBlockState(probe.atY(1)).isAir(),
                "orbit chunk empty (ChunkProviderOrbit)");
        check(orbit.getBiome(probe).is(SpaceMod.id("orbit")), "biome space_m:orbit");
        check(!orbit.dimensionType().hasCeiling() && orbit.dimensionType().hasSkyLight(), "orbit dimension type");
        helper.succeed();
    }

    /** SavedData: Raster (1024er Zellen), Anlegen/Finden, Speichern/Laden 1:1, nur temporaere Stationen entfernbar. */
    @GameTest(template = "empty3x3x3", batch = "space_orbit")
    public static void stationSavedData(GameTestHelper helper) {
        SolarSystemWorldSavedData data = data(helper);
        CelestialBody mun = CelestialBody.getBody("mun");
        CelestialBody duna = CelestialBody.getBody("duna");

        OrbitalStation station = data.addStation(-21, 17, mun);
        check(data.getStationFromPosition(-21 * 1024 + 1, 17 * 1024 + 1023) == station, "grid lookup inside cell");
        check(data.getStationFromPosition(-20 * 1024, 17 * 1024) != station, "neighbour cell");
        check(OrbitalStation.getStation(-21, 17) == station, "getStation(dX, dZ)");

        station.name = "Testwerk";
        station.target = duna;
        station.gravityMultiplier = 0.5F;
        station.setState(OrbitalStation.StationState.TRANSFER, 77);
        station.stateTimer = 13;

        CompoundTag nbt = data.toNBT();
        SolarSystemWorldSavedData copy = SolarSystemWorldSavedData.load(nbt);
        OrbitalStation loaded = copy.getStationFromPosition(-21 * 1024, 17 * 1024);
        check(loaded != null, "station saved");
        check(loaded.dX == -21 && loaded.dZ == 17 && loaded.orbiting == mun && loaded.target == duna, "coords/bodies saved");
        check(loaded.state == OrbitalStation.StationState.TRANSFER && loaded.stateTimer == 13 && loaded.maxStateTimer == 77, "state saved");
        check("Testwerk".equals(loaded.name) && loaded.gravityMultiplier == 0.5F && !loaded.hasStation, "name/gravity/hasStation saved");

        // Don't remove stations with launched cores, only temporary stations!
        OrbitalStation.addStation(-21, 17, mun);
        check(station.hasStation && station.orbiting == mun && station.target == mun, "addStation marks travelable");
        data.removeStation(station);
        check(data.getStationFromPosition(-21 * 1024, 17 * 1024) == station, "launched station not removed");
        station.hasStation = false;
        data.removeStation(station);
        check(data.getStationFromPosition(-21 * 1024, 17 * 1024) == null, "temporary station removed");

        // findFreeSpace innerhalb maxStationDistance
        OrbitalStation free = data.addStation(mun);
        int size = com.space_m.config.SpaceConfig.get().maxStationDistance / OrbitalStation.STATION_SIZE;
        check(Math.abs(free.dX) <= size && Math.abs(free.dZ) <= size, "free space in range");
        data.removeStation(free);
        helper.succeed();
    }

    /** Zustaende ORBIT -> LEAVING -> TRANSFER -> ARRIVING -> ORBIT, Transferzeit-Formel, Fortschritt (easeInOutCirc). */
    @GameTest(template = "empty3x3x3", batch = "space_orbit")
    public static void stationStates(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CelestialBody kerbin = CelestialBody.getBody("kerbin");
        CelestialBody mun = CelestialBody.getBody("mun");

        check(OrbitalStation.calculateTransferTime(1000, 10, 100) == (int) (Math.log(1 + (1000D * 10 / 100 * 100)) * 150), "transfer time formula");
        check(OrbitalStation.calculateTransferTime(1000, 10, 100) == RocketTravelHooks.calculateTransferTime(1000, 10, 100), "same as rocket");

        OrbitalStation station = new OrbitalStation(kerbin, 999, 999);
        station.travelTo(level, mun);
        check(station.state == OrbitalStation.StationState.ORBIT, "no engines -> no travel");

        station.setState(OrbitalStation.StationState.TRANSFER, 10);
        station.target = mun;
        check(station.getTransferProgress(0) == 0 && station.getUnscaledProgress(0) == 0, "progress 0 at start");
        station.stateTimer = 5;
        check(Math.abs(station.getTransferProgress(0) - 0.5) < 1e-9, "easeInOutCirc(0.5) = 0.5");
        station.stateTimer = 0;
        for (int i = 0; i < 12; i++) station.update(level);
        check(station.state == OrbitalStation.StationState.ARRIVING && station.orbiting == mun, "arrived at mun");
        check(station.maxStateTimer == 20, "arrive time 20 without engines");
        for (int i = 0; i < 22; i++) station.update(level);
        check(station.state == OrbitalStation.StationState.ORBIT && station.getUnscaledProgress(0) == 0, "back in orbit");
        helper.succeed();
    }

    /** Stationskern auf y = 127 mit Phantomteilen und 12 Anschlusszellen, abgedichtet, Hauptport geschuetzt; Hooks. */
    @GameTest(template = "empty3x3x3", batch = "space_orbit")
    public static void stationCoreAndHooks(GameTestHelper helper) {
        ServerLevel orbit = orbit(helper);
        if (orbit == null) { helper.succeed(); return; }
        CelestialBody mun = CelestialBody.getBody("mun");
        int dX = 23, dZ = -19;
        int x = dX * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        int z = dZ * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        BlockPos core = new BlockPos(x, 127, z);

        OrbitalStation.addStation(dX, dZ, mun);
        OrbitalStation.spawn(orbit, x, z);
        try {
            check(orbit.getBlockState(core).getBlock() == OrbitBlocks.ORBITAL_STATION.get(), "core placed");
            check(orbit.getBlockEntity(core) instanceof TileEntityOrbitalStation te && te.isCore(), "core BE");
            for (BlockPos p : new BlockPos[] { core.offset(2, 0, 2), core.offset(-2, 1, -2), core.offset(2, 1, 0), core.offset(0, 1, -2) }) {
                check(orbit.getBlockEntity(p) instanceof IMultiblockPart part && core.equals(part.getControllerPos()), "phantom part at " + p);
            }
            check(SpaceOrbit.isMainPortCell(orbit, core.offset(1, 1, 1), orbit.getBlockState(core.offset(1, 1, 1))), "main port cell protected");
            check(Boolean.TRUE.equals(new SpaceOrbit.Sealing().isSealed(orbit, core.offset(2, 1, 0), orbit.getBlockState(core.offset(2, 1, 0)),
                    lookup(orbit))), "port parts sealed");

            // Tick: Kern meldet sich als Port an und laesst die Station laufen
            TileEntityOrbitalStation te = (TileEntityOrbitalStation) orbit.getBlockEntity(core);
            TileEntityOrbitalStation.tick(orbit, core, orbit.getBlockState(core), te);
            OrbitalStation station = OrbitalStation.getStation(dX, dZ);
            check(station.getPort() == te, "core registered as port");
            check(RocketTravelHooks.get().getPort(orbit, x + 100, z - 100) == te, "rocket finds port");
            check(station.calculateSize() == 81, "minimum station size (|d| < 5 -> 9x9), got " + station.calculateSize());

            // Himmelskoerper (OrbitHooks), Laufwerk (RocketStationHooks), Sonne
            CelestialBody.Target target = CelestialBody.getTarget(orbit, x, z);
            check(target.inOrbit && target.hasStation && target.body == mun, "getTarget in orbit -> mun");
            station.gravityMultiplier = 0.5F;
            check(com.space_m.celestial.OrbitHooks.get().gravityMultiplier(orbit, x, z) == 0.5F, "gravity multiplier");
            station.gravityMultiplier = 1;
            station.name = "Kranich";
            check("Kranich".equals(RocketStationHooks.get().stationName(dX, dZ)) && RocketStationHooks.get().hasStation(dX, dZ), "drive hooks");
            float sun = com.space_m.celestial.OrbitHooks.get().sunPower(orbit, x, z);
            check(sun >= 0 && sun <= mun.getSunPower() + 1e-4, "sun power with eclipse, got " + sun);

            // /ntmstations: Laufwerk + Liste
            StationCommandAccess access = CommandStations.access();
            ItemStack drive = access.createDrive(new StationCommandAccess.StationInfo(dX, dZ, "Kranich", true));
            ItemVOTVdrive.Destination dest = ItemVOTVdrive.getDestination(drive);
            check(dest.body() == SolarSystem.Body.ORBIT && dest.x() == dX && dest.z() == dZ && ItemVOTVdrive.getProcessed(drive), "fetch drive");
            StationCommandAccess.Destination cmdDest = access.getDestination(drive);
            check(cmdDest != null && cmdDest.dimension() == SpaceDimensions.ORBIT && cmdDest.body() == null, "command destination orbit");
            check(access.getStations().stream().anyMatch(s -> s.dX() == dX && s.dZ() == dZ && s.hasStation()), "station listed");
            ItemVOTVdrive.Target driveTarget = ItemVOTVdrive.getTarget(drive, helper.getLevel());
            check(driveTarget.body() == mun && driveTarget.inOrbit() && driveTarget.isValid(), "drive target station orbiting mun");
        } finally {
            clear(orbit, core);
            SolarSystemWorldSavedData d = data(helper);
            OrbitalStation s = d.getStationFromPosition(x, z);
            if (s != null) { s.hasStation = false; d.removeStation(s); }
        }
        helper.succeed();
    }

    /** Port im Orbit: Kapsel rp_pod_20 andocken, Hydrazin-Bedarf, NEEDSFUEL -> LANDED, Einlagern, Rueckruf. */
    @GameTest(template = "empty3x3x3", batch = "space_orbit")
    public static void portDocking(GameTestHelper helper) {
        ServerLevel orbit = orbit(helper);
        if (orbit == null) { helper.succeed(); return; }
        CelestialBody kerbin = CelestialBody.getBody("kerbin");
        int dX = -27, dZ = 29;
        int x = dX * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        int z = dZ * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        BlockPos core = new BlockPos(x, 127, z);

        OrbitalStation.addStation(dX, dZ, kerbin);
        OrbitalStation.spawn(orbit, x, z);
        try {
            TileEntityOrbitalStation te = (TileEntityOrbitalStation) orbit.getBlockEntity(core);
            check(te != null, "port BE");
            TileEntityOrbitalStation.tick(orbit, core, orbit.getBlockState(core), te);

            te.spawnRocket(com.space_m.rocket.ItemCustomRocket.build(new com.space_m.rocket.RocketStruct(new ItemStack(SpaceRocketItems.RP_POD_20.get()))));
            EntityRideableRocket rocket = te.getDocked();
            check(rocket != null && rocket.isReusable(), "pod docked");
            check(rocket.getState() == RocketState.NEEDSFUEL, "reusable pod needs fuel");
            check(Math.abs(rocket.getY() + rocket.getBbHeight() - (core.getY() + 1.5D)) < 1e-6, "pod hangs below the ring");

            TileEntityOrbitalStation.tick(orbit, core, orbit.getBlockState(core), te);
            int need = te.getFillRequirement(false);
            check(te.hasDocked && te.needsFuel && !te.hasFuel, "flags while empty");
            check(te.tank().getMaxFill() == need && need > 0, "tank sized to requirement " + need + ", got " + te.tank().getMaxFill());
            check(te.getFillRequirement(true) == 500, "station to station 500 mB");

            te.tank().fillMb(SpaceFluids.HYDRAZINE.fluid(), need);
            TileEntityOrbitalStation.tick(orbit, core, orbit.getBlockState(core), te);
            check(rocket.getState() == RocketState.LANDED, "fuelled pod without drive -> LANDED, got " + rocket.getState());

            // Einlagern (Schleichen, leere Hand) -> Kapsel als Item im Port
            te.despawnRocket();
            check(te.getDocked() == null && te.hasStoredItems(), "pod stored");
            check(te.clientSlots().get(0).getItem() == SpaceRocketItems.RP_POD_20.get(), "stored capsule item");
        } finally {
            clear(orbit, core);
            SolarSystemWorldSavedData d = data(helper);
            OrbitalStation s = d.getStationFromPosition(x, z);
            if (s != null) { s.hasStation = false; d.removeStation(s); }
        }
        helper.succeed();
    }

    /** Raketen-Transfer ueber eine Orbit-Station (RocketTravelHooks): Station im Raster, Fortschritt, Ankunft, Abbau. */
    @GameTest(template = "empty3x3x3", batch = "space_orbit")
    public static void rocketTransfer(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CelestialBody kerbin = CelestialBody.getBody("kerbin");
        CelestialBody mun = CelestialBody.getBody("mun");
        RocketTravelHooks hooks = RocketTravelHooks.get();

        int[] cell = hooks.beginTransfer(level, kerbin, mun, 10);
        if (orbit(helper) == null) {
            check(cell == null, "without orbit dimension: transfer inside the rocket");
            helper.succeed();
            return;
        }
        check(cell != null, "transfer station allocated");
        int x = cell[0] * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        int z = cell[1] * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        OrbitalStation station = OrbitalStation.getStationFromPosition(x, z);
        check(station.state == OrbitalStation.StationState.TRANSFER && station.orbiting == kerbin && station.target == mun, "transfer state");
        check(hooks.transferTarget(level, x, z) == mun, "transfer target");

        for (int i = 0; i < 12 && !hooks.transferArrived(level, x, z); i++) hooks.tickTransfer(level, x, z);
        check(hooks.transferArrived(level, x, z), "arrived");
        hooks.endTransfer(level, x, z);
        check(data(helper).getStationFromPosition(x, z) == null, "temporary transfer station removed");
        helper.succeed();
    }

    private static AtmosphereBlob.ChunkLookup lookup(ServerLevel level) {
        return new AtmosphereBlob.ChunkLookup(level);
    }

    private static void clear(ServerLevel orbit, BlockPos core) {
        for (BlockPos p : BlockPos.betweenClosed(core.offset(-3, -1, -3), core.offset(3, 2, 3))) {
            BlockState state = orbit.getBlockState(p);
            if (!state.isAir()) orbit.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
    }
}
