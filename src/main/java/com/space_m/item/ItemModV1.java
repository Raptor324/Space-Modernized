package com.space_m.item;

import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hbm_m.armormod.item.IArmorModDash;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.platform.AttributeOps;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: ItemModV1 ("BLOOD IS FUEL"): Brust-Mod, +50 % Tempo (Operation 2), drei Dashes.
 */
public class ItemModV1 extends ItemArmorMod implements IArmorModDash {

    private static final UUID SPEED = UUID.fromString("1d11e63e-28c4-4e14-b09f-fe0bd1be708f");

    public ItemModV1(Properties props) {
        super(props, ArmorModificationHelper.plate_only, false, true, false, false);
    }

    @Override
    public Multimap<
            //? if < 1.21.1 {
            Attribute//?} else {
            /*net.minecraft.core.Holder<Attribute>*///?}
            , AttributeModifier> getModifiers(ItemStack armor) {
        Multimap<
                //? if < 1.21.1 {
                Attribute//?} else {
                /*net.minecraft.core.Holder<Attribute>*///?}
                , AttributeModifier> multimap = HashMultimap.create();
        multimap.put(Attributes.MOVEMENT_SPEED, modifier(SPEED, "V1 SPEED", 0.5, AttributeOps.MULTIPLY_TOTAL));
        return multimap;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("BLOOD IS FUEL").withStyle(ChatFormatting.RED));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.RED, stack, " (BLOOD IS FUEL)"));
    }

    @Override
    public int getDashes() {
        return 3;
    }
}
