package com.space_m.fork;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.MachineSirenBlockEntity.TrackType;
import com.hbm_m.item.machine.ItemCassette;
import com.space_m.item.SpaceEffects;
import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * E11/E12 (Nutzerentscheidung 2026-10-12): Fork-Gegenstaende, die HBM-Modernized nicht hat.
 * <ul>
 *   <li>Kassetten "Ducc" ({@code TrackType.DISEMBODIED}) und "Xbox Live" ({@code SUICIDE}) - Space {@code ItemCassette}.</li>
 *   <li>NITAN-Ambrosia ({@code flask_infusion} Meta NITAN): {@code nitanCount + 1}, Ambrosia 3 min, Resistenz IV 3 min.</li>
 * </ul>
 */
public final class ForkItems {

    private ForkItems() {}

    public static final RegistrySupplier<Item> CASSETTE_DUCC = SpaceItems.register("cassette_ducc",
            () -> new ItemCassette(TrackType.DISEMBODIED, new Item.Properties()), SpaceCreativeTabs.Tab.CONTROL);
    public static final RegistrySupplier<Item> CASSETTE_XBOX_LIVE = SpaceItems.register("cassette_xbox_live",
            () -> new ItemCassette(TrackType.SUICIDE, new Item.Properties()), SpaceCreativeTabs.Tab.CONTROL);

    public static final RegistrySupplier<Item> FLASK_INFUSION_NITAN = SpaceItems.basic("flask_infusion_nitan",
            NitanFlask::new, SpaceCreativeTabs.Tab.CONSUMABLE);

    /** NBT-Schluessel im persistenten Spielerspeicher (Original {@code HbmPlayerProps "nitan"}). */
    public static final String NITAN_KEY = "space_m_nitan";

    public static int getNitanCount(Player player) {
        return player.getPersistentData().getInt(NITAN_KEY);
    }

    public static void setNitanCount(Player player, int count) {
        player.getPersistentData().putInt(NITAN_KEY, count);
    }

    /** Klassenladen vor {@code ITEMS.register()}. */
    public static void init() { }

    /** Space {@code ItemFlask} (Meta NITAN). */
    public static class NitanFlask extends Item {

        public NitanFlask() {
            super(new Item.Properties());
        }

        @Override
        public ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level world, @NotNull LivingEntity entity) {
            if (!(entity instanceof Player player)) return stack;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            if (world.isClientSide) return stack;
            setNitanCount(player, getNitanCount(player) + 1);
            player.addEffect(new MobEffectInstance(SpaceEffects.holder(SpaceEffects.NITAN), 60 * 20 * 3, 0));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60 * 20 * 3, 3));
            return stack;
        }

        @Override
        //? if < 1.21.1 {
        public int getUseDuration(@NotNull ItemStack stack) {
        //?} else {
        /*public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity user) {
        *///?}
            return 32;
        }

        @Override
        public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
            return UseAnim.DRINK;
        }

        @Override
        public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }

        @Override
        //? if < 1.21.1 {
        public void appendHoverText(@NotNull ItemStack stack, Level level, java.util.List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        //?} else {
        /*public void appendHoverText(@NotNull ItemStack stack, Item.TooltipContext ctx, java.util.List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
        *///?}
            list.add(Component.translatable("item.space_m.flask_infusion_nitan.desc"));
        }
    }
}
