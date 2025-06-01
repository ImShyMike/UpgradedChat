package dev.shymike.upgradedchat.client.features;

import net.minecraft.client.gui.screen.ChatScreen;

import static dev.shymike.upgradedchat.client.UpgradedChatClient.MC;

public class ReopenLastMessage {
    public static void open() {
        if (MC.currentScreen == null) {
            ChatScreen chatScreen = new ChatScreen("");
            MC.setScreen(chatScreen);
            chatScreen.setChatFromHistory(-1);
        }
    }
}
