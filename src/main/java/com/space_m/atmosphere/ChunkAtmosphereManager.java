package com.space_m.atmosphere;

import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.dim.SpaceDimensions;

import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.ExplosionEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
//? if forge {
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.level.SaplingGrowTreeEvent;
//?} elif neoforge {
/*import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockGrowFeatureEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
*///?}

/**
 * 1:1 NTM Space {@code ChunkAtmosphereManager} (Ereignis-Weiterleitung an {@link #proxy}) plus die
 * Atmosphaeren-Teile von {@code ModEventHandler} ({@code onBlockPlaced}, {@code onBucketUse}) und
 * {@code EntityEffectHandler} (Ersticken/Korrosion).
 *
 * <p>Ereignisse: Architectury (Welt laden/entladen, Welt-/Server-Tick, Platzieren, Abbauen, Explosion, Eimer) auf beiden
 * Loadern; Lebewesen-Tick und Baumwachstum ueber die Loader-Busse ({@code LivingTickEvent}/{@code EntityTickEvent.Pre},
 * {@code SaplingGrowTreeEvent}/{@code BlockGrowFeatureEvent}).
 *
 * <p>Wasser ohne Druck: Original setzt {@code provider.isHellWorld} (Druck <= 0.2), damit Vanilla-Eimer verdampfen,
 * und schaltet es in Druckraeumen ab. {@code dimension_type.ultrawarm} ist in 1.20+ unveraenderlich -> der Eimer wird
 * hier wie in Vanilla-ultrawarm behandelt (Zischen, Rauch, leerer Eimer), wenn am Ziel kein Fluessigkeitsdruck ist.
 * Eis, das in einer drucklosen Welt abgebaut wird, verdampft ebenso (Vanilla {@code IceBlock.playerDestroy}). Die
 * schnellere Lava-Ausbreitung von {@code isHellWorld} braeuchte ein Mixin und entfaellt.
 */
public class ChunkAtmosphereManager {

	public static final ChunkAtmosphereHandler proxy = new ChunkAtmosphereHandler();

	private static boolean registered;

	public static void register() {
		if(registered) return;
		registered = true;

		CelestialEnvironment.setLocalAtmosphere(proxy);
		HbmSealing.register();

		LifecycleEvent.SERVER_LEVEL_LOAD.register(proxy::receiveWorldLoad);
		LifecycleEvent.SERVER_LEVEL_UNLOAD.register(proxy::receiveWorldUnload);
		TickEvent.SERVER_LEVEL_POST.register(proxy::receiveWorldTick);
		TickEvent.SERVER_PRE.register(server -> proxy.receiveServerTickStart());

		BlockEvent.PLACE.register((level, pos, state, placer) -> {
			if(!level.isClientSide) proxy.receiveBlockPlaced(level, pos, state);
			return EventResult.pass();
		});
		BlockEvent.BREAK.register((level, pos, state, player, xp) -> {
			if(!level.isClientSide) {
				proxy.receiveBlockBroken(level, pos);
				if(state.is(Blocks.ICE) && isLowPressureWorld(level)) proxy.queueBlockEffect(level, pos, Blocks.WATER.defaultBlockState());
			}
			return EventResult.pass();
		});
		ExplosionEvent.DETONATE.register((level, explosion, entities) -> proxy.receiveDetonate(level, explosion));
		PlayerEvent.FILL_BUCKET.register(ChunkAtmosphereManager::onBucketUse);

		//? if forge {
		MinecraftForge.EVENT_BUS.addListener(ChunkAtmosphereManager::onLivingTick);
		MinecraftForge.EVENT_BUS.addListener(ChunkAtmosphereManager::onTreeGrow);
		//?} elif neoforge {
		/*NeoForge.EVENT_BUS.addListener(ChunkAtmosphereManager::onEntityTick);
		NeoForge.EVENT_BUS.addListener(ChunkAtmosphereManager::onTreeGrow);
		*///?}
	}

	//? if forge {
	private static void onLivingTick(LivingEvent.LivingTickEvent event) {
		AtmosphereEffects.onLivingTick(event.getEntity());
	}

	private static void onTreeGrow(SaplingGrowTreeEvent event) {
		if(event.getLevel() instanceof Level level) proxy.receiveTreeGrow(level, event.getPos());
	}
	//?} elif neoforge {
	/*private static void onEntityTick(EntityTickEvent.Pre event) {
		if(event.getEntity() instanceof LivingEntity living) AtmosphereEffects.onLivingTick(living);
	}

	private static void onTreeGrow(BlockGrowFeatureEvent event) {
		if(event.getLevel() instanceof Level level) proxy.receiveTreeGrow(level, event.getPos());
	}
	*///?}

	/** Original {@code WorldProviderCelestial}: {@code isHellWorld = pressure <= 0.2} (Orbit immer). */
	public static boolean isLowPressureWorld(Level level) {
		if(!SpaceDimensions.isSpaceDimension(level.dimension())) return false;
		if(CelestialBody.inOrbit(level)) return true;
		return CelestialEnvironment.celestialAtmosphere(level).getPressure() <= 0.2F;
	}

	/** Original {@code ModEventHandler.onBucketUse}. */
	private static CompoundEventResult<ItemStack> onBucketUse(Player player, Level level, ItemStack stack, HitResult target) {
		if(level.isClientSide) return CompoundEventResult.pass();
		if(!(target instanceof BlockHitResult hit) || target.getType() != HitResult.Type.BLOCK) return CompoundEventResult.pass();
		if(stack.isEmpty() || stack.getItem() != Items.WATER_BUCKET) return CompoundEventResult.pass();
		if(!isLowPressureWorld(level)) return CompoundEventResult.pass();

		BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
		if(proxy.hasLiquidPressure(proxy.getAtmosphere(level, pos))) return CompoundEventResult.pass();

		// Vanilla BucketItem in ultrawarm-Dimension: verdampfen
		level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 2.6F + (level.random.nextFloat() - level.random.nextFloat()) * 0.8F);
		if(level instanceof ServerLevel server) {
			for(int i = 0; i < 8; i++) {
				server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + Math.random(), pos.getY() + Math.random(), pos.getZ() + Math.random(), 1, 0, 0, 0, 0);
			}
		}
		return CompoundEventResult.interruptTrue(player.getAbilities().instabuild ? stack : new ItemStack(Items.BUCKET));
	}
}
