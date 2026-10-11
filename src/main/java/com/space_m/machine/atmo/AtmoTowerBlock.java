package com.space_m.machine.atmo;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.util.BobMathUtil;
import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Atmospheric Liquefaction Tower, 1:1 NTM Space {@code AtmoTower} ({@code BlockDummyable}): {@code getDimensions
 * {6, 0, 1, 0, 0, 1}}, Offset 0, {@code makeExtra} bei Kern - dir - rot, Kern - dir und Kern - rot (die drei Sockelfelder
 * neben dem Kern, {@code TileEntityProxyCombo(false, true, true)} = Strom + Fluid). Kein GUI, nur Blick-Overlay.
 * Darstellung wie {@code RenderAtmoTower} (= {@code RenderDeuteriumTower}) als statisches OBJ-Blockmodell.
 * Original-Klasse: AtmoTower
 */
public class AtmoTowerBlock extends DummyableMachineBlock implements ILookOverlay {

    public AtmoTowerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(6, 0, 1, 0, 0, 1)
                .extra(-1, 0, -1)
                .extra(-1, 0, 0)
                .extra(0, 0, -1)
                .placementOffset(0)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AtmoTowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, SpaceBlockEntities.ATMOSPHERIC_TOWER.get(), AtmoTowerBlockEntity::tick);
    }

    /** Original {@code printHook}: Energie (rot unter 1/20 Maximum), dann beide Tanks ({@code ->} Eingang, {@code <-} Ausgang). */
    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof AtmoTowerBlockEntity tower)) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Power: " + BobMathUtil.getShortNumber(tower.getEnergyStored()) + "HE")
                .withStyle(tower.getEnergyStored() < tower.getMaxEnergyStored() / 20 ? ChatFormatting.RED : ChatFormatting.GREEN));

        for (int i = 0; i < tower.tanks.length; i++) {
            FluidTank tank = tower.tanks[i];
            text.add((i < 1 ? Component.literal("-> ").withStyle(ChatFormatting.GREEN) : Component.literal("<- ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("").withStyle(ChatFormatting.RESET)
                            .append(FluidType.forFluid(tank.getTankType()).getLocalizedName())
                            .append(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB")));
        }

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<AtmoTowerBlock> CODEC = simpleCodec(AtmoTowerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}
}
