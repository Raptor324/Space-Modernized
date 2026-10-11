package com.space_m.atmosphere;

import java.util.List;
import java.util.function.Consumer;

import com.space_m.entity.SpaceEntities;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 NTM Space {@code EntityDepress}: Druckverlust an einem Leck - weisser Partikelstrom ({@code effectNT "depress"})
 * und Sog/Druck auf alle Entities im Umkreis von 8 Bloecken. Spieler in einem anderen Druckraum (= mit
 * "Schwerkraft", {@link AtmosphereEffects#hasGravity}) bleiben unberuehrt. Mapping-Name {@code entity_depress},
 * Darstellung leer (Original {@code RenderEmpty}).
 */
public class EntityDepress extends Entity {

	private static final EntityDataAccessor<Byte> DIR = SynchedEntityData.defineId(EntityDepress.class, EntityDataSerializers.BYTE);

	/** Client-Partikel (vom Client-Teil gesetzt, Original {@code MainRegistry.proxy.effectNT}). */
	public static Consumer<EntityDepress> clientEffect = e -> {};

	public int timeToLive;

	public static double range = 8; // push/pull range

	public EntityDepress(EntityType<? extends EntityDepress> type, Level world) {
		super(type, world);
		this.noCulling = true;
		this.noPhysics = true;
		this.timeToLive = 100;
	}

	public EntityDepress(Level world, Direction dir, int timeToLive) {
		this(SpaceEntities.DEPRESS.get(), world);

		this.timeToLive = timeToLive;
		setDir(dir);
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	protected void setDir(Direction dir) {
		this.entityData.set(DIR, (byte) dir.ordinal());
	}

	public Direction getDir() {
		return Direction.values()[Math.floorMod(this.entityData.get(DIR), 6)];
	}

	//? if < 1.21.1 {
	@Override
	protected void defineSynchedData() {
		this.entityData.define(DIR, (byte) 0);
	}
	//?} else {
	/*@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DIR, (byte) 0);
	}
	*///?}

	@Override
	public void tick() {
		super.tick();

		Direction dir = getDir();

		if(level().isClientSide) clientEffect.accept(this);

		List<Entity> entities = level().getEntities(this, new AABB(
			getX() - range, getY() - range, getZ() - range, getX() + range, getY() + range, getZ() + range));

		for(Entity entity : entities) {
			Vec3 vec = new Vec3(getX() - entity.getX(), getY() - entity.getY(), getZ() - entity.getZ());

			// check if the player is in a different pressurized room
			// other living entities update gravity too infrequently to use this check, so it is for players only
			if(entity instanceof Player player) {
				if(AtmosphereEffects.hasGravity(player)) continue;
			}

			double dist = vec.length();

			if(dist > range)
				continue;

			boolean succ = true;

			switch(dir) {
			case UP: succ = entity.getY() < getY(); break;
			case DOWN: succ = entity.getY() > getY(); break;
			case EAST: succ = entity.getX() < getX(); break;
			case WEST: succ = entity.getX() > getX(); break;
			case SOUTH: succ = entity.getZ() < getZ(); break;
			case NORTH: succ = entity.getZ() > getZ(); break;
			default: break;
			}

			vec = vec.normalize();

			double speed = succ ? 0.1D : -0.1D;
			entity.setDeltaMovement(entity.getDeltaMovement().add(vec.x * speed, vec.y * speed, vec.z * speed));
		}

		timeToLive--;

		if(timeToLive <= 0 && !level().isClientSide) {
			discard();
		}
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag nbt) {
		timeToLive = nbt.getInt("ttl");
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag nbt) {
		nbt.putInt("ttl", timeToLive);
	}

}
