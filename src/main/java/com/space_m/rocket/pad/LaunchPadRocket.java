package com.space_m.rocket.pad;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.item.ModItems;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code LaunchPadRocket} ({@code launch_pad_rocket}, "Rocket Launch Pad"): 13 x 3 x 15-Sockel
 * ({@code getDimensions {2, 0, 7, 6, 6, 6}}, Offset 6) aus Hauptkoerper {@code {2, 0, 6, 6, 4, 4}} und zwei Fluegeln
 * {@code {2, 0, 4, 0, 6, 6}} / {@code {2, 0, 0, 4, 6, 6}} bei {@code -dir*2} / {@code +dir*2}; hinten (bei {@code -dir*7})
 * 4 x 3 Anschlusszellen plus eine bei {@code +rot*3} (Strom, Fluide, Inventar). Im Orbit nicht benutzbar.
 * Blick-Overlay: benoetigte Treibstoffe; im Orbit blinkend "UNUSABLE IN ORBIT".
 * Original-Klasse: LaunchPadRocket
 */
public class LaunchPadRocket extends DummyableMachineBlock implements ILookOverlay {

    public LaunchPadRocket(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create()
                // Main body
                .box(2, 0, 6, 6, 4, 4)
                .boxAt(-2, 0, 0, 2, 0, 4, 0, 6, 6)
                .boxAt(2, 0, 0, 2, 0, 0, 4, 6, 6);

        // Inputs
        for (int or = 1; or < 5; or++) {
            for (int oy = 0; oy < 3; oy++) {
                b.extra(-7, oy, -or);
            }
        }
        b.extra(-7, 0, 3);

        return b.placementOffset(6).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPadRocket(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.LAUNCH_PAD_ROCKET.get(), TileEntityLaunchPadRocket::tick);
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TileEntityLaunchPadRocket pad) {
            pad.removeTower(level);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player);
    }
    *///?}

    /** Original {@code onBlockActivated}: im Orbit nichts, sonst {@code standardOpenBehavior} (Schleichen = nichts). */
    private InteractionResult onActivated(Level level, BlockPos pos, Player player) {
        if (CelestialBody.inOrbit(level)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof TileEntityLaunchPadRocket pad) {
            MenuRegistry.openExtendedMenu((ServerPlayer) player, pad, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (CelestialBody.inOrbit(world)) {
            List<Component> text = new ArrayList<>();
            text.add(Component.literal("! ! ! ").append(Component.translatable("atmosphere.yesOrbit")).append(" ! ! !")
                    .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00)));
            ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
            return;
        }

        if (!(world.getBlockEntity(pos) instanceof TileEntityLaunchPadRocket pad)) return;

        if (pad.rocket == null) return;

        // y - pos[1] > 2: kein Overlay auf dem Versorgungsturm (Turmbloecke haben hier kein Overlay)

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Required fuels:"));

        for (FluidTank tank : pad.tanks) {
            if (tank.getTankType() == ModFluids.NONE.getSource()) continue;
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(FluidType.forFluid(tank.getTankType()).getLocalizedName().copy().withStyle(ChatFormatting.RESET))
                    .append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB").withStyle(ChatFormatting.RESET)));
        }

        if (pad.solidFuel.max > 0) {
            text.add(Component.literal("-> ").withStyle(ChatFormatting.GREEN)
                    .append(Component.translatable(ModItems.ROCKET_FUEL.get().getDescriptionId()).withStyle(ChatFormatting.RESET))
                    .append(Component.literal(": " + pad.solidFuel.level + "/" + pad.solidFuel.max + "kg").withStyle(ChatFormatting.RESET)));
        }

        if (text.size() <= 1) return;

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LaunchPadRocket> CODEC = simpleCodec(LaunchPadRocket::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
