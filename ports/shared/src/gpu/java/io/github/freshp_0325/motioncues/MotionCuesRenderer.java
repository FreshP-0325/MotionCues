package io.github.freshp_0325.motioncues;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderType.CompositeState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

public final class MotionCuesRenderer {
   private static final ResourceLocation ADAPTIVE_SHADER = ResourceLocation.fromNamespaceAndPath("motion_cues", "core/adaptive_contrast_gpu");
   private static final RenderPipeline FIXED_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(ResourceLocation.fromNamespaceAndPath("motion_cues", "pipeline/fixed"))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLES)
         .build()
   );
   private static final RenderPipeline DIFFERENCE_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(ResourceLocation.fromNamespaceAndPath("motion_cues", "pipeline/difference"))
         .withVertexShader("core/position_color")
         .withFragmentShader("core/position_color")
         .withBlend(new BlendFunction(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR))
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLES)
         .build()
   );
   private static final RenderPipeline ADAPTIVE_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(ResourceLocation.fromNamespaceAndPath("motion_cues", "pipeline/adaptive"))
         .withVertexShader(ADAPTIVE_SHADER)
         .withFragmentShader(ADAPTIVE_SHADER)
         .withSampler("Sampler0")
         .withoutBlend()
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.TRIANGLES)
         .build()
   );
   private static final RenderType FIXED_TYPE = createRenderType("motion_cues_fixed", FIXED_PIPELINE);
   private static final RenderType DIFFERENCE_TYPE = createRenderType("motion_cues_difference", DIFFERENCE_PIPELINE);
   private static final RenderType ADAPTIVE_TYPE = createRenderType("motion_cues_adaptive", ADAPTIVE_PIPELINE);
   private static TextureTarget backgroundSnapshot;

   private MotionCuesRenderer() {
   }

   private static RenderPipeline register(RenderPipeline pipeline) {
      return RenderPipelines.register(pipeline);
   }

   public static void registerPipelines(Consumer<RenderPipeline> registrar) {
      registrar.accept(FIXED_PIPELINE);
      registrar.accept(DIFFERENCE_PIPELINE);
      registrar.accept(ADAPTIVE_PIPELINE);
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

   private static RenderType createRenderType(String name, RenderPipeline pipeline) {
      return RenderType.create(name, 1536, false, false, pipeline, CompositeState.builder().createCompositeState(false));
   }

   static void renderDots(GuiGraphics graphics, MotionCuesConfig config, float alpha, double phaseX, double phaseY, float depthScale, float horizontalFlow) {
      Minecraft minecraft = Minecraft.getInstance();
      int width = graphics.guiWidth();
      int height = graphics.guiHeight();
      float cueStrength = Mth.clamp(alpha * config.opacity, 0.0F, 1.0F);
      boolean adaptiveRequested = config.colorMode.equals("ADAPTIVE_CONTRAST");
      boolean adaptive = false;
      if (adaptiveRequested) {
         graphics.flush();
         adaptive = captureBackground(minecraft);
      }

      boolean difference = config.colorMode.equals("DIFFERENCE") || adaptiveRequested && !adaptive;
      RenderType renderType = adaptive ? ADAPTIVE_TYPE : (difference ? DIFFERENCE_TYPE : FIXED_TYPE);
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
      if (adaptive) {
         RenderSystem.setShaderTexture(0, backgroundSnapshot.getColorTexture());
      }

      renderType.draw(vertices.buildOrThrow());
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
         int threshold = Math.round(Mth.clamp(config.adaptiveLuminanceThreshold, 0.0F, 1.0F) * 255.0F);
         int transition = Math.max(1, Math.round(Mth.clamp(config.adaptiveTransitionWidth, 0.0F, 1.0F) * 255.0F));
         return amount << 24 | threshold << 16 | transition << 8 | 0xFF;
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

            backgroundSnapshot = new TextureTarget("Motion Cues background", main.width, main.height, false);
            backgroundSnapshot.setFilterMode(FilterMode.LINEAR);
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(main.getColorTexture(), backgroundSnapshot.getColorTexture(), 0, 0, 0, 0, 0, main.viewWidth, main.viewHeight);
         return true;
      } catch (RuntimeException var2) {
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
