package com.space_m.fork.trinket;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.hbm_m.item.TrinketBlockItem;
import com.space_m.fork.trinket.ForkTrinkets.ForkPlushieType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Gegenstand zu {@link ForkTrinketBlock} (Original {@code ItemBlockMeta}/{@code ItemBlockBase}): Typ als NBT
 * {@code type} (HBM {@link TrinketBlockItem#make}); Plueschtier heisst "%s Plushie" und zeigt seine Aufschrift.
 */
public class ForkTrinketItem extends BlockItem {

    public ForkTrinketItem(Block block, Properties properties) {
        super(block, properties);
    }

    private ForkTrinketBlock.Kind kind() {
        return ((ForkTrinketBlock) getBlock()).kind;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (kind() == ForkTrinketBlock.Kind.PLUSHIE) {
            ForkPlushieType type = ForkTrinkets.safe(ForkPlushieType.class, TrinketBlockItem.getType(stack));
            return Component.translatable(getDescriptionId(stack), type == ForkPlushieType.NONE ? "" : type.label);
        }
        return super.getName(stack);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
    *///?}
        if (kind() == ForkTrinketBlock.Kind.PLUSHIE) {
            ForkPlushieType type = ForkTrinkets.safe(ForkPlushieType.class, TrinketBlockItem.getType(stack));
            if (type.inscription != null) tooltip.add(Component.literal(type.inscription));
        }
    }

    //? if forge {
    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.fork.trinket.client.ForkTrinketItemRenderer.instance();
            }
        });
    }
    //?} elif neoforge {
    /*@SuppressWarnings("removal")
    @Override
    public void initializeClient(Consumer<net.neoforged.neoforge.client.extensions.common.IClientItemExtensions> consumer) {
        consumer.accept(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return com.space_m.fork.trinket.client.ForkTrinketItemRenderer.instance();
            }
        });
    }
    *///?}
}
