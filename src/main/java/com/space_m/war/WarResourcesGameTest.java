package com.space_m.war;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.api.space.SpaceResources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityUFO;
import com.hbm_m.entity.mob.siege.EntitySiegeCraft;
import com.hbm_m.entity.mob.siege.SiegeTier;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.trait.CBT_Invasion;
import com.space_m.dim.PlanetOres;
import com.space_m.resources.BlockOreFluid;
import com.space_m.resources.CelestialBedrockOre;
import com.space_m.resources.CelestialBedrockOreFeature;
import com.space_m.resources.OreLocations;
import com.space_m.resources.SpaceResourceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/** R8-B: Krieg/Invasion/Nuke-Shocks und Planeten-Ressourcen (Batch {@code space_war}). */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WarResourcesGameTest {

    private WarResourcesGameTest() {}

    private static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }

    /** CelestialNukeShockHandler.trigger: Eintrag am Zielkoerper, Staerke radius/140 geklemmt, Mitte im 1/16-Raster. */
    @GameTest(template = "empty3x3x3", batch = "space_war", timeoutTicks = 20)
    public static void nukeShockIsRecorded(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        CelestialBody body = CelestialBody.getTarget(level, 0, 0).body;
        int before = CelestialNukeShockHandler.getServerShocks(body).size();
        BlockPos p = helper.absolutePos(BlockPos.ZERO);
        CelestialNukeShockHandler.trigger(level, p.getX(), p.getZ(), 70F);
        List<CelestialNukeShockHandler.ShockStatus> shocks = CelestialNukeShockHandler.getServerShocks(body);
        check(!shocks.isEmpty() && shocks.size() == Math.min(before + 1, CelestialNukeShockHandler.MAX_ACTIVE_SHOCKS), "shock added");
        CelestialNukeShockHandler.ShockStatus s = shocks.get(shocks.size() - 1);
        check(Math.abs(s.strength() - 0.5F) < 1e-4, "strength 70/140 = 0.5, got " + s.strength());
        check(Math.abs(s.centerX() * 16 - 0.5F - Math.floor(s.centerX() * 16)) < 1e-4, "center quantized");
        for (int i = 0; i < 5; i++) CelestialNukeShockHandler.trigger(level, p.getX(), p.getZ(), 1000F);
        check(CelestialNukeShockHandler.getServerShocks(body).size() == CelestialNukeShockHandler.MAX_ACTIVE_SHOCKS, "max 4 shocks");
        check(CelestialNukeShockHandler.getServerShocks(body).get(3).strength() == 1.0F, "strength clamped to 1");
        helper.succeed();
    }

    /** CBT_Invasion-Spawner: Siege-UFO/-Craft erscheinen, Abschusswerte 1/2/10, Boss = EntityUFO. */
    @GameTest(template = "empty3x3x3", batch = "space_war", timeoutTicks = 40)
    public static void invasionSpawnerAndKillValues(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        check(CBT_Invasion.spawner instanceof InvasionSpawner, "spawner installed");
        Vec3 at = helper.absoluteVec(new Vec3(1.5, 2, 1.5));
        int before = level.getEntities(WarEntities.SIEGE_UFO.get(), e -> true).size();
        check(CBT_Invasion.spawner.spawnSiege(new CBT_Invasion(), level, at.x, at.y, at.z, false, 0.1F), "siege ufo spawned");
        check(level.getEntities(WarEntities.SIEGE_UFO.get(), e -> true).size() == before + 1, "siege ufo in level");

        EntitySiegeUFO clay = new EntitySiegeUFO(WarEntities.SIEGE_UFO.get(), level);
        clay.setTier(SiegeTier.CLAY);
        EntitySiegeUFO gold = new EntitySiegeUFO(WarEntities.SIEGE_UFO.get(), level);
        gold.setTier(SiegeTier.GOLD);
        check(gold.getMaxHealth() == 25F, "gold ufo health 100*0.25");
        check(CBT_Invasion.spawner.killValue(clay) == 1 && CBT_Invasion.spawner.killValue(gold) == 2, "ufo kill values 1/2");
        check(CBT_Invasion.spawner.killValue(new EntitySiegeCraft(ModEntities.SIEGE_CRAFT.get(), level)) == 10, "siege craft 10");
        check(CBT_Invasion.spawner.isBoss(new EntityUFO(ModEntities.UFO.get(), level)), "boss = EntityUFO");

        for (EntitySiegeUFO e : level.getEntities(WarEntities.SIEGE_UFO.get(), e -> true)) e.discard();
        helper.succeed();
    }

    /** Thatmo-Bomber: statFacCarpetJet (Typ 9, 150 Ticks, 1,5-faches Tempo). */
    @GameTest(template = "empty3x3x3", batch = "space_war", timeoutTicks = 20)
    public static void carpetJetFactory(GameTestHelper helper) {
        BlockPos p = helper.absolutePos(BlockPos.ZERO);
        EntityCarpetJet jet = EntityCarpetJet.statFacCarpetJet(helper.getLevel(), p.getX(), p.getY(), p.getZ());
        double speed = jet.getDeltaMovement().horizontalDistance();
        boolean shortMode = com.hbm_m.config.ModClothConfig.get().enableBomberShortMode;
        check(Math.abs(speed - (shortMode ? 1.5 : 3.0)) < 1e-6, "carpet jet speed 1.5 * mult, got " + speed);
        check(jet.timer == 150 && Math.abs(jet.getY() - (p.getY() + 50)) < 1e-6, "timer 150, y+50");
        jet.discard();
        helper.succeed();
    }

    /** Erz-Tooltip: Eisen ueberall, Nickel ausser Kerbin, Kohle nur Kerbin, Sole auf Mun/Minmus/Ike. */
    @GameTest(template = "empty3x3x3", batch = "space_war", timeoutTicks = 20)
    public static void oreLocationTooltips(GameTestHelper helper) {
        check(OreLocations.spawnsOn("ore_iron").size() == SolarSystem.Body.values().length, "iron everywhere");
        check(OreLocations.spawnsOn("ore_nickel").size() == SolarSystem.Body.values().length - 1, "nickel all but kerbin");

        List<Component> lines = new ArrayList<>();
        OreLocations.addTooltip(Items.IRON_ORE, lines);
        check(lines.size() == 1 && lines.get(0).getString().equals("Can be found anywhere"), "vanilla iron: anywhere");

        lines.clear();
        OreLocations.addTooltip(Items.COAL_ORE, lines);
        check(lines.size() == 2 && lines.get(1).getString().contains(" - "), "coal: kerbin only");

        PlanetOres.Variant brine = PlanetOres.get("ore_brine", "mun");
        check(brine != null, "ore_brine_mun exists");
        lines.clear();
        OreLocations.addTooltip(brine.block().get().asItem(), lines);
        check(lines.size() == 4 && lines.get(0).getString().equals("Can be found on:"), "brine on 3 bodies, got " + lines.size());
        helper.succeed();
    }

    /** Bedrock-Erztabelle je Koerper, Anmeldung bei HBM, Bohrsaeuren Dres/Eve. */
    @GameTest(template = "empty3x3x3", batch = "space_war", timeoutTicks = 20)
    public static void celestialBedrockOres(GameTestHelper helper) {
        SpaceResourceRegistry.setup();
        check(CelestialBedrockOre.getAllTypes().size() == 54, "54 bedrock types, got " + CelestialBedrockOre.getAllTypes().size());
        check(CelestialBedrockOre.get(SolarSystem.Body.DRES).types.length == 6, "dres 6 types");
        check(CelestialBedrockOre.get(SolarSystem.Body.IKE).types.length == 4, "ike 4 types");
        check(SpaceResources.bedrockOres(SpaceMod.id("dres")).size() == 6, "dres registered at HBM");
        check(SpaceResources.bedrockOres(com.hbm_m.api.space.SpaceEnvironment.EARTH).size() == 4, "kerbin registered as earth");

        CelestialBedrockOreFeature.Acid dres = CelestialBedrockOreFeature.bedrockAcid(CelestialBody.getBody("dres"));
        CelestialBedrockOreFeature.Acid eve = CelestialBedrockOreFeature.bedrockAcid(CelestialBody.getBody("eve"));
        check(dres != null && dres.amount() == 500 && dres.fluid() == com.hbm_m.inventory.fluid.ModFluids.NITRIC_ACID.getSource(), "dres nitric 500");
        check(eve != null && eve.amount() == 500 && net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(eve.fluid()).equals(SpaceMod.id("kmno4")), "eve kmno4 500");
        check(CelestialBedrockOreFeature.bedrockAcid(CelestialBody.getBody("mun")) == null, "mun no override");

        double level = CelestialBedrockOre.getOreLevel(12345L, 413015, 100, 200, CelestialBedrockOre.get(SolarSystem.Body.MUN).types[0]);
        check(level >= 0 && level <= 2, "ore level in [0,2]");
        helper.succeed();
    }

    /** BlockOreFluid: Lagerstaette steigt ueber leeren Block, Anmeldung bei HBM, Foerderfluide je Koerper. */
    @GameTest(template = "empty3x3x3", batch = "space_war", timeoutTicks = 20)
    public static void fluidDeposits(GameTestHelper helper) {
        SpaceResourceRegistry.setup();
        Block full = com.space_m.dim.duna.DunaBlocks.ORE_OIL.get();
        Block empty = com.space_m.dim.duna.DunaBlocks.ORE_OIL_EMPTY.get();
        check(full instanceof BlockOreFluid, "ore_oil_duna is BlockOreFluid");
        check(SpaceResources.depositOf(full) != null && SpaceResources.fullBlockOf(empty) == full, "deposit registered");
        check(ResourceLocation.fromNamespaceAndPath("hbm_m", "oil_ds").equals(SpaceResources.depositPrimaryFluid(SpaceMod.id("laythe"), SpaceResources.ReserveType.OIL)), "laythe oil_ds");
        check(SpaceMod.id("hgas").equals(SpaceResources.depositSecondaryFluid(SpaceMod.id("tekto"), SpaceResources.ReserveType.OIL)), "tekto hgas");
        check(((BlockOreFluid) com.space_m.dim.tekto.TektoBlocks.ORE_BEDROCK_OIL.get()).requiresFracking(), "bedrock oil needs fracking");

        BlockPos top = new BlockPos(1, 2, 1);
        helper.setBlock(top, full);
        helper.setBlock(top.below(), empty);
        helper.assertBlockPresent(empty, top);
        helper.assertBlockPresent(full, top.below());
        helper.succeed();
    }
}
