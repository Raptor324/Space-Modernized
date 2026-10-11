package com.space_m.dim.duna;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
//? if < 1.21.1 {
import com.mojang.serialization.Codec;
//?} else {
/*import com.mojang.serialization.MapCodec;
*///?}
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.SpaceDimensions;
import com.space_m.dim.SpaceWorldgen;
import com.space_m.travel.CelestialTeleporter;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Weltart "The Martian" (Original {@code WorldTypeTeleport.martian}, {@code generator.martian}): normale Oberwelt,
 * aber jeder Spieler wird beim ersten Betreten auf die Duna (0, 0) gebracht ({@code onPlayerJoin} ->
 * {@code CelestialTeleporter.teleport(player, dunaDimension, 0, 0, 0, true)}, {@code HbmPlayerProps.hasWarped}),
 * stirbt man auf der Duna, erscheint man dort wieder ({@code WorldProviderDuna.getRespawnDimension}), und auf der
 * Duna steht bei (0, 0) die Marsbasis ({@code WorldGeneratorDuna}: {@code StructureManager.martian.build}).
 *
 * <p>1.20-Umsetzung: Welt-Voreinstellung {@code space_m:martian} ({@code worldgen/world_preset/martian.json}, in der
 * Liste ueber {@code #minecraft:normal}); erkennbar an der Biomquelle der Oberwelt ({@link MartianBiomeSource},
 * reicht nur an die Vanilla-Multi-Noise-Quelle durch). {@code hasWarped} = Spieler-Tag {@code space_m.has_warped}.
 * Die Basis wird beim ersten Warp gebaut (die 40x42-Struktur ragt ueber den Dekorationsbereich eines Chunks
 * hinaus; das Original setzt die Bloecke direkt in die Welt).
 */
public final class MartianWorld {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final String HAS_WARPED = "space_m.has_warped";
    private static final String BASE_BUILT = "martianBaseBuilt";

    private static final Set<UUID> DIED_ON_DUNA = ConcurrentHashMap.newKeySet();

    private MartianWorld() {}

    /** Biomquelle der Martian-Oberwelt: reicht an {@code source} durch (nur Kennzeichen der Weltart). */
    public static class MartianBiomeSource extends BiomeSource {
        //? if < 1.21.1 {
        public static final Codec<MartianBiomeSource> CODEC = RecordCodecBuilder.create(i -> i.group(
        //?} else {
        /*public static final MapCodec<MartianBiomeSource> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        *///?}
                BiomeSource.CODEC.fieldOf("source").forGetter(s -> s.source)
        ).apply(i, i.stable(MartianBiomeSource::new)));

        private final BiomeSource source;

        public MartianBiomeSource(BiomeSource source) {
            this.source = source;
        }

        //? if < 1.21.1 {
        @Override
        protected Codec<? extends BiomeSource> codec() {
            return CODEC;
        }
        //?} else {
        /*@Override
        protected MapCodec<? extends BiomeSource> codec() {
            return CODEC;
        }
        *///?}

        @Override
        protected Stream<Holder<Biome>> collectPossibleBiomes() {
            return source.possibleBiomes().stream();
        }

        @Override
        public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
            return source.getNoiseBiome(x, y, z, sampler);
        }
    }

    public static void init() {
        SpaceWorldgen.BIOME_SOURCES.register("martian", () -> MartianBiomeSource.CODEC);
        PlayerEvent.PLAYER_JOIN.register(MartianWorld::onPlayerJoin);
        PlayerEvent.PLAYER_RESPAWN.register(MartianWorld::onPlayerRespawn);
        EntityEvent.LIVING_DEATH.register(MartianWorld::onDeath);
    }

    /** {@code getTerrainType() == WorldTypeTeleport.martian}. */
    public static boolean isMartian(MinecraftServer server) {
        ServerLevel overworld = server == null ? null : server.getLevel(Level.OVERWORLD);
        return overworld != null && overworld.getChunkSource().getGenerator().getBiomeSource() instanceof MartianBiomeSource;
    }

    private static void onPlayerJoin(ServerPlayer player) {
        DunaDustStorm.syncTo(player);
        if (!isMartian(player.server) || player.getTags().contains(HAS_WARPED)) return;
        ServerLevel duna = player.server.getLevel(SpaceDimensions.DUNA);
        if (duna == null) return;
        buildBase(duna);
        CelestialTeleporter.teleport(player, SpaceDimensions.DUNA, 0, 0, 0, true);
        player.addTag(HAS_WARPED);
    }

    private static EventResult onDeath(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
        if (entity instanceof ServerPlayer player) {
            if (SpaceDimensions.DUNA.equals(player.level().dimension()) && isMartian(player.server)) DIED_ON_DUNA.add(player.getUUID());
            else DIED_ON_DUNA.remove(player.getUUID());
        }
        return EventResult.pass();
    }

    //? if < 1.21.1 {
    private static void onPlayerRespawn(ServerPlayer player, boolean conqueredEnd) {
    //?} else {
    /*private static void onPlayerRespawn(ServerPlayer player, boolean conqueredEnd, net.minecraft.world.entity.Entity.RemovalReason reason) {
    *///?}
        // Original getRespawnDimension: wer auf der Duna stirbt, bleibt (ohne eigenen Spawnpunkt) auf der Duna
        if (conqueredEnd || !DIED_ON_DUNA.remove(player.getUUID())) return;
        if (player.getRespawnPosition() != null) return;
        ServerLevel duna = player.server.getLevel(SpaceDimensions.DUNA);
        if (duna == null) return;
        BlockPos spawn = player.server.overworld().getSharedSpawnPos();
        CelestialTeleporter.teleport(player, SpaceDimensions.DUNA, spawn.getX() + 0.5, 0, spawn.getZ() + 0.5, true);
    }

    /** Original {@code WorldGeneratorDuna}: Chunk (0, 0) der Martian-Welt, {@code y = getHeightValue(0, 0) - 1}. */
    public static void buildBase(ServerLevel duna) {
        var data = com.space_m.celestial.CelestialBodyWorldSavedData.get(duna);
        if (data.providerData().getBoolean(BASE_BUILT)) return;
        try {
            int y = duna.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0) - 1;
            boolean old = com.hbm_m.block.decorations.SpotlightBlock.disableOnGeneration;
            com.hbm_m.block.decorations.SpotlightBlock.disableOnGeneration = false;
            try {
                WorldGeneratorDuna.load("martian-base.nbt").build(duna, 0, y, 0);
            } finally {
                com.hbm_m.block.decorations.SpotlightBlock.disableOnGeneration = old;
            }
            data.providerData().putBoolean(BASE_BUILT, true);
            data.setDirty();
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Martian base could not be built", e);
        }
    }
}
