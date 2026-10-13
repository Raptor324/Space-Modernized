package com.space_m.test;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineFluidTankBlockEntity;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.space_m.SpaceMod;
import com.space_m.fluid.SpaceFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Nutzer-Meldung 2026-10-13 mit Addon: Fluessigkeitsbehaelter (Fass/Kanister/Gasflasche) in HBM-Fass-GUI und
 * Raketenstartrampe (gleicher FluidTank.loadTank-Weg) - einlegen und uebertragen, auch fuer Space-Fluessigkeiten.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FluidContainerGameTest {

    private FluidContainerGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static Player mockPlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    /** Startrampen-Weg: Wasserstoff-Fass und Space-Kanister fuellen typisierte Tanks (TileEntityLaunchPadRocket-Slots 3/4). */
    @GameTest(template = "empty3x3x3", batch = "space_fluid_containers", timeoutTicks = 100)
    public static void containersLoadTypedTanks(GameTestHelper helper) {
        Fluid h2 = ModFluids.HYDROGEN.getSource();
        FluidTank[] tanks = { new FluidTank(ModFluids.NONE.getSource(), 64_000), new FluidTank(h2, 64_000), new FluidTank(SpaceFluids.ELBOWGREASE.fluid(), 64_000) };
        ItemStack[] slots = { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), h2, 1), ItemStack.EMPTY };
        for (FluidTank t : tanks) t.loadTank(3, 4, slots);
        check(tanks[1].getFill() == 16_000, "Wasserstoff-Fass -> Tank: " + tanks[1].getFill());
        check(slots[3].isEmpty() && slots[4].is(ModItems.FLUID_BARREL_EMPTY.get()), "leeres Fass fehlt: " + slots[4]);

        ItemStack canister = ItemFluidTank.make(ModItems.CANISTER_FULL.get(), SpaceFluids.ELBOWGREASE.fluid(), 1);
        check(FluidContainerRegistry.getFluidContent(canister, SpaceFluids.ELBOWGREASE.fluid()) == 1000, "Space-Kanister nicht registriert");
        slots[3] = canister;
        slots[4] = ItemStack.EMPTY;
        for (FluidTank t : tanks) t.loadTank(3, 4, slots);
        check(tanks[2].getFill() == 1000, "Space-Kanister -> Tank: " + tanks[2].getFill());
        helper.succeed();
    }

    /** HBM-Fass mit Addon: GUI-Slot nimmt Behaelter an, Fass fuellt sich, unendliches Fass arbeitet (Erde: Wassertabelle). */
    @GameTest(template = "empty3x3x3", batch = "space_fluid_containers", timeoutTicks = 200)
    public static void hbmBarrelWorksWithAddon(GameTestHelper helper) {
        BlockPos posA = helper.absolutePos(new BlockPos(0, 1, 1));
        BlockPos posB = helper.absolutePos(new BlockPos(2, 1, 1));
        helper.getLevel().setBlock(posA, ModBlocks.BARREL_TCALLOY.get().defaultBlockState(), Block.UPDATE_ALL);
        helper.getLevel().setBlock(posB, ModBlocks.BARREL_STEEL.get().defaultBlockState(), Block.UPDATE_ALL);
        MachineFluidTankBlockEntity a = (MachineFluidTankBlockEntity) helper.getLevel().getBlockEntity(posA);
        MachineFluidTankBlockEntity b = (MachineFluidTankBlockEntity) helper.getLevel().getBlockEntity(posB);
        Fluid h2 = ModFluids.HYDROGEN.getSource();

        var menu = new com.hbm_m.inventory.menu.BarrelSteelMenu(0, mockPlayer(helper).getInventory(), a, new net.minecraft.world.inventory.SimpleContainerData(7));
        ItemStack barrel = ItemFluidTank.make(ModItems.FLUID_BARREL_FULL.get(), h2, 1);
        check(menu.slots.get(MachineFluidTankBlockEntity.SLOT_LOAD_IN).mayPlace(barrel), "Fass-GUI verweigert Wasserstoff-Fass");
        check(menu.slots.get(MachineFluidTankBlockEntity.SLOT_UNLOAD_IN).mayPlace(new ItemStack(ModItems.FLUID_BARREL_EMPTY.get())), "Fass-GUI verweigert leeres Fass");

        ItemStack idH2 = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
        FluidIdentifierItem.setType(idH2, h2, true);
        a.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_ID_IN, idH2);
        a.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN, barrel);
        ItemStack idW = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
        FluidIdentifierItem.setType(idW, ModFluids.WATER.getSource(), true);
        b.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_ID_IN, idW);
        b.getItemHandler().setStackInSlot(MachineFluidTankBlockEntity.SLOT_LOAD_IN, new ItemStack(ModItems.FLUID_BARREL_INFINITE.get()));

        helper.succeedWhen(() -> {
            check(a.getFluidTank().getTankType() == h2 && a.getFluidTank().getFill() == 16_000, "Fass Wasserstoff: " + a.getFluidTank().getFill());
            check(b.getFluidTank().getFill() == b.getFluidTank().getMaxFill(), "unendliches Fass: " + b.getFluidTank().getFill());
        });
    }
}
