package com.space_m.orbit.propulsion.client;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MachineLpw2BlockEntity;
import com.hbm_m.interfaces.IMultiblockPart;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.celestial.CelestialBody;
import com.space_m.orbit.propulsion.Lpw2Thruster;
import com.space_m.orbit.propulsion.PropulsionUtil;

import dev.architectury.event.events.client.ClientGuiEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Blick-Overlay des LPW-2 im Orbit, 1:1 NTM Space {@code MachineLPW2.printHook}: steht das Triebwerk nicht prograd,
 * blinkt {@code "! ! ! WRONG DIRECTION ! ! !"} (rot/gelb), sonst je Tank {@code "-> <Fluid>: <fill>/<max>mB"}; Titel
 * gelb mit dunklem Schatten. HBMs {@code machine_lpw2} ist kein {@code ILookOverlay} (und HBMs Teilblock reicht nur an
 * solche weiter), deshalb zeichnet das Addon das Overlay ueber ein eigenes HUD-Ereignis - mit derselben Schaltung wie
 * HBMs {@code BlockLookOverlayHud} ({@code ClientConfig.doddRbmkDiagnostic}).
 * Original-Klasse: MachineLPW2 (printHook)
 */
public final class Lpw2Overlay {

    private Lpw2Overlay() {}

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register((graphics, tickDelta) -> render(graphics));
    }

    private static void render(GuiGraphics g) {
        if (!com.hbm_m.config.ClientConfig.doddRbmkDiagnostic) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        HitResult hr = mc.hitResult;
        if (hr == null || hr.getType() != HitResult.Type.BLOCK) return;

        Level world = mc.level;
        if (!CelestialBody.inOrbit(world)) return;

        BlockPos pos = ((BlockHitResult) hr).getBlockPos();
        BlockPos core = null;
        if (world.getBlockState(pos).is(ModBlocks.LPW2.get())) {
            core = pos;
        } else if (world.getBlockEntity(pos) instanceof IMultiblockPart part && part.getControllerPos() != null
                && world.getBlockState(part.getControllerPos()).is(ModBlocks.LPW2.get())) {
            core = part.getControllerPos();
        }
        if (core == null) return;

        BlockEntity te = world.getBlockEntity(core);
        if (!(te instanceof MachineLpw2BlockEntity thruster)) return;

        List<Component> text = new ArrayList<>();

        if (!Lpw2Thruster.isFacingPrograde(thruster)) {
            text.add(PropulsionUtil.blinkWarning("atmosphere.engineFacing"));
        } else {
            for (FluidTank tank : thruster.tanks) {
                text.add(PropulsionUtil.tankLine(tank, true));
            }
        }

        // Original I18nUtil.resolveKey("tile.machine_lpw2.name") = "LPW-2 Station Thruster" (HBM-Name "LPW2": W-R9B4-2)
        print(g, Component.translatable(Lpw2Thruster.NAME_KEY), 0xffff00, 0x404000, text);
    }

    /** {@code ILookOverlay.printGeneric}, Zeilen als Komponenten (gruener Pfeil, weisser Rest wie {@code RESET}). */
    private static void print(GuiGraphics g, Component title, int titleCol, int bgCol, List<Component> text) {
        Minecraft mc = Minecraft.getInstance();
        Options options = mc.options;
        if (!options.getCameraType().isFirstPerson()) return;
        if (options.hideGui) return;
        if (mc.gameMode != null && mc.gameMode.getPlayerMode() == GameType.SPECTATOR) return;

        int pX = mc.getWindow().getGuiScaledWidth() / 2 + 8;
        int pZ = mc.getWindow().getGuiScaledHeight() / 2;
        Font font = mc.font;

        g.drawString(font, title.getString(), pX + 1, pZ - 9, bgCol, false);
        g.drawString(font, title.getString(), pX, pZ - 10, titleCol, false);

        for (Component line : text) {
            g.drawString(font, line, pX, pZ, 0xFFFFFF, true);
            pZ += 10;
        }
    }
}
