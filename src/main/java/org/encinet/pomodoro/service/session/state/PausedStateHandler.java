package org.encinet.pomodoro.service.session.state;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;

public class PausedStateHandler implements PomodoroStateHandler {
    @Override
    public void tick(PomodoroSession session, Player player) {
        // Paused state does nothing on tick
    }

    @Override
    public void onEnter(PomodoroSession session, Player player, boolean isResuming) {
        // Logic for pausing is handled in PomodoroManager,
        // as it needs to store the previous state.
    }

    @Override
    public PomodoroState getState() {
        return PomodoroState.PAUSED;
    }
}