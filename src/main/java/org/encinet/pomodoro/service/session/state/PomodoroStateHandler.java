package org.encinet.pomodoro.service.session.state;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;

/**
 * Represents the behavior of a specific state in the Pomodoro timer.
 * Using the State Pattern to manage session logic.
 */
public interface PomodoroStateHandler {

    /**
     * Called every second when the session is in this state.
     * This method is responsible for updating the timer and checking for state transitions.
     *
     * @param session The current Pomodoro session context.
     * @param player  The player associated with the session.
     */
    void tick(PomodoroSession session, Player player);

    /**
     * Called when the session enters this state.
     * Use this to set up initial conditions for the state, like resetting the timer
     * and sending messages.
     *
     * @param session The current Pomodoro session context.
     * @param player  The player associated with the session.
     */
    default void onEnter(PomodoroSession session, Player player) {
        onEnter(session, player, false);
    }

    /**
     * Called when the session enters this state.
     * Use this to set up initial conditions for the state, like resetting the timer
     * and sending messages.
     *
     * @param session    The current Pomodoro session context.
     * @param player     The player associated with the session.
     * @param isResuming True if entering this state from a pause, false otherwise.
     */
    void onEnter(PomodoroSession session, Player player, boolean isResuming);

    /**
     * Called when the session exits this state.
     * Use this to clean up or save data before transitioning to a new state.
     *
     * @param session The current Pomodoro session context.
     * @param player  The player associated with the session.
     */
    default void onExit(PomodoroSession session, Player player) {
        // Default implementation does nothing.
    }

    /**
     * Gets the enum type corresponding to this state handler.
     *
     * @return The {@link PomodoroState} enum value.
     */
    PomodoroState getState();
}