package com.space_m.resources;

import java.util.EnumMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.nature.OreBedrockBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.space_m.SpaceMod;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * {@code space_m:celestial_bedrock_ore} = 1:1 NTM Space {@code WorldGeneratorCelestial.generateBedrockOres} +
 * {@code BedrockOre.generateAuto/generate}: in jedem 10. Chunk einer Space-Dimension eine Bedrock-Erzader
 * ({@code hbm_m:ore_bedrock_mineral} mit Rohem Bedrock-Erz) aus der Koerper-Tabelle {@link CelestialBedrockOre};
 * Stufe/Bohrfluessigkeit aus der Durchschnittsdichte, ausser Dres (Salpetersaeure 500 mB) und Eve
 * (Kaliumpermanganat 500 mB) = Original {@code WorldProvider<Koerper>.getBedrockAcid}. Sonst auf Koerpern mit
 * Bedrock-Eis ({@code hasIce}, Tekto immer) in jedem 3. uebrigen Chunk eine Eis-Ader (32 Packeis, Stufe 1).
 * Darueber Tiefengestein, das das Planetengestein ({@code WorldProvider<Koerper>.getStone}) bzw. beim Eis Stein ersetzt.
 *
 * <p>Abweichung (technisch): HBMs Bedrock-Erz-Blockentity hat kein Farb-/Formfeld (Original {@code color}/{@code shape}).
 */
public class CelestialBedrockOreFeature extends Feature<NoneFeatureConfiguration> {

    /** Original {@code getStone()} je Koerper (Thatmo: Rueckfall-Koerper Kerbin, Stein = sellafield_slaked). */
    private static final Map<SolarSystem.Body, ResourceLocation> STONE = new EnumMap<>(SolarSystem.Body.class);

    static {
        STONE.put(SolarSystem.Body.MUN, SpaceMod.id("moon_rock"));
        STONE.put(SolarSystem.Body.MINMUS, SpaceMod.id("minmus_stone"));
        STONE.put(SolarSystem.Body.DUNA, SpaceMod.id("duna_rock"));
        STONE.put(SolarSystem.Body.IKE, SpaceMod.id("ike_stone"));
        STONE.put(SolarSystem.Body.EVE, SpaceMod.id("eve_rock"));
        STONE.put(SolarSystem.Body.DRES, SpaceMod.id("dres_rock"));
        STONE.put(SolarSystem.Body.MOHO, SpaceMod.id("moho_stone"));
        STONE.put(SolarSystem.Body.LAYTHE, ResourceLocation.fromNamespaceAndPath("minecraft", "stone"));
        STONE.put(SolarSystem.Body.TEKTO, ResourceLocation.fromNamespaceAndPath("hbm_m", "basalt"));
    }

    /** Bohrfluessigkeit (Original {@code FluidStack}); {@code fluid == EMPTY} = keine. */
    public record Acid(Fluid fluid, int amount) {
        static final Acid NONE = new Acid(Fluids.EMPTY, 0);
    }

    public CelestialBedrockOreFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        ServerLevel server = level.getLevel();
        CelestialBody celestial = CelestialBody.getBodyOrNull(server.dimension());
        if (celestial == null || "kerbin".equals(celestial.name)) return false; // Original: dimensionId 0 -> return
        if (CelestialBody.inOrbit(server)) return false; // WorldProviderOrbit ist kein WorldProviderCelestial

        RandomSource rand = ctx.random();
        int x = ctx.origin().getX() & ~15;
        int z = ctx.origin().getZ() & ~15;

        SolarSystem.Body body = CelestialBedrockOre.bodyOf(server);
        Block planetStone = planetStone(celestial);

        if (rand.nextInt(10) == 0) {
            int randPosX = x + rand.nextInt(2) + 8;
            int randPosZ = z + rand.nextInt(2) + 8;

            generateAuto(level, server, rand, randPosX, randPosZ, body, planetStone, bedrockAcid(celestial));
            return true;
        } else if (hasIce(celestial) && rand.nextInt(3) == 0) {
            int randPosX = x + rand.nextInt(2) + 8;
            int randPosZ = z + rand.nextInt(2) + 8;

            generate(level, rand, randPosX, randPosZ, new ItemStack(Items.PACKED_ICE, 8 * 4), Acid.NONE, 1, Blocks.STONE);
            return true;
        }
        return false;
    }

    /** Original {@code WorldProvider<Koerper>.getBedrockAcid()}: Dres Salpetersaeure, Eve KMnO4, sonst null. */
    @Nullable
    public static Acid bedrockAcid(CelestialBody body) {
        if ("dres".equals(body.name)) return new Acid(ModFluids.NITRIC_ACID.getSource(), 500);
        if ("eve".equals(body.name)) {
            Fluid kmno4 = BuiltInRegistries.FLUID.get(SpaceMod.id("kmno4"));
            return new Acid(kmno4 != null ? kmno4 : Fluids.EMPTY, 500);
        }
        return null;
    }

    /** Original {@code WorldProviderCelestial.hasIce()} ({@code body.hasIce}), Tekto ueberschreibt mit true. */
    public static boolean hasIce(CelestialBody body) {
        return body.hasIce || "tekto".equals(body.name);
    }

    public static Block planetStone(CelestialBody celestial) {
        if ("thatmo".equals(celestial.name)) return block(ResourceLocation.fromNamespaceAndPath("hbm_m", "sellafield_slaked"));
        ResourceLocation id = celestial.getEnum() != null ? STONE.get(celestial.getEnum()) : null;
        return id != null ? block(id) : Blocks.STONE;
    }

    private static Block block(ResourceLocation id) {
        Block b = BuiltInRegistries.BLOCK.get(id);
        return b == Blocks.AIR ? Blocks.STONE : b;
    }

    /** Original {@code BedrockOre.getBoreFluid}. */
    public static Acid getBoreFluid(double density) {
        if (density > 1.5) return new Acid(ModFluids.SOLVENT.getSource(), 2_000);
        if (density > 1) return new Acid(ModFluids.SULFURIC_ACID.getSource(), 1_000);
        if (density > 0.75) return new Acid(ModFluids.WATER.getSource(), 1_000);
        return Acid.NONE;
    }

    /** Original {@code BedrockOre.getTier}. */
    public static int getTier(double density) {
        if (density > 1.5) return 4;
        if (density > 1) return 3;
        if (density > 0.75) return 2;
        return 1;
    }

    /** Original {@code BedrockOre.generateAuto(world, x, z, targetBlock, acidOverride)}. */
    public static void generateAuto(WorldGenLevel level, ServerLevel server, RandomSource rand, int x, int z, SolarSystem.Body body, Block targetBlock, @Nullable Acid acidOverride) {
        CelestialBedrockOre ore = CelestialBedrockOre.get(body);
        double totalLevel = 0;
        for (CelestialBedrockOre.Type type : ore.types) {
            totalLevel += CelestialBedrockOre.getOreLevel(server, x, z, type);
        }

        totalLevel /= ore.types.length;
        Acid acid = acidOverride != null ? acidOverride : getBoreFluid(totalLevel);
        int tier = getTier(totalLevel);

        generate(level, rand, x, z, new ItemStack(ModItems.BEDROCK_ORE_BASE.get()), acid, tier, targetBlock);
    }

    /** Original {@code BedrockOre.generate(world, x, z, stack, acid, color, tier, depthRock, targetBlock)}. */
    public static void generate(WorldGenLevel level, RandomSource rand, int x, int z, ItemStack stack, Acid acid, int tier, Block targetBlock) {
        int baseY = level.getMinBuildHeight();
        BlockState oreState = ModBlocks.ORE_BEDROCK.get().defaultBlockState();

        for (int ix = x - 1; ix <= x + 1; ix++) {
            for (int iz = z - 1; iz <= z + 1; iz++) {
                BlockPos pos = new BlockPos(ix, baseY, iz);
                if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                    if ((ix == x && iz == z) || rand.nextBoolean()) {
                        level.setBlock(pos, oreState, 2);
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof OreBedrockBlockEntity ore) {
                            ore.resource = stack.copy();
                            ore.acidType = acid.fluid();
                            ore.acidAmountMb = acid.amount();
                            ore.tier = tier;
                            ore.setChanged();
                        }
                    }
                }
            }
        }

        BlockState depthRock = ModBlocks.STONE_DEPTH.get().defaultBlockState();
        for (int ix = x - 3; ix <= x + 3; ix++) {
            for (int iz = z - 3; iz <= z + 3; iz++) {
                for (int iy = baseY + 1; iy < baseY + 7; iy++) {
                    BlockPos pos = new BlockPos(ix, iy, iz);
                    BlockState b = level.getBlockState(pos);
                    if (iy < baseY + 3 || b.is(Blocks.BEDROCK)) {
                        if (b.is(targetBlock) || b.is(Blocks.BEDROCK)) {
                            level.setBlock(pos, depthRock, 2);
                        }
                    }
                }
            }
        }
    }
}
