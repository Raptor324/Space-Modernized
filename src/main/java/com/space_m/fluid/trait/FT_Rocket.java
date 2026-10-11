package com.space_m.fluid.trait;

import java.io.IOException;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.inventory.fluid.trait.FluidTrait;
import com.hbm_m.util.BobMathUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * 1:1 NTM Space {@code FT_Rocket} (Fluid-Trait "rocket"): Raketentreibstoff mit spezifischem Impuls (ISP) und Schub je
 * Eimer. Genutzt von Transporter-Rampen, Stationstriebwerken (nur ISP) und HTR3 (R5/R6). Tooltip-Texte sind im Original
 * fest englisch.
 *
 * <p>Registrierung im Original direkt hinter "pheromone" in {@code FluidTrait.traitList}; das Addon haengt sie an und
 * sortiert die Liste in {@code SpaceFluids.registerTraits} nach.
 * Original-Klasse: FT_Rocket
 */
public class FT_Rocket extends FluidTrait {

    static {
        registerTrait("rocket", FT_Rocket.class);
    }

    /** Laedt die Klasse (Trait-Name "rocket" in {@code FluidTrait.traitNameMap}). */
    public static void register() { }

    // The ISP of the fuel (aka how long it can go)
    private int isp;

    // The thrust of the fuel (aka how much it can carry)
    private long thrust;

    public FT_Rocket() { }

    public FT_Rocket(int isp, long twr) {
        this.isp = isp;
        this.thrust = twr;
    }

    public int getISP() {
        return this.isp;
    }

    public long getThrust() {
        return this.thrust;
    }

    @Override
    public void addInfo(List<Component> info) {
        super.addInfo(info);

        info.add(Component.literal("[Rocket Grade]").withStyle(ChatFormatting.LIGHT_PURPLE));

        if (isp > 0)
            info.add(Component.literal("Provides ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(BobMathUtil.getShortNumber(isp) + " ISP ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("per bucket").withStyle(ChatFormatting.YELLOW)));

        info.add(Component.literal("[Thrust power]").withStyle(ChatFormatting.RED));

        if (thrust > 0)
            info.add(Component.literal("Provides ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(BobMathUtil.getShortNumber(thrust) + " N ").withStyle(ChatFormatting.RED))
                    .append(Component.literal("of thrust per bucket").withStyle(ChatFormatting.YELLOW)));
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("isp").value(isp);
        writer.name("thrust").value(thrust);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        this.isp = obj.get("isp").getAsInt();
        this.thrust = obj.get("thrust").getAsLong();
    }
}
