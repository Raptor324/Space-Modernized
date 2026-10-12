package com.space_m.satellite;

import com.hbm_m.satellite.Satellite;
import com.space_m.celestial.CelestialBody;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code SatelliteWar}: Basisklasse der Kriegssatelliten (Ziel ist ein Himmelskoerper, Wirkung ueber
 * {@code CBT_War}). Selbst nicht registriert (Original: nur {@link SatelliteRailgun} fuer {@code sat_war}).
 * Der Kriegs-Controller (R7) ruft {@link #setTarget(CelestialBody)} je Tick.
 * Original-Klasse: SatelliteWar
 */
public class SatelliteWar extends Satellite implements ISyncedSatellite {

    //time to clean up this shit and make it PROPER.

    /** Client-Geraeusch (Original {@code thePlayer.playSound("hbm:misc.fireflash", 10F, 1F)}); setzt der Client. */
    public static Runnable clientFireSound = () -> { };

    public SatelliteWar() {

    }

    public long lastOp;
    public float interp;
    public int cooldown;
    /** Original {@code SatelliteBase.health} (nie gesetzt/gespeichert, vom War-Controller angezeigt). */
    public int health;

    @Override
    public void writeToNBT(CompoundTag nbt) {
        super.writeToNBT(nbt);
        nbt.putLong("lastOp", lastOp);
    }

    @Override
    public void readFromNBT(CompoundTag nbt) {
        super.readFromNBT(nbt);
        lastOp = nbt.getLong("lastOp");
    }

    public void onClick(Level world, int x, int z) {

    }

    public void fire() {


    }

    public void setTarget(CelestialBody body) {

    }

    public void fireAtTarget(CelestialBody body) {

    }

    /** Nur Client. */
    public void playsound() {
        clientFireSound.run();
    }

    public int magSize() {
        return 0;
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeFloat(interp);

    }

    @Override
    public void deserialize(FriendlyByteBuf buf) {
        this.interp = buf.readFloat();
    }

    @Override
    public String getType() {
        return "WAR";
    }

}
