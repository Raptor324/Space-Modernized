package com.space_m.orbit.propulsion;

import java.util.List;

import com.space_m.celestial.CelestialBody;
import com.space_m.orbit.IPropulsion;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code TileEntityStationPropulsionCreative}: kreativer Stationsantrieb ohne Treibstoff, Schub
 * 10 000 000, je 20 Ticks Start/Stopp; meldet sich im Orbit (ohne Ausrichtungspruefung) bei der Station an.
 * Original-Klasse: TileEntityStationPropulsionCreative
 */
public class TileEntityStationPropulsionCreative extends BlockEntity implements IPropulsion {

    private final PropulsionUtil.Registration registration = new PropulsionUtil.Registration();

    public TileEntityStationPropulsionCreative(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.PROPULSION_CREATIVE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityStationPropulsionCreative be) {
        if (!CelestialBody.inOrbit(level)) return;

        if (!level.isClientSide) {
            be.registration.tick(be, true);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) registration.remove(this);
    }

    public boolean isRegistered() {
        return registration.isRegistered();
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public boolean canPerformBurn(int shipMass, double deltaV) {
        return true;
    }

    @Override
    public void addErrors(List<Component> errors) { }

    @Override
    public float getThrust() {
        return 10_000_000;
    }

    @Override
    public int startBurn() {
        return 20;
    }

    @Override
    public int endBurn() {
        return 20;
    }
}
