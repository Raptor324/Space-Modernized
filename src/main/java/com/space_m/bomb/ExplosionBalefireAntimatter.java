package com.space_m.bomb;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Antimaterie-Zweig von NTM Space {@code ExplosionBalefire(x, y, z, world, rad, antimatter=true)} (der Space-Zweig hat die
 * Klasse um {@code antimatter} erweitert; HBM-Modernized kennt nur die normale Balefire-Spirale, daher eigene Klasse).
 *
 * <p>Spirale wie im Original ({@code n}, {@code shell}, {@code leg}, {@code element}); je Saeule:
 * <ul>
 *   <li>Abwaerts von der Hoehenkarte bis {@code depth}: Schnee/Eis ({@code Material.snow/craftedSnow/ice/packedIce})
 *       erzeugt eine Gasflamme. Der Space-Zweig entfernt in dieser Schleife <b>keine</b> Bloecke (das
 *       {@code setBlockToAir} des Grundzweigs fehlt dort) - 1:1 uebernommen.</li>
 *   <li>Fuenf Bloecke ab {@code depth}: mit Wahrscheinlichkeit 1/{@code dist} vulkanische Lava auf {@code depth},
 *       Stein -> {@code sellafield_slaked}.</li>
 * </ul>
 * Abweichung (technisch noetig): im Original fehlt nach der Gasflamme das {@code y--} ({@code continue}) - trifft die Spirale
 * Schnee oder Eis, haengt der Server in einer Endlosschleife. Hier wird nach der Flamme weitergezaehlt.
 * Original-Klasse: ExplosionBalefire
 */
public class ExplosionBalefireAntimatter {

    public int posX;
    public int posY;
    public int posZ;
    public int lastposX = 0;
    public int lastposZ = 0;
    public int radius;
    public int radius2;
    public final Level worldObj;
    private int n = 1;
    private int nlimit;
    private int shell;
    private int leg;
    private int element;

    public ExplosionBalefireAntimatter(int x, int y, int z, Level world, int rad) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.worldObj = world;
        this.radius = rad;
        this.radius2 = this.radius * this.radius;
        this.nlimit = this.radius2 * 4;
    }

    public boolean update() {
        if (n == 0) return true;

        breakColumn(this.lastposX, this.lastposZ);
        this.shell = (int) Math.floor((Math.sqrt(n) + 1) / 2);
        int shell2 = this.shell * 2;

        if (shell2 == 0) return true;

        this.leg = (int) Math.floor((this.n - (shell2 - 1) * (shell2 - 1)) / shell2);
        this.element = (this.n - (shell2 - 1) * (shell2 - 1)) - shell2 * this.leg - this.shell + 1;
        this.lastposX = this.leg == 0 ? this.shell : this.leg == 1 ? -this.element : this.leg == 2 ? -this.shell : this.element;
        this.lastposZ = this.leg == 0 ? this.element : this.leg == 1 ? this.shell : this.leg == 2 ? -this.element : -this.shell;
        this.n++;

        return this.n > this.nlimit;
    }

    private static boolean isSnowOrIce(BlockState state) {
        return state.is(BlockTags.SNOW) || state.is(BlockTags.ICE) || state.is(Blocks.SNOW) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW);
    }

    private void breakColumn(int x, int z) {
        int dist = (int) (radius - Math.sqrt(x * x + z * z));

        if (dist > 0) {
            int pX = posX + x;
            int pZ = posZ + z;

            int y = worldObj.getHeight(Heightmap.Types.MOTION_BLOCKING, pX, pZ);
            int maxdepth = (int) (10 + radius * 0.25);
            int depth = (int) ((maxdepth * dist / radius) + (Math.sin(dist * 0.15 + 2) * 2));

            depth = Math.max(y - depth, worldObj.getMinBuildHeight());

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            while (y > depth) {
                // (Schrabidium-Cluster nur ohne Antimaterie)
                if (isSnowOrIce(worldObj.getBlockState(pos.set(pX, y, pZ)))) {
                    ParticleUtil.spawnGasFlame(worldObj, pX, y, pZ, x, y, z);
                }
                y--;
            }

            for (int i = depth; i > depth - 5; i--) {
                if (worldObj.random.nextInt(dist) == 0) {
                    worldObj.setBlock(pos.set(pX, depth, pZ), ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState(), 3);
                }

                if (worldObj.getBlockState(pos.set(pX, i, pZ)).is(Blocks.STONE))
                    worldObj.setBlock(pos, ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState(), 3);
            }
        }
    }

    public void saveToNbt(CompoundTag nbt, String name) {
        nbt.putInt(name + "posX", posX);
        nbt.putInt(name + "posY", posY);
        nbt.putInt(name + "posZ", posZ);
        nbt.putInt(name + "lastposX", lastposX);
        nbt.putInt(name + "lastposZ", lastposZ);
        nbt.putInt(name + "radius", radius);
        nbt.putInt(name + "radius2", radius2);
        nbt.putInt(name + "n", n);
        nbt.putInt(name + "nlimit", nlimit);
        nbt.putInt(name + "shell", shell);
        nbt.putInt(name + "leg", leg);
        nbt.putInt(name + "element", element);
        nbt.putBoolean(name + "antimatter", true);
    }

    public void readFromNbt(CompoundTag nbt, String name) {
        posX = nbt.getInt(name + "posX");
        posY = nbt.getInt(name + "posY");
        posZ = nbt.getInt(name + "posZ");
        lastposX = nbt.getInt(name + "lastposX");
        lastposZ = nbt.getInt(name + "lastposZ");
        radius = nbt.getInt(name + "radius");
        radius2 = nbt.getInt(name + "radius2");
        n = Math.max(nbt.getInt(name + "n"), 1); // prevents invalid read operation
        nlimit = nbt.getInt(name + "nlimit");
        shell = nbt.getInt(name + "shell");
        leg = nbt.getInt(name + "leg");
        element = nbt.getInt(name + "element");
    }
}
