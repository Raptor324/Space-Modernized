package com.space_m.time;

import java.util.List;

import com.space_m.celestial.AstronomyUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Original-Klasse: ItemWandTime ({@code com.hbm.items.tool.ItemWandTime}), Item {@code wand_time} ("Time Wand").
 * Rechtsklick auf einen Block verdoppelt {@link AstronomyUtil#TIME_MULTIPLIER} (Bahnen + Tageslaenge laufen
 * schneller), Schleichen halbiert ihn (Minimum 1). Nur Serverseite, Chat-Meldung 1:1.
 */
public class ItemWandTime extends Item {

    public ItemWandTime(Properties properties) {
        super(properties);
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
    *///?}
        list.add(Component.literal("Creative-only item"));
        list.add(Component.literal("\"Wibbly wobbly, timey-wimey... stuff\"").withStyle(ChatFormatting.ITALIC));
        list.add(Component.literal("Probably doesn't work on servers"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (context.getLevel().isClientSide() || player == null) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            AstronomyUtil.TIME_MULTIPLIER /= 2;
            if (AstronomyUtil.TIME_MULTIPLIER < 1) AstronomyUtil.TIME_MULTIPLIER = 1;
        } else {
            AstronomyUtil.TIME_MULTIPLIER *= 2;
        }

        player.sendSystemMessage(Component.literal("Celestial Time Multiplier set to: " + AstronomyUtil.TIME_MULTIPLIER));

        return InteractionResult.SUCCESS;
    }
}
