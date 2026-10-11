package com.space_m.atmosphere;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.config.SpaceConfig;
import com.space_m.main.SpaceRegistry;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1:1 NTM Space {@code AtmosphereBlob}: abgedichteter Raum als Flood-Fill (Tiefensuche ueber einen Stapel) ab der
 * Wurzel eines {@link IAtmosphereProvider}; Abbruch ("BANG HISSS"), sobald ein offener Block weiter als
 * {@code getMaxBlobRadius()} von der Wurzel entfernt erreicht wird.
 *
 * <p>Technisch noetige Abweichungen:
 * <ul>
 *   <li>{@code ThreeInts} -> {@link BlockPos}, {@code ForgeDirection.VALID_DIRECTIONS} -> {@link Direction#values()}
 *       (gleiche Reihenfolge D,U,N,S,W,E).</li>
 *   <li>Threading ({@code enableThreadedAtmospheres}): Die Suche laeuft wie im Original im Pool, liest aber Chunks ueber
 *       {@link ChunkLookup} (Server-Hauptthread liefert geladene Chunks, nie Laden; ungeladen = dicht wie
 *       {@code chunkExists}). Das <i>Ergebnis</i> (Graph fuellen, {@code onBlobCreated}, Zerfall mit
 *       Depress/Effekten) wird auf dem Server-Thread uebernommen - das Original setzte Bloecke aus dem Pool-Thread,
 *       was in 1.20+ nicht threadsicher ist.</li>
 *   <li>Volle Kollisionsbox: Cache je {@link BlockState} statt je Block (seit 1.13 sind Meta-Varianten wie Doppelstufen
 *       Zustaende desselben Blocks); Pruefung mit leerer Welt statt {@code null}.</li>
 *   <li>{@code BlockDummyable} -> HBM-Multiblocks ({@code IMultiblockController}/{@code UniversalMachinePartBlock});
 *       Tueren und andere "halb-dichte" Bloecke ueber {@link IBlockSealable} bzw. {@link SealProvider}
 *       (Raptors HBM-Tueren nur lesend, {@link HbmSealing}).</li>
 *   <li>Pflanzen-Wachstum ({@code IGrowable} + Metadaten) -> {@link BonemealableBlock} + Alters-Eigenschaft
 *       ({@code age}/{@code stage}).</li>
 * </ul>
 */
public class AtmosphereBlob implements Runnable {

	/**
	 * Somewhat based on the Advanced-Rocketry implementation, but extended to
	 * define the gases and gas pressure inside the enclosed volume
	 */

	// Graph containing the enclosed area
	protected final AdjacencyGraph<BlockPos> graph;

	// Handler, provides atmosphere information and receives callbacks
	protected IAtmosphereProvider handler;

	private static final AtomicInteger THREAD_ID = new AtomicInteger();
	private static ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(32), r -> {
		Thread t = new Thread(r, "space_m atmosphere #" + THREAD_ID.incrementAndGet());
		t.setDaemon(true);
		return t;
	});

	private static final ConcurrentHashMap<BlockState, Boolean> fullBounds = new ConcurrentHashMap<>();

	private volatile boolean executing;
	private BlockPos blockPos;

	// If true, run depressurization effects on blobbing failure
	public boolean runDepress;
	public Direction depressDir = Direction.UP;

	private LinkedHashMap<BlockPos, Integer> plants = new LinkedHashMap<>();

	public AtmosphereBlob(IAtmosphereProvider handler) {
		this.handler = handler;
		graph = new AdjacencyGraph<BlockPos>();
	}

	public IAtmosphereProvider getHandler() {
		return handler;
	}

	public boolean isPositionAllowed(Level world, BlockPos pos, ChunkLookup lookup) {
		return !isBlockSealed(world, pos, lookup);
	}

	// ─────────────────────────────── Abdichtung ───────────────────────────────

	/**
	 * Abdichtung durch Bloecke anderer Mods (HBM-Tueren, Multiblocks), die {@link IBlockSealable} nicht implementieren
	 * koennen. {@code null} = nicht zustaendig.
	 */
	public interface SealProvider {
		@Nullable
		Boolean isSealed(Level world, BlockPos pos, BlockState state, ChunkLookup lookup);

		/** true = Zustand kann sich ohne Platzieren/Abbauen aendern (Tuer) -> Handler beobachtet die Position. */
		default boolean isDynamic(BlockState state) { return false; }
	}

	private static final java.util.List<SealProvider> PROVIDERS = new java.util.concurrent.CopyOnWriteArrayList<>();

	public static void registerSealProvider(SealProvider provider) {
		PROVIDERS.add(provider);
	}

	/** true, wenn der Zustand an dieser Stelle sich ohne Blockwechsel aendern kann (Tueren). */
	public static boolean isDynamicSeal(BlockState state) {
		if(state.getBlock() instanceof IBlockSealable sealable) return sealable.isDynamic(state);
		for(SealProvider provider : PROVIDERS) {
			if(provider.isDynamic(state)) return true;
		}
		return false;
	}

	public static boolean isBlockSealed(Level world, BlockPos pos) {
		return isBlockSealed(world, pos, new ChunkLookup(world));
	}

	public static boolean isBlockSealed(Level world, BlockPos pos, ChunkLookup lookup) {
		if(world.isOutsideBuildHeight(pos)) return false;

		// Prevent loading new chunks, or we violate thread safety!
		LevelChunk chunk = lookup.chunk(pos.getX() >> 4, pos.getZ() >> 4);
		if(chunk == null) return true;

		BlockState state = chunk.getBlockState(pos);

		if(state.isAir()) return false; // Air obviously doesn't seal
		if(state.getBlock() instanceof FarmBlock) return false;
		if(state.getBlock() instanceof FenceBlock) return false;
		if(state.getBlock() instanceof IBlockSealable sealable) { // Custom semi-sealables, like doors
			if(sealable.isDynamic(state)) lookup.dynamic(pos);
			return sealable.isSealed(world, pos);
		}
		for(SealProvider provider : PROVIDERS) {
			Boolean sealed = provider.isSealed(world, pos, state, lookup);
			if(sealed != null) {
				if(provider.isDynamic(state)) lookup.dynamic(pos);
				return sealed;
			}
		}
		if(HbmSealing.isMultiblock(state)) return false; // Machines can't seal, almost all have gaps

		// Liquids need to know what pressurized atmosphere they're in to determine evaporation
		if(state.liquid() || !state.getFluidState().isEmpty() || !state.isSolid()) return false;
		if(state.is(BlockTags.LEAVES)) return false; // Leaves never block air

		Boolean isFull = fullBounds.get(state);
		if(isFull != null) return isFull;

		VoxelShape shape = null;
		try {
			// In the most common case, blocks don't use the passed World, only when they need to check neighbours to modify their bounding box
			shape = state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
		} catch(Exception ex) {}

		if(shape == null || shape.isEmpty()) {
			fullBounds.put(state, false);
			return false; // No collision, can't seal (like lamps)
		}

		AABB bb = shape.bounds();

		// size * 100 to correct rounding errors
		int minX = (int) (bb.minX * 100);
		int minY = (int) (bb.minY * 100);
		int minZ = (int) (bb.minZ * 100);
		int maxX = (int) (bb.maxX * 100);
		int maxY = (int) (bb.maxY * 100);
		int maxZ = (int) (bb.maxZ * 100);

		isFull = minX == 0 && minY == 0 && minZ == 0 && maxX == 100 && maxY == 100 && maxZ == 100;

		fullBounds.put(state, isFull);

		return isFull;
	}

	/**
	 * Chunkzugriff fuer die Abdichtungspruefung. Auf dem Server-Thread {@code getChunkNow}, im Pool-Thread
	 * {@code getChunk(x, z, false)} (laesst den Hauptthread liefern, laedt nie). Ein Chunk je Suche wird gemerkt.
	 */
	public static final class ChunkLookup {
		private final Level level;
		private final Long2ObjectOpenHashMap<LevelChunk> cache = new Long2ObjectOpenHashMap<>();
		private final Set<BlockPos> dynamicSeals = new HashSet<>();

		public ChunkLookup(Level level) {
			this.level = level;
		}

		@Nullable
		public LevelChunk chunk(int cx, int cz) {
			long key = ChunkPos.asLong(cx, cz);
			if(cache.containsKey(key)) return cache.get(key);
			LevelChunk chunk;
			if(level instanceof ServerLevel server && !server.getServer().isSameThread()) {
				chunk = server.getChunkSource().getChunk(cx, cz, false);
			} else {
				chunk = level.getChunkSource().getChunkNow(cx, cz);
			}
			cache.put(key, chunk);
			return chunk;
		}

		@Nullable
		public BlockState state(BlockPos pos) {
			LevelChunk chunk = chunk(pos.getX() >> 4, pos.getZ() >> 4);
			return chunk != null ? chunk.getBlockState(pos) : null;
		}

		@Nullable
		public BlockEntity blockEntity(BlockPos pos) {
			LevelChunk chunk = chunk(pos.getX() >> 4, pos.getZ() >> 4);
			return chunk != null ? chunk.getBlockEntity(pos) : null;
		}

		void dynamic(BlockPos pos) {
			dynamicSeals.add(pos.immutable());
		}

		public Set<BlockPos> dynamicSeals() {
			return dynamicSeals;
		}
	}

	// ─────────────────────────────── Gase ───────────────────────────────

	public int getBlobMaxRadius() {
		return handler.getMaxBlobRadius();
	}

	public boolean hasFluid(net.minecraft.resources.ResourceLocation fluid) {
		return hasFluid(fluid, 0.001);
	}

	public boolean hasFluid(net.minecraft.resources.ResourceLocation fluid, double abovePressure) {
		if(!fluid.equals(handler.getFluidType())) return false;
		return handler.getFluidPressure() >= abovePressure;
	}

	public boolean hasPressure(double abovePressure) {
		return handler.getFluidPressure() >= abovePressure;
	}

	public void consume(int amount) {
		handler.consume(amount);
	}

	public void produce(int amount) {
		handler.produce(amount);
	}

	// ─────────────────────────────── Graph ───────────────────────────────

	/**
	 * Adds a block position to the blob
	 */
	public void addBlock(int x, int y , int z) {
		addBlock(new BlockPos(x, y, z));
	}

	/**
	 * Recursively checks for contiguous blocks and adds them to the graph
	 */
	public void addBlock(BlockPos blockPos) {
		blockPos = blockPos.immutable();
		if(!this.contains(blockPos) &&
				(this.getBlobSize() == 0 || this.contains(blockPos.above()) || this.contains(blockPos.below()) ||
						this.contains(blockPos.east()) || this.contains(blockPos.west()) ||
						this.contains(blockPos.north()) || this.contains(blockPos.south()))) {
			if(!executing) {
				this.blockPos = blockPos;
				executing = true;

				if(SpaceConfig.get().enableThreadedAtmospheres) {
					try {
						pool.execute(this);
					} catch (RejectedExecutionException e) {
						SpaceRegistry.LOGGER.warn("Atmosphere calculation at " + this.getRootPosition() + " aborted due to oversize queue!");
						// Abweichung: Original laesst executing=true stehen (Blob danach dauerhaft tot)
						executing = false;
					}
				} else {
					this.run();
				}
			}
		}
	}

	/** true, solange eine Suche laeuft oder ihr Ergebnis noch nicht uebernommen ist. */
	public boolean isExecuting() {
		return executing;
	}

	private void addSingleBlock(BlockPos blockPos) {
		if(!graph.contains(blockPos)) {
			graph.add(blockPos, getPositionsToAdd(blockPos));
			Level world = handler.getWorld();
			if(world.getBlockState(blockPos).getBlock() instanceof BonemealableBlock) {
				addPlant(world, blockPos);
			}
		}
	}

	/**
	 * @return the BlockPosition of the root of the blob
	 */
	public BlockPos getRootPosition() {
		return handler.getRootPosition();
	}

	/**
	 * Gets adjacent blocks if they exist in the blob
	 * @param blockPos block to find things adjacent to
	 * @return list containing valid adjacent blocks
	 */
	protected HashSet<BlockPos> getPositionsToAdd(BlockPos blockPos) {
		HashSet<BlockPos> set = new HashSet<>();

		for(Direction direction : Direction.values()) {

			BlockPos offset = blockPos.relative(direction);
			if(graph.contains(offset))
				set.add(offset);
		}

		return set;
	}

	/**
	 * Given a block position returns whether or not it exists in the graph
	 * @return true if the block exists in the blob
	 */
	public boolean contains(BlockPos position) {
		boolean contains;

		synchronized (graph) {
			contains = graph.contains(position);
		}

		return contains;
	}

	/**
	 * Given a block position returns whether or not it exists in the graph
	 * @return true if the block exists in the blob
	 */
	public boolean contains(int x, int y, int z) {
		return contains(new BlockPos(x, y, z));
	}

	/**
	 * Removes the block at the given coords for this blob
	 */
	public void removeBlock(BlockPos blockPos) {
		synchronized (graph) {
			graph.remove(blockPos);

			for(Direction direction : Direction.values()) {

				BlockPos newBlock = blockPos.relative(direction);
				if(graph.contains(newBlock) && !graph.doesPathExist(newBlock, handler.getRootPosition()))
					runEffectOnWorldBlocks(handler.getWorld(), graph.removeAllNodesConnectedTo(newBlock));
			}
		}
	}

	/**
	 * Removes all nodes from the blob
	 */
	public void clearBlob() {
		Level world = handler.getWorld();

		synchronized (graph) {
			runEffectOnWorldBlocks(world, new HashSet<>(getLocations()));

			graph.clear();
		}
	}

	/**
	 * @return a set containing all locations
	 */
	public Set<BlockPos> getLocations() {
		return graph.getKeys();
	}

	/**
	 * @return the number of elements in the blob
	 */
	public int getBlobSize() {
		synchronized (graph) {
			return graph.size();
		}
	}

	@Override
	public void run() {
		final Level world = handler.getWorld();
		final BlockPos start = blockPos;
		final ChunkLookup lookup = new ChunkLookup(world);

		Stack<BlockPos> stack = new Stack<>();
		stack.push(start);

		final int maxSize = this.getBlobMaxRadius();
		final HashSet<BlockPos> addableBlocks = new HashSet<>();

		// Breadth first search; non recursive
		while(!stack.isEmpty()) {
			BlockPos stackElement = stack.pop();
			addableBlocks.add(stackElement);

			for(Direction dir : Direction.values()) {
				BlockPos searchNextPosition = stackElement.relative(dir);

				// Don't path areas we have already scanned
				if(!contains(searchNextPosition) && !addableBlocks.contains(searchNextPosition)) {

					try {
						if(isPositionAllowed(world, searchNextPosition, lookup)) {
							if(searchNextPosition.distSqr(this.getRootPosition()) <= (double) maxSize * maxSize) {
								stack.push(searchNextPosition);
								addableBlocks.add(searchNextPosition);
							} else {
								// BANG HISSS
								final boolean depress = runDepress;
								final Direction dirDepress = depressDir;
								finish(world, lookup, () -> {
									if(depress) decompress(start, dirDepress);

									// Failed to seal, void
									clearBlob();
								});
								return;
							}
						}
					} catch (Exception e) {
						// Catches errors with additional information
						SpaceRegistry.LOGGER.info("Error: AtmosphereBlob has failed to form correctly due to an error. \nCurrentBlock: " + stackElement + "\tNextPos: " + searchNextPosition + "\tDir: " + dir + "\tStackSize: " + stack.size());
						e.printStackTrace();

						// Failed to seal, void
						finish(world, lookup, this::clearBlob);
						return;
					}
				}
			}
		}

		//only one instance can editing this at a time because this will not run again b/c "worker" is not null
		finish(world, lookup, () -> {
			synchronized (graph) {
				for(BlockPos addableBlock : addableBlocks) {
					addSingleBlock(addableBlock);
				}

				handler.onBlobCreated(this);
			}
		});
	}

	/** Uebernimmt das Suchergebnis auf dem Server-Thread (direkt, wenn die Suche schon dort lief). */
	private void finish(Level world, ChunkLookup lookup, Runnable result) {
		Runnable task = () -> {
			try {
				ChunkAtmosphereManager.proxy.watchSealables(world, lookup.dynamicSeals());
				result.run();
			} finally {
				executing = false;
			}
		};

		MinecraftServer server = world.getServer();
		if(server == null || server.isSameThread()) {
			task.run();
		} else {
			server.execute(task);
		}
	}

	/**
	 * @param world
	 * @param blocks Collection containing affected locations
	 */
	protected void runEffectOnWorldBlocks(Level world, Collection<BlockPos> blocks) {
		BlockPos root = handler.getRootPosition();
		CBT_Atmosphere newAtmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(world, root, this);

		for(BlockPos pos : blocks) {
			final BlockState block = world.getBlockState(pos);
			ChunkAtmosphereManager.proxy.runEffectsOnBlock(newAtmosphere, world, block, pos);
		}
	}

	public void decompress(BlockPos pos, Direction dir) {
		Level world = handler.getWorld();

		EntityDepress depress = new EntityDepress(world, dir.getOpposite(), 20);
		depress.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
		world.addFreshEntity(depress);

		world.playSound(null, depress.getX(), depress.getY(), depress.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0F, 1.6F);
		world.playSound(null, depress.getX(), depress.getY(), depress.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 0.25F);
	}

	// ─────────────────────────────── Pflanzen ───────────────────────────────

	public void checkGrowth() {
		Level world = handler.getWorld();

		Iterator<HashMap.Entry<BlockPos, Integer>> iterator = plants.entrySet().iterator();
		while(iterator.hasNext()) {
			HashMap.Entry<BlockPos, Integer> entry = iterator.next();
			BlockPos pos = entry.getKey();
			int oldMeta = entry.getValue();
			BlockState state = world.getBlockState(pos);

			if(!(state.getBlock() instanceof BonemealableBlock)) {
				iterator.remove();
				continue;
			}

			int newMeta = growthStage(state);

			if(newMeta != oldMeta) {
				entry.setValue(newMeta);
				produce(Math.max(newMeta - oldMeta, 0) * ChunkAtmosphereHandler.CROP_GROWTH_CONVERSION);
			}
		}
	}

	public void addPlant(Level world, BlockPos pos) {
		plants.put(pos.immutable(), growthStage(world.getBlockState(pos)));
	}

	/** Ersatz fuer die Metadaten des Originals: Wert der Alters-Eigenschaft ({@code age}/{@code stage}), sonst 0. */
	public static int growthStage(BlockState state) {
		for(Property<?> property : state.getProperties()) {
			if(property instanceof IntegerProperty integer && ("age".equals(property.getName()) || "stage".equals(property.getName()))) {
				return state.getValue(integer);
			}
		}
		return 0;
	}

}
