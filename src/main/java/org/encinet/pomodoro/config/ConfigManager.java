package org.encinet.pomodoro.config;

import com.google.common.reflect.ClassPath;
import org.bukkit.configuration.file.YamlConfiguration;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.annotations.ConfigFile;
import org.encinet.pomodoro.config.utils.FileManager;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all configuration files for the plugin using classpath scanning.
 * This manager automatically discovers, loads, and updates any class in the
 * 'impl' package annotated with @ConfigFile.
 *
 * @author Noctiro
 */
public class ConfigManager {
    public static final String VERSION_KEY = "config-version";
    private static final String CONFIG_IMPL_PACKAGE = "org.encinet.pomodoro.config.impl";

    private final Pomodoro plugin;
    private final FileManager fileManager;
    private final Map<Class<? extends AbstractConfig>, ManagedConfig> configs = new HashMap<>();
    private final Map<Class<? extends AbstractConfig>, AbstractConfig> configImplementations = new HashMap<>();

    /**
     * Represents a loaded configuration file and its metadata.
     */
    private record ManagedConfig(File file, YamlConfiguration configuration, ConfigFile metadata) {}

    public ConfigManager(Pomodoro plugin) {
        this.plugin = plugin;
        this.fileManager = new FileManager(plugin);
    }

    /**
     * Initializes and loads all defined configuration files by scanning the classpath.
     */
    public void initialize() {
        plugin.getLogger().info("Scanning for configuration files...");
        try {
            ClassPath classPath = ClassPath.from(plugin.getClass().getClassLoader());
            for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClasses(CONFIG_IMPL_PACKAGE)) {
                Class<?> clazz = classInfo.load();
                if (AbstractConfig.class.isAssignableFrom(clazz) && clazz.isAnnotationPresent(ConfigFile.class)) {
                    loadConfig((Class<? extends AbstractConfig>) clazz);
                }
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to scan classpath for configs", e);
        }

        plugin.getLogger().info("All configuration files loaded.");
        loadAllImplementations();
    }

    /**
     * Loads a specific configuration file based on its class and @ConfigFile annotation.
     *
     * @param configClass The configuration class to load.
     */
    private void loadConfig(Class<? extends AbstractConfig> configClass) {
        ConfigFile metadata = configClass.getAnnotation(ConfigFile.class);
        String fileName = metadata.name();

        YamlConfiguration finalConfig = fileManager.loadAndManageFile(fileName, VERSION_KEY, metadata.version());
        File file = new File(plugin.getDataFolder(), fileName); // For the record

        configs.put(configClass, new ManagedConfig(file, finalConfig, metadata));
        plugin.getLogger().info("Loaded configuration: " + fileName);

        // Instantiate and register the implementation
        try {
            Constructor<? extends AbstractConfig> constructor = configClass.getConstructor(YamlConfiguration.class, java.util.logging.Logger.class);
            AbstractConfig configInstance = constructor.newInstance(finalConfig, plugin.getLogger());
            configImplementations.put(configClass, configInstance);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to instantiate config class: " + configClass.getSimpleName(), e);
        }
    }

    /**
     * Reloads all configuration files from disk.
     */
    public void reloadAll() {
        plugin.getLogger().info("Reloading all configuration files...");
        configs.clear();
        configImplementations.clear();
        initialize();
        plugin.getLogger().info("All configuration files reloaded.");
    }

    private void loadAllImplementations() {
        for (AbstractConfig config : configImplementations.values()) {
            config.loadConfig();
        }
    }

    /**
     * Retrieves a specific, loaded configuration implementation in a type-safe way.
     *
     * @param configClass The class of the configuration to retrieve (e.g., PomodoroConfig.class).
     * @param <T>         The type of the configuration class.
     * @return The requested configuration instance.
     * @throws IllegalStateException if the requested config is not loaded.
     */
    public <T extends AbstractConfig> T getConfig(Class<T> configClass) {
        AbstractConfig config = configImplementations.get(configClass);
        if (config == null) {
            throw new IllegalStateException("Attempted to access unloaded config class: " + configClass.getSimpleName());
        }
        return configClass.cast(config);
    }
}