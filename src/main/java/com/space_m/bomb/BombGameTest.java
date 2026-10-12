package com.space_m.bomb;

import com.hbm_m.entity.logic.EntityNukeExplosionMK3;
import com.hbm_m.item.ModItems;
import com.space_m.SpaceMod;
import com.space_m.item.SpaceMaterialItems;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R9-B1: Platzierung und Grundverhalten der Space-Bomben/Einzelbloecke gegen das Original.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BombGameTest {

    private BombGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** Alle Bloecke setzbar, Blockentitaeten mit eigenem Typ; Kondensator 1e17 HE, Laderaten power/200, power/600. */
    @GameTest(template = "empty3x3x3", batch = "space_r9b1", timeoutTicks = 20)
    public static void placement(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, SpaceBombs.NUKE_ANTIMATTER.get());
        check(helper.getBlockEntity(p) instanceof TileEntityAntimatter, "nuke_antimatter ohne TileEntityAntimatter");
        helper.setBlock(p, SpaceBombs.CAPACITOR_COMPLEX.get());
        check(helper.getBlockEntity(p) instanceof TileEntityCapacitorComplex cap
                && cap.getMaxEnergyStored() == 100_000_000_000_000_000L
                && cap.getType() == SpaceBombs.CAPACITOR_COMPLEX_BE.get(), "capacitor_complex 1e17 HE / eigener Typ");
        helper.setBlock(p, SpaceBombs.FF.get());
        check(helper.getBlockEntity(p) instanceof TileEntityFF, "ff ohne TileEntityFF");
        helper.setBlock(p, SpaceBombs.OIL_BARREL.get());
        helper.setBlock(p, SpaceBombs.DET_SALT.get());
        helper.setBlock(p, Blocks.AIR);
        check(!SpaceBombs.SALTED_FALLOUT.get().isRandomlyTicking(SpaceBombs.SALTED_FALLOUT.get().defaultBlockState()),
                "salted_fallout tickt im Original nie");
        check(SpaceBombs.FF.get().asItem() == net.minecraft.world.item.Items.AIR, "ff hat im Original kein Item");
        helper.succeed();
    }

    /** Geladen nur mit Kern + 2 Bleiteilchen + 2 Hafnium; Start nur geladen; fehlender Teil bricht den Countdown ab. */
    @GameTest(template = "empty3x3x3", batch = "space_r9b1", timeoutTicks = 40)
    public static void antimatterLoading(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, SpaceBombs.NUKE_ANTIMATTER.get());
        TileEntityAntimatter be = (TileEntityAntimatter) helper.getBlockEntity(p);
        check(be.timer == 18000, "Timer 18000");
        be.handleButtonPacket(0, 0);
        check(!be.started, "ungeladen startet nicht");
        be.setItem(2, new ItemStack(ModItems.PELLET_ANTIMATTER.get()));
        be.setItem(0, new ItemStack(ModItems.PARTICLE_LEAD.get()));
        be.setItem(1, new ItemStack(ModItems.PARTICLE_LEAD.get()));
        be.setItem(3, new ItemStack(SpaceMaterialItems.INGOT_HAFNIUM.get()));
        check(!be.isLoaded(), "ohne zweites Hafnium nicht geladen");
        be.setItem(4, new ItemStack(SpaceMaterialItems.INGOT_HAFNIUM.get()));
        check(be.isLoaded(), "geladen");
        be.handleButtonPacket(5, 1);
        check(be.timer == 100, "Timer in Sekunden * 20");
        be.handleButtonPacket(0, 0);
        check(be.started, "gestartet");
        be.setItem(2, ItemStack.EMPTY);
        helper.runAfterDelay(2, () -> {
            check(!be.started, "Countdown bricht ohne Kern ab");
            check(helper.getBlockState(p).is(SpaceBombs.NUKE_ANTIMATTER.get()), "Bombe steht noch");
            helper.succeed();
        });
    }

    /** Troll-Stoerer traegt sich wie der Feldstoerer in EntityNukeExplosionMK3.at ein. */
    @GameTest(template = "empty3x3x3", batch = "space_r9b1", timeoutTicks = 60)
    public static void trollDisturber(GameTestHelper helper) {
        BlockPos p = new BlockPos(1, 1, 1);
        helper.setBlock(p, SpaceBombs.TROLL_DISTURBER.get());
        BlockPos abs = helper.absolutePos(p);
        String dim = helper.getLevel().dimension().location().toString();
        helper.succeedWhen(() -> check(EntityNukeExplosionMK3.at.containsKey(
                new EntityNukeExplosionMK3.ATEntry(dim, abs.getX(), abs.getY(), abs.getZ())), "kein Eintrag in MK3.at"));
    }
}
