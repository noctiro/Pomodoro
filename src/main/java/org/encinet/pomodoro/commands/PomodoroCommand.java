package org.encinet.pomodoro.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.PomodoroManager;
import org.encinet.pomodoro.service.session.PomodoroSession;
import org.encinet.pomodoro.service.session.PomodoroState;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;
import org.encinet.pomodoro.ui.UIManager;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.BiConsumer;

public class PomodoroCommand {
    private static final Pomodoro PLUGIN = Pomodoro.getInstance();
    private static final PomodoroManager POMODORO_MANAGER = PLUGIN.getPomodoroManager();
    private static final UIManager UI_MANAGER = PLUGIN.getUiManager();
    private static final LanguageManager LANG = PLUGIN.getLanguageManager();
    private static final PlayerPresetManager PRESET_MANAGER = PLUGIN.getPlayerPresetManager();

    public static LiteralCommandNode<CommandSourceStack> getCommand() {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("pomodoro")
                .executes(PomodoroCommand::executeBase)
                .then(buildStartCommand())
                .then(buildStopCommand())
                .then(buildPauseCommand())
                .then(buildResumeCommand())
                .then(buildGuiCommand())
                .then(buildReloadCommand());
        return command.build();
    }

    private static int executeBase(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof Player player) {
            PomodoroSession session = POMODORO_MANAGER.getSession(player);
            if (session != null && session.getState() != PomodoroState.STOPPED) {
                UI_MANAGER.openTimerUI(player);
            } else {
                UI_MANAGER.openPresetSelectionUI(player);
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildStartCommand() {
        return Commands.literal("start")
                .requires(playerPredicate(player -> {
                    PomodoroSession session = POMODORO_MANAGER.getSession(player);
                    return session == null || session.getState() == PomodoroState.STOPPED;
                }))
                .executes(playerExecutor(player -> UI_MANAGER.openPresetSelectionUI(player)))
                .then(Commands.argument("preset", StringArgumentType.string())
                        .suggests(presetSuggestionProvider())
                        .executes(playerExecutor(PomodoroCommand::executeStart)));
    }

    private static void executeStart(Player player, CommandContext<CommandSourceStack> context) {
        PomodoroSession session = POMODORO_MANAGER.getSession(player);
        if (session != null && session.getState() != PomodoroState.STOPPED) {
            player.sendMessage(LANG.getMessage(player, "messages.timer_already_running"));
            return;
        }
        String presetKey = context.getArgument("preset", String.class);
        Map<String, PresetConfig.Preset> presets = PRESET_MANAGER.getPlayerPresets(player);
        PresetConfig.Preset preset = presets.get(presetKey);

        if (preset == null) {
            player.sendMessage(LANG.getMessage(player, "messages.preset_not_found", Map.of("preset", presetKey)));
            return;
        }

        POMODORO_MANAGER.start(player, preset);
        player.sendMessage(LANG.getMessage(player, "messages.timer_start", Map.of("preset", preset.name())));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildStopCommand() {
        return Commands.literal("stop")
                .requires(playerPredicate(player -> {
                    PomodoroSession session = POMODORO_MANAGER.getSession(player);
                    return session != null && session.getState() != PomodoroState.STOPPED;
                }))
                .executes(playerExecutor(player -> {
                    POMODORO_MANAGER.stop(player);
                    player.sendMessage(LANG.getMessage(player, "messages.timer_stop"));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildPauseCommand() {
        return Commands.literal("pause")
                .requires(playerPredicate(player -> {
                    PomodoroSession session = POMODORO_MANAGER.getSession(player);
                    return session != null
                            && (session.getState() == PomodoroState.WORK || session.getState() == PomodoroState.BREAK
                                    || session.getState() == PomodoroState.LONG_BREAK);
                }))
                .executes(playerExecutor(player -> {
                    POMODORO_MANAGER.pause(player);
                    player.sendMessage(LANG.getMessage(player, "messages.timer_pause"));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildResumeCommand() {
        return Commands.literal("resume")
                .requires(playerPredicate(player -> {
                    PomodoroSession session = POMODORO_MANAGER.getSession(player);
                    return session != null && session.getState() == PomodoroState.PAUSED;
                }))
                .executes(playerExecutor(player -> {
                    POMODORO_MANAGER.resume(player);
                    player.sendMessage(LANG.getMessage(player, "messages.timer_resume"));
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildGuiCommand() {
        return Commands.literal("gui")
                .requires(playerPredicate(player -> player.hasPermission("pomodoro.admin")))
                .executes(playerExecutor(UI_MANAGER::openPresetSelectionUI));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildReloadCommand() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("pomodoro.admin"))
                .executes(context -> {
                    PLUGIN.reload();
                    CommandSender sender = context.getSource().getSender();
                    if (sender instanceof Player player) {
                        sender.sendMessage(LANG.getMessage(player, "messages.reload"));
                    } else {
                        sender.sendMessage(LANG.getMessage("messages.reload"));
                    }
                    return Command.SINGLE_SUCCESS;
                });
    }

    // --- Predicates & Executors ---

    private static Predicate<CommandSourceStack> playerPredicate(Predicate<Player> predicate) {
        return source -> source.getSender() instanceof Player player && predicate.test(player);
    }

    private static Command<CommandSourceStack> playerExecutor(Consumer<Player> consumer) {
        return context -> {
            if (context.getSource().getSender() instanceof Player player) {
                consumer.accept(player);
                return Command.SINGLE_SUCCESS;
            }
            // This part should ideally not be reached if requires(playerPredicate(...)) is
            // used.
            return 0;
        };
    }

    private static Command<CommandSourceStack> playerExecutor(
            BiConsumer<Player, CommandContext<CommandSourceStack>> consumer) {
        return context -> {
            if (context.getSource().getSender() instanceof Player player) {
                consumer.accept(player, context);
                return Command.SINGLE_SUCCESS;
            }
            return 0;
        };
    }

    // --- Suggestion Providers ---

    private static SuggestionProvider<CommandSourceStack> presetSuggestionProvider() {
        return (context, builder) -> {
            if (context.getSource().getSender() instanceof Player player) {
                Map<String, PresetConfig.Preset> presets = PRESET_MANAGER.getPlayerPresets(player);
                presets.keySet().stream()
                        .filter(preset -> preset.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                        .forEach(builder::suggest);
            }
            return builder.buildFuture();
        };
    }
}
