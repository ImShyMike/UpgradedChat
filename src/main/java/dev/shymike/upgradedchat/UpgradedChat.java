package dev.shymike.upgradedchat;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class UpgradedChat implements ModInitializer {
    public static final String MOD_ID = "upgradedchat";
    public static final String MOD_NAME = "Upgraded Chat";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.error("{} is not a server side mod!", MOD_NAME);
    }
}
