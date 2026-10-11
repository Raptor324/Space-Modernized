package com.space_m.lifesupport;

import java.util.List;
import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.platform.StackNbt;
import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.atmosphere.IOxygenTank;
import com.space_m.celestial.trait.CBT_Atmosphere;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 NTM Space {@code ItemModOxy} (PLSS): Sauerstofftank als Ruestungsmod (Slot {@code plate_only}, nur Helme).
 * Haelt {@code maxFuel} mB Sauerstoff (NBT {@code fuel}), verbraucht {@code consumption} mB je {@code rate} Ticks,
 * solange die Umgebung nicht atembar ist. NBT {@code ticks} zaehlt die Benutzungsdauer (> 20 = Atemgeraeusch).
 * Original-Klasse: ItemModOxy
 *
 * <p>Den Atem-Check ({@code ArmorUtil.checkForOxy}) und den HUD-Balken macht der Atmosphaeren-Kern
 * ({@code com.space_m.atmosphere}, {@link IOxygenTank}); das Atemgeraeusch (Original {@code modUpdate}, nur Client,
 * nur eigener Spieler) spielt {@link LifeSupportClient}, weil HBM {@code modUpdate} nur serverseitig aufruft.
 */
public class ItemModOxy extends ItemArmorMod implements IFillableItem, IOxygenTank {

    // Quite similar to JetpackBase, but with a few crucial differences meaning we can't subclass

    private final Supplier<Fluid> fuel;
    private final int maxFuel;
    private final int rate;
    private final int consumption;

    public ItemModOxy(Properties properties, Supplier<Fluid> fuel, int maxFuel, int rate, int consumption) {
        super(properties, ArmorModificationHelper.plate_only, true, false, false, false);
        this.maxFuel = maxFuel;
        this.rate = rate;
        this.consumption = consumption;
        this.fuel = fuel;
    }

    static Component fluidName(Fluid fluid) {
        return com.hbm_m.api.fluids.FluidLocalization.nameFromFluidId(
                net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid));
    }

    @Override
    public void addInformation(ItemStack itemstack, @Nullable Level level, List<Component> list) {
        list.add(fluidName(fuel.get()).copy().append(": " + getFuel(itemstack) + "mB / " + this.maxFuel + "mB").withStyle(ChatFormatting.LIGHT_PURPLE));
        list.add(Component.empty());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, list, flag);
        list.add(Component.translatable("armor.mustSeal").withStyle(ChatFormatting.GOLD));
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        // Original ohne schliessende Klammer
        list.add(Component.literal("  ").append(stack.getHoverName()).append(" (").append(fluidName(fuel.get()))
                .append(": " + getFuel(stack) + "mB / " + this.maxFuel + "mB").withStyle(ChatFormatting.RED));
    }

    // returns true if the entity can breathe, either via the contained air, or via the atmosphere itself
    // if contained air is used, it'll be decremented here, this saves on multiple atmosphere checks
    @Override
    public boolean attemptBreathing(LivingEntity entity, ItemStack stack, @Nullable CBT_Atmosphere atmosphere, boolean forceConsume) {
        if (!forceConsume && ChunkAtmosphereManager.proxy.canBreathe(atmosphere)) {
            setInUse(stack, false);
            return true;
        }

        if (entity.tickCount % rate == 0)
            setFuel(stack, Math.max(getFuel(stack) - consumption, 0));

        boolean hasFuel = getFuel(stack) > 0;

        setInUse(stack, hasFuel);

        return hasFuel;
    }

    @Override
    public boolean acceptsFluid(Fluid type, ItemStack stack) {
        return type == fuel.get();
    }

    @Override
    public int tryFill(Fluid type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack))
            return amount;

        int fill = getFuel(stack);
        int toFill = Math.min(amount, maxFuel - fill);

        setFuel(stack, fill + toFill);

        return amount - toFill;
    }

    @Override
    public boolean providesFluid(Fluid type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(Fluid type, int amount, ItemStack stack) {
        return 0;
    }

    @Override
    public @Nullable Fluid getFirstFluidType(ItemStack stack) {
        return null;
    }

    public static boolean getInUse(ItemStack stack) {
        CompoundTag tag = StackNbt.read(stack);
        if (tag == null) return false;
        return tag.getInt("ticks") > 20;
    }

    public static void setInUse(ItemStack stack, boolean inUse) {
        CompoundTag tag = StackNbt.orCreate(stack);
        if (inUse) {
            tag.putInt("ticks", tag.getInt("ticks") + 1);
        } else {
            tag.putInt("ticks", 0);
        }
    }

    @Override
    public int getFuel(ItemStack stack) {
        return fuelOf(stack);
    }

    /** Original {@code static getFuel(stack)}. */
    public static int fuelOf(ItemStack stack) {
        CompoundTag tag = StackNbt.read(stack);
        if (tag == null) return 0;
        return tag.getInt("fuel");
    }

    public static void setFuel(ItemStack stack, int i) {
        StackNbt.orCreate(stack).putInt("fuel", i);
    }

    @Override
    public int getMaxFuel() {
        return maxFuel;
    }

    @Override
    public int getFill(ItemStack stack) {
        return 0;
    }
}
