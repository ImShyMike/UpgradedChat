package dev.shymike.upgradedchat.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.shymike.upgradedchat.client.UpgradedChatClient.LAST_SERVER;
import static dev.shymike.upgradedchat.client.UpgradedChatClient.MC;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @Unique private static final int MAX_TRACKED_MESSAGES = 10;
    @Unique private static final int MAX_TRACKED_TICKS = 10 * 20; // 10 seconds

    @Unique private final Map<Text, Integer> messageCounts = new LinkedHashMap<>();

    @Shadow protected abstract void addVisibleMessage(ChatHudLine message);
    @Shadow protected abstract void addMessage(ChatHudLine message);
    @Shadow protected abstract void logChatMessage(ChatHudLine message);

    @ModifyExpressionValue(
            method = {
                    "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
                    "addToMessageHistory",
                    "addVisibleMessage"
            },
            at = @At(value = "CONSTANT", args = "intValue=100")
    )
    public int extendChatHistoryLimit(int original) {
        return 16_384;
    }

    @Inject(method = "clear", at = @At("HEAD"), cancellable = true)
    private void preventChatClearOnRejoinSameServer(boolean reset, CallbackInfo ci) {
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
        if (messageCounts.containsKey(newText)) {
            boolean stackedMessage = this.handleRepeatedMessage(newText);
            if (stackedMessage) {
                ci.cancel();
                return;
            } else {
                messageCounts.remove(newText);
            }
        }

        messageCounts.put(newText, 1);
        this.removeOldEntries();
    }

    @Unique
    private boolean handleRepeatedMessage(Text repeatedText) {
        int currentCount = messageCounts.get(repeatedText);

        Text baseCopy = repeatedText.copy();
        Text oldSuffix;
        if (currentCount > 1) {
            oldSuffix = baseCopy.copy().append(
                    Text.literal(String.format(" [x%d]", currentCount))
                            .styled(s -> s.withColor(Formatting.GRAY))
            );
        } else {
            oldSuffix = repeatedText;
        }
        Text newSuffix = baseCopy.copy().append(
                Text.literal(String.format(" [x%d]", currentCount + 1))
                        .styled(s -> s.withColor(Formatting.GRAY))
        );

        ChatHudAccessor accessor = (ChatHudAccessor) MC.inGameHud.getChatHud();
        List<ChatHudLine> allMessages = accessor.getMessages();
        List<ChatHudLine.Visible> visibleMessages = accessor.getVisibleMessages();

        ChatHudLine removedLine = this.tryRemoveMessage(allMessages, visibleMessages, oldSuffix);
        if (removedLine == null) {
            return false;
        }

        ChatHudLine updatedLine = new ChatHudLine(
                MC.inGameHud.getTicks(),
                newSuffix,
                removedLine.signature(),
                removedLine.indicator()
        );

        this.logChatMessage(updatedLine);
        this.addMessage(updatedLine);
        this.addVisibleMessage(updatedLine);

        messageCounts.put(repeatedText, currentCount + 1);
        return true;
    }

    @Unique
    private ChatHudLine tryRemoveMessage(
            List<ChatHudLine> messages,
            List<ChatHudLine.Visible> visibleMessages,
            Text message
    ) {
        int indexToRemove = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatHudLine messageLine = messages.get(i);
            if (messageLine.content().equals(message)
                    && messageLine.creationTick() + MAX_TRACKED_TICKS > MC.inGameHud.getTicks()) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove == -1) {
            return null;
        }

        ChatHudLine removedMessage = messages.remove(indexToRemove);
        if (indexToRemove < visibleMessages.size()) {
            visibleMessages.remove(indexToRemove);
        }
        return removedMessage;
    }

    @Unique
    private void removeOldEntries() {
        if (messageCounts.size() <= MAX_TRACKED_MESSAGES) {
            return;
        }
        Iterator<Text> keyIterator = messageCounts.keySet().iterator();
        if (keyIterator.hasNext()) {
            keyIterator.next();
            keyIterator.remove();
        }
    }
}
