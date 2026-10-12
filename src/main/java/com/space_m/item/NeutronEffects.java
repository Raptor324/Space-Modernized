package com.space_m.item;

import java.util.List;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.hazard.HazardEntry;
import com.hbm_m.hazard.HazardRegistry;
import com.hbm_m.hazard.HazardSystem;
import com.hbm_m.hazard.transformer.HazardTransformerBase;
import com.hbm_m.hazard.transformer.HazardTransformerRadiationNBT;
import com.hbm_m.hazard.type.HazardTypeRadiation;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.ContaminationUtil;
import com.space_m.config.SpaceConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * R9-B (R4-B-Rest, W-R4b-2/W-GR-1): Neutronenstrahlung von NTM Space ueber die sAPI-Erweiterungspunkte, ohne Raptors
 * Strahlungssystem zu aendern. Alles wirkt nur bei {@code HAZ_01_disableNeutrons = false} (Standard wie im Original: aus).
 * <ul>
 *   <li>Gegenstaende: {@code HazardTypeRadiation} zaehlt {@code ntmNeutron} dazu ({@link HazardTypeRadiation#registerExtraLevel});
 *       der Space-Anteil von {@code HazardTransformerRadiationNBT} (Eintrag fuer jeden Gegenstand) als eigener Trafo.</li>
 *   <li>Koerper ({@code EntityEffectHandler.handleRadiation}): Aktivierung {@code neut / 20} RAD_BYPASS, Zerfall {@code * 0.998816}.</li>
 *   <li>Entgiftungsdusche ({@code TileEntityDecon.deconNeutron}): {@code * 0.899916} fuer Koerper und Inventar.</li>
 *   <li>Neutronen-Eintraege des Space-Zweigs an HBM-Gegenstaenden ({@code HazardRegistry}/{@code OreDictManager .neutron()}).</li>
 * </ul>
 */
// Original-Klasse: HazardTransformerRadiationNBT (Neutronenanteil)
public final class NeutronEffects {

    private NeutronEffects() {}

    private static boolean inited;
    private static boolean hbmDone;

    public static synchronized void init() {
        if (inited) return;
        inited = true;
        HazardTypeRadiation.registerExtraLevel(NeutronEffects::itemActivation);
        HazardSystem.trafos.add(new NeutronTrafo());
    }

    /** {@code stack.nbt.getFloat(NEUTRON_KEY)} (Original HazardTypeRadiation.onUpdate/addHazardInformation). */
    public static double itemActivation(ItemStack stack) {
        if (stack.isEmpty()) return 0D;
        CompoundTag t = PlatformHooks.getItemTag(stack);
        return t != null && t.contains(HazardTypeNeutron.NEUTRON_KEY) ? t.getFloat(HazardTypeNeutron.NEUTRON_KEY) : 0D;
    }

    /**
     * Space {@code HazardTransformerRadiationNBT.transformPost}: Strahlungseintrag {@code RAD_KEY + NEUTRON_KEY} fuer jeden
     * Gegenstand. HBMs eigener Trafo liefert bereits den {@code RAD_KEY}-Teil (nur wenn vorhanden); hier kommt der
     * Neutronenteil dazu - bei vorhandenem {@code RAD_KEY} mit Pegel 0, damit der Zusatzpegel (s.o.) wie im Original
     * genau einmal fuer diesen Eintrag zaehlt.
     */
    private static final class NeutronTrafo extends HazardTransformerBase {
        @Override public void transformPre(ItemStack stack, List<HazardEntry> entries) { }

        @Override
        public void transformPost(ItemStack stack, List<HazardEntry> entries) {
            CompoundTag t = PlatformHooks.getItemTag(stack);
            if (t == null || !t.contains(HazardTypeNeutron.NEUTRON_KEY)) return;
            float level = t.contains(HazardTransformerRadiationNBT.RAD_KEY) ? 0F : t.getFloat(HazardTypeNeutron.NEUTRON_KEY);
            entries.add(new HazardEntry(HazardRegistry.RADIATION, level));
        }
    }

    /** Original {@code ContaminationUtil.contaminate(entity, NEUTRON, CREATIVE, rad)} (Teil nach dem Umgebungswert). */
    static void contaminateNeutron(LivingEntity entity, float amount) {
        if (entity instanceof Player player) {
            if ((player.isCreative() || player.isSpectator())) return; // CREATIVE: Kreativ ausgenommen
            if (player.tickCount < 200) return;
        }
        HbmLivingProps.incrementNeutronActivation(entity, amount * ContaminationUtil.calculateRadiationMod(entity));
    }

    /** {@code EntityEffectHandler.handleRadiation} (Server, nach isRadImmune), ueber {@code SpaceEnvironment.onLivingTick}. */
    public static void tick(LivingEntity entity) {
        if (entity.level().isClientSide) return;
        if (ContaminationUtil.isRadImmune(entity)) return;
        boolean disabled = SpaceConfig.get().disableNeutron;
        float neut = disabled ? 0F : HbmLivingProps.getNeutronActivation(entity);
        if (neut > 0) {
            ContaminationUtil.contaminate(entity, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, neut / 20F);
            HbmLivingProps.setNeutronActivation(entity, neut * 0.998816F); // 20 minute half life
        }
        if (neut < 1e-5 && HbmLivingProps.getNeutronActivation(entity) != 0F) HbmLivingProps.setNeutronActivation(entity, 0);
    }

    /** {@code TileEntityDecon.deconNeutron}. */
    public static void decontaminate(LivingEntity e) {
        boolean disabled = SpaceConfig.get().disableNeutron;
        float neut = disabled ? 0F : HbmLivingProps.getNeutronActivation(e);
        if (neut > 0) {
            ContaminationUtil.contaminate(e, ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, neut / 20F);
            HbmLivingProps.setNeutronActivation(e, neut * 0.899916F);
        }
        if (e instanceof Player player) {
            for (ItemStack s : player.getInventory().items) decay(s, 0.899916F);
            for (ItemStack s : player.getInventory().armor) decay(s, 0.899916F);
        }
    }

    /** {@code HazardTypeNeutron.decay}. */
    public static void decay(ItemStack stack, float factor) {
        if (stack == null || stack.isEmpty()) return;
        CompoundTag t = PlatformHooks.getItemTag(stack);
        if (t == null || !t.contains(HazardTypeNeutron.NEUTRON_KEY)) return;
        float activation = t.getFloat(HazardTypeNeutron.NEUTRON_KEY);
        PlatformHooks.editItemTag(stack, tag -> {
            tag.putFloat(HazardTypeNeutron.NEUTRON_KEY, activation * factor);
            if (activation < 1e-5) tag.remove(HazardTypeNeutron.NEUTRON_KEY);
        });
    }

    /** Space-HazardRegistry/OreDictManager: NEUTRON-Eintraege an HBM-Gegenstaenden (angehaengt, HBM-Regeln bleiben). */
    public static synchronized void registerHbmNeutrons() {
        if (hbmDone) return;
        hbmDone = true;
        final float billet = 0.5F, nugget = 0.1F, ingot = 1.0F, block = 10.0F;
        final float au198 = 500.0F, pb209 = 10000.0F, radsource = 3.0F;
        final float pobe = 75.0F * radsource, rabe = 7.5F * radsource, pube = 10.0F * radsource;
        final float purg = 6.25F, pu240 = 7.5F;

        add("hbm_m:demon_core_closed", 10_000F);
        add("hbm_m:lamp_demon", 10_000F);
        add(ModMaterialItems.item(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET), au198 * billet * 0.02F);
        add(ModMaterialItems.item(ModMaterials.FLASHLEAD, MaterialShape.BILLET), pb209 * billet * 0.002F);
        add(ModMaterialItems.item(ModMaterials.PO210BE, MaterialShape.BILLET), pobe / 10 * billet);
        add(ModMaterialItems.item(ModMaterials.RA226BE, MaterialShape.BILLET), rabe / 10 * billet);
        add(ModMaterialItems.item(ModMaterials.PU238BE, MaterialShape.BILLET), pube / 10 * billet);
        add("hbm_m:pile_rod_plutonium", rabe * billet * 0.2F);
        add("hbm_m:pile_rod_source", rabe * billet * 0.3F);

        // OreDictManager: PURG .neutron(purg / 60), PU240 .neutron(pu240 / 120) je nugget/billet/ingot/block
        frame(ModMaterials.PU_MIX, "hbm_m:block_pu_mix", purg / 60F, nugget, billet, ingot, block);
        frame(ModMaterials.PLUTONIUM240, "hbm_m:block_pu240", pu240 / 120F, nugget, billet, ingot, block);

        // HazardRegistry: sat_gerald -> AUTISM 1 (HazardTypeAutism, ebenfalls Space-neu)
        Item gerald = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse("hbm_m:sat_gerald"));
        if (gerald != null && gerald != Items.AIR) HazardSystem.addEntry(gerald, new HazardEntry(HazardTypeAutism.AUTISM, 1F));
    }

    private static void frame(ModMaterials mat, String blockId, float neut, float nugget, float billet, float ingot, float block) {
        if (ModMaterialItems.has(mat, MaterialShape.NUGGET)) add(ModMaterialItems.item(mat, MaterialShape.NUGGET), neut * nugget);
        if (ModMaterialItems.has(mat, MaterialShape.BILLET)) add(ModMaterialItems.item(mat, MaterialShape.BILLET), neut * billet);
        if (ModMaterialItems.has(mat, MaterialShape.INGOT)) add(ModMaterialItems.item(mat, MaterialShape.INGOT), neut * ingot);
        add(blockId, neut * block);
    }

    private static void add(String id, float level) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(id));
        add(item, level);
    }

    private static void add(Item item, float level) {
        if (item == null || item == Items.AIR) return;
        HazardSystem.addEntry(item, new HazardEntry(HazardTypeNeutron.NEUTRON, level));
    }
}
