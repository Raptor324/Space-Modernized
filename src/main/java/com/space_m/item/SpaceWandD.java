package com.space_m.item;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.api.space.WandDHook;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.platform.PlatformHooks;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;
import com.space_m.celestial.trait.CBT_Destroyed;
import com.space_m.dim.SpaceDimensions;
import com.space_m.travel.CelestialTeleporter;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1 NTM Space {@code com.hbm.items.tool.ItemWandD} ("Dimension teleporter and atmosphere debugger"). Space ersetzt
 * die Master-Fassung (Kran-Struktur) vollstaendig; HBMs {@code wand_d} ruft diese Logik ueber
 * {@link WandDHook} auf ({@link SpaceApi#setWandDHook}).
 * <ul>
 *   <li>Block getroffen (500 m), nicht schleichend: Teleport zum Ziel {@code dim} (0 = Orbit, y 128, sonst
 *       {@code SolarSystem.Body.values()[dim]}, y 300, am Boden).</li>
 *   <li>Block getroffen, schleichend: Ziel weiterschalten.</li>
 *   <li>Kein Treffer ausserhalb des Orbits: Atmosphaere ausgeben bzw. (schleichend) die Sonne zerstoeren / "kidding".</li>
 *   <li>Kein Treffer im Orbit: Beton unter den Spieler.</li>
 * </ul>
 * Original-Klasse: ItemWandD (NTM Space)
 */
public final class SpaceWandD implements WandDHook {

    public static final SpaceWandD INSTANCE = new SpaceWandD();

    private SpaceWandD() {}

    /** Im Mod-Konstruktor. */
    public static void init() {
        SpaceApi.setWandDHook(INSTANCE);
    }

    @Override
    public boolean active() {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand, ItemStack stack) {
        if (world.isClientSide)
            return InteractionResultHolder.pass(stack);

        // Library.rayTrace(player, 500, 1, false, true, false) - wie HBMs wand_d ueber pick(); kein Treffer = MISS statt null
        HitResult pos = player.pick(500, 1F, false);

        if (pos != null && pos.getType() == HitResult.Type.BLOCK) {

            if (!player.isShiftKeyDown()) {
                int targetId = tag(stack).getInt("dim");
                // Original: stackTagCompound wird angelegt (Tooltip zeigt danach das Ziel)
                PlatformHooks.editItemTag(stack, t -> t.putInt("dim", targetId));

                if (targetId == 0) {
                    CelestialTeleporter.teleport(player, SpaceDimensions.ORBIT, player.getX(), 128, player.getZ(), false);
                    player.sendSystemMessage(Component.literal("Teleported to: ORBIT"));
                } else {
                    SolarSystem.Body target = SolarSystem.Body.values()[targetId];

                    CelestialTeleporter.teleport(player, target.getBody().dimension, player.getX(), 300, player.getZ(), true);
                    player.sendSystemMessage(Component.literal("Teleported to: " + target.getBody().getUnlocalizedName()));
                }

            } else {

                int targetId = tag(stack).getInt("dim");
                targetId++;

                if (targetId >= SolarSystem.Body.values().length) {
                    targetId = 0;
                }

                final int newId = targetId;
                PlatformHooks.editItemTag(stack, t -> t.putInt("dim", newId));

                if (targetId == 0) {
                    player.sendSystemMessage(Component.literal("Set teleport target to: ORBIT"));
                } else {
                    SolarSystem.Body target = SolarSystem.Body.values()[targetId];
                    player.sendSystemMessage(Component.literal("Set teleport target to: " + target.getBody().getUnlocalizedName()));
                }
            }
        } else if (!CelestialBody.inOrbit(world)) {
            if (!player.isShiftKeyDown()) {
                // TESTING: View atmospheric data
                CBT_Atmosphere atmosphere = CelestialBody.getTrait(world, CBT_Atmosphere.class);

                boolean isVacuum = true;
                if (atmosphere != null) {
                    for (FluidEntry entry : atmosphere.fluids) {
                        // if(entry.pressure > 0.001) {
                            // Original FluidType.getUnlocalizedName() = "hbmfluid." + Name
                            player.sendSystemMessage(Component.literal("Atmosphere: hbmfluid." + entry.fluid.getPath() + " - " + entry.pressure + "bar"));
                            isVacuum = false;
                        // }
                    }
                }

                if (isVacuum)
                    player.sendSystemMessage(Component.literal("Atmosphere: NEAR VACUUM"));
            } else {
                toggleStar(world, player::sendSystemMessage);
            }
        } else {
            world.setBlockAndUpdate(new BlockPos(Mth.floor(player.getX()), Mth.floor(player.getY()) - 1, Mth.floor(player.getZ())), ModBlocks.CONCRETE.get().defaultBlockState());
        }

        return InteractionResultHolder.pass(stack);
    }

    /**
     * Original-Zweig "schleichen + Rechtsklick in den Himmel": Stern zerstoeren ({@code CBT_Destroyed}, {@code degas})
     * bzw. alles zuruecksetzen ("kidding"). Auch fuer {@code /space destroystar}.
     *
     * @return true = Sonne jetzt zerstoert
     */
    public static boolean toggleStar(Level world, Consumer<Component> chat) {
        CelestialBody star = CelestialBody.getStar(world);

        if (!star.hasTrait(CBT_Destroyed.class)) {

            // TESTING: END OF TIME
            star.modifyTraits(new CBT_Destroyed());

            // TESTING: END OF LIFE
            CelestialBody.degas(world);

            // GOD
            // DAMN
            chat.accept(Component.literal("GOD").withStyle(ChatFormatting.RED));
            chat.accept(Component.literal("DAMN").withStyle(ChatFormatting.RED));
            chat.accept(Component.literal("THE").withStyle(ChatFormatting.RED));
            chat.accept(Component.literal("SUN").withStyle(ChatFormatting.RED, ChatFormatting.OBFUSCATED));
            return true;
        } else {

            star.clearTraits();
            CelestialBody.clearTraits(world);

            chat.accept(Component.literal("kidding"));
            return false;
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable Level level, List<Component> list) {
        list.add(Component.literal("Dimension teleporter and atmosphere debugger."));

        CompoundTag nbt = PlatformHooks.getItemTag(stack);
        if (nbt != null && !nbt.isEmpty()) {
            int targetId = nbt.getInt("dim");
            if (targetId == 0) {
                list.add(Component.literal("Teleportation target: ORBIT"));
            } else {
                SolarSystem.Body target = SolarSystem.Body.values()[targetId];
                list.add(Component.literal("Teleportation target: " + target.getBody().getUnlocalizedName()));
            }
        }
    }

    private static CompoundTag tag(ItemStack stack) {
        CompoundTag tag = PlatformHooks.getItemTag(stack);
        return tag != null ? tag : new CompoundTag();
    }
}
