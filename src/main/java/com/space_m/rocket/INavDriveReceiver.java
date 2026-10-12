package com.space_m.rocket;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Fahrzeug, das ein Ziel-Laufwerk annimmt (Original: {@code ItemVOTVdrive.onItemRightClick} mit
 * {@code player.ridingEntity instanceof EntityRideableRocket}). R5-B: {@code EntityRideableRocket} implementiert die
 * Original-Logik (Stufen/Orbit/wiederverwendbar, Zustand LANDED/AWAITING, Laufwerk tauschen, Geraeusch).
 */
public interface INavDriveReceiver {

    /** @return Stack, der in der Hand bleibt (altes Laufwerk oder leer); {@code null} = nicht zustaendig. */
    @Nullable
    ItemStack insertNavDrive(Player player, ItemStack held);
}
