package com.space_m.lifesupport;

import java.util.List;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.lib.SpaceTags;
import com.space_m.sound.SpaceSounds;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.event.events.client.ClientTooltipEvent;
import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Client-Teil der Lebenserhaltung (R2-B): Renderer, PLSS-Atemgeraeusch, Tooltip "armor.canSeal".
 */
public final class LifeSupportClient {

    private LifeSupportClient() {}

    /** Aus {@code SpaceClient.init()} (Mod-Konstruktor, nur physischer Client). */
    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> tickBreathing(mc));

        // Original ArmorFSB.addInformation: "canSeal" als erste Zeile der Ruestungsinfo (hier: Tag space_m:sealed_armor)
        //? if < 1.21.1 {
        ClientTooltipEvent.ITEM.register((stack, lines, flag) -> addSealTooltip(stack, lines));
        //?} else {
        /*ClientTooltipEvent.ITEM.register((stack, lines, context, flag) -> addSealTooltip(stack, lines));
        *///?}
    }

    /** Aus {@code SpaceClient.setup()}. */
    public static void setup() {
        BlockEntityRendererRegistry.register(SpaceBlockEntities.AIR_SCRUBBER.get(), RenderAirScrubber::new);
    }

    private static void addSealTooltip(ItemStack stack, List<Component> lines) {
        if (stack.isEmpty() || !stack.is(SpaceTags.SEALED_ARMOR)) return;
        Component line = Component.translatable("armor.canSeal").withStyle(ChatFormatting.BLUE);
        lines.add(Math.min(1, lines.size()), line);
    }

    // ── Original ItemModOxy.modUpdate (Client, nur eigener Spieler): Atemgeraeusch-Schleife ──

    private static BreathingSound audioBreathing;

    private static void tickBreathing(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            stopBreathing();
            return;
        }

        boolean inUse = false;
        ItemStack armor = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!armor.isEmpty() && ArmorModificationHelper.hasMods(armor)) {
            ItemStack stack = ArmorModificationHelper.pryMod(armor, ArmorModificationHelper.plate_only);
            inUse = stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemModOxy && ItemModOxy.getInUse(stack);
        }

        if (inUse && !player.getAbilities().instabuild) {
            if (audioBreathing == null || audioBreathing.isStopped() || !mc.getSoundManager().isActive(audioBreathing)) {
                audioBreathing = new BreathingSound(player);
                mc.getSoundManager().play(audioBreathing);
            }

            audioBreathing.keepAlive();
        } else {
            stopBreathing();
        }
    }

    private static void stopBreathing() {
        if (audioBreathing != null) {
            audioBreathing.stopSound();
            audioBreathing = null;
        }
    }

    /** Original {@code getLoopedSound("hbm:player.plss_breathing", entity, 0.1F, 5.0F, 1.0F, 10)}: folgt dem Spieler, 10 Ticks Keep-Alive. */
    // Original-Klasse: AudioDynamicEntity (an ein Entity gebundener Schleifenklang von getLoopedSound(sound, entity, ...);
    // ebenso OrbitClient.HumSound fuer misc.stationhum)
    static final class BreathingSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;
        private int keepAlive;

        BreathingSound(LocalPlayer player) {
            super(SpaceSounds.PLAYER_PLSS_BREATHING.get(), SoundSource.PLAYERS, player.getRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.1F;
            this.pitch = 1.0F;
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
            this.keepAlive = 10;
        }

        void keepAlive() {
            keepAlive = 10;
        }

        void stopSound() {
            stop();
        }

        @Override
        public void tick() {
            if (player.isRemoved() || --keepAlive < 0) {
                stop();
                return;
            }
            this.x = player.getX();
            this.y = player.getY();
            this.z = player.getZ();
        }
    }
}
