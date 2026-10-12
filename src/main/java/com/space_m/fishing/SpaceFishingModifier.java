package com.space_m.fishing;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.SpaceMod;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Global Loot Modifier {@code space_m:space_fishing} auf {@code minecraft:gameplay/fishing}: ersetzt in den
 * Space-Dimensionen die Angelbeute nach {@link SpaceFishing} (Original {@code ModEventHandler.updateFish}).
 * Angemeldet in {@code data/forge|neoforge/loot_modifiers/global_loot_modifiers.json}.
 */
public class SpaceFishingModifier extends
        //? if forge {
        net.minecraftforge.common.loot.LootModifier {
        //?} elif neoforge {
        /*net.neoforged.neoforge.common.loot.LootModifier {
        *///?}

    //? if forge {
    public static final com.mojang.serialization.Codec<SpaceFishingModifier> CODEC =
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, SpaceFishingModifier::new));
    //?} elif neoforge {
    /*public static final com.mojang.serialization.MapCodec<SpaceFishingModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, SpaceFishingModifier::new));
    *///?}

    public SpaceFishingModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        if (!SpaceFishing.FISHING_TABLE.equals(String.valueOf(context.getQueriedLootTableId()))) return generatedLoot;

        List<ItemStack> replaced = SpaceFishing.replace(context, category -> {
            List<ItemStack> out = new ArrayList<>();
            LootTable table = vanillaTable(context, category == SpaceFishing.Category.JUNK);
            if (table != null) table.getRandomItemsRaw(context, out::add);
            return out;
        });
        if (replaced == null) return generatedLoot;

        ObjectArrayList<ItemStack> loot = new ObjectArrayList<>();
        loot.addAll(replaced);
        return loot;
    }

    private static LootTable vanillaTable(LootContext context, boolean junk) {
        var server = context.getLevel().getServer();
        //? if < 1.21.1 {
        return server.getLootData().getLootTable(junk ? BuiltInLootTables.FISHING_JUNK : BuiltInLootTables.FISHING_TREASURE);
        //?} else {
        /*return server.reloadableRegistries().getLootTable(junk ? BuiltInLootTables.FISHING_JUNK : BuiltInLootTables.FISHING_TREASURE);
        *///?}
    }

    @Override
    //? if forge {
    public com.mojang.serialization.Codec<? extends net.minecraftforge.common.loot.IGlobalLootModifier> codec() {
    //?} elif neoforge {
    /*public com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier> codec() {
    *///?}
        return CODEC;
    }

    // ── Anmeldung des Codecs (Mod-Bus RegisterEvent) ──

    //? if forge {
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRegister(net.minecraftforge.registries.RegisterEvent event) {
            event.register(net.minecraftforge.registries.ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    helper -> helper.register(SpaceFishing.id(), CODEC));
        }
    }
    //?} elif neoforge {
    /*@net.neoforged.fml.common.EventBusSubscriber(modid = SpaceMod.MOD_ID, bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @net.neoforged.bus.api.SubscribeEvent
        public static void onRegister(net.neoforged.neoforge.registries.RegisterEvent event) {
            event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS,
                    helper -> helper.register(SpaceFishing.id(), CODEC));
        }
    }
    *///?}
}
