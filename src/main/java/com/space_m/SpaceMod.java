package com.space_m;

import com.space_m.main.SpaceRegistry;

import net.minecraft.resources.ResourceLocation;
//? if forge {
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
//?} elif neoforge {
/*import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
*///?}

/**
 * Einstieg des Addons Space Modernized (Port von NTM Space, 1.7.10) fuer beide Loader.
 *
 * <p>Die gesamte Initialisierung laeuft loaderneutral ueber {@link SpaceRegistry#init()} (Architectury);
 * hier wird nur die Mod-Bus-Verdrahtung des jeweiligen Loaders erledigt.
 */
@Mod(SpaceMod.MOD_ID)
public final class SpaceMod {
    public static final String MOD_ID = /*$ mod_id */"space_m";

    //? if forge {
    @SuppressWarnings("removal")
    public SpaceMod() {
        // Forge: Architectury-DeferredRegister findet die Mod-Bus nur nach expliziter Anmeldung
        // ("Can't get event bus for mod space_m because it was not registered!").
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(MOD_ID, modBus);
        SpaceRegistry.init();
    }
    //?} elif neoforge {
    /*public SpaceMod(IEventBus modBus) {
        // NeoForge: Architectury loest die Mod-Bus selbst auf.
        SpaceRegistry.init();
    }
    *///?}

    /** {@code space_m:<path>}. */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
