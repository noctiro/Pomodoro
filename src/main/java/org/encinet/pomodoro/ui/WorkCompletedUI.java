package org.encinet.pomodoro.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.service.PomodoroManager;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;
import org.encinet.pomodoro.ui.util.ItemBuilder;

import java.util.Map;

public class WorkCompletedUI {
    public static void open(Player player) {
        PomodoroManager manager = Pomodoro.getInstance().getPomodoroManager();
        PomodoroSession session = manager.getSession(player);
        if (session == null) {
            return;
        }
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();

        Inventory inventory = Bukkit.createInventory(null, 27, languageManager.getMessage(player, "ui.work_completed.title"));

        // Filler
        ItemStack filler = ItemBuilder.createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }

        // Start Break Button
        NamespacedKey actionKey = new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action");
        ItemStack startBreakItem = new ItemBuilder(Material.EMERALD_BLOCK)
                .displayName(languageManager.getMessage(player, "ui.work_completed.start_break.name"))
                .lore(languageManager.getMessageList(player, "ui.work_completed.start_break.lore"))
                .persistentData(actionKey, "next_state")
                .build();
        inventory.setItem(11, startBreakItem);

        // Status Item
        String workDurationFormatted = languageManager.formatTime(session.getWorkDuration(), player.locale().toString());
        String totalFocusedTimeFormatted = languageManager.formatTime(session.getWorkDuration() + session.getExtraTime(), player.locale().toString());
        inventory.setItem(13, new ItemBuilder(Material.CLOCK)
                .displayName(languageManager.getMessage(player, "ui.work_completed.status.name"))
                .lore(languageManager.getMessageList(player, "ui.work_completed.status.lore",
                        Map.of("work_duration", workDurationFormatted, "total_focused_time", totalFocusedTimeFormatted)))
                .build());

        // Stop Button
        ItemStack stopItem = new ItemBuilder(Material.BARRIER)
                .displayName(languageManager.getMessage(player, "ui.timer.stop.name"))
                .lore(languageManager.getMessageList(player, "ui.timer.stop.lore"))
                .persistentData(actionKey, "stop")
                .build();
        inventory.setItem(15, stopItem);


        player.openInventory(inventory);
    }

    public static void update(Player player) {
        PomodoroManager manager = Pomodoro.getInstance().getPomodoroManager();
        PomodoroSession session = manager.getSession(player);
        if (session == null || session.getState() != PomodoroState.WORK_COMPLETED) {
            return;
        }

        InventoryView view = player.getOpenInventory();
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        Component titleComponent = languageManager.getMessage(player, "ui.work_completed.title");

        if (view.title().equals(titleComponent)) {
            Inventory inventory = view.getTopInventory();
            String workDurationFormatted = languageManager.formatTime(session.getWorkDuration(), player.locale().toString());
            String totalFocusedTimeFormatted = languageManager.formatTime(session.getWorkDuration() + session.getExtraTime(), player.locale().toString());

            ItemStack statusItem = new ItemBuilder(Material.CLOCK)
                    .displayName(languageManager.getMessage(player, "ui.work_completed.status.name"))
                    .lore(languageManager.getMessageList(player, "ui.work_completed.status.lore",
                            Map.of("work_duration", workDurationFormatted, "total_focused_time", totalFocusedTimeFormatted)))
                    .build();
            inventory.setItem(13, statusItem);
        }
    }
}