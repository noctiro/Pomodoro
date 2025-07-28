package org.encinet.pomodoro.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import org.bukkit.event.Listener;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.ConfigManager;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PomodoroConfig;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.PomodoroManager;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;
import org.encinet.pomodoro.service.sound.SoundManager;
import org.encinet.pomodoro.ui.IconSelectionUI;
import org.encinet.pomodoro.ui.UIManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

public class PlayerListener implements Listener {
    private final Map<UUID, String> playerInputState = new HashMap<>();
    private final Map<UUID, Long> pullingBack = new HashMap<>();

    // Service instances
    private final PomodoroManager pomodoroManager = Pomodoro.getInstance().getPomodoroManager();
    private final ConfigManager configManager = Pomodoro.getInstance().getConfigManager();
    private final LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
    private final SoundManager soundManager = Pomodoro.getInstance().getSoundManager();
    private final PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
    private final UIManager uiManager = Pomodoro.getInstance().getUiManager();

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PomodoroSession session = pomodoroManager.getSession(player);

        if (session == null)
            return;

        // Move TextDisplay with the player
       if (session.getTextDisplay() != null && !event.getFrom().toVector().equals(event.getTo().toVector())) {
           PomodoroConfig config = configManager.getConfig(PomodoroConfig.class);
           session.getTextDisplay().teleport(event.getTo().clone().add(0, config.getTextDisplayOffsetY(), 0));
       }

        // Handle movement restrictions
        if (session.getState() == PomodoroState.WORK) {
            PomodoroConfig config = configManager.getConfig(PomodoroConfig.class);
            if (config.isRestrictMovement()) {
                if (!event.getFrom().toVector().equals(event.getTo().toVector())) {
                    event.setCancelled(true);
                    languageManager.sendActionBar(player, "cant_move");
                }
            } else {
                checkDistanceAndAct(player, event.getTo(), s -> player.teleport(s.getStartLocation()));
            }
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        for (Entity passenger : event.getVehicle().getPassengers()) {
            if (passenger instanceof Player player) {
                PomodoroSession session = pomodoroManager.getSession(player);
                if (session != null) {
                    if (session.getTextDisplay() != null) {
                        PomodoroConfig config = configManager.getConfig(PomodoroConfig.class);
                        session.getTextDisplay().teleport(player.getLocation().add(0, config.getTextDisplayOffsetY(), 0));
                    }
                    if (session.getState() == PomodoroState.WORK) {
                        checkDistanceAndAct(player, event.getTo(), s -> player.teleport(s.getStartLocation()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        if (pomodoroManager.isPlayerTooFar(player, event.getTo())) {
            event.setCancelled(true);
            languageManager.sendActionBar(player, "cant_teleport");
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        PomodoroSession session = pomodoroManager.getSession(player);
        if (session != null) {
            if (session.getTextDisplay() != null) {
                player.hideEntity(Pomodoro.getInstance(), session.getTextDisplay());
            }
            checkDistanceAndAct(player, player.getLocation(), s -> {
                player.teleport(s.getStartLocation());
                if (s.getTextDisplay() != null) {
                    player.showEntity(Pomodoro.getInstance(), s.getTextDisplay());
                }
            });
        }
    }

    private void checkDistanceAndAct(Player player, Location destination, Consumer<PomodoroSession> action) {
        PomodoroSession session = pomodoroManager.getSession(player);
        if (session == null || session.getState() != PomodoroState.WORK)
            return;

        UUID playerUUID = player.getUniqueId();
        if (pomodoroManager.isLocationTooFar(session, destination)) {
            long startTime = pullingBack.computeIfAbsent(playerUUID, k -> {
                languageManager.sendActionBar(player, "moved_too_far");
                soundManager.playLeaveWarningSound(player);
                return System.currentTimeMillis();
            });

            PomodoroConfig config = configManager.getConfig(PomodoroConfig.class);
            if (System.currentTimeMillis() - startTime > config.getPullBackDelay()) {
                action.accept(session);
                pullingBack.remove(playerUUID);
            } else {
                pullPlayerBack(player, session.getStartLocation(), destination);
            }
        } else {
            pullingBack.remove(playerUUID);
        }
    }

    private void pullPlayerBack(Player player, Location start, Location current) {
        if (!current.getWorld().getUID().equals(start.getWorld().getUID())) {
            player.teleport(start);
            return;
        }
        Vector direction = start.toVector().subtract(current.toVector());
        // If the direction vector's length is effectively zero, teleport the player.
        // This is the safest way to handle this case and prevent normalization errors.
        if (direction.lengthSquared() < 1.0E-12) {
            player.teleport(start);
            return;
        }
    
        Vector velocity = direction.normalize().multiply(0.5);
        player.setVelocity(velocity);
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();

        if (playerInputState.containsKey(playerUUID)) {
            event.setCancelled(true);
            String state = playerInputState.remove(playerUUID);
            String input = event.signedMessage().message();
            handleChatInput(player, state, input);
        } else {
            handleWorkChat(player, event);
        }
    }

    private void handleChatInput(Player player, String state, String input) {
        if (state.startsWith("create")) {
            handleCreatePreset(player, input);
        } else if (state.startsWith("rename:")) {
            handleRenamePreset(player, state, input);
        } else if (state.startsWith("reicon:")) {
            handleReiconPreset(player, state, input);
        }
    }

    private void handleCreatePreset(Player player, String input) {
        if (!PlayerPresetManager.isValidPresetName(input)) {
            player.sendMessage(languageManager.getMessage(player, "messages.invalid_preset_name"));
            setPlayerInputState(player.getUniqueId(), "create"); // Re-prompt
            return;
        }
        String key = input.toLowerCase().replace(" ", "_");
        PresetConfig.Preset defaultPreset = configManager.getConfig(PresetConfig.class).getTemplatePreset();
        List<Material> icons = IconSelectionUI.getCommonIcons();
        String icon = icons.get(new Random().nextInt(icons.size())).toString();
        playerPresetManager.addPlayerPreset(player, key, input, icon,
                defaultPreset.work(), defaultPreset.breakTime(), defaultPreset.longBreak(),
                defaultPreset.sessions());
        player.sendMessage(languageManager.getMessage(player, "messages.preset_created", Map.of("preset_name", input)));
        uiManager.openPresetMainUI(player);
    }

    private void handleRenamePreset(Player player, String state, String input) {
        if (!PlayerPresetManager.isValidPresetName(input)) {
            player.sendMessage(languageManager.getMessage(player, "messages.invalid_preset_name"));
            setPlayerInputState(player.getUniqueId(), state); // Re-prompt with the same state
            return;
        }
        String key = state.split(":")[1];
        String oldName = playerPresetManager.getPlayerPresets(player).get(key).name();
        playerPresetManager.renamePlayerPreset(player, key, input);
        player.sendMessage(languageManager.getMessage(player, "messages.preset_renamed",
                Map.of("old_name", oldName, "new_name", input)));
        uiManager.openPresetEditingUI(player, key);
    }

    private void handleReiconPreset(Player player, String state, String input) {
        String key = state.split(":")[1];
        PresetConfig.Preset preset = playerPresetManager.getPlayerPresets(player).get(key);
        boolean success = playerPresetManager.updatePlayerPresetIcon(player, key, input, preset.enchanted());
        if (success) {
            uiManager.openIconSelectionUI(player, key);
        } else {
            player.sendMessage(languageManager.getMessage(player, "messages.invalid_icon"));
            uiManager.openIconSelectionUI(player, key);
        }
    }

    private void handleWorkChat(Player player, AsyncChatEvent event) {
        PomodoroSession session = pomodoroManager.getSession(player);
        if (session != null && session.getState() == PomodoroState.WORK) {
            PomodoroConfig config = configManager.getConfig(PomodoroConfig.class);
            if (!config.isAllowChat()) {
                event.setCancelled(true);
                player.sendMessage(languageManager.getMessage(player, "messages.cant_chat"));
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        pomodoroManager.stop(player);
        playerPresetManager.savePlayerPresets(player.getUniqueId(), playerPresetManager.getPlayerPresets(player));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerPresetManager.loadPlayerPresets(event.getPlayer());
    }

    public void setPlayerInputState(UUID uuid, String state) {
        playerInputState.put(uuid, state);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        pomodoroManager.stop(event.getPlayer());
    }
}
