package io.github.freshp_0325.motioncues.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.github.freshp_0325.motioncues.MotionCuesConfigScreen;

public final class MotionCuesModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return MotionCuesConfigScreen::create;
    }
}
