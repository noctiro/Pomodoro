package org.encinet.pomodoro.service;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.scheduler.BukkitRunnable;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.impl.PomodoroConfig;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;
import org.encinet.pomodoro.ui.TimerUI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PomodoroManager {
    private final Map<UUID, PomodoroSession> sessions = new HashMap<>();
    private final PomodoroVisuals visuals;

    public PomodoroManager() {
        this.visuals = new PomodoroVisuals(this);
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Map.Entry<UUID, PomodoroSession> entry : sessions.entrySet()) {
                    Player player = Bukkit.getPlayer(entry.getKey());
                    if (player != null) {
                        tick(player);
                    }
                }
            }
        }.runTaskTimer(Pomodoro.getInstance(), 0L, 20L);
    }

    public void start(Player player, PresetConfig.Preset preset) {
        stop(player); // Stop any existing session
        PomodoroSession session = new PomodoroSession(preset);
        session.setStartLocation(player.getLocation());

        // Create BossBar
        BossBar bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        bossBar.addPlayer(player);
        session.setBossBar(bossBar);

        // Create TextDisplay
        PomodoroConfig pomodoroConfig = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);

        TextDisplay textDisplay = player.getWorld().spawn(player.getLocation().add(0, pomodoroConfig.getTextDisplayOffsetY(), 0), TextDisplay.class, display -> {
            display.setBillboard(Billboard.CENTER);
            display.setTeleportDuration(0);
        });
        // Hide the TextDisplay from the player so they can interact with blocks
        player.hideEntity(Pomodoro.getInstance(), textDisplay);
        session.setTextDisplay(textDisplay);

        sessions.put(player.getUniqueId(), session);
        session.setState(PomodoroState.WORK, player); // This will trigger onEnter and all necessary updates
        visuals.update(player);
    }

    public void stop(Player player) {
        PomodoroSession session = sessions.remove(player.getUniqueId());
        if (session != null) {
            session.setState(PomodoroState.STOPPED, player);
        }
    }

    public void pause(Player player) {
        PomodoroSession session = getSession(player);
        if (session != null && (session.getState() == PomodoroState.WORK || session.getState() == PomodoroState.BREAK || session.getState() == PomodoroState.LONG_BREAK)) {
            session.setPreviousState(session.getState());
            session.setState(PomodoroState.PAUSED, player);
        }
    }

    public void resume(Player player) {
        PomodoroSession session = getSession(player);
        if (session != null && session.getState() == PomodoroState.PAUSED) {
            // Check player location before resuming
            if (isLocationTooFar(session, player.getLocation())) {
                Pomodoro.getInstance().getLanguageManager().sendActionBar(player, "actionbar.moved_too_far");
                player.teleport(session.getStartLocation());
            }
            session.setState(session.getPreviousState(), player, true);
        }
    }

    public void nextState(Player player) {
        PomodoroSession session = getSession(player);
        if (session != null && session.getState() == PomodoroState.WORK_COMPLETED) {
            if (session.getCurrentSession() >= session.getSessions()) {
                session.setState(PomodoroState.LONG_BREAK, player);
            } else {
                session.setCurrentSession(session.getCurrentSession() + 1);
                session.setState(PomodoroState.BREAK, player);
            }
        }
    }

    private void tick(Player player) {
        PomodoroSession session = getSession(player);
        if (session == null) {
            return;
        }

        session.tick(player);

        // Teleport TextDisplay
        TextDisplay textDisplay = session.getTextDisplay();
        if (textDisplay != null) {
            PomodoroConfig config = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);
            textDisplay.teleport(player.getLocation().add(0, config.getTextDisplayOffsetY(), 0));
        }

        visuals.update(player);
        if (session.getState() != PomodoroState.WORK_COMPLETED) {
            TimerUI.update(player, this);
        }
    }

    public PomodoroSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public Map<UUID, PomodoroSession> getSessions() {
        return sessions;
    }

    public PomodoroVisuals getVisuals() {
        return visuals;
    }

    /**
     * Checks if a player is too far from their Pomodoro start location during a WORK session.
     *
     * @param player The player to check.
     * @param to     The destination location.
     * @return true if the player is too far, false otherwise.
     */
    public boolean isPlayerTooFar(Player player, Location to) {
        PomodoroSession session = getSession(player);
        if (session == null || session.getState() != PomodoroState.WORK) {
            return false;
        }
        return isLocationTooFar(session, to);
    }

    /**
     * Checks if a given location is too far from the session's start location.
     *
     * @param session  The player's Pomodoro session.
     * @param location The location to check.
     * @return true if the location is too far, false otherwise.
     */
    public boolean isLocationTooFar(PomodoroSession session, Location location) {
        if (session == null) return false;
        PomodoroConfig config = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);
        Location startLocation = session.getStartLocation();
        if (!location.getWorld().getUID().equals(startLocation.getWorld().getUID())) {
            return true;
        }
        double maxDist = config.getMaxDistance();
        return location.distanceSquared(startLocation) > (maxDist * maxDist);
    }

}