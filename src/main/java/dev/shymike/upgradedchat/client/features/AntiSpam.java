package dev.shymike.upgradedchat.client.features;

import dev.shymike.upgradedchat.mixin.ChatHudAccessor;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.client.util.ChatMessages;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import dev.shymike.upgradedchat.client.config.Config.Entries;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static dev.shymike.upgradedchat.client.UpgradedChatClient.MC;

public class AntiSpam {
    public static final Map<Text, Integer> messageCounts = new LinkedHashMap<>();

    public static boolean handleRepeatedMessage(Text repeatedText) {
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

        ChatHudLine removedLine = tryRemoveMessage(allMessages, visibleMessages, oldSuffix);
        if (removedLine == null) {
            return false;
        }

        ChatHudLine updatedLine = new ChatHudLine(
                MC.inGameHud.getTicks(),
                newSuffix,
                removedLine.signature(),
                removedLine.indicator()
        );

        accessor.invokeLogChatMessage(updatedLine);
        accessor.invokeAddVisibleMessage(updatedLine);
        accessor.invokeAddMessage(updatedLine);

        messageCounts.put(repeatedText, currentCount + 1);
        return true;
    }

    private static ChatHudLine tryRemoveMessage(
            List<ChatHudLine> messages,
            List<ChatHudLine.Visible> visibleMessages,
            Text message
    ) {
        int indexToRemove = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            ChatHudLine candidate = messages.get(i);
            if (candidate.content().equals(message)
                    && candidate.creationTick() + Entries.ANTI_SPAM_TICKS.value() > MC.inGameHud.getTicks()
            ) {
                indexToRemove = i;
                break;
            }
        }

        if (indexToRemove == -1) {
            // no message found (too old)
            return null;
        }

        ChatHudLine removedMessage = messages.remove(indexToRemove);

        // compute and remove all visible lines using logic from ChatHud.addVisibleMessage()
        ChatHud chatHud = MC.inGameHud.getChatHud();
        int wrapWidth = MathHelper.floor((double) chatHud.getWidth() / chatHud.getChatScale());
        MessageIndicator.Icon icon = removedMessage.getIcon();
        if (icon != null) {
            wrapWidth -= (icon.width + 4 + 2);
        }
        List<OrderedText> wrapped = ChatMessages.breakRenderedChatMessageLines(
                removedMessage.content(),
                wrapWidth,
                MC.textRenderer
        );
        int linesToRemove = wrapped.size();

        // find the first visual line
        int startIndex = -1;
        int targetTick = removedMessage.creationTick();
        for (int i = 0; i < visibleMessages.size(); i++) {
            ChatHudLine.Visible vis = visibleMessages.get(i);
            if (vis.addedTime() == targetTick) {
                startIndex = i;
                break;
            }
        }

        // remove the needed visible lines after the start index
        if (startIndex != -1) {
            for (int i = 0; i < linesToRemove; i++) {
                if (startIndex < visibleMessages.size()
                        && visibleMessages.get(startIndex).addedTime() == targetTick
                ) {
                    visibleMessages.remove(startIndex);
                } else {
                    break;
                }
            }
        }

        return removedMessage;
    }

    public static void removeOldEntries() {
        if (messageCounts.size() <= Entries.ANTI_SPAM_RANGE.value()) {
            return;
        }
        Iterator<Text> keyIterator = messageCounts.keySet().iterator();
        if (keyIterator.hasNext()) {
            keyIterator.next();
            keyIterator.remove();
        }
    }
}
