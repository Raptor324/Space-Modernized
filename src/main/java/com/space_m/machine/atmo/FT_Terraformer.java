package com.space_m.machine.atmo;

import java.io.IOException;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm_m.inventory.fluid.trait.FluidTrait;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 NTM Space {@code FT_Terraformer} (Fluid-Trait "terraformer"): Gase, die per Chemikalienwerfer
 * ({@code EntityChemical}) Boden in das Gestein eines Zielkoerpers verwandeln. Ziel = Koerpername des
 * Originals ({@code SolarSystem.Body}, z.B. {@code DUNA}). R4 haengt ihn an {@code space_m:ammonia}.
 *
 * <p>{@link #getTargetBlock()}: Duna -> {@code duna_sands}, Laythe -> {@code laythe_silt}, sonst Stein. Die beiden
 * Planetenbloecke kommen in R3 ({@code space_m:duna_sands}/{@code space_m:laythe_silt}); fehlen sie, gilt Stein.
 * Original-Klasse: FT_Terraformer
 */
public class FT_Terraformer extends FluidTrait {

    static {
        registerTrait("terraformer", FT_Terraformer.class);
    }

    /** Laedt die Klasse (Trait-Name "terraformer" in {@code FluidTrait.traitNameMap}). */
    public static void register() { }

    @Nullable
    private String target;

    public FT_Terraformer() {}

    /** @param target Original-Enumname des Koerpers ({@code SolarSystem.Body.name()}, z.B. "DUNA") */
    public FT_Terraformer(String target) {
        this.target = target;
    }

    @Nullable
    public String getTarget() {
        return target;
    }

    public Block getTargetBlock() {
        if ("DUNA".equals(target)) return block("duna_sands");
        if ("LAYTHE".equals(target)) return block("laythe_silt");
        return Blocks.STONE;
    }

    private static Block block(String name) {
        Block b = BuiltInRegistries.BLOCK.get(com.space_m.SpaceMod.id(name));
        return b == null || b == Blocks.AIR ? Blocks.STONE : b;
    }

    @Override
    public void addInfo(List<Component> info) {
        info.add(Component.literal("[Terraforming]").withStyle(ChatFormatting.AQUA));
    }

    @Override
    public void serializeJSON(JsonWriter writer) throws IOException {
        writer.name("block").value(target);
    }

    @Override
    public void deserializeJSON(JsonObject obj) {
        this.target = obj.get("block").getAsString();
    }
}
