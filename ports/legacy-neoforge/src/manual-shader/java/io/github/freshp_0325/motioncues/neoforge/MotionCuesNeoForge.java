package io.github.freshp_0325.motioncues.neoforge;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.freshp_0325.motioncues.MotionCues;
import io.github.freshp_0325.motioncues.MotionCuesConfigScreen;
import io.github.freshp_0325.motioncues.MotionCuesKeyMappings;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = "motion_cues", dist = Dist.CLIENT)
public final class MotionCuesNeoForge {
   public MotionCuesNeoForge(IEventBus modBus, ModContainer container) {
      MotionCues.initialize(FMLPaths.CONFIGDIR.get());
      container.registerExtensionPoint(IConfigScreenFactory.class, (IConfigScreenFactory)(ignoredContainer, parent) -> MotionCuesConfigScreen.create(parent));
      modBus.addListener(this::registerGuiLayers);
      modBus.addListener(this::registerKeyMappings);
      modBus.addListener(this::registerShaders);
      NeoForge.EVENT_BUS.addListener(this::clientTick);
      NeoForge.EVENT_BUS.addListener(this::screenRenderPost);
   }

   private void registerGuiLayers(RegisterGuiLayersEvent event) {
      event.registerAbove(
         VanillaGuiLayers.HOTBAR,
         ResourceLocation.fromNamespaceAndPath("motion_cues", "cues"),
         (graphics, partialTick) -> MotionCues.render(graphics, partialTick.getGameTimeDeltaPartialTick(false))
      );
   }

   private void registerKeyMappings(RegisterKeyMappingsEvent event) {
      event.register(MotionCuesKeyMappings.TOGGLE_CUES);
      event.register(MotionCuesKeyMappings.OPEN_SETTINGS);
   }

   private void registerShaders(RegisterShadersEvent event) {
      try {
         event.registerShader(
            new ShaderInstance(
               event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath("motion_cues", "adaptive_contrast"), DefaultVertexFormat.POSITION_TEX_COLOR
            ),
            MotionCues::setAdaptiveContrastShader
         );
      } catch (IOException var3) {
         throw new RuntimeException("Unable to load Motion Cues adaptive contrast shader", var3);
      }
   }

   private void clientTick(Post event) {
      MotionCuesKeyMappings.handle(Minecraft.getInstance());
      MotionCues.clientTick();
   }

   private void screenRenderPost(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
      MotionCues.renderConfigPreview(event.getScreen(), event.getGuiGraphics(), event.getMouseX(), event.getMouseY(), event.getPartialTick());
   }
}
