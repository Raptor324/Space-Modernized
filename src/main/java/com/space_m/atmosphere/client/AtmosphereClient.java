package com.space_m.atmosphere.client;

import java.awt.Color;
import java.util.Random;

import com.hbm_m.particle.nt.ParticleEngineNT;
import com.hbm_m.particle.nt.ParticleRocketFlameNT;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.space_m.atmosphere.AtmosphereEffects;
import com.space_m.atmosphere.EntityDepress;
import com.space_m.atmosphere.IOxygenTank;
import com.space_m.entity.SpaceEntities;
import com.space_m.network.SpacePacketHandler;

import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
//? if forge {
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
//?} elif neoforge {
/*import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
*///?}

/**
 * Client-Teil der Atmosphaere (nur physischer Client, aus {@code SpaceClient.init()}):
 * <ul>
 *   <li>S2C {@code atmosphere_oxy} -> {@link AtmosphereEffects#clientOxygen}/{@code clientGravity}.</li>
 *   <li>Luftblasen-HUD, wenn man nicht wegen Wasser erstickt (Original {@code ModEventHandlerRenderer}, ElementType.AIR:
 *       Blasen aus dem eigenen Vorrat statt Vanilla-Luft, darunter der Fuellbalken des Sauerstofftanks).</li>
 *   <li>{@code EntityDepress}: leerer Renderer, Partikel {@code effectNT "depress"} = {@code ParticleRocketFlame} weiss,
 *       Skala 0.5, ohne Druckanpassung (Original {@code ClientProxy.effectNT}).</li>
 * </ul>
 */
public final class AtmosphereClient {

	private AtmosphereClient() {}

	private static boolean initialized;

	public static void init() {
		if(initialized) return;
		initialized = true;

		SpacePacketHandler.registerS2C(SpacePacketHandler.ATMOSPHERE_OXY, AtmosphereEffects.OxyPacket::decode,
				(pkt, ctx) -> ctx.queue(pkt::apply));

		EntityRendererRegistry.register(SpaceEntities.DEPRESS, NoopRenderer::new);
		EntityDepress.clientEffect = AtmosphereClient::depressParticle;

		//? if forge {
		MinecraftForge.EVENT_BUS.addListener(AtmosphereClient::onRenderOverlay);
		//?} elif neoforge {
		/*NeoForge.EVENT_BUS.addListener(AtmosphereClient::onRenderLayer);
		*///?}
	}

	// ─────────────────────────────── Depress-Partikel ───────────────────────────────

	private static void depressParticle(EntityDepress entity) {
		Minecraft mc = Minecraft.getInstance();
		if(!(entity.level() instanceof ClientLevel world) || mc.player == null) return;
		var rand = world.random;
		Direction dir = entity.getDir();

		double x = entity.getX() + rand.nextGaussian() * 0.25;
		double y = entity.getY() + rand.nextGaussian() * 0.25;
		double z = entity.getZ() + rand.nextGaussian() * 0.25;
		if(new Vec3(mc.player.getX() - x, mc.player.getY() - y, mc.player.getZ() - z).length() > 350) return;

		ParticleDepress fx = new ParticleDepress(world, x, y, z, 0xFFFFFF);
		fx.setScale(0.5F);
		fx.setMotion(dir.getStepX() * 0.5 + rand.nextGaussian() * 0.25, dir.getStepY() * 0.5 + rand.nextGaussian() * 0.25, dir.getStepZ() * 0.5 + rand.nextGaussian() * 0.25);
		fx.setMaxAge(100 + rand.nextInt(20));
		ParticleEngineNT.INSTANCE.add(fx);
	}

	/** Original {@code ParticleRocketFlame.setCustomColor}: HBMs Port kennt keine Eigenfarbe -> eigene Darstellung. */
	private static final class ParticleDepress extends ParticleRocketFlameNT {
		private final float customRed, customGreen, customBlue;

		ParticleDepress(ClientLevel level, double x, double y, double z, int color) {
			super(level, x, y, z);
			Color c = new Color(color);
			this.customRed = c.getRed() / 255F;
			this.customGreen = c.getGreen() / 255F;
			this.customBlue = c.getBlue() / 255F;
		}

		void setMotion(double mx, double my, double mz) {
			this.xd = mx;
			this.yd = my;
			this.zd = mz;
		}

		@Override
		public void render(VertexConsumer ignored, Camera camera, float pt, PoseStack pose) {
			if(age == 0) return;
			VertexConsumer c = buffer();
			Random urandom = new Random(this.seed);
			Vec3 p = camPos(camera, pt);
			Vector3f l = left(camera);
			Vector3f u = up(camera);

			for(int i = 0; i < 10; i++) {
				float add = urandom.nextFloat() * 0.3F;
				float dark = 1 - Math.min(((float) age / (float) (lifetime * 0.25F)), 1);
				float r = (customRed != 0) ? (customRed * dark + add) : (1 * dark + add);
				float g = (customGreen != 0) ? (customGreen * dark + add) : (0.6F * dark + add);
				float b = (customBlue != 0) ? (customBlue * dark + add) : (0 + add);
				float a = (float) Math.pow(1 - Math.min(((float) age / (float) lifetime), 1), 0.5);

				float spread = (float) Math.pow(((float) age / (float) lifetime) * 4F, 1.5) + 1F;
				spread *= this.particleScale;
				float scale = (urandom.nextFloat() * 0.5F + 0.1F + ((float) age / (float) lifetime) * 2F) * particleScale;
				float pX = (float) (p.x + (urandom.nextGaussian() - 1D) * 0.2F * spread);
				float pY = (float) (p.y + (urandom.nextGaussian() - 1D) * 0.5F * spread);
				float pZ = (float) (p.z + (urandom.nextGaussian() - 1D) * 0.2F * spread);
				quad(c, pX, pY, pZ, l, u, scale, r, g, b, a * 0.75F, 0xF000F0, 0, 0, 1, 1);
			}
		}
	}

	// ─────────────────────────────── HUD ───────────────────────────────

	//? if forge {
	private static final net.minecraft.resources.ResourceLocation GUI_ICONS = new net.minecraft.resources.ResourceLocation("textures/gui/icons.png");

	private static void onRenderOverlay(RenderGuiOverlayEvent.Pre event) {
		if(event.getOverlay() != VanillaGuiOverlay.AIR_LEVEL.type()) return;
		Minecraft mc = Minecraft.getInstance();
		if(!(mc.gui instanceof ForgeGui gui) || mc.player == null || mc.options.hideGui) return;
		if(!gui.shouldDrawSurvivalElements()) return;
		int[] rightHeight = { gui.rightHeight };
		if(renderAir(event.getGuiGraphics(), mc.player, event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(), rightHeight)) {
			gui.rightHeight = rightHeight[0];
			event.setCanceled(true);
		}
	}

	private static void drawBubble(GuiGraphics g, int x, int y, boolean full) {
		g.blit(GUI_ICONS, x, y, full ? 16 : 25, 18, 9, 9);
	}
	//?} elif neoforge {
	/*private static final net.minecraft.resources.ResourceLocation AIR = net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/air");
	private static final net.minecraft.resources.ResourceLocation AIR_BURSTING = net.minecraft.resources.ResourceLocation.withDefaultNamespace("hud/air_bursting");

	private static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
		if(!VanillaGuiLayers.AIR_LEVEL.equals(event.getName())) return;
		Minecraft mc = Minecraft.getInstance();
		if(mc.player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
		int[] rightHeight = { mc.gui.rightHeight };
		if(renderAir(event.getGuiGraphics(), mc.player, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(), rightHeight)) {
			mc.gui.rightHeight = rightHeight[0];
			event.setCanceled(true);
		}
	}

	private static void drawBubble(GuiGraphics g, int x, int y, boolean full) {
		g.blitSprite(full ? AIR : AIR_BURSTING, x, y, 9, 9);
	}
	*///?}

	/** Original {@code ModEventHandlerRenderer} (ElementType.AIR). true = Vanilla-Luftanzeige unterdruecken. */
	private static boolean renderAir(GuiGraphics g, Player player, int width, int height, int[] rightHeight) {
		boolean cancel = false;

		// If we're suffocating for a reason other than water, render the HUD bubbles
		int air = AtmosphereEffects.clientOxygen;
		if(air < AtmosphereEffects.MAX_OXYGEN) {
			int left = width / 2 + 91;
			int top = height - rightHeight[0];

			int full = Mth.ceil((double) (air - 2) * 10.0D / 100.0D);
			int partial = Mth.ceil((double) air * 10.0D / 100.0D) - full;

			for(int i = 0; i < full + partial; ++i) {
				drawBubble(g, left - i * 8 - 9, top, i < full);
			}
			rightHeight[0] += 10;

			// Prevent regular bubbles rendering
			cancel = true;
		}

		ItemStack tankStack = AtmosphereEffects.getOxygenTank(player);
		if(!tankStack.isEmpty()) {
			IOxygenTank tank = (IOxygenTank) tankStack.getItem();

			float tot = (float) tank.getFuel(tankStack) / (float) tank.getMaxFuel();

			int right = width / 2 + 91;
			int top = height - rightHeight[0] + 3;
			// Original: Tessellator-Rechteck 82x5 (dunkelgrau) + Fuellbalken (Farbe 1-tot, tot, tot)
			g.fill(right - 82, top - 1, right + 1, top + 5, 0xFF404040);
			int color = 0xFF000000 | ((int) ((1F - tot) * 255) << 16) | ((int) (tot * 255) << 8) | (int) (tot * 255);
			g.fill(right - (int) (81 * tot), top, right, top + 4, color);

			rightHeight[0] += 6;
			cancel = true;
		}

		return cancel;
	}
}
