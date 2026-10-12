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
 * Gegenstaende im Spielerinventar (NBT {@code ntmNeutron} += rad / Stapel / 10) und erhoeht die Umgebungsstrahlung
 * (Original {@code ContaminationUtil.contaminate(NEUTRON)}: {@code radEnv += rad * 10}). Tooltip
 * {@code [trait.neutron]} + {@code RAD/s^2}. Wie im Original standardmaessig aus ({@code HAZ_01_disableNeutrons = true}).
 *
 * <p>R9-B: Inventar-Aktivierung, Koerperaktivierung ({@code HbmLivingProps.incrementNeutronActivation}), Zerfall und
 * Strahlung aktivierter Gegenstaende ueber {@link NeutronEffects} (sAPI-Haken, Raptors Strahlungssystem unveraendert).
 */
public class HazardTypeNeutron extends HazardTypeBase {

    public static final String NEUTRON_KEY = "ntmNeutron";

    public static final HazardTypeNeutron NEUTRON = new HazardTypeNeutron();

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        if (com.space_m.config.SpaceConfig.get().disableNeutron) return;
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

            if (target instanceof Player player) {
                for (ItemStack s : player.getInventory().items) apply(s, rad);
                for (ItemStack s : player.getInventory().armor) apply(s, rad);
            }

            // ContaminationUtil.contaminate(target, NEUTRON, CREATIVE, rad): Umgebungswert + Koerperaktivierung (R9-B)
            float radEnv = HbmLivingProps.getRadEnv(target);
            HbmLivingProps.setRadEnv(target, radEnv + rad * 10);
            NeutronEffects.contaminateNeutron(target, rad);
        }
    }

    @Override
    public void updateEntity(ItemEntity item, float level) { }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack, List<HazardModifier> modifiers) {
        level = HazardModifier.evalAllModifiers(stack, player, level, modifiers);
        if (level < 1e-5 || com.space_m.config.SpaceConfig.get().disableNeutron) return;

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
