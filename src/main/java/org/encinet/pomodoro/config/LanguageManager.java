package org.encinet.pomodoro.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.impl.PomodoroConfig;
import org.encinet.pomodoro.config.utils.FileManager;
import org.encinet.pomodoro.service.session.PomodoroState;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LanguageManager {
    private static final String VERSION_KEY = "config-version";
    private static final int LATEST_VERSION = 1;
    private final Pomodoro plugin;
    private final FileManager fileManager;
    private final Map<String, YamlConfiguration> languageConfigs = new HashMap<>();
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration baseMessages;

    public LanguageManager(Pomodoro plugin) {
        this.plugin = plugin;
        this.fileManager = new FileManager(plugin);
    }

    public void loadLanguages() {
        plugin.getLogger().info("Loading language files...");
        languageConfigs.clear();

        // Define the base language
        String baseLang = "en";
        String baseLangFile = "message/" + baseLang + ".yml";

        // Load and set the base language first
        loadLanguageFile(baseLangFile);
        baseMessages = languageConfigs.get(baseLang);

        // Scan and load all message files from the JAR's resources
        try (var jar = new java.util.jar.JarFile(plugin.getJarFile())) {
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                var entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("message/") && name.endsWith(".yml") && !name.equals(baseLangFile)) {
                    loadLanguageFile(name);
                }
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().severe("Could not load language files from JAR: " + e.getMessage());
        }
    }

    private void loadLanguageFile(String fileName) {
        YamlConfiguration langConfig = fileManager.loadAndManageFile(fileName, VERSION_KEY, LATEST_VERSION);
        String langCode = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.lastIndexOf('.'));
        languageConfigs.put(langCode, langConfig);
        plugin.getLogger().info("Loaded language file: " + fileName + " for locale " + langCode);
    }

    public Component getMessage(String key, String locale, TagResolver... resolvers) {
        PomodoroConfig pomodoroConfig = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);
        String messageFormat = null;

        if (pomodoroConfig.isAdaptiveLanguage() && locale != null) {
            messageFormat = getMessageForLocale(key, locale);
            if (messageFormat == null && locale.contains("_")) {
                messageFormat = getMessageForLocale(key, locale.substring(0, locale.indexOf('_')));
            }
        }

        if (messageFormat == null) {
            messageFormat = getMessageForLocale(key, pomodoroConfig.getDefaultLanguage());
        }

        if (messageFormat == null) {
            messageFormat = baseMessages.getString(key);
        }

        if (messageFormat == null) {
            messageFormat = "<yellow>Missing message for key: " + key + "</yellow>";
        }

        return miniMessage.deserialize(messageFormat, resolvers);
    }

    private String getMessageForLocale(String key, String locale) {
        if (locale == null) return null;
        YamlConfiguration langConfig = languageConfigs.get(locale);
        return langConfig != null ? langConfig.getString(key) : null;
    }

    public Component getMessage(String key, TagResolver... resolvers) {
        return getMessage(key, (String) null, resolvers);
    }

    public Component getMessage(Player player, String key, TagResolver... resolvers) {
        return getMessage(key, player.locale().toString(), resolvers);
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        return getMessage(key, resolvers);
    }

    public Component getMessage(Player player, String key, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        return getMessage(player, key, resolvers);
    }

    public void sendMessage(Player player, String key, TagResolver... resolvers) {
        player.sendMessage(getMessage(player, key, resolvers));
    }

    public void sendMessage(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(getMessage(player, key, placeholders));
    }

    public void sendMessage(Player player, String key) {
        player.sendMessage(getMessage(player, key));
    }

    public List<Component> getMessageList(String key, String locale, TagResolver... resolvers) {
        PomodoroConfig pomodoroConfig = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);
        List<String> messageFormat = null;

        if (pomodoroConfig.isAdaptiveLanguage() && locale != null) {
            messageFormat = getMessageListForLocale(key, locale);
            if (messageFormat == null && locale.contains("_")) {
                messageFormat = getMessageListForLocale(key, locale.substring(0, locale.indexOf('_')));
            }
        }

        if (messageFormat == null) {
            messageFormat = getMessageListForLocale(key, pomodoroConfig.getDefaultLanguage());
        }

        if (messageFormat == null) {
            messageFormat = baseMessages.getStringList(key);
        }

        if (messageFormat == null || messageFormat.isEmpty()) {
            return Collections.singletonList(miniMessage.deserialize("<yellow>Missing message list for key: " + key + "</yellow>"));
        }

        return messageFormat.stream()
                .map(line -> miniMessage.deserialize(line, resolvers))
                .collect(Collectors.toList());
    }

    private List<String> getMessageListForLocale(String key, String locale) {
        if (locale == null) return null;
        YamlConfiguration langConfig = languageConfigs.get(locale);
        if (langConfig != null && langConfig.isList(key)) {
            return langConfig.getStringList(key);
        }
        return null;
    }

    public List<Component> getMessageList(Player player, String key) {
        return getMessageList(key, player.locale().toString());
    }

    public List<Component> getMessageList(Player player, String key, TagResolver... resolvers) {
        return getMessageList(key, player.locale().toString(), resolvers);
    }

    public List<Component> getMessageList(Player player, String key, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        return getMessageList(player, key, resolvers);
    }

    public String getStatusMessage(PomodoroState state, Player player) {
        String key = "status." + state.toString().toLowerCase();
        return miniMessage.serialize(getMessage(player, key));
    }

    public void sendActionBar(Player player, String key, TagResolver... resolvers) {
        Component message = getMessage("actionbar." + key, player.locale().toString(), resolvers);
        player.sendActionBar(message);
    }

    public void sendActionBar(Player player, String key, Map<String, String> placeholders) {
        TagResolver[] resolvers = placeholders.entrySet().stream()
                .map(entry -> Placeholder.unparsed(entry.getKey(), entry.getValue()))
                .toArray(TagResolver[]::new);
        sendActionBar(player, key, resolvers);
    }

    public String formatTime(int totalSeconds, String locale) {
        if (totalSeconds < 0) {
            totalSeconds = 0;
        }
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        String minuteUnit = getMessageForLocale("time_units.minutes", locale);
        String secondUnit = getMessageForLocale("time_units.seconds", locale);

        if (minuteUnit == null) minuteUnit = "min";
        if (secondUnit == null) secondUnit = "s";


        if (minutes > 0 && seconds > 0) {
            return String.format("%d %s %d %s", minutes, minuteUnit, seconds, secondUnit);
        } else if (minutes > 0) {
            return String.format("%d %s", minutes, minuteUnit);
        } else {
            return String.format("%d %s", seconds, secondUnit);
        }
    }
}
