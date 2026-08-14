package io.github.freshp_0325.motioncues.fabric;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.freshp_0325.motioncues.MotionCues;
import io.github.freshp_0325.motioncues.MotionCuesKeyMappings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterInit;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AfterRender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public final class MotionCuesFabric implements ClientModInitializer {
   public void onInitializeClient() {
      MotionCues.initialize(FabricLoader.getInstance().getConfigDir());
      KeyBindingHelper.registerKeyBinding(MotionCuesKeyMappings.TOGGLE_CUES);
      KeyBindingHelper.registerKeyBinding(MotionCuesKeyMappings.OPEN_SETTINGS);
      CoreShaderRegistrationCallback.EVENT
         .register(
            (CoreShaderRegistrationCallback)context -> context.register(
               ResourceLocation.fromNamespaceAndPath("motion_cues", "adaptive_contrast"),
               DefaultVertexFormat.POSITION_TEX_COLOR,
               MotionCues::setAdaptiveContrastShader
            )
         );
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         MotionCuesKeyMappings.handle(client);
         MotionCues.clientTick();
      });
      HudRenderCallback.EVENT
         .register((HudRenderCallback)(graphics, tickCounter) -> MotionCues.render(graphics, tickCounter.getGameTimeDeltaPartialTick(false)));
      ScreenEvents.AFTER_INIT
         .register(
            (AfterInit)(client, screen, width, height) -> {
               if (MotionCues.isConfigPreviewScreen(screen)) {
                  ScreenEvents.afterRender(screen)
                     .register(
                        (AfterRender)(renderedScreen, graphics, mouseX, mouseY, partialTick) -> MotionCues.renderConfigPreview(
                           renderedScreen, graphics, mouseX, mouseY, partialTick
                        )
                     );
               }
            }
         );
   }
}
