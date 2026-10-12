package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.celestial.CelestialBody;
import com.space_m.fluid.trait.FT_Rocket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code MachineHTR3} ({@code machine_htr3}, "LPW-3N Station Thruster"): {@code getDimensions
 * {3, 3, 3, 3, 5, 5}}, Offset 5, Hoehenversatz 3, {@code getDirModified = dir.getRotation(DOWN)}, eine Anschlusszelle bei
 * {@code -rot*5} (nur Fluid). Rechtsklick mit Fluid-Identifikator: Tanktyp = Produkt der ersten Heizstufe, wenn es
 * {@code FT_Rocket} hat. Overlay im Orbit: Ausrichtung, Tank, an der Anschlusszelle "Connect to PWR from here".
 * Original-Klasse: MachineHTR3
 */
public class MachineHTR3 extends StationThrusterBlock {

    private static final int[][] EXTRAS = { {0, 0, -5} };

    public MachineHTR3(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean rotatedPlacement() {
        return true;
    }

    @Override
    protected int[][] extras() {
        return EXTRAS;
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(3, 3, 3, 3, 5, 5);
        for (int[] e : EXTRAS) b.extra(e[0], e[1], e[2]);
        return b.placementOffset(5).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 3;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHTR3(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.HTR3.get(), TileEntityMachineHTR3::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(InteractionHand.MAIN_HAND));
    }
    *///?}

    /** Original {@code onBlockActivated}: Server, nicht geschlichen, Fluid-Identifikator in der Hand -> Tanktyp setzen. */
    private InteractionResult onActivated(Level world, BlockPos pos, Player player, ItemStack held) {
        if (!world.isClientSide && !player.isShiftKeyDown()) {

            if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier identifier) {
                if (!(world.getBlockEntity(pos) instanceof TileEntityMachineHTR3 htr3)) return InteractionResult.PASS;

                Fluid type = identifier.getType(world, pos, held);

                FT_Heatable heatable = FluidType.getTrait(type, FT_Heatable.class);

                if (heatable != null && heatable.getFirstStep() != null
                        && FluidType.hasTrait(heatable.getFirstStep().typeProduced, FT_Rocket.class)) {
                    htr3.tanks[0].setTankType(heatable.getFirstStep().typeProduced);
                    htr3.setChanged();
                }

                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;

        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!CelestialBody.inOrbit(world)) return;

        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineHTR3 thruster)) return;

        List<Component> text = new ArrayList<>();

        if (!thruster.isFacingPrograde()) {
            text.add(PropulsionUtil.blinkWarning("atmosphere.engineFacing"));
        } else {
            for (FluidTank tank : thruster.tanks) {
                text.add(PropulsionUtil.tankLine(tank, true));
            }

            if (lookedAtPort(pos, world.getBlockState(pos)) != null) {
                text.add(Component.literal("Connect to PWR from here"));
            }
        }

        print(g, this, text);
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
    /*public static final com.mojang.serialization.MapCodec<MachineHTR3> CODEC = simpleCodec(MachineHTR3::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
