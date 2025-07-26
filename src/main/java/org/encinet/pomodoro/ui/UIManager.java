package org.encinet.pomodoro.ui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.encinet.pomodoro.Pomodoro;

public class UIManager {
    public void openPresetSelectionUI(Player player) {
        Inventory inventory = PresetSelectionUI.create(player);
        Bukkit.getScheduler().runTask(Pomodoro.getInstance(), () -> player.openInventory(inventory));
    }

    public void openTimerUI(Player player) {
        Inventory inventory = TimerUI.create(player, Pomodoro.getInstance().getPomodoroManager());
        if (inventory != null) {
            Bukkit.getScheduler().runTask(Pomodoro.getInstance(), () -> player.openInventory(inventory));
        }
    }

    public void openPresetEditingUI(Player player, String presetKey) {
        Inventory inventory = PresetEditingUI.create(player, presetKey);
        if (inventory != null) {
            Bukkit.getScheduler().runTask(Pomodoro.getInstance(), () -> player.openInventory(inventory));
        }
    }

    public void openIconSelectionUI(Player player, String presetKey) {
        Inventory inventory = IconSelectionUI.create(player, presetKey);
        if (inventory != null) {
            Bukkit.getScheduler().runTask(Pomodoro.getInstance(), () -> player.openInventory(inventory));
        }
    }
}