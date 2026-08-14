package io.github.freshp_0325.motioncues;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.CompiledShaderProgram;
import net.minecraft.client.renderer.CoreShaders;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30;

final class MotionCuesRenderer {
   private static final ShaderProgram ADAPTIVE_CONTRAST = new ShaderProgram(
      ResourceLocation.fromNamespaceAndPath("motion_cues", "core/adaptive_contrast"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY
   );
   private static TextureTarget backgroundSnapshot;

   private MotionCuesRenderer() {
   }

   static void setAdaptiveContrastShader(Object ignored) {
   }

   static void finish(GuiGraphics graphics) {
      graphics.flush();
   }

   static float cameraYaw(Camera camera) {
      return camera.getYRot();
   }

   static float cameraPitch(Camera camera) {
      return camera.getXRot();
   }

   static void renderDots(GuiGraphics graphics, MotionCuesConfig config, float alpha, double phaseX, double phaseY, float depthScale, float horizontalFlow) {
      Minecraft minecraft = Minecraft.getInstance();
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      float cueStrength = Mth.clamp(alpha * config.opacity, 0.0F, 1.0F);
      boolean adaptiveRequested = config.colorMode.equals("ADAPTIVE_CONTRAST");
      CompiledShaderProgram adaptiveShader = null;
      if (adaptiveRequested) {
         graphics.flush();
         if (captureBackground(minecraft)) {
            try {
               adaptiveShader = RenderSystem.setShader(ADAPTIVE_CONTRAST);
            } catch (RuntimeException var47) {
            }
         }
      }

      boolean adaptive = adaptiveShader != null;
      boolean difference = config.colorMode.equals("DIFFERENCE") || adaptiveRequested && !adaptive;
      float radius = Math.max(0.75F, config.dotRadius * depthScale);
      int margin = config.edgeMargin;
      Matrix4f matrix = graphics.pose().last().pose();
      BufferBuilder vertices = Tesselator.getInstance()
         .begin(Mode.TRIANGLES, adaptive ? DefaultVertexFormat.POSITION_TEX_COLOR : DefaultVertexFormat.POSITION_COLOR);
      float top = height * (1.0F - config.verticalCoverage) * 0.5F;
      float usableHeight = height * config.verticalCoverage;
      float rowSpacing = usableHeight / config.dotsPerColumn;
      float baseDepth = config.columnsPerSide * config.columnSpacing;
      float flowRatio = Mth.clamp(horizontalFlow / config.maxFlowSpeed, 0.0F, 1.0F);
      float maxDepth = Math.min(width * config.maxHorizontalCoverage, (float)config.columnSpacing * config.maxColumnsPerSide);
      float fieldDepth = Mth.lerp(flowRatio, baseDepth, Math.max(baseDepth, maxDepth));
      int columnsToGenerate = (int)Math.ceil(fieldDepth / config.columnSpacing) + 1;
      long leftCycle = floorCycle(phaseX, config.columnSpacing);
      long rightCycle = floorCycle(-phaseX, config.columnSpacing);
      float leftShift = wrap(phaseX, config.columnSpacing);
      float rightShift = wrap(-phaseX, config.columnSpacing);

      for (int column = -1; column < columnsToGenerate; column++) {
         float leftDistance = column * config.columnSpacing + leftShift;
         float rightDistance = column * config.columnSpacing + rightShift;
         if (!(leftDistance > fieldDepth) || !(rightDistance > fieldDepth)) {
            int leftX = Math.round(margin + leftDistance);
            int rightX = Math.round(width - margin - rightDistance);
            int rowsToGenerate = config.dotsPerColumn + 1;

            for (int i = 0; i < rowsToGenerate; i++) {
               if (leftDistance <= fieldDepth) {
                  float stagger = (column - leftCycle & 1L) == 1L ? rowSpacing * 0.5F : 0.0F;
                  float localY = i * rowSpacing + wrap(phaseY + rowSpacing * 0.5 + stagger, rowSpacing);
                  if (localY <= usableHeight) {
                     int y = Math.round(top + localY);
                     float verticalFade = boundaryFade(localY, usableHeight, rowSpacing * 0.65F);
                     float strength = cueStrength * verticalFade * horizontalBoundaryFade(leftDistance, fieldDepth, config.columnSpacing);
                     drawCircle(vertices, matrix, leftX, y, radius, colorFor(config, adaptive, difference, strength), adaptive, width, height);
                  }
               }

               if (rightDistance <= fieldDepth) {
                  float stagger = (column - rightCycle & 1L) == 1L ? rowSpacing * 0.5F : 0.0F;
                  float localY = i * rowSpacing + wrap(phaseY + rowSpacing * 0.5 + stagger, rowSpacing);
                  if (localY <= usableHeight) {
                     int y = Math.round(top + localY);
                     float verticalFade = boundaryFade(localY, usableHeight, rowSpacing * 0.65F);
                     float strength = cueStrength * verticalFade * horizontalBoundaryFade(rightDistance, fieldDepth, config.columnSpacing);
                     drawCircle(vertices, matrix, rightX, y, radius, colorFor(config, adaptive, difference, strength), adaptive, width, height);
                  }
               }
            }
         }
      }

      graphics.flush();
      RenderSystem.disableCull();
      if (adaptive) {
         RenderSystem.disableBlend();
         adaptiveShader.bindSampler("BackgroundSampler", backgroundSnapshot.getColorTextureId());
         adaptiveShader.safeGetUniform("ScreenSize").set((float)backgroundSnapshot.viewWidth, (float)backgroundSnapshot.viewHeight);
         adaptiveShader.safeGetUniform("LuminanceThreshold").set(config.adaptiveLuminanceThreshold);
         adaptiveShader.safeGetUniform("TransitionWidth").set(config.adaptiveTransitionWidth);
         RenderSystem.setShader(adaptiveShader);
      } else if (difference) {
         RenderSystem.enableBlend();
         RenderSystem.blendFunc(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR);
         RenderSystem.setShader(CoreShaders.POSITION_COLOR);
      } else {
         RenderSystem.enableBlend();
         RenderSystem.defaultBlendFunc();
         RenderSystem.setShader(CoreShaders.POSITION_COLOR);
      }

      BufferUploader.drawWithShader(vertices.buildOrThrow());
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
   }

   private static void drawCircle(
      BufferBuilder vertices, Matrix4f matrix, int cx, int cy, float radius, int color, boolean withCenterUv, int guiWidth, int guiHeight
   ) {
      if (color >>> 24 != 0 || (color & 16777215) != 0) {
         float centerU = (float)cx / guiWidth;
         float centerV = 1.0F - (float)cy / guiHeight;
         int segments = Math.max(16, Math.min(36, Math.round(radius * 6.0F)));

         for (int i = 0; i < segments; i++) {
            double angle1 = (Math.PI * 2) * i / segments;
            double angle2 = (Math.PI * 2) * (i + 1) / segments;
            addVertex(vertices, matrix, cx, cy, color, withCenterUv, centerU, centerV);
            addVertex(vertices, matrix, cx + (float)Math.cos(angle1) * radius, cy + (float)Math.sin(angle1) * radius, color, withCenterUv, centerU, centerV);
            addVertex(vertices, matrix, cx + (float)Math.cos(angle2) * radius, cy + (float)Math.sin(angle2) * radius, color, withCenterUv, centerU, centerV);
         }
      }
   }

   private static void addVertex(BufferBuilder vertices, Matrix4f matrix, float x, float y, int color, boolean withUv, float centerU, float centerV) {
      VertexConsumer vertex = vertices.addVertex(matrix, x, y, 0.0F);
      if (withUv) {
         vertex.setUv(centerU, centerV);
      }

      vertex.setColor(color);
   }

   private static int colorFor(MotionCuesConfig config, boolean adaptive, boolean difference, float strength) {
      int amount = Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 255.0F);
      if (adaptive) {
         return amount << 24 | 16777215;
      } else {
         return difference ? 0xFF000000 | amount << 16 | amount << 8 | amount : amount << 24 | config.dotColorRgb;
      }
   }

   private static boolean captureBackground(Minecraft minecraft) {
      try {
         RenderTarget main = minecraft.getMainRenderTarget();
         if (backgroundSnapshot == null || backgroundSnapshot.width != main.width || backgroundSnapshot.height != main.height) {
            if (backgroundSnapshot != null) {
               backgroundSnapshot.destroyBuffers();
            }

            backgroundSnapshot = new TextureTarget(main.width, main.height, false);
            backgroundSnapshot.setFilterMode(9729);
         }

         GL30.glBindFramebuffer(36008, main.frameBufferId);
         GL30.glBindFramebuffer(36009, backgroundSnapshot.frameBufferId);
         GL30.glBlitFramebuffer(0, 0, main.viewWidth, main.viewHeight, 0, 0, backgroundSnapshot.viewWidth, backgroundSnapshot.viewHeight, 16384, 9728);
         GL30.glBindFramebuffer(36160, main.frameBufferId);
         return true;
      } catch (RuntimeException var2) {
         minecraft.getMainRenderTarget().bindWrite(false);
         return false;
      }
   }

   private static float boundaryFade(float position, float extent, float fadeWidth) {
      float outer = Mth.clamp(position / fadeWidth, 0.0F, 1.0F);
      float inner = Mth.clamp((extent - position) / fadeWidth, 0.0F, 1.0F);
      float value = Math.min(outer, inner);
      return value * value * (3.0F - 2.0F * value);
   }

   private static float horizontalBoundaryFade(float position, float extent, float spacing) {
      float outer = Mth.clamp((position + spacing) / spacing, 0.0F, 1.0F);
      float inner = Mth.clamp((extent - position) / spacing, 0.0F, 1.0F);
      float value = Math.min(outer, inner);
      return value * value * (3.0F - 2.0F * value);
   }

   private static float wrap(double value, float cycle) {
      double result = value % cycle;
      return (float)(result < 0.0 ? result + cycle : result);
   }

   private static long floorCycle(double value, float cycle) {
      return (long)Math.floor(value / cycle);
   }
}
