package com.space_m.machine.chem.client;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Client-Helfer fuer die {@code printHook}s (nur Client-Seite aufrufen). */
public final class ChemClientUtil {

    private ChemClientUtil() {}

    /** Der angeschaute Block (Original: die {@code x, y, z} des {@code printHook} vor {@code findCore}). */
    @Nullable
    public static BlockPos lookedAtBlock() {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (!(hit instanceof BlockHitResult bhr) || hit.getType() != HitResult.Type.BLOCK) return null;
        return bhr.getBlockPos();
    }
}
