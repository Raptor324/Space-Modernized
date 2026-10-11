package com.space_m.test;

import java.util.List;

import com.hbm_m.api.space.SpaceApi;
import com.space_m.SpaceMod;
import com.space_m.atmosphere.AtmosphereBlob;
import com.space_m.atmosphere.AtmosphereEffects;
import com.space_m.atmosphere.ChunkAtmosphereManager;
import com.space_m.atmosphere.EntityDepress;
import com.space_m.atmosphere.IAtmosphereProvider;
import com.space_m.celestial.CelestialFluids;
import com.space_m.celestial.trait.CBT_Atmosphere;
import com.space_m.dim.SpaceDimensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
//? if forge {
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
//?} elif neoforge {
/*import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
*///?}

/**
 * R2-A: Atmosphaere gegen NTM Space ({@code AtmosphereBlob}, {@code ChunkAtmosphereHandler},
 * {@code EntityEffectHandler.handleOxy}, {@code HbmLivingProps.setOxy}). Alle Raeume werden in der Mun-Dimension
 * (Vakuum) in 200 Hoehe gebaut (eigene, zwangsgeladene Chunks je Test).
 */
@GameTestHolder(SpaceMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AtmosphereGameTest {

	private AtmosphereGameTest() {}

	private static void check(boolean cond, String msg) {
		if(!cond) throw new GameTestAssertException(msg);
	}

	/**
	 * Mun (Vakuum). Der NeoForge-1.21.1-GameTestServer laedt keine Datapack-Dimensionen (s. DimensionGameTest) - dann
	 * Oberwelt mit Test-Vakuum im Testbereich (y 190-210, x 0-280, z 0-20); Pruefungen, die die Koerper-Atmosphaere
	 * direkt lesen (Verbrennung draussen, drucklose Welt), entfallen dort.
	 */
	private static ServerLevel mun(GameTestHelper helper) {
		ServerLevel level = helper.getLevel().getServer().getLevel(SpaceDimensions.MUN);
		if(level != null) return level;
		com.space_m.atmosphere.ChunkAtmosphereHandler.testVacuum = (l, p) -> l.dimension() == Level.OVERWORLD
				&& p.getY() >= 190 && p.getY() <= 210 && p.getX() >= 0 && p.getX() <= 280 && p.getZ() >= 0 && p.getZ() <= 20;
		return helper.getLevel().getServer().overworld();
	}

	private static boolean realMun(Level level) {
		return level.dimension() == SpaceDimensions.MUN;
	}

	/** Testquelle: ein Fluid mit festem Druck (Original IAtmosphereProvider, z.B. Luftpumpe). */
	private static final class TestProvider implements IAtmosphereProvider {
		final Level level;
		final BlockPos root;
		final ResourceLocation fluid;
		final double pressure;
		int consumed;
		int created;

		TestProvider(Level level, BlockPos root, ResourceLocation fluid, double pressure) {
			this.level = level;
			this.root = root;
			this.fluid = fluid;
			this.pressure = pressure;
		}

		@Override public Level getWorld() { return level; }
		@Override public int getMaxBlobRadius() { return 16; }
		@Override public BlockPos getRootPosition() { return root; }
		@Override public ResourceLocation getFluidType() { return fluid; }
		@Override public double getFluidPressure() { return pressure; }
		@Override public void consume(int amount) { consumed += amount; }
		@Override public void produce(int amount) { }
		@Override public void onBlobCreated(AtmosphereBlob blob) { created++; }
	}

	/** Hohlwuerfel 5x5x5 aus Stein (Innenraum 3x3x3 = 27 Bloecke) um {@code center}; Umgebung geleert. */
	private static void buildRoom(ServerLevel level, BlockPos center) {
		int cx = center.getX() >> 4, cz = center.getZ() >> 4;
		level.setChunkForced(cx, cz, true);
		level.getChunk(cx, cz);
		for(int x = -3; x <= 3; x++) for(int y = -3; y <= 3; y++) for(int z = -3; z <= 3; z++) {
			int m = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
			level.setBlock(center.offset(x, y, z), m == 2 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
		}
	}

	private static void clearRoom(ServerLevel level, BlockPos center) {
		for(int x = -3; x <= 3; x++) for(int y = -3; y <= 3; y++) for(int z = -3; z <= 3; z++) {
			level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
		}
		level.setChunkForced(center.getX() >> 4, center.getZ() >> 4, false);
	}

	private static AtmosphereBlob blob(TestProvider provider) {
		AtmosphereBlob blob = ChunkAtmosphereManager.proxy.getBlob(provider);
		check(blob != null, "Blob nicht registriert");
		return blob;
	}

	private static void waitFormed(TestProvider provider, int size) {
		AtmosphereBlob blob = blob(provider);
		check(!blob.isExecuting(), "Flood-Fill laeuft noch");
		check(blob.getBlobSize() == size, "Blobgroesse " + blob.getBlobSize() + ", erwartet " + size);
	}

	/** Abgedichteter Raum mit Luft: atembar, Verbrennung moeglich und verbraucht; draussen Mun-Vakuum. */
	@GameTest(template = "empty3x3x3", batch = "space_atmosphere", timeoutTicks = 200)
	public static void sealedRoomHoldsAir(GameTestHelper helper) {
		ServerLevel level = mun(helper);
		BlockPos center = new BlockPos(8, 200, 8);
		buildRoom(level, center);
		TestProvider provider = new TestProvider(level, center, CelestialFluids.EARTHAIR, 1.0);
		ChunkAtmosphereManager.proxy.registerAtmosphere(provider);

		helper.succeedWhen(() -> {
			waitFormed(provider, 27);
			check(provider.created == 1, "onBlobCreated " + provider.created);
			BlockPos corner = center.offset(1, 1, 1);
			BlockPos outside = center.offset(0, 6, 0);

			CBT_Atmosphere inside = ChunkAtmosphereManager.proxy.getAtmosphere(level, corner);
			check(inside != null && inside.hasFluid(CelestialFluids.EARTHAIR, 0.21), "Raum hat Luft");
			check(ChunkAtmosphereManager.proxy.canBreathe(inside), "Raum atembar");
			check(ChunkAtmosphereManager.proxy.getAtmosphere(level, outside) == null, "Mun draussen = Vakuum");
			check(ChunkAtmosphereManager.proxy.hasAtmosphere(level, corner) && !ChunkAtmosphereManager.proxy.hasAtmosphere(level, outside), "hasAtmosphere");

			// HBM-API: Druck und Verbrennung (TileEntityMachineBase.breatheAir)
			check(Math.abs(SpaceApi.env().pressure(level, corner) - 1.0) < 1e-6, "Druck innen 1 atm, ist " + SpaceApi.env().pressure(level, corner));
			check(SpaceApi.env().pressure(level, outside) == 0.0, "Druck draussen 0");
			check(SpaceApi.env().consumeOxygen(level, corner, 5), "Ofen brennt im Raum");
			if(realMun(level)) check(provider.consumed == 5, "5 mB aus dem Raum verbraucht, ist " + provider.consumed); // Oberwelt: Koerperluft zuerst
			if(realMun(level)) check(!SpaceApi.env().consumeOxygen(level, outside, 5), "Ofen erlischt im Vakuum");

			// Wand ist kein Teil des Raums
			check(!blob(provider).contains(center.offset(2, 0, 0)), "Wand nicht im Blob");

			ChunkAtmosphereManager.proxy.unregisterAtmosphere(provider);
			clearRoom(level, center);
		});
	}

	/** Leck: Raum zerfaellt mit Druckverlust (EntityDepress), Wiederverschliessen baut ihn neu auf. */
	@GameTest(template = "empty3x3x3", batch = "space_atmosphere", timeoutTicks = 300)
	public static void leakDepressurizesAndReseals(GameTestHelper helper) {
		ServerLevel level = mun(helper);
		BlockPos center = new BlockPos(72, 200, 8);
		BlockPos wall = center.offset(2, 0, 0);
		buildRoom(level, center);
		TestProvider provider = new TestProvider(level, center, CelestialFluids.EARTHAIR, 1.0);
		ChunkAtmosphereManager.proxy.registerAtmosphere(provider);

		helper.startSequence()
			.thenWaitUntil(() -> waitFormed(provider, 27))
			.thenExecute(() -> {
				// Original: BreakEvent -> onBlockRemoved (Block ist zu dem Zeitpunkt noch da)
				ChunkAtmosphereManager.proxy.receiveBlockBroken(level, wall);
				level.setBlock(wall, Blocks.AIR.defaultBlockState(), 3);
			})
			.thenWaitUntil(() -> {
				waitFormed(provider, 0);
				List<EntityDepress> depress = level.getEntitiesOfClass(EntityDepress.class, new AABB(wall).inflate(3));
				check(!depress.isEmpty(), "EntityDepress am Leck");
				check(ChunkAtmosphereManager.proxy.getAtmosphere(level, center) == null, "Raum ist Vakuum");
			})
			.thenExecute(() -> {
				level.setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
				ChunkAtmosphereManager.proxy.receiveBlockPlaced(level, wall, Blocks.STONE.defaultBlockState());
			})
			.thenWaitUntil(() -> {
				waitFormed(provider, 27);
				check(ChunkAtmosphereManager.proxy.canBreathe(ChunkAtmosphereManager.proxy.getAtmosphere(level, center)), "wieder atembar");
			})
			.thenExecute(() -> {
				ChunkAtmosphereManager.proxy.unregisterAtmosphere(provider);
				for(EntityDepress e : level.getEntitiesOfClass(EntityDepress.class, new AABB(center).inflate(8))) e.discard();
				clearRoom(level, center);
			})
			.thenSucceed();
	}

	/** Abdichtungsregeln (Original isBlockSealed). */
	@GameTest(template = "empty3x3x3", batch = "space_atmosphere", timeoutTicks = 40)
	public static void sealingRules(GameTestHelper helper) {
		ServerLevel level = mun(helper);
		BlockPos p = new BlockPos(136, 200, 8);
		level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, true);
		level.getChunk(p);

		record Case(BlockState state, boolean sealed, String name) {}
		List<Case> cases = List.of(
			new Case(Blocks.AIR.defaultBlockState(), false, "Luft"),
			new Case(Blocks.STONE.defaultBlockState(), true, "Stein"),
			new Case(Blocks.GLASS.defaultBlockState(), true, "Glas"),
			new Case(Blocks.OAK_SLAB.defaultBlockState(), false, "Stufe unten"),
			new Case(Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), true, "Doppelstufe"),
			new Case(Blocks.OAK_FENCE.defaultBlockState(), false, "Zaun"),
			new Case(Blocks.FARMLAND.defaultBlockState(), false, "Ackerboden"),
			new Case(Blocks.OAK_LEAVES.defaultBlockState(), false, "Laub"),
			new Case(Blocks.TORCH.defaultBlockState(), false, "Fackel"),
			new Case(Blocks.WATER.defaultBlockState(), false, "Wasser"),
			new Case(Blocks.OAK_DOOR.defaultBlockState(), false, "Vanilla-Tuer"),
			new Case(com.hbm_m.block.ModBlocks.FURNACE_BRICK.get().defaultBlockState(), false, "HBM-Ziegelofen (unsealable)"));

		for(Case c : cases) {
			level.setBlock(p, c.state, 2);
			check(AtmosphereBlob.isBlockSealed(level, p) == c.sealed, c.name + ": dicht=" + !c.sealed);
		}
		level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
		check(AtmosphereBlob.isBlockSealed(level, new BlockPos(p.getX(), level.getMaxBuildHeight() + 1, p.getZ())) == false, "ueber der Welt offen");
		level.setChunkForced(p.getX() >> 4, p.getZ() >> 4, false);
		helper.succeed();
	}

	/** Ersticken: 100 Ticks Vorrat, dann oxyprime-Schaden; im Raum atmen; Immunitaet per Tag. */
	@GameTest(template = "empty3x3x3", batch = "space_atmosphere", timeoutTicks = 200)
	public static void suffocation(GameTestHelper helper) {
		ServerLevel level = mun(helper);
		BlockPos center = new BlockPos(200, 200, 8);
		buildRoom(level, center);
		TestProvider provider = new TestProvider(level, center, CelestialFluids.EARTHAIR, 1.0);
		ChunkAtmosphereManager.proxy.registerAtmosphere(provider);

		helper.succeedWhen(() -> {
			waitFormed(provider, 27);

			// Vakuum: Vorrat sinkt, ab <= 0 Schaden alle 8 Ticks (clamp(oxy/60+8,1,8)), 1 Schaden
			Pig outside = EntityType.PIG.create(level);
			outside.setPos(center.getX() + 0.5, center.getY() + 6, center.getZ() + 0.5);
			outside.tickCount = 104; // 104 % 8 == 0 -> Schadenstick
			float health = outside.getHealth();
			CBT_Atmosphere vacuum = ChunkAtmosphereManager.proxy.getAtmosphere(outside);
			check(vacuum == null, "Schwein draussen im Vakuum");
			for(int i = 0; i < 100; i++) AtmosphereEffects.handleOxy(outside, vacuum);
			check(AtmosphereEffects.getOxy(outside) == 0, "Vorrat nach 100 Ticks 0, ist " + AtmosphereEffects.getOxy(outside));
			check(outside.getHealth() < health, "oxyprime-Schaden bei Vorrat 0");
			check(outside.getLastDamageSource() != null && outside.getLastDamageSource().is(AtmosphereEffects.OXYPRIME), "Schadensart oxyprime");

			// Im Raum: Vorrat bleibt voll
			Pig inside = EntityType.PIG.create(level);
			inside.setPos(center.getX() + 0.5, center.getY() - 1, center.getZ() + 0.5);
			inside.tickCount = 100;
			CBT_Atmosphere air = ChunkAtmosphereManager.proxy.getAtmosphere(inside);
			check(air != null, "Schwein im Raum hat Luft (Augenhoehe)");
			for(int i = 0; i < 120; i++) AtmosphereEffects.handleOxy(inside, air);
			check(AtmosphereEffects.getOxy(inside) == AtmosphereEffects.MAX_OXYGEN, "Vorrat im Raum voll");
			check(AtmosphereEffects.getAtmosphereCached(inside) != null && AtmosphereEffects.hasGravity(inside), "Atmosphaere gecacht, Schwerkraft-Flag");

			// Immunitaet: HBM-Glyphide per Tag space_m:suffocation_immune
			EntityType<?> glyphid = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "glyphid"));
			check(glyphid.is(com.space_m.lib.SpaceTags.SUFFOCATION_IMMUNE), "Glyphide im Tag suffocation_immune");
			check(!AtmosphereEffects.isSuffocationImmune(outside), "Schwein nicht immun");

			ChunkAtmosphereManager.proxy.unregisterAtmosphere(provider);
			clearRoom(level, center);
		});
	}

	/** Welt-Effekte: Fackel/Wasser/Setzling verschwinden ohne Luft, bleiben im Druckraum. */
	@GameTest(template = "empty3x3x3", batch = "space_atmosphere", timeoutTicks = 200)
	public static void blockEffectsNeedAir(GameTestHelper helper) {
		ServerLevel level = mun(helper);
		BlockPos center = new BlockPos(264, 200, 8);
		buildRoom(level, center);
		TestProvider provider = new TestProvider(level, center, CelestialFluids.EARTHAIR, 1.0);
		ChunkAtmosphereManager.proxy.registerAtmosphere(provider);

		helper.succeedWhen(() -> {
			waitFormed(provider, 27);
			BlockPos out = center.offset(0, 4, 0); // auf dem Dach
			level.setBlock(out.below(), Blocks.DIRT.defaultBlockState(), 2);

			for(BlockState state : List.of(Blocks.TORCH.defaultBlockState(), Blocks.WATER.defaultBlockState(), Blocks.OAK_SAPLING.defaultBlockState())) {
				level.setBlock(out, state, 2);
				check(ChunkAtmosphereManager.proxy.runEffectsOnBlock(level, state, out), state + " ohne Luft entfernt");
				check(level.getBlockState(out).isAir(), state + " ist weg");
			}
			BlockState redstone = Blocks.REDSTONE_TORCH.defaultBlockState();
			level.setBlock(out, redstone, 2);
			check(!ChunkAtmosphereManager.proxy.runEffectsOnBlock(level, redstone, out), "Redstonefackel braucht keine Luft");

			BlockPos in = center.offset(0, -1, 0);
			level.setBlock(in, Blocks.TORCH.defaultBlockState(), 2);
			check(!ChunkAtmosphereManager.proxy.runEffectsOnBlock(level, Blocks.TORCH.defaultBlockState(), in), "Fackel im Raum bleibt");

			if(realMun(level)) check(ChunkAtmosphereManager.isLowPressureWorld(level), "Mun: drucklos (Eimer verdampft)");
			check(!ChunkAtmosphereManager.isLowPressureWorld(helper.getLevel()), "Oberwelt: Wasser normal");

			level.setBlock(out, Blocks.AIR.defaultBlockState(), 2);
			level.setBlock(out.below(), Blocks.AIR.defaultBlockState(), 2);
			ChunkAtmosphereManager.proxy.unregisterAtmosphere(provider);
			clearRoom(level, center);
		});
	}

	/** Oberwelt (Kerbin): Luft ueberall, Verbrennung ohne Raum. */
	@GameTest(template = "empty3x3x3", batch = "space_atmosphere", timeoutTicks = 20)
	public static void overworldBreathable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
		CBT_Atmosphere atmosphere = ChunkAtmosphereManager.proxy.getAtmosphere(level, pos);
		check(ChunkAtmosphereManager.proxy.canBreathe(atmosphere), "Oberwelt atembar");
		check(SpaceApi.env().consumeOxygen(level, pos, 1), "Verbrennung auf der Oberwelt");
		Pig pig = EntityType.PIG.create(level);
		pig.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		AtmosphereEffects.handleOxy(pig, atmosphere);
		check(AtmosphereEffects.getOxy(pig) == AtmosphereEffects.MAX_OXYGEN, "kein Ersticken auf der Oberwelt");
		helper.succeed();
	}
}
