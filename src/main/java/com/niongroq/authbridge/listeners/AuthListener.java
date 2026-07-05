package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.niongroq.authbridge.managers.ConfigManager;
import com.niongroq.authbridge.managers.WhitelistManager;
import com.niongroq.authbridge.managers.PlayerHider;
import com.niongroq.authbridge.utils.MessageUtils;
import fr.xephi.authme.api.v3.AuthMeApi;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AuthListener {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final WhitelistManager whitelistManager;
    private final PlayerHider playerHider;
    private final Logger logger;
    private final ConcurrentHashMap<String, Boolean> authenticatedPlayers;

    public AuthListener(ProxyServer server, ConfigManager configManager, 
                       WhitelistManager whitelistManager, PlayerHider playerHider, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.whitelistManager = whitelistManager;
        this.playerHider = playerHider;
        this.logger = logger;
        this.authenticatedPlayers = new ConcurrentHashMap<>();
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        String playerName = event.getPlayer().getUsername();
        if (isPlayerAuthenticated(playerName)) {
            authenticatedPlayers.put(playerName, true);
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String currentServer = event.getServer().getServerInfo().getName();

        if (currentServer.equals(configManager.getAuthServer())) {
            playerHider.hidePlayer(player);
        } else {
            playerHider.showPlayer(player);
        }
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getCommandSource();
        String command = event.getCommand().toLowerCase();
        String commandName = extractCommandName(command);

        String currentServer = getCurrentServerName(player);

        if (whitelistManager.isGloballyBlockedCommand(commandName)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            sendBlockedMessage(player, "command-blocked");
            return;
        }

        if (!isPlayerAuthenticated(player.getUsername())) {
            if (whitelistManager.isAlwaysAllowedCommand(commandName)) {
                return;
            }

            if (whitelistManager.isWhitelistedCommandOnAuth(commandName)) {
                return;
            }

            if (whitelistManager.isServerSwitchingCommand(commandName)) {
                event.setResult(CommandExecuteEvent.CommandResult.denied());
                sendBlockedMessage(player, "not-logged-in");
                return;
            }

            event.setResult(CommandExecuteEvent.CommandResult.denied());
            sendBlockedMessage(player, "not-logged-in");
            return;
        }

        if (currentServer.equals(configManager.getAuthServer())) {
            if (whitelistManager.isServerSwitchingCommand(commandName)) {
                event.setResult(CommandExecuteEvent.CommandResult.denied());
                sendBlockedMessage(player, "server-blocked");
                return;
            }
        }
    }

    private String extractCommandName(String command) {
        command = command.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex != -1) {
            command = command.substring(0, spaceIndex);
        }
        
        int colonIndex = command.indexOf(':');
        if (colonIndex != -1) {
            command = command.substring(colonIndex + 1);
        }
        
        return command.toLowerCase();
    }

    private String getCurrentServerName(Player player) {
        Optional<ServerConnection> connection = player.getCurrentServer();
        if (connection.isPresent()) {
            return connection.get().getServerInfo().getName();
        }
        return "unknown";
    }

    private boolean isPlayerAuthenticated(String playerName) {
        try {
            return AuthMeApi.getInstance().isAuthenticated(playerName);
        } catch (Exception e) {
            logger.debug("Failed to check authentication status for: " + playerName);
            return false;
        }
    }

    private void sendBlockedMessage(Player player, String messageKey) {
        String message = configManager.getMessage(messageKey);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(message));
        }
    }
}