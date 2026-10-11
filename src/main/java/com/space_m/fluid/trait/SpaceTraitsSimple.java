package com.space_m.fluid.trait;

import java.util.List;

import com.hbm_m.inventory.fluid.trait.FluidTrait;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * Die drei neuen "Tag"-Traits aus NTM Space {@code FluidTraitSimple} ({@code FT_ULTRAKILL}, {@code FT_EXPLOSIVE},
 * {@code FT_Leaded}). Im Original haengen sie an keinem Fluid ({@code Fluids.ULTRAKILL/EXPLOSIVE/LEADED} werden nie
 * benutzt); "uk" und "explosive" stehen am Ende von {@code FluidTrait.traitList}, {@code FT_Leaded} ist nicht
 * registriert (zeigt daher nie einen Tooltip) - beides 1:1.
 */
public final class SpaceTraitsSimple {

    static {
        Registrar.reg("uk", FT_ULTRAKILL.class); // x
        Registrar.reg("explosive", FT_EXPLOSIVE.class); // x
    }

    private SpaceTraitsSimple() {}

    /** Laedt die Klasse (Trait-Namen "uk"/"explosive"). */
    public static void register() { }

    public static class FT_ULTRAKILL extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[ULTRAKILL]").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public static class FT_EXPLOSIVE extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Explosive]").withStyle(ChatFormatting.RED));
        }
    }

    public static class FT_Leaded extends FluidTrait {
        @Override public void addInfoHidden(List<Component> info) {
            info.add(Component.literal("[Leaded Fuel]").withStyle(ChatFormatting.BLUE));
        }
    }

    /** Zugang zu {@code FluidTrait.registerTrait} (protected). */
    private static final class Registrar extends FluidTrait {
        static void reg(String name, Class<? extends FluidTrait> clazz) {
            registerTrait(name, clazz);
        }
    }
}
