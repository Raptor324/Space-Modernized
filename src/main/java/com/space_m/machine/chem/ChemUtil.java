package com.space_m.machine.chem;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.util.BobMathUtil;
import com.space_m.SpaceMod;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Gemeinsame Helfer der R7-B-Maschinen (Chemie + Bergbau): Original-Ausrichtung ({@code dir}/{@code rot} eines
 * {@code BlockDummyable}), {@code DirPos}, Fluid-Namen des Originals ({@code Fluids.X}) und die Overlay-Zeilen
 * der {@code printHook}s.
 *
 * <p>Ausrichtung: {@code dir = ForgeDirection.getOrientation(meta - offset)} = {@code FACING} des Kerns,
 * {@code dir.getRotation(UP)} = {@link Direction#getClockWise()}, {@code dir.getRotation(DOWN)} =
 * {@link Direction#getCounterClockWise()}.
 */
public final class ChemUtil {

    private ChemUtil() {}

    /** Original {@code DirPos}: Nachbarzelle + Richtung vom Kern zum Rohr/Kabel. */
    public record DirPos(BlockPos pos, Direction dir) {}

    /** Original {@code ForgeDirection.getOrientation(getBlockMetadata() - offset)}. */
    public static Direction dir(BlockState state) {
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    /** Original {@code dir.getRotation(ForgeDirection.UP)}. */
    public static Direction rot(Direction dir) {
        return dir.getClockWise();
    }

    /** {@code new DirPos(x + dir*a + rot*b, y + up, z + dir*a + rot*b, face)} */
    public static DirPos at(BlockPos core, Direction dir, int alongDir, int alongRot, int up, Direction face) {
        Direction rot = rot(dir);
        return new DirPos(core.offset(dir.getStepX() * alongDir + rot.getStepX() * alongRot, up,
                dir.getStepZ() * alongDir + rot.getStepZ() * alongRot), face);
    }

    /** {@code core + dir*a + rot*b + up} */
    public static BlockPos offset(BlockPos core, Direction dir, int alongDir, int alongRot, int up) {
        Direction rot = rot(dir);
        return core.offset(dir.getStepX() * alongDir + rot.getStepX() * alongRot, up,
                dir.getStepZ() * alongDir + rot.getStepZ() * alongRot);
    }

    // ── Fluide ───────────────────────────────────────────────────────────────────────────────

    /** Original {@code Fluids.NONE}. */
    public static Fluid none() {
        return ModFluids.NONE.getSource();
    }

    /** Original {@code type == Fluids.NONE}. */
    public static boolean isNone(@Nullable Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY || fluid == none();
    }

    /**
     * Original {@code Fluids.<NAME>} ueber den Namen (klein): zuerst {@code hbm_m:<name>}, dann {@code space_m:<name>}.
     * {@code earthair} = {@code hbm_m:air} (Master-Name). Unbekannt -> {@link #none()}.
     */
    public static Fluid fluid(String name) {
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.equals("earthair")) n = "air";
        Fluid f = lookup(ResourceLocation.fromNamespaceAndPath("hbm_m", n));
        if (f == null) f = lookup(SpaceMod.id(n));
        return f == null ? none() : f;
    }

    @Nullable
    private static Fluid lookup(ResourceLocation id) {
        if (!BuiltInRegistries.FLUID.containsKey(id)) return null;
        Fluid f = BuiltInRegistries.FLUID.get(id);
        return f == Fluids.EMPTY ? null : f;
    }

    /** Name eines Fluids wie {@code getTankType().getLocalizedName()}. */
    public static Component fluidName(Fluid fluid) {
        return FluidType.forFluid(fluid).getLocalizedName();
    }

    // ── Overlay ({@code printHook}) ──────────────────────────────────────────────────────────

    /** {@code (in ? GREEN "-> " : RED "<- ") + RESET + name + ": " + fill + "/" + max + "mB"} */
    public static Component tankLine(FluidTank tank, boolean in) {
        MutableComponent line = Component.literal(in ? "-> " : "<- ").withStyle(in ? ChatFormatting.GREEN : ChatFormatting.RED);
        line.append(fluidName(tank.getTankType()).copy().withStyle(ChatFormatting.RESET));
        line.append(Component.literal(": " + tank.getFill() + "/" + tank.getMaxFill() + "mB").withStyle(ChatFormatting.RESET));
        return line;
    }

    /** {@code (in ? GREEN "-> " : RED "<- ") + RESET + text} */
    public static Component arrowLine(Component text, boolean in) {
        MutableComponent line = Component.literal(in ? "-> " : "<- ").withStyle(in ? ChatFormatting.GREEN : ChatFormatting.RED);
        line.append(text.copy().withStyle(ChatFormatting.RESET));
        return line;
    }

    /** {@code (red ? RED : GREEN) + "Power: " + getShortNumber(power) + "HE"} */
    public static Component powerLine(long power, boolean red) {
        return Component.literal("Power: " + BobMathUtil.getShortNumber(power) + "HE").withStyle(red ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    /** {@code "&[" + (getBlink() ? 0xff0000 : 0xffff00) + "&]! ! ! " + resolveKey(key) + " ! ! !"} */
    public static Component blinkWarning(String key) {
        return Component.literal("! ! ! ").append(Component.translatable(key)).append(" ! ! !")
                .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00));
    }

    /** Wie {@link #blinkWarning}, fester englischer Text des Originals. */
    public static Component blinkLiteral(String text) {
        return Component.literal("! ! ! " + text + " ! ! !")
                .withStyle(s -> s.withColor(BobMathUtil.getBlink() ? 0xff0000 : 0xffff00));
    }

    /** Gleicher Gegenstand + gleiche Daten (1.20: Tags, 1.21: Komponenten). */
    public static boolean sameItemSameData(net.minecraft.world.item.ItemStack a, net.minecraft.world.item.ItemStack b) {
        //? if < 1.21.1 {
        return net.minecraft.world.item.ItemStack.isSameItemSameTags(a, b);
        //?} else {
        /*return net.minecraft.world.item.ItemStack.isSameItemSameComponents(a, b);
        *///?}
    }

    /** Neue Zeilenliste (Kurzform). */
    public static List<Component> lines() {
        return new ArrayList<>();
    }
}
