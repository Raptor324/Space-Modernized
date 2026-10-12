package com.space_m.test;

import java.util.List;
import java.util.Set;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.SpaceMod;
import com.space_m.dim.SpaceDimensions;
import com.space_m.entity.mob.EntitySiegeCraftSpace;
import com.space_m.fishing.SpaceFishing;
import com.space_m.orbit.propulsion.Lpw2Thruster;
import com.space_m.war.InvasionSpawner;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R9-B4: LPW-2 als Stationstriebwerk (Tanks, Schub, Brennen, Ausrichtung), Angeln auf den Himmelskoerpern
 * (Ike = Pluesch, tote Welten = 1.7.10-Muell statt Fisch, Kerbin/Orbit unveraendert) und das Space-Siege-Craft der
 * Invasion.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R9b4GameTest {

    private R9b4GameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9b4", timeoutTicks = 20)
    public static void lpw2Thruster(GameTestHelper helper) {
        check(SpaceApi.lpw2().active(), "Lpw2Hooks nicht gesetzt");
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        MachineLpw2BlockEntity be = new MachineLpw2BlockEntity(pos,
                ModBlocks.LPW2.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.SOUTH));
        be.setLevel(helper.getLevel());

        check(be.tanks.length == 2, "2 Tanks");
        check(be.tanks[0].getTankType() == ModFluids.KEROSENE_REFORM.getSource() && be.tanks[0].getMaxFill() == 256_000, "Tank 0 kerosene_reform 256000");
        check(be.tanks[1].getTankType() == ModFluids.OXYGEN.getSource() && be.tanks[1].getMaxFill() == 256_000, "Tank 1 oxygen 256000");
        check(Lpw2Thruster.isFacingPrograde(be), "SOUTH = prograd");

        Lpw2Thruster.Engine engine = Lpw2Thruster.engine(be);
        check(engine == Lpw2Thruster.engine(be), "Engine im addonState");
        check(engine.getThrust() == 2_000_000_000.0F, "Schub F1");
        check(!engine.canPerformBurn(200_000, 1000), "leer -> kein Brennen");
        int cost = engine.getFuelCost();
        check(cost > 0, "fuelCost > 0");
        be.tanks[0].setFill(256_000);
        be.tanks[1].setFill(256_000);
        check(engine.canPerformBurn(200_000, 1000), "voll -> Brennen");
        check(engine.startBurn() == 120 && be.isOn, "startBurn 120, an");
        check(be.tanks[0].getFill() == 256_000 - cost && be.tanks[1].getFill() == 256_000 - cost, "Verbrauch fuelCost je Tank");
        check(engine.endBurn() == 120 && !be.isOn, "endBurn 120, aus");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9b4", timeoutTicks = 20)
    public static void celestialFishing(GameTestHelper helper) {
        check(!SpaceFishing.isCelestial(Level.OVERWORLD) && !SpaceFishing.isCelestial(SpaceDimensions.ORBIT), "Kerbin/Orbit vanilla");
        check(SpaceFishing.isCelestial(SpaceDimensions.MUN) && SpaceFishing.isCelestial(SpaceDimensions.IKE), "Mun/Ike Space-Tabelle");

        RandomSource rand = RandomSource.create(42);
        List<ItemStack> vanilla = List.of(new ItemStack(Items.NAME_TAG));
        for (SpaceFishing.Category c : SpaceFishing.Category.values()) {
            List<ItemStack> ike = SpaceFishing.loot(SpaceDimensions.IKE, c, rand, x -> vanilla);
            check(ike.size() == 1 && ike.get(0).getItem() == ModBlocks.PLUSHIE.get().asItem(), "Ike -> Pluesch (" + c + ")");
        }

        Set<Item> dead = Set.of(Items.POTION, Items.STRING, Items.FISHING_ROD, Items.BOWL, Items.STICK, Items.INK_SAC, Items.TRIPWIRE_HOOK);
        for (int i = 0; i < 200; i++) {
            ItemStack s = SpaceFishing.loot(SpaceDimensions.MUN, SpaceFishing.Category.FISH, rand, x -> vanilla).get(0);
            check(dead.contains(s.getItem()), "Mun-Fisch = Muell, ist " + s);
            if (s.is(Items.INK_SAC)) check(s.getCount() == 10, "10 Tintenbeutel");
            if (s.is(Items.FISHING_ROD)) check(s.getDamageValue() >= 1 && s.getDamageValue() <= (int) (0.9F * s.getMaxDamage()), "Angel abgenutzt");
        }
        check(SpaceFishing.loot(SpaceDimensions.MUN, SpaceFishing.Category.JUNK, rand, x -> vanilla).get(0).is(Items.NAME_TAG), "Muell = Overworld");
        check(SpaceFishing.loot(SpaceDimensions.DUNA, SpaceFishing.Category.TREASURE, rand, x -> vanilla).get(0).is(Items.NAME_TAG), "Schatz = Overworld");
        check(SpaceFishing.rollCategory(rand, 0, false) != SpaceFishing.Category.TREASURE, "Schatz nur im offenen Wasser");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9b4", timeoutTicks = 20)
    public static void invasionSiegeCraft(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        new InvasionSpawner().spawnSiege(null, level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, true, 0F);
        List<EntitySiegeCraftSpace> crafts = level.getEntitiesOfClass(EntitySiegeCraftSpace.class, new AABB(pos).inflate(2));
        check(crafts.size() == 1, "Invasion spawnt das Space-Siege-Craft (entity_micro_ufo), gefunden " + crafts.size());
        crafts.forEach(e -> e.discard());
        helper.succeed();
    }
}
