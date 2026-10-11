package com.space_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: ItemMineralOre ({@code mineral_fragment}, 6 Metadaten {@code clump_peroxide} ... {@code clump_schrabidic};
 * Name {@code item.<typ>}). Je Metadatum ein Item; Tooltip-Zeile der Saeure wie im Original.
 */
public class ItemMineralClump extends Item implements ITooltipProvider {

    public ItemMineralClump(Properties props) {
        super(props);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        switch (BuiltInRegistries.ITEM.getKey(this).getPath()) {
            case "clump_peroxide" -> list.add(Component.literal("[Peroxide]").withStyle(ChatFormatting.YELLOW));
            case "clump_nitric" -> list.add(Component.literal("[Nitric]").withStyle(ChatFormatting.GOLD));
            case "clump_sulfuric" -> list.add(Component.literal("[Sulfuric]").withStyle(ChatFormatting.YELLOW));
            case "clump_solvent" -> list.add(Component.literal("[Solvent]").withStyle(ChatFormatting.RED));
            case "clump_hydrochloric" -> list.add(Component.literal("[Chloric]").withStyle(ChatFormatting.GREEN));
            case "clump_schrabidic" -> list.add(Component.literal("[Schrabidic]").withStyle(ChatFormatting.BLUE));
            default -> { }
        }
    }
}
