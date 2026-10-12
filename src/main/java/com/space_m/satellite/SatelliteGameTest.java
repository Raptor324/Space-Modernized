package com.space_m.satellite;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.item.ISatChip;
import com.hbm_m.satellite.Satellite;
import com.hbm_m.satellite.SatelliteManager;
import com.hbm_m.satellite.SatelliteMapper;
import com.hbm_m.satellite.SatelliteRelay;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.dim.SpaceDimensions;
import com.space_m.rocket.RocketPayloadHooks;
import com.space_m.rocket.SpaceRocketItems;

import io.netty.buffer.Unpooled;
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
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R6-C: Satelliten-Registry nach HBM (IDs, Gegenstaende, Farben, Zielkoerper), Gegenstandsdaten und Einstellungspaket,
 * koerpergebundenes Aussetzen (Relais -> Duna, Orbit -> umkreister Koerper), Railkanone/Krieg, Client-Sync-Rundreise,
 * Raketen-Nutzlast-Haken.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SatelliteGameTest {

    private SatelliteGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
    }

    private static boolean colorIs(float[] c, float r, float g, float b) {
        return Math.abs(c[0] - r) < 1e-6 && Math.abs(c[1] - g) < 1e-6 && Math.abs(c[2] - b) < 1e-6;
    }

    /** XSatelliteRegistry: neue Klassen, Space-Zuordnung der sat_*-Nutzlasten, HBM-satellite_* bleiben, Farben, Ziele. */
    @GameTest(template = "empty3x3x3", batch = "space_satellite", timeoutTicks = 20)
    public static void registry(GameTestHelper helper) {
        SpaceSatelliteRegistry.ensureRegistered();
        check(Satellite.ID_TO_CLASS.get(SpaceSatelliteRegistry.ID_DYSON_RELAY) == SatelliteDysonRelay.class, "ID Dyson-Relais");
        check(Satellite.ID_TO_CLASS.get(SpaceSatelliteRegistry.ID_RAILGUN) == SatelliteRailgun.class, "ID Railkanone");
        check(Satellite.ITEM_TO_CLASS.get(SpaceSatelliteItems.SAT_WAR.get()) == SatelliteRailgun.class, "sat_war -> Railgun");
        check(Satellite.ITEM_TO_CLASS.get(SpaceSatelliteItems.SAT_DYSON_RELAY.get()) == SatelliteDysonRelay.class, "sat_dyson_relay");
        check(Satellite.ITEM_TO_CLASS.get(item("hbm_m:sat_mapper")) == SatelliteMapper.class, "hbm_m:sat_mapper -> Mapper (Space)");
        check(Satellite.ITEM_TO_CLASS.get(item("hbm_m:sat_foeq")) == SatelliteRelay.class, "hbm_m:sat_foeq -> Relay");
        check(Satellite.ITEM_TO_CLASS.get(item("hbm_m:satellite_spy")) == SatelliteMapper.class, "HBM satellite_spy bleibt (Soyuz)");
        check(Satellite.ITEM_TO_CLASS.get(item("hbm_m:satellite_precision_laser")) == com.hbm_m.satellite.SatellitePrecisionLaser.class, "HBM satellite_precision_laser bleibt");
        check(SpaceSatelliteRegistry.isSatelliteItem(SpaceSatelliteItems.SAT_RAY_SCAN.get()), "isSatelliteItem");
        check(!SpaceSatelliteRegistry.isSatelliteItem(SpaceRocketItems.RP_STATION_CORE_20.get()), "Stationskern ist kein Satellit");
        check(colorIs(SpaceSatelliteRegistry.getRegisteredColor(SatelliteDysonRelay.class), 1.0F, 0.9F, 0.8F), "Farbe Dyson");
        check(colorIs(SpaceSatelliteRegistry.getRegisteredColor(SatelliteMapper.class), 0.538F, 1.0F, 0.523F), "Farbe Mapper");
        check(SpaceSatelliteRegistry.getTargetBody(SatelliteRelay.class, SolarSystem.Body.KERBIN) == SolarSystem.Body.DUNA, "Relais -> Duna");
        check(SpaceSatelliteRegistry.getTargetBody(com.hbm_m.satellite.SatelliteLunarMiner.class, SolarSystem.Body.KERBIN) == SolarSystem.Body.MUN, "Mondbergbau -> Mun");
        check(SpaceSatelliteRegistry.getTargetBody(com.hbm_m.satellite.SatelliteMiner.class, SolarSystem.Body.KERBIN) == SolarSystem.Body.DRES, "Asteroidenbergbau -> Dres");
        check(SpaceSatelliteRegistry.getTargetBody(SatelliteRailgun.class, SolarSystem.Body.EVE) == SolarSystem.Body.EVE, "sonst Startkoerper");
        // Handstart nur bis 16 t (ItemSatellite())
        check(SatelliteItemLogic.canLaunchByHand(item("hbm_m:sat_mapper")), "sat_mapper von Hand");
        check(!SatelliteItemLogic.canLaunchByHand(SpaceSatelliteItems.SAT_PRECISION_LASER.get()), "Praezisionslaser nicht von Hand");
        helper.succeed();
    }

    /** SatelliteBase-Gegenstandsdaten, ensure/copy, receiveControl-Klemmung. */
    @GameTest(template = "empty3x3x3", batch = "space_satellite", timeoutTicks = 20)
    public static void itemData(GameTestHelper helper) {
        ItemStack stack = new ItemStack(SpaceSatelliteItems.SAT_DYSON_RELAY.get());
        SatelliteOrbit o = SatelliteItemData.read(stack);
        check(o.altitude == 100F && o.inclination == 0F && o.phaseOffset == 0F && !o.isBlinking && o.blinkPeriod == 0.3F
                && "None".equals(o.owner) && Math.abs(o.colorG - 0.9F) < 1e-6, "Standardwerte");
        SatelliteItemData.ensureItemData(stack);
        CompoundTag tag = com.hbm_m.platform.PlatformHooks.getItemTag(stack);
        check(tag != null && tag.contains("satAltitude") && tag.contains("satColorR") && tag.contains("satOwner"), "ensureItemData schreibt");

        CompoundTag ctl = new CompoundTag();
        ctl.putInt("satColorR", 300);
        ctl.putInt("satColorG", -5);
        ctl.putInt("satColorB", 51);
        ctl.putFloat("satAltitude", 200F);
        ctl.putFloat("satInclination", -500F);
        ctl.putFloat("satPhaseOffset", -90F);
        ctl.putString("satOwner", "Dev");
        ctl.putBoolean("satIsBlinking", true);
        ctl.putFloat("satBlink", 5F);
        SatSettingsPacket.receiveControl(stack, ctl);
        o = SatelliteItemData.read(stack);
        check(o.colorR == 1F && o.colorG == 0F && Math.abs(o.colorB - 0.2F) < 1e-6, "Farbe geklemmt");
        check(o.altitude == 125F && o.inclination == -180F && o.phaseOffset == 270F, "Bahn geklemmt/normalisiert");
        check(o.isBlinking && o.blinkPeriod == 1F && "Dev".equals(o.owner), "Blinken/Besitzer");

        ItemStack other = new ItemStack(SpaceSatelliteItems.SAT_DETECTOR.get());
        SatelliteItemData.copyItemData(stack, other);
        check(SatelliteItemData.getAltitude(other) == 125F && "Dev".equals(SatelliteItemData.getOwner(other)), "copyItemData");

        check(Math.abs(SatelliteOrbit.getOrbitSpeedKmPerSecond(100F) - (float) (2 * Math.PI * 100 / 30.0)) < 1e-3, "Bahngeschwindigkeit");
        check(SatelliteOrbit.applyPhaseOffsetToOrbitAngle(90F, 100F, 0, 360F) == 90F, "Phase");
        helper.succeed();
    }

    /** XSatelliteRegistry.orbit: Frequenz belegen, Bahndaten, Relais kreist um Duna, Orbit -> umkreister Koerper. */
    @GameTest(template = "empty3x3x3", batch = "space_satellite", timeoutTicks = 40)
    public static void orbitBodyBound(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().overworld();
        ServerLevel duna = helper.getLevel().getServer().getLevel(SpaceDimensions.DUNA);
        ServerLevel orbitDim = helper.getLevel().getServer().getLevel(SpaceDimensions.ORBIT);
        if (duna == null) {
            // NeoForge-GameTestServer laedt keine Datapack-Dimensionen (wie DimensionGameTest): nur Kerbin-Teil pruefen
            com.space_m.main.SpaceRegistry.LOGGER.info("[space_m] GameTestServer without datapack dimensions - satellite body binding checked on Kerbin only");
            int f = 77_160_009;
            try {
                ItemStack det = new ItemStack(SpaceSatelliteItems.SAT_DETECTOR.get());
                SatelliteItemData.setAltitude(det, 90F);
                SpaceSatellites.orbit(overworld, det, f, 0, 64, 0);
                check(SatelliteManager.get(overworld).getSatFromFreq(f) instanceof com.hbm_m.satellite.SatelliteDetector, "Detektor um Kerbin");
                SatelliteOrbit o = SpaceSatellites.getOrbit(overworld, f);
                check(o != null && o.altitude == 90F, "Bahndaten gespeichert");
                check(SatelliteManager.get(overworld).getSatFromFreq(f).addonData.contains(SpaceSatellites.ORBIT_KEY), "Bahndaten im HBM-Satelliten (W-R6C-1)");
                check(com.hbm_m.api.space.SpaceApi.satellites() instanceof SpaceSatelliteHooks, "HBM-Satelliten-Haken gesetzt (W-R6C-2)");
            } finally {
                SatelliteManager.get(overworld).remove(f);
                SatelliteOrbitSavedData.get(overworld).prune(SatelliteManager.get(overworld));
            }
            helper.succeed();
            return;
        }
        int f1 = 77_160_001, f2 = 77_160_002, f3 = 77_160_003;
        try {
            ItemStack det = new ItemStack(SpaceSatelliteItems.SAT_DETECTOR.get());
            ISatChip.setFreqS(det, f1);
            SatelliteItemData.setAltitude(det, 90F);
            SatelliteItemData.setOwner(det, "Tester");
            SpaceSatellites.orbit(overworld, det, f1, 0, 64, 0);
            check(SatelliteManager.get(overworld).getSatFromFreq(f1) instanceof com.hbm_m.satellite.SatelliteDetector, "Detektor um Kerbin");
            SatelliteOrbit o = SpaceSatellites.getOrbit(overworld, f1);
            check(o != null && o.altitude == 90F && "Tester".equals(o.owner), "Bahndaten gespeichert");
            check(SatelliteManager.get(overworld).getSatFromFreq(f1).addonData.contains(SpaceSatellites.ORBIT_KEY), "Bahndaten im HBM-Satelliten (W-R6C-1)");

            ItemStack relay = new ItemStack(item("hbm_m:sat_foeq"));
            SatelliteManager.orbitSatellite(overworld, relay, f2, 0, 64, 0); // HBM-Weg (Soyuz/Befehl) ueber den Haken
            check(SatelliteManager.get(overworld).getSatFromFreq(f2) == null, "Relais nicht um Kerbin");
            check(SatelliteManager.get(duna).getSatFromFreq(f2) instanceof SatelliteRelay, "Relais um Duna");
            check(SpaceSatellites.getLevelFromFreq(overworld, 0, 0, f2) == duna, "getDataFromFreq findet Duna");
            check(SpaceSatellites.getSatFromFreq(overworld, 0, 0, f2) instanceof SatelliteRelay, "getSatFromFreq");
            SatelliteOrbit ro = SpaceSatellites.getOrbit(duna, f2);
            check(ro != null && Math.abs(ro.colorG - 0.15F) < 1e-6, "Relais-Farbe");

            if (orbitDim != null) {
                // Ohne Station umkreist man Kerbin (OrbitHooks-Standard / R6-A)
                ServerLevel target = SpaceSatellites.getLevel(orbitDim, 0, 0);
                CelestialBody orbiting = CelestialBody.getTarget(orbitDim, 0, 0).body;
                check(target.dimension() == (orbiting.dimension != null ? orbiting.dimension : net.minecraft.world.level.Level.OVERWORLD), "Orbit -> umkreister Koerper");
                ItemStack radar = new ItemStack(item("hbm_m:sat_radar"));
                SpaceSatellites.orbit(orbitDim, radar, f3, 0, 64, 0);
                check(SatelliteManager.get(orbitDim).getSatFromFreq(f3) == null, "nichts in der Orbit-Dimension");
                check(SatelliteManager.get(target).getSatFromFreq(f3) != null, "Satellit beim umkreisten Koerper");
            }
        } finally {
            SatelliteManager.get(overworld).remove(f1);
            SatelliteManager.get(overworld).remove(f3);
            SatelliteManager.get(duna).remove(f2);
            SatelliteOrbitSavedData.get(overworld).prune(SatelliteManager.get(overworld));
            SatelliteOrbitSavedData.get(duna).prune(SatelliteManager.get(duna));
        }
        helper.succeed();
    }

    /** SatelliteRailgun: erster Schuss legt CBT_War an, weitere starten Projektile; Muendungsfeuer; NBT; Sync. */
    @GameTest(template = "empty3x3x3", batch = "space_satellite", timeoutTicks = 20)
    public static void railgunWar(GameTestHelper helper) {
        CelestialBody dres = CelestialBody.getBody("dres");
        boolean hadWar = dres.hasTrait(CBT_War.class);
        Map<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> saved = new HashMap<>(dres.getTraits());
        try {
            SatelliteRailgun rail = new SatelliteRailgun();
            rail.onClick(helper.getLevel(), 0, 0);
            check(!rail.canFire(), "ohne Ziel kein Schuss");
            rail.setTarget(dres);
            check(rail.hasTarget() && rail.getTarget() == dres, "Ziel gesetzt");
            if (!hadWar) {
                rail.onClick(helper.getLevel(), 0, 0);
                check(dres.hasTrait(CBT_War.class), "Krieg erklaert (CBT_War 100/0)");
                check(dres.getTrait(CBT_War.class).health == 100, "Gesundheit 100");
            }
            int before = dres.getTrait(CBT_War.class).getProjectiles().size();
            rail.onClick(helper.getLevel(), 0, 0);
            CBT_War war = dres.getTrait(CBT_War.class);
            check(war.getProjectiles().size() == before + 1, "Projektil gestartet");
            CBT_War.Projectile p = war.getProjectiles().get(war.getProjectiles().size() - 1);
            check(p.getType() == CBT_War.ProjectileType.SMALL && p.getDamage() == 50 && p.getSize() == 20 && p.getTravel() == 100F
                    && p.getTarget().equals(SpaceDimensions.DRES.location()), "Projektilwerte 1:1");
            check(rail.canFire(), "feuerbereit");
            rail.fire();
            check(rail.interp > 0F, "Muendungsfeuer laeuft");

            rail.lastOp = 1234L;
            CompoundTag nbt = new CompoundTag();
            rail.writeToNBT(nbt);
            SatelliteRailgun loaded = new SatelliteRailgun();
            loaded.readFromNBT(nbt);
            check(loaded.lastOp == 1234L, "NBT lastOp");

            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            rail.serialize(buf);
            SatelliteRailgun client = new SatelliteRailgun();
            client.deserialize(buf);
            check(client.interp == rail.interp && buf.readableBytes() == 0, "Sync interp");
        } finally {
            if (!hadWar) dres.clearTraits();
            else dres.setTraits(saved);
        }
        helper.succeed();
    }

    /** PermaSync-Abschnitt SATELLITES: Rundreise ueber den Client-Speicher. */
    @GameTest(template = "empty3x3x3", batch = "space_satellite", timeoutTicks = 20)
    public static void clientSync(GameTestHelper helper) {
        SpaceSatelliteRegistry.ensureRegistered();
        var beforeAll = SatelliteClientData.getClientSats();
        try {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf("duna");
            buf.writeInt(1);
            buf.writeUtf("duna");
            buf.writeInt(2);
            SatelliteOrbit o = SatelliteOrbit.withColor(new float[] {0.5F, 0.25F, 1F});
            o.altitude = 110F;
            o.owner = "Sync";
            buf.writeInt(5);
            buf.writeInt(SpaceSatelliteRegistry.ID_DYSON_RELAY);
            o.serialize(buf);
            SatelliteRailgun rail = new SatelliteRailgun();
            rail.interp = 42F;
            buf.writeInt(6);
            buf.writeInt(SpaceSatelliteRegistry.ID_RAILGUN);
            o.serialize(buf);
            rail.serialize(buf);
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);

            SatelliteSyncPacket.apply(bytes);
            var sats = SatelliteClientData.getClientSats();
            check(sats.size() == 2, "zwei Client-Satelliten");
            check(sats.get(5).satellite instanceof SatelliteDysonRelay && sats.get(5).orbit.altitude == 110F && "Sync".equals(sats.get(5).orbit.owner), "Relais-Daten");
            check(sats.get(6).satellite instanceof SatelliteRailgun r && r.interp == 42F, "Railkanone interp");
            check(SatelliteClientData.getClientSats(CelestialBody.getBody("duna")).size() == 2, "je Koerper");
            check(SatelliteClientData.getClientSats(CelestialBody.getBody("mun")).isEmpty(), "andere Koerper leer");
        } finally {
            SatelliteClientData.clear();
            SatelliteClientData.setClientSats(beforeAll);
        }
        helper.succeed();
    }

    /** RocketPayloadHooks (R5-A-Schnittstelle) sind gesetzt und arbeiten 1:1. */
    @GameTest(template = "empty3x3x3", batch = "space_satellite", timeoutTicks = 20)
    public static void payloadHooks(GameTestHelper helper) {
        RocketPayloadHooks hooks = RocketPayloadHooks.get();
        check(hooks instanceof SatelliteEvents.PayloadHooks, "R6-C-Haken gesetzt");
        check(hooks.isSatelliteItem(SpaceSatelliteItems.SAT_WAR.get()), "sat_war Nutzlast");
        check(!hooks.isSatelliteItem(SpaceRocketItems.RP_CAPSULE_20.get()), "Kapsel keine Nutzlast");
        ItemStack a = new ItemStack(item("hbm_m:sat_mapper"));
        SatelliteItemData.setInclination(a, 33F);
        ItemStack b = new ItemStack(item("hbm_m:sat_mapper"));
        hooks.copyItemData(a, b);
        check(SatelliteItemData.getInclination(b) == 33F, "copyItemData ueber Haken");
        check(SatelliteEvents.isPayloadSatellite(item("hbm_m:sat_mapper")) && SatelliteEvents.isHbmPayloadSatellite(item("hbm_m:sat_mapper")), "HBM-Nutzlast erkannt");
        check(!SatelliteEvents.isPayloadSatellite(item("hbm_m:satellite_spy")), "Soyuz-Satellit ist keine Raketennutzlast");
        helper.succeed();
    }
}
