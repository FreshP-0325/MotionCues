package io.github.freshp_0325.motioncues;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalDouble;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix3x2f;

public final class MotionCuesRenderer {
   private static final boolean GUI_USES_QUAD_INDEX_BUFFER = true;
   private static final Identifier ADAPTIVE_SHADER = Identifier.fromNamespaceAndPath("motion_cues", "core/adaptive_contrast_gpu");
   private static final RenderPipeline FIXED_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("motion_cues", "pipeline/fixed"))
         .withVertexShader("core/gui")
         .withFragmentShader("core/gui")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
         .build()
   );
   private static final RenderPipeline DIFFERENCE_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("motion_cues", "pipeline/difference"))
         .withVertexShader("core/gui")
         .withFragmentShader("core/gui")
         .withColorTargetState(new ColorTargetState(BlendFunction.INVERT))
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
         .build()
   );
   private static final RenderPipeline ADAPTIVE_PIPELINE = register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.GUI_SNIPPET})
         .withLocation(Identifier.fromNamespaceAndPath("motion_cues", "pipeline/adaptive"))
         .withVertexShader(ADAPTIVE_SHADER)
         .withFragmentShader(ADAPTIVE_SHADER)
         .withSampler("Sampler0")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
         .build()
   );
   private static TextureTarget backgroundSnapshot;
   private static GpuSampler backgroundSampler;

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

   static void finish(GuiGraphicsExtractor graphics) {
   }

   static float cameraYaw(Camera camera) {
      return camera.yRot();
   }

   static float cameraPitch(Camera camera) {
      return camera.xRot();
   }

   static void renderDots(
      GuiGraphicsExtractor graphics, MotionCuesConfig config, float alpha, double phaseX, double phaseY, float depthScale, float horizontalFlow
   ) {
      Minecraft minecraft = Minecraft.getInstance();
      boolean adaptiveRequested = config.colorMode.equals("ADAPTIVE_CONTRAST");
      boolean adaptive = adaptiveRequested && ensureBackgroundSnapshot(minecraft);
      boolean difference = config.colorMode.equals("DIFFERENCE") || adaptiveRequested && !adaptive;
      RenderPipeline pipeline = adaptive ? ADAPTIVE_PIPELINE : (difference ? DIFFERENCE_PIPELINE : FIXED_PIPELINE);
      TextureSetup textures = adaptive ? TextureSetup.singleTexture(backgroundSnapshot.getColorTextureView(), backgroundSampler()) : TextureSetup.noTexture();
      graphics.guiRenderState
         .addGuiElement(
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

   private static GpuSampler backgroundSampler() {
      if (backgroundSampler == null) {
         backgroundSampler = RenderSystem.getDevice()
            .createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, 1, OptionalDouble.empty());
      }

      return backgroundSampler;
   }

   private static void drawCircle(
      VertexConsumer vertices, Matrix3x2f pose, int cx, int cy, float radius, int color, boolean withCenterUv, int guiWidth, int guiHeight
   ) {
      if (color >>> 24 != 0 || (color & 16777215) != 0) {
         float centerU = (float)cx / guiWidth;
         float centerV = 1.0F - (float)cy / guiHeight;
         int segments = Math.max(16, Math.min(36, Math.round(radius * 6.0F)));

         for (int index = 0; index < segments; index++) {
            double angle1 = (Math.PI * 2) * index / segments;
            double angle2 = (Math.PI * 2) * (index + 1) / segments;
            addVertex(vertices, pose, cx, cy, color, withCenterUv, centerU, centerV);
            addVertex(vertices, pose, cx + (float)Math.cos(angle1) * radius, cy + (float)Math.sin(angle1) * radius, color, withCenterUv, centerU, centerV);
            addVertex(vertices, pose, cx + (float)Math.cos(angle2) * radius, cy + (float)Math.sin(angle2) * radius, color, withCenterUv, centerU, centerV);
            if (GUI_USES_QUAD_INDEX_BUFFER) {
               addVertex(vertices, pose, cx, cy, color, withCenterUv, centerU, centerV);
            }
         }
      }
   }

   private static void addVertex(VertexConsumer vertices, Matrix3x2f pose, float x, float y, int color, boolean withUv, float centerU, float centerV) {
      float transformedX = pose.m00() * x + pose.m10() * y + pose.m20();
      float transformedY = pose.m01() * x + pose.m11() * y + pose.m21();
      VertexConsumer vertex = vertices.addVertex(transformedX, transformedY, 0.0F);
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

      public void buildVertices(VertexConsumer vertices) {
         if (this.adaptive) {
            MotionCuesRenderer.captureBackground(Minecraft.getInstance());
         }

         MotionCuesDotField.generate(
            this.config,
            this.cueStrength,
            this.phaseX,
            this.phaseY,
            this.depthScale,
            this.horizontalFlow,
            this.width,
            this.height,
            (x, y, radius, strength) -> MotionCuesRenderer.drawCircle(
               vertices,
               this.pose,
               x,
               y,
               radius,
               MotionCuesRenderer.colorFor(this.config, this.adaptive, this.difference, strength),
               this.adaptive,
               this.width,
               this.height
            )
         );
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
