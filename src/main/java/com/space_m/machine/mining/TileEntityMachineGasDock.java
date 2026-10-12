package com.space_m.machine.mining;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.util.ParticleUtil;
import com.space_m.celestial.CelestialBody;
import com.space_m.machine.chem.ChemUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityMachineGasDock} (Gasriesen-Sammler): Tanks Gas 64 000 mB (Typ = Gas des Planeten, sonst
 * {@code JOOLGAS}), Wasserstoff 32 000 mB, Sauerstoff 32 000 mB. Steht die Anlage auf einem Koerper, dessen Planet ein
 * Gasriese ist ({@code planet.gas != null}), startet die Sammelrakete, sobald je 500 mB Treibstoff da sind und
 * {@code launchTicks <= -20}: dabei 500 mB H2 + 500 mB O2 -> 8 000 mB Gas (wenn Platz). Nach 100 Ticks Flug landet sie
 * wieder ({@code hasRocket = true}), sinkt 20 Ticks und startet erneut. Gas wird an alle Anschluesse (3x3 darunter, je drei
 * an den Seiten) abgegeben, Treibstoff dort angefordert. Client: Gasflamme unter der Rakete, Stossring in den ersten 10
 * Ticks.
 *
 * <p>Originalfehler 1:1 uebernommen: {@code readFromNBT} ruft {@code setBoolean("hasRocker")} und {@code writeToNBT}
 * {@code getBoolean("hasRocker")} - {@code hasRocket} wird also nie gespeichert und ist nach dem Laden {@code true}
 * (hier: nur ueber das Client-Update uebertragen, beim Laden ignoriert). {@code launchTicks} wird ebenfalls nicht
 * gespeichert.
 * Original-Klasse: TileEntityMachineGasDock
 */
public class TileEntityMachineGasDock extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public FluidTank[] tanks;

    public boolean hasRocket = true;
    public int launchTicks = 0;

    public TileEntityMachineGasDock(BlockPos pos, BlockState state) {
        super(MiningMachines.GAS_DOCK_BE.get(), pos, state, 0, 0L, 0L);
        this.tanks = new FluidTank[3];
        this.tanks[0] = new FluidTank(ChemUtil.fluid("joolgas"), 64_000);
        this.tanks[1] = new FluidTank(ChemUtil.fluid("hydrogen"), 32_000);
        this.tanks[2] = new FluidTank(ChemUtil.fluid("oxygen"), 32_000);
    }

    // ── NBT: Original readFromNBT/writeToNBT ("gas"/"f1"/"f2"), hasRocket nur im Client-Update ──

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        // Original: nbt.setBoolean("hasRocker", hasRocket) - liest nichts
        tanks[0].readFromNBT(nbt, "gas");
        tanks[1].readFromNBT(nbt, "f1");
        tanks[2].readFromNBT(nbt, "f2");

        // Original deserialize (networkPackNT): hasRocket kommt nur beim Client an
        if (level != null && level.isClientSide && nbt.contains("hasRocket")) {
            hasRocket = nbt.getBoolean("hasRocket");
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        // Original: nbt.getBoolean("hasRocker") - schreibt nichts

        tanks[0].writeToNBT(nbt, "gas");
        tanks[1].writeToNBT(nbt, "f1");
        tanks[2].writeToNBT(nbt, "f2");

        // Original serialize (networkPackNT)
        nbt.putBoolean("hasRocket", hasRocket);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasDock be) {
        if (!level.isClientSide) {
            be.updateConnections();

            for (ChemUtil.DirPos con : be.getConPos()) {
                if (be.tanks[0].getFill() > 0) {
                    be.tryProvide(be.tanks[0], level, con.pos(), con.dir());
                }
            }

            CelestialBody planet = CelestialBody.getPlanet(level);

            be.cycle(planet.gas);

            be.setChanged();
            be.sendUpdateToClient(); // networkPackNT(150)
        } else {
            be.launchTicks = Mth.clamp(be.launchTicks + (be.hasRocket ? -1 : 1), be.hasRocket ? -20 : 0, 100);
            if (be.launchTicks > 0 && be.launchTicks < 100) {
                double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
                ParticleUtil.spawnGasFlame(level, x, y + 0.5 + be.launchTicks, z, 0.0, -1.0, 0.0);

                if (be.launchTicks < 10) {
                    com.space_m.machine.mining.client.MiningClientEffects.shock(level, x, y, z, 1 + level.random.nextInt(3), 1 + level.random.nextGaussian());
                }
            }
        }
    }

    /** Server-Teil von {@code updateEntity} ab {@code launchTicks}: Startzyklus, Gastyp und Sammeln. */
    void cycle(@Nullable ResourceLocation gas) {
        launchTicks = Mth.clamp(launchTicks + (hasRocket ? -1 : 1), hasRocket ? -20 : 0, 100);

        if (gas != null) {
            tanks[0].setTankType(gasFluid(gas));

            if (hasFuel() && launchTicks <= -20) {
                hasRocket = false;
                collectGas();
            } else if (launchTicks >= 100) {
                hasRocket = true;
            }
        }
    }

    private static Fluid gasFluid(ResourceLocation gas) {
        Fluid f = BuiltInRegistries.FLUID.get(gas);
        return f == null || f == net.minecraft.world.level.material.Fluids.EMPTY ? ChemUtil.none() : f;
    }

    private void updateConnections() {
        for (ChemUtil.DirPos con : getConPos()) {
            for (int i = 1; i < tanks.length; i++) {
                if (!ChemUtil.isNone(tanks[i].getTankType())) {
                    trySubscribe(tanks[i].getTankType(), level, con.pos(), con.dir());
                }
            }
        }
    }

    private void collectGas() {
        if (tanks[1].getFill() < 500) return;
        if (tanks[2].getFill() < 500) return;
        if (tanks[0].getFill() + 8000 > tanks[0].getMaxFill()) return;

        tanks[1].setFill(tanks[1].getFill() - 500);
        tanks[2].setFill(tanks[2].getFill() - 500);
        tanks[0].setFill(tanks[0].getFill() + 8000);
    }

    private boolean hasFuel() {
        return tanks[1].getFill() >= 500 && tanks[2].getFill() >= 500;
    }

    private ChemUtil.DirPos[] conPos;

    /** Original {@code getConPos}: 3x3 unter dem Kern (NEG_Y) und je drei Stellen zwei Felder seitlich. */
    protected ChemUtil.DirPos[] getConPos() {
        if (conPos == null) {
            List<ChemUtil.DirPos> list = new ArrayList<>();
            BlockPos p = worldPosition;

            // Below
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    list.add(new ChemUtil.DirPos(p.offset(x, -1, z), Direction.DOWN));
                }
            }

            // Sides
            for (int i = -1; i <= 1; i++) {
                list.add(new ChemUtil.DirPos(p.offset(i, 0, 2), Direction.SOUTH));
                list.add(new ChemUtil.DirPos(p.offset(i, 0, -2), Direction.NORTH));
                list.add(new ChemUtil.DirPos(p.offset(2, 0, i), Direction.EAST));
                list.add(new ChemUtil.DirPos(p.offset(-2, 0, i), Direction.WEST));
            }

            conPos = list.toArray(new ChemUtil.DirPos[0]);
        }

        return conPos;
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] {tanks[0]};
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] {tanks[1], tanks[2]};
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.gasDock");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    /** Kein GUI im Original. */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    /** Original {@code getRenderBoundingBox}: x-1..x+2, y..y+1, z-1..z+2. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 1, worldPosition.getZ() + 2);
    }
}
