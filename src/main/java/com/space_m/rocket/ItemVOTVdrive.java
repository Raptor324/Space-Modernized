package com.space_m.rocket;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.PlatformHooks;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.config.SpaceConfig;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

/**
 * Ziel-Laufwerk ({@code space_m:hard_drive_full}), 1:1 NTM Space {@code ItemVOTVdrive}: ein Laufwerk je Himmelskoerper
 * ({@link SolarSystem.Body}) bzw. Orbit-Station, mit Landekoordinaten, "verarbeitet"-Status und Stationsdaten.
 *
 * <p>Abweichung (technisch): Die 1.7.10-Metadaten ({@code ItemEnumMulti}, Meta = {@code Body.ordinal()}) stehen als NBT
 * {@code "body"}; Stacks fuer einen Koerper liefert {@link #of(SolarSystem.Body)}. Das Symbol (Grundbild + Koerper-Overlay
 * {@code votv.<name>}) waehlt das Item-Modell ueber die Eigenschaft {@code space_m:body}.
 * Original-Klasse: ItemVOTVdrive
 */
public class ItemVOTVdrive extends Item {

    private static final RandomSource itemRand = RandomSource.create();

    public ItemVOTVdrive(Properties props) {
        super(props.stacksTo(1));
    }

    public static ItemStack of(SolarSystem.Body body) {
        ItemStack stack = new ItemStack(SpaceRocketItems.FULL_DRIVE.get());
        PlatformHooks.editItemTag(stack, t -> t.putInt("body", body.ordinal()));
        return stack;
    }

    /** Creative-Tab: je Koerper ein verarbeitetes Laufwerk (Original {@code getSubItems}: x=1, ax=1, Processed). */
    public static List<ItemStack> creativeStacks() {
        List<ItemStack> list = new ArrayList<>();
        for (SolarSystem.Body body : SolarSystem.Body.values()) {
            ItemStack stack = of(body);
            PlatformHooks.editItemTag(stack, t -> {
                t.putInt("x", 1);
                t.putInt("ax", 1);
                t.putBoolean("Processed", true);
            });
            list.add(stack);
        }
        return list;
    }

    private static CompoundTag tag(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag != null ? tag : new CompoundTag();
    }

    //? if < 1.21.1 {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {
    //?} else {
    /*@Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        Level level = com.space_m.rocket.client.RocketClient.clientLevel();
    *///?}
        Destination destination = getDestination(stack);
        if (destination == null || level == null) return;

        if (destination.body() == SolarSystem.Body.ORBIT) {
            String identifier = tag(stack).getString("stationName");

            if (identifier.isEmpty()) identifier = "0x" + Integer.toHexString(chunkHash(destination.x(), destination.z())).toUpperCase();

            list.add(Component.literal("Destination: ORBITAL STATION"));
            list.add(Component.literal("Station: " + identifier));

            if (!level.dimension().equals(destination.body().getDimension())) {
                for (String s : Component.translatable("item.hard_drive_full.orbit.desc").getString().split("\\$")) {
                    list.add(Component.literal(s).withStyle(ChatFormatting.GOLD));
                }
            }

            return;
        }

        int processingLevel = destination.body().getProcessingLevel(CelestialBody.getBody(level));

        list.add(Component.literal("Destination: ").append(Component.translatable("body." + destination.body().name).withStyle(ChatFormatting.AQUA)));

        if (destination.x() == 0 && destination.z() == 0) {
            list.add(Component.literal("Needs destination coordinates!").withStyle(ChatFormatting.GOLD));
        } else if (!getProcessed(stack)) {
            // Display processing level info if not processed
            list.add(Component.literal("Process requirement: Level " + processingLevel));
            list.add(Component.literal("Needs processing!").withStyle(ChatFormatting.GOLD));
            list.add(Component.literal("Target coordinates: " + destination.x() + ", " + destination.z()));
        } else {
            // Display destination info if processed
            list.add(Component.literal("Processed!").withStyle(ChatFormatting.GREEN));
            list.add(Component.literal("Target coordinates: " + destination.x() + ", " + destination.z()));
        }

        if (level.dimension().equals(destination.body().getDimension())) {
            for (String s : Component.translatable("item.hard_drive_full.surface.desc").getString().split("\\$")) {
                list.add(Component.literal(s).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** Original {@code new ChunkCoordIntPair(x, z).hashCode()} (1.7.10-Formel). */
    private static int chunkHash(int x, int z) {
        int i = 1664525 * x + 1013904223;
        int j = 1664525 * (z ^ -559038737) + 1013904223;
        return i ^ j;
    }

    public static SolarSystem.Body getBody(ItemStack stack) {
        int meta = tag(stack).getInt("body");
        return SolarSystem.Body.values()[Math.floorMod(meta, SolarSystem.Body.values().length)];
    }

    @Nullable
    public static Destination getDestination(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;

        CompoundTag tag = tag(stack);
        SolarSystem.Body body = getBody(stack);
        int x = tag.getInt("x");
        int z = tag.getInt("z");
        return new Destination(body, x, z);
    }

    public static Target getTarget(@Nullable ItemStack stack, Level world) {
        if (stack == null || stack.isEmpty()) {
            return new Target(null, false, false);
        }

        Destination destination = getDestination(stack);

        if (destination.body() == SolarSystem.Body.ORBIT) {
            CompoundTag tag = tag(stack);
            if (world.isClientSide) {
                CelestialBody body = CelestialBody.getBodyOrNull(tag.getString("sDim"));
                if (body == null) body = CelestialBody.getBody(world);
                boolean hasStation = tag.getBoolean("sHas");

                return new Target(body, true, hasStation);
            }

            RocketStationHooks hooks = RocketStationHooks.get();
            boolean hasStation = hooks.hasStation(destination.x(), destination.z());
            CelestialBody orbiting = hooks.orbiting(world, destination.x(), destination.z());
            if (!hasStation || orbiting == null) orbiting = CelestialBody.getBody(world);
            String name = hooks.stationName(destination.x(), destination.z());
            CelestialBody finalOrbiting = orbiting;

            // The client can't get this information, so any time the server grabs it, serialize it to the itemstack
            CompoundTag old = tag(stack);
            if (!name.equals(old.getString("stationName")) || !finalOrbiting.name.equals(old.getString("sDim")) || hasStation != old.getBoolean("sHas")) {
                PlatformHooks.editItemTag(stack, t -> {
                    t.putString("stationName", name);
                    t.putString("sDim", finalOrbiting.name); // Original: Dimensions-ID; hier Koerpername
                    t.putBoolean("sHas", hasStation);
                });
            }

            return new Target(orbiting, true, hasStation);
        } else {
            return new Target(destination.body().getBody(), false, true);
        }
    }

    public static void setCoordinates(ItemStack stack, int x, int z) {
        PlatformHooks.editItemTag(stack, t -> {
            t.putInt("x", x);
            t.putInt("z", z);
        });
    }

    public static int getProcessingTier(ItemStack stack, CelestialBody from) {
        SolarSystem.Body body = getBody(stack);
        return body.getProcessingLevel(from);
    }

    public static boolean getProcessed(ItemStack stack) {
        return tag(stack).getBoolean("Processed");
    }

    public static void setProcessed(ItemStack stack, boolean processed) {
        PlatformHooks.editItemTag(stack, t -> t.putBoolean("Processed", processed));
    }

    // Returns an area for the Stardar to draw, so the player can pick a safe spot to land
    public static Destination getApproximateDestination(ItemStack stack) {
        SolarSystem.Body body = getBody(stack);
        CompoundTag tag = tag(stack);
        if (!tag.contains("ax") || !tag.contains("az")) {
            int max = SpaceConfig.get().maxProbeDistance;
            int ax = itemRand.nextInt(max * 2) - max;
            int az = itemRand.nextInt(max * 2) - max;
            PlatformHooks.editItemTag(stack, t -> {
                t.putInt("ax", ax);
                t.putInt("az", az);
            });
            tag = tag(stack);
        }
        return new Destination(body, tag.getInt("ax"), tag.getInt("az"));
    }

    public static void markCopied(ItemStack stack) {
        PlatformHooks.editItemTag(stack, t -> t.putBoolean("copied", true));
    }

    public static boolean wasCopied(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag != null && tag.getBoolean("copied");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isProcessed = getProcessed(stack);
        boolean onDestination = world.dimension().equals(getBody(stack).getDimension());

        // If we're on the body (or in creative), immediately process
        if (!isProcessed && (player.getAbilities().instabuild || onDestination)) {
            isProcessed = true;
            setProcessed(stack, true);
        }

        ItemStack newStack = stack;

        if (isProcessed && player.getVehicle() instanceof INavDriveReceiver rocket) {
            ItemStack result = rocket.insertNavDrive(player, stack);
            if (result != null) newStack = result;
        }

        return InteractionResultHolder.pass(newStack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        int x = pos.getX(), z = pos.getZ();

        Destination destination = getDestination(stack);
        if (destination.body() == SolarSystem.Body.ORBIT) {
            if (SpaceDimensions.isOrbit(world.dimension())) return InteractionResult.PASS;

            if (!world.isClientSide && player != null) {
                Destination target = new Destination(CelestialBody.getEnum(world), x, z);

                if (RocketStationHooks.get().recallPod(world, destination.x(), destination.z(), target)) {
                    player.sendSystemMessage(Component.literal("Recalling drop pod to coordinates: " + x + ", " + z).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                } else {
                    player.sendSystemMessage(Component.literal("Could not recall drop pod from station!").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));
                }
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        boolean onDestination = world.dimension().equals(destination.body().getDimension());
        if (!onDestination)
            return InteractionResult.PASS;

        setCoordinates(stack, x, z);
        setProcessed(stack, true);

        if (!world.isClientSide && player != null)
            player.sendSystemMessage(Component.literal("Set landing coordinates to: " + x + ", " + z).withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC));

        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    /** Original {@code ItemVOTVdrive.Destination}. */
    public record Destination(SolarSystem.Body body, int x, int z) {

        public ChunkPos getChunk() {
            return new ChunkPos(x >> 4, z >> 4);
        }
    }

    /** Original {@code ItemVOTVdrive.Target}. */
    public record Target(@Nullable CelestialBody body, boolean inOrbit, boolean isValid) {}
}
