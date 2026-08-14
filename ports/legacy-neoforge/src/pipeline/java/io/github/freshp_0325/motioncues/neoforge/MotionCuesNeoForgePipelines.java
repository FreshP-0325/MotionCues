package io.github.freshp_0325.motioncues.neoforge;

import io.github.freshp_0325.motioncues.MotionCuesRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = "motion_cues")
public final class MotionCuesNeoForgePipelines {
   private MotionCuesNeoForgePipelines() {
   }

   @SubscribeEvent
   public static void registerPipelines(RegisterRenderPipelinesEvent event) {
      MotionCuesRenderer.registerPipelines(event::registerPipeline);
   }
}
