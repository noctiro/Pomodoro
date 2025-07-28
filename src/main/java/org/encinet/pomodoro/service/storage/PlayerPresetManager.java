package org.encinet.pomodoro.service.storage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.impl.PresetConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

public class PlayerPresetManager {

    private static final int MAX_PRESET_NAME_LENGTH = 32;

    private final Map<UUID, Map<String, PresetConfig.Preset>> playerPresets = new HashMap<>();
    private final DatabaseManager databaseManager;

    public PlayerPresetManager(Pomodoro plugin) {
        this.databaseManager = plugin.getDatabaseManager();
    }

    /**
     * Loads a player's presets from the database into the cache.
     *
     * @param player The player whose presets are to be loaded.
     */
    public void loadPlayerPresets(Player player) {
        Map<String, PresetConfig.Preset> presets = databaseManager.getPlayerPresets(player.getUniqueId());
        playerPresets.put(player.getUniqueId(), presets);
    }

    /**
     * Saves all of a player's presets to the database.
     * This method uses an "upsert" operation to efficiently insert or update presets.
     *
     * @param uuid    The UUID of the player.
     * @param presets A map of the player's presets to save.
     */
    public void savePlayerPresets(UUID uuid, Map<String, PresetConfig.Preset> presets) {
        databaseManager.savePlayerPresets(uuid, presets);
    }

    /**
     * Gets a combined view of a player's presets, including global presets.
     * Player-specific presets will overwrite global presets with the same key.
     *
     * @param player The player to get presets for.
     * @return A map of combined presets.
     */
    public Map<String, PresetConfig.Preset> getPlayerPresets(Player player) {
        Map<String, PresetConfig.Preset> combinedPresets = new HashMap<>();

        // Add global presets first
        PresetConfig globalPresets = Pomodoro.getInstance().getConfigManager().getConfig(PresetConfig.class);
        if (globalPresets != null) {
            combinedPresets.putAll(globalPresets.getPresets());
        }

        // Add player-specific presets, overwriting global ones if keys match
        combinedPresets.putAll(playerPresets.getOrDefault(player.getUniqueId(), new HashMap<>()));

        return combinedPresets;
    }

    /**
     * Ensures that a player has their own copy of a preset.
     * If the player is using a global preset, this method creates a personal copy for them,
     * allowing them to modify it without affecting the global version.
     *
     * @param player The player.
     * @param key    The key of the preset to check.
     * @return The player's own preset instance. Returns null if the preset key is invalid.
     */
    public PresetConfig.Preset ensurePlayerOwnsPreset(Player player, String key) {
        if (playerPresets.getOrDefault(player.getUniqueId(), new HashMap<>()).containsKey(key)) {
            return getPlayerPresets(player).get(key);
        }

        // If not owned, it must be a global preset. Let's copy it.
        PresetConfig globalPresets = Pomodoro.getInstance().getConfigManager().getConfig(PresetConfig.class);
        if (globalPresets != null && globalPresets.getPresets().containsKey(key)) {
            PresetConfig.Preset presetToCopy = globalPresets.getPresets().get(key);

            Map<String, PresetConfig.Preset> presets = playerPresets.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
            presets.put(key, presetToCopy);
            savePlayerPresets(player.getUniqueId(), presets); // Save the whole map as we are adding a new preset

            return presetToCopy;
        }

        return null; // Should not happen if the key is valid
    }
    /**
     * Adds a new preset for a player.
     *
     * @param player    The player.
     * @param key       The unique key for the new preset.
     * @param name      The display name of the preset.
     * @param icon      The icon material name for the preset.
     * @param work      The work duration in minutes.
     * @param breakTime The break duration in minutes.
     * @param longBreak The long break duration in minutes.
     * @param sessions  The number of sessions before a long break.
     */
    public void addPlayerPreset(Player player, String key, String name, String icon, int work, int breakTime, int longBreak, int sessions) {
        Map<String, PresetConfig.Preset> presets = playerPresets.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        presets.put(key, new PresetConfig.Preset(name, icon, false, work, breakTime, longBreak, sessions));
        savePlayerPresets(player.getUniqueId(), presets);
    }

    /**
     * Removes a preset from a player's personal collection.
     *
     * @param player The player.
     * @param key    The key of the preset to remove.
     */
    public void removePlayerPreset(Player player, String key) {
        Map<String, PresetConfig.Preset> presets = playerPresets.get(player.getUniqueId());
        if (presets != null && presets.containsKey(key)) {
            presets.remove(key);
            databaseManager.deletePlayerPreset(player.getUniqueId(), key);
        }
    }

    /**
     * Renames a player's preset.
     *
     * @param player  The player.
     * @param key     The key of the preset to rename.
     * @param newName The new name for the preset.
     */
    public void renamePlayerPreset(Player player, String key, String newName) {
        Map<String, PresetConfig.Preset> presets = playerPresets.get(player.getUniqueId());
        if (presets != null && presets.containsKey(key)) {
            PresetConfig.Preset oldPreset = presets.get(key);
            presets.put(key, new PresetConfig.Preset(newName, oldPreset.icon(), oldPreset.enchanted(), oldPreset.work(), oldPreset.breakTime(), oldPreset.longBreak(), oldPreset.sessions()));
            databaseManager.updatePresetField(player.getUniqueId(), key, "name", newName);
        }
    }

    /**
     * Updates the icon for a player's preset.
     *
     * @param player    The player.
     * @param key       The key of the preset to update.
     * @param newIcon   The new icon material name.
     * @param enchanted Whether the icon should be enchanted.
     * @return true if the update was successful, false if the icon material is invalid.
     */
    public boolean updatePlayerPresetIcon(Player player, String key, String newIcon, boolean enchanted) {
        Material material = Material.getMaterial(newIcon.toUpperCase());
        if (material == null || !material.isItem()) {
            return false; // Invalid icon material
        }

        Map<String, PresetConfig.Preset> presets = playerPresets.get(player.getUniqueId());
        if (presets != null && presets.containsKey(key)) {
            PresetConfig.Preset oldPreset = presets.get(key);
            presets.put(key, new PresetConfig.Preset(oldPreset.name(), newIcon, enchanted, oldPreset.work(), oldPreset.breakTime(), oldPreset.longBreak(), oldPreset.sessions()));
            databaseManager.updatePresetField(player.getUniqueId(), key, "icon", newIcon);
            databaseManager.updatePresetField(player.getUniqueId(), key, "enchanted", enchanted);
            return true;
        }
        return false;
    }

    /**
     * A functional interface for creating a new Preset instance with one integer field updated.
     */
    @FunctionalInterface
    private interface PresetFieldUpdater {
        PresetConfig.Preset apply(PresetConfig.Preset oldPreset, int newValue);
    }

    /**
     * A generic helper method to update an integer field of a preset.
     * This consolidates the logic for updating duration, sessions, etc.
     *
     * @param player       The player whose preset is being updated.
     * @param key          The key of the preset to update.
     * @param changeAmount The amount to change the value by (can be negative).
     * @param dbField      The name of the database column to update.
     * @param getter       A function to get the current value of the field from a Preset object.
     * @param updater      A function to create a new Preset object with the updated value.
     */
    private void updatePresetIntegerField(Player player, String key, int changeAmount, String dbField, ToIntFunction<PresetConfig.Preset> getter, PresetFieldUpdater updater) {
        Map<String, PresetConfig.Preset> presets = playerPresets.get(player.getUniqueId());
        if (presets != null && presets.containsKey(key)) {
            PresetConfig.Preset oldPreset = presets.get(key);
            // Ensure the new value is at least 1
            int newValue = Math.max(1, getter.applyAsInt(oldPreset) + changeAmount);
            // Update the preset in the local cache
            presets.put(key, updater.apply(oldPreset, newValue));
            // Update the specific field in the database
            databaseManager.updatePresetField(player.getUniqueId(), key, dbField, newValue);
        }
    }

    /**
     * Updates the work duration for a specific preset.
     *
     * @param player       The player.
     * @param key          The key of the preset.
     * @param changeAmount The amount to add to the work duration (in minutes).
     */
    public void updateWorkDuration(Player player, String key, int changeAmount) {
        updatePresetIntegerField(player, key, changeAmount, "work", PresetConfig.Preset::work,
                (old, val) -> new PresetConfig.Preset(old.name(), old.icon(), old.enchanted(), val, old.breakTime(), old.longBreak(), old.sessions()));
    }

    /**
     * Updates the break duration for a specific preset.
     *
     * @param player       The player.
     * @param key          The key of the preset.
     * @param changeAmount The amount to add to the break duration (in minutes).
     */
    public void updateBreakDuration(Player player, String key, int changeAmount) {
        updatePresetIntegerField(player, key, changeAmount, "break", PresetConfig.Preset::breakTime,
                (old, val) -> new PresetConfig.Preset(old.name(), old.icon(), old.enchanted(), old.work(), val, old.longBreak(), old.sessions()));
    }

    /**
     * Updates the long break duration for a specific preset.
     *
     * @param player       The player.
     * @param key          The key of the preset.
     * @param changeAmount The amount to add to the long break duration (in minutes).
     */
    public void updateLongBreakDuration(Player player, String key, int changeAmount) {
        updatePresetIntegerField(player, key, changeAmount, "long_break", PresetConfig.Preset::longBreak,
                (old, val) -> new PresetConfig.Preset(old.name(), old.icon(), old.enchanted(), old.work(), old.breakTime(), val, old.sessions()));
    }

    /**
     * Updates the number of sessions before a long break for a specific preset.
     *
     * @param player       The player.
     * @param key          The key of the preset.
     * @param changeAmount The amount to add to the number of sessions.
     */
    public void updateSessions(Player player, String key, int changeAmount) {
        updatePresetIntegerField(player, key, changeAmount, "sessions", PresetConfig.Preset::sessions,
                (old, val) -> new PresetConfig.Preset(old.name(), old.icon(), old.enchanted(), old.work(), old.breakTime(), old.longBreak(), val));
    }

    /**
     * Validates a preset name or key.
     *
     * @param name The string to validate.
     * @return true if the string is valid, false otherwise.
     */
    public static boolean isValidPresetName(String name) {
        if (name == null || name.length() > MAX_PRESET_NAME_LENGTH || name.trim().isEmpty()) {
            return false;
        }
        return true;
    }
}