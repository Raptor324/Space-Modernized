package com.space_m.machine.mining;

import com.space_m.SpaceMod;

import net.minecraft.gametest.framework.GameTestAssertException;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/** R7-B Dish-Controller: GameTests (Batch {@code space_dish}). */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DishControlGameTest {

    private DishControlGameTest() {}

    static void check(boolean cond, String msg) {
        if (!cond) throw new GameTestAssertException(msg);
    }
}
