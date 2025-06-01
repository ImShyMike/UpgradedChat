package dev.shymike.upgradedchat.client;

import dev.shymike.upgradedchat.client.features.Keybinds;
import dev.shymike.upgradedchat.client.features.ReopenLastMessage;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;

import static dev.shymike.upgradedchat.UpgradedChat.LOGGER;
import static dev.shymike.upgradedchat.UpgradedChat.MOD_NAME;

public class UpgradedChatClient implements ClientModInitializer {
    public static MinecraftClient MC = MinecraftClient.getInstance();
    public static String LAST_SERVER = null;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_NAME);

        Keybinds.registerKeybinds();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (Keybinds.reopenLastMessageKey.wasPressed()) {
                ReopenLastMessage.open();
            }
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (LAST_SERVER == null) {
                LAST_SERVER = handler.getConnection().getAddress().toString();
            }
        });
    }
}
