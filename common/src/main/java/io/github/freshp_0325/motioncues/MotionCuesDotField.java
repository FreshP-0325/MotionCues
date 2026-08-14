package io.github.freshp_0325.motioncues;

import net.minecraft.util.Mth;

final class MotionCuesDotField {
   private MotionCuesDotField() {
   }

   static void generate(
      MotionCuesConfig config,
      float cueStrength,
      double phaseX,
      double phaseY,
      float depthScale,
      float horizontalFlow,
      int width,
      int height,
      MotionCuesDotField.DotSink sink
   ) {
      float radius = Math.max(0.75F, config.dotRadius * depthScale);
      int margin = config.edgeMargin;
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

            for (int row = 0; row < rowsToGenerate; row++) {
               if (leftDistance <= fieldDepth) {
                  submitSide(config, sink, cueStrength, phaseY, rowSpacing, usableHeight, top, fieldDepth, column, leftCycle, leftDistance, leftX, row, radius);
               }

               if (rightDistance <= fieldDepth) {
                  submitSide(
                     config, sink, cueStrength, phaseY, rowSpacing, usableHeight, top, fieldDepth, column, rightCycle, rightDistance, rightX, row, radius
                  );
               }
            }
         }
      }
   }

   private static void submitSide(
      MotionCuesConfig config,
      MotionCuesDotField.DotSink sink,
      float cueStrength,
      double phaseY,
      float rowSpacing,
      float usableHeight,
      float top,
      float fieldDepth,
      int column,
      long cycle,
      float distance,
      int x,
      int row,
      float radius
   ) {
      float stagger = (column - cycle & 1L) == 1L ? rowSpacing * 0.5F : 0.0F;
      float localY = row * rowSpacing + wrap(phaseY + rowSpacing * 0.5 + stagger, rowSpacing);
      if (!(localY > usableHeight)) {
         int y = Math.round(top + localY);
         float verticalFade = boundaryFade(localY, usableHeight, rowSpacing * 0.65F);
         float strength = cueStrength * verticalFade * horizontalBoundaryFade(distance, fieldDepth, config.columnSpacing);
         sink.circle(x, y, radius, strength);
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

   @FunctionalInterface
   interface DotSink {
      void circle(int var1, int var2, float var3, float var4);
   }
}
