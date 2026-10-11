package com.space_m.atmosphere;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.fluid.trait.FT_Corrosive;
import com.space_m.celestial.CelestialEnvironment;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.celestial.trait.CBT_Atmosphere.FluidEntry;
import com.space_m.lib.SpaceTags;
import com.space_m.main.SpaceRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code ChunkAtmosphereHandler} ("Misnomer, this isn't actually chunk based"): alle abgedichteten
 * Raeume je Dimension, ortsabhaengige Atmosphaere (Koerper + Raeume), Atem-/Druck-/Korrosionsbedingungen,
 * Welt-Effekte (Fackeln/Feuer brauchen O2, Wasser Druck, Pflanzen Luft), Terraforming-Wachstum.
 *
 * <p>Verdrahtung: {@link ChunkAtmosphereManager} (Ereignisse), {@link CelestialEnvironment#setLocalAtmosphere}
 * (HBM-API: Druck, Gase, Verbrennung/{@code breatheAir}).
 *
 * <p>Abweichungen (technisch): Schluessel {@code ResourceKey<Level>} statt Dimensions-ID; auf dem Client (und fuer nicht
 * geladene Welten) liefert {@link #getAtmosphere(Level, BlockPos)} die Koerper-Atmosphaere statt {@code null}
 * (Original kannte dort keine Blob-Liste; im Einzelspieler teilen sich Client und Server sonst die Dimensionsschluessel);
 * {@code IPlantable} -> {@link #isPlant}; Welt-Effekte beim Platzieren laufen einen Tick spaeter
 * ({@link #queueBlockEffect}), weil das Platzier-Ereignis in 1.20+ vor dem Block-Update an den Client feuert.
 */
public class ChunkAtmosphereHandler implements CelestialEnvironment.LocalAtmosphere {

	// Misnomer, this isn't actually "chunk" based, but instead a global handler
	// ah who feckin cares, for all you need to know, this is _magic_
	// Breathe easy, bub

	private final HashMap<ResourceKey<Level>, HashMap<IAtmosphereProvider, AtmosphereBlob>> worldBlobs = new HashMap<>();
	public static final int MAX_BLOB_RADIUS = 256;

	// How much CO2 is converted into O2 from various growing
	// Balanced around these amounts of plants providing for a single pressurized room:
	//  * 25 trees (955s to grow), OR
	//  * 200 crops (300s per stage)
	public static final int TREE_GROWTH_CONVERSION = 400; // per sapling -> tree
	public static final int CROP_GROWTH_CONVERSION = 15; // per stage
	public static final int GRASS_GROWTH_CONVERSION = 100; // per block

	/*
	 * Methods to get information about the current atmosphere
	 */
	@Nullable
	public CBT_Atmosphere getAtmosphere(Entity entity) {
		return getAtmosphere(entity.level(), BlockPos.containing(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ()), null);
	}

	@Override
	@Nullable
	public CBT_Atmosphere getAtmosphere(Level world, BlockPos pos) {
		return getAtmosphere(world, pos, null);
	}

	@Nullable
	public CBT_Atmosphere getAtmosphere(Level world, BlockPos pos, @Nullable AtmosphereBlob excludeBlob) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = world.isClientSide ? null : worldBlobs.get(world.dimension());

		CBT_Atmosphere atmosphere = testVacuum != null && testVacuum.test(world, pos) ? new CBT_Atmosphere() : getCelestialAtmosphere(world);

		if(blobs != null) {
			for(AtmosphereBlob blob : blobs.values()) {
				if(blob == excludeBlob) continue;

				if(blob.contains(pos)) {
					double pressure = blob.handler.getFluidPressure();
					if(pressure < 0) {
						atmosphere.reduce(pressure);
					} else {
						atmosphere.add(blob.handler.getFluidType(), pressure);
					}
				}
			}
		}

		if(atmosphere.fluids.size() == 0) return null;

		return atmosphere;
	}

	/**
	 * Nur GameTests: Bereich, der als Vakuum gilt (NeoForge-GameTest-Server laedt keine Datapack-Dimensionen, dort
	 * laufen die Atmosphaeren-Tests in der Oberwelt). Im Spiel immer {@code null}.
	 */
	@Nullable
	public static java.util.function.BiPredicate<Level, BlockPos> testVacuum = null;

	// returns a atmosphere that is safe for modification
	private CBT_Atmosphere getCelestialAtmosphere(Level world) {
		return CelestialEnvironment.celestialAtmosphere(world);
	}

	public List<AtmosphereBlob> getBlobs(Level world, BlockPos pos) {
		List<AtmosphereBlob> inBlobs = new ArrayList<AtmosphereBlob>();

		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = world.isClientSide ? null : worldBlobs.get(world.dimension());

		if(blobs == null) return inBlobs;

		for(AtmosphereBlob blob : blobs.values()) {
			if(blob.contains(pos)) {
				inBlobs.add(blob);
			}
		}

		return inBlobs;
	}

	public boolean hasAtmosphere(Level world, BlockPos pos) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = world.isClientSide ? null : worldBlobs.get(world.dimension());

		if(blobs == null) return false;

		for(AtmosphereBlob blob : blobs.values()) {
			if(blob.contains(pos)) {
				return true;
			}
		}

		return false;
	}

	public List<AtmosphereBlob> getBlobsWithinRadius(Level world, BlockPos pos) {
		return getBlobsWithinRadius(world, pos, MAX_BLOB_RADIUS);
	}

	public List<AtmosphereBlob> getBlobsWithinRadius(Level world, BlockPos pos, int radius) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(world.dimension());
		List<AtmosphereBlob> list = new LinkedList<AtmosphereBlob>();

		if(blobs == null || world.isClientSide) return list;

		double radiusSqr = (double) radius * radius;

		for(AtmosphereBlob blob : blobs.values()) {
			if(blob.getRootPosition().distSqr(pos) <= radiusSqr) {
				list.add(blob);
			}
		}

		return list;
	}

	/** Alle Raeume einer Dimension (GameTests, Scanner). */
	public Collection<AtmosphereBlob> getAllBlobs(Level world) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(world.dimension());
		return blobs != null ? blobs.values() : List.of();
	}

	@Nullable
	public AtmosphereBlob getBlob(IAtmosphereProvider handler) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(handler.getWorld().dimension());
		return blobs != null ? blobs.get(handler) : null;
	}

	// Assuming 21% AIR/9% OXY is required for breathable atmosphere
	public boolean canBreathe(LivingEntity entity) {
		CBT_Atmosphere atmosphere = getAtmosphere(entity);

		if(com.hbm_m.config.GeneralConfig.enableDebugMode && entity instanceof Player && entity.level().getGameTime() % 20 == 0) {
			if(atmosphere != null && atmosphere.fluids.size() > 0) {
				for(FluidEntry entry : atmosphere.fluids) {
					SpaceRegistry.LOGGER.info("Atmosphere: " + entry.fluid + " - " + entry.pressure + "bar");
				}
			} else {
				SpaceRegistry.LOGGER.info("Atmosphere: TOTAL VACUUM");
			}
		}

		return canBreathe(atmosphere);
	}

	public boolean canBreathe(@Nullable CBT_Atmosphere atmosphere) {
		return atmosphere != null && (atmosphere.hasFluid(CelestialFluids.EARTHAIR, 0.21) || atmosphere.hasFluid(CelestialFluids.OXYGEN, 0.09));
	}

	// Is the air pressure high enough to support liquids
	public boolean hasLiquidPressure(@Nullable CBT_Atmosphere atmosphere) {
		return atmosphere != null && atmosphere.getPressure() >= 0.19D;
	}

	public boolean willCorrode(LivingEntity entity) {
		return willCorrode(getAtmosphere(entity));
	}

	public boolean willCorrode(@Nullable CBT_Atmosphere atmosphere) {
		return atmosphere != null && atmosphere.hasTrait(FT_Corrosive.class, 0.2);
	}

	/** Original {@code block instanceof IPlantable} (Pflanzen, die Luft brauchen). */
	public static boolean isPlant(BlockState state) {
		Block block = state.getBlock();
		return block instanceof IPlantableBreathing || block instanceof BushBlock || block instanceof SugarCaneBlock
				|| block instanceof CactusBlock || state.is(SpaceTags.BREATHING_PLANTS);
	}

	/**
	 * Actions to rectify world status based on atmosphere
	 */
	private boolean runEffectsOnBlock(@Nullable CBT_Atmosphere atmosphere, Level world, BlockState state, BlockPos pos, boolean fetchAtmosphere) {
		Block block = state.getBlock();
		boolean requiresPressure = block == Blocks.WATER;
		boolean requiresO2 = (block instanceof TorchBlock && !(block instanceof RedstoneTorchBlock)) || block instanceof BaseFireBlock;
		boolean requiresCO2 = isPlant(state);

		if(!requiresO2 && !requiresCO2 && !requiresPressure) return false;

		if(fetchAtmosphere) {
			atmosphere = getAtmosphere(world, pos);
		}

		boolean canExist = true;

		if(requiresO2) {
			// Check for an atmosphere and destroy torches if there is insufficient oxygen
			canExist = !(atmosphere == null || (!atmosphere.hasFluid(CelestialFluids.OXYGEN, 0.09) && !atmosphere.hasFluid(CelestialFluids.EARTHAIR, 0.21)));
		} else if(requiresPressure) {
			canExist = hasLiquidPressure(atmosphere);
		} else if(requiresCO2) {
			// TODO: Make plants rely on CO2 once CO2 is more readily available (via natural gas most likely)
			if(block instanceof IPlantableBreathing breathing) {
				canExist = breathing.canBreathe(atmosphere);
			} else {
				canExist = !(atmosphere == null || (!atmosphere.hasFluid(CelestialFluids.OXYGEN, 0.01) && !atmosphere.hasFluid(CelestialFluids.EARTHAIR, 0.1)));
			}
		}

		if(canExist) return false;

		Block.dropResources(state, world, pos);
		world.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

		return true;
	}

	public boolean runEffectsOnBlock(@Nullable CBT_Atmosphere atmosphere, Level world, BlockState state, BlockPos pos) {
		return runEffectsOnBlock(atmosphere, world, state, pos, false);
	}

	public boolean runEffectsOnBlock(Level world, BlockState state, BlockPos pos) {
		return runEffectsOnBlock(null, world, state, pos, true);
	}


	/*
	 * Registration methods
	 */
	public void registerAtmosphere(IAtmosphereProvider handler) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(handler.getWorld().dimension());
		if(blobs == null) return;
		AtmosphereBlob blob = blobs.get(handler);

		if(blob == null) {
			blob = new AtmosphereBlob(handler);
			blob.addBlock(handler.getRootPosition());
			blobs.put(handler, blob);
		}
	}

	public void unregisterAtmosphere(IAtmosphereProvider handler) {
		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(handler.getWorld().dimension());
		if(blobs == null) return;
		blobs.remove(handler);
	}


	/*
	 * Hooks to update our volumes as necessary
	 */
	public void onSealableChange(Level world, BlockPos pos, IBlockSealable sealable, List<AtmosphereBlob> nearbyBlobs) {
		if(world.isClientSide) return;
		onSealableChange(world, pos, sealable.isSealed(world, pos), nearbyBlobs);
	}

	public void onSealableChange(Level world, BlockPos pos, boolean sealed, List<AtmosphereBlob> nearbyBlobs) {
		if(world.isClientSide) return;

		if(sealed) {
			for(AtmosphereBlob blob : nearbyBlobs) {
				if(blob.contains(pos)) {
					blob.removeBlock(pos);
				} else if(!blob.contains(blob.getRootPosition())) {
					blob.runDepress = false;
					blob.addBlock(blob.getRootPosition());
				}
			}
		} else {
			onBlockRemoved(world, pos);
		}
	}

	public void onBlockPlaced(Level world, BlockPos pos) {
		if(!AtmosphereBlob.isBlockSealed(world, pos)) return;

		List<AtmosphereBlob> nearbyBlobs = getBlobsWithinRadius(world, pos);
		for(AtmosphereBlob blob : nearbyBlobs) {
			if(blob.contains(pos)) {
				blob.removeBlock(pos);
			} else if(!blob.contains(blob.getRootPosition())) {
				blob.runDepress = false;
				blob.addBlock(blob.getRootPosition());
			}
		}
	}

	public void onBlockRemoved(Level world, BlockPos pos) {
		List<AtmosphereBlob> nearbyBlobs = getBlobsWithinRadius(world, pos);
		for(AtmosphereBlob blob : nearbyBlobs) {
			// Make sure that a block can actually be attached to the blob
			for(Direction dir : Direction.values()) {
				if(blob.contains(pos.relative(dir))) {
					blob.runDepress = true;
					blob.depressDir = dir;
					blob.addBlock(pos);
					break;
				}
			}
		}
	}

	/*
	 * Beobachtete Tuer-/Sonderbloecke (Ersatz fuer den updateSealedState-Aufruf in TileEntityDoorGeneric)
	 */
	private final HashMap<ResourceKey<Level>, HashMap<BlockPos, Boolean>> watchedSeals = new HashMap<>();

	public void watchSealables(Level world, Collection<BlockPos> positions) {
		if(positions.isEmpty() || world.isClientSide) return;
		HashMap<BlockPos, Boolean> watched = watchedSeals.computeIfAbsent(world.dimension(), k -> new HashMap<>());
		for(BlockPos pos : positions) {
			if(!watched.containsKey(pos)) watched.put(pos, AtmosphereBlob.isBlockSealed(world, pos));
		}
	}

	private void tickWatchedSeals(ServerLevel world) {
		HashMap<BlockPos, Boolean> watched = watchedSeals.get(world.dimension());
		if(watched == null || watched.isEmpty()) return;

		List<Map.Entry<BlockPos, Boolean>> changed = new ArrayList<>();
		Iterator<Map.Entry<BlockPos, Boolean>> iterator = watched.entrySet().iterator();
		while(iterator.hasNext()) {
			Map.Entry<BlockPos, Boolean> entry = iterator.next();
			BlockPos pos = entry.getKey();
			if(!world.isLoaded(pos)) continue;
			if(!AtmosphereBlob.isDynamicSeal(world.getBlockState(pos))) {
				iterator.remove();
				continue;
			}
			boolean sealed = AtmosphereBlob.isBlockSealed(world, pos);
			if(sealed != entry.getValue()) {
				entry.setValue(sealed);
				changed.add(Map.entry(pos, sealed));
			}
		}

		for(Map.Entry<BlockPos, Boolean> entry : changed) {
			onSealableChange(world, entry.getKey(), entry.getValue(), getBlobsWithinRadius(world, entry.getKey()));
		}
	}


	/*
	 * Event handlers
	 */
	public void receiveWorldLoad(ServerLevel world) {
		worldBlobs.put(world.dimension(), new HashMap<>());
		growthMap.put(world.dimension(), new ArrayDeque<>());
	}

	public void receiveWorldUnload(ServerLevel world) {
		worldBlobs.remove(world.dimension());
		growthMap.remove(world.dimension());
		watchedSeals.remove(world.dimension());
		pendingEffects.remove(world.dimension());
	}

	public void receiveWorldTick(ServerLevel world) {
		runPendingEffects(world);
		tickWatchedSeals(world);

		tickTerraforming(world);

		if(world.getGameTime() % 20 != 0) return;

		HashMap<IAtmosphereProvider, AtmosphereBlob> blobs = worldBlobs.get(world.dimension());
		if(blobs == null) return;
		for(AtmosphereBlob blob : new ArrayList<>(blobs.values())) {
			blob.checkGrowth();
		}
	}

	/** Original {@code ModEventHandler.onBlockPlaced} (Atmosphaeren-Teil) + {@code receiveBlockPlaced}. */
	public void receiveBlockPlaced(Level world, BlockPos pos, BlockState state) {
		if(world.isClientSide) return;
		queueBlockEffect(world, pos, state);
		onBlockPlaced(world, pos);
	}

	public void receiveBlockBroken(Level world, BlockPos pos) {
		if(world.isClientSide) return;
		onBlockRemoved(world, pos);
	}

	// Platzier-Effekte (Fackel ohne O2 usw.), einen Tick spaeter ausgefuehrt
	private final HashMap<ResourceKey<Level>, List<BlockPos>> pendingEffects = new HashMap<>();

	public void queueBlockEffect(Level world, BlockPos pos, BlockState state) {
		if(!(state.getBlock() instanceof TorchBlock || state.getBlock() instanceof BaseFireBlock || state.is(Blocks.WATER)
				|| isPlant(state) || state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock)) return;
		pendingEffects.computeIfAbsent(world.dimension(), k -> new ArrayList<>()).add(pos.immutable());
	}

	private void runPendingEffects(ServerLevel world) {
		List<BlockPos> pending = pendingEffects.remove(world.dimension());
		if(pending == null) return;
		for(BlockPos pos : pending) {
			BlockState state = world.getBlockState(pos);
			boolean placeCancelled = runEffectsOnBlock(world, state, pos);
			if(placeCancelled) continue;

			if(state.getBlock() instanceof net.minecraft.world.level.block.BonemealableBlock) {
				trackPlant(world, pos);
			}
		}
	}

	// Stores explosion events to execute unsealing after they complete, and the blocks that actually unseal from the explosion
	private record PendingExplosion(Level world, Vec3 center, int radius, List<BlockPos> unsealing) {}
	private static final List<PendingExplosion> explosions = new ArrayList<>();

	// For every affected blob, attempt addBlock once for the first valid position only
	// This just stores the affected blocks, since the explosion hasn't actually occurred yet
	public void receiveDetonate(Level world, Explosion explosion) {
		if(world.isClientSide) return;
		//? if < 1.21.1 {
		Vec3 center = explosion.getPosition();
		//?} else {
		/*Vec3 center = explosion.center();
		*///?}
		List<BlockPos> unsealingBlocks = new ArrayList<BlockPos>();
		double maxDist = 0;
		for(BlockPos block : explosion.getToBlow()) {
			if(AtmosphereBlob.isBlockSealed(world, block)) {
				unsealingBlocks.add(block.immutable());
			}
			maxDist = Math.max(maxDist, Math.sqrt(block.distToCenterSqr(center)));
		}
		// Original: MAX_BLOB_RADIUS + ceil(explosionSize); 1.20.1 hat keinen Radius-Getter -> weitester betroffener Block
		explosions.add(new PendingExplosion(world, center, MAX_BLOB_RADIUS + Mth.ceil(maxDist), unsealingBlocks));
	}

	// If we stored any explosions, process them now
	public void receiveServerTickStart() {
		if(explosions.isEmpty()) return;

		for(PendingExplosion pair : new ArrayList<>(explosions)) {
			BlockPos explosion = BlockPos.containing(pair.center);
			List<AtmosphereBlob> nearbyBlobs = getBlobsWithinRadius(pair.world, explosion, pair.radius);

			for(BlockPos pos : pair.unsealing) {
				if(nearbyBlobs.size() == 0) break;

				Iterator<AtmosphereBlob> iterator = nearbyBlobs.iterator();

				while(iterator.hasNext()) {
					AtmosphereBlob blob = iterator.next();
					for(Direction dir : Direction.values()) {
						if(blob.contains(pos.relative(dir))) {
							blob.runDepress = true;
							blob.depressDir = dir;
							blob.addBlock(pos);
							iterator.remove();
							break;
						}
					}
				}
			}
		}

		explosions.clear();
	}

	public void receiveTreeGrow(Level world, BlockPos pos) {
		if(world.isClientSide) return;
		List<AtmosphereBlob> nearbyBlobs = getBlobsWithinRadius(world, pos, MAX_BLOB_RADIUS);
		for(AtmosphereBlob blob : nearbyBlobs) {
			if(blob.contains(pos)) {
				blob.produce(TREE_GROWTH_CONVERSION);
				break;
			}
		}
	}

	public void trackPlant(Level world, BlockPos pos) {
		List<AtmosphereBlob> nearbyBlobs = getBlobsWithinRadius(world, pos);
		for(AtmosphereBlob blob : nearbyBlobs) {
			if(blob.contains(pos)) {
				blob.addPlant(world, pos);
			}
		}
	}

	// ─────────────────────────────── HBM-API (CelestialEnvironment.LocalAtmosphere) ───────────────────────────────

	/** Blob-Teil von {@code TileEntityMachineBase.breatheAir}. */
	@Override
	public boolean breatheFromBlob(Level level, BlockPos pos, int amount) {
		List<AtmosphereBlob> blobs = getBlobs(level, pos);
		for(AtmosphereBlob blob : blobs) {
			if(blob.hasFluid(CelestialFluids.EARTHAIR, 0.19) || blob.hasFluid(CelestialFluids.OXYGEN, 0.09)) {
				blob.consume(amount);
				return true;
			}
		}

		return false;
	}

	// ─────────────────────────────── Terraforming ───────────────────────────────

	private static final HashMap<ResourceKey<Level>, Queue<Growth>> growthMap = new HashMap<>();

	private void tickTerraforming(ServerLevel world) {
		Queue<Growth> growths = growthMap.get(world.dimension());
		if(growths == null) return;

		for(int g = 0; g < 64; g++) {
			Growth growth = growths.poll();

			if(growth == null) return;

			for(int i = 0; i < 4; i++) {
				Direction dir = Direction.values()[world.random.nextInt(4) + 2];

				int x = growth.x + dir.getStepX();
				int z = growth.z + dir.getStepZ();

				for(int y = growth.y + 1; y >= growth.y - 1; y--) {
					BlockPos pos = new BlockPos(x, y, z);
					Block block = world.getBlockState(pos).getBlock();

					boolean shouldReplace = growth.from != null
						? block == growth.from
						: block == Blocks.DIRT || block == ModBlocks.WASTE_EARTH.get() || block == ModBlocks.DIRT_DEAD.get() || block == ModBlocks.DIRT_OILY.get();

					if(shouldReplace) {
						world.setBlock(pos, growth.into.defaultBlockState(), 3);

						if(growth.into == Blocks.GRASS_BLOCK) world.levelEvent(2005, pos.above(), 0);

						if(growth.time > 0) addGrowth(world, growth.from, growth.into, x, y, z, growth.time - 1);

						return;
					}
				}
			}
		}
	}

	public void addGrowth(Level world, Block into, int x, int y, int z, int size) {
		addGrowth(world, null, into, x, y, z, size);
	}

	public void addGrowth(Level world, Block into, int x, int y, int z, int count, int size) {
		addGrowth(world, null, into, x, y, z, count, size);
	}

	public void addGrowth(Level world, @Nullable Block from, Block into, int x, int y, int z, int size) {
		Queue<Growth> growths = growthMap.get(world.dimension());
		if(growths == null) return;
		growths.add(new Growth(from, into, x, y, z, size));
	}

	public void addGrowth(Level world, @Nullable Block from, Block into, int x, int y, int z, int count, int size) {
		Queue<Growth> growths = growthMap.get(world.dimension());
		if(growths == null) return;
		for(int i = 0; i < count; i++) {
			growths.add(new Growth(from, into, x, y, z, size));
		}
	}

	private static class Growth {

		public final Block from;
		public final Block into;
		public final int time; // remaining

		public final int x;
		public final int y;
		public final int z;

		public Growth(@Nullable Block from, Block into, int x, int y, int z, int time) {
			this.from = from;
			this.into = into;
			this.time = time;

			this.x = x;
			this.y = y;
			this.z = z;
		}

	}

}
