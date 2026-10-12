package com.space_m.resources;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.BedrockOreHooks;
import com.hbm_m.item.tool.ItemOreDensityScanner;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * W-R8B-1: NTM Space {@code CelestialBedrockOre} in den HBM-Stellen fuer {@code hbm_m:bedrock_ore_base}:
 * <ul>
 *   <li>Bagger: {@code ItemBedrockOreBase.setOreAmount(world, ...)} (Koerper + Sorten des Koerpers),</li>
 *   <li>Tooltip: "Mined on: Koerper" + Sorten des Koerpers (Original {@code addInformation}),</li>
 *   <li>Erzschlammer: Mengen je Sorte des Abbau-Koerpers,</li>
 *   <li>Erzdichtescanner: Sorten des Koerpers, Bohrsaeure des Koerpers ({@code getBedrockAcid}).</li>
 * </ul>
 *
 * <p>Abweichung (begruendet): das Original legt je Sorte 26 Verarbeitungsstufen an ({@code ItemBedrockOreNew}, 54 Sorten
 * x 26 Grade, Rezepte in Zentrifuge/Kristallisator/Elektrolyse/Lichtbogen/Pyro-/Kombinationsofen). Diese Items gibt es im
 * Addon noch nicht; bis dahin gibt der Erzschlammer die HBM-Grundstufe derselben Sortenart aus
 * ({@code hbm_m:bedrock_ore_base_<suffix>}, light/heavy/rare/actinide/nonmetal/crystal) - deren Weiterverarbeitung liefert
 * die HBM-(Master-)Ausbeute. Sorten ohne HBM-Gegenstueck (schrabidic, hazard, plastic) werden nicht geschlaemmt.
 */
public final class SpaceBedrockOreHooks implements BedrockOreHooks {

    public static final SpaceBedrockOreHooks INSTANCE = new SpaceBedrockOreHooks();

    private SpaceBedrockOreHooks() {}

    @Override
    public boolean setOreAmount(ServerLevel level, ItemStack stack, int x, int z, double mult) {
        CelestialBedrockOre.setOreAmount(level, stack, x, z, mult);
        return true;
    }

    @Override
    public boolean appendBaseOreTooltip(ItemStack stack, List<Component> tooltip) {
        SolarSystem.Body body = CelestialBedrockOre.getOreBody(stack);
        tooltip.add(Component.literal("Mined on: ").append(Component.translatable("body." + body.name)));
        CelestialBedrockOre ore = CelestialBedrockOre.get(body);
        if (ore == null) return true;
        for (CelestialBedrockOre.Type type : ore.types) {
            double amount = CelestialBedrockOre.getOreAmount(stack, type);
            tooltip.add(typeName(type)
                    .append(": " + ((int) (amount * 100)) / 100D + " (")
                    .append(Component.translatable(ItemOreDensityScanner.translateDensity(amount)).withStyle(ItemOreDensityScanner.getColor(amount)))
                    .append(Component.literal(")").withStyle(ChatFormatting.GRAY)));
        }
        return true;
    }

    @Nullable
    @Override
    public Map<ResourceLocation, Double> oreAmounts(ItemStack stack) {
        CelestialBedrockOre ore = CelestialBedrockOre.get(CelestialBedrockOre.getOreBody(stack));
        if (ore == null) return null;
        Map<ResourceLocation, Double> out = new LinkedHashMap<>();
        for (CelestialBedrockOre.Type type : ore.types) {
            if (baseItem(type) == null) continue; // keine Verarbeitungsstufe vorhanden (s. Klassenkommentar)
            out.put(type.id(), CelestialBedrockOre.getOreAmount(stack, type));
        }
        return out;
    }

    @Override
    public ItemStack slopperOutput(ResourceLocation oreType) {
        CelestialBedrockOre.Type type = CelestialBedrockOre.byId(oreType);
        Item item = type != null ? baseItem(type) : null;
        return item != null ? new ItemStack(item) : ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public List<ScanLine> scan(ServerLevel level, int x, int z) {
        CelestialBedrockOre ore = CelestialBedrockOre.get(CelestialBedrockOre.bodyOf(level));
        if (ore == null) return null;
        List<ScanLine> lines = new ArrayList<>();
        for (CelestialBedrockOre.Type type : ore.types) {
            lines.add(new ScanLine(typeName(type), CelestialBedrockOre.getOreLevel(level, x, z, type), type.index));
        }
        return lines;
    }

    @Override
    public int scanSummaryIndex() {
        return CelestialBedrockOre.getTotalTypeCount();
    }

    @Nullable
    @Override
    public BoreFluid bedrockAcid(Level level) {
        CelestialBody body = CelestialBody.getBodyOrNull(level.dimension());
        if (body == null) return null;
        CelestialBedrockOreFeature.Acid acid = CelestialBedrockOreFeature.bedrockAcid(body);
        return acid == null ? null : new BoreFluid(acid.fluid(), acid.amount());
    }

    private static net.minecraft.network.chat.MutableComponent typeName(CelestialBedrockOre.Type type) {
        return Component.translatable("item.hbm_m.bedrock_ore.type." + type.suffix + ".name");
    }

    /** HBM-Grundstufe derselben Sortenart ({@code hbm_m:bedrock_ore_base_<suffix>}), sonst null. */
    @Nullable
    private static Item baseItem(CelestialBedrockOre.Type type) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "bedrock_ore_base_" + type.suffix));
        return item == Items.AIR ? null : item;
    }

    /** Im Mod-Konstruktor (Client + Server). */
    public static void install() {
        com.hbm_m.api.space.SpaceApi.setBedrockOreHooks(INSTANCE);
    }
}
