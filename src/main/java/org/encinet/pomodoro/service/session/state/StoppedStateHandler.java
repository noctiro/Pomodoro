package org.encinet.pomodoro.service.session.state;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;

public class StoppedStateHandler implements PomodoroStateHandler {
    @Override
    public void tick(PomodoroSession session, Player player) {
        // Stopped state does nothing on tick
    }

    @Override
    public void onEnter(PomodoroSession session, Player player, boolean isResuming) {
        PomodoroState state = session.getState();
        PomodoroState previousState = session.getPreviousState();
        if (state == PomodoroState.WORK || state == PomodoroState.BREAK ||
                (state == PomodoroState.PAUSED && (previousState == PomodoroState.WORK || previousState == PomodoroState.BREAK))) {
            Pomodoro.getInstance().getSoundManager().playSessionFailSound(player);
        }

        if (session.getBossBar() != null) {
            session.getBossBar().removePlayer(player);
        }
        if (session.getTextDisplay() != null) {
            session.getTextDisplay().remove();
        }
    }

    @Override
    public PomodoroState getState() {
        return PomodoroState.STOPPED;
    }
}