package com.space_m.orbit.propulsion;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Gemeinsamer Teil der Triebwerks-Bloecke (Original {@code BlockDummyable implements ILookOverlay}): TESR-Darstellung,
 * optional {@code getDirModified(dir) = dir.getRotation(DOWN)} und die Frage des Overlays "schaut der Spieler auf eine
 * Anschlusszelle ({@code TileEntityProxyCombo})?".
 */
public abstract class StationThrusterBlock extends DummyableMachineBlock implements ILookOverlay {

    protected StationThrusterBlock(Properties properties) {
        super(properties);
    }

    /** Original {@code getDirModified} ueberschrieben ({@code dir.getRotation(DOWN)}). */
    protected boolean rotatedPlacement() {
        return false;
    }

    /** Anschlusszellen ({@code makeExtra}) als {forward, up, side} wie im Builder. */
    protected abstract int[][] extras();

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state == null || !rotatedPlacement()) return state;
        return state.setValue(FACING, PropulsionUtil.placementFacing(state.getValue(FACING)));
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Original im {@code printHook}: {@code world.getTileEntity(x, y, z) instanceof TileEntityProxyCombo}; liefert die Zelle oder null. */
    @Nullable
    protected BlockPos lookedAtPort(BlockPos core, BlockState state) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return null;
        BlockPos looked = bhr.getBlockPos();
        Direction dir = PropulsionUtil.dir(state);
        Direction rot = PropulsionUtil.rot(dir);
        for (int[] e : extras()) {
            BlockPos p = core.offset(dir.getStepX() * e[0] + rot.getStepX() * e[2], e[1], dir.getStepZ() * e[0] + rot.getStepZ() * e[2]);
            if (p.equals(looked)) return p;
        }
        return null;
    }

    protected static void print(net.minecraft.client.gui.GuiGraphics g, net.minecraft.world.level.block.Block block, List<Component> text) {
        ILookOverlay.printGeneric(g, Component.translatable(block.getDescriptionId()), 0xffff00, 0x404000, text);
    }
}
