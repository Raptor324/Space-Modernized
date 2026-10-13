package com.space_m.rocket.entity;

import java.util.List;

import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.dim.SpaceDimensions;
import com.space_m.rocket.ItemCustomRocket;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.SpaceRocketItems;
import com.space_m.rocket.entity.EntityRideableRocket.RocketState;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R5-B: fliegende Rakete - Start (Aufstieg mit Neigung), Stufentrennung beim Landeanflug, Transfer und Ankunft samt
 * Landung in der Mun-Dimension.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RocketFlightGameTest {

    private RocketFlightGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Kapsel + {@code stages} gleiche Stufen (Rumpf rp_f_20_12, Methalox-Triebwerk, Landebeine). */
    private static RocketStruct rocket(int stages) {
        RocketStruct rocket = new RocketStruct(new ItemStack(SpaceRocketItems.RP_CAPSULE_20.get()));
        for (int i = 0; i < stages; i++) {
            rocket.addStage(new ItemStack(SpaceRocketItems.RP_FUSELAGE_20_12.get()), new ItemStack(SpaceRocketItems.RP_LEGS_20.get()),
                    new ItemStack(SpaceRocketItems.MP_THRUSTER_20_METHALOX.get()));
        }
        return rocket;
    }

    private static ItemStack drive(SolarSystem.Body body, int x, int z) {
        ItemStack drive = ItemVOTVdrive.of(body);
        ItemVOTVdrive.setCoordinates(drive, x, z);
        ItemVOTVdrive.setProcessed(drive, true);
        return drive;
    }

    private static EntityRideableRocket spawn(GameTestHelper helper, RocketStruct struct, ItemStack drive) {
        Vec3 pos = helper.absoluteVec(new Vec3(1.5, 1, 1.5));
        EntityRideableRocket rocket = EntityRideableRocket.create(helper.getLevel(), pos.x, pos.y, pos.z, ItemCustomRocket.build(struct))
                .withProgram(drive);
        helper.getLevel().addFreshEntity(rocket);
        return rocket;
    }

    /** Start: AWAITING -> LAUNCHING (attemptLaunch), Aufstieg mit 4x-Bewegung, Neigung ab Tick 80, kein Stillstand. */
    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 400)
    public static void launch(GameTestHelper helper) {
        EntityRideableRocket rocket = spawn(helper, rocket(2), drive(SolarSystem.Body.MUN, 8, 8));
        check(rocket.getState() == RocketState.AWAITING, "spawned AWAITING");
        check(rocket.canRide(), "capsule rp_c_20 accepts passengers");
        check(rocket.getBbHeight() > 8, "two-stage rocket taller than 8 (capsule dummy), got " + rocket.getBbHeight());

        CelestialBody.Target from = CelestialBody.getTarget(helper.getLevel(), (int) rocket.getX(), (int) rocket.getZ());
        ItemVOTVdrive.Target to = rocket.getTarget();
        check(to.body() == SolarSystem.Body.MUN.getBody(), "drive target mun, got " + to.body());

        boolean fuel = rocket.getRocket().hasSufficientFuel(from.body, to.body(), from.inOrbit, to.inOrbit());
        rocket.attemptLaunch();
        check((rocket.getState() == RocketState.LAUNCHING) == fuel, "attemptLaunch follows hasSufficientFuel (" + fuel + ")");
        if (!fuel) rocket.setState(RocketState.LAUNCHING);

        double startY = rocket.getY();
        // Pruefzeitpunkt 120 statt 200: 1:1 Original (motionMult 4, Beschleunigung (t/120)^2*0.05 bis v 4) steigt die
        // Rakete bis Tick ~175 ueber Y 900 und wechselt dann regulaer in TRANSFER (Ziel Mun). Frueher "bestand" der Test
        // bei 200 nur, weil die schraeg fliegende Rakete aus den tickenden Chunks flog und stehen blieb. Der Transfer
        // selbst ist in arrivalInMun abgedeckt. Bei Tick 120: ca. 250 Bloecke Aufstieg, Neigung 12 Grad.
        helper.runAtTickTime(120, () -> {
            check(rocket.getState() == RocketState.LAUNCHING, "still launching, state " + rocket.getState());
            check(rocket.getY() > startY + 20, "rocket climbs, y " + rocket.getY() + " start " + startY);
            check(rocket.getXRot() > 0, "pitch over after tick 80, pitch " + rocket.getXRot());
            check(rocket.capDummy != null && !rocket.capDummy.isRemoved(), "capsule dummy follows tall rocket");
            rocket.discard();
            helper.succeed();
        });
    }

    /**
     * Spieltest "Ich kann nicht in die Rakete einsteigen": echter Spielerpfad wie im Spiel - Client-Zielerfassung
     * ({@code ProjectileUtil.getEntityHitResult} wie {@code GameRenderer.pick}, Reichweite 3 im Ueberlebensmodus),
     * Server-Reichweitenpruefung ({@code handleInteract}: Abstand Trefferbox-Auge unter 6) und {@code Player.interactOn}.
     * Einmal unten am Rumpf, einmal oben an der Kapsel (Turmplattform, Treffer ueber die Kapsel-Trefferbox).
     */
    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 40)
    public static void boardRocket(GameTestHelper helper) {
        EntityRideableRocket rocket = spawn(helper, rocket(2), drive(SolarSystem.Body.MUN, 8, 8));
        ServerLevel level = helper.getLevel();
        net.minecraft.server.level.ServerPlayer player = fakePlayer(level, "rocket_board_test");

        helper.runAfterDelay(3, () -> {
            check(rocket.getState() == RocketState.AWAITING, "awaiting, state " + rocket.getState());
            check(rocket.capDummy != null && !rocket.capDummy.isRemoved(), "capsule dummy present");

            // 1) unten am Rumpf, 2 Bloecke neben der Achse, Blick waagerecht zur Rakete
            String low = tryBoard(player, rocket, rocket.getY(), 2.0D);
            check(low == null, "board from ground: " + low);
            check(player.getVehicle() == rocket, "player rides rocket (ground)");
            player.stopRiding(); // danach 60 Ticks boardingCooldown (Vanilla) - zweiter Spieler fuer die Plattform

            // 2) oben auf der Turmplattform an der Kapsel
            net.minecraft.server.level.ServerPlayer player2 = fakePlayer(level, "rocket_board_test2");
            // Plattformhoehe wie TileEntityLaunchPadRocket: Oberkante pad + newHeight + 3, Rakete steht auf pad + 3
            com.space_m.rocket.RocketStruct struct = rocket.getRocket();
            double capsuleFeet = rocket.getY() + Math.floor(struct.getHeight() - struct.capsule.height + 1);
            String high = tryBoard(player2, rocket, capsuleFeet, 2.6D); // knapp ausserhalb der 4 breiten Kapsel-Trefferbox
            check(high == null, "board from tower platform: " + high);
            check(player2.getVehicle() == rocket, "player rides rocket (platform)");
            check(rocket.getPassengers().size() == 1, "one passenger");

            player2.stopRiding();
            rocket.discard();
            helper.succeed();
        });
    }

    private static net.minecraft.server.level.ServerPlayer fakePlayer(ServerLevel level, String name) {
        //? if forge {
        net.minecraft.server.level.ServerPlayer player = net.minecraftforge.common.util.FakePlayerFactory.get(level,
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name));
        //?} elif neoforge {
        /*// NeoForge-FakePlayer verweigert startRiding grundsaetzlich -> echter ServerPlayer, Netzkanal vom FakePlayer
        com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), name);
        net.minecraft.server.level.ServerPlayer player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, profile,
                net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, profile).connection;
        *///?}
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        return player;
    }

    /** @return null bei Erfolg, sonst Grund */
    private static String tryBoard(net.minecraft.server.level.ServerPlayer player, EntityRideableRocket rocket, double feetY, double side) {
        player.moveTo(rocket.getX() + side, feetY, rocket.getZ(), 90.0F, 0.0F); // Yaw 90 = Blick nach -X
        player.setYHeadRot(90.0F); // Blickrichtung lebender Wesen = Kopfdrehung
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        double reach = 3.0D; // Forge ENTITY_REACH Ueberleben
        Vec3 end = eye.add(look.scale(reach));
        AABB search = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0D);
        net.minecraft.world.phys.EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(player, eye, end, search,
                e -> !e.isSpectator() && e.isPickable(), reach * reach);
        if (hit == null) return "client pick finds no entity at y " + feetY + " (rocket " + rocket.getBoundingBox() + ", eye " + eye + " end " + end
                + " search " + search + " found " + player.level().getEntities(player, search, e -> true) + " pickable " + rocket.isPickable()
                + " clip " + rocket.getBoundingBox().clip(eye, end) + " sameLevel " + (player.level() == rocket.level()) + ")";
        net.minecraft.world.entity.Entity target = hit.getEntity();
        if (target != rocket && target != rocket.capDummy) return "picked " + target;
        if (target.getBoundingBox().distanceToSqr(eye) >= 36.0D) return "server reach check fails";
        net.minecraft.world.InteractionResult r = target.interactAt(player, hit.getLocation().subtract(target.position()), net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!r.consumesAction()) r = player.interactOn(target, net.minecraft.world.InteractionHand.MAIN_HAND);
        if (!r.consumesAction()) return "interact result " + r;
        return player.getVehicle() == rocket ? null : "not mounted after " + r + ", vehicle " + player.getVehicle();
    }

    /** Startrampe: Tanks aus dem Treibstoffbedarf der Rakete (updateStorageTanks), betankte Rakete fuellt sie, NONE hinten. */
    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 20)
    public static void padTanks(GameTestHelper helper) {
        RocketStruct struct = rocket(2);
        com.hbm_m.inventory.fluid.tank.FluidTank[] tanks = new com.hbm_m.inventory.fluid.tank.FluidTank[RocketStruct.MAX_STAGES * 2];
        for (int i = 0; i < tanks.length; i++) tanks[i] = new com.hbm_m.inventory.fluid.tank.FluidTank(com.hbm_m.inventory.fluid.ModFluids.NONE.getSource(), 64_000);
        com.space_m.rocket.pad.TileEntityLaunchPadRocket.SolidFuelTank solid = new com.space_m.rocket.pad.TileEntityLaunchPadRocket.SolidFuelTank();

        com.space_m.rocket.pad.TileEntityLaunchPadRocket.updateStorageTanks(struct, tanks, solid, true);

        java.util.Map<net.minecraft.world.level.material.Fluid, Integer> req = struct.getFillRequirement();
        req.remove(com.space_m.rocket.pad.TileEntityLaunchPadRocket.SOLID);
        check(!req.isEmpty(), "methalox rocket needs fluids");
        for (var e : req.entrySet()) {
            boolean found = false;
            for (var t : tanks) {
                if (t.getTankType() == e.getKey() && t.getMaxFill() == e.getValue() && t.getFill() == t.getMaxFill()) found = true;
            }
            check(found, "filled tank for " + e.getKey() + " x" + e.getValue());
        }
        check(tanks[tanks.length - 1].getTankType() == com.hbm_m.inventory.fluid.ModFluids.NONE.getSource(), "unused tanks NONE at the end");
        helper.succeed();
    }

    /** Stufentrennung: Landeanflug wirft die unterste Stufe ab (2 -> 1), Zustand LANDING, Ziel ueber den Laufwerkskoordinaten. */
    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 40)
    public static void stageSeparation(GameTestHelper helper) {
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        EntityRideableRocket rocket = spawn(helper, rocket(2), drive(SolarSystem.Body.KERBIN, at.getX(), at.getZ()));
        double heightBefore = rocket.getRocket().getHeight();

        CelestialBody.Target from = CelestialBody.getTarget(helper.getLevel(), at.getX(), at.getZ());
        rocket.setState(RocketState.LAUNCHING);
        rocket.beginLandingSequence(from, rocket.getTarget());

        check(rocket.getRocket().stages.size() == 1, "one stage expended, stages " + rocket.getRocket().stages.size());
        check(rocket.getRocket().getHeight() < heightBefore, "rocket got shorter");
        check(rocket.getState() == RocketState.LANDING, "landing after separation, state " + rocket.getState());
        check(Math.abs(rocket.getY() - 800) < 1.0E-6 && Math.abs(rocket.getX() - (at.getX() + 0.5)) < 1.0E-6, "placed above destination at y 800");
        rocket.discard();
        helper.succeed();
    }

    private static void arrivalWithoutMunLevel(GameTestHelper helper) {
        EntityRideableRocket rocket = spawn(helper, rocket(1), drive(SolarSystem.Body.MUN, 40, 40));
        CelestialBody.Target from = CelestialBody.getTarget(helper.getLevel(), (int) rocket.getX(), (int) rocket.getZ());
        rocket.setState(RocketState.LAUNCHING);
        rocket.beginCelestialTransfer(from, rocket.getTarget());
        check(rocket.getState() == RocketState.TRANSFER, "transfer started, state " + rocket.getState());
        int queued = com.space_m.travel.CelestialTeleporter.queued();
        rocket.skipTransfer();
        helper.runAfterDelay(2, () -> {
            check(rocket.getState() == RocketState.LANDING, "landing sequence after transfer, state " + rocket.getState());
            check(rocket.getRocket().stages.isEmpty(), "transfer stage expended");
            check(rocket.teleportPending, "teleport to space_m:mun requested (queued before " + queued + ")");
            rocket.discard();
            helper.succeed();
        });
    }

    /** Transfer Kerbin -> Mun ohne Station (Transfer in der Rakete), Ankunft in space_m:mun, Landung auf dem Boden. */
    @GameTest(template = "empty3x3x3", batch = "space_rocket", timeoutTicks = 8000)
    public static void arrivalInMun(GameTestHelper helper) {
        ServerLevel mun = helper.getLevel().getServer().getLevel(SpaceDimensions.MUN);
        if (mun == null) {
            // NeoForge-1.21.1-GameTestServer laedt keine Datapack-Dimensionen (s. DimensionGameTest): bis zum
            // eingereihten Wechsel nach space_m:mun pruefen
            arrivalWithoutMunLevel(helper);
            return;
        }

        int dx = 40;
        int dz = 40;
        // Mun-Gelaende am Ziel vorab erzeugen (sonst wartet die gelandete Rakete im Testlauf auf die Weltgenerierung)
        for (int cx = (dx >> 4) - 3; cx <= (dx >> 4) + 3; cx++) {
            for (int cz = (dz >> 4) - 3; cz <= (dz >> 4) + 3; cz++) mun.getChunk(cx, cz);
        }
        EntityRideableRocket rocket = spawn(helper, rocket(1), drive(SolarSystem.Body.MUN, dx, dz));

        CelestialBody.Target from = CelestialBody.getTarget(helper.getLevel(), (int) rocket.getX(), (int) rocket.getZ());
        rocket.setState(RocketState.LAUNCHING);
        rocket.beginCelestialTransfer(from, rocket.getTarget());
        check(rocket.getState() == RocketState.TRANSFER, "transfer started, state " + rocket.getState());
        check(rocket.getTransferTime() > 0, "transfer time > 0");

        ServerLevel orbit = helper.getLevel().getServer().getLevel(SpaceDimensions.ORBIT);
        if (orbit == null) {
            helper.runAtTickTime(10, () -> {
                check(rocket.getTransferProgress() > 0, "transfer progresses");
                check(rocket.getTransferTarget() == SolarSystem.Body.MUN.getBody(), "travelling to mun");
                rocket.skipTransfer();
            });
        } else {
            // R6-A: mit Orbit-Dimension laeuft der Transfer wie im Original ueber eine Transfer-Station im Orbit-Raster.
            // Der Wechsel in den Orbit ist eingereiht (CelestialTeleporter, ein Eintrag je Level-Tick) - daher jeden Tick
            // nachsehen statt zu festem Zeitpunkt; sobald die Rakete im Orbit wartet, Stationstransfer abkuerzen.
            helper.onEachTick(() -> {
                for (net.minecraft.world.entity.Entity e : orbit.getAllEntities()) {
                    if (!(e instanceof EntityRideableRocket r) || r.getState() != RocketState.TRANSFER) continue;
                    com.space_m.orbit.OrbitalStation station = com.space_m.orbit.OrbitalStation.getStationFromPosition((int) r.getX(), (int) r.getZ());
                    if (station.state == com.space_m.orbit.OrbitalStation.StationState.TRANSFER && station.target == SolarSystem.Body.MUN.getBody()) {
                        check(r.getTransferTarget() == SolarSystem.Body.MUN.getBody(), "travelling to mun");
                        station.stateTimer = Math.max(station.stateTimer, station.maxStateTimer); // Transfer abkuerzen
                    }
                }
            });
        }

        helper.succeedWhen(() -> {
            List<EntityRideableRocket> arrived = mun.getEntitiesOfClass(EntityRideableRocket.class,
                    new AABB(dx - 2, mun.getMinBuildHeight(), dz - 2, dx + 3, 1000, dz + 3));
            check(!arrived.isEmpty(), "rocket arrived in mun");
            EntityRideableRocket r = arrived.get(0);
            check(r.getRocket().stages.isEmpty(), "transfer stage expended");
            check(r.getState() == RocketState.LANDED, "landed on mun, state " + r.getState() + " y " + r.getY() + " ground "
                    + mun.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, dx, dz) + " motion " + r.getDeltaMovement() + " tick " + r.tickCount
                    + " entityTicking " + mun.isPositionEntityTicking(r.blockPosition()) + " chunk " + mun.getChunkSource().getChunkDebugData(r.chunkPosition()));
            r.discard();
        });
    }
}
