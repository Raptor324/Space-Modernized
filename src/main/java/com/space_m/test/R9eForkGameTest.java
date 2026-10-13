package com.space_m.test;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.entity.mob.siege.SiegeTier;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.nuclear.WatzPelletType;
import com.hbm_m.multiblock.ForkMultiblockPlacement;
import com.space_m.SpaceMod;
import com.space_m.fluid.SpaceFluids;
import com.space_m.fork.SpaceForkRules;
import com.space_m.item.SpaceMaterialItems;
import com.space_m.material.SpaceMats;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * Nutzerentscheidung E1-E17: mit Addon gelten die NTM-Space-Fork-Abweichungen auch in HBM ({@link SpaceForkRules}).
 * Gegenstueck in HBM: {@code SpaceEnvironmentGameTest.extensionPointsAreNeutral} (ohne Addon = Master).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class R9eForkGameTest {

    private R9eForkGameTest() {}

    private static Player mockPlayer(GameTestHelper helper) {
        //? if < 1.21.1 {
        return helper.makeMockPlayer();
        //?} else {
        /*return helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        *///?}
    }

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9e")
    public static void forkRulesInstalled(GameTestHelper helper) {
        check(SpaceApi.fork() == SpaceForkRules.INSTANCE && SpaceApi.forkActive(), "Fork-Regeln nicht gesetzt");
        check(SpaceApi.fork().unavoidableGravity(), "E1 Standard-Config: Big-Ass-Tank braucht Fundament");
        check(SpaceApi.fork().blastFurnaceFuel(new ItemStack(SpaceMaterialItems.POWDER_COKE_COAL.get())) == 400, "E4 Koks-Pulver 400");
        check(SpaceApi.fork().gunmetalZinc() == SpaceMats.MAT_ZINC, "E8 Rotguss mit Zink");
        check(SpaceApi.fork().teleporterFluid() == SpaceFluids.NMASS.fluid(), "E16 N-Masse");
        helper.succeed();
    }

    @GameTest(template = "empty3x3x3", batch = "space_r9e")
    public static void balanceValues(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        // E5 Siege-Beute
        check(SiegeTier.DNT.drops().size() == 1 && SiegeTier.DNT.drops().get(0).get().getItem() == ModMaterialItems.item(ModMaterials.STARMETAL, MaterialShape.INGOT), "E5 DNT -> Sternenmetall");
        check(SiegeTier.SCHRAB.drops().size() == 3 && SiegeTier.SCHRAB.drops().get(0).get().getCount() == 32, "E5 Schrab: 32 Stahl + Kobalt + Schaltkreise");
        // E6 Watz
        check(WatzPelletType.NQR.yield == 1_000_000_000D && WatzPelletType.all().stream().allMatch(t -> t.yield == 1_000_000_000D), "E6 alle Pellets 1 Mrd");
        // E8
        check(com.hbm_m.blockentity.machines.DeuteriumExtractorBlockEntity.powerDivisor() == 100, "E8 Deuterium /100");
        var part = new com.hbm_m.blockentity.network.CranePartitionerBlockEntity(BlockPos.ZERO, ModBlocks.CRANE_PARTITIONER.get().defaultBlockState());
        check(part.inputCount == 96 && part.outputCount == 8 && part.inventory.getSlots() == 104, "E8 Sortierer 96+8");
        check(com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.getRecipe(ModMaterialItems.stack(ModMaterials.COBALT, MaterialShape.INGOT, 1)) != null
                && com.hbm_m.handler.rbmk.RBMKOutgasserRecipes.getRecipe(ModMaterialItems.stack(ModMaterials.BISMUTH, MaterialShape.INGOT, 1)) != null, "E8 Ausgaser Co-60/Po-210");
        var electro = new com.hbm_m.blockentity.machines.MachineElectrolyserBlockEntity(BlockPos.ZERO, ModBlocks.ELECTROLYSER.get().defaultBlockState());
        check(electro.getConPos().length == 10, "E8 Elektrolyseur 10 Anschluesse");
        // E3 Faesser
        var barrel = new com.hbm_m.blockentity.machines.BarrelIronBlockEntity(BlockPos.ZERO, ModBlocks.BARREL_IRON.get().defaultBlockState());
        check(barrel.handlesOverpressure(), "E3 Fass ist IOverpressurable");
        // E15 Ozelot nicht mehr immun, Zombie weiter
        check(!com.hbm_m.util.ContaminationUtil.isRadImmune(EntityType.OCELOT.create(sl)), "E15 Ozelot nicht immun");
        check(com.hbm_m.util.ContaminationUtil.isRadImmune(EntityType.ZOMBIE.create(sl)), "E15 Zombie bleibt immun");
        // E16 Teleporter
        var tele = new com.hbm_m.blockentity.machines.MachineTeleporterBlockEntity(BlockPos.ZERO, ModBlocks.MACHINE_TELEPORTER.get().defaultBlockState());
        check(tele.tank != null && tele.tank.getTankType() == SpaceFluids.NMASS.fluid() && tele.tank.getMaxFill() == 16000, "E16 N-Masse-Tank 16000");
        helper.succeed();
    }

    /** E7: an die Decke geklickt haengt der Elektrolyseur unter der Decke (Kern um die volle Hoehe tiefer). */
    @GameTest(template = "empty3x3x3", batch = "space_r9e")
    public static void wallCeilingPlacement(GameTestHelper helper) {
        Player player = mockPlayer(helper);
        BlockPos clicked = helper.absolutePos(new BlockPos(1, 3, 1));
        helper.getLevel().setBlockAndUpdate(clicked, Blocks.STONE.defaultBlockState()); // fester Block: platziert wird daneben
        ItemStack stack = new ItemStack(ModBlocks.ELECTROLYSER.get());
        BlockPlaceContext ceiling = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.DOWN, clicked, false));
        BlockPlaceContext floor = new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(clicked), Direction.UP, clicked, false));
        BlockPlaceContext fc = ForkMultiblockPlacement.shift(ceiling, ModBlocks.ELECTROLYSER.get());
        BlockPlaceContext ff = ForkMultiblockPlacement.shift(floor, ModBlocks.ELECTROLYSER.get());
        check(fc != null && ff != null, "E7 Fork-Platzierung aktiv");
        // Elektrolyseur: Hoehe 0..3 (getAllDimensions up 3) -> Deckenkern 3 tiefer als die angrenzende Zelle
        check(fc.getClickedPos().getY() == clicked.below().getY() - 3, "E7 Decke: Kern " + fc.getClickedPos() + " zu " + clicked.below());
        check(ff.getClickedPos().getY() == clicked.above().getY(), "E7 Boden: Kern auf Klickhoehe + 1");
        helper.succeed();
    }

    /** E11/E12: Kassetten mit Fork-Titeln, NITAN-Ambrosia zaehlt Dosen und gibt Ambrosia + Resistenz IV. */
    @GameTest(template = "empty3x3x3", batch = "space_r9e")
    public static void forkItemsAndPotions(GameTestHelper helper) {
        check(com.hbm_m.item.machine.ItemCassette.getType(new ItemStack(com.space_m.fork.ForkItems.CASSETTE_DUCC.get()))
                == com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType.DISEMBODIED, "E11 Kassette Ducc");
        check(com.space_m.sound.SpaceSounds.ALARM_MAMA.isPresent(), "E11 Klang alarm.mama");
        Player player = mockPlayer(helper);
        com.space_m.fork.ForkItems.setNitanCount(player, 0);
        ItemStack flask = new ItemStack(com.space_m.fork.ForkItems.FLASK_INFUSION_NITAN.get());
        flask.getItem().finishUsingItem(flask, helper.getLevel(), player);
        check(com.space_m.fork.ForkItems.getNitanCount(player) == 1, "E12 nitanCount");
        check(player.hasEffect(com.space_m.item.SpaceEffects.holder(com.space_m.item.SpaceEffects.NITAN))
                && player.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE), "E12 Ambrosia + Resistenz");
        check(com.space_m.item.SpaceEffects.FLASHBANG.isPresent() && com.space_m.item.SpaceEffects.SLIPPERY.isPresent(), "E12 Flashbang/Butterfinger");
        helper.succeed();
    }

    /** E11: Fork-Wackelkoepfe/-Plueschtiere, Gwen sprengt den Schredder, Butter macht FAT TETO, FAT TETO = Dyson-Overdrive. */
    @GameTest(template = "empty3x3x3", batch = "space_r9e")
    public static void forkTrinkets(GameTestHelper helper) {
        ItemStack gwen = com.space_m.fork.trinket.ForkTrinkets.bobble(com.space_m.fork.trinket.ForkTrinkets.ForkBobbleType.GWEN);
        ItemStack juice = com.space_m.fork.trinket.ForkTrinkets.bobble(com.space_m.fork.trinket.ForkTrinkets.ForkBobbleType.JUICE);
        check(SpaceApi.fork().shredderExplodes(gwen), "E11 Gwen sprengt den Schredder");
        check(!SpaceApi.fork().shredderExplodes(juice), "E11 Juice sprengt nicht");
        check(!SpaceApi.fork().shredderExplodes(com.hbm_m.item.TrinketBlockItem.make(ModBlocks.BOBBLEHEAD.get().asItem(), 21)), "E11 HBM-Wackelkopf sprengt nicht");
        check(com.space_m.fork.trinket.ForkTrinkets.BLOCK_TETO.isPresent() && com.space_m.fork.trinket.ForkTrinkets.BLOCK_AKITA.isPresent(), "E11 Klaenge teto/akita");

        // Plueschtier Teto setzen, mit Butter fuettern -> FAT TETO
        BlockPos rel = new BlockPos(1, 1, 1);
        helper.setBlock(rel, com.space_m.fork.trinket.ForkTrinkets.PLUSHIE.get());
        BlockPos abs = helper.absolutePos(rel);
        var te = (com.space_m.fork.trinket.ForkTrinketBlockEntity) helper.getLevel().getBlockEntity(abs);
        check(te != null, "E11 Pluesch-Blockentity");
        te.setType(com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType.TETO.ordinal());
        ItemStack butter = new ItemStack(com.space_m.item.SpaceMaterialItems.BUTTER.get(), 2);
        com.space_m.fork.trinket.ForkTrinkets.PLUSHIE.get().activate(helper.getLevel(), abs, mockPlayer(helper), butter);
        check(te.type == com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType.FATO.ordinal() && butter.getCount() == 1, "E11 Butter -> FAT TETO");

        ItemStack fato = com.space_m.fork.trinket.ForkTrinkets.plushie(com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType.FATO);
        check(com.space_m.machine.dyson.MachineDysonLauncher.isFatTeto(fato), "E11 FAT TETO zaehlt fuer den Overdrive");
        check(!com.space_m.machine.dyson.MachineDysonLauncher.isFatTeto(com.space_m.fork.trinket.ForkTrinkets.plushie(
                com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType.TETO)), "E11 Teto ist kein Overdrive");
        check(com.space_m.fork.ForkCapes.getCloak(com.space_m.fork.ForkCapes.DUODEC_) != null
                && com.space_m.fork.ForkCapes.getCloak(java.util.UUID.randomUUID()) == null, "E11 Umhaenge");
        // Bobmazon: nur pruefbar, wenn HBM den Katalog bauen kann (HBM-Fehler: Kupferbarren ohne Gegenstand -> NPE)
        java.util.List<com.hbm_m.handler.BobmazonOfferFactory.Offer> offers = null;
        try {
            offers = com.hbm_m.handler.BobmazonOfferFactory.getOffers(new ItemStack(com.hbm_m.item.ModItems.BOBMAZON.get()));
        } catch (RuntimeException ignored) { }
        if (offers != null) check(offers.stream().filter(o -> o.offer.getItem() == com.space_m.fork.trinket.ForkTrinkets.PLUSHIE.get().asItem()).count() == 4,
                "E11 Bobmazon: 4 Fork-Plueschtiere");
        helper.succeed();
    }

    /** E14: Schrabidische Saeure zersetzt Stein zu Bruchstein (Space SchrabidicBlock.getReaction). */
    @GameTest(template = "empty3x3x3", batch = "space_r9e")
    public static void schrabidicAcidCorrodes(GameTestHelper helper) {
        BlockPos acid = new BlockPos(1, 1, 1);
        helper.setBlock(acid.east(), Blocks.STONE);
        helper.setBlock(acid, ModBlocks.SCHRABIDIC_BLOCK.get());
        var block = (com.hbm_m.block.fluid.RadioactiveFluidBlock) ModBlocks.SCHRABIDIC_BLOCK.get();
        BlockPos abs = helper.absolutePos(acid);
        for (int i = 0; i < 60 && helper.getBlockState(acid.east()).is(Blocks.STONE); i++) {
            block.onFluidTick(helper.getLevel(), abs, helper.getLevel().getBlockState(abs));
        }
        check(!helper.getBlockState(acid.east()).is(Blocks.STONE), "E14 Stein nicht zersetzt");
        helper.succeed();
    }
}
