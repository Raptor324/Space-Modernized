package com.space_m.fluid;

import java.util.List;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.fluid.FluidContainerDefs;
import com.hbm_m.inventory.fluid.FluidHazardSymbol;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FT_Combustible;
import com.hbm_m.inventory.fluid.trait.FT_Coolable;
import com.hbm_m.inventory.fluid.trait.FT_Flammable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Pheromone;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.hbm_m.inventory.fluid.trait.FluidTraitSimple;
import com.space_m.SpaceMod;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.block.SpaceBlocks;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.fluid.trait.FT_Rocket;
import com.space_m.machine.atmo.AtmoFluids;
import com.space_m.machine.atmo.AtmoTowerBlockEntity;
import com.space_m.machine.atmo.FT_Terraformer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R4-A: die 59 Space-Fluide gegen das Original ({@code Fluids.java} Space-Zweig): Registrierung ueber HbmFluidApi,
 * metaOrder, Traits/Temperatur/Gefahrenraute, Spreadsheet-Treibstoffe, Behaelter, Weltfluid-Eimer, Koerper-Atmosphaeren
 * und Atmo-Tower (Stickstoff).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceFluidsGameTest {

    private SpaceFluidsGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static int idx(Fluid f) {
        List<ModFluids.FluidEntry> list = HbmFluidRegistry.getOrderedFluids();
        for (int i = 0; i < list.size(); i++) if (list.get(i).getSource() == f) return i;
        return -1;
    }

    /** 59 Fluide registriert, HBM kennt sie (Name/Farbe), Reihenfolge = Space-metaOrder. */
    @GameTest(template = "empty3x3x3", batch = "space_fluids", timeoutTicks = 20)
    public static void registrationAndOrder(GameTestHelper helper) {
        List<SpaceFluids.Def> defs = SpaceFluids.definitions();
        check(defs.size() == 59, "59 Space-Fluide, ist " + defs.size());
        int last = -1;
        for (SpaceFluids.Def d : defs) {
            check(BuiltInRegistries.FLUID.containsKey(d.id()), d.id() + " nicht registriert");
            check(BuiltInRegistries.FLUID.get(d.id()) == d.fluid(), d.id() + " Registry != Entry");
            check(ModFluids.getEntry(d.name) == d.entry, d.name + " fehlt in ModFluids");
            check(FluidType.forFluid(d.fluid()).getName().equals(d.name), d.name + " FluidType-Name");
            check(FluidType.forFluid(d.fluid()).getColor() == d.color, d.name + " Farbe");
            int i = idx(d.fluid());
            check(i > last, d.name + " nicht in metaOrder-Reihenfolge (" + i + " <= " + last + ")");
            last = i;
        }
        check(idx(SpaceFluids.FLUORINE.fluid()) == idx(ModFluids.CHLORINE.getSource()) + 1, "FLUORINE hinter CHLORINE");
        int kr = idx(SpaceFluids.KRYPTON.fluid());
        check(idx(ModFluids.XENON.getSource()) == kr + 1 && idx(SpaceFluids.ARGON.fluid()) == kr + 2, "KRYPTON, XENON, ARGON");
        check(idx(SpaceFluids.SUPERHEATED_HYDROGEN.fluid()) == idx(ModFluids.MUG_HOT.getSource()) + 1, "SUPERHEATED_HYDROGEN hinter MUG_HOT");
        for (String n : SpaceFluids.NOT_REGISTERED) check(!BuiltInRegistries.FLUID.containsKey(SpaceMod.id(n)), n + " darf kein eigenes Fluid sein");
        helper.succeed();
    }

    /** Traits, Temperatur, Gefahrenraute, Trait-Reihenfolge, Spreadsheet-Werte, Heizketten, FT_Rocket an HBM-Fluiden. */
    @GameTest(template = "empty3x3x3", batch = "space_fluids", timeoutTicks = 20)
    public static void traits(GameTestHelper helper) {
        FT_Terraformer tf = FluidType.getTrait(SpaceFluids.AMMONIA.fluid(), FT_Terraformer.class);
        check(tf != null && "DUNA".equals(tf.getTarget()), "Ammoniak: Terraformer Ziel DUNA");
        check(FluidType.hasTrait(SpaceFluids.AMMONIA.fluid(), FluidTraitSimple.FT_Gaseous.class), "Ammoniak gasfoermig");

        FluidType n2 = FluidType.forFluid(SpaceFluids.NITROGEN.fluid());
        check(n2.getTemperature() == -90 && n2.hasTrait(FluidTraitSimple.FT_Gaseous_ART.class), "Stickstoff -90 °C, EVAP");
        check(n2.symbol == FluidHazardSymbol.CROYGENIC && n2.poison == 1, "Stickstoff Raute 1/0/0 kryogen");

        FluidType eve = FluidType.forFluid(SpaceFluids.EVEAIR.fluid());
        check(eve.poison == 4 && eve.flammability == 0 && eve.reactivity == 0 && eve.symbol == FluidHazardSymbol.OXIDIZER, "Eve-Luft Raute 4/0/0 OX");
        FluidType fl = FluidType.forFluid(SpaceFluids.FLUORINE.fluid());
        check(fl.poison == 4 && fl.flammability == 4 && fl.reactivity == 4, "Fluor 4/4/4");

        FT_Rocket hz = FluidType.getTrait(SpaceFluids.HYDRAZINE.fluid(), FT_Rocket.class);
        check(hz != null && hz.getISP() == 210 && hz.getThrust() == 600_000, "Hydrazin FT_Rocket(210, 600000)");
        FT_Rocket ker = FluidType.getTrait(ModFluids.KEROSENE.getSource(), FT_Rocket.class);
        check(ker != null && ker.getISP() == 308, "Kerosin FT_Rocket(308)");
        check(FluidType.forFluid(SpaceFluids.GASEOUS_SCHRABIDIUM_BROMIDE.fluid()).getTemperature() == 3000, "Schrabidiumbromid-Gas 3000 °C");

        FT_Flammable meth = FluidType.getTrait(SpaceFluids.METHANOL.fluid(), FT_Flammable.class);
        FT_Combustible methC = FluidType.getTrait(SpaceFluids.METHANOL.fluid(), FT_Combustible.class);
        check(meth != null && meth.getHeatEnergy() == 375_000, "Methanol 375000 TU, ist " + (meth == null ? -1 : meth.getHeatEnergy()));
        check(methC != null && methC.getCombustionEnergy() == 937_000, "Methanol Verbrennung 937000");

        FT_Heatable h2 = FluidType.getTrait(ModFluids.HYDROGEN.getSource(), FT_Heatable.class);
        check(h2 != null && h2.getFirstStep() != null && h2.getFirstStep().typeProduced == SpaceFluids.SUPERHEATED_HYDROGEN.fluid(), "H2 -> ueberhitzter H2");
        FT_Coolable ub = FluidType.getTrait(SpaceFluids.GASEOUS_URANIUM_BROMIDE.fluid(), FT_Coolable.class);
        check(ub != null && ub.coolsTo == SpaceFluids.URANIUM_BROMIDE.fluid(), "Uranbromid-Gas kuehlt zu Uranbromid");

        int ph = FluidTrait.traitList.indexOf(FT_Pheromone.class);
        check(FluidTrait.traitList.indexOf(FT_Rocket.class) == ph + 1 && FluidTrait.traitList.indexOf(FT_Terraformer.class) == ph + 2,
                "Traitliste: pheromone, rocket, terraformer");
        check(FluidTrait.traitNameMap.containsKey("uk") && FluidTrait.traitNameMap.containsKey("explosive"), "Traits uk/explosive");
        helper.succeed();
    }

    /** Kanister/Gasflaschen und Weltfluid-Eimer (bucket_bromine/ccl/mercury). */
    @GameTest(template = "empty3x3x3", batch = "space_fluids", timeoutTicks = 20)
    public static void containers(GameTestHelper helper) {
        check(FluidContainerDefs.getCanister(SpaceFluids.HYDRAZINE.fluid()) != null, "Hydrazin-Kanister");
        check(FluidContainerDefs.getGastank(SpaceFluids.DUNAAIR.fluid()) != null, "Duna-Luft-Gasflasche");
        check(FluidContainerDefs.getGastank(ModFluids.AIR.getSource()) != null, "EARTHAIR-Gasflasche an hbm_m:air");
        check(FluidContainerDefs.getCanister(SpaceFluids.MILK.fluid()) == null, "Milch ohne Kanister");

        bucket(SpaceMod.id("bucket_bromine"), SpaceFluids.BROMINE.fluid());
        bucket(SpaceMod.id("bucket_ccl"), SpaceFluids.CCL.fluid());
        bucket(SpaceMod.id("bucket_mercury"), ModFluids.MERCURY.getSource());
        check(FluidContainerRegistry.getFluidType(new ItemStack(net.minecraft.world.item.Items.MILK_BUCKET)) == SpaceFluids.MILK.fluid(), "Milcheimer -> Milch");
        check(FluidContainerRegistry.getFluidType(new ItemStack(com.space_m.dim.duna.DunaBlocks.DRY_ICE.get())) == ModFluids.CARBONDIOXIDE.getSource(), "Trockeneis -> CO2");
        helper.succeed();
    }

    private static void bucket(net.minecraft.resources.ResourceLocation id, Fluid fluid) {
        check(BuiltInRegistries.ITEM.containsKey(id), id + " fehlt");
        ItemStack s = new ItemStack(BuiltInRegistries.ITEM.get(id));
        check(FluidContainerRegistry.getFluidType(s) == fluid, id + " -> " + FluidType.forFluid(fluid).getName());
        check(FluidContainerRegistry.getFluidContent(s, fluid) == 1000, id + " 1000 mB");
    }

    /** Alle Atmosphaeren-Gase der Koerper sind registrierte Gase; Duna-Hauptgas = dunaair. Atmo-Tower erzeugt Stickstoff. */
    @GameTest(template = "empty3x3x3", batch = "space_fluids", timeoutTicks = 20)
    public static void bodiesAndTower(GameTestHelper helper) {
        for (CelestialBody body : CelestialBody.getAllBodies()) {
            CBT_Atmosphere atmo = body.getDefaultTrait(CBT_Atmosphere.class);
            if (atmo == null) continue;
            for (CBT_Atmosphere.FluidEntry e : atmo.fluids) {
                check(CelestialFluids.fluid(e.fluid) != null, body.name + ": Gas " + e.fluid + " nicht registriert");
                check(CelestialBody.isGas(e.fluid), body.name + ": " + e.fluid + " ist kein Gas");
            }
        }
        CBT_Atmosphere duna = CelestialBody.getBody("duna").getDefaultTrait(CBT_Atmosphere.class);
        check(duna != null && AtmoFluids.fluid(duna.getMainFluid()) == SpaceFluids.DUNAAIR.fluid(), "Duna-Hauptgas = space_m:dunaair");
        for (var id : List.of(CelestialFluids.EVEAIR, CelestialFluids.DUNAAIR, CelestialFluids.TEKTOAIR, CelestialFluids.NITROGEN,
                CelestialFluids.MILK, CelestialFluids.BROMINE, CelestialFluids.CCL, CelestialFluids.JOOLGAS, CelestialFluids.SARNUSGAS,
                CelestialFluids.UGAS, CelestialFluids.NGAS)) {
            check(CelestialFluids.fluid(id) != null, "CelestialFluids " + id + " nicht registriert");
        }

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        var state = SpaceBlocks.MACHINE_ATMO_TOWER.get().defaultBlockState();
        AtmoTowerBlockEntity be = new AtmoTowerBlockEntity(pos, state);
        be.setLevel(helper.getLevel());
        check(be.tanks[1].getTankType() == SpaceFluids.NITROGEN.fluid(), "Turm-Ausgang = Stickstoff");
        be.tanks[0].setFill(10_000);
        be.setEnergyStored(AtmoTowerBlockEntity.MAX_POWER);
        AtmoTowerBlockEntity.tick(helper.getLevel(), pos, state, be);
        check(be.tanks[1].getFill() == 100 && be.tanks[0].getFill() == 5_000, "Turm 50:1 -> 100 mB Stickstoff, ist " + be.tanks[1].getFill());
        check(SpaceBlockEntities.ATMOSPHERIC_TOWER.get() != null, "Turm-BE");
        helper.succeed();
    }
}
