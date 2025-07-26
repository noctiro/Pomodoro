package org.encinet.pomodoro.service.session;

import org.encinet.pomodoro.service.session.state.*;

public enum PomodoroState {
    WORK("work", new WorkStateHandler()),
    BREAK("break", new BreakStateHandler()),
    LONG_BREAK("long_break", new LongBreakStateHandler()),
    PAUSED("paused", new PausedStateHandler()),
    STOPPED("stopped", new StoppedStateHandler());

    private final String messageKey;
    private final PomodoroStateHandler handler;

    PomodoroState(String messageKey, PomodoroStateHandler handler) {
        this.messageKey = messageKey;
        this.handler = handler;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public PomodoroStateHandler getHandler() {
        return handler;
    }
}