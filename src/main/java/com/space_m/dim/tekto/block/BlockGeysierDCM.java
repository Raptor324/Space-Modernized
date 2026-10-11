package com.space_m.dim.tekto.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.space_m.dim.tekto.TektoBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.blocks.BlockGeysierDCM} ({@code geysir_chloric} "DCM Geysir", Kern der Tekto-Vulkane):
 * {@code BlockContainer} aus Stein, leuchtet (15), Textur Basalt. Die Block-Entity ({@code tileentity_dcm})
 * stoesst jeden Tick eine Dampfsaeule aus ({@code missileContrail}, Massstab 1.5, nach oben 4, Alter 100-119) und
 * veraetzt Lebewesen bis 60 Bloecke darueber (Saeure, 10 % der Max-Gesundheit, 3-20, Zischen).
 *
 * <p>Original ruft das Partikel serverseitig ueber {@code MainRegistry.proxy.effectNT} - wirksam nur im
 * Einzelspieler (integrierter Server mit ClientProxy). Hier laeuft es im Client-Tick der Block-Entity.
 */
public class BlockGeysierDCM extends Block implements EntityBlock {

    public BlockGeysierDCM(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDCM(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != TektoBlocks.DCM.get()) return null;
        return level.isClientSide ? (l, p, s, be) -> ((TileEntityDCM) be).clientTick() : (l, p, s, be) -> ((TileEntityDCM) be).serverTick();
    }

    public static class TileEntityDCM extends BlockEntity {

        public TileEntityDCM(BlockPos pos, BlockState state) {
            super(TektoBlocks.DCM.get(), pos, state);
        }

        void clientTick() {
            CompoundTag data = new CompoundTag();
            data.putDouble("posX", worldPosition.getX() + 0.5);
            data.putDouble("posY", worldPosition.getY() + 0);
            data.putDouble("posZ", worldPosition.getZ() + 0.5);
            data.putString("type", "missileContrail");
            data.putFloat("scale", 1.5f);
            data.putDouble("moX", 0);
            data.putDouble("moY", 4);
            data.putDouble("moZ", 0);
            data.putInt("maxAge", 100 + level.random.nextInt(20));
            data.putInt("color", 0xA4D7DD);
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
        }

        void serverTick() {
            vapor();
        }

        // copy pasting without reading makes you create a fluid geyser that... zaps you to death with electricty?
        private void vapor() {
            List<Entity> entities = this.level.getEntitiesOfClass(Entity.class, new AABB(worldPosition.getX() - 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() - 0.5,
                    worldPosition.getX() + 1.5, worldPosition.getY() + 60, worldPosition.getZ() + 1.5));

            for (Entity e : entities) {
                if (e instanceof LivingEntity living) {
                    if (e.hurt(com.hbm_m.damagesource.ModDamageSources.acid(level), Mth.clamp(living.getMaxHealth() * 0.1F, 3, 20))) {
                        level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    }
                }
            }
        }
        // getRenderBoundingBox/getMaxRenderDistanceSquared: ohne Block-Renderer bedeutungslos (Partikel)
    }
}
