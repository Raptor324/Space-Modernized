package com.space_m.deco;

import java.util.List;

import com.space_m.SpaceMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.shapes.CollisionContext;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/** R9-B2: Staemme (Achse, Tags), Erdbeere (Pflanzen + Beute), Fleisch (Halt + Beute), Maschendraht-Tor, Metallbloecke. */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SpaceDecoGameTest {

    private SpaceDecoGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static int count(List<ItemStack> drops, net.minecraft.world.item.Item item) {
        return drops.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9b2")
    public static void logsAndMetalBlocks(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        for (Block log : List.of(SpaceDeco.PRIMED_LOG.get(), SpaceDeco.EU_LOG.get(), SpaceDeco.BF_LOG.get(), SpaceDeco.LATTICE_LOG.get())) {
            helper.setBlock(rel, log.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            BlockState s = helper.getBlockState(rel);
            check(s.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X, "Stammachse X nicht gesetzt");
            check(s.is(BlockTags.LOGS) && s.is(BlockTags.MINEABLE_WITH_AXE), "Stamm ohne logs/mineable_axe");
        }
        check(SpaceDeco.BLOCK_NICKEL.get().defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS), "block_nickel nicht beacon_base");
        check(SpaceDeco.BLOCK_OSMIRIDIUM.get().defaultBlockState().is(BlockTags.BEACON_BASE_BLOCKS), "block_osmiridium nicht beacon_base");
        check(SpaceDeco.ASTROTURF.get().defaultBlockState().is(BlockTags.MINEABLE_WITH_SHOVEL), "astroturf nicht Schaufel");
        var rm = helper.getLevel().getRecipeManager();
        check(rm.byKey(SpaceMod.id("deco/block_nickel")).isPresent() && rm.byKey(SpaceMod.id("deco/fence_gate")).isPresent(), "Deko-Rezepte fehlen");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9b2")
    public static void strawberryPlantAndHarvest(GameTestHelper helper) {
        BlockPos soilRel = new BlockPos(1, 1, 1);
        helper.setBlock(soilRel, Blocks.FARMLAND.defaultBlockState());
        BlockPos soil = helper.absolutePos(soilRel);
        check(SpaceDeco.canPlantStrawberry(helper.getLevel(), soil), "Ackerboden nimmt keine Erdbeere");
        helper.setBlock(soilRel.above(), SpaceDeco.CROP_STRAWBERRY.get().defaultBlockState());
        BlockState crop = helper.getBlockState(soilRel.above());
        check(crop.canSurvive(helper.getLevel(), soil.above()), "Erdbeere haelt nicht auf Ackerboden");
        check(!SpaceDeco.canPlantStrawberry(helper.getLevel(), soil), "Doppelt gepflanzt");

        var ripe = SpaceDeco.CROP_STRAWBERRY.get().getStateForAge(7);
        List<ItemStack> drops = Block.getDrops(ripe, helper.getLevel(), soil.above(), null);
        check(count(drops, com.hbm_m.item.ModItems.STRAWBERRY.get()) == 4, "reife Erdbeere != 4: " + drops);
        List<ItemStack> young = Block.getDrops(SpaceDeco.CROP_STRAWBERRY.get().getStateForAge(5), helper.getLevel(), soil.above(), null);
        check(count(young, com.hbm_m.item.ModItems.STRAWBERRY.get()) == 2, "Stufe 5 != 2: " + young);
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9b2")
    public static void fleshAndGate(GameTestHelper helper) {
        BlockPos base = new BlockPos(1, 1, 1);
        helper.setBlock(base, Blocks.STONE.defaultBlockState());
        BlockPos top = helper.absolutePos(base.above());
        check(SpaceDeco.FLESH_BLOCK.get().defaultBlockState().canSurvive(helper.getLevel(), top), "Fleisch haelt nicht auf Stein");
        helper.setBlock(base, Blocks.ICE.defaultBlockState());
        check(!SpaceDeco.FLESH_BLOCK.get().defaultBlockState().canSurvive(helper.getLevel(), top), "Fleisch haelt auf Eis");
        List<ItemStack> fd = Block.getDrops(SpaceDeco.CHARRED_FLESH_BLOCK.get().defaultBlockState(), helper.getLevel(), top, null);
        check(fd.size() == 1 && fd.get(0).getItem() == com.space_m.item.SpaceMaterialItems.GRILLED_FLESH.get(), "verkohltes Fleisch -> grilled_flesh");

        helper.setBlock(base, Blocks.STONE.defaultBlockState());
        BlockState lower = SpaceDeco.FENCE_GATE.get().defaultBlockState().setValue(DoorBlock.FACING, Direction.EAST);
        helper.setBlock(base.above(), lower);
        helper.setBlock(base.above(2), lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        check(!helper.getBlockState(base.above()).getCollisionShape(helper.getLevel(), top, CollisionContext.empty()).isEmpty(), "Tor zu ohne Kollision");
        BlockState open = lower.setValue(DoorBlock.OPEN, true);
        check(open.getCollisionShape(helper.getLevel(), top, CollisionContext.empty()).isEmpty(), "Tor offen mit Kollision");
        List<ItemStack> gd = Block.getDrops(lower, helper.getLevel(), top, null);
        check(count(gd, SpaceDeco.FENCE_GATE_ITEM.get()) == 1, "Tor unten droppt kein fence_gate");
        check(Block.getDrops(lower.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), helper.getLevel(), top, null).isEmpty(), "Tor oben droppt");
        helper.succeed();
    }
}
