# Pomodoro

A highly customizable, GUI-driven Pomodoro timer to help players manage their time and stay focused in Minecraft.

![Paper](https://img.shields.io/badge/Paper-1.21%2B-blue) ![Folia](https://img.shields.io/badge/Folia-1.21%2B-green)
![PacketEvents](https://img.shields.io/badge/Dependency-PacketEvents-orange)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](./LICENSE)

## Requirements

- **Server Version**: Paper / Folia 1.21+
- **Dependencies**: [PacketEvents](https://modrinth.com/plugin/packetevents) (Required)

## How to Use

This plugin is designed to be used primarily through its interactive GUI.

1.  **Open the Main Menu**: Simply type `/pomodoro` or `/pomo`.
2.  **From the GUI, you can**:
    - **Start a Timer**: Select a pre-configured preset to begin your session.
    - **Create Custom Presets**: Design your own work/break schedules.
    - **Edit & Delete**: Right-click your custom presets to modify or remove them.
    - **Control Active Timers**: Pause, resume, stop, and toggle visuals like the Boss Bar and on-screen titles.

## Commands & Permissions

While most actions are performed in the GUI, commands are available as convenient shortcuts. The main command is `/pomodoro` (alias: `/pomo`).

### Player Commands

- `/pomo` or `/pomo gui`: Opens the main menu.
- `/pomo start [preset_name]`: Starts a session. Opens the GUI if no preset is specified.
- `/pomo pause`: Pauses the timer.
- `/pomo resume`: Resumes the timer.
- `/pomo stop`: Stops the current session.
- `/pomo next`: Manually advances to the next state (e.g., from work to break).

### Admin Commands

- `/pomo reload`: Reloads all configuration files.
  - **Permission**: `pomodoro.admin`

## Configuration

The plugin's behavior can be extensively customized through two main files: `config.yml` and `presets.yml`.

### `config.yml`

This file controls the global settings of the plugin. Here you can configure:

- **Restrictions**: Control player actions like chatting and movement during work sessions.
- **Display Defaults**: Set the default visibility for the Boss Bar and Title timers.
- **Language**: Configure the default language and enable/disable adaptive language detection.
- **Colors & Sounds**: Customize the Boss Bar colors and sound effects for all plugin events.

For detailed information on each option, please refer to the comments within the `config.yml` file.

### `presets.yml`

This file defines the available Pomodoro timer presets. You can add, remove, or modify presets here.

**Preset Structure:**

```yaml
presets:
  standard:
    name: "Standard"
    icon: "CLOCK"
    enchanted: false
    work: 25 # minutes
    break: 5 # minutes
    long-break: 15 # minutes
    sessions: 4 # Number of work sessions before a long break
```

- `name`: The display name in the GUI.
- `icon`: The material name for the icon in the GUI (e.g., `BOOK`, `DIAMOND_SWORD`).
- `enchanted`: If `true`, the icon will have an enchanted glint.
- `work`, `break`, `long-break`: Duration in minutes.
- `sessions`: The number of work cycles to complete before a long break starts.

## Supported Languages

The plugin is fully translatable and supports the following languages out of the box. The language can be set globally in `config.yml`, and an **adaptive language** option will automatically use a player's client language if available.

| Language             | Code    | Language              | Code    |
| :------------------- | :------ | :-------------------- | :------ |
| English              | `en`    | German                | `de`    |
| Spanish              | `es`    | French                | `fr`    |
| Japanese             | `ja`    | Korean                | `ko`    |
| Polish               | `pl`    | Portuguese            | `pt`    |
| Russian              | `ru`    | Swedish               | `sv`    |
| Chinese (Simplified) | `zh_CN` | Chinese (Traditional) | `zh_TW` |
| Chinese (Hong Kong)  | `zh_HK` |                       |         |

## License

This project is licensed under the Apache 2.0 License. See the [LICENSE](./LICENSE) file for details.
