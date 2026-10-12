package com.space_m.machine.mining;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.material.MaterialShapes;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.machine.chem.ChemUtil;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.rocket.SpaceRocketItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
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
 * R7-B Bergbau (Magma, Gas-Dock, Drive-Processor): GameTests (Batch {@code space_mining}). Strukturgroessen und Werte
 * 1:1, Magma-Bohrer arbeitet ausserhalb von Moho nicht (Lava-Pruefung getrennt), Sammelzyklus des Gas-Docks
 * (20 Ticks Sinken -> Start mit 500+500 mB -> 8 000 mB Gas, Landung nach 101 Ticks), Drive-Processor (Verarbeiten in
 * 100 Ticks, Klonen, Fehlermeldungen).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MiningMachinesGameTest {

    private MiningMachinesGameTest() {}

    static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static int cells(DummyableMachineBlock block) {
        return block.getStructureHelper().getPartOffsets().size() + 1;
    }

    private static BlockState facing(BlockState state, Direction dir) {
        return state.setValue(DummyableMachineBlock.FACING, dir);
    }

    /** Strukturen ({@code getDimensions}), Hoehenversatz, Strom/Tanks. */
    @GameTest(template = "empty3x3x3", batch = "space_mining", timeoutTicks = 20)
    public static void miningValues(GameTestHelper helper) {
        check(cells(MiningMachines.MACHINE_MAGMA.get()) == 7 * 7 * 7, "magma 7x7x7, ist " + cells(MiningMachines.MACHINE_MAGMA.get()));
        check(cells(MiningMachines.GAS_DOCK.get()) == 3 * 3, "gas dock 3x3x1");
        check(cells(MiningMachines.MACHINE_DRIVE_PROCESSOR.get()) == 2, "drive processor 2 cells");
        check(MiningMachines.MACHINE_MAGMA.get().getHeightOffset() == 3, "magma height offset 3");

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        ServerLevel level = helper.getLevel();

        TileEntityMachineMagma magma = new TileEntityMachineMagma(pos, facing(MiningMachines.MACHINE_MAGMA.get().defaultBlockState(), Direction.NORTH));
        magma.setLevel(level);
        check(magma.consumption == 10_000 && magma.getMaxEnergyStored() == 1_000_000L, "magma 10 000 HE/t, 1 000 000 HE");
        check(TileEntityMachineMagma.maxLiquid == MaterialShapes.BLOCK.q(16) && magma.tanks.length == 0, "magma 16 blocks, no tanks");

        TileEntityMachineGasDock dock = new TileEntityMachineGasDock(pos, MiningMachines.GAS_DOCK.get().defaultBlockState());
        check(dock.tanks[0].getTankType() == ChemUtil.fluid("joolgas") && dock.tanks[0].getMaxFill() == 64_000, "gas tank joolgas 64 000");
        check(dock.tanks[1].getTankType() == ChemUtil.fluid("hydrogen") && dock.tanks[1].getMaxFill() == 32_000, "hydrogen 32 000");
        check(dock.tanks[2].getTankType() == ChemUtil.fluid("oxygen") && dock.tanks[2].getMaxFill() == 32_000, "oxygen 32 000");
        check(dock.hasRocket && dock.launchTicks == 0, "rocket docked at start");

        TileEntityMachineDriveProcessor proc = new TileEntityMachineDriveProcessor(pos, MiningMachines.MACHINE_DRIVE_PROCESSOR.get().defaultBlockState());
        check(proc.maxPower == 2_000 && proc.getMaxEnergyStored() == 2_000L && proc.maxProgress == 100, "drive processor 2 000 HE, 100 ticks");
        check(proc.getInventory().getSlots() == 4 && proc.getInventory().getSlotLimit(0) == 1, "4 slots, stack limit 1");

        helper.succeed();
    }

    /** Magma-Bohrer: nicht auf Moho -> kein Betrieb, kein Verbrauch, keine Schmelze; Lava-Pruefung 3x3 vier unter dem Kern. */
    @GameTest(template = "empty3x3x3", batch = "space_mining", timeoutTicks = 20)
    public static void magmaNeedsMoho(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        check(!"moho".equals(CelestialBody.getBody(level).name), "test level must not be moho");

        BlockPos core = helper.absolutePos(new BlockPos(1, 5, 1));
        TileEntityMachineMagma magma = new TileEntityMachineMagma(core, facing(MiningMachines.MACHINE_MAGMA.get().defaultBlockState(), Direction.NORTH));
        magma.setLevel(level);
        magma.setEnergyStored(1_000_000L);

        check(!magma.isValidPosition(), "no lava -> invalid");
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                level.setBlock(core.offset(x, -4, z), Blocks.LAVA.defaultBlockState(), 2);
        check(magma.isValidPosition(), "3x3 lava below -> valid");

        for (int i = 0; i < 20; i++) TileEntityMachineMagma.tick(level, core, magma.getBlockState(), magma);
        check(!magma.operating, "not on moho -> not operating");
        check(magma.getEnergyStored() == 1_000_000L, "no power used");
        check(magma.liquids.isEmpty(), "no liquids");

        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++)
                level.setBlock(core.offset(x, -4, z), Blocks.AIR.defaultBlockState(), 2);

        helper.succeed();
    }

    /** Gas-Dock: ohne Gasriesen kein Sammeln; mit Gas (Jool) 20 Ticks Sinken, Start + Sammeln, Landung nach 100 Ticks Flug. */
    @GameTest(template = "empty3x3x3", batch = "space_mining", timeoutTicks = 20)
    public static void gasDockCycle(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        TileEntityMachineGasDock dock = new TileEntityMachineGasDock(pos, MiningMachines.GAS_DOCK.get().defaultBlockState());
        dock.setLevel(level);
        dock.tanks[1].setFill(1_000);
        dock.tanks[2].setFill(1_000);

        // kein Gasriese: Zaehler laeuft, aber kein Start
        for (int i = 0; i < 40; i++) dock.cycle(null);
        check(dock.hasRocket && dock.launchTicks == -20 && dock.tanks[0].getFill() == 0, "no gas giant -> no launch");

        dock = new TileEntityMachineGasDock(pos, MiningMachines.GAS_DOCK.get().defaultBlockState());
        dock.setLevel(level);
        dock.tanks[1].setFill(1_000);
        dock.tanks[2].setFill(1_000);

        net.minecraft.resources.ResourceLocation jool = SpaceMod.id("joolgas");
        for (int i = 0; i < 19; i++) dock.cycle(jool);
        check(dock.hasRocket && dock.tanks[0].getFill() == 0, "still sinking after 19 ticks");
        dock.cycle(jool);
        check(!dock.hasRocket, "launch at launchTicks -20");
        check(dock.tanks[0].getFill() == 8_000 && dock.tanks[1].getFill() == 500 && dock.tanks[2].getFill() == 500, "500+500 mB -> 8 000 mB gas");

        for (int i = 0; i < 100; i++) dock.cycle(jool);
        check(!dock.hasRocket && dock.launchTicks == 99, "in flight after 100 ticks");
        dock.cycle(jool);
        check(dock.hasRocket && dock.launchTicks == 100, "landed at launchTicks 100");

        for (int i = 0; i < 120; i++) dock.cycle(jool);
        check(!dock.hasRocket && dock.tanks[0].getFill() == 16_000 && dock.tanks[1].getFill() == 0, "second trip");

        for (int i = 0; i < 241; i++) dock.cycle(jool);
        check(dock.tanks[0].getFill() == 16_000, "no fuel -> no further trip");

        // Originalfehler: hasRocket wird nicht gespeichert
        dock.hasRocket = false;
        TileEntityMachineGasDock loaded = new TileEntityMachineGasDock(pos, MiningMachines.GAS_DOCK.get().defaultBlockState());
        //? if < 1.21.1 {
        loaded.load(dock.saveWithoutMetadata());
        //?} else {
        /*loaded.loadWithComponents(dock.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
        *///?}
        check(loaded.hasRocket && loaded.tanks[0].getFill() == 16_000, "hasRocket not saved (original), tanks saved");

        helper.succeed();
    }

    /** Drive-Processor: Verarbeitung in 100 Ticks, Fehlermeldungen, Klonen. */
    @GameTest(template = "empty3x3x3", batch = "space_mining", timeoutTicks = 20)
    public static void driveProcessor(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));

        TileEntityMachineDriveProcessor proc = new TileEntityMachineDriveProcessor(pos, MiningMachines.MACHINE_DRIVE_PROCESSOR.get().defaultBlockState());
        proc.setLevel(level);

        ItemStack drive = ItemVOTVdrive.of(SolarSystem.Body.MOHO);
        proc.getInventory().setStackInSlot(0, drive);

        // kein Strom
        proc.serverTick();
        check(proc.status.contains("No power") && !proc.isProcessing, "no power status");

        proc.setEnergyStored(2_000L);
        int needed = ItemVOTVdrive.getProcessingTier(drive, CelestialBody.getBody(level));
        if (needed > 0) {
            proc.serverTick();
            check(proc.status.contains("Low tier"), "low tier status (needs " + needed + ")");
            CompoundTag start = new CompoundTag();
            start.putBoolean("process", true);
            proc.receiveControl(start);
            check(!proc.isProcessing, "low tier -> no processing");
        }

        proc.getInventory().setStackInSlot(2, new ItemStack(SpaceMaterialItems.PROCESST3.get()));
        check(proc.getProcessingTier() == 3, "processt3 -> tier 3");
        proc.serverTick();

        CompoundTag start = new CompoundTag();
        start.putBoolean("process", true);
        proc.receiveControl(start);
        check(proc.isProcessing, "processing started");

        for (int i = 0; i < 99; i++) {
            proc.setEnergyStored(2_000L);
            proc.serverTick();
        }
        check(proc.isProcessing && proc.progress == 99 && !ItemVOTVdrive.getProcessed(proc.getInventory().getStackInSlot(0)), "99 ticks: still processing");
        proc.setEnergyStored(2_000L);
        proc.serverTick();
        check(!proc.isProcessing && ItemVOTVdrive.getProcessed(proc.getInventory().getStackInSlot(0)), "processed after 100 ticks");
        check(proc.status.contains("Done!") && proc.getEnergyStored() == 1_800L, "done, 200 HE per tick");
        check(proc.hasDrive, "hasDrive");

        // bereits verarbeitet -> kein Neustart
        proc.receiveControl(start);
        check(!proc.isProcessing, "processed drive not restarted");

        // Klonen ohne Ziel
        CompoundTag clone = new CompoundTag();
        clone.putBoolean("clone", true);
        proc.receiveControl(clone);
        check(proc.status.contains("No target"), "clone without target");

        proc.getInventory().setStackInSlot(1, new ItemStack(SpaceMaterialItems.HARD_DRIVE.get()));
        proc.receiveControl(clone);
        ItemStack copy = proc.getInventory().getStackInSlot(1);
        check(proc.status.contains("Drive cloned"), "clone status");
        check(copy.is(SpaceRocketItems.FULL_DRIVE.get()) && ItemVOTVdrive.getProcessed(copy)
                && ItemVOTVdrive.getBody(copy) == SolarSystem.Body.MOHO, "clone is full drive with same data");
        check(ItemVOTVdrive.wasCopied(copy) && ItemVOTVdrive.wasCopied(proc.getInventory().getStackInSlot(0)), "both marked copied");

        // Prozessor entfernt -> Status zurueckgesetzt
        proc.getInventory().setStackInSlot(2, ItemStack.EMPTY);
        proc.serverTick();
        check(proc.getProcessingTier() == 0, "no processor -> tier 0");

        helper.succeed();
    }
}
