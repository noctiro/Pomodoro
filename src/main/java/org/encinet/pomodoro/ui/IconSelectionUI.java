package org.encinet.pomodoro.ui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.encinet.pomodoro.Pomodoro;
import org.encinet.pomodoro.config.LanguageManager;
import org.encinet.pomodoro.config.impl.PresetConfig;
import org.encinet.pomodoro.service.storage.PlayerPresetManager;
import org.encinet.pomodoro.ui.util.ItemBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IconSelectionUI {
    private static final List<Material> COMMON_ICONS = Arrays.asList(
            Material.CLOCK, Material.BOOK, Material.WRITABLE_BOOK, Material.DIAMOND_SWORD, Material.DIAMOND_PICKAXE,
            Material.CRAFTING_TABLE, Material.FURNACE, Material.CHEST, Material.ANVIL, Material.ENCHANTING_TABLE,
            Material.REDSTONE, Material.LAPIS_LAZULI, Material.GOLD_INGOT, Material.IRON_INGOT, Material.DIAMOND, Material.EMERALD,
            Material.APPLE, Material.BREAD, Material.WHEAT_SEEDS, Material.OAK_SAPLING, Material.COBBLESTONE, Material.DIRT,
            Material.SAND, Material.GLASS, Material.BUCKET, Material.WATER_BUCKET, Material.LAVA_BUCKET
    );

    public static List<Material> getCommonIcons() {
        return COMMON_ICONS;
    }

    public static Inventory create(Player player, String presetKey) {
        LanguageManager languageManager = Pomodoro.getInstance().getLanguageManager();
        PlayerPresetManager playerPresetManager = Pomodoro.getInstance().getPlayerPresetManager();
        PresetConfig.Preset preset = playerPresetManager.getPlayerPresets(player).get(presetKey);

        if (preset == null) {
            player.sendMessage(languageManager.getMessage(player, "messages.preset_not_found"));
            player.closeInventory();
            return null;
        }

        Inventory inventory = Bukkit.createInventory(null, 54, languageManager.getMessage(player, "ui.icon_selection.title"));

        // Fill border with glass panes
        ItemStack filler = ItemBuilder.createFiller();
        for (int i = 0; i < inventory.getSize(); i++) {
            if (i < 9 || i > 44 || i % 9 == 0 || i % 9 == 8) {
                inventory.setItem(i, filler);
            }
        }

        NamespacedKey presetKeyName = new NamespacedKey(Pomodoro.getInstance(), "preset_key");
        NamespacedKey actionKey = new NamespacedKey(Pomodoro.getInstance(), "pomodoro_action");

        // Icon Display Logic
        List<Material> iconsToDisplay = new ArrayList<>(COMMON_ICONS);
        Material currentIcon = Material.getMaterial(preset.icon());

        if (currentIcon != null && !iconsToDisplay.contains(currentIcon)) {
            iconsToDisplay.remove(iconsToDisplay.size() - 1); // Remove last to make space
            iconsToDisplay.add(currentIcon); // Add current icon at the end
        }

        int iconSlot = 10;
        for (Material icon : iconsToDisplay) {
            if (iconSlot % 9 == 8) iconSlot += 2; // Skip border
            if (iconSlot > 44) break; // Out of icon area

            ItemBuilder iconBuilder = new ItemBuilder(icon)
                    .persistentData(presetKeyName, presetKey)
                    .persistentData(actionKey, "select_icon");

            if (icon == currentIcon) {
                iconBuilder.enchant(Enchantment.LURE, 1, true).itemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            inventory.setItem(iconSlot++, iconBuilder.build());
        }

        // Enchant toggle
        ItemBuilder enchantBuilder = new ItemBuilder(preset.enchanted() ? Material.ENCHANTED_BOOK : Material.BOOK)
                .displayName(languageManager.getMessage(player, "ui.icon_selection.enchant.name"))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "toggle_enchant");
        String statusKey = preset.enchanted() ? "ui.icon_selection.enchant_status.enabled" : "ui.icon_selection.enchant_status.disabled";
        Component statusComponent = languageManager.getMessage(player, statusKey);
        enchantBuilder.lore(languageManager.getMessageList(player, "ui.icon_selection.enchant.lore", Placeholder.component("status", statusComponent)));
        if (preset.enchanted()) {
            enchantBuilder.enchant(Enchantment.LURE, 1, true).itemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        inventory.setItem(48, enchantBuilder.build());


        // Custom ID
        ItemStack customIdItem = new ItemBuilder(Material.NAME_TAG)
                .displayName(languageManager.getMessage(player, "ui.icon_selection.custom_id.name"))
                .lore(languageManager.getMessageList(player, "ui.icon_selection.custom_id.lore"))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "custom_icon")
                .build();
        inventory.setItem(49, customIdItem);


        // Back button
        ItemStack backItem = new ItemBuilder(Material.ARROW)
                .displayName(languageManager.getMessage(player, "ui.back_button"))
                .persistentData(presetKeyName, presetKey)
                .persistentData(actionKey, "back")
                .build();
        inventory.setItem(53, backItem);

        return inventory;
    }
}