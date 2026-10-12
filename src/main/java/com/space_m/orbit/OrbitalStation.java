package com.space_m.orbit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Stack;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystem.AstroMetric;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.rocket.ItemVOTVdrive.Destination;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 1:1 NTM Space {@code com.hbm.dim.orbit.OrbitalStation}: eine Raumstation im Orbit-Raster.
 *
 * <p>Space station spatial space
 * <ul>
 *   <li>Stations are spread out over the orbital dimension, in restricted areas ({@link #STATION_SIZE} x STATION_SIZE)</li>
 *   <li>Attempting to leave the region your station is in will cause you to fall back to the orbited planet</li>
 *   <li>The current station you are near is fetched using the player's XZ coordinate</li>
 *   <li>The station will determine what body is being orbited, and therefore what to show in the skybox</li>
 * </ul>
 * Persistenz in {@link SolarSystemWorldSavedData} (Liste {@code stations}, Felder wie im Original). Ports und
 * Antriebe sind wie im Original fluechtig (melden sich beim Laden/Ticken wieder an).
 *
 * <p>Abweichungen (technisch): {@code dimensionId} im Sync -> Koerpername; {@code ThreeInts} -> {@link BlockPos};
 * {@code ChunkCoordIntPair} -> {@code ChunkPos}; {@code mainPort.getWorldObj()} -> Level des Hauptports.
 * Original-Klasse: OrbitalStation
 */
public class OrbitalStation {

    public String name = ""; // I dub thee

    public CelestialBody orbiting;
    public CelestialBody target;

    private double eclipseAmount;

    public StationState state = StationState.ORBIT;
    public int stateTimer;
    public int maxStateTimer = 100;

    public boolean hasStation = false;

    // the coordinates of the station within the dimension
    public int dX;
    public int dZ;

    public boolean hasEngines = true;
    public List<BlockPos> errorsAt = new ArrayList<>();
    public int errorTimer;

    public float gravityMultiplier = 1;

    public enum StationState {
        ORBIT, // big chillin
        LEAVING, // prepare engines for transfer
        TRANSFER, // going from A to B
        ARRIVING, // spool down engines
    }

    @Nullable private TileEntityOrbitalStation mainPort;
    private final LinkedHashMap<BlockPos, TileEntityOrbitalStation> ports = new LinkedHashMap<>();
    private int portIndex = 0;

    private final HashSet<IPropulsion> engines = new HashSet<>();

    /** Client: Station unter dem Spieler ({@link StationSyncPacket}). Original statisch mit Koerper 0 vorbelegt. */
    @Nullable private static OrbitalStation clientStation;
    /** Client: Stationen ueber dem eigenen Koerper ({@code SolarSystemWorldSavedData.clientStations}). */
    public static final List<OrbitalStation> orbitingStations = new ArrayList<>();

    public static final int STATION_SIZE = 1024; // total area for each station
    public static final int BUFFER_SIZE = 256; // size of the buffer region that drops you out of orbit (preventing seeing other stations)
    public static final int WARNING_SIZE = 32; // size of the region that warns the player about falling out of orbit

    // For client
    public OrbitalStation(CelestialBody orbiting) {
        this.orbiting = orbiting;
        this.target = orbiting;
    }

    // For server
    public OrbitalStation(CelestialBody orbiting, int x, int z) {
        this(orbiting);
        this.dX = x;
        this.dZ = z;
    }

    /** Original {@code clientStation} (Startwert {@code new OrbitalStation(CelestialBody.getBody(0))}). */
    public static OrbitalStation clientStation() {
        if (clientStation == null) clientStation = new OrbitalStation(CelestialBody.getBody("kerbin"));
        return clientStation;
    }

    public static void setClientStation(@Nullable OrbitalStation station) {
        clientStation = station;
    }

    public void travelTo(Level world, CelestialBody target) {
        if (state != StationState.ORBIT) return; // only when at rest can we start a new journey
        if (!canTravel(orbiting, target)) return;

        setState(StationState.LEAVING, getLeaveTime());
        this.target = target;
    }

    public void update(Level world) {
        if (!world.isClientSide) {
            eclipseAmount = -1;

            if (state == StationState.LEAVING) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.TRANSFER, getTransferTime());
                }
            } else if (state == StationState.TRANSFER) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.ARRIVING, getArriveTime());
                    orbiting = target;
                }
            } else if (state == StationState.ARRIVING) {
                if (stateTimer > maxStateTimer) {
                    setState(StationState.ORBIT, 0);
                }
            }

            markDirty();

            hasEngines = engines.size() > 0;

            errorTimer--;
            if (errorTimer <= 0) {
                errorsAt = new ArrayList<>();
                errorTimer = 0;
            }
        }

        stateTimer++;
    }

    /** {@code SolarSystemWorldSavedData.get(world).markDirty()} (nur Server). */
    public void markDirty() {
        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
        if (data != null) data.setDirty();
    }

    private boolean canTravel(CelestialBody from, CelestialBody to) {
        if (engines.size() == 0) return false;

        double deltaV = SolarSystem.getDeltaVBetween(from, to);
        int shipMass = 200_000; // Always static, to not punish building big cool stations
        float totalThrust = getTotalThrust();

        boolean canTravel = true;
        errorsAt = new ArrayList<>();

        for (IPropulsion engine : engines) {
            float massPortion = engine.getThrust() / totalThrust;
            if (!engine.canPerformBurn(Math.round(shipMass * massPortion), deltaV)) {
                BlockEntity te = engine.getBlockEntity();
                canTravel = false;
                errorsAt.add(te.getBlockPos());
                errorTimer = 100;
            }
        }

        return canTravel;
    }

    private float getTotalThrust() {
        float thrust = 0;
        for (IPropulsion engine : engines) {
            thrust += engine.getThrust();
        }
        return thrust;
    }

    // Has the side effect of beginning engine burns
    private int getLeaveTime() {
        int leaveTime = 20;
        for (IPropulsion engine : engines) {
            int time = engine.startBurn();
            if (time > leaveTime) leaveTime = time;
        }
        return leaveTime;
    }

    // And this one will end engine burns
    private int getArriveTime() {
        int arriveTime = 20;
        for (IPropulsion engine : engines) {
            int time = engine.endBurn();
            if (time > arriveTime) arriveTime = time;
        }
        return arriveTime;
    }

    private int getTransferTime() {
        if (mainPort == null || mainPort.getLevel() == null) return -1;

        int size = calculateSize();
        double distance = SolarSystem.calculateDistanceBetweenTwoBodies(mainPort.getLevel(), orbiting, target);
        float thrust = getTotalThrust();

        return calculateTransferTime(distance, size, thrust);
    }

    public static int calculateTransferTime(double distance, int size, float thrust) {
        return (int) (Math.log(1 + (distance * size / thrust * 100)) * 150);
    }

    public void setState(StationState state, int timeUntilNext) {
        this.state = state;
        stateTimer = 0;
        maxStateTimer = timeUntilNext;
    }

    public boolean recallPod(Destination destination) {
        if (!hasStation) return false;
        if (destination.body().getBody() != orbiting) return false;

        for (TileEntityOrbitalStation port : ports.values()) {
            EntityRideableRocket rocket = port.getDocked();

            if (rocket == null || !rocket.isReusable()) continue;

            // ensure the rocket has fuel before sending it off
            RocketState state = rocket.getState();
            if (state != RocketState.AWAITING && state != RocketState.LANDED) continue;

            // and make sure it doesn't have a rider!!
            if (!rocket.getPassengers().isEmpty()) continue;

            rocket.recallPod(destination);
            return true;
        }

        return false;
    }

    public static void addPropulsion(IPropulsion propulsion) {
        BlockEntity te = propulsion.getBlockEntity();
        OrbitalStation station = getStationFromPosition(te.getBlockPos().getX(), te.getBlockPos().getZ());
        station.engines.add(propulsion);
    }

    public static void removePropulsion(IPropulsion propulsion) {
        BlockEntity te = propulsion.getBlockEntity();
        OrbitalStation station = getStationFromPosition(te.getBlockPos().getX(), te.getBlockPos().getZ());
        station.engines.remove(propulsion);
    }

    public void addPort(TileEntityOrbitalStation port) {
        ports.put(port.getBlockPos(), port);
        if (port.isCore()) mainPort = port;
    }

    public void removePort(TileEntityOrbitalStation port) {
        ports.remove(port.getBlockPos());
    }

    @Nullable
    public TileEntityOrbitalStation getPort() {
        if (ports.size() == 0) return null;

        // First, find any port that's available
        int index = 0;
        for (TileEntityOrbitalStation port : ports.values()) {
            if (!port.hasDocked && !port.isReserved) {
                portIndex = index;
                return port;
            }
            index++;
        }

        // Failing that, round robin on the occupied ports
        portIndex++;
        if (portIndex >= ports.size()) portIndex = 0;

        return ports.values().toArray(new TileEntityOrbitalStation[0])[portIndex];
    }

    @Nullable
    public static TileEntityOrbitalStation getPort(int x, int z) {
        return getStationFromPosition(x, z).getPort();
    }

    // I can't stop pronouncing this as hors d'oeuvre
    private static final Direction[] horDir = new Direction[] { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST };

    // calculates the top down area of the station
    // super fucking fast but like, don't call it every frame
    public int calculateSize() {
        if (mainPort == null || mainPort.getLevel() == null) return 0;
        Level world = mainPort.getLevel();
        BlockPos core = mainPort.getBlockPos();

        int minX, maxX;
        int minZ, maxZ;
        minX = maxX = core.getX();
        minZ = maxZ = core.getZ();

        Stack<BlockPos> stack = new Stack<>();
        stack.push(core);

        HashSet<BlockPos> visited = new HashSet<>();

        while (!stack.isEmpty()) {
            BlockPos pos = stack.pop();
            visited.add(pos);

            if (pos.getX() < minX) minX = pos.getX();
            if (pos.getX() > maxX) maxX = pos.getX();
            if (pos.getZ() < minZ) minZ = pos.getZ();
            if (pos.getZ() > maxZ) maxZ = pos.getZ();

            for (Direction dir : horDir) {
                BlockPos nextPos = pos.relative(dir);

                if (!visited.contains(nextPos) && isInStation(world, nextPos, core)) {
                    stack.push(nextPos);
                }
            }
        }

        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    /** Original {@code world.getHeightValue(x, z) > 1}: irgendein Block ueber y = 0 in dieser Saeule. */
    private static boolean isInStation(Level world, BlockPos pos, BlockPos core) {
        if (world.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ()) > 1) return true;
        return Math.abs(pos.getX() - core.getX()) < 5 && Math.abs(pos.getZ() - core.getZ()) < 5; // minimum station size
    }

    public double getUnscaledProgress(float partialTicks) {
        if (state == StationState.ORBIT) return 0;
        return Mth.clamp(((double) stateTimer + partialTicks) / (double) maxStateTimer, 0, 1);
    }

    public double getTransferProgress(float partialTicks) {
        if (state != StationState.TRANSFER) return 0;
        return easeInOutCirc(getUnscaledProgress(partialTicks));
    }

    public double getEclipseAmount(Level world) {
        if (eclipseAmount > -1) return eclipseAmount;

        double sunSize = SolarSystem.calculateSunSize(orbiting);

        double progress = getTransferProgress(0);

        List<AstroMetric> metrics;

        // Get our orrery of bodies, this is cached for reuse in sky rendering
        if (state == StationState.ORBIT) {
            double altitude = OrbitWorld.getOrbitalAltitude(orbiting);
            metrics = SolarSystem.calculateMetricsFromSatellite(world, 0, orbiting, altitude);
        } else {
            double fromAlt = OrbitWorld.getOrbitalAltitude(orbiting);
            double toAlt = OrbitWorld.getOrbitalAltitude(target);
            metrics = SolarSystem.calculateMetricsBetweenSatelliteOrbits(world, 0, orbiting, target, fromAlt, toAlt, progress);

            double sunTargetSize = SolarSystem.calculateSunSize(target);
            sunSize = lerp(progress, sunSize, sunTargetSize);
        }

        // Get our eclipse amount
        eclipseAmount = CelestialEnvironment.getEclipseFactor(metrics, sunSize, SolarSystem.MAX_APPARENT_SIZE_ORBIT);

        return eclipseAmount;
    }

    static double lerp(double t, double a, double b) {
        return a + (b - a) * t;
    }

    private double easeInOutCirc(double t) {
        return t < 0.5
            ? (1 - Math.sqrt(1 - Math.pow(2 * t, 3))) / 2
            : (Math.sqrt(1 - Math.pow(-2 * t + 2, 3)) + 1) / 2;
    }

    // Finds a space station for a given set of coordinates
    public static OrbitalStation getStationFromPosition(int x, int z) {
        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
        if (data == null) return clientStation(); // Client ohne Server-Daten (Original: Welt 0 des integrierten Servers)

        OrbitalStation station = data.getStationFromPosition(x, z);

        // Fallback for when a station doesn't exist (should only occur when using debug wand!)
        if (station == null) {
            station = data.addStation(Mth.floor((float) x / STATION_SIZE), Mth.floor((float) z / STATION_SIZE), CelestialBody.getBody("kerbin"));
        }

        return station;
    }

    public static OrbitalStation getStation(int x, int z) {
        return getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);
    }

    /** Station an x/z: Server ueber die SavedData, Client die synchronisierte {@link #clientStation()}. */
    public static OrbitalStation getStationFromPosition(Level level, int x, int z) {
        return level.isClientSide ? clientStation() : getStationFromPosition(x, z);
    }

    public void serialize(FriendlyByteBuf buf) {
        buf.writeUtf(orbiting.name);
        buf.writeUtf(target.name);
        buf.writeInt(state.ordinal());
        buf.writeInt(stateTimer);
        buf.writeInt(maxStateTimer);
        buf.writeBoolean(hasEngines);
        buf.writeFloat(gravityMultiplier);

        buf.writeUtf(name);

        buf.writeInt(errorsAt.size());
        for (BlockPos error : errorsAt) {
            buf.writeInt(error.getX());
            buf.writeInt(error.getY());
            buf.writeInt(error.getZ());
        }
    }

    public static OrbitalStation deserialize(FriendlyByteBuf buf) {
        OrbitalStation station = new OrbitalStation(CelestialBody.getBody(buf.readUtf()));
        station.target = CelestialBody.getBody(buf.readUtf());
        station.state = StationState.values()[buf.readInt()];
        station.stateTimer = buf.readInt();
        station.maxStateTimer = buf.readInt();
        station.hasEngines = buf.readBoolean();
        station.gravityMultiplier = buf.readFloat();

        station.name = buf.readUtf();

        station.errorsAt = new ArrayList<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            int x = buf.readInt();
            int y = buf.readInt();
            int z = buf.readInt();

            station.errorsAt.add(new BlockPos(x, y, z));
        }
        return station;
    }

    /** Original {@code spawn(world, x, z)}: Hauptport (Kern) auf y = 127 setzen, falls dort noch keiner steht. */
    public static void spawn(ServerLevel world, int x, int z) {
        int y = 127;
        BlockPos pos = new BlockPos(x, y, z);
        if (world.getBlockState(pos).getBlock() == OrbitBlocks.ORBITAL_STATION.get()) return;

        // BlockDummyable.safeRem + setBlock(meta 12 = NORTH) + fillSpace: der Kern baut seine Struktur in onPlace
        world.setBlock(pos, OrbitBlocks.ORBITAL_STATION.get().defaultBlockState()
                .setValue(BlockOrbitalStation.FACING, Direction.NORTH), 3);
    }

    // Mark the station as travelable
    public static void addStation(int x, int z, CelestialBody body) {
        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
        if (data == null) return;
        OrbitalStation station = data.getStationFromPosition(x * STATION_SIZE, z * STATION_SIZE);

        if (station == null) {
            station = data.addStation(x, z, body);
        }

        station.orbiting = station.target = body;
        station.hasStation = true;
        data.setDirty();
    }

    /** Nur fuer Tests/Diagnose: angemeldete Antriebe und Ports. */
    public int engineCount() { return engines.size(); }
    public int portCount() { return ports.size(); }
}
