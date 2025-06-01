package dev.shymike.upgradedchat.client.features;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.shymike.upgradedchat.client.config.ConfigGui;

public class ModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> ConfigGui.getConfigScreen(screen).build();
    }
}