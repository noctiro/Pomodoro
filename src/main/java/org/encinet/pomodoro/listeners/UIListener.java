package org.encinet.pomodoro.listeners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;

import java.util.Map;

public class UIListener implements Listener {
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        Component titleComponent = event.getView().title();
        String title = miniMessage.serialize(titleComponent);
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();

        String uiTitle = miniMessage.serialize(languageManager.getMessage(player, "ui.title"));
        String editTitlePrefix = miniMessage
                .serialize(languageManager.getMessage(player, "ui.edit_title", Map.of("preset_name", "")))
                .split(":")[0];
        String iconTitle = miniMessage.serialize(languageManager.getMessage(player, "ui.icon_selection.title"));
        String workCompletedTitle = miniMessage.serialize(languageManager.getMessage(player, "ui.work_completed.title"));

        boolean isPomodoroUi = title.equals(uiTitle) || title.startsWith(editTitlePrefix) || title.equals(iconTitle) || title.equals(workCompletedTitle);

        if (!isPomodoroUi) {
            return;
        }

        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        if (title.equals(uiTitle)) {
            // Distinguish between PresetSelectionUI (54) and TimerUI (27)
            if (event.getClickedInventory().getSize() == 27) {
                handleTimerClick(event);
            } else {
                handlePresetSelection(event);
            }
        } else if (title.startsWith(editTitlePrefix)) {
            handlePresetEditing(event);
        } else if (title.equals(iconTitle)) {
            handleIconSelection(event);
        } else if (title.equals(workCompletedTitle)) {
            handleWorkCompletedClick(event);
        }
    }

    private void handleTimerClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        String action = clickedItem.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action"),
                PersistentDataType.STRING);

        if (action == null) {
            Pomodoro.getInstance().getSoundManager().playFailSound(player);
            return;
        }

        switch (action) {
            case "toggle_pause":
                if (Pomodoro.getInstance().getPomodoroManager().getSession(player).isPaused()) {
                    Pomodoro.getInstance().getPomodoroManager().resume(player);
                } else {
                    Pomodoro.getInstance().getPomodoroManager().pause(player);
                }
                Pomodoro.getInstance().getSoundManager().playClickSound(player);
                Pomodoro.getInstance().getUiManager().openTimerUI(player); // Refresh
                break;
            case "stop":
                Pomodoro.getInstance().getPomodoroManager().stop(player);
                player.closeInventory();
                Pomodoro.getInstance().getSoundManager().playBackSound(player);
                break;
            case "toggle_bossbar":
                toggleVisual(player, "bossbar");
                break;
            case "toggle_title":
                toggleVisual(player, "title");
                break;
        }
    }

    private void toggleVisual(Player player, String type) {
        PomodoroSession session = Pomodoro.getInstance().getPomodoroManager().getSession(player);
        if (session == null) return;

        if ("bossbar".equals(type)) {
            session.setBossbarEnabled(!session.isBossbarEnabled());
        } else if ("title".equals(type)) {
            session.setTitleEnabled(!session.isTitleEnabled());
        }

        Pomodoro.getInstance().getSoundManager().playClickSound(player);
        Pomodoro.getInstance().getUiManager().openTimerUI(player); // Refresh
    }

    private void handlePresetSelection(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();

        if (slot == 49) { // Create New Preset
            Pomodoro.getInstance().getPlayerListener().setPlayerInputState(player.getUniqueId(), "create");
            player.closeInventory();
            player.sendMessage(languageManager.getMessage(player, "messages.enter_preset_name"));
            Pomodoro.getInstance().getSoundManager().playClickSound(player);
        } else if (slot >= 0 && slot < 45) { // Player Presets
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || !clickedItem.hasItemMeta()) {
                Pomodoro.getInstance().getSoundManager().playFailSound(player);
                return;
            }
            String presetKey = clickedItem.getItemMeta().getPersistentDataContainer()
                    .get(new NamespacedKey(Pomodoro.getInstance(), "preset_key"), PersistentDataType.STRING);

            if (presetKey == null) {
                Pomodoro.getInstance().getSoundManager().playFailSound(player);
                return; // Not a preset item
            }

            PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
            PresetConfig.Preset preset = playerPresetManager.getPlayerPresets(player).get(presetKey);

            if (preset == null) {
                player.sendMessage(
                        Pomodoro.getInstance().getLanguageManager().getMessage(player, "messages.preset_not_found"));
                player.closeInventory();
                Pomodoro.getInstance().getSoundManager().playFailSound(player);
                return;
            }

            if (event.isLeftClick()) {
                Pomodoro.getInstance().getPomodoroManager().start(player, preset);
                player.closeInventory();
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
            } else if (event.isRightClick()) {
                if (event.isShiftClick()) {
                    // Delete
                    playerPresetManager.removePlayerPreset(player, presetKey);
                    player.sendMessage(languageManager.getMessage(player, "messages.preset_removed",
                            Map.of("preset_name", preset.name())));
                    Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                    Pomodoro.getInstance().getUiManager().openPresetSelectionUI(player); // Refresh
                } else {
                    // Edit
                    playerPresetManager.ensurePlayerOwnsPreset(player, presetKey);
                    Pomodoro.getInstance().getSoundManager().playClickSound(player);
                    Pomodoro.getInstance().getUiManager().openPresetEditingUI(player, presetKey);
                }
            }
        }
    }

    private void handlePresetEditing(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        var container = clickedItem.getItemMeta().getPersistentDataContainer();
        String presetKey = container.get(
                new NamespacedKey(Pomodoro.getInstance(), "preset_key"),
                PersistentDataType.STRING);
        String action = container.get(
                new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action"),
                PersistentDataType.STRING);

        if (presetKey == null || action == null) {
            Pomodoro.getInstance().getSoundManager().playFailSound(player);
            return;
        }

        PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
        int amount = event.isShiftClick() ? 10 : 1;

        switch (action) {
            case "update_work":
                if (event.isLeftClick())
                    playerPresetManager.updateWorkDuration(player, presetKey, amount);
                else if (event.isRightClick())
                    playerPresetManager.updateWorkDuration(player, presetKey, -amount);
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                break;
            case "update_break":
                if (event.isLeftClick())
                    playerPresetManager.updateBreakDuration(player, presetKey, amount);
                else if (event.isRightClick())
                    playerPresetManager.updateBreakDuration(player, presetKey, -amount);
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                break;
            case "update_long_break":
                if (event.isLeftClick())
                    playerPresetManager.updateLongBreakDuration(player, presetKey, amount);
                else if (event.isRightClick())
                    playerPresetManager.updateLongBreakDuration(player, presetKey, -amount);
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                break;
            case "update_sessions":
                if (event.isLeftClick())
                    playerPresetManager.updateSessions(player, presetKey, amount);
                else if (event.isRightClick())
                    playerPresetManager.updateSessions(player, presetKey, -amount);
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                break;
            case "rename":
                player.closeInventory();
                Pomodoro.getInstance().getPlayerListener().setPlayerInputState(player.getUniqueId(),
                        "rename:" + presetKey);
                player.sendMessage(Pomodoro.getInstance().getLanguageManager().getMessage(player,
                        "messages.enter_new_preset_name"));
                Pomodoro.getInstance().getSoundManager().playClickSound(player);
                return;
            case "reicon":
                Pomodoro.getInstance().getSoundManager().playClickSound(player);
                Pomodoro.getInstance().getUiManager().openIconSelectionUI(player, presetKey);
                return;
            case "back":
                Pomodoro.getInstance().getUiManager().openPresetSelectionUI(player);
                Pomodoro.getInstance().getSoundManager().playBackSound(player);
                return;
        }
        Pomodoro.getInstance().getUiManager().openPresetEditingUI(player, presetKey); // Refresh
    }

    private void handleWorkCompletedClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        String action = clickedItem.getItemMeta().getPersistentDataContainer().get(
                new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action"),
                PersistentDataType.STRING);

        if (action == null) {
            Pomodoro.getInstance().getSoundManager().playFailSound(player);
            return;
        }

        switch (action) {
            case "next_state":
                Pomodoro.getInstance().getPomodoroManager().nextState(player);
                player.closeInventory();
                Pomodoro.getInstance().getSoundManager().playClickSound(player);
                break;
            case "stop":
                Pomodoro.getInstance().getPomodoroManager().stop(player);
                player.closeInventory();
                Pomodoro.getInstance().getSoundManager().playBackSound(player);
                break;
        }
    }

    private void handleIconSelection(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta())
            return;

        var container = clickedItem.getItemMeta().getPersistentDataContainer();
        String presetKey = container.get(
                new NamespacedKey(Pomodoro.getInstance(), "preset_key"),
                PersistentDataType.STRING);
        String action = container.get(
                new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action"),
                PersistentDataType.STRING);

        if (presetKey == null || action == null) {
            Pomodoro.getInstance().getSoundManager().playFailSound(player);
            return;
        }

        PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
        PresetConfig.Preset preset = playerPresetManager.getPlayerPresets(player).get(presetKey);

        if (preset == null) {
            player.sendMessage(
                    Pomodoro.getInstance().getLanguageManager().getMessage(player, "messages.preset_not_found"));
            player.closeInventory();
            Pomodoro.getInstance().getSoundManager().playFailSound(player);
            return;
        }

        switch (action) {
            case "select_icon":
                String iconName = clickedItem.getType().toString();
                playerPresetManager.updatePlayerPresetIcon(player, presetKey, iconName, preset.enchanted());
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                Pomodoro.getInstance().getUiManager().openIconSelectionUI(player, presetKey); // Refresh
                break;
            case "toggle_enchant":
                playerPresetManager.updatePlayerPresetIcon(player, presetKey, preset.icon(), !preset.enchanted());
                Pomodoro.getInstance().getSoundManager().playSuccessSound(player);
                Pomodoro.getInstance().getUiManager().openIconSelectionUI(player, presetKey); // Refresh
                break;
            case "custom_icon":
                player.closeInventory();
                Pomodoro.getInstance().getPlayerListener().setPlayerInputState(player.getUniqueId(),
                        "reicon:" + presetKey);
                player.sendMessage(Pomodoro.getInstance().getLanguageManager().getMessage(player,
                        "messages.enter_new_icon_name"));
                Pomodoro.getInstance().getSoundManager().playClickSound(player);
                break;
            case "back":
                Pomodoro.getInstance().getUiManager().openPresetEditingUI(player, presetKey);
                Pomodoro.getInstance().getSoundManager().playBackSound(player);
                break;
        }
    }
}