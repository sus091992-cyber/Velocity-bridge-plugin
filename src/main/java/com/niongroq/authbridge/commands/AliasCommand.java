package com.niongroq.authbridge.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import com.niongroq.authbridge.managers.ConfigManager;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class AliasCommand implements SimpleCommand {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;

    public AliasCommand(ProxyServer server, ConfigManager configManager, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.logger = logger;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String alias = invocation.alias().toLowerCase();

        String aliasedCommand = configManager.getCustomAliases().get("/" + alias);

        if (aliasedCommand == null || aliasedCommand.isEmpty()) {
            aliasedCommand = "/server " + alias;
        }

        String finalCommand = aliasedCommand.startsWith("/") ? 
                            aliasedCommand.substring(1) : aliasedCommand;

        server.getCommandManager().executeAsync(source, finalCommand);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }
}