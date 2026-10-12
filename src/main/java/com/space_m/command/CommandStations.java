package com.space_m.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.space_m.celestial.CelestialBody;
import com.space_m.dim.SpaceDimensions;
import com.space_m.travel.CelestialTeleporter;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Original-Klasse: CommandStations ({@code com.hbm.commands.CommandStations}), Befehl {@code /ntmstations}
 * (Brigadier statt {@code CommandBase}; Unterbefehle, Meldungen und Teleport-Hoehen 1:1):
 * <pre>
 * /ntmstations launch          - Spawns a station for the held drive.
 * /ntmstations tp              - Teleport to held drive station.
 * /ntmstations list            - Lists all active stations.
 * /ntmstations fetch &lt;id|name&gt; - Creates a drive programmed with a specific station ID or name.
 * </pre>
 * Wie im Original ohne eigene Berechtigungsstufe ({@code CommandBase}-Standard 4 = OP), nur fuer Spieler.
 * Stationen/Laufwerke ueber {@link StationCommandAccess} (bis R6: keine).
 */
public final class CommandStations {
    private CommandStations() {}

    private static StationCommandAccess access = StationCommandAccess.NONE;

    public static void setAccess(StationCommandAccess impl) {
        access = impl != null ? impl : StationCommandAccess.NONE;
    }

    public static StationCommandAccess access() {
        return access;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmstations")
                .requires(s -> s.hasPermission(4))
                .executes(c -> usage(c.getSource()))
                .then(Commands.literal("launch").executes(c -> launch(c.getSource())))
                .then(Commands.literal("tp").executes(c -> tp(c.getSource())))
                .then(Commands.literal("list").executes(c -> list(c.getSource())))
                .then(Commands.literal("fetch")
                        .executes(c -> fetch(c.getSource(), ""))
                        .then(Commands.argument("station", StringArgumentType.greedyString())
                                .executes(c -> fetch(c.getSource(), StringArgumentType.getString(c, "station"))))));
    }

    private static int usage(CommandSourceStack source) {
        if (player(source) == null) return 0;
        String name = "ntmstations";
        source.sendFailure(Component.literal("")
                .append(line(name, "launch ", "- Spawns a station for the held drive.\n"))
                .append(line(name, "tp ", "- Teleport to held drive station.\n"))
                .append(line(name, "list ", "- Lists all active stations.\n"))
                .append(line(name, "fetch <id|name> ", "- Creates a drive programmed with a specific station ID or name.")));
        return 0;
    }

    private static Component line(String name, String sub, String desc) {
        return Component.literal("/" + name + " " + sub).withStyle(ChatFormatting.GREEN)
                .append(Component.literal(desc).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private static ServerPlayer player(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer p) return p;
        showMessage(source, "commands.satellite.should_be_run_as_player", true);
        return null;
    }

    private static int launch(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return 0;

        StationCommandAccess.Destination dest = getStationDestination(source, player.getMainHandItem());
        if (dest == null) return 0;

        access.addStation(dest.x(), dest.z(), CelestialBody.getBody(player.level()));

        showMessage(source, "commands.station.launched", false);
        return 1;
    }

    private static int tp(CommandSourceStack source) {
        ServerPlayer player = player(source);
        if (player == null) return 0;

        StationCommandAccess.Destination destination = getStationDestination(source, player.getMainHandItem());
        if (destination == null) return 0;

        ResourceKey<Level> dimension = destination.dimension();
        boolean orbit = SpaceDimensions.isOrbit(dimension);

        int x = destination.x();
        int z = destination.z();

        if (orbit) {
            int size = access.stationSize();
            x = x * size + (size / 2);
            z = z * size + (size / 2);
        }

        player.stopRiding();

        if (player.level().dimension() != dimension) {
            if (orbit) {
                CelestialTeleporter.teleport(player, dimension, x + 0.5D, 130.0D, z + 0.5D, false);
            } else {
                CelestialTeleporter.teleport(player, dimension, x + 0.5D, 300.0D, z + 0.5D, true);
            }
        } else {
            if (orbit) {
                player.teleportTo(x + 0.5D, 130.0D, z + 0.5D);
            } else {
                int y = player.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                player.teleportTo(x + 0.5D, y + 1, z + 0.5D);
            }
        }

        if (orbit) {
            ServerLevel targetWorld = source.getServer().getLevel(SpaceDimensions.ORBIT);
            if (targetWorld != null) access.spawnStation(targetWorld, x, z);
        }

        showMessage(source, "commands.station.teleported", false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        if (player(source) == null) return 0;

        boolean hasAnyStations = false;
        for (StationCommandAccess.StationInfo station : access.getStations()) {
            if (!station.hasStation()) continue;

            String messageText = stationId(station.dX(), station.dZ());
            if (!station.name().isEmpty()) messageText += " - " + station.name();

            showMessage(source, messageText, false);

            hasAnyStations = true;
        }

        if (!hasAnyStations) {
            showMessage(source, "commands.station.no_stations", true);
        }
        return 1;
    }

    private static int fetch(CommandSourceStack source, String arg) {
        ServerPlayer player = player(source);
        if (player == null) return 0;

        String toMatch = arg.trim();
        if (toMatch.isEmpty()) {
            showMessage(source, "commands.station.invalid_station", true);
            return 0;
        }

        for (StationCommandAccess.StationInfo station : access.getStations()) {
            String stationId = stationId(station.dX(), station.dZ());
            if (station.name().trim().equalsIgnoreCase(toMatch) || stationId.equalsIgnoreCase(toMatch)) {
                ItemStack drive = access.createDrive(station);
                if (!player.getInventory().add(drive)) player.drop(drive, false);

                showMessage(source, "commands.station.drive_created", false);
                return 1;
            }
        }

        showMessage(source, "commands.station.no_match", true);
        return 0;
    }

    /**
     * Stations-ID wie im Original: {@code "0x" + toHexString(new ChunkCoordIntPair(dX, dZ).hashCode()).toUpperCase()}
     * mit dem 1.7.10-Hash (der heutige {@code ChunkPos.hashCode} rechnet anders).
     */
    public static String stationId(int dX, int dZ) {
        int i = 1664525 * dX + 1013904223;
        int j = 1664525 * (dZ ^ -559038737) + 1013904223;
        return "0x" + Integer.toHexString(i ^ j).toUpperCase();
    }

    /** Original {@code showMessage}: Uebersetzungsschluessel (oder Text) in Rot (Fehler) bzw. Gruen. */
    private static void showMessage(CommandSourceStack source, String key, boolean isError) {
        source.sendSystemMessage(Component.translatable(key).withStyle(isError ? ChatFormatting.RED : ChatFormatting.GREEN));
    }

    private static StationCommandAccess.Destination getStationDestination(CommandSourceStack source, ItemStack stack) {
        StationCommandAccess.Destination destination = stack.isEmpty() ? null : access.getDestination(stack);
        if (destination == null) {
            showMessage(source, "commands.station.invalid_drive", true);
        }
        return destination;
    }
}
