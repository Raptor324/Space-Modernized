package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.dim.SpaceDimensions;
import com.space_m.fluid.SpaceFluids;
import com.space_m.fluid.trait.FT_Rocket;
import com.space_m.orbit.OrbitalStation;
import com.space_m.orbit.OrbitalStation.StationState;
import com.space_m.orbit.TileEntityOrbitalStation;
import com.space_m.rocket.ItemVOTVdrive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
 * R6-B: Stationsantrieb. (1) Strukturen, Werte und Treibstoffe der Triebwerke 1:1; (2) Ende-zu-Ende in der Orbit-Dimension:
 * zwei Xenon-Triebwerke an einer Station, Stationscomputer startet die Reise Kerbin -> Mun, Verbrauch
 * {@code getFuelCost(deltaV, 200 000 * Schubanteil, ISP 4200)} mB und {@code * 5 000} HE je Triebwerk, Zustaende
 * LEAVING (200 Ticks) -> TRANSFER ({@code calculateTransferTime}) -> ARRIVING (200 Ticks) -> ORBIT um die Mun; ohne
 * Treibstoff keine Reise und Fehlermeldungen je Triebwerk; (3) Kreativantrieb, HTR-Neo-Bedingungen, Startport.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PropulsionGameTest {

    private PropulsionGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static BlockState facing(BlockState state, Direction dir) {
        return state.setValue(DummyableMachineBlock.FACING, dir);
    }

    private static int cells(DummyableMachineBlock block) {
        return block.getStructureHelper().getPartOffsets().size() + 1;
    }

    /** Strukturgroessen ({@code getDimensions}), Platzierung, Schub, Brennzeiten, Tanks, ISP der Treibstoffe. */
    @GameTest(template = "empty3x3x3", batch = "space_propulsion", timeoutTicks = 20)
    public static void thrusterValues(GameTestHelper helper) {
        check(cells(PropulsionBlocks.MACHINE_XENON_THRUSTER.get()) == 3 * 3 * 2, "xenon 3x3x2, ist " + cells(PropulsionBlocks.MACHINE_XENON_THRUSTER.get()));
        check(cells(PropulsionBlocks.MACHINE_HTR3.get()) == 7 * 7 * 11, "htr3 7x7x11");
        check(cells(PropulsionBlocks.MACHINE_HTRF4.get()) == 5 * 5 * 21, "htrf4 5x5x21");
        check(cells(PropulsionBlocks.MACHINE_HTRF4NEO.get()) == 5 * 5 * 21, "htrf4neo 5x5x21");
        check(cells(PropulsionBlocks.ORBITAL_STATION_COMPUTER.get()) == 1, "computer 1 cell");
        check(PropulsionBlocks.MACHINE_HTR3.get().getHeightOffset() == 3 && PropulsionBlocks.MACHINE_XENON_THRUSTER.get().getHeightOffset() == 1
                && PropulsionBlocks.MACHINE_HTRF4.get().getHeightOffset() == 2, "height offsets 3/1/2");
        check(PropulsionUtil.placementFacing(Direction.SOUTH) == Direction.EAST && PropulsionUtil.placementFacing(Direction.NORTH) == Direction.WEST,
                "getDirModified = getRotation(DOWN)");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerLevel level = helper.getLevel();

        TileEntityXenonThruster xenon = new TileEntityXenonThruster(pos, facing(PropulsionBlocks.MACHINE_XENON_THRUSTER.get().defaultBlockState(), Direction.WEST));
        xenon.setLevel(level);
        check(xenon.isFacingPrograde() && xenon.getThrust() == 1_400_000F && xenon.endBurn() == 200, "xenon prograde WEST, thrust, 200 ticks");
        check(xenon.tanks[0].getTankType() == ModFluids.XENON.getSource() && xenon.tanks[0].getMaxFill() == 4_000 && xenon.getMaxEnergyStored() == 20_000_000L, "xenon tank/power");
        check(PropulsionUtil.isp(xenon.tanks[0]) == 4200, "xenon ISP 4200");

        TileEntityMachineHTR3 htr3 = new TileEntityMachineHTR3(pos, facing(PropulsionBlocks.MACHINE_HTR3.get().defaultBlockState(), Direction.SOUTH));
        check(htr3.isFacingPrograde() && htr3.getThrust() == 800_000_000F && htr3.endBurn() == 80, "htr3 values");
        check(htr3.tanks[0].getTankType() == SpaceFluids.SUPERHEATED_HYDROGEN.fluid() && htr3.tanks[0].getMaxFill() == 1_280_000, "htr3 tank");
        check(PropulsionUtil.isp(htr3.tanks[0]) == 900, "superheated hydrogen ISP 900");
        check("missileContrailMUD".equals(TileEntityMachineHTR3.getContrailType(SpaceFluids.GAS_WATZ.fluid()))
                && "missileContrailSCH".equals(TileEntityMachineHTR3.getContrailType(SpaceFluids.GASEOUS_SCHRABIDIUM_BROMIDE.fluid()))
                && "missileContrailUP".equals(TileEntityMachineHTR3.getContrailType(SpaceFluids.GASEOUS_URANIUM_BROMIDE.fluid()))
                && "missileContrail".equals(TileEntityMachineHTR3.getContrailType(SpaceFluids.SUPERHEATED_HYDROGEN.fluid())), "contrail types");
        check(FluidType.hasTrait(SpaceFluids.GASEOUS_THORIUM_BROMIDE.fluid(), FT_Rocket.class), "bromide rocket fuel");

        // HTR3: Brennen ohne Strom, Verbrauch fuelCost mB
        htr3.setLevel(level);
        htr3.tanks[0].setFill(1_280_000);
        int cost = SolarSystem.getFuelCost(1000, 200_000, 900);
        check(htr3.canPerformBurn(200_000, 1000) && htr3.getFuelCost() == cost, "htr3 burn fuel " + cost);
        check(htr3.startBurn() == 80 && htr3.isOn && htr3.tanks[0].getFill() == 1_280_000 - cost, "htr3 consumes fuel");

        TileEntityMachineHTRF4 htrf4 = new TileEntityMachineHTRF4(pos, facing(PropulsionBlocks.MACHINE_HTRF4.get().defaultBlockState(), Direction.SOUTH));
        check(htrf4.getThrust() == 1_600_000_000F && htrf4.endBurn() == 180 && htrf4.tanks[0].getMaxFill() == 64000
                && htrf4.tanks[0].getTankType() == ModFluids.PLASMA_DT.getSource() && htrf4.getMaxEnergyStored() == 1_000_000_000L, "htrf4 values");
        check(PropulsionUtil.isp(htrf4.tanks[0]) == 12000, "plasma DT ISP 12000");

        // HTR-F4 NEO: braucht Plasmaenergie >= fuelCost (ISP 250), vollen Speicher, Kuehlung
        TileEntityMachineHTRNeo neo = new TileEntityMachineHTRNeo(pos, facing(PropulsionBlocks.MACHINE_HTRF4NEO.get().defaultBlockState(), Direction.SOUTH));
        check(neo.getThrust() == 1_600_000_000F && neo.endBurn() == 180, "neo values");
        check(!neo.canPerformBurn(200_000, 1000), "neo cold start fails");
        List<Component> errors = new ArrayList<>();
        neo.addErrors(errors);
        check(errors.size() == 3, "neo: plasma, power, coolant errors, got " + errors.size());
        neo.plasmaEnergySync = SolarSystem.getFuelCost(1000, 200_000, 250);
        neo.setEnergyStored(TileEntityMachineHTRNeo.maxPower);
        check(!neo.canPerformBurn(200_000, 1000) && !neo.isCool(), "neo needs coolant");

        // Kreativantrieb
        TileEntityStationPropulsionCreative creative = new TileEntityStationPropulsionCreative(pos, PropulsionBlocks.PROPULSION_CREATIVE.get().defaultBlockState());
        check(creative.canPerformBurn(1, 1e9) && creative.getThrust() == 10_000_000F && creative.startBurn() == 20 && creative.endBurn() == 20, "creative");

        // Startport: 29 Slots, 12 Anschluesse, 10 Tanks a 64 000 mB
        TileEntityOrbitalStationLauncher launcher = new TileEntityOrbitalStationLauncher(pos, PropulsionBlocks.ORBITAL_STATION_LAUNCHER.get().defaultBlockState());
        launcher.setLevel(level);
        check(TileEntityOrbitalStationLauncher.SLOT_COUNT == 29 && launcher.getInventory().getSlots() == 29, "launcher 29 slots");
        check(launcher.getConPos().length == 12 && launcher.tanks.length == 10 && launcher.tanks[0].getMaxFill() == 64_000, "launcher connections/tanks");
        check(!launcher.canLaunch(), "launcher without rocket can't launch");
        helper.succeed();
    }

    /**
     * Ende-zu-Ende in {@code space_m:orbit}: Station mit Kern (Hauptport fuer Groesse/Transferzeit), zwei prograde
     * Xenon-Triebwerke (+ eins in falscher Richtung, nicht angemeldet), Reise Kerbin -> Mun ueber den Stationscomputer.
     */
    @GameTest(template = "empty3x3x3", batch = "space_propulsion", timeoutTicks = 40)
    public static void stationTravelsToMun(GameTestHelper helper) {
        ServerLevel orbit = helper.getLevel().getServer().getLevel(SpaceDimensions.ORBIT);
        // NeoForge-1.21.1-GameTestServer laedt keine Datapack-Dimensionen (s. DimensionGameTest) - R6-A
        if (orbit == null) { helper.succeed(); return; }
        check(orbit != null && CelestialBody.inOrbit(orbit), "orbit dimension");
        CelestialBody kerbin = CelestialBody.getBody("kerbin");
        CelestialBody mun = CelestialBody.getBody("mun");

        int dX = 37, dZ = -41;
        int x = dX * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        int z = dZ * OrbitalStation.STATION_SIZE + OrbitalStation.STATION_SIZE / 2;
        BlockPos core = new BlockPos(x, 127, z);

        OrbitalStation.addStation(dX, dZ, kerbin);
        OrbitalStation.spawn(orbit, x, z);

        BlockState west = facing(PropulsionBlocks.MACHINE_XENON_THRUSTER.get().defaultBlockState(), Direction.WEST);
        TileEntityXenonThruster a = new TileEntityXenonThruster(core.offset(12, 0, 0), west);
        TileEntityXenonThruster b = new TileEntityXenonThruster(core.offset(12, 0, 4), west);
        TileEntityXenonThruster wrong = new TileEntityXenonThruster(core.offset(12, 0, 8), facing(west, Direction.NORTH));
        TileEntityOrbitalStationComputer computer = new TileEntityOrbitalStationComputer(core.offset(0, 1, 6),
                PropulsionBlocks.ORBITAL_STATION_COMPUTER.get().defaultBlockState());
        TileEntityXenonThruster[] engines = { a, b, wrong };

        try {
            TileEntityOrbitalStation port = (TileEntityOrbitalStation) orbit.getBlockEntity(core);
            check(port != null, "station core");
            TileEntityOrbitalStation.tick(orbit, core, orbit.getBlockState(core), port);

            OrbitalStation station = OrbitalStation.getStation(dX, dZ);
            check(station.orbiting == kerbin && station.state == StationState.ORBIT, "station orbits kerbin");

            for (TileEntityXenonThruster e : engines) {
                e.setLevel(orbit);
                e.tanks[0].setFill(4_000);
                e.setEnergyStored(TileEntityXenonThruster.maxPower);
                TileEntityXenonThruster.tick(orbit, e.getBlockPos(), e.getBlockState(), e);
            }
            computer.setLevel(orbit);
            check(station.engineCount() == 2, "two prograde engines registered, got " + station.engineCount());

            // Reise: deltaV Kerbin -> Mun, Schubanteil 1/2 -> Masse 100 000 je Triebwerk
            double deltaV = SolarSystem.getDeltaVBetween(kerbin, mun);
            int fuel = SolarSystem.getFuelCost(deltaV, Math.round(200_000 * 0.5F), 4200);
            check(fuel > 0 && fuel <= 4_000, "fuel per engine " + fuel);

            check(computer.travelTo(mun, ItemVOTVdrive.of(SolarSystem.Body.MUN)), "computer starts journey");
            check(computer.isTravelling() && station.state == StationState.LEAVING && station.target == mun, "LEAVING to mun");
            check(station.maxStateTimer == 200, "prewarm 200 ticks, got " + station.maxStateTimer);
            for (TileEntityXenonThruster e : new TileEntityXenonThruster[] { a, b }) {
                check(e.isOn && e.tanks[0].getFill() == 4_000 - fuel, "xenon used " + fuel + " mB, left " + e.tanks[0].getFill());
                check(e.getEnergyStored() == TileEntityXenonThruster.maxPower - (long) fuel * 5_000, "xenon used " + fuel * 5_000 + " HE");
            }
            check(!wrong.isOn && wrong.tanks[0].getFill() == 4_000, "retrograde engine untouched");
            check(!computer.getDrive().isEmpty(), "drive kept in computer");

            int ticks = 0;
            while (station.state == StationState.LEAVING && ticks < 1000) { station.update(orbit); ticks++; }
            check(ticks == 202 && station.state == StationState.TRANSFER, "LEAVING took " + ticks);

            int size = station.calculateSize();
            double distance = SolarSystem.calculateDistanceBetweenTwoBodies(orbit, kerbin, mun);
            int transfer = (int) (Math.log(1 + (distance * size / 2_800_000F * 100)) * 150);
            check(station.maxStateTimer == transfer && transfer > 0, "transfer time " + station.maxStateTimer + " expected " + transfer);

            ticks = 0;
            while (station.state == StationState.TRANSFER && ticks < 100_000) { station.update(orbit); ticks++; }
            check(ticks == transfer + 1 && station.state == StationState.ARRIVING && station.orbiting == mun, "TRANSFER took " + ticks);
            check(station.maxStateTimer == 200 && !a.isOn && !b.isOn, "engines off, cooldown 200");

            ticks = 0;
            while (station.state == StationState.ARRIVING && ticks < 1000) { station.update(orbit); ticks++; }
            check(ticks == 201 && station.state == StationState.ORBIT && station.orbiting == mun, "back in orbit around mun after " + ticks);
            check(CelestialBody.getTarget(orbit, x, z).body == mun, "getTarget -> mun");

            // Ohne Treibstoff: keine Reise, Fehler je Triebwerk
            a.tanks[0].setFill(0);
            b.tanks[0].setFill(0);
            check(!computer.travelTo(mun, ItemVOTVdrive.of(SolarSystem.Body.MUN)), "already at mun");
            computer.travelTo(kerbin, ItemVOTVdrive.of(SolarSystem.Body.KERBIN));
            check(station.state == StationState.ORBIT && station.orbiting == mun, "no fuel -> no travel");
            check(station.errorsAt.contains(a.getBlockPos()) && station.errorsAt.contains(b.getBlockPos()), "errors at both engines");
            List<Component> errors = new ArrayList<>();
            a.addErrors(errors);
            check(errors.stream().anyMatch(c -> c.getString().equals(" - Insufficient fuel: needs " + a.getFuelCost() + "mB")), "fuel error " + errors);
        } finally {
            for (TileEntityXenonThruster e : engines) e.setRemoved();
            for (BlockPos p : BlockPos.betweenClosed(core.offset(-3, -1, -3), core.offset(3, 2, 3))) {
                if (!orbit.getBlockState(p).isAir()) orbit.setBlock(p, Blocks.AIR.defaultBlockState(), 2 | 16);
            }
            SolarSystemWorldSavedData d = SolarSystemWorldSavedData.get(helper.getLevel().getServer());
            OrbitalStation s = d.getStationFromPosition(x, z);
            if (s != null) { s.hasStation = false; d.removeStation(s); }
        }
        helper.succeed();
    }
}
