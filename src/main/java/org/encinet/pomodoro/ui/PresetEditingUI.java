package org.encinet.pomodoro.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;
import org.encinet.pomodoro.ui.util.ItemBuilder;

import java.util.Map;

public class PresetEditingUI {
    public static Inventory create(Player player, String presetKey) {
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
        PresetConfig.Preset preset = playerPresetManager.getPlayerPresets(player).get(presetKey);

        if (preset == null) {
            player.sendMessage(languageManager.getMessage(player, "messages.preset_not_found"));
            player.closeInventory();
            return null;
        }

        Inventory inventory = Bukkit.createInventory(null, 36, languageManager.getMessage(player, "ui.edit.title", Map.of("preset_name", preset.name())));
        NamespacedKey presetKeyName = new NamespacedKey(Pomodoro.getInstance(), "preset_key");
        NamespacedKey actionKey = new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action");

        // Work Duration
        ItemStack workItem = new ItemBuilder(Material.CLOCK)
                .displayName(languageManager.getMessage(player, "ui.work_duration.name"))
                .lore(languageManager.getMessageList(player, "ui.work_duration.lore", Map.of("duration", String.valueOf(preset.work()))))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "update_work")
                .build();
        inventory.setItem(10, workItem);

        // Break Duration
        ItemStack breakItem = new ItemBuilder(Material.COMPASS)
                .displayName(languageManager.getMessage(player, "ui.break_duration.name"))
                .lore(languageManager.getMessageList(player, "ui.break_duration.lore", Map.of("duration", String.valueOf(preset.breakTime()))))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "update_break")
                .build();
        inventory.setItem(12, breakItem);

        // Long Break Duration
        ItemStack longBreakItem = new ItemBuilder(Material.RECOVERY_COMPASS)
                .displayName(languageManager.getMessage(player, "ui.long_break_duration.name"))
                .lore(languageManager.getMessageList(player, "ui.long_break_duration.lore", Map.of("duration", String.valueOf(preset.longBreak()))))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "update_long_break")
                .build();
        inventory.setItem(14, longBreakItem);

        // Sessions
        ItemStack sessionsItem = new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .displayName(languageManager.getMessage(player, "ui.sessions.name"))
                .lore(languageManager.getMessageList(player, "ui.sessions.lore", Map.of("count", String.valueOf(preset.sessions()))))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "update_sessions")
                .build();
        inventory.setItem(16, sessionsItem);

        // Rename button
        ItemStack renameItem = new ItemBuilder(Material.NAME_TAG)
                .displayName(languageManager.getMessage(player, "ui.rename_preset.name"))
                .lore(languageManager.getMessageList(player, "ui.rename_preset.lore"))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "rename")
                .build();
        inventory.setItem(29, renameItem);

        // Re-icon button
        ItemStack reIconItem = new ItemBuilder(Material.ITEM_FRAME)
                .displayName(languageManager.getMessage(player, "ui.reicon_preset.name"))
                .lore(languageManager.getMessageList(player, "ui.reicon_preset.lore"))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "reicon")
                .build();
        inventory.setItem(31, reIconItem);

        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .displayName(languageManager.getMessage(player, "ui.back_button"))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "back")
                .build();
        inventory.setItem(33, backItem);

        return inventory;
    }
}