package com.space_m.machine.chem;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * 1:1 NTM Space {@code MachineCryoDistill} ({@code machine_cryo_distill}, "Cryogenic Distillation Machine"):
 * Offset 3, Hoehenversatz 2, gefuellt wie {@code fillSpace}: Mittelebene {@code {0,0,3,3,2,2}}, Laufsteg-Seiten
 * {@code {0,2,3,0,2,2}}/{@code {0,2,-2,3,2,2}}, Tanks oben {@code {2,0,3,-1,2,2}}/{@code {3,0,-2,3,2,2}}; Anschluesse
 * ({@code makeExtra}, Fluid + Strom) vorne vier Ausgaenge (dir 3, rot -2..2 ohne 0, y-2), seitlich Eingang und Strom
 * (dir -1/-2, rot -2, y-2). Overlay nur beim Blick auf einen Anschluss ({@code hitCheck}).
 * Original-Klasse: MachineCryoDistill
 */
public class MachineCryoDistill extends DummyableMachineBlock implements ILookOverlay {

    public MachineCryoDistill(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                // Midpoint plane
                .box(0, 0, 3, 3, 2, 2)
                // Each side of the walkway
                .box(0, 2, 3, 0, 2, 2)
                .box(0, 2, -2, 3, 2, 2)
                // Top tanks
                .box(2, 0, 3, -1, 2, 2)
                .box(3, 0, -2, 3, 2, 2)
                // Front face outputs
                .extra(3, -2, -2)
                .extra(3, -2, -1)
                .extra(3, -2, 1)
                .extra(3, -2, 2)
                // Side inputs
                .extra(-1, -2, -2)
                .extra(-2, -2, -2)
                .placementOffset(3)
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
        return new TileEntityMachineCryoDistill(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ChemMachines.CRYO_DISTILL_BE.get(), TileEntityMachineCryoDistill::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ChemMachines.standardOpen(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ChemMachines.standardOpen(level, pos, player);
    }
    *///?}

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineCryoDistill distill)) return;
        BlockPos hit = com.space_m.machine.chem.client.ChemClientUtil.lookedAtBlock();
        if (hit == null) return;

        Direction dir = ChemUtil.dir(distill.getBlockState());
        List<Component> text = ChemUtil.lines();

        if (hitCheck(dir, pos, -1, -2, -2, hit)) {
            text.add(ChemUtil.arrowLine(ChemUtil.fluidName(distill.tanks[0].getTankType()), true));
        }
        if (hitCheck(dir, pos, -2, -2, -2, hit)) {
            text.add(ChemUtil.arrowLine(Component.literal("Power"), true));
        }

        if (hitCheck(dir, pos, 3, -2, -2, hit)
                || hitCheck(dir, pos, 3, -1, -2, hit)
                || hitCheck(dir, pos, 3, 1, -2, hit)
                || hitCheck(dir, pos, 3, 2, -2, hit)) {
            for (int i = 1; i < 5; i++) text.add(ChemUtil.arrowLine(ChemUtil.fluidName(distill.tanks[i].getTankType()), false));
        }

        if (!text.isEmpty()) {
            ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
        }
    }

    /** Original {@code hitCheck(dir, core, exDir, exRot, exY, hit)}. */
    protected boolean hitCheck(Direction dir, BlockPos core, int exDir, int exRot, int exY, BlockPos hit) {
        return ChemUtil.offset(core, dir, exDir, exRot, exY).equals(hit);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineCryoDistill> CODEC = simpleCodec(MachineCryoDistill::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
