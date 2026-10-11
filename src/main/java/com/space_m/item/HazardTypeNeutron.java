package com.space_m.item;

import java.util.List;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.hazard.type.HazardTypeBase;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.ContaminationUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code HazardTypeNeutron} ({@code HazardRegistry.NEUTRON}): Neutronenstrahlung aus Curium/Californium/
 * Einsteinium. Pro Tick {@code rad = level * Stapel / 20 * Strahlungsmod} (Reacher wie bei Strahlung), aktiviert die
 * Gegenstaende im Spielerinventar (NBT {@code ntmNeutron} += rad / Stapel / 10, hier vorerst aus) und erhoeht die Umgebungsstrahlung
 * (Original {@code ContaminationUtil.contaminate(NEUTRON)}: {@code radEnv += rad * 10}). Tooltip
 * {@code [trait.neutron]} + {@code RAD/s^2}.
 *
 * <p>Abweichung (technisch noetig, Wunsch W-R4b-2): die Neutronenaktivierung des Koerpers
 * ({@code HbmLivingProps.incrementNeutronActivation}) und die Strahlung aktivierter Gegenstaende
 * ({@code HazardTransformerRadiationNBT}) gibt es in HBM-Modernized nicht; Raptors Strahlungssystem wird nicht
 * veraendert. Bis dahin wirkt nur der Umgebungsanteil.
 */
public class HazardTypeNeutron extends HazardTypeBase {

    public static final String NEUTRON_KEY = "ntmNeutron";

    public static final HazardTypeNeutron NEUTRON = new HazardTypeNeutron();

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        boolean reacher = false;
        if (target instanceof Player player && !com.hbm_m.config.GeneralConfig.enable528)
            reacher = player.getInventory().contains(new ItemStack(ModItems.REACHER.get()));

        level *= stack.getCount();

        if (level > 0) {
            float rad = (level / 20F) * ContaminationUtil.calculateRadiationMod(target);

            if (com.hbm_m.config.GeneralConfig.enable528 && reacher) {
                rad = rad / 49F;
            } else if (reacher) {
                rad = (float) BobMathUtil.squirt(rad);
            }

            // Original: Inventar-Aktivierung (apply) - erst mit HBM-Gegenstueck (W-R4b-2), sonst nur NBT-Ballast,
            // der Stapel trennt und nirgends wirkt.

            float radEnv = HbmLivingProps.getRadEnv(target);
            HbmLivingProps.setRadEnv(target, radEnv + rad * 10);
        }
    }

    @Override
    public void updateEntity(ItemEntity item, float level) { }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack, List<HazardModifier> modifiers) {
        level = HazardModifier.evalAllModifiers(stack, player, level, modifiers);
        if (level < 1e-5) return;

        addBracketedTrait(list, "trait.neutron", ChatFormatting.BLUE);
        String neut = "" + (Math.floor(level * 1000) / 1000);
        list.add(Component.literal(neut + "RAD/s^2").withStyle(ChatFormatting.LIGHT_PURPLE));
        if (stack.getCount() > 1) {
            list.add(Component.literal("Stack: " + (Math.floor(level * 1000 * stack.getCount()) / 1000) + "RAD/s^2")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void apply(ItemStack stack, float rad) {
        if (stack == null || stack.isEmpty()) return;
        PlatformHooks.editItemTag(stack, t -> t.putFloat(NEUTRON_KEY, t.getFloat(NEUTRON_KEY) + (rad / stack.getCount()) / 10));
    }
}
