package dev.shymike.upgradedchat.client.features;

import com.mojang.brigadier.Command;
import dev.shymike.upgradedchat.client.config.ConfigGui;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import static dev.shymike.upgradedchat.client.UpgradedChatClient.MC;

public class ChatCommands {
    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("upgradedchat")
                    .executes(context -> {
                        TickScheduler.scheduleForNextTick(() -> {MC.setScreen(ConfigGui.getConfigScreen(null).build());});
                        return Command.SINGLE_SUCCESS;
                    }));
        });
    }
}
