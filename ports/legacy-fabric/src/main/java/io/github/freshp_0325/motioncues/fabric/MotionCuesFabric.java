package io.github.freshp_0325.motioncues.fabric;

import io.github.freshp_0325.motioncues.MotionCues;
import io.github.freshp_0325.motioncues.MotionCuesKeyMappings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class MotionCuesFabric
        implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MotionCues.initialize(
                FabricLoader.getInstance().getConfigDir());
        KeyBindingHelper.registerKeyBinding(
                MotionCuesKeyMappings.TOGGLE_CUES);
        KeyBindingHelper.registerKeyBinding(
                MotionCuesKeyMappings.OPEN_SETTINGS);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MotionCuesKeyMappings.handle(client);
            MotionCues.clientTick();
        });
        HudRenderCallback.EVENT.register(
                (graphics, tickCounter) ->
                        MotionCues.render(
                                graphics,
                                tickCounter
                                        .getGameTimeDeltaPartialTick(
                                                false)));
        ScreenEvents.AFTER_INIT.register(
                (client, screen, width, height) -> {
                    if (!MotionCues.isConfigPreviewScreen(screen)) {
                        return;
                    }
                    ScreenEvents.afterRender(screen).register(
                            (renderedScreen, graphics,
                             mouseX, mouseY, partialTick) ->
                                    MotionCues.renderConfigPreview(
                                            renderedScreen,
                                            graphics,
                                            mouseX,
                                            mouseY,
                                            partialTick));
                });
    }
}
