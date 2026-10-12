package com.space_m.orbit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.UniversalMachinePartBlock;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.network.InfoToastPacket;
import com.space_m.atmosphere.AtmosphereBlob;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.OrbitHooks;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.command.CommandStations;
import com.space_m.command.StationCommandAccess;
import com.space_m.config.SpaceConfig;
import com.space_m.network.SpacePacketHandler;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketStationHooks;
import com.space_m.rocket.entity.EntityRideableRocket;
import com.space_m.rocket.entity.RocketTravelHooks;
import com.space_m.travel.CelestialTeleporter;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * R6-A: Anbindung der Raumstationen an die uebrigen Runden (alle Original-Direktaufrufe von {@code OrbitalStation},
 * {@code SolarSystemWorldSavedData.getStations} und {@code TileEntityOrbitalStation}):
 * <ul>
 *   <li>{@link OrbitHooks} (R1-A Himmelskoerper): {@code CelestialBody.getTarget/getGravity},
 *       {@code WorldProviderCelestial.getSunPower} im Orbit, Stationsliste im Trait-Sync ({@code PermaSyncHandler}).</li>
 *   <li>{@link RocketTravelHooks} (R5-B Rakete): Transfer ueber eine Orbit-Station, Andocken, Stationskern-Nutzlast.</li>
 *   <li>{@link RocketStationHooks} (R5-A Laufwerk): Stationsname/-ziel, Drop-Pod-Rueckruf ({@code recallPod}).</li>
 *   <li>{@link CommandStations#setAccess} ({@code /ntmstations}).</li>
 *   <li>Abdichtung des Stationsports samt Phantomteilen ({@code IBlockSealable.isSealed = true}).</li>
 *   <li>{@code ModEventHandler}: Rand des Stationsrasters ({@code info.orbitfall}, Absturz zum umkreisten Koerper);
 *       Hauptport nicht abbaubar ({@code removedByPlayer = false}).</li>
 *   <li>Stations-Sync an Spieler im Orbit ({@link StationSyncPacket}).</li>
 * </ul>
 * Aufruf {@link #init()} aus {@code SpaceRegistry.init()}.
 */
public final class SpaceOrbit {

    private SpaceOrbit() {}

    private static final RandomSource rand = RandomSource.create();

    public static void init() {
        OrbitHooks.set(new Orbit());
        RocketTravelHooks.set(new Travel());
        RocketStationHooks.set(new Stations());
        CommandStations.setAccess(new Command());
        AtmosphereBlob.registerSealProvider(new Sealing());

        TickEvent.PLAYER_PRE.register(SpaceOrbit::playerTick);
        BlockEvent.BREAK.register((level, pos, state, player, xp) -> isMainPortCell(level, pos, state) ? EventResult.interruptFalse() : EventResult.pass());
    }

    // ── Kern-Suche (Original CompatExternal.getCoreFromPos / BlockDummyable.findCore) ─────────

    @Nullable
    public static BlockPos findCore(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof UniversalMachinePartBlock && level.getBlockEntity(pos) instanceof IMultiblockPart part) {
            return part.getControllerPos();
        }
        return pos;
    }

    @Nullable
    public static BlockEntity getCoreFromPos(Level level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null ? level.getBlockEntity(core) : null;
    }

    /** Kern oder Phantomteil des Hauptports ({@code orbital_station}). */
    static boolean isMainPortCell(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() == OrbitBlocks.ORBITAL_STATION.get()) return true;
        if (!(state.getBlock() instanceof UniversalMachinePartBlock)) return false;
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockState(core).getBlock() == OrbitBlocks.ORBITAL_STATION.get();
    }

    // ── Spieler-Tick (ModEventHandler.onPlayerTick, Phase START, Server) ──────────────────────

    private static void playerTick(Player player) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) return;
        if (!CelestialBody.inOrbit(player.level())) return;

        // Check for players attempting to cross over to another orbital grid
        if (!(player.getVehicle() instanceof EntityRideableRocket)) {
            double rx = Math.abs(player.getX()) % OrbitalStation.STATION_SIZE;
            double rz = Math.abs(player.getZ()) % OrbitalStation.STATION_SIZE;

            int minBuffer = OrbitalStation.BUFFER_SIZE;
            int maxBuffer = OrbitalStation.STATION_SIZE - minBuffer;

            int minWarning = OrbitalStation.BUFFER_SIZE + OrbitalStation.WARNING_SIZE;
            int maxWarning = OrbitalStation.STATION_SIZE - minWarning;

            if (rx < minWarning || rx > maxWarning || rz < minWarning || rz > maxWarning) {
                InfoToastPacket.sendTo(serverPlayer, "info.orbitfall", 60, InfoToastPacket.ID_GAS_HAZARD, 0xFF5555);
            }

            if (rx < minBuffer || rx > maxBuffer || rz < minBuffer || rz > maxBuffer) {
                OrbitalStation station = OrbitalStation.getStationFromPosition((int) player.getX(), (int) player.getZ());
                int max = SpaceConfig.get().maxProbeDistance;
                CelestialTeleporter.teleport(player, station.orbiting.dimension, rand.nextInt(max * 2) - max, 800, rand.nextInt(max * 2) - max, false);
            }
        }

        // Stations-Sync (Original: Kern-TE an Spieler im Umkreis / Rakete an ihren Fahrer)
        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(player.level());
        if (data != null) {
            OrbitalStation station = data.getStationFromPosition((int) player.getX(), (int) player.getZ());
            if (station == null) station = new OrbitalStation(CelestialBody.getBody("kerbin"));
            SpacePacketHandler.sendToPlayer(serverPlayer, SpacePacketHandler.ORBITAL_STATION, new StationSyncPacket(station));
        }
    }

    // ── OrbitHooks ─────────────────────────────────────────────────────────────────────────────

    static final class Orbit implements OrbitHooks {
        @Override
        public float gravityMultiplier(Level level, int x, int z) {
            return OrbitalStation.getStationFromPosition(level, x, z).gravityMultiplier;
        }

        @Override
        public CelestialBody orbiting(Level level, int x, int z) {
            return OrbitalStation.getStationFromPosition(level, x, z).orbiting;
        }

        @Override
        public boolean hasStation(Level level, int x, int z) {
            return OrbitalStation.getStationFromPosition(level, x, z).hasStation;
        }

        @Override
        public float sunPower(Level level, int x, int z) {
            return OrbitWorld.getSunPower(level, x, z);
        }

        /** Original {@code PermaSyncHandler}: {@code hasStation && orbiting.dimensionId == player.dimension}. */
        @Override
        public List<int[]> syncedStations(ServerPlayer player) {
            List<int[]> list = new ArrayList<>();
            SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(player.level());
            if (data == null) return list;
            for (OrbitalStation station : data.getStations().values()) {
                if (station.hasStation && station.orbiting.dimension == player.level().dimension()) {
                    list.add(new int[] { station.dX, station.dZ });
                }
            }
            return list;
        }
    }

    // ── RocketTravelHooks ──────────────────────────────────────────────────────────────────────

    static final class Travel implements RocketTravelHooks {
        @Override
        public int[] beginTransfer(ServerLevel level, CelestialBody from, CelestialBody to, int transferTime) {
            SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(level);
            if (data == null || level.getServer().getLevel(com.space_m.dim.SpaceDimensions.ORBIT) == null) return null;
            OrbitalStation station = data.addStation(from);

            station.setState(OrbitalStation.StationState.TRANSFER, transferTime);
            station.orbiting = from;
            station.target = to;
            data.setDirty();

            return new int[] { station.dX, station.dZ };
        }

        @Override
        public void tickTransfer(Level level, int x, int z) {
            OrbitalStation.getStationFromPosition(x, z).update(level);
        }

        @Override
        public double transferProgress(Level level, int x, int z) {
            return OrbitalStation.getStationFromPosition(x, z).getUnscaledProgress(0);
        }

        @Override
        public boolean transferArrived(Level level, int x, int z) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(x, z);
            return station.getUnscaledProgress(0) > 0.99 || station.state == OrbitalStation.StationState.ARRIVING;
        }

        @Override
        public CelestialBody transferTarget(Level level, int x, int z) {
            return OrbitalStation.getStationFromPosition(x, z).target;
        }

        @Override
        public void endTransfer(Level level, int x, int z) {
            SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(level);
            if (data != null) data.removeStation(OrbitalStation.getStationFromPosition(x, z));
        }

        @Override
        public void spawnStation(ServerLevel orbit, int x, int z) {
            OrbitalStation.spawn(orbit, x, z);
        }

        @Override
        public void addStation(int x, int z, CelestialBody orbiting) {
            OrbitalStation.addStation(x, z, orbiting);
        }

        @Override
        public RocketPort getPort(Level level, int x, int z) {
            if (level.isClientSide) return null;
            return OrbitalStation.getPort(x, z);
        }

        @Override
        public void dockAt(Level level, BlockPos top, EntityRideableRocket rocket) {
            if (getCoreFromPos(level, top) instanceof IDockingPort port) port.dockRocket(rocket);
        }
    }

    // ── RocketStationHooks (Orbit-Laufwerke) ───────────────────────────────────────────────────

    static final class Stations implements RocketStationHooks {
        @Override
        public String stationName(int x, int z) {
            return OrbitalStation.getStation(x, z).name;
        }

        @Override
        public boolean hasStation(int x, int z) {
            return OrbitalStation.getStation(x, z).hasStation;
        }

        /** Original {@code if(!station.hasStation) station.orbiting = CelestialBody.getBody(world)}. */
        @Override
        public CelestialBody orbiting(Level level, int x, int z) {
            OrbitalStation station = OrbitalStation.getStation(x, z);
            if (!station.hasStation) station.orbiting = CelestialBody.getBody(level);
            return station.orbiting;
        }

        @Override
        public boolean recallPod(Level level, int x, int z, ItemVOTVdrive.Destination target) {
            return OrbitalStation.getStation(x, z).recallPod(target);
        }
    }

    // ── /ntmstations ───────────────────────────────────────────────────────────────────────────

    static final class Command implements StationCommandAccess {
        @Override
        public Destination getDestination(ItemStack stack) {
            if (!(stack.getItem() instanceof ItemVOTVdrive)) return null;
            ItemVOTVdrive.Destination d = ItemVOTVdrive.getDestination(stack);
            if (d == null) return null;
            return new Destination(d.body().getBody(), d.body().getDimension(), d.x(), d.z());
        }

        @Override
        public void addStation(int x, int z, CelestialBody orbiting) {
            OrbitalStation.addStation(x, z, orbiting);
        }

        @Override
        public void spawnStation(ServerLevel orbit, int x, int z) {
            OrbitalStation.spawn(orbit, x, z);
        }

        @Override
        public Collection<StationInfo> getStations() {
            List<StationInfo> list = new ArrayList<>();
            SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
            if (data == null) return list;
            for (OrbitalStation station : data.getStations().values()) {
                list.add(new StationInfo(station.dX, station.dZ, station.name, station.hasStation));
            }
            return list;
        }

        /** Original {@code new ItemStack(full_drive, 1, Body.ORBIT.ordinal())} mit x, z, Processed, stationName. */
        @Override
        public ItemStack createDrive(StationInfo station) {
            ItemStack drive = ItemVOTVdrive.of(SolarSystem.Body.ORBIT);
            ItemVOTVdrive.setCoordinates(drive, station.dX(), station.dZ());
            ItemVOTVdrive.setProcessed(drive, true);
            com.hbm_m.platform.PlatformHooks.editItemTag(drive, t -> t.putString("stationName", station.name()));
            return drive;
        }

        @Override
        public int stationSize() {
            return OrbitalStation.STATION_SIZE;
        }
    }

    // ── Abdichtung (IBlockSealable.isSealed = true) ───────────────────────────────────────────

    static final class Sealing implements AtmosphereBlob.SealProvider {
        @Override
        public Boolean isSealed(Level world, BlockPos pos, BlockState state, AtmosphereBlob.ChunkLookup lookup) {
            if (state.getBlock() instanceof BlockOrbitalStation) return true;
            if (state.getBlock() instanceof UniversalMachinePartBlock && lookup.blockEntity(pos) instanceof IMultiblockPart part) {
                BlockPos core = part.getControllerPos();
                if (core != null) {
                    BlockState coreState = lookup.state(core);
                    if (coreState != null && coreState.getBlock() instanceof BlockOrbitalStation) return true;
                }
            }
            return null;
        }

        @Override
        public boolean isDynamic(BlockState state) {
            return false;
        }
    }
}
