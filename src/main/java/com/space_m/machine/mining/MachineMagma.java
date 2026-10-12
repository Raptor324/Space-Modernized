package com.space_m.machine.mining;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.celestial.CelestialBody;
import com.space_m.machine.chem.ChemUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineMagma} ({@code machine_magma}, "Magma Drill"): {@code getDimensions {3, 3, 3, 3, 3, 3}}
 * (7x7x7), Offset 3, Hoehenversatz 3; sechs Anschlusszellen (Strom + Fluid) an der Rueckseite ({@code -dir*3}, eine und
 * zwei unter dem Kern, mittig und {@code +-rot}). Overlay: nicht auf Moho / zu wenig Lava blinkend, sonst Strom, Tanks
 * und die gesammelten Schmelzen. Tooltip {@code addStandardInfo}.
 * Original-Klasse: MachineMagma
 */
public class MachineMagma extends DummyableMachineBlock implements ILookOverlay {

    public MachineMagma(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(3, 3, 3, 3, 3, 3);
        // Original fillSpace: makeExtra bei x - dir*3 (+-rot), y-1 und y-2
        b.extra(-3, -1, 0).extra(-3, -2, 0);
        b.extra(-3, -1, 1).extra(-3, -1, -1);
        b.extra(-3, -2, 1).extra(-3, -2, -1);
        return b.placementOffset(3).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 3;
    }

    /** Original TESR ({@code RenderMagma}). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineMagma(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, MiningMachines.MAGMA_BE.get(), TileEntityMachineMagma::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineMagma drill)) return;

        List<Component> text = new ArrayList<>();

        CelestialBody body = CelestialBody.getBody(world);

        if (!"moho".equals(body.name)) {
            text.add(ChemUtil.blinkLiteral("MUST BE ON MOHO"));
        } else if (!drill.validPosition) {
            text.add(ChemUtil.blinkLiteral("INSUFFICIENT LAVA FOUND"));
        } else {
            text.add(ChemUtil.powerLine(drill.getEnergyStored(), drill.getEnergyStored() < drill.consumption));

            for (int i = 0; i < drill.tanks.length; i++)
                text.add(ChemUtil.tankLine(drill.tanks[i], i == 0));

            for (MaterialStack sta : drill.liquids) {
                text.add(Component.translatable(sta.material.getUnlocalizedName())
                        .append(": " + Mats.formatAmount(sta.amount, false)).withStyle(ChatFormatting.YELLOW));
            }
        }

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** Original {@code addInformation}: {@code addStandardInfo} (Umschalttaste zeigt {@code .desc}). */
    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineMagma> CODEC = simpleCodec(MachineMagma::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
