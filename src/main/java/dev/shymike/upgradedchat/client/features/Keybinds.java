package dev.shymike.upgradedchat.client.features;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static KeyBinding reopenLastMessageKey;

    public static void registerKeybinds() {
        reopenLastMessageKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.upgradedchat.open_chat",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                "category.upgradedchat"
        ));
    }
}