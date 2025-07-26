package org.encinet.pomodoro.config.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.encinet.pomodoro.config.AbstractConfig;
import org.encinet.pomodoro.config.annotations.ConfigFile;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@ConfigFile(name = "presets.yml", version = 1)
public class PresetConfig extends AbstractConfig {
        private final Map<String, Preset> presets = new HashMap<>();
        private Preset templatePreset;
    
        public PresetConfig(YamlConfiguration config, Logger logger) {
            super(config, logger);
            loadPresets();
        }
    
        private void loadPresets() {
            // Load template preset
            ConfigurationSection templateSection = config.getConfigurationSection("template");
            if (templateSection != null) {
                boolean enchanted = templateSection.getBoolean("enchanted", false);
                int work = templateSection.getInt("work");
                int breakTime = templateSection.getInt("break");
                int longBreak = templateSection.getInt("long-break");
                int sessions = templateSection.getInt("sessions");
                // Name and Icon are null for the template.
                templatePreset = new Preset(null, null, enchanted, work, breakTime, longBreak, sessions);
            } else {
                logger.warning("Template preset not found in presets.yml. Using a default template.");
                // The default template also shouldn't have a name/icon.
                templatePreset = new Preset(null, null, false, 25, 5, 15, 4);
            }

            // Load user presets
            ConfigurationSection presetsSection = config.getConfigurationSection("presets");
            if (presetsSection != null) {
                for (String key : presetsSection.getKeys(false)) {
                    ConfigurationSection presetSection = presetsSection.getConfigurationSection(key);
                    if (presetSection != null) {
                        presets.put(key, loadPresetFromSection(presetSection));
                    }
                }
            }
        }
    
        private Preset loadPresetFromSection(ConfigurationSection section) {
            String name = section.getString("name");
            String icon = section.getString("icon");
            boolean enchanted = section.getBoolean("enchanted", false);
            int work = section.getInt("work");
            int breakTime = section.getInt("break");
            int longBreak = section.getInt("long-break");
            int sessions = section.getInt("sessions");
            return new Preset(name, icon, enchanted, work, breakTime, longBreak, sessions);
        }
    
        public Map<String, Preset> getPresets() {
            return presets;
        }
    
        public Preset getTemplatePreset() {
            return templatePreset;
        }

    public record Preset(String name, String icon, boolean enchanted, int work, int breakTime, int longBreak, int sessions) {
    }
}
