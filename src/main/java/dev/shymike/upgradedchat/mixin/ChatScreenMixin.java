package dev.shymike.upgradedchat.mixin;

import net.minecraft.client.gui.screen.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import dev.shymike.upgradedchat.client.config.Config.Entries;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @ModifyArg(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setMaxLength(I)V"
            ),
            index = 0
    )
    private int modifyChatInputLength(int original) {
        return Entries.CHAT_MAX_CHARACTERS.value();
    }
}
