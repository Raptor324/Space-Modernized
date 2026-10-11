package com.space_m.test;

import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Weather;
import com.space_m.command.CommandStations;
import com.space_m.dim.SpaceDimensions;
import com.space_m.time.CelestialTime;
import com.space_m.travel.CelestialTeleporter;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R1-D: Reise ({@code CelestialTeleporter}), Befehle, Zeit und Wetter je Himmelskoerper.
 * Zieldimension ist {@code space_m:mun}, solange das Dimensions-Framework sie liefert; sonst dient der Nether als
 * Ersatz ("Overworld-Mock": beliebige Nicht-Overworld-Dimension mit geteilter Vanilla-Zeit).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceTravelGameTest {

    private SpaceTravelGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static ResourceKey<Level> targetDim(MinecraftServer server) {
        return server.getLevel(SpaceDimensions.MUN) != null ? SpaceDimensions.MUN : Level.NETHER;
    }

    /** Sichere Landeposition 1:1 placeInPortal: erster Nicht-Luft-Block von oben + 5; ungrounded = Y unveraendert. */
    @GameTest(template = "empty3x3x3", batch = "space_travel", timeoutTicks = 20)
    public static void groundedLandingHeight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos top = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.setBlock(new BlockPos(1, 2, 1), Blocks.STONE);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, top.getX(), top.getZ()); // erste Luft ueber dem hoechsten Block
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));

        double y = new CelestialTeleporter(pig, Level.OVERWORLD, top.getX() + 0.5D, 300.0D, top.getZ() + 0.5D, true).placeInLevel(level);
        check(y == surface - 1 + 5, "grounded y = top block + 5, expected " + (surface + 4) + " got " + y);

        double y2 = new CelestialTeleporter(pig, Level.OVERWORLD, top.getX() + 0.5D, 130.0D, top.getZ() + 0.5D, false).placeInLevel(level);
        check(y2 == 130.0D, "ungrounded keeps y, got " + y2);
        pig.discard();
        helper.succeed();
    }

    /** Spieler auf einem Reittier: Spieler + Reittier wechseln die Dimension, Spieler sitzt wieder auf. */
    @GameTest(template = "empty3x3x3", batch = "space_travel", timeoutTicks = 100)
    public static void playerWithMountTeleports(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceKey<Level> dim = targetDim(server);
        ServerLevel target = server.getLevel(dim);
        check(target != null, "target level " + dim.location());

        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 1, 1));
        // 1.20.1: GameTestHelper-Mock braucht einen Netty-Kanal -> Forge-FakePlayer; 1.21.1: Neo-FakePlayer
        // verweigert startRiding -> Vanilla-Mock (EmbeddedChannel)
        //? if forge {
        ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.get(helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "space_m_travel_test"));
        //?} else {
        /*ServerPlayer player = helper.makeMockServerPlayerInLevel();
        *///?}
        player.moveTo(pig.getX(), pig.getY(), pig.getZ());
        check(player.startRiding(pig, true), "player mounts pig");

        // Teleport ueber das Reittier: der Spieler wird erkannt (rider), das Reittier mitgenommen (1:1 runTeleport)
        CelestialTeleporter.teleport(pig, dim, 8.5D, 300.0D, 8.5D, true);
        check(CelestialTeleporter.queued() >= 1, "queued");

        helper.succeedWhen(() -> {
            check(player.level() == target, "player in " + dim.location() + ", is in " + player.level().dimension().location());
            Entity vehicle = player.getVehicle();
            check(vehicle != null && vehicle.getType() == EntityType.PIG && vehicle.level() == target, "player remounted on pig in target");
            double expected = new CelestialTeleporter(player, dim, 8.5D, 300.0D, 8.5D, true).placeInLevel(target);
            check(Math.abs(vehicle.getY() - expected) < 1.0E-6, "landing y " + expected + " got " + vehicle.getY());
            check(pig.isRemoved(), "old pig removed");
            player.stopRiding();
            vehicle.discard();
            //? if forge {
            target.removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            //?} else {
            /*server.getPlayerList().remove(player);
            *///?}
        });
    }

    /** Ortszeit je Dimension: eigene Zeit, laeuft je Tick weiter, /time set wirkt, Overworld bleibt unberuehrt. */
    @GameTest(template = "empty3x3x3", batch = "space_travel", timeoutTicks = 60)
    public static void localTimeIndependent(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        ResourceKey<Level> dim = targetDim(server);
        ServerLevel local = server.getLevel(dim);
        ServerLevel overworld = server.overworld();
        boolean mock = dim == Level.NETHER;
        if (mock) CelestialTime.addTestLocalTimeLevel(dim);

        check(CelestialTime.hasLocalTime(local), "local time active for " + dim.location());
        check(!CelestialTime.hasLocalTime(overworld), "overworld keeps vanilla time");

        long owBefore = overworld.getDayTime();
        local.setDayTime(5_000_000L);
        check(local.getDayTime() == 5_000_000L, "local time set, got " + local.getDayTime());
        check(overworld.getDayTime() == owBefore, "overworld time untouched");

        boolean daylight = local.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DAYLIGHT);
        helper.runAfterDelay(20, () -> {
            long now = local.getDayTime();
            if (daylight) check(now >= 5_000_018L && now <= 5_000_022L, "local time advanced ~20 ticks, got " + now);
            check(Math.abs(overworld.getDayTime() - now) > 1_000_000L, "times independent");
            if (mock) CelestialTime.removeTestLocalTimeLevel(dim);
            helper.succeed();
        });
    }

    /** Tageslaenge 1:1: Sonnenwinkel der Overworld (Kerbin) aus rotationalPeriod/orbitalPeriod. */
    @GameTest(template = "empty3x3x3", batch = "space_travel", timeoutTicks = 20)
    public static void dayLengthFromBody(GameTestHelper helper) {
        CelestialBody kerbin = CelestialBody.getBodyOrNull("kerbin");
        check(kerbin != null, "kerbin registered");
        double dayLength = CelestialTime.getDayLength(kerbin);
        // 21549 s * (1200/21600) * 20 / (1 - 1/426.x) ~ 23997 Ticks
        check(dayLength > 23_900 && dayLength < 24_100, "kerbin day length ~24000, got " + dayLength);
        ServerLevel ow = helper.getLevel();
        check(CelestialTime.bodyFor(ow.dimensionType()) == kerbin, "overworld type -> kerbin");
        long t = 6000;
        float expected = CelestialTime.celestialAngle(dayLength, t, 0.0F);
        check(ow.dimensionType().timeOfDay(t) == expected, "timeOfDay uses body day length");
        CelestialBody mun = CelestialBody.getBodyOrNull("mun");
        if (mun != null) {
            double munDay = CelestialTime.getDayLength(mun);
            // Mun: 138984 s Rotation (gebunden) -> ~154428 Ticks Rotation, Tag laenger
            check(munDay > 100_000, "mun day length long, got " + munDay);
        }
        helper.succeed();
    }

    /** Wetter aus CBT_Weather: Kerbin-Regen erzwingen -> Overworld regnet; /weather clear geht an den Koerper. */
    @GameTest(template = "empty3x3x3", batch = "space_travel", timeoutTicks = 200)
    public static void weatherFromBody(GameTestHelper helper) {
        ServerLevel ow = helper.getLevel();
        CelestialBody kerbin = CelestialBody.getBody(ow);
        CBT_Weather weather = CBT_Weather.ensureTrait(kerbin);
        check(weather != null, "kerbin has weather trait");
        weather.forceRain(ow.getRandom(), 1000);
        helper.succeedWhen(() -> {
            check(ow.isRaining(), "overworld raining from CBT_Weather, level " + ow.getRainLevel(1.0F));
            MinecraftServer server = ow.getServer();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), "weather clear 100");
            check(!weather.raining && weather.rainTime == 100, "/weather clear -> CBT_Weather.forceClear");
        });
    }

    /** /totaltime add|set aendert die Gesamtzeit (Bahnen), nicht die Tageszeit; /ntmstations ohne Stationen. */
    @GameTest(template = "empty3x3x3", batch = "space_travel", timeoutTicks = 20)
    public static void commands(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var source = server.createCommandSourceStack().withSuppressedOutput();
        long before = server.overworld().getGameTime();
        long dayBefore = server.overworld().getDayTime();
        int r = server.getCommands().getDispatcher().getRoot().getChild("totaltime") != null ? 1 : 0;
        check(r == 1, "/totaltime registered");
        check(server.getCommands().getDispatcher().getRoot().getChild("ntmstations") != null, "/ntmstations registered");
        server.getCommands().performPrefixedCommand(source, "totaltime add 1000");
        long after = server.overworld().getGameTime();
        check(after >= before + 1000 && after <= before + 1001, "totaltime add 1000: " + before + " -> " + after);
        check(server.overworld().getDayTime() == dayBefore, "day time untouched");
        server.getCommands().performPrefixedCommand(source, "totaltime set " + before);
        check(server.overworld().getGameTime() == before, "totaltime set");
        check(CommandStations.stationId(0, 0).equals("0x" + Integer.toHexString(1013904223 ^ (1664525 * -559038737 + 1013904223)).toUpperCase()),
                "station id = 1.7.10 ChunkCoordIntPair hash");
        helper.succeed();
    }
}
