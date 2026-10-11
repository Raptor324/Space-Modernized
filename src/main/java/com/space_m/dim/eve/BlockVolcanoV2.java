package com.space_m.dim.eve;

import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code com.hbm.blocks.BlockVolcanoV2} ({@code geysir_electric}, "Discharge Geyser"): Basaltblock mit
 * {@link TileEntityLightningVolcano}, das sich alle 100-499 Ticks entlaedt - Plasmawelle 20 Bloecke ueber dem Block,
 * Donner (Lautstaerke 200) und Stromschlag fuer alle Lebewesen in der Saeule darueber (bis +60, Schaden
 * {@code clamp(maxHealth, 3, 20)}, {@code ModDamageSource.electricity}). {@code isOpaqueCube = false}.
 *
 * <p>Das Original hat keine Metadaten-Ausrichtung beim Setzen ({@code WorldGenElectricVolcano} setzt Meta 0 =
 * {@code ForgeDirection.DOWN}); der Strahl zeigt deshalb nach oben (siehe {@code RenderVol2}).
 */
public class BlockVolcanoV2 extends BaseEntityBlock {

    public BlockVolcanoV2(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLightningVolcano(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, EveBlocks.ELECTRIC_VOLCANO_BE.get(), TileEntityLightningVolcano::tick);
    }

    //? if >= 1.21.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockVolcanoV2> CODEC = simpleCodec(BlockVolcanoV2::new);
    @Override protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    *///?}

    /** 1:1 {@code BlockVolcanoV2.TileEntityLightningVolcano} (Mapping {@code tileentity_electric_volcano}). */
    public static class TileEntityLightningVolcano extends BaseHbmBlockEntity {

        private static final ResourceLocation TESLA = ResourceLocation.fromNamespaceAndPath("hbm_m", "weapon.tesla");

        public int chargetime;
        public float flashd;

        public TileEntityLightningVolcano(BlockPos pos, BlockState state) {
            super(EveBlocks.ELECTRIC_VOLCANO_BE.get(), pos, state);
            // Quick pre-set to prevent it immediately firing on load
            chargetime = (new Random()).nextInt(400) + 100;
        }

        /** {@code updateEntity} (Server und Client). */
        public static void tick(Level level, BlockPos pos, BlockState state, TileEntityLightningVolcano te) {
            te.updateEntity(level);
        }

        private void updateEntity(Level worldObj) {
            // flashd is used locally to render the spike, and on the server for timing
            if (chargetime > 0) {
                flashd = 0;
            } else {
                flashd += 0.3f;
                flashd = Math.min(100.0f, flashd + 0.3f * (100.0f - flashd) * 0.15f);
            }

            if (!worldObj.isClientSide && worldObj instanceof ServerLevel server) {
                // getBlockMetadata() == 0 (nie anders gesetzt) -> ForgeDirection.DOWN (0, -1, 0)
                Direction dir = Direction.DOWN;
                int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();

                if (chargetime == 1) {
                    long t = worldObj.getGameTime() / 5L;
                    double x = (int) (xCoord + dir.getStepX() * t % 1) + 0.5;
                    double y = (int) (yCoord + dir.getStepY() * t % 1) + 20.5;
                    double z = (int) (zCoord + dir.getStepZ() * t % 1) + 0.5;

                    CompoundTag data = new CompoundTag();
                    data.putString("type", "plasmablast");
                    data.putFloat("r", 1F);
                    data.putFloat("g", 1F);
                    data.putFloat("b", 1F);
                    data.putFloat("scale", 10 * 4);
                    data.putFloat("yaw", 90);

                    com.hbm_m.particle.helper.IParticleCreator.sendPacket(server, x, y, z, 256, data);
                    worldObj.playSound(null, x, y, z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 200, 0.8F + worldObj.random.nextFloat() * 0.2F);
                    vapor(server);
                }

                // we only want to modify chargetime on the server, since it is synced to clients
                if (chargetime > 0) chargetime--;

                if (flashd >= 100) {
                    chargetime = worldObj.random.nextInt(400) + 100;
                }

                networkPack(server, 256);
            }
        }

        /** {@code PacketDispatcher.sendToAllAround(new BufPacket(...), 256)}: {@code chargetime} an Spieler im Umkreis. */
        private void networkPack(ServerLevel server, int range) {
            CompoundTag tag = new CompoundTag();
            tag.putInt("charge", chargetime);
            ClientboundBlockEntityDataPacket packet = com.hbm_m.platform.BlockHooks.dataPacket(this, tag);
            for (var player : server.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
                if (player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= (double) range * range)
                    player.connection.send(packet);
            }
        }

        @Override
        protected void applyClientUpdate(@NotNull CompoundTag tag) {
            if (tag.contains("charge")) chargetime = tag.getInt("charge");
        }

        @Override
        protected void readNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
            chargetime = nbt.getInt("charge");
        }

        @Override
        protected void writeNbtData(@NotNull CompoundTag nbt, @Nullable HolderLookup.Provider registries) {
            nbt.putInt("charge", this.chargetime);
        }

        /** {@code INFINITE_EXTENT_AABB}. */
        @Override
        public AABB getRenderBoundingBox() {
            return AABB.ofSize(worldPosition.getCenter(), 1.0E6, 1.0E6, 1.0E6);
        }

        private void vapor(ServerLevel world) {
            int xCoord = worldPosition.getX(), yCoord = worldPosition.getY(), zCoord = worldPosition.getZ();
            List<Entity> entities = world.getEntitiesOfClass(Entity.class,
                    new AABB(xCoord - 0.5, yCoord + 0.5, zCoord - 0.5, xCoord + 1.5, yCoord + 60, zCoord + 1.5));

            if (!entities.isEmpty()) {
                for (Entity e : entities) {
                    if (e instanceof LivingEntity living) {
                        if (e.hurt(com.hbm_m.damagesource.ModDamageSources.electricity(world), Mth.clamp(living.getMaxHealth() * 1F, 3, 20))) {
                            world.playSound(null, e.getX(), e.getY(), e.getZ(), tesla(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }
            }
        }

        private static SoundEvent tesla() {
            SoundEvent s = BuiltInRegistries.SOUND_EVENT.get(TESLA);
            return s != null ? s : SoundEvent.createVariableRangeEvent(TESLA);
        }
    }
}
