package com.space_m.machine.atmo;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 NTM Space {@code BlockAtmosphereEditor.TileEntityAtmosphereEditor}: Zustand {@code isOn} (aus), Durchsatz-Exponent
 * {@code throughputFactor} (10), Gas {@code fluid} ({@code EARTHAIR}), Modus {@code isEmitting} (an). Alle 5 Ticks
 * (Spielzeit) im Betrieb: {@code FT_Gaseous.release/capture(world, fluid, 10^throughputFactor)}.
 *
 * <p>Abweichung (technisch noetig): das Original schickt seinen Zustand jeden Tick an alle Spieler im Umkreis 25
 * ({@code BufPacket}) - er aendert sich nur durch Werkzeuge/Identifikator, daher wird hier nur dann synchronisiert
 * (gleiches Ergebnis fuer das Blick-Overlay). NBT: Fluid als Registry-ID statt {@code FluidType.getID()}.
 * Original-Klasse: TileEntityAtmosphereEditor
 */
public class AtmosphereEditorBlockEntity extends BaseHbmBlockEntity {

    boolean isOn = false;
    int throughputFactor = 10;
    Fluid fluid = ModFluids.AIR.getSource();
    boolean isEmitting = true;

    public AtmosphereEditorBlockEntity(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ATMOSPHERE_EDITOR.get(), pos, state);
    }

    public boolean isOn() { return isOn; }
    public int getThroughputFactor() { return throughputFactor; }
    public Fluid getFluid() { return fluid; }
    public boolean isEmitting() { return isEmitting; }

    public static void tick(Level level, BlockPos pos, BlockState state, AtmosphereEditorBlockEntity be) {
        if (level.isClientSide) return;

        if (be.isOn && level.getGameTime() % 5 == 0) {
            double amount = Math.pow(10, be.throughputFactor);
            if (be.isEmitting) {
                CelestialBody.releaseGas(level, AtmoFluids.id(be.fluid), amount);
            } else {
                CelestialBody.captureGas(level, AtmoFluids.id(be.fluid), amount);
            }
        }
    }

    void markChanged() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putInt("throughput", throughputFactor);
        tag.putString("fluid", AtmoFluids.id(fluid).toString());
        tag.putBoolean("emit", isEmitting);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        isOn = tag.getBoolean("isOn");
        throughputFactor = tag.contains("throughput") ? tag.getInt("throughput") : 10;
        fluid = tag.contains("fluid") ? AtmoFluids.fluid(CelestialFluids.parse(tag.getString("fluid"))) : ModFluids.AIR.getSource();
        isEmitting = !tag.contains("emit") || tag.getBoolean("emit");
    }
}
