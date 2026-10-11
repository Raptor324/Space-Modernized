package com.space_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemModInsert;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: ItemModInsert mit {@code withCorrosionProtection()} ({@code insert_cmb}: 9999 HP, -30 % Schaden,
 * -10 % Projektil, -60 % Explosion). Der Korrosionsschutz selbst laeuft ueber den Item-Tag
 * {@code space_m:corrosion_protection} ({@code AtmosphereEffects.getCorrosionProtection}); hier nur die
 * zusaetzliche Tooltipzeile des Originals (nach den Werten, vor der Leerzeile).
 */
public class ItemModInsertCorrosion extends ItemModInsert {

    public ItemModInsertCorrosion(int durability, float damageMod, float projectileMod, float explosionMod, float speed) {
        super(durability, damageMod, projectileMod, explosionMod, speed);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        super.addInformation(stack, level, list);
        Component blank = list.isEmpty() ? null : list.remove(list.size() - 1);
        list.add(Component.literal("Corrosive atmosphere protection").withStyle(ChatFormatting.GOLD));
        if (blank != null) list.add(blank);
    }
}
