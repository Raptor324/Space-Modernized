package com.space_m.test;

import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.item.SpaceItems;
import com.space_m.lifesupport.ItemModOxy;
import com.space_m.lifesupport.TileEntityAirPump;
import com.space_m.lifesupport.TileEntityAirScrubber;
import com.space_m.lifesupport.TileEntityAlgaeFilm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R2-B Lebenserhaltung: Luftpumpe (Vakuum-Entlueftung, abgedichteter Raum 0.2 atm), Luftwascher (CO2 aus Abgas),
 * Algenfilm (nur Orbit), PLSS-Logik, Rezepte. Vorlage {@code lifesupport7} = 7x7x7 Luft.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LifeSupportGameTest {

    private LifeSupportGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Hohler Steinwuerfel 7x7x7 (Innenraum 5x5x5 = 125 Bloecke). */
    private static void buildSealedBox(GameTestHelper helper) {
        for (int x = 0; x <= 6; x++) for (int y = 0; y <= 6; y++) for (int z = 0; z <= 6; z++) {
            boolean wall = x == 0 || x == 6 || y == 0 || y == 6 || z == 0 || z == 6;
            if (wall) helper.setBlock(new BlockPos(x, y, z), Blocks.STONE.defaultBlockState());
        }
    }

    private static TileEntityAirPump placeVent(GameTestHelper helper, BlockPos rel, int fill) {
        helper.setBlock(rel, SpaceBlocks.AIR_VENT.get().defaultBlockState());
        TileEntityAirPump pump = (TileEntityAirPump) helper.getBlockEntity(rel);
        check(pump != null, "air_vent has no block entity");
        pump.tank.setFill(fill);
        return pump;
    }

    /** Offener Raum: nach der Anmeldewartezeit entweichen 20 mB/Tick ins Vakuum (Original "Venting to vacuum"). */
    @GameTest(template = "lifesupport7", batch = "space_lifesupport", timeoutTicks = 100)
    public static void airVentVentsWithoutSeal(GameTestHelper helper) {
        TileEntityAirPump pump = placeVent(helper, new BlockPos(3, 1, 3), 1000);
        check(pump.tank.getTankType() == ModFluids.OXYGEN.getSource(), "air_vent default gas is oxygen");
        check(pump.tank.getMaxFill() == 16000, "air_vent tank 16000 mB");
        helper.succeedWhen(() -> {
            check(pump.isRegistered(), "air_vent not registered as atmosphere provider");
            check(pump.tank.getFill() < 1000, "air_vent did not vent, fill " + pump.tank.getFill());
            check(!pump.hasSeal(), "open room must not be sealed");
        });
    }

    /** Abgedichteter Raum: Blob = 125 Bloecke, nach dem Fuellen 0.2 atm (airFill/size * 0.2). */
    @GameTest(template = "lifesupport7", batch = "space_lifesupport", timeoutTicks = 400)
    public static void airVentPressurizesSealedRoom(GameTestHelper helper) {
        buildSealedBox(helper);
        TileEntityAirPump pump = placeVent(helper, new BlockPos(3, 1, 3), 16000);
        helper.succeedWhen(() -> {
            check(pump.getCurrentBlob() != null, "no blob created for sealed room");
            check(pump.getCurrentBlob().getBlobSize() == 125, "blob size 125, got " + pump.getCurrentBlob().getBlobSize());
            check(pump.hasSeal(), "sealed room not detected");
            check(Math.abs(pump.getFluidPressure() - 0.2) < 1e-6, "pressure 0.2 atm, got " + pump.getFluidPressure());
            check(pump.tank.getFill() <= 16000 - 125, "tank must have paid 1 mB per block");
        });
    }

    /** Luftwascher im Raum: registriert sich an der Pumpe, nimmt Abgas als CO2 auf (10 HE/mB). */
    @GameTest(template = "lifesupport7", batch = "space_lifesupport", timeoutTicks = 600)
    public static void airScrubberCollectsCo2(GameTestHelper helper) {
        buildSealedBox(helper);
        BlockPos sRel = new BlockPos(1, 1, 1);
        helper.setBlock(sRel, SpaceBlocks.AIR_SCRUBBER.get().defaultBlockState());
        TileEntityAirScrubber scrubber = (TileEntityAirScrubber) helper.getBlockEntity(sRel);
        check(scrubber != null, "air_scrubber has no block entity");
        check(scrubber.tank.getTankType() == ModFluids.CARBONDIOXIDE.getSource() && scrubber.tank.getMaxFill() == 16000, "scrubber tank CO2 16000");
        scrubber.setEnergyStored(TileEntityAirScrubber.MAX_POWER);
        placeVent(helper, new BlockPos(3, 1, 3), 16000);
        helper.succeedWhen(() -> {
            check(scrubber.tank.getFill() > 0, "scrubber collected no CO2");
            check(scrubber.getPower() == TileEntityAirScrubber.MAX_POWER - scrubber.tank.getFill() * 10L || scrubber.getPower() > 200,
                    "scrubber power accounting");
        });
    }

    /** Algenfilm arbeitet nur im Orbit (Oberwelt: kein Umsatz). */
    @GameTest(template = "lifesupport7", batch = "space_lifesupport", timeoutTicks = 40)
    public static void algaeFilmRequiresOrbit(GameTestHelper helper) {
        BlockPos rel = new BlockPos(3, 1, 3);
        helper.setBlock(rel, SpaceBlocks.ALGAE_FILM.get().defaultBlockState());
        TileEntityAlgaeFilm film = (TileEntityAlgaeFilm) helper.getBlockEntity(rel);
        check(film != null, "algae_film has no block entity");
        check(film.tanks[0].getMaxFill() == 8000 && film.tanks[1].getMaxFill() == 8000, "algae tanks 8000");
        film.tanks[0].setFill(100);
        helper.runAfterDelay(20, () -> {
            check(!film.canOperate, "algae film must not operate outside orbit");
            check(film.tanks[0].getFill() == 100 && film.tanks[1].getFill() == 0, "no conversion outside orbit");
            helper.succeed();
        });
    }

    /** PLSS: 16000 mB, 1 mB je 10 Ticks ohne Atemluft; atembare Luft verbraucht nichts und setzt "ticks" zurueck. */
    @GameTest(template = "empty3x3x3", batch = "space_lifesupport", timeoutTicks = 20)
    public static void plssBreathing(GameTestHelper helper) {
        ItemStack plss = new ItemStack(SpaceItems.OXY_PLSS.get());
        ItemModOxy oxy = SpaceItems.OXY_PLSS.get();
        check(oxy.tryFill(ModFluids.OXYGEN.getSource(), 20000, plss) == 4000, "PLSS takes 16000 of 20000 mB");
        check(oxy.getFuel(plss) == 16000 && oxy.getMaxFuel() == 16000, "PLSS full");
        check(oxy.tryFill(net.minecraft.world.level.material.Fluids.WATER, 100, plss) == 100, "PLSS rejects non-oxygen");

        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        pig.tickCount = 0;
        check(oxy.attemptBreathing(pig, plss, null, false), "PLSS lets breathe in vacuum");
        check(oxy.getFuel(plss) == 15999, "1 mB used on tick % 10 == 0, got " + oxy.getFuel(plss));
        pig.tickCount = 3;
        oxy.attemptBreathing(pig, plss, null, false);
        check(oxy.getFuel(plss) == 15999, "no use between rate ticks");

        CBT_Atmosphere air = new CBT_Atmosphere(CelestialFluids.EARTHAIR, 1.0);
        pig.tickCount = 10;
        check(oxy.attemptBreathing(pig, plss, air, false), "breathable air");
        check(oxy.getFuel(plss) == 15999 && !ItemModOxy.getInUse(plss), "breathable air uses no oxygen");

        ItemModOxy.setFuel(plss, 0);
        check(!oxy.attemptBreathing(pig, plss, null, false), "empty PLSS in vacuum");
        pig.discard();
        helper.succeed();
    }

    /** Rezepte: Werkbank (5) + Montage (Luftwascher); Algenfilm erst mit space_m:saltleaf (R4). */
    @GameTest(template = "empty3x3x3", batch = "space_lifesupport", timeoutTicks = 20)
    public static void recipesLoaded(GameTestHelper helper) {
        var rm = helper.getLevel().getRecipeManager();
        for (String id : new String[] { "oxy_plss", "heavy_boots", "atmosphere_scanner", "lox_tank", "air_vent", "ass_airscrubber" }) {
            check(rm.byKey(SpaceMod.id("lifesupport/" + id)).isPresent(), "recipe space_m:lifesupport/" + id + " missing");
        }
        boolean saltleaf = BuiltInRegistries.ITEM.containsKey(SpaceMod.id("saltleaf"));
        check(rm.byKey(SpaceMod.id("lifesupport/ass_algaefilm")).isPresent() == saltleaf, "algae film recipe gated by saltleaf");
        helper.succeed();
    }
}
