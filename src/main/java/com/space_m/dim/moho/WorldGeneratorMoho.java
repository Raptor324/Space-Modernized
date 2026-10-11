package com.space_m.dim.moho;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Random;

import org.slf4j.Logger;

import com.hbm_m.block.bomb.VolcanoBlock;
import com.hbm_m.world.gen.legacy.L;
import com.hbm_m.world.gen.nbt.JigsawPiece;
import com.hbm_m.world.gen.nbt.JigsawPool;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.hbm_m.world.gen.nbt.SpawnCondition;
import com.mojang.logging.LogUtils;
import com.space_m.SpaceMod;
import com.space_m.dim.SpaceWorldgen;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.LegacyWorld;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code com.hbm.dim.moho.WorldGeneratorMoho}.
 *
 * <p><b>Erze</b> ({@code generateMoho}): Datapack-Features in allen Moho-Biomen ({@code placed_feature/moho/*}), Werte
 * 1:1: Mineral {@code mineralSpawn}x10 y12-43, Glowstone 14x12 y5-34, Phosphor {@code netherPhosphorusSpawn}x6 y8-71,
 * Australium 8x4 y0-23, Schiefer 1x12 y8-39, Basalt 10x32 y0-127 im Gestein und die Basalt-Erze (HBM
 * {@code ore_basalt_sulfur|fluorite|asbestos|gem|molysite} = Meta 0-4) im Basalt.
 *
 * <p><b>Oberflaeche</b> = dieses Feature ({@code space_m:moho_surface}, nach den Erzen): zwei Nether-Geysire mit
 * Netherrack-Kragen auf freiem Moho-Gestein/-Regolith (y 11-111) und die Nachbehandlung des 16x16-Feldes (Versatz +8,
 * y 32-127): schwebende Lava bekommt eine Lavaquelle darunter, Vulkankerne werden "statisch, erloeschend"
 * ({@code BlockVolcano.META_STATIC_EXTINGUISHING}).
 *
 * <p><b>Moho-Basis</b> ({@code NBTStructure.registerStructure(mohoDimension, SpawnCondition "moho_base")}): Pools
 * start/default/room/fallback/snorkel 1:1, Teile {@code assets/space_m/structures/mohobase/*.nbt} (umgesetzt mit
 * {@code scratchpad/sR3c_struct.py}, keine fehlenden Bloecke), unter der Original-ID 413020 - erscheint, sobald HBM
 * Strukturen je Dimension erzeugt (Wunsch W-B1). {@code BlockOre.addValidBody} (Tooltip) folgt mit R8.
 */
public class WorldGeneratorMoho extends Feature<NoneFeatureConfiguration> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Original {@code SpaceConfig.mohoDimension}. */
    public static final int MOHO_DIMENSION_ID = 413020;

    public static final RegistrySupplier<WorldGeneratorMoho> MOHO_SURFACE = SpaceWorldgen.FEATURES.register("moho_surface", WorldGeneratorMoho::new);

    public WorldGeneratorMoho() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static void init() {
        LifecycleEvent.SETUP.register(WorldGeneratorMoho::registerStructures);
    }

    private static NBTStructure load(String file) {
        String path = "/assets/" + SpaceMod.MOD_ID + "/structures/mohobase/" + file;
        try (InputStream in = WorldGeneratorMoho.class.getResourceAsStream(path)) {
            if (in == null) throw new IOException("missing " + path);
            return new NBTStructure("mohobase/" + file, in);
        } catch (IOException e) {
            throw new IllegalStateException("[space_m] Moho structure could not be loaded: " + path, e);
        }
    }

    private static void registerStructures() {
        try {
            NBTStructure core = load("moho-core.nbt");
            NBTStructure cornerLab = load("moho-corner-lab.nbt");
            NBTStructure cornerHeffer = load("moho-corner-heffer.nbt");
            NBTStructure cornerExtension = load("moho-corner-extension.nbt");
            NBTStructure cornerEmpty = load("moho-corner-empty.nbt");
            NBTStructure roomTape = load("moho-room-tape.nbt");
            NBTStructure roomReception = load("moho-room-reception.nbt");
            NBTStructure roomKitchen = load("moho-room-kitchen.nbt");
            NBTStructure cornerCap = load("moho-corner-cap.nbt");
            NBTStructure snorkel = load("moho-snorkel.nbt");

            NBTStructure.registerStructure(MOHO_DIMENSION_ID, new SpawnCondition("moho_base") {{
                spawnWeight = 4;
                minHeight = 63 - 11;
                maxHeight = 63;
                sizeLimit = 64;
                rangeLimit = 64;
                startPool = "start";
                pools = new HashMap<String, JigsawPool>() {{
                    put("start", new JigsawPool() {{
                        add(new JigsawPiece("moho_core", core) {{ heightOffset = -11; }}, 1);
                    }});
                    put("default", new JigsawPool() {{
                        add(new JigsawPiece("moho_corner_lab", cornerLab) {{ heightOffset = -14; }}, 2);
                        add(new JigsawPiece("moho_corner_heffer", cornerHeffer) {{ heightOffset = -17; }}, 2);
                        add(new JigsawPiece("moho_corner_extension", cornerExtension) {{ heightOffset = -11; }}, 1);
                        add(new JigsawPiece("moho_corner_empty", cornerEmpty) {{ heightOffset = -11; }}, 1);
                        fallback = "fallback";
                    }});
                    put("room", new JigsawPool() {{
                        add(new JigsawPiece("moho_room_tape", roomTape) {{ heightOffset = -11; }}, 1);
                        add(new JigsawPiece("moho_room_reception", roomReception) {{ heightOffset = -11; }}, 1);
                        add(new JigsawPiece("moho_room_kitchen", roomKitchen) {{ heightOffset = -11; }}, 1);
                        fallback = "room";
                    }});
                    put("fallback", new JigsawPool() {{
                        add(new JigsawPiece("moho_fall", cornerCap) {{ heightOffset = -11; }}, 1);
                    }});
                    put("snorkel", new JigsawPool() {{
                        add(new JigsawPiece("moho_snorkel", snorkel) {{ heightOffset = -11; }}, 1);
                    }});
                }};
            }});

            NBTStructure.registerNullWeight(MOHO_DIMENSION_ID, 20);
        } catch (RuntimeException e) {
            LOGGER.error("[space_m] Moho base structure registration failed", e);
        }
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        int i = ctx.origin().getX() & ~15;
        int j = ctx.origin().getZ() & ~15;

        BlockState mohoStone = MohoBlocks.MOHO_STONE.get().defaultBlockState();
        BlockState mohoRegolith = MohoBlocks.MOHO_REGOLITH.get().defaultBlockState();
        BlockState geysir = com.hbm_m.block.ModBlocks.GEYSIR_NETHER.get().defaultBlockState();
        BlockState netherrack = Blocks.NETHERRACK.defaultBlockState();

        for (int k = 0; k < 2; k++) {
            int x = i + rand.nextInt(16) + 8;
            int z = j + rand.nextInt(16) + 8;
            int d = 16 + rand.nextInt(96);

            for (int y = d - 5; y <= d; y++) {
                BlockState b = L.getState(world, x, y, z);
                if (L.getState(world, x, y + 1, z).isAir() && (Legacy.is(b, mohoStone) || Legacy.is(b, mohoRegolith))) {
                    L.setState(world, x, y, z, geysir, 3);
                    L.setState(world, x + 1, y, z, netherrack, 3);
                    L.setState(world, x - 1, y, z, netherrack, 3);
                    L.setState(world, x, y, z + 1, netherrack, 3);
                    L.setState(world, x, y, z - 1, netherrack, 3);
                    L.setState(world, x + 1, y - 1, z, netherrack, 3);
                    L.setState(world, x - 1, y - 1, z, netherrack, 3);
                    L.setState(world, x, y - 1, z + 1, netherrack, 3);
                    L.setState(world, x, y - 1, z - 1, netherrack, 3);
                }
            }
        }

        // Kick the volcanoes into action, and fix SOME floating lava
        // a full fix for floating lava would cause infinite cascades so we uh, don't
        BlockState volcanoCore = com.hbm_m.block.ModBlocks.VOLCANO_CORE.get().defaultBlockState();
        BlockState extinguishing = volcanoCore.setValue(VolcanoBlock.MODE, VolcanoBlock.META_STATIC_EXTINGUISHING);
        BlockState lava = Blocks.LAVA.defaultBlockState();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 32; y < 128; y++) {
                    int ox = i + x + 8;
                    int oz = j + z + 8;
                    BlockState b = L.getState(world, ox, y, oz);

                    if (b.is(Blocks.LAVA) && L.getState(world, ox, y - 1, oz).isAir()) {
                        // setBlock(flowing_lava, 0, 0) + markBlockForUpdate: Quelle darunter, Fliess-Tick einplanen
                        if (L.setState(world, ox, y - 1, oz, lava, 2)) {
                            world.scheduleTick(new BlockPos(ox, y - 1, oz), Fluids.LAVA, Fluids.LAVA.getTickDelay(world));
                        }
                    } else if (b.getBlock() == volcanoCore.getBlock()) {
                        L.setState(world, ox, y, oz, extinguishing, 2);
                    }
                }
            }
        }
        return true;
    }
}
