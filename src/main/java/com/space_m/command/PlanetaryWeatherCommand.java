package com.space_m.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystemWorldSavedData;
import com.space_m.celestial.trait.CBT_Weather;
import com.space_m.dim.SpaceDimensions;
import com.space_m.time.CelestialWeather;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.TimeArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

/**
 * Port von {@code ModEventHandler.handlePlanetaryWeatherCommand} (Original faengt {@code CommandWeather} in
 * {@code CommandEvent} ab): {@code /weather clear|rain|thunder [Dauer]} wirkt in Himmelskoerper-Dimensionen
 * (Overworld = Kerbin, Planeten/Monde, Orbit = umkreister Koerper) auf den {@link CBT_Weather}-Trait.
 *
 * <p>Umsetzung: der Vanilla-Befehlsbaum wird mit gleichen Knoten neu angemeldet (Brigadier verschmilzt die Knoten
 * und uebernimmt die neuen Ausfuehrer, Berechtigung 2 bleibt). Dimensionen ohne Himmelskoerper (Nether, End,
 * fremde Mods) laufen unveraendert wie Vanilla. Dauer: Vanilla-Argument {@code TimeArgument} (Ticks, "10s"/"1d");
 * ohne Angabe 1:1 Original {@code (300 + rand(600)) * 20} Ticks.
 */
public final class PlanetaryWeatherCommand {
    private PlanetaryWeatherCommand() {}

    private static final SimpleCommandExceptionType NO_WEATHER =
            new SimpleCommandExceptionType(Component.literal("This celestial body has no weather cycle."));

    private enum Kind { CLEAR, RAIN, THUNDER }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("weather")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("clear")
                        .executes(c -> run(c.getSource(), Kind.CLEAR, -1))
                        .then(Commands.argument("duration", TimeArgument.time(1))
                                .executes(c -> run(c.getSource(), Kind.CLEAR, IntegerArgumentType.getInteger(c, "duration")))))
                .then(Commands.literal("rain")
                        .executes(c -> run(c.getSource(), Kind.RAIN, -1))
                        .then(Commands.argument("duration", TimeArgument.time(1))
                                .executes(c -> run(c.getSource(), Kind.RAIN, IntegerArgumentType.getInteger(c, "duration")))))
                .then(Commands.literal("thunder")
                        .executes(c -> run(c.getSource(), Kind.THUNDER, -1))
                        .then(Commands.argument("duration", TimeArgument.time(1))
                                .executes(c -> run(c.getSource(), Kind.THUNDER, IntegerArgumentType.getInteger(c, "duration"))))));
    }

    private static int run(CommandSourceStack source, Kind kind, int time) throws CommandSyntaxException {
        ServerLevel world = source.getLevel();
        boolean orbit = SpaceDimensions.isOrbit(world.dimension());
        if (!orbit && CelestialWeather.celestialBody(world) == null) {
            return vanilla(source, kind, time);
        }

        int duration = time != -1 ? time : (300 + RandomSource.create().nextInt(600)) * 20;

        BlockPos pos = BlockPos.containing(source.getPosition());
        CelestialBody body = CelestialBody.getTarget(world, pos.getX(), pos.getZ()).body;
        CBT_Weather weather = CBT_Weather.ensureTrait(body);
        if (weather == null || !CBT_Weather.supportsWeather(body)) {
            throw NO_WEATHER.create();
        }

        switch (kind) {
            case CLEAR -> {
                weather.forceClear(world.getRandom(), duration);
                source.sendSuccess(() -> Component.translatable("commands.weather.set.clear"), true);
            }
            case RAIN -> {
                weather.forceRain(world.getRandom(), duration);
                source.sendSuccess(() -> Component.translatable("commands.weather.set.rain"), true);
            }
            case THUNDER -> {
                weather.forceThunder(duration);
                source.sendSuccess(() -> Component.translatable("commands.weather.set.thunder"), true);
            }
        }

        SolarSystemWorldSavedData data = SolarSystemWorldSavedData.get();
        if (data != null) data.setDirty();
        return duration;
    }

    /** Vanilla {@code WeatherCommand.setClear/setRain/setThunder} (unveraendert fuer Nicht-Koerper-Dimensionen). */
    private static int vanilla(CommandSourceStack source, Kind kind, int time) {
        ServerLevel overworld = source.getServer().overworld();
        switch (kind) {
            case CLEAR -> {
                overworld.setWeatherParameters(duration(overworld, time, ServerLevel.RAIN_DELAY), 0, false, false);
                source.sendSuccess(() -> Component.translatable("commands.weather.set.clear"), true);
            }
            case RAIN -> {
                overworld.setWeatherParameters(0, duration(overworld, time, ServerLevel.RAIN_DURATION), true, false);
                source.sendSuccess(() -> Component.translatable("commands.weather.set.rain"), true);
            }
            case THUNDER -> {
                overworld.setWeatherParameters(0, duration(overworld, time, ServerLevel.THUNDER_DURATION), true, true);
                source.sendSuccess(() -> Component.translatable("commands.weather.set.thunder"), true);
            }
        }
        return time;
    }

    private static int duration(ServerLevel overworld, int time, IntProvider provider) {
        return time == -1 ? provider.sample(overworld.getRandom()) : time;
    }
}
