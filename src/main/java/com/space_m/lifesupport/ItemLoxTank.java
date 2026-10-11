package com.space_m.lifesupport;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFillableItem;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 NTM Space {@code ItemSyringe} (Zweig {@code lox_tank}): Rechtsklick fuellt den PLSS im Helm (Mod-Slot
 * {@code plate_only}) mit 1000 mB Sauerstoff; nur wenn wirklich etwas hineinging, Klang {@code item.jetpackTank} und
 * ein Tank weniger. Stapel 16.
 * Original-Klasse: ItemSyringe (lox_tank) – HBMs {@code ItemSyringe} kennt nur seine eigenen Typen.
 */
public class ItemLoxTank extends Item implements ITooltipProvider {

    public ItemLoxTank(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);

        if (!helmet.isEmpty()) {
            if (!world.isClientSide) {

                if (ArmorModificationHelper.hasMods(helmet)) {
                    ItemStack tankStack = ArmorModificationHelper.pryMods(helmet)[ArmorModificationHelper.plate_only];
                    if (tankStack == null || tankStack.isEmpty() || !(tankStack.getItem() instanceof IFillableItem fillable))
                        return InteractionResultHolder.pass(stack);

                    Fluid oxygen = ModFluids.OXYGEN.getSource();
                    if (!fillable.acceptsFluid(oxygen, tankStack))
                        return InteractionResultHolder.pass(stack);

                    if (fillable.tryFill(oxygen, 1000, tankStack) < 1000) {
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), HbmSoundsNT.get("hbm:item.jetpackTank"), SoundSource.PLAYERS, 1.0F, 1.0F);
                        stack.shrink(1);
                    }

                    ArmorModificationHelper.applyMod(helmet, tankStack);
                }
            }
        }

        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        list.add(Component.literal("Fills a worn PLSS with 1000mB of oxygen"));
    }
}
