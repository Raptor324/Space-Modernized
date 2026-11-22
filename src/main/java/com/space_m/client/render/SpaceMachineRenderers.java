package com.space_m.client.render;

import java.util.Random;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.hbm_m.client.render.CardinalOrientation;
import com.hbm_m.client.render.LegacyAnimator;
import com.hbm_m.client.render.UvShiftConsumer;
import com.hbm_m.client.render.implementations.MinerRocketRenderer;
import com.hbm_m.client.render.implementations.ObjBerHelper;
import com.hbm_m.client.render.machine.MachineRenderApi;
import com.hbm_m.client.render.machine.MachineRenderers;
import com.hbm_m.client.render.machine.MachineSpec;
import com.hbm_m.interfaces.IEnergyConnector;
import com.hbm_m.platform.RenderHooks;
import com.hbm_m.util.BobMathUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.space_m.blockentity.SpaceBlockEntities;
import com.space_m.lifesupport.TileEntityAirScrubber;
import com.space_m.machine.atmo.AtmosphericCompressorBlockEntity;
import com.space_m.machine.atmo.AtmosphericEmitterBlockEntity;
import com.space_m.machine.chem.ChemMachines;
import com.space_m.machine.chem.TileEntityHydroponic;
import com.space_m.machine.chem.TileEntityMachineAlkylation;
import com.space_m.machine.chem.TileEntityMachineCryoDistill;
import com.space_m.machine.chem.TileEntityMachineMilkReformer;
import com.space_m.machine.chem.TileEntityMachineVacuumCircuit;
import com.space_m.machine.dyson.DysonBlockEntities;
import com.space_m.machine.dyson.TileEntityDysonConverterAnatmogenesis;
import com.space_m.machine.dyson.TileEntityDysonConverterHE;
import com.space_m.machine.dyson.TileEntityDysonConverterTU;
import com.space_m.machine.dyson.TileEntityDysonLauncher;
import com.space_m.machine.dyson.TileEntityDysonReceiver;
import com.space_m.machine.dyson.client.DysonModels;
import com.space_m.machine.mining.DishMachines;
import com.space_m.machine.mining.MiningMachines;
import com.space_m.machine.mining.TileEntityDishControl;
import com.space_m.machine.mining.TileEntityMachineDriveProcessor;
import com.space_m.SpaceMod;
import com.space_m.machine.mining.TileEntityMachineGasDock;
import com.space_m.machine.mining.TileEntityMachineMagma;
import com.space_m.machine.misc.MiscBlockEntities;
import com.space_m.machine.misc.TileEntityMachineSolarPanel;
import com.space_m.machine.misc.TileEntityMachineStardar;
import com.space_m.machine.misc.TileEntityRadiator;
import com.space_m.orbit.OrbitBlocks;
import com.space_m.orbit.TileEntityOrbitalStation;
import com.space_m.orbit.propulsion.PropulsionBlockEntities;
import com.space_m.orbit.propulsion.TileEntityMachineHTR3;
import com.space_m.orbit.propulsion.TileEntityMachineHTRF4;
import com.space_m.orbit.propulsion.TileEntityMachineHTRNeo;
import com.space_m.orbit.propulsion.TileEntityOrbitalStationComputer;
import com.space_m.orbit.propulsion.TileEntityOrbitalStationLauncher;
import com.space_m.orbit.propulsion.TileEntityXenonThruster;
import com.space_m.orbit.propulsion.client.PropulsionModels;
import com.space_m.rocket.MachineRocketAssemblyBlockEntity;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.client.RocketModels;
import com.space_m.rocket.client.RocketPronter;
import com.space_m.rocket.pad.TileEntityLaunchPadRocket;
import com.space_m.rocket.transport.CombatDropPodBlockEntity;
import com.space_m.rocket.transport.TransporterRocketBlockEntity;
import com.space_m.rocket.transport.client.CombatDropPodRenderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Nucleus declarative machine registrations for Space-Modernized.
 * Replaces bespoke legacy BlockEntityRenderers across all machine subsystems.
 */
public final class SpaceMachineRenderers {

    private static final Random DROP_POD_RAND = new Random();
    private static final ResourceLocation GAS_ROCKET_TEX = SpaceMod.id("textures/models/gas_rocket.png");
    private static final String ROCKET_ASSEMBLY_MODEL = "space_m:models/machines/rocket_assembly.obj";
    private static final String ROCKET_ASSEMBLY_TEX = "space_m:textures/models/machines/rocket_assembly.png";
    private static final com.hbm_m.client.render.SimpleObjModel ROCKET_PAD_MODEL = new com.hbm_m.client.render.SimpleObjModel(SpaceMod.id("models/machines/rocket_pad.obj"));
    private static final ResourceLocation ROCKET_PAD_SUPPORT_TEX = SpaceMod.id("textures/models/machines/rocket_pad_support.png");
    private static final com.hbm_m.client.render.SimpleObjModel DOCKING_PORT_MODEL = new com.hbm_m.client.render.SimpleObjModel(SpaceMod.id("models/machines/docking_port.obj"));
    private static final ResourceLocation DOCKING_PORT_TEX = SpaceMod.id("textures/models/machines/docking_port.png");
    private static final ResourceLocation DOCKING_PORT_LAUNCHER_TEX = SpaceMod.id("textures/models/machines/docking_port_launcher.png");

    private SpaceMachineRenderers() {}

    public static void registerAll() {
        registerChemMachines();
        registerAtmosphericAndLifeSupport();
        registerMiscAndPower();
        registerMiningAndDish();
        registerDysonSwarm();
        registerPropulsionAndOrbit();
        registerRocketry();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Chemical Machines
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerChemMachines() {
        // Alkylation Unit (1.7.10 RenderAlkylation: 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("alkylation_unit", ChemMachines.ALKYLATION_BE.get(), TileEntityMachineAlkylation.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("Cylinder")
            .register();

        // Cryogenic Distillation Column (1.7.10 RenderCryoDistill: Y = -2.0, 180 + [0,90,180,270])
        MachineRenderers.machine("cryo_distill", ChemMachines.CRYO_DISTILL_BE.get(), TileEntityMachineCryoDistill.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, -2.0, 90f))
            .part("cryo_distill")
            .register();

        // Vacuum Solderer (1.7.10 RenderVacuumCircuit: 2->90, 4->180, 3->270, 5->0, static parts + item)
        MachineRenderers.machine("vacuum_solderer", ChemMachines.VACUUM_CIRCUIT_BE.get(), TileEntityMachineVacuumCircuit.class)
            .part("Base")
            .part("Arm")
            .part("Bit")
            .part("Fan")
            .hook(SpaceMachineRenderers::renderVacuumCircuitItem)
            .register();

        // Milk Reformer (1.7.10 RenderMilkReformer: 180 + [90,180,270,0])
        MachineRenderers.machine("milk_reformer", ChemMachines.MILK_REFORMER_BE.get(), TileEntityMachineMilkReformer.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, 180f))
            .part("Cube.001")
            .register();

        // Hydroponic Bay (Lights emissive strip when energy >= 200; item renders both)
        MachineRenderers.machine("hydroponic", ChemMachines.HYDROBAY_BE.get(), TileEntityHydroponic.class)
            .part("Base")
            .lightOverride("Lights", be -> be.getEnergyStored() >= 200 ? 0xF000F0 : -1)
            .conditionalPart("Lights", be -> be.getEnergyStored() >= 200)
            .itemParts("Base", "Lights")
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Atmospheric & Life Support
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerAtmosphericAndLifeSupport() {
        // Air Scrubber (1.7.10 RenderAirScrubber: 180 + [0,90,180,270])
        MachineRenderers.machine("air_scrubber", SpaceBlockEntities.AIR_SCRUBBER.get(), TileEntityAirScrubber.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, 90f))
            .part("Base")
            .rotatingPart("Fan", 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevRot, be.rot))
            .register();

        // Atmospheric Vent / Compressor (1.7.10 RenderAtmosphericCompressor: 2x2 corner offset, fan pivot (-0.19, 0, 0.19))
        MachineRenderers.machine("atmo_vent", SpaceBlockEntities.ATMOSPHERIC_VENT.get(), AtmosphericCompressorBlockEntity.class)
            .blockTransform((be, anim) -> {
                setupTransform(be, anim, 0.0, 90f);
                anim.translate(0.5, 0.0, -0.5);
            })
            .part("Body_Cylinder")
            .rotatingPart("Fan_Cylinder.001", 0f, -1f, 0f, -0.19f, 0f, 0.19f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevRot, be.rot))
            .register();

        // Atmospheric Outtake / Emitter (1.7.10 RenderAtmosphericEmitter: facing + (-0.5, 0, 0.5))
        MachineRenderers.machine("atmo_emitter", SpaceBlockEntities.ATMO_EMITTER.get(), AtmosphericEmitterBlockEntity.class)
            .blockTransform((be, anim) -> {
                anim.setupBlockTransform(facing(be));
                anim.translate(-0.5, 0.0, 0.5);
            })
            .part("Base")
            .rotatingPart("Fan", 0f, -1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevFan, be.fan))
            .register();

        // Combat Drop Pod (1.7.10 RenderCombatPod: T(0.5, 1.5, 0.5), then hook T(0, -0.25, 0))
        MachineRenderers.machine("combat_drop_pod", SpaceBlockEntities.COMBAT_DROP_POD.get(), CombatDropPodBlockEntity.class)
            .blockTransform((be, anim) -> anim.translate(0.5, 1.5, 0.5))
            .hook(SpaceMachineRenderers::renderCombatPodHook)
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Misc & Power
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerMiscAndPower() {
        // Solar Panel (1.7.10 RenderSolarPanel: facing-rotated panel + world-aligned connectors)
        MachineRenderers.machine("solar_panel", MiscBlockEntities.SOLAR_PANEL.get(), TileEntityMachineSolarPanel.class)
            .blockTransform((be, anim) -> anim.translate(0.5, 0.0, 0.5))
            .staticPart("solar_panel", (be, pt, gt, pose) -> {
                Direction dir = facing(be);
                if (!CardinalOrientation.isIdentity(dir)) {
                    pose.mulPose(CardinalOrientation.get(dir));
                }
                return true;
            })
            .conditionalPart("connector_bottom", be -> canConnectSolar(be, Direction.DOWN))
            .conditionalPart("connector_top", be -> canConnectSolar(be, Direction.UP))
            .conditionalPart("connector_pos_z", "connector_front", (be, pt, gt, pose) -> true, be -> canConnectSolar(be, Direction.SOUTH))
            .conditionalPart("connector_neg_z", "connector_side", (be, pt, gt, pose) -> true, be -> canConnectSolar(be, Direction.NORTH))
            .conditionalPart("connector_pos_x", "connector_front", (be, pt, gt, pose) -> {
                pose.mulPose(Axis.YP.rotationDegrees(90f));
                return true;
            }, be -> canConnectSolar(be, Direction.EAST))
            .conditionalPart("connector_neg_x", "connector_side", (be, pt, gt, pose) -> {
                pose.mulPose(Axis.YP.rotationDegrees(90f));
                return true;
            }, be -> canConnectSolar(be, Direction.WEST))
            .itemParts("solar_panel", "connector_bottom", "connector_top", "connector_front", "connector_side")
            .register();

        // Stardar (1.7.10 RenderStardar: Y = -3.0, base + drive facing-rotated, dish yaw/pitch in world space)
        MachineRenderers.machine("stardar", MiscBlockEntities.STARDAR.get(), TileEntityMachineStardar.class)
            .blockTransform((be, anim) -> anim.translate(0.5, -3.0, 0.5))
            .staticPart("base", SpaceMachineRenderers::rotateStardarBase)
            .conditionalPart("drive", SpaceMachineRenderers::rotateStardarBase, be -> !be.getInventory().getStackInSlot(0).isEmpty())
            .rotatingPart("rotation", 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevDishYaw, be.dishYaw))
            .turntableChainPart("pitch", "pitch", 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f,
                (be, pt) -> Mth.lerp(pt, be.prevDishYaw, be.dishYaw),
                MachineSpec.JointNode.rotate(1f, 0f, 0f, 0f, 10.6f, 0f, (be, pt, gt, out, off) -> {
                    out[off] = Mth.lerp(pt, be.prevDishPitch, be.dishPitch);
                    return true;
                }))
            .itemParts("base", "drive", "rotation", "pitch")
            .register();

        // Radiator (1.7.10 RenderRadiator: T(0.5, 0.5, 0.5), facing, T(-0.5, 0, 0), Rz(-90))
        MachineRenderers.machine("radiator", MiscBlockEntities.RADIATOR.get(), TileEntityRadiator.class)
            .blockTransform((be, anim) -> {
                setupTransform(be, anim, 0.5, 0f);
                anim.translate(-0.5, 0.0, 0.0);
                anim.rotate(-90f, 0f, 0f, 1f);
            })
            .part("Base")
            .part("PanelStart")
            .part("FrameStart")
            .part("Panel1").part("Frame1")
            .part("Panel2").part("Frame2")
            .part("Panel3").part("Frame3")
            .part("Panel4").part("Frame4")
            .part("Panel5").part("Frame5")
            .part("PanelLast").part("FrameLast")
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Mining & Dish Subsystems
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerMiningAndDish() {
        // Magma Drill (1.7.10 RenderMagma: Y = -3.0, 3->0, 5->90, 2->180, 4->270)
        MachineRenderers.machine("magma_drill", MiningMachines.MAGMA_BE.get(), TileEntityMachineMagma.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, -3.0, 90f))
            .part("Foundation")
            .part("Drill")
            .part("Pistons")
            .part("Cables")
            .rotatingPart("Drillhead", 0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevDrillRotation, be.drillRotation))
            .rotatingPart("Blades", 0f, -1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevDrillRotation, be.drillRotation))
            .hook(SpaceMachineRenderers::renderMagmaLavaQuad)
            .register();

        // Gas Dock (1.7.10 RenderDecoBlock: platform Base + gas rocket hook)
        MachineRenderers.machine("gas_dock", MiningMachines.GAS_DOCK_BE.get(), TileEntityMachineGasDock.class)
            .part("Base")
            .hook(SpaceMachineRenderers::renderGasDockRocket)
            .register();

        // Drive Processor
        MachineRenderers.machine("drive_processor", MiningMachines.DRIVE_PROCESSOR_BE.get(), TileEntityMachineDriveProcessor.class)
            .part("Base")
            .conditionalPart("Drive", be -> be.hasDrive)
            .itemParts("Base", "Drive")
            .register();

        // Dish Controller
        MachineRenderers.machine("dish_controller", DishMachines.DISH_CONTROL_BE.get(), TileEntityDishControl.class)
            .part("dish_control")
            .part("joystick")
            .conditionalPart("drive", TileEntityDishControl::starDarHasDisk)
            .itemParts("dish_control", "joystick", "drive")
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Dyson Swarm
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerDysonSwarm() {
        // Dyson Spinlaunch Launcher (1.7.10: 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("dyson_launcher", DysonBlockEntities.LAUNCHER.get(), TileEntityDysonLauncher.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("Launch")
            .part("The_Thing_That_Rotates", (be, pt, gt, pose) -> {
                float t = Mth.lerp(pt, be.lastRotation, be.rotation);
                pose.translate(0, 8.5F, 0);
                pose.mulPose(Axis.XP.rotationDegrees(45.0F));
                pose.mulPose(Axis.YP.rotationDegrees(t));
                pose.mulPose(Axis.XP.rotationDegrees(-45.0F));
                pose.translate(0, -8.5F, 0);
                return true;
            })
            .itemParts("Launch", "The_Thing_That_Rotates", "Payload")
            .hook(SpaceMachineRenderers::renderDysonLauncherPayload)
            .register();

        // Dyson Receiver (1.7.10: 2->0, 4->90, 3->180, 5->270; Coil1/Coil3 +Z, Coil2 -Z)
        MachineRenderers.machine("dyson_receiver", DysonBlockEntities.RECEIVER.get(), TileEntityDysonReceiver.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("DysonReceiver")
            .rotatingPart("Coil1", 0f, 0f, 1f, 0f, 1.5f, 0f, 0f, 0f, 0f, (be, pt) -> be.isReceiving && be.getLevel() != null ? (double) (be.getLevel().getGameTime() + pt) : 0.0)
            .rotatingPart("Coil2", 0f, 0f, -1f, 0f, 1.5f, 0f, 0f, 0f, 0f, (be, pt) -> be.isReceiving && be.getLevel() != null ? (double) (be.getLevel().getGameTime() + pt) : 0.0)
            .rotatingPart("Coil3", 0f, 0f, 1f, 0f, 1.5f, 0f, 0f, 0f, 0f, (be, pt) -> be.isReceiving && be.getLevel() != null ? (double) (be.getLevel().getGameTime() + pt) : 0.0)
            .hook(SpaceMachineRenderers::renderDysonReceiverBeam)
            .register();

        // Dyson HE Converter (1.7.10: 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("dyson_he_converter", DysonBlockEntities.CONVERTER_HE.get(), TileEntityDysonConverterHE.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("HE_Converter")
            .rotatingPart("Coil1", 0f, 0f, 1f, 0f, 1.5f, 0f, 0f, 0f, 0f, (be, pt) -> be.isConverting && be.getLevel() != null ? (double) (be.getLevel().getGameTime() + pt) : 0.0)
            .rotatingPart("Coil2", 0f, 0f, -1f, 0f, 1.5f, 0f, 0f, 0f, 0f, (be, pt) -> be.isConverting && be.getLevel() != null ? (double) (be.getLevel().getGameTime() + pt) : 0.0)
            .rotatingPart("Coil3", 0f, 0f, 1f, 0f, 1.5f, 0f, 0f, 0f, 0f, (be, pt) -> be.isConverting && be.getLevel() != null ? (double) (be.getLevel().getGameTime() + pt) : 0.0)
            .register();

        // Dyson TU Converter (1.7.10: 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("dyson_tu_converter", DysonBlockEntities.CONVERTER_TU.get(), TileEntityDysonConverterTU.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("TU_Converter")
            .register();

        // Dyson Anatmogenesis (1.7.10: 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("dyson_anatmogenesis", DysonBlockEntities.CONVERTER_ANATMOGENESIS.get(), TileEntityDysonConverterAnatmogenesis.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("Anatmogenesis")
            .part("Coils", (be, pt, gt, pose) -> {
                if (be.isConverting && be.getLevel() != null) {
                    float t = be.getLevel().getGameTime() + pt;
                    float jitter = (float) (Math.sin(t * 0.1) * 0.025 + be.getLevel().random.nextFloat() * 0.02);
                    pose.translate(0f, jitter, 0f);
                }
                return true;
            })
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Propulsion & Orbit
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerPropulsionAndOrbit() {
        // Orbital Station Docking Port (1.7.10: Y = +1.0, 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("orbital_station", OrbitBlocks.ORBITAL_STATION_BE.get(), TileEntityOrbitalStation.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 1.0, -90f))
            .part("Port")
            .itemParts("Port", "ArmZN", "ArmZP", "ArmXP", "ArmXN")
            .hook(SpaceMachineRenderers::renderOrbitalStationArms)
            .register();

        // Orbital Station Launcher (1.7.10: Y = +1.0, 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("orbital_station_launcher", PropulsionBlockEntities.ORBITAL_STATION_LAUNCHER.get(), TileEntityOrbitalStationLauncher.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 1.0, -90f))
            .part("Port")
            .itemParts("Port", "ArmZN", "ArmZP", "ArmXP", "ArmXN")
            .hook(SpaceMachineRenderers::renderStationLauncherArmsAndRocket)
            .register();

        // Orbital Station Computer (1.7.10: 2->0, 4->90, 3->180, 5->270)
        MachineRenderers.machine("orbital_computer", PropulsionBlockEntities.ORBITAL_STATION_COMPUTER.get(), TileEntityOrbitalStationComputer.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, 0.0, -90f))
            .part("base")
            .conditionalPart("drive", be -> be.hasDrive)
            .register();

        // HTR-3 Thruster (1.7.10 RenderHTR3: Y = -3.0, facing, animated cover/suspension/piston/shrouds/engine)
        MachineRenderers.machine("htr3", PropulsionBlockEntities.HTR3.get(), TileEntityMachineHTR3.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, -3.0, 0f))
            .part("Center").part("PipeL1").part("PipeL2").part("PipeR1").part("PipeR2")
            .part("CoverTop", SpaceMachineRenderers::animateHtr3Cover)
            .part("CoverBottom", SpaceMachineRenderers::animateHtr3Cover)
            .part("SuspensionTop", SpaceMachineRenderers::animateHtr3Suspension)
            .part("SuspensionBottom", SpaceMachineRenderers::animateHtr3Suspension)
            .part("Piston", SpaceMachineRenderers::animateHtr3Piston)
            .part("ShroudH", SpaceMachineRenderers::animateHtr3ShroudH)
            .part("ShroudV", SpaceMachineRenderers::animateHtr3ShroudV)
            .part("Engine", SpaceMachineRenderers::animateHtr3Engine)
            .register();

        // HTR-F4 Thruster (Static composite, Y = -2.0 + facing)
        MachineRenderers.machine("htrf4", PropulsionBlockEntities.HTRF4.get(), TileEntityMachineHTRF4.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, -2.0, 0f))
            .part("Center").part("Center1.001").part("Center2.001").part("Center3.001")
            .part("Engine.001").part("Power1.001").part("Power2.001")
            .part("Rotor1.001").part("Rotor2.001").part("Rotor3.001")
            .register();

        // HTR-F4 Neo (1.7.10 RenderHTRNeo: Y = -2.0 + facing, both rotors rotate +Z)
        MachineRenderers.machine("htrf4_neo", PropulsionBlockEntities.HTRF4_NEO.get(), TileEntityMachineHTRNeo.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, -2.0, 0f))
            .part("Base")
            .part("Engine")
            .rotatingPart("Rotor", 0f, 0f, 1f, 0f, 2.5f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevRotor, be.rotor))
            .rotatingPart("Rotor2", 0f, 0f, 1f, 0f, 2.5f, 0f, 0f, 0f, 0f, (be, pt) -> Mth.lerp(pt, be.prevRotor, be.rotor))
            .hook(SpaceMachineRenderers::renderHtrNeoPlume)
            .register();

        // Xenon Ion Thruster (1.7.10 RenderXenonThruster: Y = -1.0, -90 + facing, T(0, 0, -0.5))
        MachineRenderers.machine("xenon_thruster", PropulsionBlockEntities.XENON_THRUSTER.get(), TileEntityXenonThruster.class)
            .blockTransform((be, anim) -> {
                setupTransform(be, anim, -1.0, -90f);
                anim.translate(0.0, 0.0, -0.5);
            })
            .part("Thruster")
            .hook(SpaceMachineRenderers::renderXenonPlume)
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Rocketry & Assembly
    // ─────────────────────────────────────────────────────────────────────────
    private static void registerRocketry() {
        // Rocket Assembly Machine (1.7.10 RenderRocketAssembly: Y = -2.0, 180 + [0,90,180,270])
        MachineRenderers.machine("rocket_assembly", SpaceBlockEntities.ROCKET_ASSEMBLY.get(), MachineRocketAssemblyBlockEntity.class)
            .blockTransform((be, anim) -> setupTransform(be, anim, -2.0, 90f))
            .part("Base")
            .hook(SpaceMachineRenderers::renderAssemblyRocketHook)
            .register();

        // Launch Pad Rocket (Launch clamp tower + standing rocket hook; item renders pad + tower parts)
        MachineRenderers.machine("launch_pad_rocket", SpaceBlockEntities.LAUNCH_PAD_ROCKET.get(), TileEntityLaunchPadRocket.class)
            .part("Base")
            .itemParts("Base", "Tower_Base", "Tower_Segment", "Tower_Cap", "Tower_Segment_Small")
            .hook(SpaceMachineRenderers::renderLaunchPadRocketHook)
            .register();

        // Transporter Rocket Pad (Refueling arm pipe rotation + transporter hook)
        MachineRenderers.machine("transporter_pad", SpaceBlockEntities.TRANSPORTER_ROCKET.get(), TransporterRocketBlockEntity.class)
            .part("base")
            .rotatingPart("pipe", 1f, 0f, 0f, 0f, 0.75f, -0.75f, 0f, 0f, 0f,
                (be, pt) -> -30.0f * Mth.clamp(getPipeEngage(be, pt), 0f, 1f))
            .hook(SpaceMachineRenderers::renderTransporterRocketHook)
            .register();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper Methods & Render Hooks
    // ─────────────────────────────────────────────────────────────────────────

    private static Direction facing(BlockEntity be) {
        var state = be.getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        }
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING);
        }
        return Direction.NORTH;
    }

    /**
     * Standard 1.7.10 BlockDummyable TESR transform:
     * translate(0.5, yOffset, 0.5) + CardinalOrientation(facing) [N=90, W=180, S=270, E=0] + rotate(extraYaw, 0, 1, 0).
     * extraYaw = 0f   -> 2:90,  4:180, 3:270, 5:0
     * extraYaw = -90f -> 2:0,   4:90,  3:180, 5:270
     * extraYaw = 90f  -> 2:180, 4:270, 3:0,   5:90
     * extraYaw = 180f -> 2:270, 4:0,   3:90,  5:180
     */
    private static void setupTransform(BlockEntity be, LegacyAnimator anim, double yOffset, float extraYaw) {
        anim.setupBlockTransform(facing(be));
        if (yOffset != 0.0) {
            anim.translate(0.0, yOffset, 0.0);
        }
        if (extraYaw != 0.0f) {
            anim.rotate(extraYaw, 0f, 1f, 0f);
        }
    }

    private static boolean rotateStardarBase(TileEntityMachineStardar be, float pt, long gt, PoseStack pose) {
        Direction dir = facing(be);
        if (!CardinalOrientation.isIdentity(dir)) {
            pose.mulPose(CardinalOrientation.get(dir));
        }
        pose.mulPose(Axis.YP.rotationDegrees(90f));
        return true;
    }

    private static double htr3Time(TileEntityMachineHTR3 be, float pt) {
        return be.lastTime + (be.time - be.lastTime) * pt;
    }

    private static double htr3Cover(double t) {
        double coverTimer = (t / 5D) % (Math.PI * 4);
        return (Math.sin(coverTimer) + Math.sin(coverTimer * 2) + Math.sin(coverTimer * 4)) * 0.5;
    }

    private static double htr3Sway(double t) {
        double swayTimer = (t / 3D) % (Math.PI * 4);
        return (Math.sin(swayTimer) + Math.sin(swayTimer * 2) + Math.sin(swayTimer * 4) + 2.23255D) * 0.5;
    }

    private static double htr3BellH(double t) {
        double bellTimer = (t / 5D) % (Math.PI * 4);
        return (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
    }

    private static double htr3BellV(double t) {
        double bellTimer = (t / 5D) % (Math.PI * 4);
        return (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
    }

    private static boolean animateHtr3Cover(TileEntityMachineHTR3 be, float pt, long gt, PoseStack pose) {
        double cover = htr3Cover(htr3Time(be, pt));
        pose.translate(0, 0, -cover * 0.125);
        return true;
    }

    private static boolean animateHtr3Suspension(TileEntityMachineHTR3 be, float pt, long gt, PoseStack pose) {
        double cover = htr3Cover(htr3Time(be, pt));
        pose.translate(0, 0, 3.5);
        pose.scale(1f, 1f, (float) ((3.0 + cover * 0.125) / 3.0));
        pose.translate(0, 0, -3.5);
        return true;
    }

    private static boolean animateHtr3Piston(TileEntityMachineHTR3 be, float pt, long gt, PoseStack pose) {
        double t = htr3Time(be, pt);
        double sway = htr3Sway(t);
        double pistonTimer = (t / 5D) % (Math.PI * 2);
        double piston = BobMathUtil.sps(pistonTimer);
        pose.translate(0, 0, piston * 0.375D + 0.375D - sway * 0.125D);
        return true;
    }

    private static boolean animateHtr3ShroudH(TileEntityMachineHTR3 be, float pt, long gt, PoseStack pose) {
        double h = htr3BellH(htr3Time(be, pt));
        pose.translate(0, -h * 0.125D, 0);
        return true;
    }

    private static boolean animateHtr3ShroudV(TileEntityMachineHTR3 be, float pt, long gt, PoseStack pose) {
        double v = htr3BellV(htr3Time(be, pt));
        pose.translate(v * 0.125D, 0, 0);
        return true;
    }

    private static boolean animateHtr3Engine(TileEntityMachineHTR3 be, float pt, long gt, PoseStack pose) {
        double t = htr3Time(be, pt);
        double sway = htr3Sway(t);
        double h = htr3BellH(t);
        double v = htr3BellV(t);
        pose.translate(0, 0, -sway * 0.125D);
        pose.translate(0, 3.5, -1);
        pose.mulPose(Axis.YP.rotationDegrees((float) (v * 2.0)));
        pose.mulPose(Axis.XP.rotationDegrees((float) (h * 2.0)));
        pose.translate(0, -3.5, 1);
        return true;
    }

    private static boolean canConnectSolar(TileEntityMachineSolarPanel be, Direction dir) {
        if (be.getLevel() == null) return false;
        BlockPos p = be.getBlockPos().relative(dir);
        BlockEntity neighbor = be.getLevel().getBlockEntity(p);
        return neighbor instanceof IEnergyConnector c && c.canConnectEnergy(dir.getOpposite());
    }

    private static float getPipeEngage(TransporterRocketBlockEntity pad, float pt) {
        if (pad.launchTicks >= 0) {
            return !pad.hasRocket ? (pad.launchTicks + pt) * 0.25F : 1.0F;
        } else {
            return 1 - (-pad.launchTicks - 1 + pt) * 0.25F;
        }
    }

    private static void renderVacuumCircuitItem(TileEntityMachineVacuumCircuit be, float pt, PoseStack pose, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        ItemStack stack = be.display;
        if (stack != null && !stack.isEmpty()) {
            pose.pushPose();
            pose.translate(0.625D, 1.25D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.mulPose(Axis.XP.rotationDegrees(-90));
            pose.scale(1.5F, 1.5F, 1.5F);
            ObjBerHelper.renderDecoItemInFrame(pose, buf, stack, light, be.getLevel());
            pose.popPose();
        }
    }

    private static void renderMagmaLavaQuad(TileEntityMachineMagma drill, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        float lavaHeight = Mth.lerp(pt, drill.prevLavaHeight, drill.lavaHeight);
        if (lavaHeight > 0.01F) {
            TextureAtlasSprite lava = Minecraft.getInstance().getBlockRenderer().getBlockModelShaper()
                    .getParticleIcon(Blocks.LAVA.defaultBlockState());
            VertexConsumer lvc = buf.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
            Matrix4f m = ps.last().pose();
            Vector3f n = new Vector3f(0F, 1F, 0F).mul(ps.last().normal());

            for (int ox = -1; ox <= 1; ox++) {
                for (int oz = -1; oz <= 1; oz++) {
                    float minX = ox - 0.5F, maxX = ox + 0.5F, minZ = oz - 0.5F, maxZ = oz + 0.5F;
                    RenderHooks.vertexFull(lvc, m, maxX, lavaHeight, maxZ, 255, 255, 255, 255, lava.getU1(), lava.getV1(), OverlayTexture.NO_OVERLAY, light, n.x(), n.y(), n.z());
                    RenderHooks.vertexFull(lvc, m, maxX, lavaHeight, minZ, 255, 255, 255, 255, lava.getU1(), lava.getV0(), OverlayTexture.NO_OVERLAY, light, n.x(), n.y(), n.z());
                    RenderHooks.vertexFull(lvc, m, minX, lavaHeight, minZ, 255, 255, 255, 255, lava.getU0(), lava.getV0(), OverlayTexture.NO_OVERLAY, light, n.x(), n.y(), n.z());
                    RenderHooks.vertexFull(lvc, m, minX, lavaHeight, maxZ, 255, 255, 255, 255, lava.getU0(), lava.getV1(), OverlayTexture.NO_OVERLAY, light, n.x(), n.y(), n.z());
                }
            }
        }
    }

    private static void renderGasDockRocket(TileEntityMachineGasDock transporter, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        if (transporter.launchTicks < 100) {
            ps.pushPose();
            ps.translate(0.0F, 0.75F + Mth.clamp(transporter.launchTicks + (transporter.hasRocket ? -pt : pt), 0, 200), 0.0F);
            MinerRocketRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(GAS_ROCKET_TEX)), light);
            ps.popPose();
        }
    }

    private static void renderDysonLauncherPayload(TileEntityDysonLauncher launcher, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        float t = Mth.lerp(pt, launcher.lastRotation, launcher.rotation);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DysonModels.SPINLAUNCH_TEX));
        if (launcher.satCount > 0 && !launcher.isSpinningDown) {
            ps.pushPose();
            ps.translate(0, 8.5F, 0);
            ps.mulPose(Axis.XP.rotationDegrees(45.0F));
            ps.mulPose(Axis.YP.rotationDegrees(t));
            ps.mulPose(Axis.XP.rotationDegrees(-45.0F));
            ps.translate(0, -8.5F, 0);
            DysonModels.SPINLAUNCH.renderPart("Payload", ps, vc, light);
            ps.popPose();
        }
        if (launcher.isSpinningDown) {
            float p = launcher.payloadTicks + pt;
            ps.pushPose();
            ps.translate(1.0F, 8.5F, 0);
            ps.mulPose(Axis.XP.rotationDegrees(45.0F));
            ps.mulPose(Axis.YP.rotationDegrees(90.0F));
            ps.translate(p * 10.0F, 0, 0);
            ps.mulPose(Axis.XP.rotationDegrees(-45.0F));
            ps.translate(0, -8.5F, 0);
            DysonModels.SPINLAUNCH.renderPart("Payload", ps, vc, light);
            ps.popPose();
        }
    }

    private static void renderDysonReceiverBeam(TileEntityDysonReceiver receiver, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        if (receiver.isReceiving && receiver.getLevel() != null) {
            long time = receiver.getLevel().getGameTime();
            int length = receiver.beamLength;
            int color = 0xff8800;
            DysonModels.prontBeamSolid(ps, buf, new Vec3(0, 0, length), true, color, color, 0, 1, 0F, 2, 0.4F, 0.5F);
            DysonModels.prontBeamSolid(ps, buf, new Vec3(0, 0, length), false, color, color, (int) (time % 1000), (length / 2), 0.0625F, 2, 0.4F, 0.5F);
        }
    }

    private static void renderOrbitalStationArms(TileEntityOrbitalStation station, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        float armRotation = Mth.lerp(pt, station.prevRot, station.rot);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DOCKING_PORT_TEX));
        ps.pushPose();
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.translate(0, -1.75F, -2);
            ps.mulPose(Axis.XP.rotationDegrees(-armRotation));
            ps.translate(0, 1.75F, 2);
            DOCKING_PORT_MODEL.renderPart("ArmZP", ps, vc, light);
            ps.popPose();
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        ps.popPose();
    }

    private static void renderStationLauncherArmsAndRocket(TileEntityOrbitalStationLauncher launcher, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        float armRotation = Mth.lerp(pt, launcher.prevRot, launcher.rot);
        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(DOCKING_PORT_LAUNCHER_TEX));
        ps.pushPose();
        for (int i = 0; i < 4; i++) {
            ps.pushPose();
            ps.translate(0, -1.75F, -2);
            ps.mulPose(Axis.XP.rotationDegrees(-armRotation));
            ps.translate(0, 1.75F, 2);
            DOCKING_PORT_MODEL.renderPart("ArmZP", ps, vc, light);
            ps.popPose();
            ps.mulPose(Axis.YP.rotationDegrees(90));
        }
        ps.popPose();

        if (launcher.rocket != null && launcher.rocket.extraIssues.isEmpty()) {
            ps.pushPose();
            ps.translate(0, -launcher.rocket.getHeight() - 0.5, 0);
            RocketPronter.prontRocket(launcher.rocket, ps, buf, light, false);
            ps.popPose();
        }
    }

    private static void renderHtrNeoPlume(TileEntityMachineHTRNeo rocket, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        float trailStretch = rocket.getLevel() != null ? rocket.getLevel().random.nextFloat() : 0F;
        trailStretch = 1.2F - (trailStretch * trailStretch * 0.1F);
        trailStretch *= rocket.thrustAmount;

        if (trailStretch > 0) {
            ps.pushPose();
            ps.translate(0, 0, 12);
            ps.scale(1, 1, trailStretch);
            ps.translate(0, 0, -12);

            PropulsionModels.HTRF4_NEO.renderPartEntity("Exhaust", ps,
                    buf.getBuffer(RenderType.entityTranslucentEmissive(PropulsionModels.HTRF4_EXHAUST_TEX)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, rocket.plasmaR, rocket.plasmaG, rocket.plasmaB, rocket.thrustAmount);

            long time = System.currentTimeMillis();
            double sparkleSpin = time / 250D % 1D;

            float a = rocket.thrustAmount * 0.75F;
            VertexConsumer sparkle = new UvShiftConsumer(buf.getBuffer(RenderType.eyes(PropulsionModels.PLASMA_SPARKLE_TEX)), 0F, (float) sparkleSpin, 0F, 1F);
            PropulsionModels.HTRF4_NEO.renderPartEntity("Exhaust", ps, sparkle, 0xF000F0, OverlayTexture.NO_OVERLAY,
                    Math.min(rocket.plasmaR * 2 * a, 1F), Math.min(rocket.plasmaG * 2 * a, 1F), Math.min(rocket.plasmaB * 2 * a, 1F), 1F);
            ps.popPose();
        }
    }

    private static void renderXenonPlume(TileEntityXenonThruster rocket, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        float trailStretch = rocket.getLevel() != null ? rocket.getLevel().random.nextFloat() : 0F;
        trailStretch = 1.2F - (trailStretch * trailStretch * 0.2F);
        trailStretch *= rocket.thrustAmount;

        if (trailStretch > 0) {
            ps.pushPose();
            ps.translate(0, 0, -1F);
            ps.scale(1, 1, trailStretch);
            ps.translate(0, 0, 1F);

            PropulsionModels.XENON_THRUSTER.renderPartEntity("Exhaust", ps,
                    buf.getBuffer(RenderType.entityTranslucentEmissive(PropulsionModels.XENON_EXHAUST_TEX)),
                    0xF000F0, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, rocket.thrustAmount);
            ps.popPose();
        }
    }

    private static void renderAssemblyRocketHook(MachineRocketAssemblyBlockEntity te, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        if (te.rocket != null && te.rocket.extraIssues.isEmpty()) {
            ps.pushPose();
            ps.translate(0F, 2.95F, 0F);

            if (!te.rocket.stages.isEmpty()) {
                RocketStruct.RocketStage stage = te.rocket.stages.get(0);
                if (stage.thruster != null) {
                    ps.translate(0, -stage.thruster.height, 0);
                }
            }

            RocketPronter.prontRocket(te.rocket, ps, buf, light, false);
            ps.popPose();

            VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(RocketModels.texture(ROCKET_ASSEMBLY_TEX)));

            ps.pushPose();
            for (int i = 0; i < te.rocket.stages.size(); i++) {
                RocketStruct.RocketStage stage = te.rocket.stages.get(i);
                RocketStruct.RocketStage nextStage = i < te.rocket.stages.size() - 1 ? te.rocket.stages.get(i + 1) : null;

                double targetHeight = 0;
                if (stage.fuselage != null) targetHeight += stage.fuselage.height * stage.getStack();
                if (nextStage != null && nextStage.thruster != null) targetHeight += nextStage.thruster.height;

                while (targetHeight > 1) {
                    ps.translate(0, 1, 0);
                    RocketModels.model(ROCKET_ASSEMBLY_MODEL).renderPart("Support", ps, vc, light);
                    targetHeight--;
                }
                ps.translate(0, targetHeight, 0);

                RocketModels.model(ROCKET_ASSEMBLY_MODEL).renderPart("Level", ps, vc, light);
            }
            ps.popPose();
        }
    }

    private static void renderLaunchPadRocketHook(TileEntityLaunchPadRocket pad, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        if (pad.canSeeSky && pad.height >= 8) {
            VertexConsumer vc = buf.getBuffer(RenderType.entityCutoutNoCull(ROCKET_PAD_SUPPORT_TEX));
            ps.pushPose();
            ROCKET_PAD_MODEL.renderPart("Tower_Base", ps, vc, light);

            for (int oy = 8; oy < pad.height - 2; oy += 3) {
                ROCKET_PAD_MODEL.renderPart("Tower_Segment", ps, vc, light);
                ps.translate(0, 3, 0);
            }

            ps.translate(0, -3, 0);

            for (int oy = 0; oy < (pad.height - 2) % 3; oy++) {
                ROCKET_PAD_MODEL.renderPart("Tower_Segment_Small", ps, vc, light);
                ps.translate(0, 1, 0);
            }

            ps.translate(0, -1, 0);
            ROCKET_PAD_MODEL.renderPart("Tower_Cap", ps, vc, light);
            ps.popPose();
        }

        if (pad.rocket != null) {
            ps.pushPose();
            ps.translate(0.0F, 3.0F, 0.0F);
            RocketPronter.prontRocket(pad.rocket, ps, buf, light);
            ps.popPose();
        }
    }

    private static void renderTransporterRocketHook(TransporterRocketBlockEntity pad, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        if (pad.launchTicks < 100) {
            ps.pushPose();
            ps.translate(0.0F, 0.75F + Mth.clamp(pad.launchTicks + (pad.hasRocket ? -pt : pt), 0, 200), 0.0F);
            MinerRocketRenderer.MODEL.renderAll(ps, buf.getBuffer(RenderType.entityCutoutNoCull(MinerRocketRenderer.TEXTURE)), light);
            ps.popPose();
        }
    }

    private static int podIdentity(BlockPos p) {
        return (p.getY() + p.getZ() * 27644437) * 27644437 + p.getX();
    }

    private static void renderCombatPodHook(CombatDropPodBlockEntity pod, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay, MachineRenderApi api) {
        ps.pushPose();
        ps.translate(0.0F, -0.25F, 0.0F);

        DROP_POD_RAND.setSeed(podIdentity(pod.getBlockPos()));
        double yaw = DROP_POD_RAND.nextDouble() * 360;
        double pitch = DROP_POD_RAND.nextDouble() * 8;
        double roll = DROP_POD_RAND.nextDouble() * 6;

        ps.mulPose(Axis.YP.rotationDegrees((float) yaw));
        ps.mulPose(Axis.XP.rotationDegrees((float) pitch));
        ps.mulPose(Axis.ZP.rotationDegrees((float) roll));

        double open = Mth.lerp(pt, pod.prevHatchopen, pod.hatchopen);
        double open2 = Mth.lerp(pt, pod.prevHatchopen2, pod.hatchopen2);

        VertexConsumer vc = buf.getBuffer(RenderType.entityCutout(CombatDropPodRenderer.skin(pod.color)));

        ps.pushPose();
        ps.translate(0, open * 0.013, open * 0.01);
        ps.mulPose(Axis.XP.rotationDegrees((float) open));
        CombatDropPodRenderer.MODEL.renderPart("hatch1", ps, vc, light);
        ps.popPose();

        ps.pushPose();
        ps.translate(0, -open2 * 0.013, -open * 0.01);
        ps.mulPose(Axis.XP.rotationDegrees((float) open2));
        CombatDropPodRenderer.MODEL.renderPart("hatch2", ps, vc, light);
        ps.popPose();

        CombatDropPodRenderer.MODEL.renderPart("bomb", ps, vc, light);

        ps.popPose();
    }
}
