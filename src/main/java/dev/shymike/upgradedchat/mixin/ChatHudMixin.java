package dev.shymike.upgradedchat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.shymike.upgradedchat.client.features.AntiSpam;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.shymike.upgradedchat.client.config.Config.Entries;

import static dev.shymike.upgradedchat.client.UpgradedChatClient.LAST_SERVER;
import static dev.shymike.upgradedchat.client.UpgradedChatClient.MC;
import static dev.shymike.upgradedchat.client.features.AntiSpam.messageCounts;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @ModifyExpressionValue(
            method = {
                    "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
                    "addToMessageHistory",
                    "addVisibleMessage"
            },
            at = @At(value = "CONSTANT", args = "intValue=100")
    )
    public int extendChatHistoryLimit(int original) {
        return Entries.CHAT_HISTORY_LIMIT.value();
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void preventChatClearOnRejoinSameServer(boolean reset, CallbackInfo ci) {
        if (!reset) { // this makes F3 + D work
            return;
        }

        ClientPlayNetworkHandler handler = MC.getNetworkHandler();
        if (handler == null) return;
        ClientConnection connection = handler.getConnection();
        String currentServer = connection.getAddress().toString();
        if (LAST_SERVER != null && LAST_SERVER.equals(currentServer)) {
            ci.cancel();
        }
        LAST_SERVER = currentServer;
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    public void spamPrevention(
            Text newText,
            MessageSignatureData messageSignatureData,
            MessageIndicator messageIndicator,
            CallbackInfo ci
    ) {
        if (!Entries.ANTI_SPAM.value()) return;

        if (messageCounts.containsKey(newText)) {
            boolean stackedMessage = AntiSpam.handleRepeatedMessage(newText);
            if (stackedMessage) {
                ci.cancel();
                return;
            } else {
                messageCounts.remove(newText);
            }
        }

        messageCounts.put(newText, 1);
        AntiSpam.removeOldEntries();
    }
}
