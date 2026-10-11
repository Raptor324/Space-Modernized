package com.space_m.lifesupport;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.FluidLocalization;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 NTM Space {@code BlockAlgaeFilm} ({@code algae_film}, "Microgravity Algae Film"): nicht-opaker Vollblock mit
 * OBJ-Modell {@code algae_film.obj} (Textur {@code algae_film}), Ausrichtung nach Blickrichtung (Metadaten 2/5/3/4),
 * Shift-Info {@code .desc}, Blick-Overlay (ohne Orbit blinkt "REQUIRES ZERO-G", Tanks CO2 rein / O2 raus).
 * Original-Klasse: BlockAlgaeFilm
 * Original-Klasse: RenderAlgaeFilm – statisches Blockmodell {@code models/block/algae_film.json} (forge:obj),
 * Drehung im Blockstate (Original {@code ObjUtil.renderWithIcon} 0/90/180/270 -> y 0/270/180/90).
 */
public class BlockAlgaeFilm extends BaseEntityBlock implements ILookOverlay {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    public BlockAlgaeFilm(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Original {@code onBlockPlacedBy}: Blickrichtung 0/1/2/3 -> Metadaten 2/5/3/4 (zeigt zum Spieler). */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityAlgaeFilm(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.ALGAE_FILM.get(), TileEntityAlgaeFilm::tick);
    }

    @Override
    public void printHook(net.minecraft.client.gui.GuiGraphics guiGraphics, Level world, BlockPos pos) {

        BlockEntity tile = world.getBlockEntity(pos);

        if (!(tile instanceof TileEntityAlgaeFilm film)) return;

        List<Component> text = new ArrayList<>();

        if (!film.canOperate) {
            int blink = BobMathUtil.getBlink() ? 0xff0000 : 0xffff00;
            text.add(Component.literal("! ! ! ").append(Component.translatable("atmosphere.noGravity")).append(" ! ! !")
                    .withStyle(s -> s.withColor(TextColor.fromRgb(blink))));
        }

        text.add(Component.empty().append(Component.literal("-> ").withStyle(ChatFormatting.GREEN))
                .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(film.tanks[0].getTankType())))
                .append(": " + film.tanks[0].getFill() + "/" + film.tanks[0].getMaxFill() + "mB"));
        text.add(Component.empty().append(Component.literal("<- ").withStyle(ChatFormatting.RED))
                .append(FluidLocalization.nameFromFluidId(BuiltInRegistries.FLUID.getKey(film.tanks[1].getTankType())))
                .append(": " + film.tanks[1].getFill() + "/" + film.tanks[1].getMaxFill() + "mB"));

        LookOverlayText.print(guiGraphics, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<BlockAlgaeFilm> CODEC = simpleCodec(BlockAlgaeFilm::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
