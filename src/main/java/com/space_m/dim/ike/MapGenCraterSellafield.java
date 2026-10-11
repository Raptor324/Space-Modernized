package com.space_m.dim.ike;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.MapGenBase;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.world.gen.terrain.MapGenCrater} (die Oberwelt-Fassung, nicht {@code com.hbm.dim.mapgen.MapGenCrater}):
 * Strahlungskrater mit Wahrscheinlichkeit 1/{@code frequency} je Start-Chunk, Radius 8-64, Profil
 * {@code -x^2/r^2 * d + d} (Tiefe 0.35 * r), Suche ab y 127 abwaerts, Boden {@link #regolith} (Mitte) bzw.
 * {@link #rock}. {@code range} bleibt der MapGenBase-Standard 8 ({@code setSize} wird fuer Ike nicht aufgerufen).
 *
 * <p>Auf Ike ({@code ChunkProviderIke.sellafield}): {@code new MapGenCrater(WorldConfig.radfreq / 10)},
 * Mitte {@code sellafield} (Metadatum 0), Rand {@code sellafield_slaked}. HBM-Modernized hat die Klasse nur als
 * Oberwelt-Carver ({@code com.hbm_m.world.gen.terrain.MapGenCrater}, Wueste, fest {@code radfreq}); im Planeten-Puffer
 * laeuft deshalb diese Fassung.
 */
public class MapGenCraterSellafield extends MapGenBase {

	private final int frequency;
	private int minSize = 8;
	private int maxSize = 64;

	public BlockState regolith;
	public BlockState rock;

	/** {@code targetBiome}: null = ueberall. */
	public BiomeGenBaseCelestial targetBiome;

	public MapGenCraterSellafield(int frequency) {
		this.frequency = frequency;
	}

	public void setSize(int minSize, int maxSize) {
		this.minSize = minSize;
		this.maxSize = maxSize;

		this.range = (maxSize / 8) + 1;
	}

	private double depthFunc(double x, double rad, double depth) {
		return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
	}

	// This function is looped over from -this.range to +this.range on both XZ axes.
	@Override
	protected void func_151538_a(CelestialWorld world, int offsetX, int offsetZ, int chunkX, int chunkZ, BlockState[] blocks) {
		// 1.7.10: rand.nextInt(0) wuerfe eine Ausnahme (radfreq < 10) - dann gibt es keine Krater
		if (frequency <= 0) return;

		if (rand.nextInt(frequency) == 0 && (targetBiome == null || targetBiome == world.getBiomeGenForCoords(offsetX * 16, offsetZ * 16))) {
			int xCoord = -offsetX + chunkX;
			int zCoord = -offsetZ + chunkZ;

			double radius = rand.nextInt(maxSize - minSize) + minSize;
			double depth = radius * 0.35D;

			for (int bx = 15; bx >= 0; bx--) { // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
				for (int bz = 15; bz >= 0; bz--) {
					for (int y = 127; y >= 0; y--) {
						int index = (bx * 16 + bz) * 256 + y;

						if (blocks[index] != null && (Legacy.isOpaqueCube(blocks[index]) || Legacy.isLiquid(blocks[index]))) {
							// x, z are the coordinates relative to the target virtual chunk origin
							int x = xCoord * 16 + bx;
							int z = zCoord * 16 + bz;

							// y is at the current height now
							double r = Math.sqrt(x * x + z * z);

							if (r - rand.nextInt(3) <= radius) {
								// Carve out to intended depth
								int dep = (int) Legacy.clamp(depthFunc(r, radius, depth), 0, y - 1);
								for (int i = 0; i < dep; i++) {
									blocks[index - i] = null;
								}

								index -= dep;
								y -= dep;

								dep = Math.min(3, y - 1);

								// Fill back in
								if (r + rand.nextInt(3) <= radius / 3D) {
									for (int i = 0; i < dep; i++) {
										blocks[index - i] = regolith;
									}
								} else {
									for (int i = 0; i < dep; i++) {
										blocks[index - i] = rock;
									}
								}
							}

							break;
						}
					}
				}
			}
		}
	}

}
