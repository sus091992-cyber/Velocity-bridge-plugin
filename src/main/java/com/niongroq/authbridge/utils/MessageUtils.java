package com.niongroq.authbridge.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class MessageUtils {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();

    public static Component colorize(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(message);
    }

    public static String stripColor(String message) {
        if (message == null) {
            return "";
        }
        return message.replaceAll("&[0-9a-fk-or]", "");
    }

    public static String replaceColorCode(String message, String colorCode) {
        if (message == null || colorCode == null) {
            return message;
        }
        return message.replace("%color%", colorCode);
    }

    public static String replacePlaceholder(String message, String placeholder, String replacement) {
        if (message == null || placeholder == null || replacement == null) {
            return message;
        }
        return message.replace("%" + placeholder + "%", replacement);
    }
}