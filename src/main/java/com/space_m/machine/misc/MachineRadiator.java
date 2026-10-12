package com.space_m.machine.misc;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.orbit.propulsion.PropulsionUtil;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code MachineRadiator} ({@code machine_radiator}, Vakuum-Kuehler): {@code getDimensions {1, 1, 0, 0, 1, 1}}
 * (3x3 quer zur Blickrichtung), Offset 0, Hoehenversatz 1, vier Fluid-Anschlusszellen an den Ecken ({@code +-rot}, y+-1).
 * Overlay: Vakuum-Warnung nur ohne {@code vacuumOptimised} (der Radiator ist es), Eingangs-/Ausgangstank.
 * Original-Klasse: MachineRadiator
 */
public class MachineRadiator extends R7MachineBlock {

    public MachineRadiator(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create().box(1, 1, 0, 0, 1, 1)
                .extra(0, 1, 1).extra(0, -1, 1).extra(0, 1, -1).extra(0, -1, -1)
                .placementOffset(0).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public int getHeightOffset() {
        return 1;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityRadiator(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, MiscBlockEntities.RADIATOR.get(), TileEntityRadiator::tick);
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityRadiator tower)) return;

        List<Component> text = new ArrayList<>();

        if (!tower.vacuumOptimised) {
            CBT_Atmosphere atmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);
            if (CelestialBody.inOrbit(world) || atmosphere == null || atmosphere.getPressure() < 0.01) {
                text.add(PropulsionUtil.blinkWarning("atmosphere.noVacuum"));
            }
        }

        for (int i = 0; i < tower.tanks.length; i++)
            text.add(PropulsionUtil.tankLine(tower.tanks[i], i < 1));

        print(g, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineRadiator> CODEC = simpleCodec(MachineRadiator::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
