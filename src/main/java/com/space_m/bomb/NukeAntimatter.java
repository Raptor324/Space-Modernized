package com.space_m.bomb;

import com.hbm_m.block.bomb.NukeBaseBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code NukeAntimatter} ({@code BlockMachineBase}, GUI 0, TESR {@code RenderAntimatter}).
 * <ul>
 *   <li>Redstone an einem Nachbarn ({@code onNeighborBlockChange}) zuendet ueber {@link #explode(Level, BlockPos)};
 *       anders als die HBM-Atombomben nicht schon beim Setzen.</li>
 *   <li>{@code onBlockPlacedBy} ist im Original ueberschrieben (nur Log), {@code BlockMachineBase} setzt die Drehung daher
 *       nie: Metadaten bleiben 0, die Bombe steht immer gleich ausgerichtet -> hier immer Grundzustand.</li>
 *   <li>Detonator/{@code IBomb}: geladen -> {@code DETONATED}, sonst {@code ERROR_MISSING_COMPONENT}.</li>
 * </ul>
 * Original-Klasse: NukeAntimatter
 */
public class NukeAntimatter extends NukeBaseBlock {

    public NukeAntimatter(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAntimatter(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof TileEntityAntimatter amat) amat.serverTick();
        };
    }

    /** Original: keine Drehung (Metadaten 0). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    /** Original {@code BlockMachineBase}: Setzen zuendet nicht (nur {@code onNeighborBlockChange}). */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) { }

    /** Nur fuer {@code NukeBaseBlock}; die Antimaterie-Bombe zuendet ueber {@link TileEntityAntimatter#explode()}. */
    @Override
    protected void explode(Level level, double x, double y, double z) { }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos) {
        if (level.isClientSide) return BombReturnCode.UNDEFINED;
        if (level.getBlockEntity(pos) instanceof TileEntityAntimatter bomb) {
            if (bomb.isLoaded()) {
                bomb.explode();
                return BombReturnCode.DETONATED;
            }
            return BombReturnCode.ERROR_MISSING_COMPONENT;
        }
        // Original: ClassCastException/NPE ohne Tile - hier ohne Absturz
        return BombReturnCode.UNDEFINED;
    }

    //? if > 1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<NukeAntimatter> CODEC = simpleCodec(NukeAntimatter::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.Block> codec() { return CODEC; }
     *///?}
}
