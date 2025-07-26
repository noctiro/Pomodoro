package org.encinet.pomodoro;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.encinet.pomodoro.commands.PomodoroCommand;

import java.util.List;
import org.encinet.pomodoro.config.ConfigManager;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.listeners.PlayerListener;
import org.encinet.pomodoro.listeners.UIListener;
import org.encinet.pomodoro.service.PomodoroManager;
import org.encinet.pomodoro.service.sound.SoundManager;
import org.encinet.pomodoro.service.storage.DatabaseManager;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;
import org.encinet.pomodoro.ui.UIManager;

/**
 * @author Noctiro
 */
public final class Pomodoro extends JavaPlugin {
    private static Pomodoro instance;
    private ConfigManager configManager;
    private LanguageManager languageManager;
    private PomodoroManager pomodoroManager;
    private UIManager uiManager;
    private PlayerPresetManager playerPresetManager;
    private DatabaseManager databaseManager;
    private PlayerListener playerListener;
    private SoundManager soundManager;

    @Override
    public void onEnable() {
        // Plugin startup logic
        instance = this;

        configManager = new ConfigManager(this);
        configManager.initialize();

        languageManager = new LanguageManager(this);
        languageManager.loadLanguages();

        soundManager = new SoundManager();
        pomodoroManager = new PomodoroManager();
        uiManager = new UIManager();
        databaseManager = new DatabaseManager(this);
        playerPresetManager = new PlayerPresetManager(this);


        LifecycleEventManager<org.bukkit.plugin.Plugin> lifecycleManager = this.getLifecycleManager();
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands commands = event.registrar();
            commands.register(
                    PomodoroCommand.getCommand(),
                    List.of("pomo")
            );
        });
        playerListener = new PlayerListener();
        getServer().getPluginManager().registerEvents(new UIListener(), this);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if (pomodoroManager != null) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                pomodoroManager.stop(player);
            }
        }
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
    }

    public void reload() {
        configManager.reloadAll();
        languageManager.loadLanguages();
    }

    public static Pomodoro getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public PomodoroManager getPomodoroManager() {
        return pomodoroManager;
    }

    public UIManager getUiManager() {
        return uiManager;
    }

    public PlayerPresetManager getPlayerPresetManager() {
        return playerPresetManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public SoundManager getSoundManager() {
        return soundManager;
    }

    public java.io.File getJarFile() {
        return getFile();
    }
}
