package com.space_m.rocket;

import java.util.List;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.rocket.RocketPart.PartType;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R5-A: {@link RocketStruct}-Berechnungen gegen Original-Werte (Massen/Hoehen von Hand aus den Space-Teiledaten,
 * Treibstoffbedarf mit {@code scratchpad/sR5a/dv.py} = 1:1 Nachrechnung von {@code SolarSystem.getCostBetween} in Java-
 * float/double-Arithmetik), Validierung/Hinweise, Serialisierung (NBT, Netzwerk, Entity-Sync), Teile-Tabelle und die VAB
 * (Plattformen, Bauen, Zerlegen).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketStructGameTest {

    private RocketStructGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void eq(long expected, long actual, String what) {
        if (expected != actual) throw new GameTestAssertException(what + ": erwartet " + expected + ", ist " + actual);
    }

    private static void eqd(double expected, double actual, String what) {
        if (Math.abs(expected - actual) > 1e-6) throw new GameTestAssertException(what + ": erwartet " + expected + ", ist " + actual);
    }

    private static RocketPart part(String id) {
        RocketPart.resolve();
        RocketPart p = RocketPart.byId(ResourceLocation.tryParse(id));
        check(p != null && p.part != null, "Teil fehlt: " + id);
        return p;
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
    }

    /** rp_c_20 + rp_f_20_12 + mp_thruster_20_kerosene (Original: 8000 + 4000 + 2400 kg, 64000 mB, 1 536 000 N, 308 s). */
    private static RocketStruct single() {
        RocketStruct r = new RocketStruct(part("space_m:rp_c_20"));
        r.addStage(part("space_m:rp_f_20_12"), null, part("hbm_m:mp_thruster_20_kerosene"), 1, 1);
        return r;
    }

    /** Zweistufig: unten 2x rp_f_20_12 + Kerosin-Dreifach, oben rp_f_20_3 + Landebeine + Hydrolox. */
    private static RocketStruct twoStage() {
        RocketStruct r = new RocketStruct(part("space_m:rp_c_20"));
        r.addStage(part("space_m:rp_f_20_3"), part("space_m:rp_l_20"), part("space_m:mp_thruster_20_hydrogen"), 1, 1);
        r.addStage(part("space_m:rp_f_20_12"), null, part("hbm_m:mp_thruster_20_kerosene_triple"), 2, 1);
        return r;
    }

    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void partTable(GameTestHelper helper) {
        RocketPart.resolve();
        int resolved = 0;
        for (RocketPart p : RocketPart.all()) if (p.part != null) resolved++;
        // 149 Original-MissilePart-Eintraege (Duplikat mp_thruster_10_hydrazine entfernt); Space-Satelliten (5) erst ab R6
        check(RocketPart.all().size() == 149, "Teile-Tabelle: " + RocketPart.all().size());
        check(resolved >= 144, "aufgeloeste Teile: " + resolved);

        RocketPart t = part("space_m:mp_thruster_20_methalox");
        eq(890_000, t.getThrust(), "Schub methalox");
        eq(320, t.getISP(), "ISP methalox");
        eq(2400, t.mass, "Masse methalox");
        check(t.getFuel() == ModFluids.GAS.getSource() && t.getOxidizer() == ModFluids.OXYGEN.getSource(), "Methalox = Gas/Sauerstoff");
        RocketPart k = part("hbm_m:mp_thruster_15_kerosene");
        eq(120_000, k.getThrust(), "Schub HBM-Teil (Space-Wert)");
        check(k.getOxidizer() == ModFluids.PEROXIDE.getSource(), "Kerosin + Peroxid");
        RocketPart s = part("hbm_m:mp_thruster_10_solid");
        check(s.getFuel() == net.minecraft.world.level.material.Fluids.EMPTY && s.getOxidizer() == null, "Feststoff");
        RocketPart hz = part("space_m:mp_thruster_20_hydrazine");
        check(hz.getFuel() != null && "hydrazine".equals(BuiltInRegistries.FLUID.getKey(hz.getFuel()).getPath()), "Hydrazin");
        eq(PartType.FUSELAGE.ordinal(), part("space_m:mp_fuselage_20_hydrazine").type.ordinal(), "Typ");
        eqd(12.0, part("space_m:rp_f_20_12").height, "Hoehe rp_f_20_12");
        eqd(2.0, part("hbm_m:mp_thruster_20_kerosene").height, "Space-Hoehe mp_thruster_20_kerosene");
        check(part("hbm_m:sat_mapper").warhead == RocketPart.WarheadType.SATELLITE, "Satellit als Nutzlast");
        eq(16_000, part("hbm_m:sat_mapper").mass, "Satellitenmasse (ItemSatellite())");
        check(RocketPart.getPart(new ItemStack(item("hbm_m:mp_c_1"))) == null, "Chips sind keine Raketenteile");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void massesAndHeights(GameTestHelper helper) {
        RocketStruct r = single();
        check(r.validate(), "einstufig gueltig");
        eq(14_400, r.getLaunchMass(), "Trockenmasse");
        eq(30_400, r.getWetMass(0), "Nassmasse (Tank/4)");
        eq(1_536_000, r.getThrust(), "Schub");
        eqd(17.5, r.getHeight(), "Hoehe");

        RocketStruct t = twoStage();
        check(t.validate(), "zweistufig gueltig");
        eq(28_200, t.getLaunchMass(0), "Stufe 1 Startmasse (Oberstufe nass)");
        eq(60_200, t.getWetMass(0), "Stufe 1 nass");
        eq(11_800, t.getLaunchMass(1), "Stufe 2 trocken");
        eq(2_542_000, t.getThrust(), "Schub Kerosin-Dreifach");
        eqd(34.5, t.getHeight(), "Gesamthoehe");
        eqd(26.0, t.getHeight(0), "Hoehe Stufe 1");
        eqd(8.5, t.getHeight(1), "Hoehe Stufe 2 + Kapsel");
        eqd(26.0, t.getOffset(1), "Versatz Stufe 2");
        eq(2, t.stages.get(0).getStack(), "Stapel");
        eq(1, t.stages.get(0).getCluster(), "Buendel");

        Map<Fluid, Integer> fill = t.getFillRequirement();
        eq(128_000, fill.get(ModFluids.KEROSENE.getSource()), "Kerosin");
        eq(128_000, fill.get(ModFluids.PEROXIDE.getSource()), "Peroxid");
        eq(16_000, fill.get(ModFluids.HYDROGEN.getSource()), "Wasserstoff");
        eq(16_000, fill.get(ModFluids.OXYGEN.getSource()), "Sauerstoff");
        helper.succeed();
    }

    /** Referenzwerte aus sR5a/dv.py (Original-Formeln, Koerperdaten aus SolarSystem.init). */
    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void fuelRequirement(GameTestHelper helper) {
        CelestialBody kerbin = CelestialBody.getBody("kerbin");
        CelestialBody mun = CelestialBody.getBody("mun");
        CelestialBody minmus = CelestialBody.getBody("minmus");

        RocketStruct r = single();
        eq(26_600, r.getFuelRequired(0, kerbin, mun, false, false), "Kerbin -> Mun");
        eq(7_600, r.getFuelRequired(0, mun, kerbin, false, false), "Mun -> Kerbin");
        eq(5_600, r.getFuelRequired(0, kerbin, mun, true, false), "Kerbin-Orbit -> Mun");
        eq(64_000, r.getFuelCapacity(0), "Tank");
        check(r.hasSufficientFuel(kerbin, mun, false, false), "reicht zur Mun");

        RocketStruct t = twoStage();
        eq(44_600, t.getFuelRequired(0, kerbin, minmus, false, false), "Stufe 1 Kerbin -> Minmus");
        eq(6_300, t.getFuelRequired(1, minmus, kerbin, false, false), "Stufe 2 Minmus -> Kerbin");

        RocketStruct weak = new RocketStruct(part("space_m:rp_c_20"));
        weak.addStage(part("space_m:rp_f_20_12"), null, part("hbm_m:mp_thruster_10_kerosene"), 1, 1);
        eq(Integer.MAX_VALUE, weak.getFuelRequired(0, kerbin, mun, false, false), "zu wenig Schub");
        List<String> issues = weak.findIssues(0, kerbin, mun, false, false);
        check(issues.stream().anyMatch(s -> s.endsWith("Insufficient thrust")), "Hinweis zu wenig Schub: " + issues);

        List<String> ok = r.findIssues(0, kerbin, mun, false, false);
        check(ok.contains("Dry mass: 14400kg") && ok.contains("Wet mass: 30400kg") && ok.contains("Thrust: 1536000N") && ok.contains("ISP: 308s"), "Werte: " + ok);
        check(ok.stream().anyMatch(s -> s.endsWith("Trip possible! 64000/26600mB")), "Reise moeglich: " + ok);

        // Kapsel ohne Stufen: nur aus dem Orbit auf den umkreisten Koerper
        RocketStruct capsule = new RocketStruct(part("space_m:rp_c_20"));
        check(capsule.hasSufficientFuel(kerbin, kerbin, true, false) && !capsule.hasSufficientFuel(kerbin, mun, true, false), "Kapsel allein");
        RocketStruct pod = new RocketStruct(part("space_m:rp_pod_20"));
        check(pod.hasSufficientFuel(mun, mun, true, false) && !pod.hasSufficientFuel(mun, mun, false, false), "Drop-Pod");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void validation(GameTestHelper helper) {
        RocketStruct mismatch = new RocketStruct(part("space_m:rp_c_20"));
        mismatch.addStage(part("space_m:mp_fuselage_10_hydrazine"), null, part("hbm_m:mp_thruster_20_kerosene"), 1, 1);
        check(!mismatch.validate(), "Treibstoff passt nicht");
        check(mismatch.findIssues(0, null, null, false, false).contains("§cStage 1 fuel mismatch"), "Hinweis fuel mismatch");

        RocketStruct uneven = new RocketStruct(part("space_m:rp_c_20"));
        uneven.addStage(part("space_m:rp_f_20_6"), null, part("hbm_m:mp_thruster_20_kerosene"), 3, 2);
        check(!uneven.validate(), "ungerade Triebwerke");
        check(uneven.findIssues(0, null, null, false, false).contains("§cStage 1 uneven thrusters"), "Hinweis uneven");

        RocketStruct legs = new RocketStruct(part("space_m:rp_c_20"));
        legs.addStage(part("space_m:rp_f_20_3"), null, part("space_m:mp_thruster_20_hydrogen"), 1, 1);
        legs.addStage(part("space_m:rp_f_20_12"), null, part("hbm_m:mp_thruster_20_kerosene"), 1, 1);
        check(legs.validate(), "ohne Beine trotzdem gueltig");
        check(legs.findIssues(0, null, null, false, false).contains("§eStage 2 lacks landing legs"), "Hinweis Beine");

        RocketStruct warhead = new RocketStruct(part("hbm_m:mp_warhead_15_he"));
        warhead.addStage(part("space_m:rp_f_20_12"), null, part("hbm_m:mp_thruster_20_kerosene"), 1, 1);
        check(!warhead.validate(), "Sprengkopf ist keine Kapsel");
        check(warhead.findIssues(0, null, null, false, false).contains("§cInvalid Capsule/Satellite"), "Hinweis Kapsel");

        RocketStruct station = new RocketStruct(part("space_m:rp_sc_20"));
        station.addStage(part("space_m:rp_f_20_12"), null, part("hbm_m:mp_thruster_20_kerosene"), 1, 1);
        check(station.validate(), "Stationskern gueltig");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void serialization(GameTestHelper helper) {
        RocketStruct t = twoStage();
        t.satFreq = 1234;

        CompoundTag nbt = new CompoundTag();
        t.writeToNBT(nbt);
        RocketStruct n = RocketStruct.readFromNBT(nbt);
        check(n.capsule == t.capsule && n.stages.size() == 2 && n.stages.get(0).fuselageCount == 2
                && n.stages.get(1).fins == t.stages.get(1).fins && n.getLaunchMass() == t.getLaunchMass(), "NBT");
        eq(1234, nbt.getInt("freq"), "Frequenz");

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        t.addIssue("VAB ceiling too low ");
        t.writeToByteBuffer(buf);
        RocketStruct b = RocketStruct.readFromByteBuffer(buf);
        check(b.stages.size() == 2 && b.stages.get(1).thruster == t.stages.get(1).thruster && b.extraIssues.size() == 1, "Netzwerk");

        int[] w = t.writeToWatchable();
        eq(RocketStruct.WATCHABLE_SIZE, w.length, "Sync-Laenge");
        RocketStruct d = RocketStruct.readFromWatchable(w);
        check(d.capsule == t.capsule && d.stages.get(0).fuselage == t.stages.get(0).fuselage && d.stages.get(0).fuselageCount == 2
                && d.stages.get(0).thrusterCount == 1 && d.stages.get(1).fins == t.stages.get(1).fins, "Entity-Sync");
        RocketStruct empty = RocketStruct.readFromWatchable(new int[RocketStruct.WATCHABLE_SIZE]);
        check(empty.capsule != null && empty.capsule.part == SpaceRocketItems.RP_CAPSULE_20.get(), "Notfall-Kapsel");

        ItemStack stack = ItemCustomRocket.build(single(), true);
        RocketStruct back = ItemCustomRocket.get(stack);
        check(back != null && back.validate() && ItemCustomRocket.hasFuel(stack), "rocket_custom");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void navDrive(GameTestHelper helper) {
        ItemStack drive = ItemVOTVdrive.of(com.space_m.celestial.SolarSystem.Body.MUN);
        check(ItemVOTVdrive.getBody(drive) == com.space_m.celestial.SolarSystem.Body.MUN, "Koerper");
        ItemVOTVdrive.setCoordinates(drive, 100, -200);
        ItemVOTVdrive.Destination dest = ItemVOTVdrive.getDestination(drive);
        check(dest.x() == 100 && dest.z() == -200 && !ItemVOTVdrive.getProcessed(drive), "Ziel");
        ItemVOTVdrive.Target target = ItemVOTVdrive.getTarget(drive, helper.getLevel());
        check(target.body() == CelestialBody.getBody("mun") && !target.inOrbit() && target.isValid(), "Target");
        ItemVOTVdrive.Destination approx = ItemVOTVdrive.getApproximateDestination(drive);
        check(approx.equals(ItemVOTVdrive.getApproximateDestination(drive)), "Naeherung stabil");
        helper.succeed();
    }

    /** VAB: Struktur, Buehnen auf Original-Hoehen, Bauen und Zerlegen. */
    @GameTest(template = "empty3x3x3", batch = "space_rocket_vab", timeoutTicks = 60)
    public static void assembly(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(1, 4, 1));
        level.setBlock(core, SpaceRocketBlocks.MACHINE_ROCKET_ASSEMBLY.get().defaultBlockState(), 3);
        check(level.getBlockEntity(core) instanceof MachineRocketAssemblyBlockEntity, "VAB-Kern");
        MachineRocketAssemblyBlockEntity be = (MachineRocketAssemblyBlockEntity) level.getBlockEntity(core);
        // GameTest-Barrierendecke ueber dem Testbereich entfernen (Buehnen reichen bis +29)
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++) for (int h = 1; h <= 40; h++) {
            if (!level.getBlockState(core.offset(x, h, z)).isAir()) level.setBlock(core.offset(x, h, z), Blocks.AIR.defaultBlockState(), 3);
        }

        // Oberstufe in die oberen Slots (1-3), Unterstufe darunter (4-6) - wie im GUI
        be.getInventory().setStackInSlot(0, new ItemStack(SpaceRocketItems.RP_CAPSULE_20.get()));
        be.getInventory().setStackInSlot(1, new ItemStack(SpaceRocketItems.RP_FUSELAGE_20_3.get()));
        be.getInventory().setStackInSlot(2, new ItemStack(SpaceRocketItems.RP_LEGS_20.get()));
        be.getInventory().setStackInSlot(3, new ItemStack(SpaceRocketItems.MP_THRUSTER_20_HYDROGEN.get()));
        be.getInventory().setStackInSlot(4, new ItemStack(SpaceRocketItems.RP_FUSELAGE_20_12.get(), 2));
        be.getInventory().setStackInSlot(6, new ItemStack(item("hbm_m:mp_thruster_20_kerosene_triple")));
        be.updateEntity();

        check(be.rocket.validate(), "Bauplan gueltig: " + be.rocket.findIssues(0, null, null, false, false));
        eq(28_200, be.rocket.getLaunchMass(), "Startmasse wie Struct-Test");
        // Plattformen: Stufe 1 bei 12*2 + 2 = 26, Stufe 2 bei 26 + 3 = 29; Saeule bis 29
        check(level.getBlockState(core.offset(-3, 26, 0)).getBlock() instanceof RocketAssemblyPartBlock, "Buehne 26");
        check(level.getBlockState(core.offset(0, 29, 3)).getBlock() instanceof RocketAssemblyPartBlock, "Buehne 29");
        check(level.getBlockState(core.above(29)).getBlock() instanceof RocketAssemblyPartBlock, "Saeule 29");
        check(level.getBlockState(core.above(30)).isAir(), "ueber der Saeule frei");
        check(RocketAssemblyPartBlock.findCore(level, core.offset(-3, 26, 0)).equals(core), "Buehne findet Kern");
        check(RocketAssemblyPartBlock.findCore(level, core.offset(4, 10, 4)).equals(core), "Ecksaeule findet Kern");

        be.construct();
        ItemStack built = be.getInventory().getStackInSlot(MachineRocketAssemblyBlockEntity.SLOT_RESULT);
        check(built.getItem() == SpaceRocketItems.ROCKET_CUSTOM.get(), "Rakete gebaut");
        check(be.getInventory().getStackInSlot(1).isEmpty(), "Teile verbraucht");
        be.updateEntity(); // Hoehe 0 -> Buehnen weg
        check(!(level.getBlockState(core.offset(-3, 26, 0)).getBlock() instanceof RocketAssemblyPartBlock), "Buehnen entfernt");

        check(be.canDeconstruct(), "zerlegbar");
        be.deconstruct();
        eq(2, be.getInventory().getStackInSlot(4).getCount(), "Rumpfe zurueck");
        check(be.getInventory().getStackInSlot(2).getItem() == SpaceRocketItems.RP_LEGS_20.get(), "Beine zurueck");

        // Decke zu niedrig
        level.setBlock(core.above(10), Blocks.STONE.defaultBlockState(), 3);
        be.updateEntity();
        check(be.rocket.extraIssues.contains("§cVAB ceiling too low "), "Deckenhinweis: " + be.rocket.extraIssues);
        level.setBlock(core.above(10), Blocks.AIR.defaultBlockState(), 3);

        for (int i = 0; i < MachineRocketAssemblyBlockEntity.SLOT_COUNT; i++) be.getInventory().setStackInSlot(i, ItemStack.EMPTY);
        level.destroyBlock(core, false);
        helper.succeed();
    }
}
