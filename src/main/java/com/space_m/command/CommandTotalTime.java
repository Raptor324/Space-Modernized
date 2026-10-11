package com.space_m.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * Original-Klasse: CommandTotalTime ({@code com.hbm.commands.CommandTotalTime}), Befehl {@code /totaltime set|add <ticks>}
 * (Berechtigung 2). Setzt die <b>Gesamtzeit</b> ({@code getGameTime}, Original {@code getTotalWorldTime}), aus der
 * das Sonnensystem die Bahnpositionen aller Himmelskoerper rechnet - nicht die Tageszeit.
 *
 * <p>Im Original laeuft die Schleife ueber alle Welten, wirkt aber nur auf die Overworld ({@code DerivedWorldInfo}
 * ignoriert {@code incrementTotalWorldTime}, das trotz des Namens den Wert <i>setzt</i>); hier direkt auf
 * {@code overworldData()}. {@code add} setzt Gesamtzeit + Wert.
 */
public final class CommandTotalTime {
    private CommandTotalTime() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("totaltime")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("set")
                        .then(Commands.argument("time", IntegerArgumentType.integer(0))
                                .executes(c -> {
                                    int i = IntegerArgumentType.getInteger(c, "time");
                                    setTime(c.getSource().getServer(), i);
                                    c.getSource().sendSuccess(() -> Component.translatable("commands.time.set", i), true);
                                    return i;
                                })))
                .then(Commands.literal("add")
                        .then(Commands.argument("time", IntegerArgumentType.integer(0))
                                .executes(c -> {
                                    int i = IntegerArgumentType.getInteger(c, "time");
                                    addTime(c.getSource().getServer(), i);
                                    c.getSource().sendSuccess(() -> Component.translatableWithFallback("commands.time.added", "Added %s to the time", i), true);
                                    return i;
                                }))));
    }

    /** Set the time in the server object. */
    public static void setTime(MinecraftServer server, long time) {
        ServerLevelData worldInfo = server.getWorldData().overworldData();
        worldInfo.setGameTime(time);
    }

    /** Adds (or removes) time in the server object. */
    public static void addTime(MinecraftServer server, long time) {
        ServerLevelData worldInfo = server.getWorldData().overworldData();
        worldInfo.setGameTime(worldInfo.getGameTime() + time);
    }
}
