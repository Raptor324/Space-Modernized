package com.space_m.orbit.propulsion;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.celestial.CelestialBody;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineXenonThruster} ({@code machine_xenon_thruster}): {@code getDimensions {1, 1, 0, 1, 1, 1}},
 * Offset 1, Hoehenversatz 1, zwei Anschlusszellen links/rechts vom Kern ({@code +-rot}, Strom + Fluid). Overlay nur im
 * Orbit: falsche Ausrichtung blinkend, sonst Strom und Xenon-Tank.
 * Original-Klasse: MachineXenonThruster
 */
public class MachineXenonThruster extends StationThrusterBlock {

    private static final int[][] EXTRAS = { {0, 0, -1}, {0, 0, 1} };

    public MachineXenonThruster(Properties properties) {
        super(properties);
    }

    @Override
    protected int[][] extras() {
        return EXTRAS;
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(1, 1, 0, 1, 1, 1);
        for (int[] e : EXTRAS) b.extra(e[0], e[1], e[2]);
        return b.placementOffset(1).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityXenonThruster(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.XENON_THRUSTER.get(), TileEntityXenonThruster::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!CelestialBody.inOrbit(world)) return;

        if (!(world.getBlockEntity(pos) instanceof TileEntityXenonThruster thruster)) return;

        List<Component> text = new ArrayList<>();

        if (!thruster.isFacingPrograde()) {
            text.add(PropulsionUtil.blinkWarning("atmosphere.engineFacing"));
        } else {
            text.add(PropulsionUtil.powerLine(thruster.getEnergyStored()));
            for (FluidTank tank : thruster.tanks) {
                text.add(PropulsionUtil.tankLine(tank, true));
            }
        }

        print(g, this, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineXenonThruster> CODEC = simpleCodec(MachineXenonThruster::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
