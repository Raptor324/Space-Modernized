package com.space_m.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.hbm_m.api.space.SpaceApi;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialTraitsPacket;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Destroyed;
import com.space_m.celestial.trait.CelestialBodyTrait;
import com.space_m.item.SpaceWandD;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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
 * Zerstoerte Sonne (NTM Space {@code ItemWandD}, schleichend in den Himmel): {@code CBT_Destroyed} an Kerbol,
 * {@code degas} entfernt die Kerbin-Atmosphaere, zweiter Aufruf ("kidding") setzt beides zurueck; Sync zum Client
 * und {@code /space destroystar} (Abweichung, Nutzerwunsch).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DestroyedStarGameTest {

    private DestroyedStarGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_destroyed_star", timeoutTicks = 20)
    public static void destroyStarToggles(GameTestHelper helper) {
        ServerLevel overworld = helper.getLevel().getServer().getLevel(Level.OVERWORLD);
        check(overworld != null, "Oberwelt vorhanden");
        check(SpaceApi.wandD() == SpaceWandD.INSTANCE && SpaceApi.wandD().active(), "wand_d-Haken = Space");

        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get(overworld);
        check(data != null, "Sonnensystem-Daten");
        CelestialBody star = CelestialBody.getStar(overworld);
        CelestialBody kerbin = CelestialBody.getBody(overworld);
        check(star == SolarSystem.kerbol, "Stern der Oberwelt = Kerbol");

        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> starBefore = data.getTraits(star.name);
        HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> kerbinBefore = data.getTraits(kerbin.name);
        var clientBefore = SolarSystemWorldSavedData.clientTraits;
        try {
            data.clearTraits(star.name);
            data.clearTraits(kerbin.name);
            check(!star.hasTrait(CBT_Destroyed.class), "Sonne anfangs heil");
            boolean hadAtmosphere = CelestialBody.hasTrait(overworld, CBT_Atmosphere.class);

            // 1. Aufruf: zerstoeren
            List<Component> chat = new ArrayList<>();
            check(SpaceWandD.toggleStar(overworld, chat::add), "erster Aufruf zerstoert");
            check(star.hasTrait(CBT_Destroyed.class), "CBT_Destroyed an Kerbol");
            check(!CelestialBody.hasTrait(overworld, CBT_Atmosphere.class), "degas: keine Atmosphaere mehr");
            check(data.getTraits(kerbin.name) != null && !data.getTraits(kerbin.name).containsKey(CBT_Atmosphere.class), "degas als Ueberschreibung gespeichert");
            check(chat.size() == 4 && chat.get(0).getString().equals("GOD") && chat.get(3).getString().equals("SUN"), "Chat GOD DAMN THE SUN");

            // Sync zum Client: Kerbol traegt CBT_Destroyed
            SolarSystemWorldSavedData.updateClientTraits(new HashMap<>());
            FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
            CelestialTraitsPacket.writeTraits(buf, data);
            CelestialTraitsPacket.readTraits(buf);
            check(buf.readableBytes() == 0, "Puffer vollstaendig gelesen");
            var clientStar = SolarSystemWorldSavedData.getClientTraits(star.name);
            check(clientStar != null && clientStar.containsKey(CBT_Destroyed.class), "Client sieht CBT_Destroyed");

            // 2. Aufruf (ueber /space destroystar): kidding
            var server = overworld.getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "space destroystar");
            check(!star.hasTrait(CBT_Destroyed.class), "zweiter Aufruf entfernt CBT_Destroyed");
            check(data.getTraits(star.name) == null && data.getTraits(kerbin.name) == null, "kidding: keine Ueberschreibungen");
            check(CelestialBody.hasTrait(overworld, CBT_Atmosphere.class) == hadAtmosphere, "Atmosphaere wieder Standard");

            List<Component> chat2 = new ArrayList<>();
            check(SpaceWandD.toggleStar(overworld, chat2::add) && star.hasTrait(CBT_Destroyed.class), "dritter Aufruf zerstoert wieder");
            check(!SpaceWandD.toggleStar(overworld, chat2::add) && chat2.get(chat2.size() - 1).getString().equals("kidding"), "vierter Aufruf: kidding");
        } finally {
            SolarSystemWorldSavedData.updateClientTraits(clientBefore);
            restore(data, star.name, starBefore);
            restore(data, kerbin.name, kerbinBefore);
        }
        helper.succeed();
    }

    private static void restore(SolarSystemWorldSavedData data, String name, HashMap<Class<? extends CelestialBodyTrait>, CelestialBodyTrait> traits) {
        if (traits == null) data.clearTraits(name);
        else data.setTraits(name, traits.values().toArray(new CelestialBodyTrait[0]));
    }
}
