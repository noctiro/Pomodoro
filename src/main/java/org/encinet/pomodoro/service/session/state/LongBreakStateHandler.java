package org.encinet.pomodoro.service.session.state;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.service.PomodoroManager;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;

public class LongBreakStateHandler implements PomodoroStateHandler {
    @Override
    public void tick(PomodoroSession session, Player player) {
        session.setTimeLeft(session.getTimeLeft() - 1);

        if (session.getTimeLeft() <= 0) {
            PomodoroManager pomodoroManager = Pomodoro.getInstance().getPomodoroManager();
            if (pomodoroManager.isLocationTooFar(session, player.getLocation())) {
                player.teleport(session.getStartLocation());
                Pomodoro.getInstance().getLanguageManager().sendActionBar(player, "moved_too_far");
            }
            session.setState(PomodoroState.WORK, player);
        } else if (session.getTimeLeft() <= 5) {
            Pomodoro.getInstance().getSoundManager().playTimerEndWarningSound(player);
        }
    }

    @Override
    public void onEnter(PomodoroSession session, Player player, boolean isResuming) {
        if (!isResuming) {
            session.setTimeLeft(session.getLongBreakDuration());
            session.setCurrentSession(1); // Reset session count
            Pomodoro.getInstance().getLanguageManager().sendMessage(player, "messages.long_break_start");
            Pomodoro.getInstance().getSoundManager().playCompleteAllRoundsSound(player);
        }
    }

    @Override
    public PomodoroState getState() {
        return PomodoroState.LONG_BREAK;
    }
}