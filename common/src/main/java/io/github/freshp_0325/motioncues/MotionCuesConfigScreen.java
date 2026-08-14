package io.github.freshp_0325.motioncues;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MotionCuesConfigScreen {
   private MotionCuesConfigScreen() {
   }

   public static Screen create(Screen parent) {
      MotionCuesConfig config = MotionCues.config();
      MotionCuesConfig defaults = new MotionCuesConfig();
      MotionCuesConfigScreen.PreviewState preview = new MotionCuesConfigScreen.PreviewState(config.copy());
      ConfigBuilder builder = ConfigBuilder.create()
         .setParentScreen(parent)
         .setTitle(tr("title"))
         .setSavingRunnable(MotionCues::saveConfig)
         .setAfterInitConsumer(preview::attach);
      ConfigEntryBuilder entries = builder.entryBuilder();
      ConfigCategory general = builder.getOrCreateCategory(tr("category.general"));
      general.addEntry(
         preview.watch(
            entries.startBooleanToggle(tr("enabled"), config.enabled)
               .setDefaultValue(defaults.enabled)
               .setTooltip(new Component[]{tr("enabled.tooltip")})
               .setSaveConsumer(value -> config.enabled = value)
               .build(),
            (draft, value) -> draft.enabled = value
         )
      );
      general.addEntry(
         preview.watch(
            entries.startSelector(tr("visibility_mode"), new String[]{"MOTION_ONLY", "ALWAYS_WHILE_PLAYING", "ALWAYS"}, config.visibilityMode)
               .setDefaultValue(defaults.visibilityMode)
               .setNameProvider(value -> tr("visibility_mode." + value.toLowerCase()))
               .setTooltip(new Component[]{tr("visibility_mode.tooltip")})
               .setSaveConsumer(value -> config.visibilityMode = value)
               .build(),
            (draft, value) -> draft.visibilityMode = value
         )
      );
      general.addEntry(
         preview.watch(
            entries.startFloatField(tr("idle_fade_delay"), config.idleFadeDelaySeconds)
               .setMin(0.0F)
               .setMax(60.0F)
               .setDefaultValue(defaults.idleFadeDelaySeconds)
               .setTooltip(new Component[]{tr("idle_fade_delay.tooltip")})
               .setSaveConsumer(value -> config.idleFadeDelaySeconds = value)
               .build(),
            (draft, value) -> draft.idleFadeDelaySeconds = value
         )
      );
      ConfigCategory motion = builder.getOrCreateCategory(tr("category.motion"));
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("sensitivity"), config.sensitivity)
               .setMin(10.0F)
               .setMax(400.0F)
               .setDefaultValue(defaults.sensitivity)
               .setTooltip(new Component[]{tr("sensitivity.tooltip")})
               .setSaveConsumer(value -> config.sensitivity = value)
               .build(),
            (draft, value) -> draft.sensitivity = value
         )
      );
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("smoothing"), config.smoothing)
               .setMin(0.05F)
               .setMax(1.0F)
               .setDefaultValue(defaults.smoothing)
               .setTooltip(new Component[]{tr("smoothing.tooltip")})
               .setSaveConsumer(value -> config.smoothing = value)
               .build(),
            (draft, value) -> draft.smoothing = value
         )
      );
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("max_flow_speed"), config.maxFlowSpeed)
               .setMin(0.5F)
               .setMax(30.0F)
               .setDefaultValue(defaults.maxFlowSpeed)
               .setTooltip(new Component[]{tr("max_flow_speed.tooltip")})
               .setSaveConsumer(value -> config.maxFlowSpeed = value)
               .build(),
            (draft, value) -> draft.maxFlowSpeed = value
         )
      );
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("movement_threshold"), config.movementThreshold)
               .setMin(0.0F)
               .setMax(0.2F)
               .setDefaultValue(defaults.movementThreshold)
               .setTooltip(new Component[]{tr("movement_threshold.tooltip")})
               .setSaveConsumer(value -> config.movementThreshold = value)
               .build(),
            (draft, value) -> draft.movementThreshold = value
         )
      );
      motion.addEntry(entries.startTextDescription(tr("view_projection_description")).build());
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("depth_effect_strength"), config.depthEffectStrength)
               .setMin(0.0F)
               .setMax(2.0F)
               .setDefaultValue(defaults.depthEffectStrength)
               .setTooltip(new Component[]{tr("depth_effect_strength.tooltip")})
               .setSaveConsumer(value -> config.depthEffectStrength = value)
               .build(),
            (draft, value) -> draft.depthEffectStrength = value
         )
      );
      motion.addEntry(
         preview.watch(
            entries.startBooleanToggle(tr("camera_rotation_cues"), config.cameraRotationCues)
               .setDefaultValue(defaults.cameraRotationCues)
               .setTooltip(new Component[]{tr("camera_rotation_cues.tooltip")})
               .setSaveConsumer(value -> config.cameraRotationCues = value)
               .build(),
            (draft, value) -> draft.cameraRotationCues = value
         )
      );
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("yaw_sensitivity"), config.yawSensitivity)
               .setMin(0.0F)
               .setMax(1.0F)
               .setDefaultValue(defaults.yawSensitivity)
               .setTooltip(new Component[]{tr("yaw_sensitivity.tooltip")})
               .setSaveConsumer(value -> config.yawSensitivity = value)
               .build(),
            (draft, value) -> draft.yawSensitivity = value
         )
      );
      motion.addEntry(
         preview.watch(
            entries.startFloatField(tr("pitch_sensitivity"), config.pitchSensitivity)
               .setMin(0.0F)
               .setMax(1.0F)
               .setDefaultValue(defaults.pitchSensitivity)
               .setTooltip(new Component[]{tr("pitch_sensitivity.tooltip")})
               .setSaveConsumer(value -> config.pitchSensitivity = value)
               .build(),
            (draft, value) -> draft.pitchSensitivity = value
         )
      );
      ConfigCategory appearance = builder.getOrCreateCategory(tr("category.appearance"));
      appearance.addEntry(
         preview.watch(
            entries.startSelector(tr("color_mode"), new String[]{"ADAPTIVE_CONTRAST", "DIFFERENCE", "FIXED"}, config.colorMode)
               .setDefaultValue(defaults.colorMode)
               .setNameProvider(value -> tr("color_mode." + value.toLowerCase()))
               .setTooltip(new Component[]{tr("color_mode.tooltip")})
               .setSaveConsumer(value -> config.colorMode = value)
               .build(),
            (draft, value) -> draft.colorMode = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startFloatField(tr("adaptive_threshold"), config.adaptiveLuminanceThreshold)
               .setMin(0.1F)
               .setMax(0.9F)
               .setDefaultValue(defaults.adaptiveLuminanceThreshold)
               .setTooltip(new Component[]{tr("adaptive_threshold.tooltip")})
               .setSaveConsumer(value -> config.adaptiveLuminanceThreshold = value)
               .build(),
            (draft, value) -> draft.adaptiveLuminanceThreshold = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startFloatField(tr("adaptive_transition"), config.adaptiveTransitionWidth)
               .setMin(0.001F)
               .setMax(0.25F)
               .setDefaultValue(defaults.adaptiveTransitionWidth)
               .setTooltip(new Component[]{tr("adaptive_transition.tooltip")})
               .setSaveConsumer(value -> config.adaptiveTransitionWidth = value)
               .build(),
            (draft, value) -> draft.adaptiveTransitionWidth = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startColorField(tr("dot_color"), config.dotColorRgb)
               .setDefaultValue(defaults.dotColorRgb)
               .setTooltip(new Component[]{tr("dot_color.tooltip")})
               .setSaveConsumer(value -> config.dotColor = String.format("#%06X", value & 16777215))
               .build(),
            (draft, value) -> draft.dotColor = String.format("#%06X", value & 16777215)
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startFloatField(tr("opacity"), config.opacity)
               .setMin(0.1F)
               .setMax(1.0F)
               .setDefaultValue(defaults.opacity)
               .setTooltip(new Component[]{tr("opacity.tooltip")})
               .setSaveConsumer(value -> config.opacity = value)
               .build(),
            (draft, value) -> draft.opacity = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startIntSlider(tr("dot_radius"), config.dotRadius, 1, 12)
               .setDefaultValue(defaults.dotRadius)
               .setTooltip(new Component[]{tr("dot_radius.tooltip")})
               .setSaveConsumer(value -> config.dotRadius = value)
               .build(),
            (draft, value) -> draft.dotRadius = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startIntSlider(tr("edge_margin"), config.edgeMargin, 0, 80)
               .setDefaultValue(defaults.edgeMargin)
               .setTooltip(new Component[]{tr("edge_margin.tooltip")})
               .setSaveConsumer(value -> config.edgeMargin = value)
               .build(),
            (draft, value) -> draft.edgeMargin = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startIntSlider(tr("columns_per_side"), config.columnsPerSide, 1, 3)
               .setDefaultValue(defaults.columnsPerSide)
               .setTooltip(new Component[]{tr("columns_per_side.tooltip")})
               .setSaveConsumer(value -> config.columnsPerSide = value)
               .build(),
            (draft, value) -> draft.columnsPerSide = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startIntSlider(tr("max_columns_per_side"), config.maxColumnsPerSide, 1, 20)
               .setDefaultValue(defaults.maxColumnsPerSide)
               .setTooltip(new Component[]{tr("max_columns_per_side.tooltip")})
               .setSaveConsumer(value -> config.maxColumnsPerSide = value)
               .build(),
            (draft, value) -> draft.maxColumnsPerSide = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startIntSlider(tr("dots_per_column"), config.dotsPerColumn, 1, 20)
               .setDefaultValue(defaults.dotsPerColumn)
               .setTooltip(new Component[]{tr("dots_per_column.tooltip")})
               .setSaveConsumer(value -> config.dotsPerColumn = value)
               .build(),
            (draft, value) -> draft.dotsPerColumn = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startIntSlider(tr("column_spacing"), config.columnSpacing, 2, 40)
               .setDefaultValue(defaults.columnSpacing)
               .setTooltip(new Component[]{tr("column_spacing.tooltip")})
               .setSaveConsumer(value -> config.columnSpacing = value)
               .build(),
            (draft, value) -> draft.columnSpacing = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startFloatField(tr("vertical_coverage"), config.verticalCoverage)
               .setMin(0.25F)
               .setMax(0.95F)
               .setDefaultValue(defaults.verticalCoverage)
               .setTooltip(new Component[]{tr("vertical_coverage.tooltip")})
               .setSaveConsumer(value -> config.verticalCoverage = value)
               .build(),
            (draft, value) -> draft.verticalCoverage = value
         )
      );
      appearance.addEntry(
         preview.watch(
            entries.startFloatField(tr("horizontal_coverage"), config.maxHorizontalCoverage)
               .setMin(0.08F)
               .setMax(0.48F)
               .setDefaultValue(defaults.maxHorizontalCoverage)
               .setTooltip(new Component[]{tr("horizontal_coverage.tooltip")})
               .setSaveConsumer(value -> config.maxHorizontalCoverage = value)
               .build(),
            (draft, value) -> draft.maxHorizontalCoverage = value
         )
      );
      Screen screen = builder.build();
      MotionCues.registerConfigPreview(screen, preview);
      return screen;
   }

   private static Component tr(String key) {
      return Component.translatable("motion_cues.config." + key);
   }

   private static final class PreviewState implements MotionCues.ConfigPreviewSession {
      private final MotionCuesConfig draft;
      private final List<Runnable> readers = new ArrayList<>();
      private boolean visible;
      private Screen attachedScreen;
      private Checkbox checkbox;

      private PreviewState(MotionCuesConfig draft) {
         this.draft = draft;
      }

      private <T, E extends AbstractConfigListEntry<T>> E watch(E entry, BiConsumer<MotionCuesConfig, T> updater) {
         this.readers.add(() -> {
            try {
               updater.accept(this.draft, (T)entry.getValue());
            } catch (RuntimeException var4) {
            }
         });
         return entry;
      }

      private void attach(Screen screen) {
         if (screen instanceof AbstractConfigScreen configScreen) {
            int buttonWidth = Math.min(200, Math.max(1, (screen.width - 62) / 3));
            int cancelButtonX = screen.width / 2 - buttonWidth - 3;
            Checkbox newCheckbox = Checkbox.builder(MotionCuesConfigScreen.tr("show_config_preview"), Minecraft.getInstance().font)
               .pos(4, screen.height - 25)
               .selected(this.visible)
               .tooltip(Tooltip.create(MotionCuesConfigScreen.tr("show_config_preview.tooltip")))
               .onValueChange((ignored, selected) -> this.visible = selected)
               .build();
            newCheckbox.setX(Math.max(4, cancelButtonX - newCheckbox.getWidth() - 6));
            newCheckbox.setY(screen.height - 26 + Math.max(0, (20 - newCheckbox.getHeight()) / 2));
            configScreen.childrenL().add(newCheckbox);
            this.attachedScreen = screen;
            this.checkbox = newCheckbox;
         }
      }

      @Override
      public MotionCuesConfig current() {
         this.readers.forEach(Runnable::run);
         return this.draft.sanitize();
      }

      @Override
      public boolean isVisible() {
         return this.visible;
      }

      @Override
      public void renderControl(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
         if (screen == this.attachedScreen && this.checkbox != null) {
            this.checkbox.render(graphics, mouseX, mouseY, partialTick);
         }
      }
   }
}
