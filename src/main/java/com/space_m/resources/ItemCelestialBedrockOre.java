package com.space_m.resources;

import java.util.List;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial.SmeltingBehavior;
import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1:1 NTM Space {@code ItemBedrockOreNew}: Bedrock-Erz je Himmelskoerper-Sorte ({@link CelestialBedrockOre.Type}) und
 * Verarbeitungsstufe ({@link BedrockOreGrade}). Im Original ein Item mit Metadatum {@code grade << 8 | type.index};
 * hier ein Item je Paar ({@code space_m:bedrock_ore_<grade>_<koerper>_<sorte>}, 54 x 26 = 1404), damit die
 * HBM-Rezepttypen (Zentrifuge, Kristallisator, Kombinations-/Pyro-Ofen, Lichtbogenofen, Elektrolyse, Schredder) es
 * ohne NBT-Zutaten erkennen.
 *
 * <p>Name 1:1 {@code getItemStackDisplayName}: Grade-Vorlage mit Sortenname + " (Koerper)"; Tooltip je Trait, bei
 * {@code BASE} die vier Ausbeuten. Icon: Grundtextur {@code bedrock_ore_<praefix>_<sorte>} mit Sortenfarben gebacken
 * (Original {@code RGBMutatorInterpolatedComponentRemap(0xFFFFFF, 0x505050, light, dark)}), Toenung {@link BedrockOreGrade#tint}
 * auf Ebene 0, Overlays je Trait (Original {@code getRenderPasses}).
 */
public class ItemCelestialBedrockOre extends Item implements ITooltipProvider {

    public static final int none = 0xFFFFFF;
    public static final int roasted = 0xCFCFCF;
    public static final int arc = 0xC3A2A2;
    public static final int washed = 0xDBE2CB;

    /** Original {@code ProcessingTrait}. */
    public enum ProcessingTrait {
        ROASTED,
        ARC,
        WASHED,
        CENTRIFUGED,
        SULFURIC,
        SOLVENT,
        RAD;

        /** Lang (HBM-Modernized): {@code item.bedrock_ore.trait.*} des Originals. */
        public String langKey() {
            return "tooltip.hbm_m.bedrock_ore.trait." + name().toLowerCase(Locale.ROOT);
        }
    }

    /** Original {@code BedrockOreGrade} (Reihenfolge = Original-Ordinal). */
    public enum BedrockOreGrade {
        BASE(none, "base"),                                                                     //from the slopper
        BASE_ROASTED(roasted, "base", ProcessingTrait.ROASTED),                                 //optional combination oven step, yields vitriol
        BASE_WASHED(washed, "base", ProcessingTrait.WASHED),                                    //primitive-ass acidizer with water
        PRIMARY(none, "primary", ProcessingTrait.CENTRIFUGED),                                  //centrifuging for more primary
        PRIMARY_ROASTED(roasted, "primary", ProcessingTrait.ROASTED),                           //optional comboven
        PRIMARY_SULFURIC(0xFFFFD3, "primary", ProcessingTrait.SULFURIC),                        //sulfuric acid
        PRIMARY_NOSULFURIC(0xD3D4FF, "primary", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SULFURIC), //from centrifuging, sulfuric byproduct removed
        PRIMARY_SOLVENT(0xD3F0FF, "primary", ProcessingTrait.SOLVENT),                          //solvent
        PRIMARY_NOSOLVENT(0xFFDED3, "primary", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SOLVENT), //solvent byproduct removed
        PRIMARY_RAD(0xECFFD3, "primary", ProcessingTrait.RAD),                                  //radsolvent
        PRIMARY_NORAD(0xEBD3FF, "primary", ProcessingTrait.CENTRIFUGED, ProcessingTrait.RAD),   //radsolvent byproduct removed
        PRIMARY_FIRST(0xFFD3D4, "primary", ProcessingTrait.CENTRIFUGED),                        //higher first material yield
        PRIMARY_SECOND(0xD3FFEB, "primary", ProcessingTrait.CENTRIFUGED),                       //higher second material yield
        CRUMBS(none, "crumbs", ProcessingTrait.CENTRIFUGED),                                    //endpoint for primary, recycling

        SULFURIC_BYPRODUCT(none, "sulfuric", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SULFURIC), //from centrifuging
        SULFURIC_ROASTED(roasted, "sulfuric", ProcessingTrait.ROASTED, ProcessingTrait.SULFURIC),    //comboven again
        SULFURIC_ARC(arc, "sulfuric", ProcessingTrait.ARC, ProcessingTrait.SULFURIC),                //alternate step
        SULFURIC_WASHED(washed, "sulfuric", ProcessingTrait.WASHED, ProcessingTrait.SULFURIC),       //sulfuric endpoint

        SOLVENT_BYPRODUCT(none, "solvent", ProcessingTrait.CENTRIFUGED, ProcessingTrait.SOLVENT),    //from centrifuging
        SOLVENT_ROASTED(roasted, "solvent", ProcessingTrait.ROASTED, ProcessingTrait.SOLVENT),       //comboven again
        SOLVENT_ARC(arc, "solvent", ProcessingTrait.ARC, ProcessingTrait.SOLVENT),                   //alternate step
        SOLVENT_WASHED(washed, "solvent", ProcessingTrait.WASHED, ProcessingTrait.SOLVENT),          //solvent endpoint

        RAD_BYPRODUCT(none, "rad", ProcessingTrait.CENTRIFUGED, ProcessingTrait.RAD),                //from centrifuging
        RAD_ROASTED(roasted, "rad", ProcessingTrait.ROASTED, ProcessingTrait.RAD),                   //comboven again
        RAD_ARC(arc, "rad", ProcessingTrait.ARC, ProcessingTrait.RAD),                               //alternate step
        RAD_WASHED(washed, "rad", ProcessingTrait.WASHED, ProcessingTrait.RAD);                      //rad endpoint

        public final int tint;
        public final String prefix;
        public final ProcessingTrait[] traits;

        BedrockOreGrade(int tint, String prefix, ProcessingTrait... traits) {
            this.tint = tint;
            this.prefix = prefix;
            this.traits = traits;
        }

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private final BedrockOreGrade grade;
    private final CelestialBedrockOre.Type type;

    public ItemCelestialBedrockOre(Properties properties, BedrockOreGrade grade, CelestialBedrockOre.Type type) {
        super(properties);
        this.grade = grade;
        this.type = type;
    }

    public BedrockOreGrade getGrade() {
        return grade;
    }

    public CelestialBedrockOre.Type getOreType() {
        return type;
    }

    /** Original {@code getItemStackDisplayName}. */
    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        Component typeName = Component.translatable("item.hbm_m.bedrock_ore.type." + type.suffix + ".name");
        return Component.translatable("item.hbm_m.bedrock_ore.grade." + grade.key() + ".name", typeName)
                .append(" (").append(Component.translatable("body." + type.body.name)).append(")");
    }

    /** Original {@code addInformation}. */
    @Override
    public void appendHbmTooltip(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> list, @NotNull TooltipFlag flag) {
        for (ProcessingTrait trait : grade.traits) {
            list.add(Component.translatable(trait.langKey()));
        }

        if (grade == BedrockOreGrade.BASE) {
            list.add(Component.literal("Processing outputs").withStyle(ChatFormatting.DARK_GRAY));
            list.add(output("Main: ", type.primary));
            list.add(output("Sulfuric: ", type.byproductAcid));
            list.add(output("Solvent: ", type.byproductSolvent));
            list.add(output("HPS: ", type.byproductRad));
        }
    }

    private static Component output(String label, CelestialBedrockOre.Output o) {
        return Component.literal(label).withStyle(ChatFormatting.DARK_GRAY)
                .append(Component.translatable(o.mat().getUnlocalizedName()).withStyle(ChatFormatting.GRAY));
    }

    // ── statische Helfer 1:1 ItemBedrockOreNew ──

    /** Original {@code make(grade, type, amount)}. */
    public static ItemStack make(BedrockOreGrade grade, CelestialBedrockOre.Type type, int amount) {
        return new ItemStack(CelestialBedrockOreItems.ore(grade, type), amount);
    }

    public static ItemStack make(BedrockOreGrade grade, CelestialBedrockOre.Type type) {
        return make(grade, type, 1);
    }

    /** Original {@code toFluid}: Schmelze nur fuer SMELTABLE, {@code ceil(FRAGMENT.q(amount) * mult)}. */
    @Nullable
    public static MaterialStack toFluid(CelestialBedrockOre.Output o, double amount) {
        if (o.mat() != null && o.mat().smeltable == SmeltingBehavior.SMELTABLE) {
            return new MaterialStack(o.mat(), (int) Math.ceil(MaterialShapes.FRAGMENT.q(o.amount()) * amount));
        }
        return null;
    }

    /** Original {@code extract}: Bedrock-Erz-Fragmente des Materials, hoechstens 64. */
    public static ItemStack extract(CelestialBedrockOre.Output o, double amount) {
        Item fragment = Mats.getItemForShape(o.mat(), MaterialShapes.FRAGMENT);
        if (fragment == null) return ItemStack.EMPTY;
        return new ItemStack(fragment, Math.min((int) Math.ceil(o.amount() * amount), 64));
    }
}
