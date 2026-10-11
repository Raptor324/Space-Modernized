package com.space_m.machine.atmo;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.space_m.SpaceMod;
import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.AtmosphereReaction;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R2-C: Atmo-Maschinen gegen das Original (Strukturen aus {@code getDimensions/makeExtra}, Tower-/Kompressor-/Emitter-
 * Logik aus den TileEntities, Atmosphaeren-Chemie, Trait "terraformer"). Logik-Tests laufen auf frei erzeugten BEs
 * (die Tuerme passen nicht in die 3x3x3-Vorlage); der Emitter wird zusaetzlich echt platziert.
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AtmoMachinesGameTest {

    private AtmoMachinesGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    private static int cells(DummyableMachineBlock block) {
        return block.getStructureHelper().getPartOffsets().size() + 1;
    }

    private static <T extends com.hbm_m.blockentity.BaseHbmBlockEntity> T attach(GameTestHelper helper, T be) {
        be.setLevel(helper.getLevel());
        return be;
    }

    /** Registrierung, Strukturgroessen (2x2x7, 2x2x4, 2x2x3), Anschlussfelder, Chemie, Trait. */
    @GameTest(template = "empty3x3x3", batch = "space_atmo_machines", timeoutTicks = 20)
    public static void registrationAndStructures(GameTestHelper helper) {
        check(cells(SpaceBlocks.MACHINE_ATMO_TOWER.get()) == 28, "Turm 2x2x7, ist " + cells(SpaceBlocks.MACHINE_ATMO_TOWER.get()));
        check(cells(SpaceBlocks.MACHINE_ATMO_VENT.get()) == 16, "Kompressor 2x2x4, ist " + cells(SpaceBlocks.MACHINE_ATMO_VENT.get()));
        check(cells(SpaceBlocks.MACHINE_ATMO_EMITTER.get()) == 12, "Emitter 2x2x3, ist " + cells(SpaceBlocks.MACHINE_ATMO_EMITTER.get()));
        check(SpaceBlockEntities.ATMOSPHERE_EDITOR.get() != null, "Editor-BE");

        // getConPos: acht Felder rund um den 2x2-Sockel, keines im Sockel
        BlockState tower = SpaceBlocks.MACHINE_ATMO_TOWER.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.EAST);
        var base = AtmoConnections.base(BlockPos.ZERO, tower);
        var con = AtmoConnections.conPos(BlockPos.ZERO, tower);
        check(base.size() == 4, "Sockel 4 Felder, ist " + base.size());
        check(con.size() == 8 && con.stream().noneMatch(d -> base.contains(d.pos())), "8 Anschlussfelder, ist " + con.size());

        check(AtmosphereReaction.REACTIONS.contains(AtmosphereRecipes.LITHIUM_CARBONATE), "Atmosphaeren-Chemie eingetragen");
        check(AtmosphereRecipes.LITHIUM_CARBONATE.inputs().size() == 2
                && AtmosphereRecipes.LITHIUM_CARBONATE.output().fill() == 32_000, "Reaktion 1:1");
        check(FluidTrait.traitNameMap.get("terraformer") == FT_Terraformer.class, "Trait terraformer");
        check(new FT_Terraformer("MUN").getTargetBlock() == net.minecraft.world.level.block.Blocks.STONE, "Terraformer-Standard Stein");
        helper.succeed();
    }

    /** Emitter im Spiel: 12 Zellen (Kern + 11 Teile), Abriss raeumt alles weg. */
    @GameTest(template = "empty3x3x3", batch = "space_atmo_machines", timeoutTicks = 40)
    public static void emitterPlacement(GameTestHelper helper) {
        BlockPos core = new BlockPos(1, 1, 1);
        // FACING NORTH: Sockel nach Sueden (-dir) und Osten (rot) -> passt in die Vorlage
        helper.setBlock(core, SpaceBlocks.MACHINE_ATMO_EMITTER.get().defaultBlockState().setValue(DummyableMachineBlock.FACING, Direction.NORTH));
        helper.runAfterDelay(2, () -> {
            BlockPos abs = helper.absolutePos(core);
            ServerLevel level = helper.getLevel();
            int parts = 0;
            for (BlockPos p : BlockPos.betweenClosed(abs.offset(-1, 0, -1), abs.offset(1, 2, 1))) {
                if (level.getBlockState(p).is(com.hbm_m.block.ModBlocks.UNIVERSAL_MACHINE_PART.get())) parts++;
            }
            check(level.getBlockEntity(abs) instanceof AtmosphericEmitterBlockEntity, "Emitter-BE am Kern");
            check(parts == 11, "11 Teile, ist " + parts);
            helper.destroyBlock(core);
            helper.runAfterDelay(2, () -> {
                int left = 0;
                for (BlockPos p : BlockPos.betweenClosed(abs.offset(-1, 0, -1), abs.offset(1, 2, 1))) {
                    if (!level.getBlockState(p).isAir()) left++;
                }
                check(left == 0, "Abriss: " + left + " Bloecke uebrig");
                helper.succeed();
            });
        });
    }

    /** Emitter: Haelfte je Tick, 10 HE/mB, Gas geht in die Koerper-Atmosphaere. */
    @GameTest(template = "empty3x3x3", batch = "space_atmo_machines", timeoutTicks = 20)
    public static void emitterLogic(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = SpaceBlocks.MACHINE_ATMO_EMITTER.get().defaultBlockState();
        AtmosphericEmitterBlockEntity be = attach(helper, new AtmosphericEmitterBlockEntity(pos, state));

        var air = ModFluids.AIR.getSource();
        check(AtmoFluids.isGas(air), "hbm_m:air ist Gas");
        be.tank.setTankType(air);
        be.tank.setFill(1000);
        be.setEnergyStored(100_000);

        CBT_Atmosphere before = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        double p0 = before != null ? before.getPressure(CelestialFluids.EARTHAIR) : 0;

        AtmosphericEmitterBlockEntity.tick(level, pos, state, be);
        check(be.tank.getFill() == 500, "toSpill = fill/2 -> 500, ist " + be.tank.getFill());
        check(be.getEnergyStored() == 100_000 - 5_000, "Energie -10/mB, ist " + be.getEnergyStored());

        CBT_Atmosphere after = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        double p1 = after != null ? after.getPressure(CelestialFluids.EARTHAIR) : 0;
        check(p1 > p0, "Luft in die Atmosphaere abgegeben (" + p0 + " -> " + p1 + ")");

        // zu wenig Strom: nichts passiert
        be.setEnergyStored(10);
        AtmosphericEmitterBlockEntity.tick(level, pos, state, be);
        check(be.tank.getFill() == 500, "ohne Strom kein Ausstoss");

        // Rueckbau des Testgases
        CelestialBody.captureGas(level, CelestialFluids.EARTHAIR, 500);
        helper.succeed();
    }

    /** Kompressor auf Kerbin: Hauptgas Luft, +200 mB und -maxPower/50 je Tick, im Orbit/Vakuum NONE. */
    @GameTest(template = "empty3x3x3", batch = "space_atmo_machines", timeoutTicks = 20)
    public static void compressorLogic(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = SpaceBlocks.MACHINE_ATMO_VENT.get().defaultBlockState();
        AtmosphericCompressorBlockEntity be = attach(helper, new AtmosphericCompressorBlockEntity(pos, state));

        CBT_Atmosphere atmosphere = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        check(atmosphere != null && atmosphere.getPressure() > 0, "Kerbin hat Atmosphaere");

        be.setEnergyStored(AtmosphericCompressorBlockEntity.MAX_POWER);
        AtmosphericCompressorBlockEntity.tick(level, pos, state, be);

        var main = AtmoFluids.fluid(atmosphere.getMainFluid());
        check(be.tank.getTankType() == main, "Tank = Hauptgas " + atmosphere.getMainFluid() + ", ist " + AtmoFluids.id(be.tank.getTankType()));
        if (!AtmoFluids.isNone(main)) {
            check(be.tank.getFill() == 200, "+200 mB, ist " + be.tank.getFill());
            check(be.getEnergyStored() == AtmosphericCompressorBlockEntity.MAX_POWER - 20_000, "-maxPower/50, ist " + be.getEnergyStored());
            CelestialBody.releaseGas(level, atmosphere.getMainFluid(), 200); // Rueckbau
        }

        // Fluid-Identifikator: Gas ausserhalb der Atmosphaere wird abgelehnt
        check(!be.switchGas(ModFluids.WATER.getSource()), "Wasser ist kein Atmosphaerengas");
        helper.succeed();
    }

    /** Turm: 50:1, Energie -maxPower/100; ohne registrierten Stickstoff (vor R4) bleibt er stehen. */
    @GameTest(template = "empty3x3x3", batch = "space_atmo_machines", timeoutTicks = 20)
    public static void towerLogic(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockState state = SpaceBlocks.MACHINE_ATMO_TOWER.get().defaultBlockState();
        AtmoTowerBlockEntity be = attach(helper, new AtmoTowerBlockEntity(pos, state));

        be.tanks[0].setFill(10_000);
        be.setEnergyStored(AtmoTowerBlockEntity.MAX_POWER);
        AtmoTowerBlockEntity.tick(level, pos, state, be);

        if (AtmoFluids.isNone(be.tanks[1].getTankType())) {
            check(be.tanks[0].getFill() == 10_000 && be.getEnergyStored() == AtmoTowerBlockEntity.MAX_POWER, "ohne Stickstoff kein Betrieb");
        } else {
            // convert = min(5000, 10000)/50 = 100
            check(be.tanks[0].getFill() == 10_000 - 5_000, "Luft -convert*50, ist " + be.tanks[0].getFill());
            check(be.tanks[1].getFill() == 100, "Stickstoff +100, ist " + be.tanks[1].getFill());
            check(be.getEnergyStored() == AtmoTowerBlockEntity.MAX_POWER - 10_000, "Energie -maxPower/100");
        }
        helper.succeed();
    }

    /** Editor: Werkzeuge schalten, Abgabe 10^f mB alle 5 Ticks in die Atmosphaere. */
    @GameTest(template = "empty3x3x3", batch = "space_atmo_machines", timeoutTicks = 40)
    public static void editorLogic(GameTestHelper helper) {
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, SpaceBlocks.ATMOSPHERE_EDITOR.get().defaultBlockState());
        ServerLevel level = helper.getLevel();
        BlockPos pos = helper.absolutePos(rel);
        check(level.getBlockEntity(pos) instanceof AtmosphereEditorBlockEntity, "Editor-BE");
        AtmosphereEditorBlockEntity be = (AtmosphereEditorBlockEntity) level.getBlockEntity(pos);
        check(!be.isOn() && be.getThroughputFactor() == 10 && be.isEmitting() && be.getFluid() == ModFluids.AIR.getSource(), "Standardwerte");

        //? if < 1.21.1 {
        var player = helper.makeMockPlayer();
        //?} else {
        /*var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
        var block = SpaceBlocks.ATMOSPHERE_EDITOR.get();
        block.onScrew(level, player, pos, Direction.UP, 0, 0, 0, net.minecraft.world.InteractionHand.MAIN_HAND, com.hbm_m.api.block.IToolable.ToolType.HAND_DRILL);
        check(be.getThroughputFactor() == 11, "Handbohrer +1");
        be.throughputFactor = 9; // 1e9 mB = 0,001 atm
        block.onScrew(level, player, pos, Direction.UP, 0, 0, 0, net.minecraft.world.InteractionHand.MAIN_HAND, com.hbm_m.api.block.IToolable.ToolType.SCREWDRIVER);
        check(be.isOn(), "Schraubenzieher an");

        CBT_Atmosphere before = CelestialBody.getTrait(level, CBT_Atmosphere.class);
        double p0 = before != null ? before.getPressure(CelestialFluids.EARTHAIR) : 0;

        helper.runAfterDelay(6, () -> {
            CBT_Atmosphere after = CelestialBody.getTrait(level, CBT_Atmosphere.class);
            double p1 = after != null ? after.getPressure(CelestialFluids.EARTHAIR) : 0;
            check(p1 > p0, "Editor gibt Luft ab (" + p0 + " -> " + p1 + ")");
            // Aufnahme-Modus und aus: Rueckbau
            block.onScrew(level, player, pos, Direction.UP, 0, 0, 0, net.minecraft.world.InteractionHand.MAIN_HAND, com.hbm_m.api.block.IToolable.ToolType.DEFUSER);
            check(!be.isEmitting(), "Entschaerfer: Aufnahme");
            block.onScrew(level, player, pos, Direction.UP, 0, 0, 0, net.minecraft.world.InteractionHand.MAIN_HAND, com.hbm_m.api.block.IToolable.ToolType.SCREWDRIVER);
            CelestialBody.captureGas(level, CelestialFluids.EARTHAIR, (p1 - p0) * com.space_m.celestial.AstronomyUtil.MB_PER_ATM);
            check(FluidType.forFluid(be.getFluid()) != null, "Fluidtyp");
            helper.succeed();
        });
    }
}
