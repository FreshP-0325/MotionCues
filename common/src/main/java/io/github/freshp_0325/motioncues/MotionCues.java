package io.github.freshp_0325.motioncues;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class MotionCues {
   public static final String MOD_ID = "motion_cues";
   private static final MotionTracker TRACKER = new MotionTracker();
   private static MotionCuesConfig config = new MotionCuesConfig();
   private static Path configPath;
   private static long configModifiedTime;
   private static int configCheckTicks;
   private static Screen configPreviewScreen;
   private static MotionCues.ConfigPreviewSession configPreviewSession;
   private static long configPreviewStartedNanos;

   private MotionCues() {
   }

   public static void initialize(Path configDirectory) {
      configPath = configDirectory.resolve("motion-cues.json");
      config = MotionCuesConfig.load(configPath);
      configModifiedTime = modifiedTime(configPath);
   }

   public static void setAdaptiveContrastShader(Object shader) {
      MotionCuesRenderer.setAdaptiveContrastShader(shader);
   }

   public static boolean toggleEnabled() {
      config.enabled = !config.enabled;
      TRACKER.reset();
      saveConfig();
      return config.enabled;
   }

   public static void clientTick() {
      checkForConfigChanges();
      Minecraft minecraft = Minecraft.getInstance();
      Entity camera = minecraft.getCameraEntity();
      if (camera == null || minecraft.level == null || !config.enabled) {
         TRACKER.reset();
      } else if (minecraft.isPaused()) {
         TRACKER.pause(config);
      } else {
         Camera renderedCamera = minecraft.gameRenderer.getMainCamera();
         TRACKER.sample(camera, MotionCuesRenderer.cameraYaw(renderedCamera), MotionCuesRenderer.cameraPitch(renderedCamera), config);
      }
   }

   static MotionCuesConfig config() {
      return config;
   }

   static void saveConfig() {
      if (configPath != null && config.save(configPath)) {
         configModifiedTime = modifiedTime(configPath);
      }
   }

   static void registerConfigPreview(Screen screen, MotionCues.ConfigPreviewSession previewSession) {
      configPreviewScreen = screen;
      configPreviewSession = previewSession;
      configPreviewStartedNanos = System.nanoTime();
   }

   public static boolean isConfigPreviewScreen(Screen screen) {
      return screen != null && screen == configPreviewScreen;
   }

   private static void checkForConfigChanges() {
      if (configPath != null && ++configCheckTicks >= 20) {
         configCheckTicks = 0;
         long modified = modifiedTime(configPath);
         if (modified != 0L && modified != configModifiedTime) {
            configModifiedTime = modified;
            MotionCuesConfig reloaded = MotionCuesConfig.tryReload(configPath);
            if (reloaded != null) {
               config = reloaded;
            }
         }
      }
   }

   private static long modifiedTime(Path path) {
      try {
         return Files.getLastModifiedTime(path).toMillis();
      } catch (IOException var2) {
         return 0L;
      }
   }

   public static void render(GuiGraphics graphics, float partialTick) {
      Minecraft minecraft = Minecraft.getInstance();
      if (config.enabled && !minecraft.options.hideGui && minecraft.level != null) {
         if (!isConfigPreviewScreen(minecraft.screen)) {
            if (minecraft.screen == null || config.visibilityMode.equals("ALWAYS")) {
               float alpha = TRACKER.alpha();
               if (!(alpha < 0.02F)) {
                  MotionCuesRenderer.renderDots(
                     graphics,
                     config,
                     alpha,
                     TRACKER.phaseX(partialTick),
                     TRACKER.phaseY(partialTick),
                     TRACKER.depthScale(partialTick),
                     TRACKER.horizontalFlow()
                  );
               }
            }
         }
      }
   }

   public static void renderConfigPreview(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      MotionCues.ConfigPreviewSession session = configPreviewSession;
      if (isConfigPreviewScreen(screen) && session != null) {
         MotionCuesConfig preview = null;

         try {
            preview = session.current();
         } catch (RuntimeException var15) {
         }

         if (preview != null && preview.enabled && session.isVisible()) {
            double seconds = (System.nanoTime() - configPreviewStartedNanos) / 1.0E9;
            double phaseX = seconds * 4.0;
            double phaseY = Math.sin(seconds * 0.75) * 8.0;
            float depthScale = Mth.clamp(1.0F + (float)Math.sin(seconds * 0.9) * 0.18F * preview.depthEffectStrength, 0.55F, 1.65F);
            float horizontalFlow = preview.maxFlowSpeed * (0.35F + 0.35F * Math.abs((float)Math.sin(seconds * 0.55)));
            MotionCuesRenderer.renderDots(graphics, preview, 1.0F, phaseX, phaseY, depthScale, horizontalFlow);
         }

         session.renderControl(screen, graphics, mouseX, mouseY, partialTick);
         MotionCuesRenderer.finish(graphics);
      }
   }

   interface ConfigPreviewSession {
      MotionCuesConfig current();

      boolean isVisible();

      void renderControl(Screen var1, GuiGraphics var2, int var3, int var4, float var5);
   }
}
