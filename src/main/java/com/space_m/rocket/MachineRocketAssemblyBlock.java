package com.space_m.rocket;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Vehicle Assembly Base (VAB), 1:1 NTM Space {@code MachineRocketAssembly} ({@code BlockDummyable}): 9x9-Plattform auf
 * Hoehe +2 ueber dem Setzpunkt (Kern in der Mitte, {@code getHeightOffset 2}, {@code getOffset 4}), dazu vier L-foermige
 * Eckbeine (je zwei Saeulen, 3 Bloecke hoch). Die Rakete wird auf dem Kern aufgebaut; Arbeitsbuehnen und Stuetzsaeulen
 * setzt die Blockentity je nach Raketenhoehe ({@link RocketAssemblyPartBlock}). Im Orbit nicht benutzbar (Blick-Overlay
 * warnt). Darstellung per BER ({@code RenderRocketAssembly}).
 * Original-Klasse: MachineRocketAssembly
 */
public class MachineRocketAssemblyBlock extends DummyableMachineBlock implements ILookOverlay {

    public MachineRocketAssemblyBlock(Properties properties) {
        super(properties);
    }

    /** Original {@code fillSpace}: Deckplatte {0,0,4,4,4,4} und acht Beinsaeulen. */
    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(0, 0, 4, 4, 4, 4)
                .box(0, 2, 4, -3, 4, -4)
                .box(0, 2, 4, -4, 4, -3)
                .box(0, 2, 4, -3, -4, 4)
                .box(0, 2, 4, -4, -3, 4)
                .box(0, 2, -3, 4, -4, 4)
                .box(0, 2, -4, 4, -3, 4)
                .box(0, 2, -3, 4, 4, -4)
                .box(0, 2, -4, 4, 4, -3)
                .placementOffset(4)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 2;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineRocketAssemblyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return createTickerHelper(type, SpaceBlockEntities.ROCKET_ASSEMBLY.get(), MachineRocketAssemblyBlockEntity::serverTick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }
    *///?}

    /** Original {@code onBlockActivated}: im Orbit {@code false}, sonst {@code standardOpenBehavior}. */
    public static InteractionResult openMenu(Level level, BlockPos pos, Player player) {
        if (CelestialBody.inOrbit(level)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof MachineRocketAssemblyBlockEntity be) {
            MenuRegistry.openExtendedMenu(sp, be, buf -> buf.writeBlockPos(pos));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Original {@code breakBlock}: {@code isBreaking = true} (Laufwerke fallen nicht heraus), dann Plattformen/Saeulen entfernen. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof MachineRocketAssemblyBlockEntity be) {
                be.isBreaking = true;
                be.dropInventoryContents();
                be.removeGenerated();
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
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

    /** Original {@code printHook}: nur im Orbit eine blinkende Warnung. */
    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (CelestialBody.inOrbit(world)) {
            List<Component> text = new ArrayList<>();
            text.add(Component.literal("! ! ! ").append(Component.translatable("atmosphere.yesOrbit")).append(" ! ! !")
                    .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00)));
            ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
        }
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineRocketAssemblyBlock> CODEC = simpleCodec(MachineRocketAssemblyBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
