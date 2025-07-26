package org.encinet.pomodoro.service;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.boss.BossBar;

import java.time.Duration;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PomodoroConfig;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class PomodoroVisuals {

    private final PomodoroManager pomodoroManager;

    public PomodoroVisuals(PomodoroManager pomodoroManager) {
        this.pomodoroManager = pomodoroManager;
        PacketEvents.getAPI().getEventManager().registerListener(new TextDisplayPacketListener());
    }

    public void update(Player player) {
        PomodoroSession session = pomodoroManager.getSession(player);
        if (session == null)
            return;

        if (session.isBossbarEnabled()) {
            if (session.getBossBar() != null) {
                session.getBossBar().setVisible(true);
            }
            updateBossBar(player, session);
        } else {
            if (session.getBossBar() != null) {
                session.getBossBar().setVisible(false);
            }
        }

        if (session.isTitleEnabled()) {
            updateTitle(player, session);
        } else {
            player.clearTitle();
        }

        updateTextDisplay(player, session);
    }

    private void updateBossBar(Player player, PomodoroSession session) {
        int minutes = session.getTimeLeft() / 60;
        int seconds = session.getTimeLeft() % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        String status = languageManager.getStatusMessage(session.getState(), player);

        BossBar bossBar = session.getBossBar();
        bossBar.setTitle(LegacyComponentSerializer.legacySection()
                .serialize(languageManager.getMessage(player, "bossbar.title", Map.of(
                        "preset_name", session.getPreset().name(),
                        "status", status,
                        "current_session", String.valueOf(session.getCurrentSession()),
                        "total_sessions", String.valueOf(session.getSessions()),
                        "time", time))));

        PomodoroConfig config = Pomodoro.getInstance().getConfigManager().getConfig(PomodoroConfig.class);

        double totalDuration;
        PomodoroState currentState = session.getState();
        PomodoroState displayState = currentState == PomodoroState.PAUSED ? session.getPreviousState() : currentState;

        switch (displayState) {
            case WORK -> {
                totalDuration = session.getWorkDuration();
                bossBar.setColor(config.getWorkColor());
            }
            case BREAK -> {
                totalDuration = session.getBreakDuration();
                bossBar.setColor(config.getBreakColor());
            }
            case LONG_BREAK -> {
                totalDuration = session.getLongBreakDuration();
                bossBar.setColor(config.getLongBreakColor());
            }
            default -> totalDuration = 1; // Avoid division by zero
        }

        if (currentState == PomodoroState.PAUSED) {
            bossBar.setColor(config.getPausedColor());
        }

        double progress = (totalDuration > 0) ? (double) session.getTimeLeft() / totalDuration : 0;
        bossBar.setProgress(Math.max(0, Math.min(1, progress)));
    }

    private void updateTitle(Player player, PomodoroSession session) {
        int minutes = session.getTimeLeft() / 60;
        int seconds = session.getTimeLeft() % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        String status = languageManager.getStatusMessage(session.getState(), player);

        Component titleComponent = languageManager.getMessage(player, "title.title", Map.of(
                "preset_name", session.getPreset().name()
        ));
        Component subtitleComponent = languageManager.getMessage(player, "title.subtitle", Map.of(
                "status", status,
                "current_session", String.valueOf(session.getCurrentSession()),
                "total_sessions", String.valueOf(session.getSessions()),
                "time", time
        ));

        Title.Times times = Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofSeconds(1));
        Title title = Title.title(titleComponent, subtitleComponent, times);
        player.showTitle(title);
    }

    private void updateTextDisplay(Player owner, PomodoroSession session) {
        if (session == null)
            return;

        int minutes = session.getTimeLeft() / 60;
        int seconds = session.getTimeLeft() % 60;
        String time = String.format("%02d:%02d", minutes, seconds);
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        String status = languageManager.getStatusMessage(session.getState(), owner);

        TextDisplay textDisplay = session.getTextDisplay();
        if (textDisplay != null) {
            textDisplay.text(languageManager.getMessage(owner, "text-display.format", Map.of(
                    "preset_name", session.getPreset().name(),
                    "status", status,
                    "time", time)));
        }
    }

    private class TextDisplayPacketListener extends PacketListenerAbstract {
        @Override
        public void onPacketSend(PacketSendEvent event) {
            if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) {
                return;
            }
            if (!(event.getPlayer() instanceof Player)) {
                return;
            }
            Player player = (Player) event.getPlayer();

            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            int entityId = wrapper.getEntityId();

            PomodoroSession session = pomodoroManager.getSessions().values().stream()
                    .filter(s -> s.getTextDisplay() != null && s.getTextDisplay().getEntityId() == entityId)
                    .findFirst().orElse(null);

            if (session == null) {
                return;
            }

            List<EntityData<?>> entityDataList = wrapper.getEntityMetadata();
            Optional<EntityData<?>> textDataOptional = entityDataList.stream()
                    .filter(data -> data.getIndex() == 23) // 23 is the index for the text component in TextDisplay
                    .findFirst();

            if (textDataOptional.isPresent()) {
                int minutes = session.getTimeLeft() / 60;
                int seconds = session.getTimeLeft() % 60;
                String time = String.format("%02d:%02d", minutes, seconds);
                LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
                String status = languageManager.getStatusMessage(session.getState(), player);

                Component translatedText = languageManager.getMessage(player, "text-display.format", Map.of(
                        "preset_name", session.getPreset().name(),
                        "status", status,
                        "time", time));

                EntityData<?> originalData = textDataOptional.get();
                // The value of the EntityData should be a Component, not an
                // Optional<Component>.
                // The PacketEvents API likely handles the Optional wrapping internally for this
                // data type.
                // The ClassCastException indicates that an Optional was being used where a
                // Component was expected.
                EntityData<?> newTextData = new EntityData(originalData.getIndex(), originalData.getType(),
                        translatedText);
                entityDataList.set(entityDataList.indexOf(originalData), newTextData);
            }
        }
    }
}