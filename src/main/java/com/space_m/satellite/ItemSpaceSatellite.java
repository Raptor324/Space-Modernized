package com.space_m.satellite;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ISatChip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Die Space-neuen Satelliten-Gegenstaende ({@code sat_dyson_relay}, {@code sat_war}, {@code sat_precision_laser},
 * {@code sat_detector}, {@code sat_ray_scan}), 1:1 NTM Space {@code ItemSatellite} (Stapel 1, Frequenz {@code freq}).
 * Raketenwerte (Masse, Groesse 2.0 m, Nutzlast SATELLITE) in R5-As Teile-Tabelle ({@code RocketParts}).
 * Original-Klasse: ItemSatellite
 */
public class ItemSpaceSatellite extends Item implements ISatChip {

    public ItemSpaceSatellite(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Original {@code onCreated}: {@code SatelliteBase.ensureItemData}. */
    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        SatelliteItemData.ensureItemData(stack);
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        SatelliteItemLogic.appendTooltip(stack, level, list, true);
    }
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        SatelliteItemLogic.appendTooltip(stack, null, list, true);
    }
    *///?}

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> result = SatelliteItemLogic.use(world, player, stack);
        return result != null ? result : InteractionResultHolder.pass(stack);
    }
}
