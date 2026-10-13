package com.space_m.item;

import java.util.List;
import java.util.Random;

import com.hbm_m.config.RadiationConfig;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.hazard.modifier.HazardModifier;
import com.hbm_m.hazard.type.HazardTypeBase;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 1:1 NTM Space {@code HazardTypeGlitch} ({@code HazardRegistry.GLITCH}): Wirkung wie {@link HazardTypeAutism}, Tooltip
 * zeigt zu 10 % "WORLD", sonst alle 0,5 s den Namen eines zufaelligen Gegenstands (Registry-ID; unbelegte ID ->
 * {@code STACKTRACE0x00<n>}).
 *
 * <p>Im Original haengt der Eintrag an {@code bobblehead} Metadatum 25 (Space-Reihenfolge: MICROWAVE). HBM-Modernized
 * fuehrt den Wackelkopf-Typ im Stapel-NBT; angehaengt wird der Eintrag daher ueber den stapelabhaengigen Transformer
 * {@link com.space_m.fork.trinket.ForkTrinketHazards} (HBM-Wackelkopf Typ MICROWAVE).
 */
public class HazardTypeGlitch extends HazardTypeBase {

    public static final HazardTypeGlitch GLITCH = new HazardTypeGlitch();

    static int setSize = 0;

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
        if (player.level().random.nextInt(10) == 0) {
            list.add(Component.literal("WORLD").withStyle(ChatFormatting.DARK_RED));
        } else {
            Random rand = new Random(System.currentTimeMillis() / 500);
            if (setSize == 0) setSize = BuiltInRegistries.ITEM.size();
            int r = rand.nextInt(setSize);
            Item item = BuiltInRegistries.ITEM.byId(r);
            if (item != null && item != Items.AIR) {
                list.add(new ItemStack(item).getHoverName());
            } else {
                list.add(Component.literal("STACKTRACE0x00" + r).withStyle(ChatFormatting.RED));
            }
        }
    }
}
