package com.space_m.war;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.entity.logic.TomBlastEntity;
import com.hbm_m.util.WorldUtil;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Impact;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.celestial.trait.CBT_War;
import com.space_m.celestial.trait.CelestialBodyTrait.CBT_BATTLEFIELD;
import com.space_m.network.SpacePacket;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Server-Teil von Krieg/Invasion/Einschlag (R8-B), Einstieg aus {@code SpaceRegistry.init()}:
 * <ul>
 *   <li>{@link CBT_Invasion#spawner} = {@link InvasionSpawner}; Abschuesse ({@code ModEventHandler.onEntityDeath}:
 *       {@code alien.onKill(entity, body)}).</li>
 *   <li>Bossleiste der Invasion (Original {@code ModEventHandlerClient}: {@code BossStatus.setBossStatus(invasion)}
 *       solange {@code isInvading && wave < 4}; Name "Wave n", Fuellung {@code (killreq - kills) / killreq}) als
 *       {@link ServerBossEvent} je Dimension.</li>
 *   <li>{@code CBT_Impact} beim Tom-Einschlag (Original {@code EntityTomBlast.onUpdate}, erster Tick).</li>
 *   <li>Thatmo-Schlachtfeld (Original {@code WorldProviderThatmo.updateWeather}, Server): je Spieler alle 8 s (Uhrzeit,
 *       {@code currentTimeMillis() % 8000 < 50}) ein Carpet-Jet 40 Bloecke um den Spieler, 20 Bloecke ueber ihm.</li>
 *   <li>Kriegsprojektile (Original {@code PermaSyncHandler}, CBT_War: flashtime/traveltime je Tick) an die Spieler
 *       des Koerpers.</li>
 *   <li>{@link CelestialNukeShockHandler}; War-Behemoth-Spawn auf Thatmo (Original {@code BiomeGenThatmo.creatures}
 *       Gewicht 2, 1-1).</li>
 * </ul>
 */
public final class WarEvents {

    private WarEvents() {}

    private static boolean initialized;

    private static final Map<ResourceKey<Level>, ServerBossEvent> bossBars = new HashMap<>();

    public static void init() {
        if (initialized) return;
        initialized = true;

        CBT_Invasion.spawner = new InvasionSpawner();
        CelestialNukeShockHandler.init();

        EntityEvent.LIVING_DEATH.register(WarEvents::onEntityDeath);
        EntityEvent.ADD.register(WarEvents::onEntityJoin);
        TickEvent.SERVER_LEVEL_POST.register(WarEvents::onLevelTick);
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            for (ServerBossEvent bar : bossBars.values()) bar.removeAllPlayers();
            bossBars.clear();
        });

        // BiomeGenThatmo: this.creatures.add(new SpawnListEntry(EntityWarBehemoth.class, 2, 1, 1))
        BiomeModifications.addProperties(
                ctx -> ctx.getKey().map(id -> id.equals(SpaceMod.id("thatmo"))).orElse(false),
                (ctx, props) -> props.getSpawnProperties().addSpawn(MobCategory.CREATURE,
                        new MobSpawnSettings.SpawnerData(WarEntities.WAR_BEHEMOTH.get(), 2, 1, 1)));
        // Platzierung: 1.7.10 performWorldGenSpawning prueft kein getCanSpawnHere -> Chunk-Erzeugung ohne Lichtpruefung (R9-B4)
        //? if < 1.21.1 {
        SpawnPlacementsRegistry.register(WarEntities.WAR_BEHEMOTH, net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, com.space_m.entity.mob.MobSpawns::checkThatmoMonster);
        //?} else {
        /*SpawnPlacementsRegistry.register(WarEntities.WAR_BEHEMOTH, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, com.space_m.entity.mob.MobSpawns::checkThatmoMonster);
        *///?}
    }

    // ── Abschuesse ──

    private static EventResult onEntityDeath(LivingEntity entity, DamageSource source) {
        Level level = entity.level();
        if (level.isClientSide) return EventResult.pass();

        CelestialBody body = CelestialBody.getBody(level);

        CBT_Invasion alien = body.getTrait(CBT_Invasion.class);
        if (alien != null) {
            alien.onKill(entity, body);
        }
        return EventResult.pass();
    }

    // ── Tom-Einschlag ──

    private static EventResult onEntityJoin(Entity entity, Level level) {
        if (level == null || level.isClientSide) return EventResult.pass();

        if (entity instanceof TomBlastEntity tom && !tom.did) {
            CelestialBody body = CelestialBody.getBody(level);
            body.modifyTraits(new CBT_Impact(level.getGameTime()));
        }
        return EventResult.pass();
    }

    // ── je Welt-Tick ──

    private static void onLevelTick(ServerLevel level) {
        // Original: CelestialBody.getBody(world) (unbekannte Dimension -> Kerbin)
        CelestialBody body = CelestialBody.getBody(level);

        updateBossBar(level, body);

        if (CelestialBody.getBodyOrNull(level.dimension()) != null && body.getTrait(CBT_BATTLEFIELD.class) != null) {
            updateBattlefield(level);
        }

        syncWarProjectiles(level, body);
    }

    private static void updateBossBar(ServerLevel level, CelestialBody body) {
        CBT_Invasion invasion = body.getTrait(CBT_Invasion.class);
        boolean active = invasion != null && invasion.isInvading && invasion.wave < 4;

        ServerBossEvent bar = bossBars.get(level.dimension());
        if (!active) {
            if (bar != null) {
                bar.removeAllPlayers();
                bossBars.remove(level.dimension());
            }
            return;
        }

        if (bar == null) {
            bar = new ServerBossEvent(invasion.getBossName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS);
            bossBars.put(level.dimension(), bar);
        }

        bar.setName(invasion.getBossName());
        float max = invasion.getMaxHealth();
        bar.setProgress(max > 0 ? Math.max(0F, Math.min(1F, invasion.getHealth() / max)) : 0F);

        List<ServerPlayer> inLevel = level.players();
        for (ServerPlayer p : new ArrayList<>(bar.getPlayers())) {
            if (!inLevel.contains(p)) bar.removePlayer(p);
        }
        for (ServerPlayer p : inLevel) {
            bar.addPlayer(p);
        }
    }

    /** Aktive Bossleiste einer Dimension (Tests). */
    public static ServerBossEvent bossBar(ResourceKey<Level> dimension) {
        return bossBars.get(dimension);
    }

    private static void updateBattlefield(ServerLevel level) {
        // i am violating the genercizing philosphy mellow told me about, this shit sucks
        // if other planets have the battlefield trait, what could be done....?
        for (ServerPlayer p : new ArrayList<>(level.players())) {
            long currentTime = System.currentTimeMillis();
            if (currentTime % 8000 < 50) {
                BlockPos playerPos = BlockPos.containing(p.getX(), p.getY(), p.getZ());
                int radius = 40;

                int offsetX = level.random.nextInt(radius * 2 + 1) - radius;
                int offsetZ = level.random.nextInt(radius * 2 + 1) - radius;

                BlockPos targetPos = playerPos.offset(offsetX, 0, offsetZ);
                spawnCarpetJet(level, targetPos);
            }
        }
    }

    /** Original: {@code statFacCarpetJet(world, x, y, z)}, {@code posY = y + 20}, {@code loadAndSpawnEntityInWorld}. */
    public static EntityCarpetJet spawnCarpetJet(ServerLevel level, BlockPos targetPos) {
        EntityCarpetJet bomber = EntityCarpetJet.statFacCarpetJet(level, targetPos.getX(), targetPos.getY(), targetPos.getZ());
        bomber.setPos(bomber.getX(), targetPos.getY() + 20, bomber.getZ());
        WorldUtil.loadAndSpawnEntityInWorld(bomber);
        return bomber;
    }

    private static void syncWarProjectiles(ServerLevel level, CelestialBody body) {
        if (level.players().isEmpty()) return;
        CBT_War war = body.getTrait(CBT_War.class);
        if (war == null || war.getProjectiles().isEmpty()) return;

        List<CBT_War.Projectile> projectiles = war.getProjectiles();
        float[] flash = new float[projectiles.size()];
        float[] travel = new float[projectiles.size()];
        for (int i = 0; i < projectiles.size(); i++) {
            flash[i] = projectiles.get(i).getFlashtime();
            travel[i] = projectiles.get(i).getTravel();
        }
        WarProjectilePacket pkt = new WarProjectilePacket(body.name, flash, travel);
        for (ServerPlayer p : level.players()) {
            SpacePacketHandler.sendToPlayer(p, SpacePacketHandler.WAR_PROJECTILES, pkt);
        }
    }

    /** Original {@code PermaSyncHandler} CBT_War-Abschnitt: je Projektil flashtime + traveltime. */
    public static final class WarProjectilePacket implements SpacePacket {
        public final String body;
        public final float[] flash;
        public final float[] travel;

        public WarProjectilePacket(String body, float[] flash, float[] travel) {
            this.body = body;
            this.flash = flash;
            this.travel = travel;
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(body);
            buf.writeVarInt(flash.length);
            for (int i = 0; i < flash.length; i++) {
                buf.writeFloat(flash[i]);
                buf.writeFloat(travel[i]);
            }
        }

        public static WarProjectilePacket decode(FriendlyByteBuf buf) {
            String body = buf.readUtf();
            int n = buf.readVarInt();
            float[] flash = new float[n];
            float[] travel = new float[n];
            for (int i = 0; i < n; i++) {
                flash[i] = buf.readFloat();
                travel[i] = buf.readFloat();
            }
            return new WarProjectilePacket(body, flash, travel);
        }
    }

}
