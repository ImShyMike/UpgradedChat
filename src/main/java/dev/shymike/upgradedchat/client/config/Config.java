package dev.shymike.upgradedchat.client.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static dev.shymike.upgradedchat.UpgradedChat.LOGGER;
import static dev.shymike.upgradedchat.UpgradedChat.MOD_ID;

public class Config {
    public static class Entries {
        public static final Entry<Boolean> ANTI_SPAM = new Entry<>("anti_spam", true);
        public static final Entry<Integer> ANTI_SPAM_TICKS = new Entry<>("anti_spam_ticks", 200);
        public static final Entry<Integer> ANTI_SPAM_RANGE = new Entry<>("anti_spam_range", 10);
        public static final Entry<Integer> CHAT_HISTORY_LIMIT = new Entry<>("chat_history_limit", 16_384);
        public static final Entry<Integer> CHAT_MAX_CHARACTERS = new Entry<>("chat_max_characters", 256);
    }

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static Path getConfigFilePath() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("upgradedchat.json");
    }

    public static void loadFromFile() {
        Path configFile = getConfigFilePath();

        if (Files.notExists(configFile)) {
            try {
                Files.createDirectories(configFile.getParent());
                Files.createFile(configFile);
                writeAllDefaultsToFile(configFile);
            } catch (IOException e) {
                LOGGER.error("[{}] Could not create new config file: {}", MOD_ID, e.toString());
                return;
            }
        }

        JsonObject savedConfig;
        try (Reader reader = Files.newBufferedReader(configFile)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (parsed != null && parsed.isJsonObject()) {
                savedConfig = parsed.getAsJsonObject();
            } else {
                savedConfig = new JsonObject();
            }
        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error("[{}] Failed to read or parse config file (using defaults): {}", MOD_ID, e.toString());
            savedConfig = new JsonObject();
        }

        List<Entry<?>> all = getAllEntries();
        for (Entry<?> ent : all) {
            ent.readFromJson(savedConfig);
        }
    }

    public static void saveToFile() {
        Path configFile = getConfigFilePath();
        JsonObject out = new JsonObject();
        for (Entry<?> ent : getAllEntries()) {
            ent.writeToJson(out, false);
        }

        try (Writer writer = Files.newBufferedWriter(configFile, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(out, writer);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to write config to disk: {}", MOD_ID, e.toString());
        }
    }

    private static void writeAllDefaultsToFile(Path configFile) {
        JsonObject defaultObj = new JsonObject();
        for (Entry<?> ent : getAllEntries()) {
            ent.writeToJson(defaultObj, true);
        }
        try (Writer w = Files.newBufferedWriter(configFile, StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(defaultObj, w);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to write defaults to config file: {}", MOD_ID, e.toString());
        }
    }

    private static List<Entry<?>> getAllEntries() {
        List<Entry<?>> result = new ArrayList<>();
        for (Field f : Entries.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers()) && Entry.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    Entry<?> ent = (Entry<?>) f.get(null);
                    result.add(ent);
                } catch (IllegalAccessException e) {
                    LOGGER.error("[{}] Reflection error when collecting config entries: {}", MOD_ID, e.toString());
                }
            }
        }
        return result;
    }

    public static class Entry<T> {
        private final String key;
        private final T defaultValue;
        private T value;

        public Entry(String key, T defaultValue) {
            this.key = key;
            this.value = defaultValue;
            this.defaultValue = defaultValue;
        }

        public String key() {
            return key;
        }

        public T value() {
            return value;
        }

        public T defaultValue() {
            return defaultValue;
        }

        public void setValue(T v) {
            this.value = v;
        }

        @SuppressWarnings("unchecked")
        public void readFromJson(JsonObject obj) {
            if (!obj.has(key)) return;

            JsonElement el = obj.get(key);
            try {
                Object parsedValue = switch (defaultValue) {
                    case Boolean ignored -> el.getAsBoolean();
                    case Integer ignored -> el.getAsInt();
                    case Long ignored -> el.getAsLong();
                    case Double ignored -> el.getAsDouble();
                    case Float ignored -> el.getAsFloat();
                    case String ignored -> el.getAsString();
                    case null, default -> {
                        LOGGER.warn("[{}] Unsupported config type for key '{}', skipping readFromJson.", MOD_ID, key);
                        yield null;
                    }
                };

                if (parsedValue != null) setValue((T) parsedValue);

            } catch (Exception e) {
                LOGGER.warn("[{}] Failed to parse key '{}' (wrong type?): {}", MOD_ID, key, e.toString());
            }
        }

        public void writeToJson(JsonObject obj, boolean defaults) {
            switch (defaults ? defaultValue : value) {
                case Boolean b -> obj.addProperty(key, b);
                case Integer i -> obj.addProperty(key, i);
                case Long l -> obj.addProperty(key, l);
                case Double v -> obj.addProperty(key, v);
                case Float v -> obj.addProperty(key, v);
                case String s -> obj.addProperty(key, s);
                case null, default ->
                        LOGGER.warn("[{}] Unsupported config type for key '{}' when writing {}to JSON.", MOD_ID, key, defaults ? "defaults " : "");
            }
        }
    }
}
