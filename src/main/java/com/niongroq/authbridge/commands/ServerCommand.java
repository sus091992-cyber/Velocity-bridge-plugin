package com.niongroq.authbridge.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.niongroq.authbridge.managers.ConfigManager;
import com.niongroq.authbridge.managers.WhitelistManager;
import com.niongroq.authbridge.utils.MessageUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final WhitelistManager whitelistManager;
    private final Logger logger;

    public ServerCommand(ProxyServer server, ConfigManager configManager,
                         WhitelistManager whitelistManager, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.whitelistManager = whitelistManager;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(MessageUtils.colorize("&cOnly players can use this command!"));
            return;
        }

        Player player = (Player) source;

        if (args.length < 1) {
            player.sendMessage(MessageUtils.colorize("&cUsage: /server <server>"));
            return;
        }

        String serverName = args[0].toLowerCase();

        // ── fix: was incorrectly checking globallyBlockedCommands instead of blockedServers ──
        if (configManager.getBlockedServers().contains(serverName)) {
            player.sendMessage(MessageUtils.colorize(
                configManager.getMessage("server-blocked")));
            return;
        }

        Optional<RegisteredServer> target = server.getServer(serverName);
        if (!target.isPresent()) {
            player.sendMessage(MessageUtils.colorize("&cServer not found!"));
            return;
        }

        player.createConnectionRequest(target.get()).fireAndForget();
        player.sendMessage(MessageUtils.colorize("&aConnecting to &b" + serverName + "&a..."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        List<String> suggestions = new ArrayList<>();
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 1 && source instanceof Player) {
            String partial = args[0].toLowerCase();
            server.getAllServers().forEach(reg -> {
                String name = reg.getServerInfo().getName();
                // Never suggest blocked servers
                if (!configManager.getBlockedServers().contains(name.toLowerCase())
                        && name.toLowerCase().startsWith(partial)) {
                    suggestions.add(name);
                }
            });
        }

        return suggestions;
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}
