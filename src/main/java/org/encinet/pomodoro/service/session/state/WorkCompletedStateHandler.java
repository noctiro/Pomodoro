package org.encinet.pomodoro.service.session.state;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;
import org.encinet.pomodoro.ui.WorkCompletedUI;

public class WorkCompletedStateHandler implements PomodoroStateHandler {
    @Override
    public void tick(PomodoroSession session, Player player) {
        session.setExtraTime(session.getExtraTime() + 1);
        Pomodoro.getInstance().getPomodoroManager().getVisuals().update(player);
        WorkCompletedUI.update(player);
    }

    @Override
    public void onEnter(PomodoroSession session, Player player, boolean isResuming) {
        if (!isResuming) {
            session.setExtraTime(0);
            // Open the UI for the player to start the break
            WorkCompletedUI.open(player);
            Pomodoro.getInstance().getLanguageManager().sendMessage(player, "messages.work_completed");
        }
    }

    @Override
    public PomodoroState getState() {
        return PomodoroState.WORK_COMPLETED;
    }

    @Override
    public void onExit(PomodoroSession session, Player player) {
        int totalFocusTime = session.getWorkDuration() + session.getExtraTime();
        Pomodoro.getInstance().getDatabaseManager().addWorkSession(player.getUniqueId(), totalFocusTime);
    }
}