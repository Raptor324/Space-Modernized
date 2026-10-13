package com.space_m.item;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.Level;

/**
 * Original-Klasse: BSword ({@code butter_sword}, {@code ToolMaterial.WOOD}, Vanilla-Kampf-Tab): trifft es einen
 * Tintenfisch, explodiert der mit Staerke 2 (mit Feuer).
 */
public class ItemButterSword extends SwordItem {

    public ItemButterSword(Properties props) {
        //? if < 1.21.1 {
        super(Tiers.WOOD, 3, -2.4F, props);
        //?} else {
        /*super(Tiers.WOOD, props.attributes(SwordItem.createAttributes(Tiers.WOOD, 3, -2.4F)));
        *///?}
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (target instanceof Squid) {
            target.level().explode(attacker, target.getX(), target.getY(), target.getZ(), 2.0F, true, Level.ExplosionInteraction.MOB);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    /** {@code setCreativeTab(CreativeTabs.tabCombat)}. */
    static void addToCombatTab(RegistrySupplier<? extends Item> item) {
        // Loader-Ereignis statt Architectury-modifyBuiltin (stuerzt auf NeoForge ab, siehe SpaceTabFill)
        com.space_m.main.SpaceTabFill.modify(CreativeModeTabs.COMBAT, "ItemButterSword",
                sink -> sink.accept(new ItemStack(item.get()), String.valueOf(item.getId())));
    }
}
