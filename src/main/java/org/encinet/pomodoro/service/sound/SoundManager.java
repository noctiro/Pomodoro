package org.encinet.pomodoro.service.sound;

import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.impl.PomodoroConfig;

public class SoundManager {

    private PomodoroConfig getConfig() {
        return Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);
    }

    private void playSound(Player player, PomodoroConfig.Sound sound) {
        if (sound == null || sound.name() == null || sound.name().isEmpty()) {
            return; // Don't play if the sound is not configured
        }
        try {
            player.playSound(player.getLocation(), sound.name(), sound.volume(), sound.pitch());
        } catch (Exception e) {
            // Log a warning if the sound name is invalid or another error occurs.
            Pomodoro.getInstance().getLogger().warning("Could not play sound: " + sound.name() + ". Please verify the sound name in your configuration.");
        }
    }

    public void playClickSound(Player player) {
        playSound(player, getConfig().getSoundUiClick());
    }

    public void playTimerEndWarningSound(Player player) {
        playSound(player, getConfig().getSoundTimerEndWarning());
    }

    public void playNextCycleSound(Player player) {
        playSound(player, getConfig().getSoundNextCycle());
    }

    public void playLeaveWarningSound(Player player) {
        playSound(player, getConfig().getSoundLeaveWarning());
    }

    public void playSuccessSound(Player player) {
        playSound(player, getConfig().getSoundUiSuccess());
    }

    public void playFailSound(Player player) {
        playSound(player, getConfig().getSoundUiFail());
    }

    public void playBackSound(Player player) {
        playSound(player, getConfig().getSoundUiBack());
    }

    public void playCompleteAllRoundsSound(Player player) {
        playSound(player, getConfig().getSoundCompleteAllRounds());
    }

    public void playNewRoundStartSound(Player player) {
        playSound(player, getConfig().getSoundNewRoundStart());
    }

    public void playSessionFailSound(Player player) {
        playSound(player, getConfig().getSoundSessionFail());
    }
}