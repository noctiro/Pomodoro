package org.encinet.pomodoro.service.session;

import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.impl.PomodoroConfig;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.ui.TimerUI;

public class PomodoroSession {
    private PomodoroState state = PomodoroState.STOPPED;
    private PomodoroState previousState;
    private final PresetConfig.Preset preset;

    private final int workDuration;
    private final int breakDuration;
    private final int longBreakDuration;
    private final int sessions;

    private int currentSession = 1;
    private int timeLeft;
    private int extraTime = 0;

    private BossBar bossBar;
    private TextDisplay textDisplay;
    private Location startLocation;

    private boolean bossbarEnabled;
    private boolean titleEnabled;

    public PomodoroSession(PresetConfig.Preset preset) {
        this.preset = preset;
        // Pre-calculate durations in seconds
        this.workDuration = preset.work() * 60;
        this.breakDuration = preset.breakTime() * 60;
        this.longBreakDuration = preset.longBreak() * 60;
        this.sessions = preset.sessions();
        this.timeLeft = this.workDuration;

        PomodoroConfig config = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);
        this.bossbarEnabled = config.isBossbarDefault();
        this.titleEnabled = config.isTitleDefault();
    }

    public void setState(PomodoroState newState, Player player) {
        setState(newState, player, false);
    }

    public void setState(PomodoroState newState, Player player, boolean isResuming) {
        if (this.state != newState) {
            this.state = newState;
            this.state.getHandler().onEnter(this, player, isResuming);

            // Update UI on state change
            TimerUI.update(player, Pomodoro.getInstance().getPomodoroManager());
            player.updateCommands();
        }
    }

    public void tick(Player player) {
        if (state.getHandler() != null) {
            state.getHandler().tick(this, player);
        }
    }

    public boolean isPaused() {
        return this.state == PomodoroState.PAUSED;
    }

    public PomodoroState getState() {
        return state;
    }

    public PomodoroState getPreviousState() {
        return previousState;
    }

    public void setPreviousState(PomodoroState previousState) {
        this.previousState = previousState;
    }

    public PresetConfig.Preset getPreset() {
        return preset;
    }

    public int getWorkDuration() {
        return workDuration;
    }

    public int getBreakDuration() {
        return breakDuration;
    }

    public int getLongBreakDuration() {
        return longBreakDuration;
    }

    public int getSessions() {
        return sessions;
    }

    public int getCurrentSession() {
        return currentSession;
    }

    public void setCurrentSession(int currentSession) {
        this.currentSession = currentSession;
    }

    public int getTimeLeft() {
        return timeLeft;
    }

    public void setTimeLeft(int timeLeft) {
        this.timeLeft = timeLeft;
    }

    public BossBar getBossBar() {
        return bossBar;
    }

    public void setBossBar(BossBar bossBar) {
        this.bossBar = bossBar;
    }

    public TextDisplay getTextDisplay() {
        return textDisplay;
    }

    public void setTextDisplay(TextDisplay textDisplay) {
        this.textDisplay = textDisplay;
    }

    public Location getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(Location startLocation) {
        this.startLocation = startLocation;
    }

    public boolean isBossbarEnabled() {
        return bossbarEnabled;
    }

    public void setBossbarEnabled(boolean bossbarEnabled) {
        this.bossbarEnabled = bossbarEnabled;
    }

    public boolean isTitleEnabled() {
        return titleEnabled;
    }

    public void setTitleEnabled(boolean titleEnabled) {
        this.titleEnabled = titleEnabled;
    }

    public int getExtraTime() {
        return extraTime;
    }

    public void setExtraTime(int extraTime) {
        this.extraTime = extraTime;
    }
}