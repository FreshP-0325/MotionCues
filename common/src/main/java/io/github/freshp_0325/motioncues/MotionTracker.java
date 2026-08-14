package io.github.freshp_0325.motioncues;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

final class MotionTracker {
   private boolean initialized;
   private double lastX;
   private double lastY;
   private double lastZ;
   private double previousDx;
   private double previousDy;
   private double previousDz;
   private float lastYaw;
   private float lastPitch;
   private double previousPhaseX;
   private double previousPhaseY;
   private double phaseX;
   private double phaseY;
   private float flowX;
   private float flowY;
   private float previousDepthScale = 1.0F;
   private float depthScale = 1.0F;
   private float alpha;
   private int idleTicks;
   private boolean motionWasActive;

   void sample(Entity camera, float cameraYaw, float cameraPitch, MotionCuesConfig config) {
      double x = camera.getX();
      double y = camera.getY();
      double z = camera.getZ();
      if (!this.initialized) {
         this.initialized = true;
         this.lastX = x;
         this.lastY = y;
         this.lastZ = z;
         this.lastYaw = cameraYaw;
         this.lastPitch = cameraPitch;
      } else {
         double dx = x - this.lastX;
         double dy = y - this.lastY;
         double dz = z - this.lastZ;
         float yawDelta = Mth.wrapDegrees(cameraYaw - this.lastYaw);
         float pitchDelta = Mth.wrapDegrees(cameraPitch - this.lastPitch);
         this.lastX = x;
         this.lastY = y;
         this.lastZ = z;
         this.lastYaw = cameraYaw;
         this.lastPitch = cameraPitch;
         if (dx * dx + dy * dy + dz * dz > 16.0) {
            this.reset();
         } else {
            double accelX = dx - this.previousDx;
            double accelY = dy - this.previousDy;
            double accelZ = dz - this.previousDz;
            this.previousDx = dx;
            this.previousDy = dy;
            this.previousDz = dz;
            double yaw = Math.toRadians(cameraYaw);
            double pitch = Math.toRadians(cameraPitch);
            double sinYaw = Math.sin(yaw);
            double cosYaw = Math.cos(yaw);
            double sinPitch = Math.sin(pitch);
            double cosPitch = Math.cos(pitch);
            double rightX = -cosYaw;
            double rightY = 0.0;
            double rightZ = -sinYaw;
            double upX = -sinYaw * sinPitch;
            double upZ = cosYaw * sinPitch;
            double forwardX = -sinYaw * cosPitch;
            double forwardY = -sinPitch;
            double forwardZ = cosYaw * cosPitch;
            double accelRight = accelX * rightX + accelY * rightY + accelZ * rightZ;
            double accelUp = accelX * upX + accelY * cosPitch + accelZ * upZ;
            double accelForward = accelX * forwardX + accelY * forwardY + accelZ * forwardZ;
            float targetX = this.deadZone((float)(-accelRight * config.sensitivity));
            float targetY = this.deadZone((float)(accelUp * config.sensitivity));
            float targetDepthScale = Mth.clamp(1.0F + (float)accelForward * config.sensitivity * config.depthEffectStrength / 20.0F, 0.55F, 1.65F);
            if (config.cameraRotationCues) {
               targetX += this.deadZone(-yawDelta * config.yawSensitivity);
               targetY += this.deadZone(-pitchDelta * config.pitchSensitivity);
            }

            targetX = Mth.clamp(targetX, -config.maxFlowSpeed, config.maxFlowSpeed);
            targetY = Mth.clamp(targetY, -config.maxFlowSpeed, config.maxFlowSpeed);
            this.previousPhaseX = this.phaseX;
            this.previousPhaseY = this.phaseY;
            this.previousDepthScale = this.depthScale;
            this.flowX = this.flowX + (targetX - this.flowX) * config.smoothing;
            this.flowY = this.flowY + (targetY - this.flowY) * config.smoothing;
            this.depthScale = this.depthScale + (targetDepthScale - this.depthScale) * config.smoothing;
            this.phaseX = this.phaseX + this.flowX;
            this.phaseY = this.phaseY + this.flowY;
            double speedSquared = dx * dx + dy * dy + dz * dz;
            boolean rotating = config.cameraRotationCues && (Math.abs(yawDelta) > 0.01F || Math.abs(pitchDelta) > 0.01F);
            boolean alwaysVisible = !config.visibilityMode.equals("MOTION_ONLY");
            boolean motionDetected = speedSquared > config.movementThreshold * config.movementThreshold || rotating;
            float targetAlpha;
            if (alwaysVisible) {
               this.idleTicks = 0;
               targetAlpha = 1.0F;
            } else if (motionDetected) {
               this.idleTicks = 0;
               this.motionWasActive = true;
               targetAlpha = 1.0F;
            } else if (this.motionWasActive) {
               this.idleTicks++;
               int delayTicks = Math.round(config.idleFadeDelaySeconds * 20.0F);
               targetAlpha = this.idleTicks <= delayTicks ? 1.0F : 0.0F;
            } else {
               targetAlpha = 0.0F;
            }

            this.alpha = this.alpha + (targetAlpha - this.alpha) * (targetAlpha > this.alpha ? 0.22F : 0.08F);
            if (targetAlpha == 0.0F && this.alpha < 0.02F) {
               this.motionWasActive = false;
            }
         }
      }
   }

   private float deadZone(float value) {
      return Math.abs(value) < 0.12F ? 0.0F : value;
   }

   double phaseX(float partialTick) {
      return this.previousPhaseX + (this.phaseX - this.previousPhaseX) * partialTick;
   }

   double phaseY(float partialTick) {
      return this.previousPhaseY + (this.phaseY - this.previousPhaseY) * partialTick;
   }

   float horizontalFlow() {
      return Math.abs(this.flowX);
   }

   float depthScale(float partialTick) {
      return Mth.lerp(partialTick, this.previousDepthScale, this.depthScale);
   }

   float alpha() {
      return this.alpha;
   }

   void pause(MotionCuesConfig config) {
      this.previousPhaseX = this.phaseX;
      this.previousPhaseY = this.phaseY;
      this.previousDepthScale = this.depthScale;
      this.flowX = this.flowY = 0.0F;
      this.depthScale = 1.0F;
      if (config.visibilityMode.equals("MOTION_ONLY")) {
         this.alpha = 0.0F;
         this.idleTicks = 0;
         this.motionWasActive = false;
         this.initialized = false;
      } else {
         this.alpha = 1.0F;
      }
   }

   void reset() {
      this.initialized = false;
      this.previousDx = this.previousDy = this.previousDz = 0.0;
      this.previousPhaseX = this.previousPhaseY = this.phaseX = this.phaseY = this.flowX = this.flowY = this.alpha = 0.0F;
      this.previousDepthScale = this.depthScale = 1.0F;
      this.idleTicks = 0;
      this.motionWasActive = false;
   }
}
