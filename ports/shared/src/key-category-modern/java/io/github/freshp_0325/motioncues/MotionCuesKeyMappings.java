package io.github.freshp_0325.motioncues;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping.Category;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class MotionCuesKeyMappings {
   private static final Category CATEGORY = Category.register(Identifier.fromNamespaceAndPath("motion_cues", "main"));
   public static final KeyMapping TOGGLE_CUES = new KeyMapping("key.motion_cues.toggle", Type.KEYSYM, -1, CATEGORY);
   public static final KeyMapping OPEN_SETTINGS = new KeyMapping("key.motion_cues.open_settings", Type.KEYSYM, -1, CATEGORY);

   private MotionCuesKeyMappings() {
   }

   public static void handle(Minecraft minecraft) {
      while (TOGGLE_CUES.consumeClick()) {
         boolean enabled = MotionCues.toggleEnabled();
         if (minecraft.player != null) {
            minecraft.player.sendOverlayMessage(Component.translatable(enabled ? "motion_cues.message.enabled" : "motion_cues.message.disabled"));
         }
      }

      boolean shouldOpenSettings = false;

      while (OPEN_SETTINGS.consumeClick()) {
         shouldOpenSettings = true;
      }

      if (shouldOpenSettings && !MotionCues.isConfigPreviewScreen(minecraft.screen)) {
         minecraft.setScreen(MotionCuesConfigScreen.create(minecraft.screen));
      }
   }
}
