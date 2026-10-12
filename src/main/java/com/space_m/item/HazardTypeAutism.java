package com.space_m.item;

import java.util.List;

import com.hbm_m.config.RadiationConfig;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.hazard.type.HazardTypeBase;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code HazardTypeAutism} ({@code HazardRegistry.AUTISM}, am Satelliten {@code sat_gerald}): Uebelkeit
 * {@code ceil(level)} Ticks, ausser mit Lichtschutz am Helm ({@code ArmorRegistry.hasProtection(target, 3, LIGHT)});
 * abschaltbar wie Blendung ({@code disableBlinding}). Tooltip {@code [trait.confusion]}.
 */
public class HazardTypeAutism extends HazardTypeBase {

    public static final HazardTypeAutism AUTISM = new HazardTypeAutism();

    @Override
    public void onUpdate(LivingEntity target, float level, ItemStack stack) {
        if (RadiationConfig.disableBlinding) return;
        if (!ArmorRegistry.hasProtection(target, 3, HazardClass.LIGHT)) {
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, (int) Math.ceil(level), 0));
        }
    }

    @Override
    public void updateEntity(ItemEntity item, float level) { }

    @Override
    public void addHazardInformation(Player player, List<Component> list, float level, ItemStack stack, List<HazardModifier> modifiers) {
        addBracketedTrait(list, "trait.confusion", ChatFormatting.GOLD);
    }
}
