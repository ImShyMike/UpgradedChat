package dev.shymike.upgradedchat.client;

import dev.shymike.upgradedchat.client.config.Config;
import dev.shymike.upgradedchat.client.features.ChatCommands;
import dev.shymike.upgradedchat.client.features.Keybinds;
import dev.shymike.upgradedchat.client.features.ReopenLastMessage;
import dev.shymike.upgradedchat.client.features.TickScheduler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;

import static dev.shymike.upgradedchat.UpgradedChat.LOGGER;
import static dev.shymike.upgradedchat.UpgradedChat.MOD_NAME;

public class UpgradedChatClient implements ClientModInitializer {
    public static MinecraftClient MC = MinecraftClient.getInstance();
    public static String LAST_SERVER = null;
    private boolean serverChecked = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing {}", MOD_NAME);

        Config.loadFromFile();

        TickScheduler.register();

        ChatCommands.register();

        Keybinds.registerKeybinds();

        ClientTickEvents.END_CLIENT_TICK.register(Keybinds::tick);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!serverChecked && client.player != null && client.getNetworkHandler() != null) {
                String currentServer = client.isInSingleplayer() ? "local" : client.getNetworkHandler().getConnection().getAddress().toString();
                if (LAST_SERVER != null && !currentServer.equals(LAST_SERVER)) {
                    client.inGameHud.getChatHud().clear(false); // clear chat
                }
                LAST_SERVER = currentServer;
                serverChecked = true;
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            serverChecked = false;
        });
    }
}
