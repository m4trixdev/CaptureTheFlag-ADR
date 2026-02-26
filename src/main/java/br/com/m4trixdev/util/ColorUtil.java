package br.com.m4trixdev.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

public final class ColorUtil {

    private static final LegacyComponentSerializer SERIALIZER =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .build();

    private ColorUtil() {}

    public static String translate(String text) {
        if (text == null || text.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static Component component(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return SERIALIZER.deserialize(text);
    }

    public static boolean isEmpty(String text) {
        return text == null || text.trim().isEmpty();
    }
}
