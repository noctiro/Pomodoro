package org.encinet.pomodoro.config.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a configuration file, providing metadata for automatic loading.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigFile {
    /**
     * The name of the configuration file (e.g., "config.yml").
     */
    String name();

    /**
     * The current version of the configuration file.
     */
    int version() default 1;
}