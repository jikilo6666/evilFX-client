package dev.evilfx.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.evilfx.gui.EvilScreen;

/** Opens the evilFX menu from Mod Menu's "configure" button (optional dependency). */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return EvilScreen::new;
    }
}
