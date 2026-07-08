package com.niongroq.authbridge;

import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.niongroq.authbridge.listeners.AuthChannelListener;
import com.niongroq.authbridge.listeners.AuthListener;
import com.niongroq.authbridge.listeners.FakePluginListener;
import com.niongroq.authbridge.listeners.PluginChannelListener;
import com.niongroq.authbridge.listeners.TabCompleteListener;
import com.niongroq.authbridge.managers.RaidBarManager;
import com.niongroq.authbridge.managers.ConfigManager;
import com.niongroq.authbridge.managers.WhitelistManager;
import com.niongroq.authbridge.managers.PlayerHider;
import com.niongroq.authbridge.commands.ServerCommand;
import com.niongroq.authbridge.commands.AliasCommand;
import com.niongroq.authbridge.commands.SayVeloCommand;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import java.nio.file.Path;

@Plugin(
    id = "authbridge",
    name = "AuthBridge",
    version = "3.4.0",
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
    private RaidBarManager raidBarManager;

    @Inject
    public AuthBridge(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server        = server;
        this.logger        = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        try {
            logger.info("╔════════════════════════════════════════════╗");
            logger.info("║          AuthBridge v3.4.0 Loading         ║");
            logger.info("║              By: S1MPLE                    ║");
            logger.info("╚════════════════════════════════════════════╝");

            this.configManager    = new ConfigManager(dataDirectory, logger);
            this.whitelistManager = new WhitelistManager(dataDirectory, logger);

            configManager.loadConfig();
            whitelistManager.loadWhitelist();
            logger.info("✓ Configurations loaded successfully");

            this.playerHider    = new PlayerHider(this, server, configManager, logger);
            this.raidBarManager = new RaidBarManager(this, server, configManager, logger);

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
        AuthListener authListener = new AuthListener(
            server, configManager, whitelistManager, playerHider,
            raidBarManager, logger);

        FakePluginListener fakePluginListener = new FakePluginListener(configManager, logger);
        PluginChannelListener channelListener = new PluginChannelListener(logger);
        TabCompleteListener tabListener       = new TabCompleteListener(
            authListener, whitelistManager, logger);

        // Register the proxy<->backend auth channel BEFORE subscribing to
        // PluginMessageEvent for it, otherwise Velocity treats it as an
        // unregistered channel and forwards it straight to the client
        // instead of delivering it to our listener.
        server.getChannelRegistrar().register(
            MinecraftChannelIdentifier.from(AuthChannelListener.CHANNEL));
        AuthChannelListener authChannelListener = new AuthChannelListener(authListener, logger);

        server.getEventManager().register(this, authListener);
        server.getEventManager().register(this, fakePluginListener);
        server.getEventManager().register(this, channelListener);
        server.getEventManager().register(this, tabListener);
        server.getEventManager().register(this, authChannelListener);

        logger.info("✓ Listeners registered (auth, fake-plugin, channel-guard, tab-complete, auth-channel)");
    }

    private void registerCommands() {
        server.getCommandManager().register(
            "server",
            new ServerCommand(server, configManager, whitelistManager, logger));

        server.getCommandManager().register(
            "sayvelo",
            new SayVeloCommand(server, logger));
        logger.info("✓ Command /sayvelo registered");

        AliasCommand aliasCmd = new AliasCommand(server, configManager, logger);
        configManager.getCustomAliases().forEach((alias, target) -> {
            String name = alias.startsWith("/") ? alias.substring(1).toLowerCase() : alias.toLowerCase();
            server.getCommandManager().register(name, aliasCmd);
            logger.info("✓ Custom alias: /" + name + " → " + target);
        });
    }

    private void registerAutoAliases() {
        if (!configManager.isAutoAliasEnabled()) {
            logger.info("⚠ Auto-alias disabled");
            return;
        }

        logger.info("📋 Registering auto-aliases from velocity.toml...");
        String authServer = configManager.getAuthServer();

        server.getAllServers().forEach(reg -> {
            String name = reg.getServerInfo().getName();
            if (name.equalsIgnoreCase(authServer)) {
                logger.info("⊘ Skipping auth server: " + name);
                return;
            }
            if (configManager.getBlockedServers().contains(name.toLowerCase())) {
                logger.info("⊘ Skipping blocked server: " + name);
                return;
            }
            AliasCommand autoAlias = new AliasCommand(server, configManager, logger);
            server.getCommandManager().register(name.toLowerCase(), autoAlias);
            logger.info("✓ Auto-alias: /" + name.toLowerCase() + " → /server " + name);
        });

        logger.info("✓ Auto-alias registration completed");
    }

    public ProxyServer getServer()                { return server; }
    public Logger getLogger()                     { return logger; }
    public Path getDataDirectory()                { return dataDirectory; }
    public ConfigManager getConfigManager()       { return configManager; }
    public WhitelistManager getWhitelistManager() { return whitelistManager; }
    public PlayerHider getPlayerHider()           { return playerHider; }
}
