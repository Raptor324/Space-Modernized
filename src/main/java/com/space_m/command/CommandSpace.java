package com.space_m.command;

import com.mojang.brigadier.CommandDispatcher;
import com.space_m.celestial.CelestialBody;
import com.space_m.item.SpaceWandD;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/**
 * Abweichung (Nutzerwunsch): {@code /space destroystar} (Berechtigung 2) gibt es im Original nicht. Er macht dasselbe
 * wie {@code wand_d} schleichend in den Himmel ({@link SpaceWandD#toggleStar}): Sonne zerstoeren (Schwarzes Loch,
 * {@code CBT_Destroyed} + {@code degas}) bzw. beim zweiten Aufruf alles zuruecksetzen ("kidding").
 * Wie beim Zauberstab nicht im Orbit (dort setzt der Stab Beton).
 */
public final class CommandSpace {
    private CommandSpace() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("space")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("destroystar")
                        .executes(c -> {
                            CommandSourceStack source = c.getSource();
                            ServerLevel level = source.getLevel();
                            if (CelestialBody.inOrbit(level)) {
                                source.sendFailure(Component.literal("Not available in orbit"));
                                return 0;
                            }
                            return SpaceWandD.toggleStar(level, source::sendSystemMessage) ? 1 : 0;
                        })));
    }
}
