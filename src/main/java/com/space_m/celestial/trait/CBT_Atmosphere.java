package com.space_m.celestial.trait;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.space_m.celestial.CelestialFluids;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Atmosphaere eines Himmelskoerpers (Partialdruecke in atm), 1:1 NTM Space {@code CBT_Atmosphere}.
 * Original-Klasse: CBT_Atmosphere
 *
 * <p>Abweichung (technisch noetig): {@code FluidType} -> Fluid-Registry-ID; NBT/Netz speichern die ID als Text statt
 * der Original-Zahl ({@code FluidType.getID()} existiert in HBM-Modernized nicht und waere nicht stabil).
 */
public class CBT_Atmosphere extends CelestialBodyTrait {

    public ArrayList<FluidEntry> fluids;

    public static class FluidEntry {
        public ResourceLocation fluid;
        public double pressure;

        public FluidEntry(ResourceLocation fluid, double percentage) {
            this.fluid = fluid;
            this.pressure = percentage;
        }
    }

    public CBT_Atmosphere() {
        fluids = new ArrayList<>();
    }

    public CBT_Atmosphere(ResourceLocation fluid, double pressure) {
        fluids = new ArrayList<>();
        fluids.add(new FluidEntry(fluid, pressure));
    }

    public CBT_Atmosphere and(ResourceLocation fluid, double pressure) {
        fluids.add(new FluidEntry(fluid, pressure));
        return this;
    }

    @Override
    public CBT_Atmosphere clone() {
        CBT_Atmosphere clone = new CBT_Atmosphere();
        for (FluidEntry entry : fluids) {
            clone.fluids.add(new FluidEntry(entry.fluid, entry.pressure));
        }
        return clone;
    }

    /** Add to the pressure by incrementing existing fluid entries. */
    public void add(ResourceLocation fluid, double pressure) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid.equals(fluid)) {
                entry.pressure += pressure;
                return;
            }
        }

        fluids.add(new FluidEntry(fluid, pressure));
    }

    /** Reduce the pressure by proportionally reducing the pressure of each entry. */
    public void reduce(double pressure) {
        double totalPressure = getPressure();
        if (pressure >= totalPressure) {
            fluids = new ArrayList<>();
            return;
        }

        double pressureRatio = pressure / totalPressure;

        for (FluidEntry entry : fluids) {
            entry.pressure *= pressureRatio;
        }
    }

    /** Fluid must be above at least .1 millibar to be "present". */
    public boolean hasFluid(ResourceLocation fluid) {
        return hasFluid(fluid, 0.0001);
    }

    public boolean hasFluid(ResourceLocation fluid, double abovePressure) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid.equals(fluid)) {
                return entry.pressure >= abovePressure;
            }
        }

        return false;
    }

    public boolean hasTrait(Class<? extends FluidTrait> trait) {
        return hasTrait(trait, 0.0001);
    }

    public boolean hasTrait(Class<? extends FluidTrait> trait, double abovePressure) {
        for (FluidEntry entry : fluids) {
            if (entry.pressure >= abovePressure && CelestialFluids.hasTrait(entry.fluid, trait)) {
                return true;
            }
        }

        return false;
    }

    /** Get the highest pressure fluid. */
    public ResourceLocation getMainFluid() {
        sortDescending();
        if (fluids.isEmpty()) return CelestialFluids.NONE; // Original: fluids.get(0) wirft bei leerer Liste
        FluidEntry first = fluids.get(0);
        return first != null ? first.fluid : CelestialFluids.NONE;
    }

    public void sortDescending() {
        fluids.sort((a, b) -> Double.compare(b.pressure, a.pressure));
    }

    /** FluidEntries store PARTIAL pressure, to get the total atmospheric pressure, use this method. */
    public double getPressure() {
        double pressure = 0;
        for (int i = 0; i < fluids.size(); i++) {
            pressure += fluids.get(i).pressure;
        }

        return pressure;
    }

    public double getPressure(ResourceLocation fluid) {
        for (FluidEntry entry : fluids) {
            if (entry.fluid.equals(fluid)) {
                return entry.pressure;
            }
        }

        return 0;
    }

    public List<Integer> getFluidColors() {
        List<Integer> colors = new ArrayList<>();
        for (FluidEntry entry : fluids) {
            colors.add(CelestialFluids.color(entry.fluid));
        }
        return colors;
    }

    @Override
    public void writeToNBT(CompoundTag nbt) {
        ListTag fluidList = new ListTag();
        for (FluidEntry entry : fluids) {
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putString("type", entry.fluid.toString());
            fluidTag.putDouble("percentage", entry.pressure);
            fluidList.add(fluidTag);
        }
        nbt.put("fluids", fluidList);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        fluids = new ArrayList<>();
        ListTag fluidList = nbt.getList("fluids", Tag.TAG_COMPOUND);
        for (int i = 0; i < fluidList.size(); i++) {
            CompoundTag fluidTag = fluidList.getCompound(i);
            ResourceLocation fluid = CelestialFluids.parse(fluidTag.getString("type"));
            double percentage = fluidTag.getDouble("percentage");
            fluids.add(new FluidEntry(fluid, percentage));
        }
    }

    // These methods are for client syncing, the precision loss is intentional
    @Override
    public void writeToBytes(FriendlyByteBuf buf) {
        buf.writeInt(fluids.size());
        for (FluidEntry entry : fluids) {
            buf.writeResourceLocation(entry.fluid);
            buf.writeFloat((float) entry.pressure);
        }
    }

    @Override
    public void readFromBytes(FriendlyByteBuf buf) {
        int size = buf.readInt();
        fluids = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            ResourceLocation fluid = buf.readResourceLocation();
            double percentage = buf.readFloat();
            fluids.add(new FluidEntry(fluid, percentage));
        }
    }
}
