package org.encinet.pomodoro.service.session.state;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;

public class WorkStateHandler implements PomodoroStateHandler {
    @Override
    public void tick(PomodoroSession session, Player player) {
        session.setTimeLeft(session.getTimeLeft() - 1);

        if (session.getTimeLeft() <= 0) {
            Pomodoro.getInstance().getSoundManager().playNextCycleSound(player);
            Pomodoro.getInstance().getDatabaseManager().addWorkSession(player.getUniqueId(), session.getWorkDuration());

            if (session.getCurrentSession() >= session.getSessions()) {
                session.setState(PomodoroState.LONG_BREAK, player);
            } else {
                session.setCurrentSession(session.getCurrentSession() + 1);
                session.setState(PomodoroState.BREAK, player);
            }
        } else if (session.getTimeLeft() <= 5) {
            Pomodoro.getInstance().getSoundManager().playTimerEndWarningSound(player);
        }
    }

    @Override
    public void onEnter(PomodoroSession session, Player player, boolean isResuming) {
        if (!isResuming) {
            session.setTimeLeft(session.getWorkDuration());
            Pomodoro.getInstance().getLanguageManager().sendMessage(player, "messages.work_start");
            Pomodoro.getInstance().getSoundManager().playNewRoundStartSound(player);
        }
    }

    @Override
    public PomodoroState getState() {
        return PomodoroState.WORK;
    }
}