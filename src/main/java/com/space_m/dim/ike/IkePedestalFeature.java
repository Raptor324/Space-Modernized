package com.space_m.dim.ike;

import com.hbm_m.blockentity.decorations.PedestalBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code space_m:ike_pedestal} = "the HACKIEST hack i ever hacked" aus {@code WorldGeneratorIke.generateIke}: jeder
 * Sockel ({@code pedestal}) im Chunk bekommt einen {@code divine_shard}. Das Original durchsucht alle 16x16xHoehe
 * Bloecke; hier werden nur die Block-Entities des Chunks geprueft (gleiches Ergebnis, Sockel sind Block-Entities).
 * Laeuft nach den Strukturen (Schritt UNDERGROUND_ORES), damit der Sockel des Ike-Artefakts schon steht.
 */
public class IkePedestalFeature extends Feature<NoneFeatureConfiguration> {

    public IkePedestalFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        ChunkAccess chunk = world.getChunk(ctx.origin().getX() >> 4, ctx.origin().getZ() >> 4);
        boolean any = false;
        for (BlockPos pos : java.util.List.copyOf(chunk.getBlockEntitiesPos())) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PedestalBlockEntity pedestal) {
                pedestal.setItem(new ItemStack(IkeBlocks.DIVINE_SHARD.get(), 1));
                any = true;
            }
        }
        return any;
    }
}
