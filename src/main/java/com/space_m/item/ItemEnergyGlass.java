package com.space_m.item;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.item.special.ModConsumables;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: ItemEnergy (nur die NTM-Space-Getraenke mit {@code makeGlass()}: {@code bottle_honey},
 * {@code glass_smilk}, {@code teacup}). 1:1 {@code onEaten}: Stapel -1, FakePlayer -> Explosion 5, Trankuebelkeit 5,
 * Effekt je Getraenk, Behaelter zurueck ({@code glass_empty} / {@code teacup_empty} / Glasflasche). Kein Flaschenoeffner
 * noetig ({@code makeGlass}: {@code requiresOpener = false}), 32 Ticks trinken.
 *
 * <p>Abweichung (technisch noetig): HBM-Modernizeds {@code ItemEnergy} kennt die drei Getraenke nicht (Effekte per
 * {@code this == ModItems.x} fest verdrahtet) - deshalb eigene Klasse im Addon mit identischem Ablauf.
 */
public class ItemEnergyGlass extends Item {

    public enum Kind { HONEY, STRAWBERRY_MILK, TEA }

    private final Kind kind;

    public ItemEnergyGlass(Properties props, Kind kind) {
        super(props);
        this.kind = kind;
    }

    private Item container() {
        return switch (kind) {
            case HONEY -> Items.GLASS_BOTTLE;
            case STRAWBERRY_MILK -> SpaceMaterialItems.GLASS_EMPTY.get();
            case TEA -> SpaceMaterialItems.TEACUP_EMPTY.get();
        };
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity entity) {
        if (!(entity instanceof Player player)) return stack;

        if (!player.getAbilities().instabuild) stack.shrink(1);

        if (!world.isClientSide) {
            if (player.getClass().getSimpleName().contains("FakePlayer")) {
                world.explode(player, player.getX(), player.getY(), player.getZ(), 5F, true, Level.ExplosionInteraction.TNT);
                return stack;
            }

            ModConsumables.applyPotionSickness(player, 5);

            switch (kind) {
                case STRAWBERRY_MILK -> player.heal(6F); //ideas welcome pls thanks
                case TEA -> {
                    player.heal(3F);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30 * 20, 4));
                }
                case HONEY -> {
                    player.heal(9F); //sweet sorrow
                    float digamma = HbmLivingProps.getDigamma(player);
                    HbmLivingProps.setDigamma(player, Math.max(digamma - 0.3F, 0F));
                }
            }

            if (!player.getAbilities().instabuild) {
                if (stack.isEmpty()) return new ItemStack(container());
                player.getInventory().add(new ItemStack(container()));
            }
            player.inventoryMenu.broadcastChanges();
        }
        return stack;
    }

    @Override
    //? if < 1.21.1 {
    public int getUseDuration(ItemStack stack) {
    //?} else {
    /*public int getUseDuration(ItemStack stack, LivingEntity user) {
    *///?}
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (ModConsumables.isPotionSick(player)) return InteractionResultHolder.pass(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }
}
