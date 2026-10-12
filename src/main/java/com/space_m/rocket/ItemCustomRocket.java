package com.space_m.rocket;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ISatChip;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Fertig montierte Rakete ({@code space_m:rocket_custom}), 1:1 NTM Space {@code ItemCustomRocket}: der Bauplan steht als
 * {@link RocketStruct}-NBT im Stack, dazu {@code hasFuel} (von der Startrampe betankt) und die Satellitenfrequenz.
 * Original-Klasse: ItemCustomRocket
 */
public class ItemCustomRocket extends Item implements ISatChip {

    public ItemCustomRocket(Properties props) {
        super(props);
    }

    public static ItemStack build(RocketStruct rocket) {
        ItemStack stack = new ItemStack(SpaceRocketItems.ROCKET_CUSTOM.get());

        CompoundTag tag = new CompoundTag();
        rocket.writeToNBT(tag);
        PlatformHooks.setItemTag(stack, tag);
        if (rocket.capsule != null && rocket.capsule.part != null && RocketPayloadHooks.get().isSatelliteItem(rocket.capsule.part)) {
            RocketPayloadHooks.get().ensureItemData(stack);
        }

        return stack;
    }

    public static ItemStack build(RocketStruct rocket, boolean hasFuel) {
        ItemStack stack = build(rocket);

        setFuel(stack, hasFuel);

        return stack;
    }

    @Nullable
    public static RocketStruct get(@Nullable ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemCustomRocket)) return null;
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        if (tag == null) return null;

        return RocketStruct.readFromNBT(tag);
    }

    public static boolean hasFuel(@Nullable ItemStack stack) {
        if (stack == null) return false;
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag != null && tag.getBoolean("hasFuel");
    }

    public static void setFuel(@Nullable ItemStack stack, boolean hasFuel) {
        if (stack == null || stack.isEmpty()) return;
        PlatformHooks.editItemTag(stack, t -> t.putBoolean("hasFuel", hasFuel));
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
    *///?}
        RocketStruct rocket = get(stack);

        if (rocket == null) return;

        Component payload = rocket.capsule != null && rocket.capsule.part != null
                ? Component.translatable(rocket.capsule.part.getDescriptionId()) : Component.literal("?");
        list.add(Component.literal("Payload: ").withStyle(ChatFormatting.BOLD).append(payload.copy().withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY))));
        list.add(Component.literal("Stages: ").withStyle(ChatFormatting.BOLD).append(Component.literal("" + rocket.stages.size()).withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY))));

        if (hasFuel(stack)) {
            list.add(Component.literal("Is fully fueled").withStyle(ChatFormatting.GRAY));
        }

        if (getFreq(stack) != 0) {
            list.add(Component.literal("Satellite Frequency: ").withStyle(ChatFormatting.BOLD).append(Component.literal("" + getFreq(stack)).withStyle(s -> s.withBold(false).withColor(ChatFormatting.GRAY))));
        }
    }
}
