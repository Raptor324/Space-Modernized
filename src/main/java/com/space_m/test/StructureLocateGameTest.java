package com.space_m.test;

import java.io.InputStream;

import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.world.gen.nbt.NBTStructure;
import com.space_m.SpaceMod;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.Structure;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Strukturen aller Koerper: {@code /locate structure space_m:<koerper>_structures} findet sie (gleicher Pfad wie der
 * Befehl: {@code ChunkGenerator.findNearestMapStructure}, Radius 100 Zellen), und die Mehrblock-Umsetzung
 * (W-R3b-1) der Dres-RBMK-Hallen passt zur HBM-Schablone.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StructureLocateGameTest {

    private StructureLocateGameTest() {}

    private static void check(boolean ok, String msg) {
        if (!ok) throw new GameTestAssertException(msg);
    }

    /** Koerper mit aktiven Strukturen (Thatmo: nur Meteor-Verliese wie im Original). */
    private static final String[] BODIES = { "mun", "duna", "dres", "minmus", "ike", "laythe", "moho", "eve", "tekto", "thatmo" };

    @GameTest(template = "empty3x3x3", batch = "space_structures", timeoutTicks = 6000)
    public static void locateFindsAllBodyStructures(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var structures = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        StringBuilder missing = new StringBuilder();
        int checked = 0;
        for (String body : BODIES) {
            var key = ResourceKey.create(Registries.STRUCTURE, SpaceMod.id(body + "_structures"));
            check(structures.containsKey(key), "structure space_m:" + body + "_structures missing");
            ServerLevel level = server.getLevel(SpaceDimensions.level(body));
            if (level == null) continue;
            var holder = structures.getHolderOrThrow(key);
            var found = level.getChunkSource().getGenerator().findNearestMapStructure(level, HolderSet.direct(holder), BlockPos.ZERO, 100, false);
            checked++;
            com.mojang.logging.LogUtils.getLogger().info("[space_m] /locate structure space_m:{}_structures -> {}", body, found == null ? "none" : found.getFirst());
            if (found == null) { missing.append(' ').append(body); continue; }
            // dort wirklich gebaut? Chunks um den Fund voll generieren und HBM-Bloecke zaehlen
            BlockPos at = found.getFirst();
            int hbm = 0;
            for (int cx = (at.getX() >> 4) - 1; cx <= (at.getX() >> 4) + 1; cx++)
                for (int cz = (at.getZ() >> 4) - 1; cz <= (at.getZ() >> 4) + 1; cz++) {
                    var c = level.getChunk(cx, cz);
                    for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) for (int y = 1; y < 256; y++) {
                        var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(c.getBlockState(new BlockPos((cx << 4) + x, y, (cz << 4) + z)).getBlock());
                        if ("hbm_m".equals(id.getNamespace()) && !id.getPath().startsWith("ore") && !id.getPath().equals("basalt") && !id.getPath().equals("moon_turf")) hbm++;
                    }
                }
            com.mojang.logging.LogUtils.getLogger().info("[space_m] {}: {} HBM structure blocks around {}", body, hbm, at);
            if (hbm < 10) missing.append(' ').append(body).append("(not built at ").append(at.toShortString()).append(')');
        }
        check(missing.length() == 0, "/locate found nothing for:" + missing + " (" + checked + " levels)");
        check(structures.containsKey(ResourceKey.create(Registries.STRUCTURE, SpaceMod.id("ike_pyramid"))), "structure space_m:ike_pyramid missing");
        helper.succeed();
    }

    private static NBTStructure load(String file) throws Exception {
        try (InputStream in = StructureLocateGameTest.class.getResourceAsStream("/assets/space_m/structures/" + file)) {
            check(in != null, "missing " + file);
            return new NBTStructure(file, in);
        }
    }

    /** W-R3b-1: jeder Original-Dummy gehoert zu einer HBM-Schablone (keine verworfenen/blockierten Teile). */
    @GameTest(template = "empty3x3x3", batch = "space_structures", timeoutTicks = 200)
    public static void dresRbmkHallMultiblocks(GameTestHelper helper) throws Exception {
        for (String f : new String[] { "dresbmk/hall/breeder.nbt", "dresbmk/hall/starbmk.nbt", "dresbmk/airlock.nbt", "martian-base.nbt", "munbase/base.nbt", "mohobase/moho-core.nbt", "nuke-sub.nbt" }) {
            int[] s = load(f).getMultiblockStats();
            com.mojang.logging.LogUtils.getLogger().info("[space_m] {}: {} Mehrblock-Maschinen, {} Teile ({} neu), {} Dummies ohne Schablone, {} blockiert", f, s[0], s[1], s[2], s[3], s[4]);
            check(s[0] > 0, f + ": no multiblock controller expanded");
            check(s[3] == 0, f + ": " + s[3] + " original dummies outside the HBM multiblock templates (facing mismatch?)");
        }
        helper.succeed();
    }

    /** Die Halle in die Welt bauen: Teile verbinden sich beim ersten Tick selbst mit ihrem Steuerblock. */
    @GameTest(template = "empty3x3x3", batch = "space_structures", timeoutTicks = 400)
    public static void dresRbmkHallPartsLink(GameTestHelper helper) throws Exception {
        NBTStructure hall = load("dresbmk/hall/breeder.nbt");
        BlockPos origin = helper.absolutePos(BlockPos.ZERO).offset(0, 120, 0);
        ServerLevel level = helper.getLevel();
        hall.build(level, origin.getX(), origin.getY(), origin.getZ(), 1, false, true);
        helper.runAfterDelay(100, () -> {
            int parts = 0, linked = 0;
            for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(hall.getSizeZ(), hall.getSizeY(), hall.getSizeX()))) {
                BlockEntity be = level.getBlockEntity(p);
                if (be instanceof IMultiblockPart part) {
                    parts++;
                    if (part.getControllerPos() != null) linked++;
                }
            }
            com.mojang.logging.LogUtils.getLogger().info("[space_m] breeder hall (rotated): {} parts, {} linked", parts, linked);
            for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(hall.getSizeZ(), hall.getSizeY(), hall.getSizeX())))
                if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2 | 16);
            check(parts > 100, "breeder hall: only " + parts + " machine parts");
            check(linked == parts, "breeder hall: " + (parts - linked) + " of " + parts + " parts not linked to a controller");
            helper.succeed();
        });
    }
}
