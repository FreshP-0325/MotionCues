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
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public final class MotionCuesRenderer {
   private static final ResourceLocation ADAPTIVE_SHADER = ResourceLocation.fromNamespaceAndPath("motion_cues", "core/adaptive_contrast_gpu");
   private static final RenderPipeline FIXED_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(ResourceLocation.fromNamespaceAndPath("motion_cues", "pipeline/fixed"))
         .withVertexShader("core/gui")
         .withFragmentShader("core/gui")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.QUADS)
         .build()
   );
   private static final RenderPipeline DIFFERENCE_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(ResourceLocation.fromNamespaceAndPath("motion_cues", "pipeline/difference"))
         .withVertexShader("core/gui")
         .withFragmentShader("core/gui")
         .withBlend(new BlendFunction(SourceFactor.ONE_MINUS_DST_COLOR, DestFactor.ONE_MINUS_SRC_COLOR))
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.QUADS)
         .build()
   );
   private static final RenderPipeline ADAPTIVE_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(ResourceLocation.fromNamespaceAndPath("motion_cues", "pipeline/adaptive"))
         .withVertexShader(ADAPTIVE_SHADER)
         .withFragmentShader(ADAPTIVE_SHADER)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS)
         .build()
   );
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
   }

   static float cameraYaw(Camera camera) {
      return camera.getYRot();
   }

   static float cameraPitch(Camera camera) {
      return camera.getXRot();
   }

   static void renderDots(GuiGraphics graphics, MotionCuesConfig config, float alpha, double phaseX, double phaseY, float depthScale, float horizontalFlow) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean adaptiveRequested = config.colorMode.equals("ADAPTIVE_CONTRAST");
      boolean adaptive = adaptiveRequested && ensureBackgroundSnapshot(minecraft);
      boolean difference = config.colorMode.equals("DIFFERENCE") || adaptiveRequested && !adaptive;
      RenderPipeline pipeline = adaptive ? ADAPTIVE_PIPELINE : (difference ? DIFFERENCE_PIPELINE : FIXED_PIPELINE);
      TextureSetup textures = adaptive ? TextureSetup.singleTexture(backgroundSnapshot.getColorTextureView()) : TextureSetup.noTexture();
      graphics.guiRenderState
         .submitGuiElement(
            new MotionCuesRenderer.DotRenderState(
               pipeline,
               textures,
               new Matrix3x2f(graphics.pose()),
               config.copy(),
               Mth.clamp(alpha * config.opacity, 0.0F, 1.0F),
               phaseX,
               phaseY,
               depthScale,
               horizontalFlow,
               graphics.guiWidth(),
               graphics.guiHeight(),
               adaptive,
               difference
            )
         );
   }

   private static void drawCircle(
      VertexConsumer vertices, Matrix3x2f pose, float z, int cx, int cy, float radius, int color, boolean withCenterUv, int guiWidth, int guiHeight
   ) {
      if (color >>> 24 != 0 || (color & 16777215) != 0) {
         float centerU = (float)cx / guiWidth;
         float centerV = 1.0F - (float)cy / guiHeight;
         int segments = Math.max(16, Math.min(36, Math.round(radius * 6.0F)));

         for (int i = 0; i < segments; i++) {
            double angle1 = (Math.PI * 2) * i / segments;
            double angle2 = (Math.PI * 2) * (i + 1) / segments;
            addVertex(vertices, pose, z, cx, cy, color, withCenterUv, centerU, centerV);
            addVertex(vertices, pose, z, cx + (float)Math.cos(angle1) * radius, cy + (float)Math.sin(angle1) * radius, color, withCenterUv, centerU, centerV);
            addVertex(vertices, pose, z, cx + (float)Math.cos(angle2) * radius, cy + (float)Math.sin(angle2) * radius, color, withCenterUv, centerU, centerV);
            addVertex(vertices, pose, z, cx, cy, color, withCenterUv, centerU, centerV);
         }
      }
   }

   private static void addVertex(VertexConsumer vertices, Matrix3x2f pose, float z, float x, float y, int color, boolean withUv, float centerU, float centerV) {
      float transformedX = pose.m00() * x + pose.m10() * y + pose.m20();
      float transformedY = pose.m01() * x + pose.m11() * y + pose.m21();
      VertexConsumer vertex = vertices.addVertex(transformedX, transformedY, z);
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

   private static boolean ensureBackgroundSnapshot(Minecraft minecraft) {
      try {
         RenderTarget main = minecraft.getMainRenderTarget();
         if (backgroundSnapshot == null || backgroundSnapshot.width != main.width || backgroundSnapshot.height != main.height) {
            if (backgroundSnapshot != null) {
               backgroundSnapshot.destroyBuffers();
            }

            backgroundSnapshot = new TextureTarget("Motion Cues background", main.width, main.height, false);
            backgroundSnapshot.setFilterMode(FilterMode.LINEAR);
         }

         return true;
      } catch (RuntimeException var2) {
         return false;
      }
   }

   private static void captureBackground(Minecraft minecraft) {
      try {
         RenderTarget main = minecraft.getMainRenderTarget();
         if (backgroundSnapshot == null || backgroundSnapshot.width != main.width || backgroundSnapshot.height != main.height) {
            return;
         }

         RenderSystem.getDevice()
            .createCommandEncoder()
            .copyTextureToTexture(main.getColorTexture(), backgroundSnapshot.getColorTexture(), 0, 0, 0, 0, 0, main.width, main.height);
      } catch (RuntimeException var2) {
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

   private static final class DotRenderState implements GuiElementRenderState {
      private final RenderPipeline pipeline;
      private final TextureSetup textureSetup;
      private final Matrix3x2f pose;
      private final MotionCuesConfig config;
      private final float cueStrength;
      private final double phaseX;
      private final double phaseY;
      private final float depthScale;
      private final float horizontalFlow;
      private final int width;
      private final int height;
      private final boolean adaptive;
      private final boolean difference;
      private final ScreenRectangle bounds;

      private DotRenderState(
         RenderPipeline pipeline,
         TextureSetup textureSetup,
         Matrix3x2f pose,
         MotionCuesConfig config,
         float cueStrength,
         double phaseX,
         double phaseY,
         float depthScale,
         float horizontalFlow,
         int width,
         int height,
         boolean adaptive,
         boolean difference
      ) {
         this.pipeline = pipeline;
         this.textureSetup = textureSetup;
         this.pose = pose;
         this.config = config;
         this.cueStrength = cueStrength;
         this.phaseX = phaseX;
         this.phaseY = phaseY;
         this.depthScale = depthScale;
         this.horizontalFlow = horizontalFlow;
         this.width = width;
         this.height = height;
         this.adaptive = adaptive;
         this.difference = difference;
         this.bounds = new ScreenRectangle(0, 0, width, height);
      }

      public void buildVertices(VertexConsumer vertices, float z) {
         this.buildVerticesAtDepth(vertices, z);
      }

      public void buildVertices(VertexConsumer vertices) {
         this.buildVerticesAtDepth(vertices, 0.0F);
      }

      private void buildVerticesAtDepth(VertexConsumer vertices, float z) {
         if (this.adaptive) {
            MotionCuesRenderer.captureBackground(Minecraft.getInstance());
         }

         float radius = Math.max(0.75F, this.config.dotRadius * this.depthScale);
         int margin = this.config.edgeMargin;
         float top = this.height * (1.0F - this.config.verticalCoverage) * 0.5F;
         float usableHeight = this.height * this.config.verticalCoverage;
         float rowSpacing = usableHeight / this.config.dotsPerColumn;
         float baseDepth = this.config.columnsPerSide * this.config.columnSpacing;
         float flowRatio = Mth.clamp(this.horizontalFlow / this.config.maxFlowSpeed, 0.0F, 1.0F);
         float maxDepth = Math.min(this.width * this.config.maxHorizontalCoverage, (float)this.config.columnSpacing * this.config.maxColumnsPerSide);
         float fieldDepth = Mth.lerp(flowRatio, baseDepth, Math.max(baseDepth, maxDepth));
         int columnsToGenerate = (int)Math.ceil(fieldDepth / this.config.columnSpacing) + 1;
         long leftCycle = MotionCuesRenderer.floorCycle(this.phaseX, (float)this.config.columnSpacing);
         long rightCycle = MotionCuesRenderer.floorCycle(-this.phaseX, (float)this.config.columnSpacing);
         float leftShift = MotionCuesRenderer.wrap(this.phaseX, (float)this.config.columnSpacing);
         float rightShift = MotionCuesRenderer.wrap(-this.phaseX, (float)this.config.columnSpacing);

         for (int column = -1; column < columnsToGenerate; column++) {
            float leftDistance = column * this.config.columnSpacing + leftShift;
            float rightDistance = column * this.config.columnSpacing + rightShift;
            if (!(leftDistance > fieldDepth) || !(rightDistance > fieldDepth)) {
               int leftX = Math.round(margin + leftDistance);
               int rightX = Math.round(this.width - margin - rightDistance);
               int rowsToGenerate = this.config.dotsPerColumn + 1;

               for (int i = 0; i < rowsToGenerate; i++) {
                  if (leftDistance <= fieldDepth) {
                     float stagger = (column - leftCycle & 1L) == 1L ? rowSpacing * 0.5F : 0.0F;
                     float localY = i * rowSpacing + MotionCuesRenderer.wrap(this.phaseY + rowSpacing * 0.5 + stagger, rowSpacing);
                     if (localY <= usableHeight) {
                        int y = Math.round(top + localY);
                        float verticalFade = MotionCuesRenderer.boundaryFade(localY, usableHeight, rowSpacing * 0.65F);
                        float strength = this.cueStrength
                           * verticalFade
                           * MotionCuesRenderer.horizontalBoundaryFade(leftDistance, fieldDepth, (float)this.config.columnSpacing);
                        MotionCuesRenderer.drawCircle(
                           vertices,
                           this.pose,
                           z,
                           leftX,
                           y,
                           radius,
                           MotionCuesRenderer.colorFor(this.config, this.adaptive, this.difference, strength),
                           this.adaptive,
                           this.width,
                           this.height
                        );
                     }
                  }

                  if (rightDistance <= fieldDepth) {
                     float stagger = (column - rightCycle & 1L) == 1L ? rowSpacing * 0.5F : 0.0F;
                     float localY = i * rowSpacing + MotionCuesRenderer.wrap(this.phaseY + rowSpacing * 0.5 + stagger, rowSpacing);
                     if (localY <= usableHeight) {
                        int y = Math.round(top + localY);
                        float verticalFade = MotionCuesRenderer.boundaryFade(localY, usableHeight, rowSpacing * 0.65F);
                        float strength = this.cueStrength
                           * verticalFade
                           * MotionCuesRenderer.horizontalBoundaryFade(rightDistance, fieldDepth, (float)this.config.columnSpacing);
                        MotionCuesRenderer.drawCircle(
                           vertices,
                           this.pose,
                           z,
                           rightX,
                           y,
                           radius,
                           MotionCuesRenderer.colorFor(this.config, this.adaptive, this.difference, strength),
                           this.adaptive,
                           this.width,
                           this.height
                        );
                     }
                  }
               }
            }
         }
      }

      public RenderPipeline pipeline() {
         return this.pipeline;
      }

      public TextureSetup textureSetup() {
         return this.textureSetup;
      }

      public ScreenRectangle scissorArea() {
         return null;
      }

      public ScreenRectangle bounds() {
         return this.bounds;
      }
   }
}
