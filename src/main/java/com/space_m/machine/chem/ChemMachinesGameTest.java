package com.space_m.machine.chem;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.space_m.SpaceMod;
import com.space_m.machine.chem.recipe.AlkylationRecipe;
import com.space_m.machine.chem.recipe.ChemRecipes;
import com.space_m.machine.chem.recipe.CryoRecipe;
import com.space_m.machine.chem.recipe.VacuumCircuitRecipe;
import com.space_m.machine.mining.DishMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R7-B Chemie: GameTests (Batch {@code space_chem}). Strukturgroessen, Rezepte (Datenpaket, 1:1 Original-Defaults),
 * Verarbeitung der Alkylierung/Kryo-Destille/Milch, Vakuum-Loeter-Bedingung, Hydroponik (Lampen, Pflanzen, Boden-Mixin).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChemMachinesGameTest {

    private ChemMachinesGameTest() {}

    static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static int cells(DummyableMachineBlock block) {
        return block.getStructureHelper().getPartOffsets().size() + 1;
    }

    private static BlockState facing(BlockState s, Direction d) {
        return s.setValue(DummyableMachineBlock.FACING, d);
    }

    /** Strukturgroessen aus getDimensions/fillSpace. */
    @GameTest(template = "empty3x3x3", batch = "space_chem", timeoutTicks = 20)
    public static void structures(GameTestHelper helper) {
        check(cells(ChemMachines.MACHINE_ALKYLATION.get()) == 4 * 5 * 3, "alkylation 3x4x5, ist " + cells(ChemMachines.MACHINE_ALKYLATION.get()));
        check(cells(ChemMachines.MACHINE_MILK_REFORMER.get()) == 3 * 3 * 7, "milk 3x3x7");
        check(cells(ChemMachines.MACHINE_VACUUM_CIRCUIT.get()) == 9, "vacuum 3x3");
        check(cells(DishMachines.DISH_CONTROL.get()) == 2, "dish 2");
        // Hydroponik: 3x3x5 = 45 minus 3 Pflanzfelder, 2 Lampen, 10 freie Bodenzellen vorne/hinten
        int hydro = cells(ChemMachines.HYDROBAY.get());
        check(hydro == 30, "hydrobay cells " + hydro);
        check(ChemMachines.MACHINE_CRYO_DISTILL.get().getHeightOffset() == 2, "cryo height offset 2");
        helper.succeed();
    }

    /** Datenpaket-Rezepte = Original registerDefaults (6 / 8 / 3). */
    @GameTest(template = "empty3x3x3", batch = "space_chem", timeoutTicks = 20)
    public static void recipes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        check(ChemRecipes.all(level, AlkylationRecipe.Type.INSTANCE).size() == 6, "6 alkylation");
        check(ChemRecipes.all(level, CryoRecipe.Type.INSTANCE).size() == 8, "8 cryo");
        check(ChemRecipes.all(level, VacuumCircuitRecipe.Type.INSTANCE).size() == 3, "3 vacuum circuit");

        CryoRecipe air = CryoRecipe.getOutput(level, ChemUtil.fluid("EARTHAIR"));
        check(air != null && ChemRecipes.type(air.getOutput(0)) == ChemUtil.fluid("NITROGEN") && ChemRecipes.fill(air.getOutput(0)) == 60
                && ChemRecipes.fill(air.getOutput(3)) == 5, "earthair -> N2 60 ... CO2 5");
        AlkylationRecipe petro = AlkylationRecipe.getOutput(level, ChemUtil.fluid("PETROLEUM"));
        check(petro != null && ChemRecipes.type(petro.getAcid()) == ChemUtil.fluid("SULFURIC_ACID") && ChemRecipes.fill(petro.getOutput1()) == 50, "petroleum alkylation");
        AlkylationRecipe chloro = AlkylationRecipe.getOutput(level, ChemUtil.fluid("CHLOROMETHANE"));
        check(chloro != null && ChemUtil.isNone(ChemRecipes.type(chloro.getAcid())), "chloromethane ohne Saeure");

        ItemStack[] in = { new ItemStack(com.hbm_m.item.ModItems.MICROCHIP.get(), 8), ItemStack.EMPTY,
                new ItemStack(com.hbm_m.item.ModItems.PCB.get(), 2), new ItemStack(com.hbm_m.item.ModItems.CAPACITOR.get(), 4) };
        VacuumCircuitRecipe t1 = VacuumCircuitRecipe.getRecipe(level, in);
        check(t1 != null && t1.duration == 200 && t1.consumption == 250, "processt1 200t 250HE");
        in[0] = new ItemStack(com.hbm_m.item.ModItems.MICROCHIP.get(), 7);
        check(VacuumCircuitRecipe.getRecipe(level, in) == null, "zu wenige Chips");
        helper.succeed();
    }

    /** Alkylierung, Kryo-Destille, Milch: ein Zyklus 1:1. */
    @GameTest(template = "empty3x3x3", batch = "space_chem", timeoutTicks = 20)
    public static void processing(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        TileEntityMachineAlkylation alk = new TileEntityMachineAlkylation(pos, facing(ChemMachines.MACHINE_ALKYLATION.get().defaultBlockState(), Direction.NORTH));
        alk.setLevel(level);
        alk.tanks[0].setTankType(ChemUtil.fluid("PETROLEUM"));
        alk.tanks[0].setFill(1000);
        alk.alkylate(); // ohne Strom/Saeure: nur Tanktypen
        check(alk.tanks[1].getTankType() == ChemUtil.fluid("SULFURIC_ACID") && alk.tanks[2].getTankType() == ChemUtil.fluid("LIGHTOIL"), "alk types");
        alk.tanks[1].setFill(1000);
        alk.setEnergyStored(10_000);
        alk.alkylate();
        check(alk.tanks[0].getFill() == 900 && alk.tanks[1].getFill() == 950 && alk.tanks[2].getFill() == 50 && alk.tanks[3].getFill() == 20
                && alk.getEnergyStored() == 6_000, "alk cycle");

        TileEntityMachineCryoDistill cryo = new TileEntityMachineCryoDistill(pos, facing(ChemMachines.MACHINE_CRYO_DISTILL.get().defaultBlockState(), Direction.NORTH));
        cryo.setLevel(level);
        cryo.tanks[0].setFill(64_000);
        cryo.setEnergyStored(1_000_000);
        cryo.distill();
        check(cryo.tanks[0].getFill() == 63_900 && cryo.tanks[1].getFill() == 60 && cryo.tanks[2].getFill() == 25
                && cryo.tanks[3].getFill() == 10 && cryo.tanks[4].getFill() == 5 && cryo.getEnergyStored() == 980_000, "cryo cycle");

        TileEntityMachineMilkReformer milk = new TileEntityMachineMilkReformer(pos, facing(ChemMachines.MACHINE_MILK_REFORMER.get().defaultBlockState(), Direction.NORTH));
        milk.setLevel(level);
        milk.tanks[0].setFill(100);
        milk.setEnergyStored(10_000);
        milk.refine();
        check(milk.tanks[0].getFill() == 0 && milk.tanks[1].getFill() == 50 && milk.tanks[2].getFill() == 35 && milk.tanks[3].getFill() == 15
                && milk.getEnergyStored() == 0, "milk cycle");
        helper.succeed();
    }

    /** Hydroponik: Struktur + Lampen setzen, Licht ab 200 HE, Weizen wird gepflanzt und ueberlebt auf den Zellen. */
    @GameTest(template = "empty3x3x3", batch = "space_chem", timeoutTicks = 40)
    public static void hydroponic(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos core = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = facing(ChemMachines.HYDROBAY.get().defaultBlockState(), Direction.NORTH);
        level.setBlock(core, state, 3);

        BlockPos[] lamps = MachineHydroponic.lampCells(core, Direction.NORTH);
        for (BlockPos l : lamps) check(level.getBlockState(l).getBlock() instanceof BlockDummyableBeam, "Lampe gesetzt " + l);
        check(HydroponicSoil.sustains(level, core) && HydroponicSoil.sustains(level, MachineHydroponic.plantCell(core, Direction.NORTH, 0).below()), "Boden traegt");

        if (!(level.getBlockEntity(core) instanceof TileEntityHydroponic hydro)) throw new GameTestAssertException("hydro BE");
        hydro.getInventory().setStackInSlot(0, new ItemStack(Items.WHEAT_SEEDS, 3));
        hydro.tanks[0].setFill(16_000);
        hydro.setEnergyStored(2_000);
        hydro.update(level, state);

        check(level.getBlockState(lamps[0]).getValue(BlockDummyableBeam.LIT), "Licht an");
        for (int i = 0; i < 3; i++) {
            BlockPos p = MachineHydroponic.plantCell(core, Direction.NORTH, i);
            BlockState plant = level.getBlockState(p);
            check(plant.getBlock() instanceof CropBlock, "Weizen " + i + " gepflanzt");
            check(plant.canSurvive(level, p), "Weizen " + i + " ueberlebt");
        }
        check(hydro.getInventory().getStackInSlot(0).isEmpty(), "3 Saaten verbraucht");

        // Abbau entfernt Lampen
        level.setBlock(core, Blocks.AIR.defaultBlockState(), 3);
        check(level.getBlockState(lamps[0]).isAir() && level.getBlockState(lamps[1]).isAir(), "Lampen entfernt");
        helper.succeed();
    }

    /** Vakuum-Loeter: in der Oberwelt (Kerbin-Luft) kein Betrieb. */
    @GameTest(template = "empty3x3x3", batch = "space_chem", timeoutTicks = 20)
    public static void vacuumNeedsVacuum(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        TileEntityMachineVacuumCircuit vac = new TileEntityMachineVacuumCircuit(pos, facing(ChemMachines.MACHINE_VACUUM_CIRCUIT.get().defaultBlockState(), Direction.NORTH));
        vac.setLevel(level);
        TileEntityMachineVacuumCircuit.tick(level, pos, vac.getBlockState(), vac);
        check(!vac.canOperate, "Oberwelt hat Atmosphaere");
        check(vac.getMaxEnergyStored() == 2_000 && vac.consumption == 100, "ohne Rezept 2000/100");
        helper.succeed();
    }
}
