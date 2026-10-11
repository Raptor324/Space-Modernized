package com.space_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ItemCustomLore;
import com.hbm_m.item.special.ModConsumables;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.space_m.SpaceMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: ItemPill (nur {@code animan}, "Problematic Meal"). {@code ItemPill(0)} = Nahrung 0, immer essbar,
 * 10 Ticks; {@code onFoodEaten}: Trankuebelkeit 5, Klang {@code player.oil}, {@code HbmPotion.run} 10 s (hier
 * {@link SpaceEffects#RUN}), Erbrechen-Partikel (25) samt {@code player.vomit}, Hunger 3 s Stufe 20.
 * Tooltip aus {@code item.animan.desc}.
 */
public class ItemPillAniman extends Item implements ITooltipProvider {

    public ItemPillAniman(Properties props) {
        super(props.food(com.hbm_m.platform.PlatformHooks.foodBuilder(0, 0F)
                //? if < 1.21.1 {
                .alwaysEat()
                //?} else {
                /*.alwaysEdible()
                *///?}
                .build()));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        ItemStack res = super.finishUsingItem(stack, world, entity);
        if (!world.isClientSide && entity instanceof Player player) {
            ModConsumables.applyPotionSickness(player, 5);

            double x = Math.floor(player.getX()), y = Math.floor(player.getY()), z = Math.floor(player.getZ());
            SoundEvent oil = BuiltInRegistries.SOUND_EVENT.get(SpaceMod.id("player.oil"));
            if (oil != null) world.playSound(null, x, y, z, oil, SoundSource.PLAYERS, 1.0F, 1.0F);

            player.addEffect(new MobEffectInstance(SpaceEffects.holder(SpaceEffects.RUN), 10 * 20, 0));

            if (world instanceof ServerLevel sl) {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "vomit");
                nbt.putString("mode", "normal");
                nbt.putInt("count", 25);
                nbt.putInt("entity", player.getId());
                IParticleCreator.sendPacket(sl, x, y, z, 25, nbt);
            }
            world.playSound(null, x, y, z, HbmSoundsNT.get("hbm:player.vomit"), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
        }
        return res;
    }

    @Override
    //? if < 1.21.1 {
    public int getUseDuration(ItemStack stack) {
    //?} else {
    /*public int getUseDuration(ItemStack stack, LivingEntity user) {
    *///?}
        return 10;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!ModConsumables.isPotionSick(player)) player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ItemCustomLore.addLore(getDescriptionId(), tooltip);
    }
}
