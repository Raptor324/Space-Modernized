package com.space_m.lifesupport;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.space_m.celestial.AstronomyUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code ItemModHeavyBoots}: Stiefel-Mod (oder allein im Stiefelslot getragen), die beim Schleichen in
 * der Luft bei niedriger Schwerkraft zusaetzlich nach unten zieht, bis Overworld-Schwerkraft erreicht ist.
 * Original-Klasse: ItemModHeavyBoots
 *
 * <p>Port: Das Original tickt ueber {@code onArmorTick} (getragen) bzw. {@code modUpdate} (als Mod) auf Client UND
 * Server; HBM ruft {@code modUpdate} nur serverseitig auf, Spielerbewegung ist aber clientseitig. Deshalb laufen
 * beide Wege ueber {@link LifeSupportEvents} (Spieler-Tick, beide Seiten). Getragen: {@link Equipable} (Stiefelslot).
 * Abweichung: die Beine-Darstellung ({@code armorRender}, Textur {@code armor/heavy_boots.png}) entfaellt, weil
 * Vanilla nur {@code ArmorItem} als Ruestung zeichnet und HBMs Mod-Tisch {@code ItemArmorMod} (kein ArmorItem) verlangt.
 */
public class ItemModHeavyBoots extends ItemArmorMod implements Equipable {

    public ItemModHeavyBoots(Properties properties) {
        super(properties, ArmorModificationHelper.boots_only, false, false, false, true);
    }

    @Override
    public void addInformation(ItemStack itemstack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("Increases fall speed in low gravity").withStyle(ChatFormatting.BLUE));
        list.add(Component.literal("Activated by crouching").withStyle(ChatFormatting.BLUE));
        list.add(Component.empty());
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
        super.appendHbmTooltip(stack, level, list, flag);
        list.add(Component.literal("Can be worn on its own!").withStyle(ChatFormatting.GOLD));
        list.add(Component.literal("We take no responsibility for any deaths that may").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        list.add(Component.literal("occur while using these boots without a space suit.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }

    @Override
    public void addDesc(List<Component> list, ItemStack stack, ItemStack armor) {
        list.add(Component.literal("  ").append(stack.getHoverName()).append(" (").append(Component.translatable("armor.fastFall"))
                .append(")").withStyle(ChatFormatting.DARK_PURPLE));
    }

    /** Original {@code onArmorTick}. */
    public void onArmorTick(Level world, Player player, ItemStack stack) {
        // if crouching in air, apply extra gravity until we match the overworld
        if (player.isShiftKeyDown() && !player.onGround() && !player.isInWater()) {
            float gravity = SpaceApi.gravity(player);
            if (gravity > 1.5F) return;
            if (gravity == 0) return;
            if (gravity < 0.2F) gravity = 0.2F;

            Vec3 m = player.getDeltaMovement();
            double motionY = m.y;
            motionY /= 0.98F;
            motionY += (gravity / 20F);
            motionY -= (AstronomyUtil.STANDARD_GRAVITY / 20F);
            motionY *= 0.98F;
            player.setDeltaMovement(m.x, motionY, m.z);
        }
    }

    /** Original {@code isValidArmor(stack, armorType, entity)}: nur Stiefelslot. */
    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.FEET;
    }

    /** Getragen (nicht als Mod): Spieler-Tick in {@link LifeSupportEvents}. */
    public static boolean isWorn(Entity entity, ItemStack stack) {
        return entity instanceof Player p && p.getItemBySlot(EquipmentSlot.FEET) == stack;
    }
}
