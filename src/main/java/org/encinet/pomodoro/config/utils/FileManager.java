package org.encinet.pomodoro.config.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.encinet.pomodoro.Pomodoro;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;

/**
 * Utility class to manage loading, version checking, and backing up configuration files.
 */
public class FileManager {
    private final Pomodoro plugin;

    public FileManager(Pomodoro plugin) {
        this.plugin = plugin;
    }

    /**
     * Loads a YAML file from the plugin's data folder.
     * <p>
     * This method handles the entire lifecycle of a configuration file:
     * <ul>
     *     <li>If the file doesn't exist, it's created from the plugin's resources.</li>
     *     <li>If it exists, its version is checked against the latest version.</li>
     *     <li>If the file is outdated, it's backed up, and a new version is saved.</li>
     * </ul>
     *
     * @param fileName      The name of the file (e.g., "config.yml").
     * @param versionKey    The YAML key where the file's version is stored.
     * @param latestVersion The current version of the file.
     * @return The loaded {@link YamlConfiguration} object.
     */
    public YamlConfiguration loadAndManageFile(String fileName, String versionKey, int latestVersion) {
        File file = new File(plugin.getDataFolder(), fileName);

        if (!file.exists()) {
            plugin.getLogger().info("Creating new file: " + fileName);
            plugin.saveResource(fileName, false);
        } else {
            checkVersionAndUpdate(file, fileName, versionKey, latestVersion);
        }

        return YamlConfiguration.loadConfiguration(file);
    }

    private void checkVersionAndUpdate(File file, String fileName, String versionKey, int latestVersion) {
        if (latestVersion <= 0) return; // Version checking is disabled

        YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        int userVersion = userConfig.getInt(versionKey, 0);

        if (userVersion < latestVersion) {
            backupAndReplace(file, fileName, userVersion, latestVersion);
        }
    }

    private void backupAndReplace(File oldFile, String fileName, int oldVersion, int newVersion) {
        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
        String backupFileName = String.format("%s_v%d.old", baseName, oldVersion);
        File backupFile = new File(plugin.getDataFolder(), backupFileName);

        // To avoid overwriting backups, check if it exists and add a timestamp if it does
        if (backupFile.exists()) {
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            backupFileName = String.format("%s_v%d_%s.old", baseName, oldVersion, timestamp);
            backupFile = new File(plugin.getDataFolder(), backupFileName);
        }

        try {
            Files.move(oldFile.toPath(), backupFile.toPath());
            plugin.getLogger().info(String.format(
                    "File '%s' is outdated (v%d -> v%d). Backed up to '%s'.",
                    fileName, oldVersion, newVersion, backupFile.getName()
            ));
            plugin.saveResource(fileName, true); // force replace
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to backup and update file: " + fileName, e);
        }
    }
}