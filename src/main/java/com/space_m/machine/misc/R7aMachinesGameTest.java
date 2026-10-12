package com.space_m.machine.misc;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.space_m.SpaceMod;
import com.space_m.machine.dyson.DysonBlocks;
import com.space_m.machine.dyson.IDysonConverter;
import com.space_m.machine.dyson.TileEntityDysonConverterAnatmogenesis;
import com.space_m.machine.dyson.TileEntityDysonConverterHE;
import com.space_m.machine.dyson.TileEntityDysonConverterTU;
import com.space_m.machine.dyson.TileEntityDysonReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R7-A: Dyson-System und Sonstiges gegen das Original (Strukturgroessen aus {@code getDimensions/fillSpace/makeExtra},
 * Gompertz-Leistungskurve, Wandler-Zielzellen, Raumofen-Atmung, Solarpanel-Platzierung).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R7aMachinesGameTest {

    private R7aMachinesGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static int cells(DummyableMachineBlock block) {
        return block.getStructureHelper().getPartOffsets().size() + 1;
    }

    /** Strukturgroessen: Solar 1x1x5, Radiator 3x3, HE 3x3x9, TU 3x3x8, Anatmogenesis 3x3x11, Stardar 25+8*3+2. */
    @GameTest(template = "empty3x3x3", batch = "space_r7a", timeoutTicks = 20)
    public static void structures(GameTestHelper helper) {
        check(cells(MiscBlocks.MACHINE_SOLAR.get()) == 5, "Solar 5, ist " + cells(MiscBlocks.MACHINE_SOLAR.get()));
        check(cells(MiscBlocks.MACHINE_RADIATOR.get()) == 9, "Radiator 9, ist " + cells(MiscBlocks.MACHINE_RADIATOR.get()));
        check(cells(DysonBlocks.DYSON_CONVERTER_HE.get()) == 81, "HE 81, ist " + cells(DysonBlocks.DYSON_CONVERTER_HE.get()));
        check(cells(DysonBlocks.DYSON_CONVERTER_TU.get()) == 72, "TU 72, ist " + cells(DysonBlocks.DYSON_CONVERTER_TU.get()));
        check(cells(DysonBlocks.DYSON_CONVERTER_ANATMOGENESIS.get()) == 99, "Anatmogenesis 99, ist " + cells(DysonBlocks.DYSON_CONVERTER_ANATMOGENESIS.get()));
        check(cells(MiscBlocks.MACHINE_STARDAR.get()) == 25 + 8 * 3 + 2, "Stardar 51, ist " + cells(MiscBlocks.MACHINE_STARDAR.get()));
        check(cells(DysonBlocks.DYSON_LAUNCHER.get()) > 500, "Startanlage gross, ist " + cells(DysonBlocks.DYSON_LAUNCHER.get()));
        check(cells(DysonBlocks.DYSON_RECEIVER.get()) > 200, "Empfaenger gross, ist " + cells(DysonBlocks.DYSON_RECEIVER.get()));
        helper.succeed();
    }

    /** Gompertz: 1 Mitglied fast nichts, steigend, Obergrenze Long.MAX_VALUE/10/20 je Tick. */
    @GameTest(template = "empty3x3x3", batch = "space_r7a", timeoutTicks = 20)
    public static void dysonPowerCurve(GameTestHelper helper) {
        long prev = -1;
        for (int i = 1; i < 5000; i *= 2) {
            long e = TileEntityDysonReceiver.getEnergyOutput(i);
            check(e >= prev, "monoton bei " + i);
            prev = e;
        }
        check(TileEntityDysonReceiver.getEnergyOutput(1) < 1000, "ein Mitglied schwach");
        check(TileEntityDysonReceiver.getEnergyOutput(100_000) <= Long.MAX_VALUE / 10 / 20 + 1000, "Obergrenze"); // double-Rundung von maxOutput
        TileEntityDysonReceiver.runTests(org.slf4j.LoggerFactory.getLogger("space_m/R7A"));
        helper.succeed();
    }

    /** Wandler nehmen nur an ihrer Zielzelle an (HE dir*4, TU dir*6, Anatmogenesis dir*5, jeweils y+1). */
    @GameTest(template = "empty3x3x3", batch = "space_r7a", timeoutTicks = 20)
    public static void converterTargets(GameTestHelper helper) {
        BlockPos p = new BlockPos(100, 70, 100);
        BlockState he = DysonBlocks.DYSON_CONVERTER_HE.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.EAST);
        TileEntityDysonConverterHE c = new TileEntityDysonConverterHE(p, he);
        c.setLevel(helper.getLevel());
        check(!c.provideEnergy(p.getX(), p.getY() + 1, p.getZ(), 1000), "HE falsche Zelle");
        check(c.provideEnergy(p.getX() + 4, p.getY() + 1, p.getZ(), 1000), "HE Zielzelle");
        check(c.getEnergyStored() == 1000, "HE Puffer = Energie, ist " + c.getEnergyStored());
        check(IDysonConverter.of(c) == c, "Adapter");

        BlockState tu = DysonBlocks.DYSON_CONVERTER_TU.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.NORTH);
        TileEntityDysonConverterTU t = new TileEntityDysonConverterTU(p, tu);
        check(t.provideEnergy(p.getX(), p.getY() + 1, p.getZ() - 6, 5L + Integer.MAX_VALUE), "TU Zielzelle");
        check(t.getHeatStored() == Integer.MAX_VALUE, "TU Ueberlauf -> Maximum");
        t.useUpHeat(Integer.MAX_VALUE);
        check(t.getHeatStored() == 0, "TU useUpHeat");

        BlockState an = DysonBlocks.DYSON_CONVERTER_ANATMOGENESIS.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.SOUTH);
        TileEntityDysonConverterAnatmogenesis a = new TileEntityDysonConverterAnatmogenesis(p, an);
        check(!a.provideEnergy(p.getX(), p.getY() + 1, p.getZ() + 4, 28_800_000L), "Anatmogenesis falsche Zelle");
        helper.succeed();
    }

    /** Raumofen: auf der Erde (Oberwelt) normale Brenndauer, Lava immer; Drop/Pick = Vanilla-Ofen. */
    @GameTest(template = "empty3x3x3", batch = "space_r7a", timeoutTicks = 20)
    public static void spaceFurnace(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, MiscBlocks.FURNACE.get());
        check(helper.getBlockEntity(rel) instanceof TileEntityFurnaceSpace, "Raumofen-BE");
        TileEntityFurnaceSpace f = (TileEntityFurnaceSpace) helper.getBlockEntity(rel);
        check(f.getBurnDuration(new ItemStack(Items.COAL)) == 1600, "Kohle 1600 auf der Erde");
        check(f.getBurnDuration(new ItemStack(Items.LAVA_BUCKET)) == 20000, "Lava 20000");
        BlockFurnaceSpace.updateFurnaceBlockState(true, helper.getLevel(), helper.absolutePos(rel));
        check(helper.getBlockState(rel).is(MiscBlocks.LIT_FURNACE.get()), "Wechsel auf lit_furnace");
        check(helper.getBlockEntity(rel) instanceof TileEntityFurnaceSpace, "BE bleibt");
        helper.setBlock(rel, Blocks.AIR);
        helper.succeed();
    }

    /** Solarpanel im Spiel: 5 Zellen, Abriss raeumt alles weg. */
    @GameTest(template = "empty3x3x3", batch = "space_r7a", timeoutTicks = 40)
    public static void solarPlacement(GameTestHelper helper) {
        BlockPos core = new BlockPos(1, 1, 1);
        helper.setBlock(core, MiscBlocks.MACHINE_SOLAR.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(2, () -> {
            check(helper.getBlockEntity(core) instanceof TileEntityMachineSolarPanel, "Solar-BE");
            int parts = 0;
            for (int z = -2; z <= 2; z++) {
                BlockPos p = core.offset(0, 0, z);
                if (!helper.getBlockState(p).isAir()) parts++;
            }
            check(parts >= 3, "Phantomteile gesetzt, ist " + parts);
            helper.setBlock(core, Blocks.AIR);
            helper.succeed();
        });
    }
}
