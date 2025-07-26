package org.encinet.pomodoro.ui.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.encinet.pomodoro.Pomodoro;

import java.util.List;

public class ItemBuilder {
    private final ItemStack itemStack;
    private final ItemMeta itemMeta;

    public ItemBuilder(Material material) {
        this.itemStack = new ItemStack(material);
        this.itemMeta = itemStack.getItemMeta();
    }

    public ItemBuilder displayName(Component displayName) {
        itemMeta.displayName(displayName);
        return this;
    }

    public ItemBuilder lore(List<Component> lore) {
        itemMeta.lore(lore);
        return this;
    }

    public ItemBuilder lore(Component... lore) {
        itemMeta.lore(List.of(lore));
        return this;
    }

    public ItemBuilder enchant(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        itemMeta.addEnchant(enchantment, level, ignoreLevelRestriction);
        return this;
    }

    public ItemBuilder itemFlags(ItemFlag... flags) {
        itemMeta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder persistentData(String key, String value) {
        itemMeta.getPersistentDataContainer().set(new NamespacedKey(Pomodoro.getInstance(), key), PersistentDataType.STRING, value);
        return this;
    }

    public ItemBuilder persistentData(NamespacedKey key, String value) {
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        return this;
    }

    public ItemStack build() {
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    public static ItemStack createFiller() {
        return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE)
                .displayName(Component.text(" "))
                .build();
    }
}
