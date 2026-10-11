package com.space_m.atmosphere;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.mob.EntityCyberCrab;
import com.hbm_m.entity.mob.glyphid.EntityGlyphid;
import com.hbm_m.network.InfoToastPacket;
import com.hbm_m.powerarmor.DNTArmor;
import com.hbm_m.powerarmor.ModArmorFSB;
import com.space_m.SpaceMod;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.lib.SpaceTags;
import com.space_m.network.SpacePacketHandler;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Ersticken und Korrosion, 1:1 NTM Space:
 * <ul>
 *   <li>{@code EntityEffectHandler.getAtmosphereCached/handleOxy/handleCorrosion} (Server, jede Entity je Tick;
 *       Nicht-Spieler fragen die Atmosphaere nur jede Sekunde ab).</li>
 *   <li>{@code HbmLivingProps.getOxy/setOxy} (100 Ticks Luftvorrat, danach Schaden {@code oxyprime}: Intervall
 *       {@code clamp(oxy/60+8, 1, 8)}, Menge {@code clamp(-oxy/40-10, 1, MAX)}; NBT {@code hfr_oxygen}),
 *       {@code setAtmosphere/hasGravity}.</li>
 *   <li>{@code ArmorUtil.checkForOxy/getOxygenTank/checkSpecialBreathing/checkForCorrosion/getCorrosionProtection}.</li>
 * </ul>
 * Ersatz der Original-Klassenpruefungen durch Tags: {@code ISuffocationImmune} -> {@link ISuffocationImmune} oder
 * Entity-Tag {@code space_m:suffocation_immune}; {@code ArmorFSB.canSeal} -> Item-Tag {@code space_m:sealed_armor};
 * {@code ItemModOxy} -> {@link IOxygenTank} im Helm-Plattenslot; {@code ItemModInsert.corrosionProtection} ->
 * Item-Tag {@code space_m:corrosion_protection} im Brust-Einsatzslot. Fisk's-Superheroes-Kompatibilitaet entfaellt.
 */
public final class AtmosphereEffects {

	private AtmosphereEffects() {}

	/** Schadensart des Originals {@code ModDamageSource.oxyprime} (absolut, durch Ruestung). */
	public static final ResourceKey<DamageType> OXYPRIME = ResourceKey.create(Registries.DAMAGE_TYPE, SpaceMod.id("oxyprime"));

	public static final String NBT_OXYGEN = "hfr_oxygen";
	public static final int MAX_OXYGEN = 100;

	/** Original: {@code ridingEntity instanceof EntityRideableRocket} ("breathe easy in your ship") - R5 setzt den Test. */
	public static Predicate<Entity> breathableVehicle = vehicle -> false;

	/** Gecachte Atmosphaere je Entity (Original {@code HbmLivingProps.atmosphere}), nur Server. */
	private static final Map<LivingEntity, CachedAtmosphere> CACHE = new WeakHashMap<>();

	private record CachedAtmosphere(@Nullable CBT_Atmosphere atmosphere) {}

	/** Client: vom Server gesendeter Luftvorrat/Schwerkraft des eigenen Spielers. */
	public static int clientOxygen = MAX_OXYGEN;
	public static boolean clientGravity = true;

	// ─────────────────────────────── Tick ───────────────────────────────

	/** Original {@code EntityEffectHandler.onUpdate}, Atmosphaeren-Teil (nur Server). */
	public static void onLivingTick(LivingEntity entity) {
		if(entity.level().isClientSide) return;

		CBT_Atmosphere atmosphere = getAtmosphereCached(entity);

		handleOxy(entity, atmosphere);
		handleCorrosion(entity, atmosphere);

		if(entity instanceof ServerPlayer player) syncPlayer(player);
	}

	@Nullable
	public static CBT_Atmosphere getAtmosphereCached(LivingEntity entity) {
		// Update non-player entities once per second
		if(entity instanceof ServerPlayer || entity.tickCount % 20 == 0) {
			CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(entity);
			setAtmosphere(entity, atmosphere);
			return atmosphere;
		}

		return getAtmosphere(entity);
	}

	public static void handleOxy(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
		if(entity.level().isClientSide) return;
		if(isSuffocationImmune(entity)) return;
		if(entity.getVehicle() != null && breathableVehicle.test(entity.getVehicle())) return; // breathe easy in your ship

		if(!checkForOxy(entity, atmosphere)) {
			setOxy(entity, getOxy(entity) - 1);
		} else {
			setOxy(entity, MAX_OXYGEN); // 5 seconds until vacuum damage
		}
	}

	// Corrosive atmospheres melt your suit, without appropriate protection
	public static void handleCorrosion(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
		if(entity.level().isClientSide) return;
		if(entity instanceof EntityGlyphid) return;
		if(entity instanceof EntityCyberCrab) return;
		if(entity.getVehicle() != null && breathableVehicle.test(entity.getVehicle())) return;

		// If we should corrode but we have armor, damage it heavily
		// once it runs out of juice, fizzle it and start damaging the player
		if(checkForCorrosion(entity, atmosphere)) {
			entity.hurt(ModDamageSources.acid(entity.level()), 1);
		}
	}

	public static boolean isSuffocationImmune(Entity entity) {
		return entity instanceof ISuffocationImmune || entity.getType().is(SpaceTags.SUFFOCATION_IMMUNE);
	}

	// ─────────────────────────────── HbmLivingProps ───────────────────────────────

	public static int getOxy(LivingEntity entity) {
		CompoundTag data = entity.getPersistentData();
		return data.contains(NBT_OXYGEN) ? data.getInt(NBT_OXYGEN) : MAX_OXYGEN;
	}

	public static void setOxy(LivingEntity entity, int oxygen) {
		if(oxygen <= 0) {
			if(entity.tickCount < 20) oxygen = 0; // limit overdamage effects when relogging

			int damageInterval = Mth.clamp(oxygen / 60 + 8, 1, 8);
			int damageAmount = Mth.clamp(-oxygen / 40 - 10, 1, Integer.MAX_VALUE);

			// Only damage every 4 ticks, giving the player more time to react
			if(entity.tickCount % damageInterval == 0) {
				entity.hurt(oxyprime(entity.level()), damageAmount);
			}
		}

		entity.getPersistentData().putInt(NBT_OXYGEN, oxygen);
	}

	public static DamageSource oxyprime(Level level) {
		return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(OXYPRIME));
	}

	@Nullable
	public static CBT_Atmosphere getAtmosphere(LivingEntity entity) {
		synchronized(CACHE) {
			CachedAtmosphere cached = CACHE.get(entity);
			return cached != null ? cached.atmosphere : null;
		}
	}

	public static void setAtmosphere(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
		synchronized(CACHE) {
			CACHE.put(entity, new CachedAtmosphere(atmosphere));
		}
	}

	/**
	 * Original {@code HbmLivingProps.hasGravity} ("gravity attached to atmospheres, for now"): true, sobald die Entity
	 * in irgendeiner Atmosphaere ist. Client: Wert des eigenen Spielers aus dem Sync.
	 */
	public static boolean hasGravity(LivingEntity entity) {
		if(entity.level().isClientSide) return clientGravity;
		synchronized(CACHE) {
			CachedAtmosphere cached = CACHE.get(entity);
			return cached != null && cached.atmosphere != null;
		}
	}

	// ─────────────────────────────── ArmorUtil ───────────────────────────────

	public static boolean checkForOxy(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
		if(!(entity instanceof Player player)) return ChunkAtmosphereManager.proxy.canBreathe(atmosphere);

		if(checkSpecialBreathing(player)) return true;

		ItemStack tank = getOxygenTank(player);
		if(tank.isEmpty()) return ChunkAtmosphereManager.proxy.canBreathe(atmosphere);

		// If we have an oxygen tank, block drowning
		boolean isInWater = entity.getAirSupply() < entity.getMaxAirSupply();
		ItemStack before = tank.copy();
		boolean canBreatheTank = ((IOxygenTank) tank.getItem()).attemptBreathing(entity, tank, atmosphere, isInWater);
		writeBackTank(player, before, tank);

		if(isInWater && canBreatheTank) {
			entity.setAirSupply(entity.getMaxAirSupply());
		}

		return canBreatheTank;
	}

	/** Original {@code ArmorModHandler.plate_only}. */
	private static final int SLOT_PLATE_ONLY = 1;
	/** Original {@code ArmorModHandler.kevlar}. */
	private static final int SLOT_KEVLAR = 6;

	private static final EquipmentSlot[] ARMOR = { EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };

	/** Original {@code getOxygenTank}: alle vier Teile dicht + Sauerstofftank im Helm; sonst {@link ItemStack#EMPTY}. */
	public static ItemStack getOxygenTank(Player player) {
		// TODO: only require pressure suits in near vacuums, and use regular oxygen tanks otherwise

		// Check that all the armor pieces are sealed
		for(EquipmentSlot slot : ARMOR) {
			ItemStack stack = player.getItemBySlot(slot);
			if(stack.isEmpty() || !stack.is(SpaceTags.SEALED_ARMOR)) return ItemStack.EMPTY;
		}

		// Check for a non-empty oxygen tank
		ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
		if(ArmorModificationHelper.hasMods(helmet)) {
			ItemStack tankMod = ArmorModificationHelper.pryMod(helmet, SLOT_PLATE_ONLY);
			if(tankMod == null || tankMod.isEmpty() || !(tankMod.getItem() instanceof IOxygenTank)) return ItemStack.EMPTY;

			return tankMod;
		}

		return ItemStack.EMPTY;
	}

	/** Der Tank ist eine Kopie aus dem Helm-NBT: Aenderungen (Fuellstand, "in use") zurueckschreiben. */
	private static void writeBackTank(Player player, ItemStack before, ItemStack after) {
		if(ItemStack.matches(before, after)) return;
		ArmorModificationHelper.applyMod(player.getItemBySlot(EquipmentSlot.HEAD), after);
	}

	private static boolean checkSpecialBreathing(Player player) {
		// Abweichung: Zuschauer atmen wie Kreativ-Spieler (Original kennt keinen Zuschauermodus)
		if(player.getAbilities().instabuild || player.isSpectator()) return true;

		// Check DNT
		for(EquipmentSlot slot : ARMOR) {
			ItemStack stack = player.getItemBySlot(slot);
			if(stack.isEmpty() || !(stack.getItem() instanceof DNTArmor)) return false;
		}

		return true;
	}

	public static boolean checkForCorrosion(LivingEntity entity, @Nullable CBT_Atmosphere atmosphere) {
		if(!ChunkAtmosphereManager.proxy.willCorrode(atmosphere)) return false;

		if(!(entity instanceof Player player)) return true;

		ItemStack insert = getCorrosionProtection(player);
		if(insert.isEmpty()) {
			boolean isSealed = true; // safe, for now...
			RandomSource rand = entity.getRandom();

			// if we have a full set of armor, deplete that rapidly first before applying damage
			for(EquipmentSlot slot : ARMOR) {
				ItemStack stack = player.getItemBySlot(slot);
				if(stack.isEmpty() || !stack.is(SpaceTags.SEALED_ARMOR)) {
					isSealed = false;
				}

				if(stack.isEmpty()) continue;

				if(rand.nextInt(2) == 0 && stack.isDamageableItem()) {
					stack.setDamageValue(stack.getDamageValue() + 1);
				}

				boolean fsb = stack.getItem() instanceof ModArmorFSB;
				if((stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage()) || !fsb || !((ModArmorFSB) stack.getItem()).isArmorEnabled(stack)) {
					player.setItemSlot(slot, ItemStack.EMPTY);
					entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.2F, 1F);
				}
			}

			if(isSealed && entity instanceof ServerPlayer serverPlayer) {
				InfoToastPacket.sendTo(serverPlayer, "info.corrosion", 60, InfoToastPacket.ID_GAS_HAZARD, 0xFF5555);
			}

			return !isSealed;
		}

		return false;
	}

	public static ItemStack getCorrosionProtection(Player player) {
		// Check chest piece has a valid insert
		ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
		if(!chest.isEmpty() && ArmorModificationHelper.hasMods(chest)) {
			ItemStack insertMod = ArmorModificationHelper.pryMod(chest, SLOT_KEVLAR);
			if(insertMod == null || insertMod.isEmpty() || !insertMod.is(SpaceTags.CORROSION_PROTECTION)) return ItemStack.EMPTY;

			return insertMod;
		}

		return ItemStack.EMPTY;
	}

	// ─────────────────────────────── Sync (Original ExtPropPacket, Teil oxygen) ───────────────────────────────

	private static final Map<ServerPlayer, Long> LAST_SYNC = new WeakHashMap<>();

	private static void syncPlayer(ServerPlayer player) {
		int oxy = getOxy(player);
		boolean gravity = hasGravity(player);
		long key = ((long) oxy << 1) | (gravity ? 1 : 0);
		Long last = LAST_SYNC.get(player);
		if(last != null && last == key && player.tickCount % 20 != 0) return;
		LAST_SYNC.put(player, key);
		SpacePacketHandler.sendToPlayer(player, SpacePacketHandler.ATMOSPHERE_OXY, new OxyPacket(oxy, gravity));
	}

	/** S2C: Luftvorrat + Schwerkraft-Flag des eigenen Spielers (Original Teil von {@code HbmLivingProps.serialize}). */
	public record OxyPacket(int oxygen, boolean gravity) implements com.space_m.network.SpacePacket {
		@Override
		public void write(FriendlyByteBuf buf) {
			buf.writeInt(oxygen);
			buf.writeBoolean(gravity);
		}

		public static OxyPacket decode(FriendlyByteBuf buf) {
			return new OxyPacket(buf.readInt(), buf.readBoolean());
		}

		public void apply() {
			clientOxygen = oxygen;
			clientGravity = gravity;
		}
	}
}
