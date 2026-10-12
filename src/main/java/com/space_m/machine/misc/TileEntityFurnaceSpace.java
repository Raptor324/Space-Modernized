package com.space_m.machine.misc;

import com.hbm_m.api.space.SpaceApi;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code TileEntityFurnaceSpace} ("Overrides the vanilla furnace TE to check airbreathing"): Vanilla-Ofen,
 * der beim Anzuenden eines neuen Brennstoffs atmet - ohne Luft ({@code TileEntityMachineBase.breatheAir(..., 0)}, hier
 * {@code SpaceApi.breatheAir}) ist die Brenndauer 0, ausser bei Lava (brennt auch ohne Atmosphaere). Der restliche
 * Ablauf (Schmelzen in 200 Ticks, Brennstoff-Behaelter, Erfahrung) ist der Vanilla-Ofen, wie im Original
 * ({@code updateEntity} = Kopie von {@code TileEntityFurnace.updateEntity} mit dieser einen Abfrage); die Abfrage sitzt
 * deshalb in {@link #getBurnDuration}, das Vanilla genau an dieser Stelle aufruft.
 * Original-Klasse: TileEntityFurnaceSpace
 */
public class TileEntityFurnaceSpace extends AbstractFurnaceBlockEntity {

    public TileEntityFurnaceSpace(BlockPos pos, BlockState state) {
        super(MiscBlockEntities.FURNACE_SPACE.get(), pos, state, RecipeType.SMELTING);
    }

    /** Vanilla ruft das nur beim Anzuenden ({@code litTime = getBurnDuration(fuel)}). */
    @Override
    protected int getBurnDuration(ItemStack fuel) {
        int burnTime = super.getBurnDuration(fuel);

        // If the machine can't combust, provide no burning (lava works without an atmosphere though!)
        if (burnTime > 0 && !fuel.is(Items.LAVA_BUCKET) && level != null && !SpaceApi.breatheAir(level, worldPosition, 0)) {
            burnTime = 0;
        }

        return burnTime;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory player) {
        return new FurnaceMenu(id, player, this, this.dataAccess);
    }
}
