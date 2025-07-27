package org.encinet.pomodoro.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.encinet.pomodoro.config.annotations.ConfigValue;
import org.encinet.pomodoro.config.utils.TypeConverter;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base configuration class that provides automatic field loading from YAML config
 */
public abstract class AbstractConfig {
    protected final Logger logger;
    protected final YamlConfiguration config;

    protected AbstractConfig(YamlConfiguration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Load configuration values using reflection and ConfigValue annotations.
     * This method now uses a TypeConverter to handle complex type conversions.
     */
    protected void loadConfig() {
        Class<?> clazz = this.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            ConfigValue annotation = field.getAnnotation(ConfigValue.class);
            if (annotation == null) continue;

            String path = annotation.value();

            try {
                if (config == null) {
                    logger.warning("Configuration is null for " + clazz.getSimpleName());
                    continue;
                }

                Object rawValue = config.get(path);
                if (rawValue == null) {
                    continue; // Skip if the value is not present in the config
                }

                field.setAccessible(true);
                // Use the TypeConverter to handle complex conversions
                Object convertedValue = TypeConverter.convert(rawValue, field.getType(), field.getGenericType());

                if (convertedValue != null) {
                    field.set(this, convertedValue);
                } else {
                    logger.warning("Failed to convert value for field: " + field.getName() +
                            " at path '" + path + "'. Found value: " + rawValue);
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE,
                        "Failed to access field: " + field.getName(), e);
            } catch (Exception e) {
                logger.log(Level.SEVERE,
                        "An unexpected error occurred while loading config for field: " + field.getName(), e);
            }
        }
    }
}
