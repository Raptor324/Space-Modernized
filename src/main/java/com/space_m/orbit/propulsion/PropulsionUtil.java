package com.space_m.orbit.propulsion;

import java.util.List;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.util.BobMathUtil;
import com.space_m.fluid.trait.FT_Rocket;
import com.space_m.orbit.IPropulsion;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Gemeinsame Kleinigkeiten des Stationsantriebs (R6-B). Uebersetzungen der 1.7.10-Richtungslogik:
 * {@code ForgeDirection.getOrientation(meta - offset)} = {@link #dir(BlockState)} (Blockstate {@code FACING}, bei den
 * Triebwerken mit {@code getDirModified} schon gedreht, s. {@link #placementFacing}), {@code dir.getRotation(UP)} =
 * {@link Direction#getClockWise()}.
 */
public final class PropulsionUtil {

    private PropulsionUtil() {}

    /** Ein Anschluss (Original {@code DirPos}). */
    public record DirPos(BlockPos pos, Direction dir) {}

    /** Original {@code ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset)}. */
    public static Direction dir(BlockState state) {
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    /** Original {@code dir.getRotation(ForgeDirection.UP)}. */
    public static Direction rot(Direction dir) {
        return dir.getClockWise();
    }

    /**
     * Original {@code BlockDummyable.onBlockPlacedBy} + {@code getDirModified(dir) = dir.getRotation(DOWN)}: die
     * gespeicherte Ausrichtung ist die dem Spieler zugewandte Richtung, gegen den Uhrzeigersinn gedreht.
     */
    public static Direction placementFacing(Direction towardsPlayer) {
        return towardsPlayer.getCounterClockWise();
    }

    /** {@code new DirPos(x + a*dir + b*rot, y + up, z + ...)} */
    public static DirPos at(BlockPos core, Direction dir, int alongDir, int alongRot, int up, Direction face) {
        Direction rot = rot(dir);
        return new DirPos(core.offset(dir.getStepX() * alongDir + rot.getStepX() * alongRot, up,
                dir.getStepZ() * alongDir + rot.getStepZ() * alongRot), face);
    }

    /** Original: {@code FT_Rocket trait = tank.getTankType().getTrait(FT_Rocket.class); isp = trait != null ? trait.getISP() : 300}. */
    public static int isp(FluidTank tank) {
        FT_Rocket trait = FluidType.getTrait(tank.getTankType(), FT_Rocket.class);
        return trait != null ? trait.getISP() : 300;
    }

    /** {@code "&[" + (getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! " + resolveKey(key) + " ! ! !"} */
    public static Component blinkWarning(String key) {
        return Component.literal("! ! ! ").append(Component.translatable(key)).append(" ! ! !")
                .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00));
    }

    /** {@code "&[" + blink + "&]! ! ! " + text + " ! ! !"} (feste englische Texte des Originals). */
    public static Component blinkLiteral(String text) {
        return Component.literal("! ! ! " + text + " ! ! !")
                .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00));
    }

    /** {@code GREEN + "-> " + RESET + tank.getTankType().getLocalizedName() + ": " + fill + "/" + max + "mB"} */
    public static Component tankLine(FluidTank tank, boolean in) {
        MutableComponent line = Component.literal(in ? "-> " : "<- ").withStyle(in ? ChatFormatting.GREEN : ChatFormatting.RED);
        line.append(FluidType.forFluid(tank.getTankType()).getLocalizedName().copy().withStyle(ChatFormatting.RESET));
        line.append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB").withStyle(ChatFormatting.RESET));
        return line;
    }

    /** {@code (power == 0 ? RED : GREEN) + getShortNumber(power) + "HE"} */
    public static Component powerLine(long power) {
        return Component.literal(BobMathUtil.getShortNumber(power) + "HE").withStyle(power == 0 ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    /** {@code RED + " - Insufficient power: needs " + getShortNumber(x) + "HE"} */
    public static void powerError(List<Component> errors, long needed) {
        errors.add(Component.literal(" - Insufficient power: needs " + BobMathUtil.getShortNumber(needed) + "HE").withStyle(ChatFormatting.RED));
    }

    /** {@code RED + " - Insufficient fuel: needs " + fuelCost + "mB"} */
    public static void fuelError(List<Component> errors, int fuelCost) {
        errors.add(Component.literal(" - Insufficient fuel: needs " + fuelCost + "mB").withStyle(ChatFormatting.RED));
    }

    /**
     * Original-Lebenszyklus {@code hasRegistered}: einmalig beim ersten Server-Tick anmelden (nur prograd), bei
     * {@code invalidate}/{@code onChunkUnload} abmelden. In 1.20/1.21 deckt {@code setRemoved} beides ab.
     */
    public static final class Registration {
        private boolean hasRegistered;

        /** @return true, wenn in diesem Tick zum ersten Mal ausgefuehrt (Original setzt dann {@code isOn = false}). */
        public boolean tick(IPropulsion engine, boolean prograde) {
            if (hasRegistered) return false;
            if (prograde) engine.registerPropulsion();
            hasRegistered = true;
            return true;
        }

        public void remove(IPropulsion engine) {
            if (hasRegistered) {
                engine.unregisterPropulsion();
                hasRegistered = false;
            }
        }

        public boolean isRegistered() {
            return hasRegistered;
        }
    }
}
