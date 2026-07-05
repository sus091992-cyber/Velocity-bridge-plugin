package com.niongroq.authbridge;

import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.niongroq.authbridge.listeners.AuthListener;
import com.niongroq.authbridge.listeners.TabCompleteListener;
import com.niongroq.authbridge.managers.ConfigManager;
import com.niongroq.authbridge.managers.WhitelistManager;
import com.niongroq.authbridge.managers.PlayerHider;
import com.niongroq.authbridge.commands.ServerCommand;
import com.niongroq.authbridge.commands.AliasCommand;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.nio.file.Path;

@Plugin(
    id = "authbridge",
    name = "AuthBridge",
    version = "3.0.0",
    description = "Professional authentication bridge plugin for Velocity",
    authors = {"S1MPLE"}
)
public class AuthBridge {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private ConfigManager configManager;
    private WhitelistManager whitelistManager;
    private PlayerHider playerHider;

    @Inject
    public AuthBridge(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            logger.info("╔════════════════════════════════════════════╗");
            logger.info("║          AuthBridge v3.0.0 Loading         ║");
            logger.info("║              By: S1MPLE                    ║");
            logger.info("╚════════════════════════════════════════════╝");

            this.configManager = new ConfigManager(dataDirectory, logger);
            this.whitelistManager = new WhitelistManager(dataDirectory, logger);
            this.playerHider = new PlayerHider(server, configManager, logger);

            configManager.loadConfig();
            whitelistManager.loadWhitelist();

            logger.info("✓ Configurations loaded successfully");

            registerListeners();
            registerCommands();
            registerAutoAliases();

            logger.info("✓ All systems initialized");
            logger.info("╔════════════════════════════════════════════╗");
            logger.info("║     AuthBridge is now ACTIVE and READY     ║");
            logger.info("╚════════════════════════════════════════════╝");

        } catch (Exception e) {
            logger.error("Failed to initialize AuthBridge", e);
            logger.error("Plugin will not work properly!");
        }
    }

    private void registerListeners() {
        server.getEventManager().register(
            this,
            new AuthListener(server, configManager, whitelistManager, playerHider, logger)
        );

        server.getEventManager().register(
            this,
            new TabCompleteListener(whitelistManager, logger)
        );
    }

    private void registerCommands() {
        server.getCommandManager().register(
            "server",
            new ServerCommand(server, configManager, whitelistManager, logger)
        );

        AliasCommand aliasCommand = new AliasCommand(server, configManager, logger);
        configManager.getCustomAliases().forEach((alias, command) -> {
            String aliasName = alias.substring(1).toLowerCase();
            server.getCommandManager().register(aliasName, aliasCommand);
            logger.info("✓ Custom alias registered: /" + aliasName + " → " + command);
        });
    }

    private void registerAutoAliases() {
        if (!configManager.isAutoAliasEnabled()) {
            logger.info("⚠ Auto-alias system is disabled");
            return;
        }

        logger.info("📋 Reading servers from velocity.toml...");

        server.getAllServers().forEach(registeredServer -> {
            String serverName = registeredServer.getServerInfo().getName();

            String authServer = configManager.getAuthServer();
            if (serverName.equalsIgnoreCase(authServer)) {
                logger.info("⊘ Skipping auth server: " + serverName);
                return;
            }

            AliasCommand autoAliasCommand = new AliasCommand(server, configManager, logger);

            server.getCommandManager().register(
                serverName.toLowerCase(),
                autoAliasCommand
            );

            logger.info("✓ Auto-alias registered: /" + serverName.toLowerCase() + 
                       " → /server " + serverName);
        });

        logger.info("✓ Auto-alias registration completed!");
    }

    public ProxyServer getServer() {
        return server;
    }

    public Logger getLogger() {
        return logger;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public PlayerHider getPlayerHider() {
        return playerHider;
    }
}