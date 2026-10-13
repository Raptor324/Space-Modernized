package com.space_m.fork.trinket;

import java.util.List;

import com.hbm_m.handler.BobmazonOfferFactory;
import com.hbm_m.handler.BobmazonOfferFactory.Offer;
import com.hbm_m.item.TrinketBlockItem;
import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.fork.SpaceForkRules;
import com.space_m.main.SpaceCreativeTabs;
import com.space_m.SpaceMod;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * E11 (Nutzerentscheidung 2026-10-12, nur mit Addon): Sammelfiguren des NTM-Space-Zweigs, die HBM-Modernized nicht
 * fuehrt (HBM-Enums {@code TrinketTypes} sind Master-Stand und werden nicht erweitert).
 *
 * <ul>
 *   <li>Wackelkoepfe ({@code BlockBobble.BobbleType}) GWEN, JUICE, JAMESH_2, MRKIMKIMORA - Block {@code space_m:bobblehead}
 *       (Material.iron, Haerte 0, kein Drop, blockTab), Tafel mit Titel/Beitrag/Gravur, Schredder -> scrap_plastic BOARD_BLANK;
 *       Gwen sprengt den Schredder ({@link SpaceForkRules#gwenBobble}).</li>
 *   <li>Plueschtiere ({@code BlockPlushie.PlushieType}) TETO, MIKU, NERU, FATO - Block {@code space_m:plushie}
 *       (Stoff-Klang, Haerte 0, blockTab ohne FATO), Klaenge {@code block.teto/miku/akita}; Butter auf Teto -> FAT TETO
 *       ({@code player.groan}, Tonhoehe 2). FAT TETO schaltet am Dyson-Starter den Sunset Overdrive ein.</li>
 * </ul>
 *
 * <p>Abweichung (technisch): eigene Bloecke statt neuer Metas im HBM-Block; Typnummern daher addon-eigen
 * (0 = NONE wie im Original, danach die Fork-Typen in Originalreihenfolge).
 */
public final class ForkTrinkets {

    private ForkTrinkets() {}

    /** Space {@code BlockBobble.BobbleType} - nur die Fork-Typen (Werte 1:1, Scrap {@code ScrapType.BOARD_BLANK}). */
    public enum ForkBobbleType {
        NONE("null", "null", null, null, false),
        GWEN("Gwen", "Gwen", "Numero Uno Homie", "HELP ME I'M TRAPPED IN THIS FUCKING POLYRESIN PRISON YOU NEED TO LET ME OUT PLEASE SMASH IT OPEN DO IT NOW CRACK IT", true),
        JUICE("Juicy_Lad", "Juicy_Lad", "The Mojave Testing for this funny fork", "\"What should the Inscription be?\",$ \"Uhh.. I'll think about it.\"", true),
        JAMESH_2("JamesH_2", "JamesH_2", "The fork itself", "COME ON AND SLAM", true),
        MRKIMKIMORA("MrKimkimora", "MrKimkimora", "Part of NTM:Space models,$textures and russian localization", "I know your IP. Your IP is 127.0.0.1.", false);

        public final String title;
        public final String label;
        public final String contribution;
        public final String inscription;
        public final boolean skinLayers;
        public final String scrap = "board_blank";

        ForkBobbleType(String title, String label, String contribution, String inscription, boolean layers) {
            this.title = title;
            this.label = label;
            this.contribution = contribution;
            this.inscription = inscription;
            this.skinLayers = layers;
        }
    }

    /** Space {@code BlockPlushie.PlushieType} - nur die Fork-Typen (Name, Aufschrift, Klang 1:1). */
    public enum ForkPlushieType {
        NONE("NONE", null, null),
        TETO("Kasane Teto", "please help I've been trapped in her basement for da-", "block.teto"),
        MIKU("Hatsune Miku", "In your wifi, and your heart.", "block.miku"),
        NERU("Akita Neru", "Careful, she might electrocute you.", "block.akita"),
        FATO("FAT TETO", "pls don't bully me", "block.teto");

        public final String label;
        public final String inscription;
        /** Klangname im Addon ({@code space_m:<sound>}). */
        public final String sound;

        ForkPlushieType(String label, String inscription, String sound) {
            this.label = label;
            this.inscription = inscription;
            this.sound = sound;
        }
    }

    public static <E extends Enum<E>> E safe(Class<E> cls, int ordinal) {
        E[] v = cls.getEnumConstants();
        return v[Math.abs(ordinal) % v.length];
    }

    // ── Klaenge (Space sounds.json block.teto/miku/akita) ──
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.SOUND_EVENT);
    public static final RegistrySupplier<SoundEvent> BLOCK_TETO = sound("block.teto");
    public static final RegistrySupplier<SoundEvent> BLOCK_MIKU = sound("block.miku");
    public static final RegistrySupplier<SoundEvent> BLOCK_AKITA = sound("block.akita");

    private static RegistrySupplier<SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(SpaceMod.id(id)));
    }

    // ── Bloecke (Original: bobblehead Material.iron Haerte 0, plushie soundTypeCloth Haerte 0; beide ohne Drop) ──
    public static final RegistrySupplier<ForkTrinketBlock> BOBBLEHEAD = SpaceBlocks.registerWithItem("bobblehead",
            () -> new ForkTrinketBlock(com.hbm_m.platform.BlockProps.copy(Blocks.IRON_BLOCK).instabreak().sound(SoundType.STONE).noOcclusion().noLootTable(),
                    ForkTrinketBlock.Kind.BOBBLE),
            null, b -> new ForkTrinketItem(b, new Item.Properties()));
    public static final RegistrySupplier<ForkTrinketBlock> PLUSHIE = SpaceBlocks.registerWithItem("plushie",
            () -> new ForkTrinketBlock(com.hbm_m.platform.BlockProps.copy(Blocks.WHITE_WOOL).instabreak().sound(SoundType.WOOL).noOcclusion().noLootTable(),
                    ForkTrinketBlock.Kind.PLUSHIE),
            null, b -> new ForkTrinketItem(b, new Item.Properties()));

    public static final RegistrySupplier<BlockEntityType<ForkTrinketBlockEntity>> TRINKET_BE =
            SpaceBlockEntities.register("fork_trinket", ForkTrinketBlockEntity::new, BOBBLEHEAD, PLUSHIE);

    public static ItemStack bobble(ForkBobbleType type) {
        return TrinketBlockItem.make(BOBBLEHEAD.get().asItem(), type.ordinal());
    }

    public static ItemStack plushie(ForkPlushieType type) {
        return TrinketBlockItem.make(PLUSHIE.get().asItem(), type.ordinal());
    }

    /** Space {@code TileEntityMachineShredder.canProcess}: {@code bobblehead} mit Meta {@code GWEN}. */
    public static boolean isGwen(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == BOBBLEHEAD.get().asItem()
                && TrinketBlockItem.getType(stack) == ForkBobbleType.GWEN.ordinal();
    }

    /** Space {@code MachineDysonLauncher}: {@code BlockPlushie} mit Meta {@code FATO}. */
    public static boolean isFatTeto(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == PLUSHIE.get().asItem()
                && TrinketBlockItem.getType(stack) == ForkPlushieType.FATO.ordinal();
    }

    /** Aus {@code SpaceRegistry.init()} vor {@code SpaceBlocks.init()} (Klasse laden, Tab, Schredder-Haken). */
    public static void init() {
        SOUNDS.register();
        // Original getSubBlocks: alle Typen ab 1 (Plueschtier ohne FATO), blockTab
        for (ForkBobbleType t : ForkBobbleType.values()) {
            if (t != ForkBobbleType.NONE) SpaceCreativeTabs.addStack(SpaceCreativeTabs.Tab.BLOCKS, () -> bobble(t));
        }
        for (ForkPlushieType t : ForkPlushieType.values()) {
            if (t != ForkPlushieType.NONE && t != ForkPlushieType.FATO) SpaceCreativeTabs.addStack(SpaceCreativeTabs.Tab.BLOCKS, () -> plushie(t));
        }
        SpaceForkRules.gwenBobble = ForkTrinkets::isGwen;
        // Bobmazon-Katalog erst beim Serverstart (HBM baut ihn beim ersten Zugriff; im Setup fehlen noch Material-Gegenstaende)
        dev.architectury.event.events.common.LifecycleEvent.SERVER_STARTING.register(s -> addBobmazonOffers());
    }

    /**
     * Space {@code BobmazonOfferFactory}: {@code for i in 1..PlushieType.values()} - die Fork-Plueschtiere stehen in der
     * Liste an ihrer Enum-Stelle (Teto/Miku/Neru nach Hundun, FAT TETO nach dem Drachen), Oel, 16, Bewertung 0.
     * Beim Serverstart bzw. Client-Beitritt (HBM baut die Liste einmalig beim ersten Zugriff; Aufruf idempotent).
     */
    public static void addBobmazonOffers() {
        List<Offer> list;
        try {
            list = BobmazonOfferFactory.getOffers(new ItemStack(com.hbm_m.item.ModItems.BOBMAZON.get()));
        } catch (RuntimeException e) {
            // HBM-Katalog laesst sich nicht bauen (z. B. Kupferbarren ohne HBM-Gegenstand) - Fork-Eintraege entfallen
            com.space_m.main.SpaceRegistry.LOGGER.warn("[space_m] Bobmazon-Katalog nicht verfuegbar, Fork-Plueschtiere nicht eingetragen: {}", e.toString());
            return;
        }
        if (list == null) return;
        synchronized (BobmazonOfferFactory.class) {
            for (Offer o : list) if (o.offer.getItem() == PLUSHIE.get().asItem()) return; // schon eingetragen
            Item hbmPlushie = com.hbm_m.block.ModBlocks.PLUSHIE.get().asItem();
            int hundun = -1, derg = -1;
            for (int i = 0; i < list.size(); i++) {
                ItemStack st = list.get(i).offer;
                if (st.getItem() != hbmPlushie) continue;
                int t = TrinketBlockItem.getType(st);
                if (t == com.hbm_m.block.decorations.TrinketTypes.PlushieType.HUNDUN.ordinal()) hundun = i;
                if (t == com.hbm_m.block.decorations.TrinketTypes.PlushieType.DERG.ordinal()) derg = i;
            }
            Offer fato = new Offer(plushie(ForkPlushieType.FATO), com.hbm_m.handler.BobmazonOfferFactory.Requirement.OIL, 16, 0);
            if (derg >= 0) list.add(derg + 1, fato); else list.add(fato);
            int at = hundun >= 0 ? hundun + 1 : list.size();
            for (ForkPlushieType t : new ForkPlushieType[] { ForkPlushieType.TETO, ForkPlushieType.MIKU, ForkPlushieType.NERU }) {
                list.add(at++, new Offer(plushie(t), com.hbm_m.handler.BobmazonOfferFactory.Requirement.OIL, 16, 0));
            }
        }
    }
}
