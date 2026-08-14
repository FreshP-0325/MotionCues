package io.github.freshp_0325.motioncues.neoforge;

import io.github.freshp_0325.motioncues.MotionCues;
import io.github.freshp_0325.motioncues.MotionCuesConfigScreen;
import io.github.freshp_0325.motioncues.MotionCuesKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = MotionCues.MOD_ID, dist = Dist.CLIENT)
public final class MotionCuesNeoForge {
    public MotionCuesNeoForge(
            IEventBus modBus, ModContainer container) {
        MotionCues.initialize(FMLPaths.CONFIGDIR.get());
        container.registerExtensionPoint(
                IConfigScreenFactory.class,
                (ignoredContainer, parent) ->
                        MotionCuesConfigScreen.create(parent));
        modBus.addListener(this::registerGuiLayers);
        modBus.addListener(this::registerKeyMappings);
        NeoForge.EVENT_BUS.addListener(this::clientTick);
        NeoForge.EVENT_BUS.addListener(this::screenRenderPost);
    }

    private void registerGuiLayers(
            RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(
                        MotionCues.MOD_ID, "cues"),
                (graphics, partialTick) ->
                        MotionCues.render(
                                graphics,
                                partialTick
                                        .getGameTimeDeltaPartialTick(
                                                false)));
    }

    private void registerKeyMappings(
            RegisterKeyMappingsEvent event) {
        event.register(MotionCuesKeyMappings.TOGGLE_CUES);
        event.register(
                MotionCuesKeyMappings.OPEN_SETTINGS);
    }

    private void clientTick(ClientTickEvent.Post event) {
        MotionCuesKeyMappings.handle(
                Minecraft.getInstance());
        MotionCues.clientTick();
    }

    private void screenRenderPost(
            ScreenEvent.Render.Post event) {
        MotionCues.renderConfigPreview(
                event.getScreen(),
                event.getGuiGraphics(),
                event.getMouseX(),
                event.getMouseY(),
                event.getPartialTick());
    }
}
