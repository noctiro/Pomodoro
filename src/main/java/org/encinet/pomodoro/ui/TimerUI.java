package org.encinet.pomodoro.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.service.PomodoroManager;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;
import org.encinet.pomodoro.ui.util.ItemBuilder;

import java.util.Map;

public class TimerUI {
    public static Inventory create(Player player, PomodoroManager manager) {
        PomodoroSession session = manager.getSession(player);
        if (session == null) {
            return null;
        }
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();

        Inventory inventory = Bukkit.createInventory(null, 27, languageManager.getMessage(player, "ui.timer.title", Map.of("preset_name", session.getPreset().name())));

        // Filler
        ItemStack filler = ItemBuilder.createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Status Item
        inventory.setItem(13, createStatusItem(player, session, languageManager));

        NamespacedKey actionKey = new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action");

        // Pause/Resume Button
        inventory.setItem(11, createToggleButton(player, session, languageManager, actionKey));

        // Stop Button
        ItemStack stopItem = new ItemBuilder(Material.BARRIER)
                .displayName(languageManager.getMessage(player, "ui.timer.stop.name"))
                .lore(languageManager.getMessageList(player, "ui.timer.stop.lore"))
                .persistentData(actionKey, "stop")
                .build();
        inventory.setItem(15, stopItem);

        // Visuals Toggle Buttons
        inventory.setItem(21, createVisualsToggleButton(player, languageManager, actionKey, "bossbar"));
        inventory.setItem(23, createVisualsToggleButton(player, languageManager, actionKey, "title"));

        return inventory;
    }

    public static void update(Player player, PomodoroManager manager) {
        PomodoroSession session = manager.getSession(player);
        if (session == null) {
            return;
        }

        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        String expectedTitlePrefix = MiniMessage.miniMessage().serialize(languageManager.getMessage(player, "ui.timer.title", Map.of("preset_name", ""))).split(":")[0];

        InventoryView openInventory = player.getOpenInventory();
        if (!MiniMessage.miniMessage().serialize(openInventory.title()).startsWith(expectedTitlePrefix)) {
            return;
        }

        Inventory inventory = openInventory.getTopInventory();

        // Status Item
        inventory.setItem(13, createStatusItem(player, session, languageManager));

        NamespacedKey actionKey = new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action");

        // Pause/Resume Button
        inventory.setItem(11, createToggleButton(player, session, languageManager, actionKey));

        // Visuals Toggle Buttons
        inventory.setItem(21, createVisualsToggleButton(player, languageManager, actionKey, "bossbar"));
        inventory.setItem(23, createVisualsToggleButton(player, languageManager, actionKey, "title"));
    }

    private static ItemStack createStatusItem(Player player, PomodoroSession session, LanguageManager languageManager) {
        int minutes = session.getTimeLeft() / 60;
        int seconds = session.getTimeLeft() % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        String status = languageManager.getStatusMessage(session.getState(), player);

        return new ItemBuilder(Material.CLOCK)
                .displayName(languageManager.getMessage(player, "ui.timer.status.name", Map.of("status", status)))
                .lore(languageManager.getMessageList(player, "ui.timer.status.lore", Map.of("time", time)))
                .build();
    }

    private static ItemStack createToggleButton(Player player, PomodoroSession session, LanguageManager languageManager, NamespacedKey actionKey) {
        PomodoroState state = session.getState();
        if (state == PomodoroState.WORK || state == PomodoroState.BREAK || state == PomodoroState.LONG_BREAK) {
            return new ItemBuilder(Material.REDSTONE_BLOCK)
                    .displayName(languageManager.getMessage(player, "ui.timer.pause.name"))
                    .lore(languageManager.getMessageList(player, "ui.timer.pause.lore"))
                    .persistentData(actionKey, "toggle_pause")
                    .build();
        } else if (state == PomodoroState.PAUSED) {
            return new ItemBuilder(Material.EMERALD_BLOCK)
                    .displayName(languageManager.getMessage(player, "ui.timer.resume.name"))
                    .lore(languageManager.getMessageList(player, "ui.timer.resume.lore"))
                    .persistentData(actionKey, "toggle_pause")
                    .build();
        }
        return null;
    }

    private static ItemStack createVisualsToggleButton(Player player, LanguageManager languageManager, NamespacedKey actionKey, String type) {
        PomodoroSession session = Pomodoro.getInstance().getPomodoroManager().getSession(player);
        if (session == null) return null;

        boolean enabled;
        Material material;
        String nameKey;
        String loreKey;

        if (type.equals("bossbar")) {
            enabled = session.isBossbarEnabled();
            material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            nameKey = "ui.timer.bossbar_toggle.name";
            loreKey = enabled ? "ui.timer.bossbar_toggle.lore_enabled" : "ui.timer.bossbar_toggle.lore_disabled";
        } else {
            enabled = session.isTitleEnabled();
            material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
            nameKey = "ui.timer.title_toggle.name";
            loreKey = enabled ? "ui.timer.title_toggle.lore_enabled" : "ui.timer.title_toggle.lore_disabled";
        }

        return new ItemBuilder(material)
                .displayName(languageManager.getMessage(player, nameKey))
                .lore(languageManager.getMessageList(player, loreKey))
                .persistentData(actionKey, "toggle_" + type)
                .build();
    }
}