package com.space_m.test;

import java.util.HashMap;
import java.util.List;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.api.space.SpaceEnvironment;
import com.space_m.SpaceMod;
import com.space_m.celestial.AstronomyUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.CelestialTraitsPacket;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Dyson;
import com.space_m.celestial.trait.CBT_Temperature;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CBT_Water;
import com.space_m.celestial.trait.CBT_Weather;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R1-A: Himmelskoerper-Daten gegen das Original (NTM Space {@code SolarSystem.init}, {@code CelestialBody},
 * {@code AstronomyUtil}) und die {@link SpaceEnvironment}-Implementierung.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CelestialGameTest {

    private CelestialGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static void near(double actual, double expected, double eps, String msg) {
        check(Math.abs(actual - expected) <= eps, msg + ": erwartet " + expected + ", ist " + actual);
    }

    private static CelestialBody body(String name) {
        CelestialBody b = CelestialBody.getBodyOrNull(name);
        check(b != null, "Koerper fehlt: " + name);
        return b;
    }

    /** 32 Koerper in Original-Reihenfolge, Baumstruktur, 10 landbare Koerper mit Dimension. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void solarSystemStructure(GameTestHelper helper) {
        List<String> expected = List.of("kerbol", "moho", "eve", "gilly", "kerbin", "mun", "minmus", "duna", "ike", "dres",
                "jool", "laythe", "vall", "tylo", "bop", "pol", "sarnus", "hale", "ovok", "eeloo", "slate", "tekto",
                "urlum", "polta", "priax", "wal", "tal", "neidon", "thatmo", "nissee", "plock", "karen");
        List<String> names = CelestialBody.getAllBodies().stream().map(b -> b.name).toList();
        check(names.equals(expected), "Koerperliste " + names);
        check(SolarSystem.kerbol.parent == null && SolarSystem.kerbol.satellites.size() == 10, "Kerbol mit 10 Planeten");
        check(body("mun").parent == body("kerbin") && body("tal").parent == body("wal") && body("wal").parent == body("urlum"), "Monde");
        check(body("mun").getPlanet() == body("kerbin") && body("mun").getStar() == SolarSystem.kerbol, "getPlanet/getStar");
        check(CelestialBody.getLandableBodies().size() == 10, "10 landbare Koerper, ist " + CelestialBody.getLandableBodies().size());
        check(SpaceDimensions.ALL.size() == 11 && SpaceDimensions.BODIES.size() == 10, "11 Dimensionen");

        // Zuordnung Koerper <-> Dimension
        check(CelestialBody.getBody(SpaceDimensions.MUN) == body("mun"), "Mun-Dimension");
        check(CelestialBody.getBody(SpaceDimensions.LAYTHE) == body("laythe"), "Laythe-Dimension");
        check(CelestialBody.getBody(Level.OVERWORLD) == body("kerbin"), "Overworld = Kerbin");
        check(CelestialBody.getBody(SpaceDimensions.ORBIT) == body("kerbin"), "Orbit faellt wie im Original auf Kerbin");
        check(CelestialBody.getBody(Level.NETHER) == body("kerbin"), "unbekannt -> Kerbin");
        check(body("mun").legacyDimensionId == 413_015 && body("thatmo").legacyDimensionId == 413_025, "Original-IDs");
        check(body("mun").getEnum() == SolarSystem.Body.MUN && SolarSystem.Body.MUN.getDimension() == SpaceDimensions.MUN, "Body-Enum");
        check(SolarSystem.Body.ORBIT.getDimension() == SpaceDimensions.ORBIT, "Body.ORBIT");
        helper.succeed();
    }

    /** Schwerkraft wie im Original: G*M/r^2, NTM-Einheit = * 1.6/9.81. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void gravityMatchesOriginal(GameTestHelper helper) {
        near(body("mun").getSurfaceGravity(), 1.62853, 1e-4, "Mun m/s^2");
        near(CelestialEnvironment.gravityOf(body("mun")), 0.26561, 1e-4, "Mun NTM");
        near(body("kerbin").getSurfaceGravity(), 9.81123, 1e-3, "Kerbin m/s^2");
        near(CelestialEnvironment.gravityOf(body("kerbin")), 1.60020, 1e-4, "Kerbin NTM");
        near(body("minmus").getSurfaceGravity(), 0.49056, 1e-4, "Minmus m/s^2");
        near(body("duna").getSurfaceGravity(), 2.94282, 1e-3, "Duna m/s^2");
        near(body("eve").getSurfaceGravity(), 16.6721, 1e-2, "Eve m/s^2");
        check(AstronomyUtil.STANDARD_GRAVITY == SpaceEnvironment.STANDARD_GRAVITY
                && AstronomyUtil.PLAYER_GRAVITY_MODIFIER == SpaceEnvironment.PLAYER_GRAVITY_MODIFIER, "Konstanten = HBM-API");

        // ueber die HBM-API (Testwelt = Overworld = Kerbin)
        ServerLevel level = helper.getLevel();
        check(SpaceApi.isInstalled() && SpaceApi.env() instanceof CelestialEnvironment, "SpaceEnvironment angemeldet");
        near(SpaceApi.env().gravity(level, helper.absolutePos(BlockPos.ZERO)), 1.60020, 1e-4, "API-Schwerkraft Kerbin");
        helper.succeed();
    }

    /** Bahn-/Rotationsperioden und Sonnenleistung. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void orbitsAndSunMatchOriginal(GameTestHelper helper) {
        // SolarSystem.runTests: "should be" 426 / 261 / 6 KSP-Tage
        near(body("kerbin").getOrbitalPeriod(), 426.09, 0.5, "Kerbin-Jahr");
        near(body("eve").getOrbitalPeriod(), 261.9, 1.5, "Eve-Jahr");
        near(body("mun").getOrbitalPeriod(), 6.43, 0.05, "Mun-Umlauf");
        // Tageslaenge in Ticks: Sekunden * (1200/21600) * 20
        near(body("mun").getRotationalPeriod(), 138_984 * (1200.0 / 21600.0) * 20, 1e-6, "Mun-Tag");
        near(body("kerbin").getRotationalPeriod(), 21_549 * (1200.0 / 21600.0) * 20, 1e-6, "Kerbin-Tag");
        near(body("kerbin").getSunPower(), 1.0, 1e-6, "Sonne Kerbin");
        near(body("mun").getSunPower(), 1.0, 1e-6, "Sonne Mun (Planet Kerbin)");
        near(body("duna").getSunPower(), 0.43055, 1e-4, "Sonne Duna");
        near(body("moho").getSunPower(), 6.6768, 1e-3, "Sonne Moho");
        double dv = SolarSystem.getDeltaVBetween(body("kerbin"), body("mun"));
        check(dv > 0 && Double.isFinite(dv), "Delta-v Kerbin->Mun " + dv);
        check(SolarSystem.getFuelCost(dv, 10_000, 300) % 100 == 0, "Treibstoff in 100er-Schritten");
        check(CelestialEnvironment.getEclipseFactor(List.of(), 1, SolarSystem.MAX_APPARENT_SIZE_SURFACE) == 0, "keine Finsternis ohne Koerper");
        helper.succeed();
    }

    /** Standard-Traits wie {@code SolarSystem.init}. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void defaultTraitsMatchOriginal(GameTestHelper helper) {
        CBT_Atmosphere eve = body("eve").getDefaultTrait(CBT_Atmosphere.class);
        check(eve != null && eve.getPressure() == 5D && eve.hasFluid(CelestialFluids.EVEAIR), "Eve 5 atm eveair");
        CBT_Temperature eveT = body("eve").getDefaultTrait(CBT_Temperature.class);
        check(eveT != null && eveT.degrees == 400, "Eve 400 C");
        CBT_Water eveW = body("eve").getDefaultTrait(CBT_Water.class);
        check(eveW != null && eveW.fluid.equals(CelestialFluids.MERCURY), "Eve Quecksilber");
        CBT_Atmosphere laythe = body("laythe").getDefaultTrait(CBT_Atmosphere.class);
        check(laythe != null && Math.abs(laythe.getPressure() - 0.6) < 1e-9 && laythe.getPressure(CelestialFluids.XENON) == 0.15D, "Laythe 0.45 Luft + 0.15 Xenon");
        check(body("mun").getDefaultTrait(CBT_Atmosphere.class) == null && body("mun").hasIce, "Mun ohne Atmosphaere, mit Eis");
        CBT_Water kerbinW = body("kerbin").getDefaultTrait(CBT_Water.class);
        check(kerbinW != null && kerbinW.fluid.equals(SpaceEnvironment.HBM_WATER), "Kerbin Wasser");
        check(body("dres").hasRings && body("sarnus").hasRings && body("urlum").hasRings && !body("jool").hasRings, "Ringe");
        check(CelestialFluids.JOOLGAS.equals(body("jool").gas) && body("thatmo").hasDefaultTrait(CelestialBodyTrait.CBT_BATTLEFIELD.class), "Gas/Battlefield");
        check(CelestialBodyTrait.traitList.size() == 13 && "invasion".equals(CelestialBodyTrait.traitMap.inverse().get(com.space_m.celestial.trait.CBT_Invasion.class)), "13 Traits");
        check(!CBT_Weather.supportsWeather(body("mun")) && CBT_Weather.supportsWeather(body("kerbin")), "Wetter nur mit Luft+Wasser");
        helper.succeed();
    }

    /** HBM-API auf Kerbin (Overworld) = Erdwerte; Koerperkennung. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void environmentOnKerbin(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        SpaceEnvironment env = SpaceApi.env();
        CelestialBody.clearTraits(level);
        near(env.pressure(level, pos), 1.0, 1e-9, "Druck 1 atm");
        near(env.gasPressure(level, pos, SpaceEnvironment.HBM_AIR), 1.0, 1e-9, "Luft 1 atm");
        check(env.temperature(level, pos) == 20, "20 C");
        check(SpaceEnvironment.HBM_WATER.equals(env.waterTableFluid(level)), "Wassertabelle");
        check(SpaceEnvironment.EARTH.equals(env.body(level)) && SpaceEnvironment.EARTH.equals(env.targetBody(level, pos)), "Kerbin = hbm_m:earth");
        check(env.consumeOxygen(level, pos, 1) && !env.inOrbit(level), "Verbrennung/kein Orbit");
        float sun = env.solarIntensity(level, pos);
        check(sun >= 0 && sun <= 1.0F, "Sonnenleistung 0..1, ist " + sun);
        check(CelestialEnvironment.bodyId(body("mun")).equals(SpaceMod.id("mun")) && CelestialEnvironment.bodyFromId(SpaceMod.id("duna")) == body("duna"), "bodyId");
        helper.succeed();
    }

    /** Terraforming ueber SavedData: Gas abgeben/entnehmen, Ueberschreibung loeschen. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void terraformingSavedData(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(BlockPos.ZERO);
        try {
            CelestialBody.emitGas(level, CelestialFluids.OXYGEN, AstronomyUtil.MB_PER_ATM * 0.5);
            CBT_Atmosphere atmo = CelestialBody.getTrait(level, CBT_Atmosphere.class);
            check(atmo != null && Math.abs(atmo.getPressure(CelestialFluids.OXYGEN) - 0.5) < 1e-9, "O2 0.5 atm abgegeben");
            near(SpaceApi.env().pressure(level, pos), 1.5, 1e-9, "Gesamtdruck 1.5 atm");
            check(CelestialBody.consumeGas(level, CelestialFluids.OXYGEN, AstronomyUtil.MB_PER_ATM), "O2 entnommen");
            atmo = CelestialBody.getTrait(level, CBT_Atmosphere.class);
            check(atmo != null && !atmo.hasFluid(CelestialFluids.OXYGEN), "O2-Eintrag entfernt");
            // Ohne Atmosphaere brennt nichts (degas)
            CelestialBody.degas(level);
            check(!SpaceApi.env().consumeOxygen(level, pos, 0), "Vakuum: keine Verbrennung");
            check(SpaceApi.env().pressure(level, pos) == 0.0, "Vakuum: 0 atm");
        } finally {
            CelestialBody.clearTraits(level);
        }
        check(CelestialBody.getTrait(level, CBT_Atmosphere.class) == body("kerbin").getDefaultTrait(CBT_Atmosphere.class)
                && body("kerbin").getDefaultTrait(CBT_Atmosphere.class).fluids.size() == 1, "Standard wiederhergestellt (unveraendert)");
        helper.succeed();
    }

    /** NBT- und Sync-Rundreise aller Trait-Daten. */
    @GameTest(template = "empty3x3x3", batch = "space_celestial", timeoutTicks = 20)
    public static void traitSerialization(GameTestHelper helper) {
        SolarSystemWorldSavedData data = new SolarSystemWorldSavedData();
        CBT_War war = new CBT_War(80, 5);
        war.launchProjectile(20, 2, 7, 1, 2, 3, CBT_War.ProjectileType.NUCLEAR, SpaceDimensions.DUNA.location());
        CBT_Weather weather = new CBT_Weather();
        weather.forceThunder(123);
        CBT_Dyson dyson = new CBT_Dyson();
        data.setTraits("mun", new CBT_Atmosphere(CelestialFluids.NITROGEN, 0.25).and(CelestialFluids.EARTHAIR, 0.5),
                new CBT_Temperature(-150), war, weather, dyson, CelestialBodyTrait.COMP);
        data.setTraits("duna", new CBT_Water(CelestialFluids.BROMINE));

        CompoundTag tag = data.toNBT();
        check(tag.contains("b_mun") && tag.getCompound("b_mun").contains("infected"), "NBT-Schluessel wie Original");
        SolarSystemWorldSavedData loaded = SolarSystemWorldSavedData.load(tag);
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> mun = loaded.getTraits("mun");
        check(mun != null && mun.size() == 6, "6 Traits geladen");
        CBT_Atmosphere atmo = (CBT_Atmosphere) mun.get(CBT_Atmosphere.class);
        check(atmo.getPressure(CelestialFluids.NITROGEN) == 0.25 && atmo.getPressure(CelestialFluids.EARTHAIR) == 0.5, "Atmosphaere NBT");
        check(((CBT_Temperature) mun.get(CBT_Temperature.class)).degrees == -150, "Temperatur NBT");
        CBT_War w = (CBT_War) mun.get(CBT_War.class);
        check(w.health == 80 && w.shield == 5 && w.projectiles.size() == 1
                && w.projectiles.get(0).getTarget().equals(SpaceDimensions.DUNA.location()), "Krieg NBT");
        check(((CBT_Weather) mun.get(CBT_Weather.class)).thundering, "Wetter NBT");
        check(((CBT_Water) loaded.getTraits("duna").get(CBT_Water.class)).fluid.equals(CelestialFluids.BROMINE), "Wasser NBT");

        // Sync-Paket (Client-Map sichern/wiederherstellen)
        var before = SolarSystemWorldSavedData.clientTraits;
        try {
            SolarSystemWorldSavedData.updateClientTraits(new HashMap<>());
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            CelestialTraitsPacket.writeTraits(buf, loaded);
            CelestialTraitsPacket.readTraits(buf);
            check(buf.readableBytes() == 0, "Puffer vollstaendig gelesen");
            HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> client = SolarSystemWorldSavedData.getClientTraits("mun");
            check(client != null && client.size() == 6, "Client-Traits Mun");
            CBT_Atmosphere ca = (CBT_Atmosphere) client.get(CBT_Atmosphere.class);
            check(Math.abs(ca.getPressure() - 0.75) < 1e-6, "Client-Atmosphaere");
            // Original-Eigenheit: readFromBytes liest shield/health vertauscht
            CBT_War cw = (CBT_War) client.get(CBT_War.class);
            check(cw.shield == 80 && cw.health == 5, "Krieg-Sync 1:1 (vertauscht)");
            check(SolarSystemWorldSavedData.getClientTraits("kerbin") == null, "Kerbin ohne Ueberschreibung");
        } finally {
            SolarSystemWorldSavedData.updateClientTraits(before);
        }

        ResourceLocation parsed = CelestialFluids.parse("");
        check(parsed.equals(CelestialFluids.NONE), "leere Fluid-ID");
        helper.succeed();
    }
}
