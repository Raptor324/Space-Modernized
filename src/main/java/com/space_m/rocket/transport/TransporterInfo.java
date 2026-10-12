package com.space_m.rocket.transport;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.CelestialBody;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code ItemTransporterLinker.TransporterInfo}: leicht vergleichbare Transporter-Beschreibung (Name,
 * Himmelskoerper-Textur, verknuepfter Partner) fuer HashSets und Client/Server-Austausch.
 *
 * <p>Abweichung (technisch noetig): die int-Dimensions-ID des Originals ist eine {@code ResourceKey<Level>}-Kennung
 * ({@link #dimension}); NBT-Schluessel {@code "dimension"} (String) statt {@code "dimensionId"} (int).
 * Original-Klasse: TransporterInfo
 */
public class TransporterInfo {

    public String name;
    @Nullable public ResourceLocation planet;
    @Nullable public TransporterInfo linkedTo;

    // Comparables
    public ResourceLocation dimension;
    public int x, y, z;

    public TransporterInfo(String name, ResourceLocation dimension, int x, int y, int z) {
        this.name = name;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;

        CelestialBody body = CelestialBody.getBody(dimension);
        planet = body != null ? body.texture : null;
    }

    public static TransporterInfo from(ResourceLocation dimension, TransporterBlockEntityBase transporter) {
        BlockPos pos = transporter.getBlockPos();
        TransporterInfo info = new TransporterInfo(transporter.getTransporterName(), dimension, pos.getX(), pos.getY(), pos.getZ());
        info.linkedTo = transporter.getLinkedTransporter();
        return info;
    }

    public ResourceKey<Level> levelKey() {
        return ResourceKey.create(Registries.DIMENSION, dimension);
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }

    public void writeToNBT(CompoundTag nbt) {
        writeToNBT(nbt, true);
    }

    private void writeToNBT(CompoundTag nbt, boolean recurse) {
        nbt.putString("name", name);
        nbt.putString("dimension", dimension.toString());
        nbt.putInt("x", x);
        nbt.putInt("y", y);
        nbt.putInt("z", z);

        if (recurse && linkedTo != null) {
            CompoundTag linked = new CompoundTag();
            linkedTo.writeToNBT(linked, false);
            nbt.put("linked", linked);
        }
    }

    public static TransporterInfo readFromNBT(CompoundTag nbt) {
        ResourceLocation dim = ResourceLocation.tryParse(nbt.getString("dimension"));
        if (dim == null) dim = Level.OVERWORLD.location();
        TransporterInfo info = new TransporterInfo(nbt.getString("name"), dim, nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
        if (nbt.contains("linked")) {
            info.linkedTo = readFromNBT(nbt.getCompound("linked"));
        }
        return info;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, x, y, z);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.getClass() != other.getClass()) return false;
        TransporterInfo info = (TransporterInfo) other;
        return dimension.equals(info.dimension)
            && x == info.x
            && y == info.y
            && z == info.z;
    }
}
