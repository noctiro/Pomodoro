package org.encinet.pomodoro.ui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.storage.DatabaseManager;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;
import org.encinet.pomodoro.ui.util.ItemBuilder;

import java.util.Map;

public class PresetSelectionUI {
    public static Inventory create(Player player) {
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        Inventory inventory = Bukkit.createInventory(null, 54, languageManager.getMessage(player, "ui.title"));
        MiniMessage miniMessage = MiniMessage.miniMessage();

        // Fill empty slots with glass panes
        ItemStack filler = ItemBuilder.createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Player Stats Head
        DatabaseManager dbManager = Pomodoro.getInstance().getDatabaseManager();
        DatabaseManager.PlayerStats stats = dbManager.getPlayerStats(player.getUniqueId());

        long hours = stats.getTotalFocusSeconds() / 3600;
        long minutes = (stats.getTotalFocusSeconds() % 3600) / 60;
        long seconds = stats.getTotalFocusSeconds() % 60;
        String formattedTime = String.format("%d h %d m %d s", hours, minutes, seconds);

        ItemStack playerHead = new ItemBuilder(Material.PLAYER_HEAD)
                .displayName(miniMessage.deserialize("<italic:false><green>" + player.getName()))
                .lore(languageManager.getMessageList(player, "ui.player_stats.lore", Map.of(
                        "total_focus_time", formattedTime,
                        "total_work_sessions", String.valueOf(stats.getTotalWorkSessions())
                )))
                .build();
        SkullMeta skullMeta = (SkullMeta) playerHead.getItemMeta();
        if (skullMeta != null) {
            skullMeta.setOwningPlayer(player);
            playerHead.setItemMeta(skullMeta);
        }
        inventory.setItem(4, playerHead);

        // Add player preset items
        PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
        Map<String, PresetConfig.Preset> playerPresets = playerPresetManager.getPlayerPresets(player);
        int slot = 10;
        for (Map.Entry<String, PresetConfig.Preset> entry : playerPresets.entrySet()) {
            if (slot % 9 == 8)
                slot += 2; // Skip border
            if (slot > 44)
                break; // Out of preset area
            Material icon = Material.getMaterial(entry.getValue().icon());
            if (icon == null)
                icon = Material.PAPER;

            ItemBuilder presetItemBuilder = new ItemBuilder(icon)
                    .displayName(miniMessage.deserialize("<italic:false><aqua>" + entry.getValue().name()))
                    .lore(languageManager.getMessageList(player, "ui.player_preset.lore", Map.of(
                            "work", String.valueOf(entry.getValue().work()),
                            "break", String.valueOf(entry.getValue().breakTime()),
                            "long_break", String.valueOf(entry.getValue().longBreak()),
                            "sessions", String.valueOf(entry.getValue().sessions()))));

            if (entry.getValue().enchanted()) {
                presetItemBuilder.enchant(org.bukkit.enchantments.Enchantment.LURE, 1, true)
                        .itemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }

            ItemStack presetItem = presetItemBuilder.build();
            ItemMeta meta = presetItem.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(new NamespacedKey(Pomodoro.getInstance(), "preset_key"),
                        PersistentDataType.STRING, entry.getKey());
                presetItem.setItemMeta(meta);
            }
            inventory.setItem(slot++, presetItem);
        }

        // Add 'Create New Preset' button
        ItemStack createItem = new ItemBuilder(Material.ANVIL)
                .displayName(languageManager.getMessage(player, "ui.create_preset.name"))
                .lore(languageManager.getMessageList(player, "ui.create_preset.lore"))
                .build();
        inventory.setItem(49, createItem);

        // Add 'Pomodoro Introduction' button
        ItemStack introItem = new ItemBuilder(Material.BOOK)
                .displayName(languageManager.getMessage(player, "ui.pomodoro_introduction.name"))
                .lore(languageManager.getMessageList(player, "ui.pomodoro_introduction.lore"))
                .build();
        inventory.setItem(48, introItem);

        return inventory;
    }
}