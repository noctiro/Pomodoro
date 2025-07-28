package org.encinet.pomodoro.config.impl;

import org.bukkit.boss.BarColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.encinet.pomodoro.config.AbstractConfig;
import org.encinet.pomodoro.config.annotations.ConfigFile;
import org.encinet.pomodoro.config.annotations.ConfigValue;

import java.util.logging.Logger;

/**
 * Configuration class for pomodoro timer settings
 */
@ConfigFile(name = "config.yml", version = 1)
public class PomodoroConfig extends AbstractConfig {
    public record Sound(String name, float volume, float pitch) {}
    @ConfigValue("restrictions.allow-chat")
    private boolean allowChat = false;

    @ConfigValue("restrictions.restrict-movement")
    private boolean restrictMovement = false;

    @ConfigValue("restrictions.max-distance")
    private int maxDistance = 10;

    @ConfigValue("restrictions.pull-back-delay")
    private int pullBackDelay = 2000;

    @ConfigValue("text-display.offset-y")
    private double textDisplayOffsetY = 1.8;

    @ConfigValue("display-defaults.bossbar")
    private boolean bossbarDefault = true;

    @ConfigValue("display-defaults.title")
    private boolean titleDefault = true;

    @ConfigValue("language.default-language")
    private String defaultLanguage = "en";

    @ConfigValue("language.adaptive-language")
    private boolean adaptiveLanguage = true;

    private final Sound soundUiClick;
    private final Sound soundTimerEndWarning;
    private final Sound soundNextCycle;
    private final Sound soundLeaveWarning;
    private final Sound soundUiSuccess;
    private final Sound soundUiFail;
    private final Sound soundUiBack;
    private final Sound soundCompleteAllRounds;
    private final Sound soundNewRoundStart;
    private final Sound soundSessionFail;

    private final BarColor workColor;
    private final BarColor breakColor;
    private final BarColor longBreakColor;
    private final BarColor pausedColor;

    public PomodoroConfig(YamlConfiguration config, Logger logger) {
        super(config, logger);
        this.soundUiClick = loadSound(config, "ui-click", "ui.button.click", 1.0f, 1.0f);
        this.soundTimerEndWarning = loadSound(config, "timer-end-warning", "block.note_block.hat", 10.0f, 1.0f);
        this.soundNextCycle = loadSound(config, "next-cycle", "entity.player.levelup", 1.0f, 1.0f);
        this.soundLeaveWarning = loadSound(config, "leave-warning", "entity.villager.no", 10.0f, 1.0f);
        this.soundUiSuccess = loadSound(config, "ui-success", "entity.experience_orb.pickup", 1.0f, 1.0f);
        this.soundUiFail = loadSound(config, "ui-fail", "entity.villager.no", 1.0f, 1.0f);
        this.soundUiBack = loadSound(config, "ui-back", "ui.button.click", 1.0f, 1.0f);
        this.soundCompleteAllRounds = loadSound(config, "complete-all-rounds", "entity.firework_rocket.launch", 1.0f, 1.0f);
        this.soundNewRoundStart = loadSound(config, "new-round-start", "block.bell.use", 1.0f, 1.0f);
        this.soundSessionFail = loadSound(config, "session-fail", "entity.villager.no", 10.0f, 1.0f);

        this.workColor = loadBarColor(config, "work", BarColor.GREEN);
        this.breakColor = loadBarColor(config, "break", BarColor.BLUE);
        this.longBreakColor = loadBarColor(config, "long-break", BarColor.YELLOW);
        this.pausedColor = loadBarColor(config, "paused", BarColor.RED);
    }

    public boolean isAllowChat() {
        return allowChat;
    }

    public boolean isRestrictMovement() {
        return restrictMovement;
    }

    public int getMaxDistance() {
        return maxDistance;
    }

    public int getPullBackDelay() {
        return pullBackDelay;
    }

    public double getTextDisplayOffsetY() {
        return textDisplayOffsetY;
    }

    public boolean isBossbarDefault() {
        return bossbarDefault;
    }

    public boolean isTitleDefault() {
        return titleDefault;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public boolean isAdaptiveLanguage() {
        return adaptiveLanguage;
    }

    public Sound getSoundUiClick() { return soundUiClick; }
    public Sound getSoundTimerEndWarning() { return soundTimerEndWarning; }
    public Sound getSoundNextCycle() { return soundNextCycle; }
    public Sound getSoundLeaveWarning() { return soundLeaveWarning; }
    public Sound getSoundUiSuccess() { return soundUiSuccess; }
    public Sound getSoundUiFail() { return soundUiFail; }
    public Sound getSoundUiBack() { return soundUiBack; }
    public Sound getSoundCompleteAllRounds() { return soundCompleteAllRounds; }
    public Sound getSoundNewRoundStart() { return soundNewRoundStart; }
    public Sound getSoundSessionFail() { return soundSessionFail; }

    public BarColor getWorkColor() { return workColor; }
    public BarColor getBreakColor() { return breakColor; }
    public BarColor getLongBreakColor() { return longBreakColor; }
    public BarColor getPausedColor() { return pausedColor; }

    private Sound loadSound(YamlConfiguration config, String key, String defaultName, float defaultVolume, float defaultPitch) {
        String name = config.getString("sounds." + key + ".name", defaultName);
        float volume = (float) config.getDouble("sounds." + key + ".volume", defaultVolume);
        float pitch = (float) config.getDouble("sounds." + key + ".pitch", defaultPitch);
        return new Sound(name, volume, pitch);
    }

    private BarColor loadBarColor(YamlConfiguration config, String key, BarColor defaultColor) {
        String colorName = config.getString("bossbar-colors." + key, defaultColor.name());
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultColor;
        }
    }
}
