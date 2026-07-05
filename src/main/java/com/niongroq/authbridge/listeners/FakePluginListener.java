package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.proxy.Player;
import com.niongroq.authbridge.managers.ConfigManager;
import com.niongroq.authbridge.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.util.Set;

/**
 * Intercepts every command that could reveal plugin/server information
 * and replies with a configurable fake plugin list instead of the real one.
 *
 * Runs at PostOrder.EARLY so it fires before AuthListener's generic
 * "command blocked" handler — the player receives only the fake response,
 * not two conflicting messages.
 *
 * Covered attack vectors:
 *   /plugins, /pl, /plugin                        — standard Bukkit/Spigot
 *   /version, /ver, /about, /icanhasbukkit        — version/info commands
 *   bukkit:pl, spigot:pl, paper:pl, velocity:pl   — namespaced variants
 *   minecraft:plugins, waterfall:plugins, …        — more namespaced variants
 */
public class FakePluginListener {

    // Every known variant that discloses plugin info, including namespaced forms
    private static final Set<String> PLUGIN_INFO_COMMANDS = Set.of(
        // Plain
        "plugins", "pl", "plugin",
        "version", "ver", "about", "icanhasbukkit",
        // Bukkit / Spigot namespaced
        "bukkit:plugins", "bukkit:pl", "bukkit:version",
        "spigot:plugins", "spigot:pl",
        // Paper
        "paper:plugins", "paper:pl", "paper:version",
        // Velocity / Waterfall / BungeeCord
        "velocity:plugins", "velocity:pl", "velocity:version",
        "waterfall:plugins", "waterfall:pl",
        "bungeecord:plugins", "bungeecord:pl",
        "bungee:plugins", "bungee:pl",
        // Purpur / Pufferfish / other forks
        "purpur:plugins", "pufferfish:plugins",
        // Minecraft base
        "minecraft:plugins", "minecraft:version"
    );

    private final ConfigManager configManager;
    private final Logger logger;

    public FakePluginListener(ConfigManager configManager, Logger logger) {
        this.configManager = configManager;
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onCommand(CommandExecuteEvent event) {
        String commandName = extractCommand(event.getCommand());

        if (!PLUGIN_INFO_COMMANDS.contains(commandName)) {
            return;
        }

        // Send fake plugin list to the player; deny the real command execution
        if (event.getCommandSource() instanceof Player) {
            sendFakePluginList((Player) event.getCommandSource());
        }

        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void sendFakePluginList(Player player) {
        String pluginName = configManager.getFakePluginName();
        String color     = configManager.getFakePluginColor();

        String messageLine = configManager.getPluginMessage()
            .replace("%plugin%", color + pluginName + "&r");
        String footerLine  = configManager.getPluginFooter();

        Component msg = MessageUtils.colorize(messageLine);
        player.sendMessage(msg);

        if (footerLine != null && !footerLine.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(footerLine));
        }
    }

    /**
     * Extract the command name from a raw command string, stripping leading /
     * and any arguments. Does NOT strip namespace prefixes so that
     * "bukkit:pl" is matched as-is against PLUGIN_INFO_COMMANDS.
     */
    private String extractCommand(String raw) {
        raw = raw.trim().toLowerCase();
        if (raw.startsWith("/")) raw = raw.substring(1);
        int space = raw.indexOf(' ');
        return space != -1 ? raw.substring(0, space) : raw;
    }
}
