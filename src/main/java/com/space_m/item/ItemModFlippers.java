package com.space_m.item;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.powerarmor.ArmorEnvsuit;
import com.hbm_m.powerarmor.ModPowerArmorItem;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Original-Klasse: ItemModFlippers ("flipows"): Stiefel-Mod, im Wasser Schub in Blickrichtung
 * ({@code 0.08 * moveForward}); nicht zusaetzlich zu einem vollstaendigen Envsuit-Satz.
 */
public class ItemModFlippers extends ItemArmorMod {

    public ItemModFlippers(Properties props) {
        super(props, ArmorModificationHelper.boots_only, false, false, false, true);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("Increases swim speed").withStyle(ChatFormatting.BLUE));
        list.add(Component.empty());
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(descLine(ChatFormatting.DARK_PURPLE, stack, " (increased swim speed)"));
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        if (!(entity instanceof Player player)) return;
        // Skip if we already have armor that applies swim speed buff
        if (armor.getItem() instanceof ArmorEnvsuit && ModPowerArmorItem.hasFSBArmor(player)) return;
        if (entity.isInWater()) {
            double mo = 0.08 * player.zza;
            Vec3 vec = entity.getLookAngle().scale(mo);
            entity.setDeltaMovement(entity.getDeltaMovement().add(vec));
        }
    }
}
