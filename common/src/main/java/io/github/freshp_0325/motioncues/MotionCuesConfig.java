package io.github.freshp_0325.motioncues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

final class MotionCuesConfig {
   boolean enabled = true;
   String visibilityMode = "MOTION_ONLY";
   float idleFadeDelaySeconds = 1.5F;
   float sensitivity = 110.0F;
   float smoothing = 0.28F;
   float maxOffset = 18.0F;
   float maxFlowSpeed = 7.0F;
   float movementThreshold = 0.003F;
   String motionModel = "VIEW_PROJECTION";
   float verticalMovementSensitivity = 5.0F;
   float depthEffectStrength = 0.8F;
   boolean cameraRotationCues = true;
   float yawSensitivity = 0.16F;
   float pitchSensitivity = 0.12F;
   float opacity = 0.72F;
   String colorMode = "ADAPTIVE_CONTRAST";
   float adaptiveLuminanceThreshold = 0.58F;
   float adaptiveTransitionWidth = 0.06F;
   String dotColor = "#E8E8E8";
   int dotRadius = 4;
   int edgeMargin = 14;
   int columnsPerSide = 2;
   int maxColumnsPerSide = 8;
   int dotsPerColumn = 6;
   int columnSpacing = 14;
   float verticalCoverage = 0.78F;
   float maxHorizontalCoverage = 0.28F;
   transient int dotColorRgb = 15263976;

   MotionCuesConfig copy() {
      MotionCuesConfig copy = new MotionCuesConfig();
      copy.enabled = this.enabled;
      copy.visibilityMode = this.visibilityMode;
      copy.idleFadeDelaySeconds = this.idleFadeDelaySeconds;
      copy.sensitivity = this.sensitivity;
      copy.smoothing = this.smoothing;
      copy.maxOffset = this.maxOffset;
      copy.maxFlowSpeed = this.maxFlowSpeed;
      copy.movementThreshold = this.movementThreshold;
      copy.motionModel = this.motionModel;
      copy.verticalMovementSensitivity = this.verticalMovementSensitivity;
      copy.depthEffectStrength = this.depthEffectStrength;
      copy.cameraRotationCues = this.cameraRotationCues;
      copy.yawSensitivity = this.yawSensitivity;
      copy.pitchSensitivity = this.pitchSensitivity;
      copy.opacity = this.opacity;
      copy.colorMode = this.colorMode;
      copy.adaptiveLuminanceThreshold = this.adaptiveLuminanceThreshold;
      copy.adaptiveTransitionWidth = this.adaptiveTransitionWidth;
      copy.dotColor = this.dotColor;
      copy.dotRadius = this.dotRadius;
      copy.edgeMargin = this.edgeMargin;
      copy.columnsPerSide = this.columnsPerSide;
      copy.maxColumnsPerSide = this.maxColumnsPerSide;
      copy.dotsPerColumn = this.dotsPerColumn;
      copy.columnSpacing = this.columnSpacing;
      copy.verticalCoverage = this.verticalCoverage;
      copy.maxHorizontalCoverage = this.maxHorizontalCoverage;
      return copy.sanitize();
   }

   static MotionCuesConfig load(Path path) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      try {
         Files.createDirectories(path.getParent());
         if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
               MotionCuesConfig loaded = (MotionCuesConfig)gson.fromJson(reader, MotionCuesConfig.class);
               if (loaded != null) {
                  loaded.sanitize();
                  loaded.save(path, gson);
                  return loaded;
               }
            }
         }

         MotionCuesConfig defaults = new MotionCuesConfig();
         defaults.sanitize();
         defaults.save(path, gson);
         return defaults;
      } catch (RuntimeException | IOException var7) {
         return new MotionCuesConfig();
      }
   }

   static MotionCuesConfig tryReload(Path path) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      try {
         MotionCuesConfig var4;
         try (Reader reader = Files.newBufferedReader(path)) {
            MotionCuesConfig loaded = (MotionCuesConfig)gson.fromJson(reader, MotionCuesConfig.class);
            var4 = loaded == null ? null : loaded.sanitize();
         }

         return var4;
      } catch (RuntimeException | IOException var7) {
         return null;
      }
   }

   MotionCuesConfig sanitize() {
      this.visibilityMode = this.visibilityMode == null ? "MOTION_ONLY" : this.visibilityMode.trim().toUpperCase();
      if (!this.visibilityMode.equals("MOTION_ONLY") && !this.visibilityMode.equals("ALWAYS_WHILE_PLAYING") && !this.visibilityMode.equals("ALWAYS")) {
         this.visibilityMode = "MOTION_ONLY";
      }

      this.idleFadeDelaySeconds = clamp(this.idleFadeDelaySeconds, 0.0F, 60.0F);
      this.sensitivity = clamp(this.sensitivity, 10.0F, 400.0F);
      this.smoothing = clamp(this.smoothing, 0.05F, 1.0F);
      this.maxOffset = clamp(this.maxOffset, 2.0F, 50.0F);
      this.maxFlowSpeed = clamp(this.maxFlowSpeed, 0.5F, 30.0F);
      this.movementThreshold = clamp(this.movementThreshold, 0.0F, 0.2F);
      this.motionModel = "VIEW_PROJECTION";
      this.verticalMovementSensitivity = clamp(this.verticalMovementSensitivity, 0.0F, 30.0F);
      this.depthEffectStrength = clamp(this.depthEffectStrength, 0.0F, 2.0F);
      this.yawSensitivity = clamp(this.yawSensitivity, 0.0F, 1.0F);
      this.pitchSensitivity = clamp(this.pitchSensitivity, 0.0F, 1.0F);
      this.opacity = clamp(this.opacity, 0.1F, 1.0F);
      this.dotRadius = Math.max(1, Math.min(this.dotRadius, 12));
      this.edgeMargin = Math.max(0, Math.min(this.edgeMargin, 80));
      this.columnsPerSide = Math.max(1, Math.min(this.columnsPerSide, 3));
      this.maxColumnsPerSide = Math.max(this.columnsPerSide, Math.min(this.maxColumnsPerSide, 20));
      this.dotsPerColumn = Math.max(1, Math.min(this.dotsPerColumn, 20));
      this.columnSpacing = Math.max(2, Math.min(this.columnSpacing, 40));
      this.verticalCoverage = clamp(this.verticalCoverage, 0.25F, 0.95F);
      this.maxHorizontalCoverage = clamp(this.maxHorizontalCoverage, 0.08F, 0.48F);
      this.colorMode = this.colorMode == null ? "ADAPTIVE_CONTRAST" : this.colorMode.trim().toUpperCase();
      if (this.colorMode.equals("INVERT")) {
         this.colorMode = "ADAPTIVE_CONTRAST";
      }

      if (!this.colorMode.equals("ADAPTIVE_CONTRAST") && !this.colorMode.equals("DIFFERENCE") && !this.colorMode.equals("FIXED")) {
         this.colorMode = "ADAPTIVE_CONTRAST";
      }

      this.adaptiveLuminanceThreshold = clamp(this.adaptiveLuminanceThreshold, 0.1F, 0.9F);
      this.adaptiveTransitionWidth = clamp(this.adaptiveTransitionWidth, 0.001F, 0.25F);
      this.dotColorRgb = parseColor(this.dotColor);
      this.dotColor = String.format("#%06X", this.dotColorRgb);
      return this;
   }

   private void save(Path path, Gson gson) throws IOException {
      try (Writer writer = Files.newBufferedWriter(path)) {
         gson.toJson(this, writer);
      }
   }

   boolean save(Path path) {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();

      try {
         Files.createDirectories(path.getParent());
         this.sanitize();
         this.save(path, gson);
         return true;
      } catch (RuntimeException | IOException var4) {
         return false;
      }
   }

   private static int parseColor(String value) {
      if (value == null) {
         return 15263976;
      } else {
         String hex = value.trim();
         if (hex.startsWith("#")) {
            hex = hex.substring(1);
         }

         if (hex.length() != 6) {
            return 15263976;
         } else {
            try {
               return Integer.parseInt(hex, 16) & 16777215;
            } catch (NumberFormatException var3) {
               return 15263976;
            }
         }
      }
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(value, max));
   }
}
