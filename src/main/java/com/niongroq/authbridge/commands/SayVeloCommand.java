package com.niongroq.authbridge.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.niongroq.authbridge.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.slf4j.Logger;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /sayvelo — broadcasts a message to every player on every connected server.
 *
 * Supports two message formats:
 *
 *   1. Legacy color codes:
 *      /sayvelo &c&lANNOUNCEMENT&r &fHello everyone!
 *
 *   2. Gradient prefix (custom GRADINT tag):
 *      /sayvelo <GRADINT:RED:WHITE:GREEN>ANNOUNCEMENT» Hello everyone!
 *      /sayvelo <GRADINT:#FF0000:#FFFFFF:#00FF00>ANNOUNCEMENT» Hello!
 *
 *      Colors can be any Minecraft named color (RED, GREEN, BLUE, GOLD, AQUA,
 *      YELLOW, WHITE, BLACK, DARK_RED, DARK_GREEN, DARK_BLUE, DARK_AQUA,
 *      DARK_PURPLE, LIGHT_PURPLE, DARK_GRAY, GRAY) or a hex value (#RRGGBB).
 *
 * Permission: authbridge.sayvelo  (console always has permission)
 */
public class SayVeloCommand implements SimpleCommand {

    // Matches <GRADINT:COLOR1:COLOR2:...>rest of message
    private static final Pattern GRADIENT_PATTERN =
        Pattern.compile("^<GRADINT:([^>]+)>(.+)$", Pattern.DOTALL);

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final ProxyServer server;
    private final Logger logger;

    public SayVeloCommand(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(MessageUtils.colorize("&6/sayvelo &7<message>"));
            source.sendMessage(MessageUtils.colorize(
                "&8Gradient: &f/sayvelo <GRADINT:RED:WHITE:GREEN>ANNOUNCEMENT\u00bb message"));
            source.sendMessage(MessageUtils.colorize(
                "&8Colors  : &fRED WHITE GREEN BLUE GOLD AQUA YELLOW PURPLE DARK_RED..."));
            return;
        }

        String raw       = String.join(" ", args);
        Component component = buildComponent(raw);

        int count = 0;
        for (Player player : server.getAllPlayers()) {
            player.sendMessage(component);
            count++;
        }

        String sender = source instanceof Player
            ? ((Player) source).getUsername() : "CONSOLE";
        source.sendMessage(MessageUtils.colorize(
            "&a\u2714 &7Broadcast sent to &f" + count + " &7player(s)."));
        logger.info("[SayVelo] {} broadcasted to {} players: {}", sender, count, raw);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) return true;
        return invocation.source().hasPermission("authbridge.sayvelo");
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return List.of(
                "<GRADINT:RED:WHITE:GREEN>ANNOUNCEMENT\u00bb",
                "<GRADINT:GOLD:YELLOW:WHITE>ANNOUNCEMENT\u00bb",
                "<GRADINT:AQUA:BLUE>INFO\u00bb",
                "<GRADINT:DARK_RED:RED:GOLD>WARNING\u00bb"
            );
        }
        return List.of();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Component buildComponent(String raw) {
        Matcher m = GRADIENT_PATTERN.matcher(raw);
        if (!m.matches()) {
            return MessageUtils.colorize(raw);
        }

        String colorsPart = m.group(1);
        String text       = m.group(2);

        StringBuilder miniMsg = new StringBuilder("<gradient:");
        String[] colors = colorsPart.split(":");
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) miniMsg.append(":");
            String c = colors[i].trim();
            miniMsg.append(c.startsWith("#") ? c : toMiniColor(c));
        }
        miniMsg.append(">").append(sanitize(text)).append("</gradient>");

        try {
            return MM.deserialize(miniMsg.toString());
        } catch (Exception e) {
            logger.warn("[SayVelo] Gradient parse failed ({}), using plain text", e.getMessage());
            return MessageUtils.colorize(text);
        }
    }

    /**
     * Map Minecraft color name to a MiniMessage-compatible name.
     * Most names are the same in lowercase; a few need mapping.
     */
    private String toMiniColor(String name) {
        switch (name.toUpperCase()) {
            case "DARK_RED":    return "dark_red";
            case "DARK_GREEN":  return "dark_green";
            case "DARK_BLUE":   return "dark_blue";
            case "DARK_AQUA":   return "dark_aqua";
            case "DARK_PURPLE": return "dark_purple";
            case "DARK_GRAY":   return "dark_gray";
            case "LIGHT_PURPLE":return "light_purple";
            default:            return name.toLowerCase();
        }
    }

    /**
     * Strip any MiniMessage tags from user-supplied text to prevent injection.
     * We keep the raw visible text intact; only gradient coloring is applied.
     */
    private String sanitize(String text) {
        return text.replace("<", "\\<").replace(">", "\\>");
    }
}
