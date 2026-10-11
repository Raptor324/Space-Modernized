package com.space_m.dim.tekto;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.tekto.TTree}: gerader Stamm ({@code rand(smallest) + tallest} hoch) auf Tekto-Gras/-Schlick
 * mit ellipsoider Blattkugel ({@code xz}/{@code y} Radien, {@code offset} unter der Spitze), optional Vinyl-Ranken
 * (1/12 je Blatt und Seite, bis 14 lang). Ranken-Metadaten wie im Original (1 = Sued, 2 = West, 4 = Nord,
 * 8 = Ost; das Original setzt fuer "westlich vom Blatt" 1 und fuer "noerdlich" 3).
 */
public class TTree {

	int offset;
	int smallest;
	int tallest;
	int xz;
	int y;
	boolean vines;
	Block logBlock;
	Block leavBlock;

	public TTree(boolean notify, int offset, int smallest, int tallest, int xz, int y, boolean vines, Block log, Block leaf) {
		this.offset = offset;
		this.smallest = smallest;
		this.tallest = tallest;
		this.xz = xz;
		this.y = y;
		this.vines = vines;
		this.logBlock = log;
		this.leavBlock = leaf;
	}

	private static BlockState vine(Block vine, int meta) {
		BlockState s = vine.defaultBlockState();
		if (!(vine instanceof VineBlock)) return s;
		return s.setValue(VineBlock.SOUTH, (meta & 1) != 0).setValue(VineBlock.WEST, (meta & 2) != 0)
				.setValue(VineBlock.NORTH, (meta & 4) != 0).setValue(VineBlock.EAST, (meta & 8) != 0);
	}

	/**
	 * Blattzustand: Original-Metadatum 0 (zerfaellt erst nach einer Nachbaraenderung). In 1.20 zerfallen Blaetter
	 * mit Abstand 7 sofort - Abstand daher aus dem naechsten Stammblock (Manhattan), hoechstens 6.
	 */
	private static BlockState leaves(Block leaf, int dx, int ly, int dz, int y, int height) {
		BlockState s = leaf.defaultBlockState();
		if (!s.hasProperty(net.minecraft.world.level.block.LeavesBlock.DISTANCE)) return s;
		int ty = Math.max(y, Math.min(y + height - 1, ly));
		int d = Math.abs(dx) + Math.abs(dz) + Math.abs(ly - ty);
		return s.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE, Math.max(1, Math.min(6, d)));
	}

	public boolean generate(WorldGenLevel world, Random rand, int x, int y, int z) {
		int height = rand.nextInt(smallest) + tallest;

		if (y < 1 || y + height + 1 > 256) {
			return false;
		}

		BlockState blockBelow = world.getBlockState(new BlockPos(x, y - 1, z));
		if (!blockBelow.is(TektoBlocks.RUBBER_GRASS.get()) && !blockBelow.is(TektoBlocks.RUBBER_SILT.get())) {
			return false;
		}

		for (int i = 0; i < height; i++) {
			world.setBlock(new BlockPos(x, y + i, z), logBlock.defaultBlockState(), 2);
		}

		int bulbStartY = y + height - offset;
		int bulbRadiusXz = xz;
		int bulbRadiusY = this.y;
		Block vineBlock = TektoBlocks.VINYL_VINES.get();

		for (int dy = -bulbRadiusY; dy <= bulbRadiusY; dy++) {
			for (int dx = -bulbRadiusXz; dx <= bulbRadiusXz; dx++) {
				for (int dz = -bulbRadiusXz; dz <= bulbRadiusXz; dz++) {

					if (Math.pow(dx / (double)bulbRadiusXz, 2) + Math.pow(dy / (double)bulbRadiusY, 2) + Math.pow(dz / (double)bulbRadiusXz, 2) <= 1) {
						BlockPos p = new BlockPos(x + dx, bulbStartY + dy, z + dz);
						if (world.getBlockState(p).isAir()) {
							world.setBlock(p, leaves(leavBlock, dx, bulbStartY + dy, dz, y, height), 2);
							if (vines) {
								for (int i = 0; i < 4; i++) {
									int vineX = x + dx + (i % 2 == 0 ? (i - 1) : 0);
									int vineZ = z + dz + (i % 2 == 1 ? (i - 2) : 0);

									if (rand.nextInt(12) == 0) {
										for (int j = 0; j < rand.nextInt(15); j++) {
											int vineY = bulbStartY + dy - j;
											BlockPos vp = new BlockPos(vineX, vineY, vineZ);
											if (world.getBlockState(vp).isAir()) {
												int meta = 0;
												if (vineX > x + dx) {
													meta = 0x2;
												} else if (vineX < x + dx) {
													meta = 0x1;
												} else if (vineZ > z + dz) {
													meta = 0x4;
												} else if (vineZ < z + dz) {
													meta = 0x3;
												}
												world.setBlock(vp, vine(vineBlock, meta), 2);
											} else {
												break;
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		return true;
	}

}
