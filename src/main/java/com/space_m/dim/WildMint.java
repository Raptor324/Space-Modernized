package com.space_m.dim;

import com.space_m.block.SpaceBlocks;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * R9-B: Wilde Minze = NTM Space {@code plant_flower} Metadatum MINT ({@code BlockNTMFlower.EnumFlowerType.MINT},
 * Name "Wild Mint", Textur {@code plant_flower.mint}). Beute: {@code mint_leaves} (Original {@code getItemDropped} +
 * {@code getDropsWithoutDamage}). Erzeugung (Original {@code HbmWorldGen}: Ebenen-Biome, je Chunk 1/32,
 * {@code generateFlowers} = Vanilla-Blumenflecken 64 Versuche) als placed feature {@code space_m:wild_mint} +
 * Biom-Modifikator (Datapack). Die Schwester "Wild Strawberries" ist HBMs {@code strawberry_bush}.
 */
public final class WildMint {

    private WildMint() {}

    public static final RegistrySupplier<Block> PLANT_FLOWER_MINT = SpaceBlocks.registerWithItem("plant_flower_mint",
            () -> new FlowerBlock(MobEffects.MOVEMENT_SPEED, 0,
                    BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak()
                            .sound(SoundType.GRASS).offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)),
            SpaceCreativeTabs.Tab.BLOCKS);

    public static void init() {
        // Klassenladen vor SpaceBlocks.init()
    }
}
