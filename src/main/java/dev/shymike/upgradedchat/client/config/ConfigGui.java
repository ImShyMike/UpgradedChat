package dev.shymike.upgradedchat.client.config;

import dev.shymike.upgradedchat.client.config.Config.Entries;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ConfigGui {
    public static ConfigBuilder getConfigScreen(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.upgradedchat.title"))
                .setSavingRunnable(() -> {
                    Config.saveToFile();
                    Config.loadFromFile();
                });

        ConfigCategory category = builder.getOrCreateCategory(Text.translatable("config.upgradedchat.title"));
        addBooleanEntry(category, builder, Entries.ANTI_SPAM);
        addIntegerEntry(category, builder, Entries.ANTI_SPAM_TICKS, 2, 1200);
        addIntegerEntry(category, builder, Entries.ANTI_SPAM_RANGE, 2, 100);
        addIntegerEntry(category, builder, Entries.CHAT_HISTORY_LIMIT, 1, 65_536);
        addIntegerEntry(category, builder, Entries.CHAT_MAX_CHARACTERS, 10, 65_536);

        return builder;
    }

    private static void addBooleanEntry(ConfigCategory category, ConfigBuilder builder,
                                        Config.Entry<Boolean> entry) {
        category.addEntry(builder.entryBuilder()
                .startBooleanToggle(Text.translatable(String.format("config.upgradedchat.entry.%s.label", entry.key())), entry.value())
                .setDefaultValue(entry.defaultValue())
                .setTooltip(Text.translatable(String.format("config.upgradedchat.entry.%s.description", entry.key())))
                .setSaveConsumer(entry::setValue)
                .build());
    }

    private static void addIntegerEntry(ConfigCategory category, ConfigBuilder builder,
                                        Config.Entry<Integer> entry, int minValue, int maxValue) {
        category.addEntry(builder.entryBuilder()
                .startIntSlider(Text.translatable(String.format("config.upgradedchat.entry.%s.label", entry.key())), entry.value(), minValue, maxValue)
                .setDefaultValue(entry.defaultValue())
                .setTooltip(Text.translatable(String.format("config.upgradedchat.entry.%s.description", entry.key())))
                .setSaveConsumer(entry::setValue)
                .build());
    }
}