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
 * 1:1 NTM Space {@code MachineHTRF4} ({@code machine_htrf4}, Legacy): {@code getDimensions {2, 2, 2, 2, 11, 9}}, Offset 11,
 * Hoehenversatz 2, {@code getDirModified = dir.getRotation(DOWN)}, Anschlusszellen bei {@code -rot*9} (Plasma) und
 * {@code -rot*9 +-dir} (Strom). Im Original ohne Rezept (nur Kreativ).
 * Original-Klasse: MachineHTRF4
 */
public class MachineHTRF4 extends StationThrusterBlock {

    private static final int[][] EXTRAS = { {0, 0, -9}, {1, 0, -9}, {-1, 0, -9} };

    public MachineHTRF4(Properties properties) {
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
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(2, 2, 2, 2, 11, 9);
        for (int[] e : EXTRAS) b.extra(e[0], e[1], e[2]);
        return b.placementOffset(11).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 2;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineHTRF4(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, PropulsionBlockEntities.HTRF4.get(), TileEntityMachineHTRF4::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!CelestialBody.inOrbit(world)) return;

        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineHTRF4 thruster)) return;

        List<Component> text = new ArrayList<>();

        if (!thruster.isFacingPrograde()) {
            text.add(PropulsionUtil.blinkWarning("atmosphere.engineFacing"));
        } else {
            text.add(PropulsionUtil.powerLine(thruster.getEnergyStored()));
            for (FluidTank tank : thruster.tanks) {
                text.add(PropulsionUtil.tankLine(tank, true));
            }

            BlockPos port = lookedAtPort(pos, world.getBlockState(pos));
            if (port != null) {
                if (pos.getX() == port.getX() || pos.getZ() == port.getZ()) {
                    text.add(Component.literal("Connect to Plasma Heater from here"));
                } else {
                    text.add(Component.literal("Connect to power from here"));
                }
            }
        }

        print(g, this, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineHTRF4> CODEC = simpleCodec(MachineHTRF4::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
