package com.space_m.deco;

import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/**
 * {@code fence_gate} = {@code com.hbm.items.tool.ItemModDoor} (blockTab, Stapel 1, Textur {@code fence_metal}): setzt
 * {@code space_m:fence_gate} als zweihohe Tuer, nur beim Klick auf eine Oberseite ({@code side == 1}); Blickrichtung
 * und Scharnierseite wie {@code placeDoorBlock} (= Vanilla-Tuerlogik von {@code DoorBlock.getStateForPlacement}).
 * Name {@code item.space_m.fence_gate} (Original {@code item.fence_gate.name}).
 */
public class ItemFenceGate extends DoubleHighBlockItem {

    public ItemFenceGate(Block block, Properties props) {
        super(block, props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getClickedFace() != Direction.UP) return InteractionResult.FAIL;
        return super.useOn(ctx);
    }

    @Override
    public String getDescriptionId() {
        return Util.makeDescriptionId("item", BuiltInRegistries.ITEM.getKey(this));
    }
}
