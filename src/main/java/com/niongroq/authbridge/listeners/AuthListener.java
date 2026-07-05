package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.niongroq.authbridge.managers.ConfigManager;
import com.niongroq.authbridge.managers.WhitelistManager;
import com.niongroq.authbridge.managers.PlayerHider;
import com.niongroq.authbridge.utils.MessageUtils;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuthListener {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final WhitelistManager whitelistManager;
    private final PlayerHider playerHider;
    private final Logger logger;

    /**
     * UUIDs of players that have been confirmed as authenticated.
     * Authentication is confirmed by plugin messaging from the backend AuthMe server.
     * Players are removed on disconnect or when they reconnect.
     */
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * UUIDs of players currently on the auth server (and therefore unauthenticated).
     * Used to detect a legitimate auth-server → other-server transition.
     */
    private final Set<UUID> playersOnAuthServer = ConcurrentHashMap.newKeySet();

    public AuthListener(ProxyServer server, ConfigManager configManager,
                        WhitelistManager whitelistManager, PlayerHider playerHider, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.whitelistManager = whitelistManager;
        this.playerHider = playerHider;
        this.logger = logger;
    }

    /**
     * Mark a player as authenticated.
     * Should be called by plugin messaging from the backend AuthMe server after a
     * successful login/register event on the backend.
     */
    public void markAuthenticated(UUID playerUuid, String playerName) {
        authenticatedPlayers.add(playerUuid);
        logger.debug("Player authenticated via plugin messaging: " + playerName);
    }

    @Subscribe
    public void onLogin(LoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        // Reset auth state on every new proxy connection
        authenticatedPlayers.remove(uuid);
        playersOnAuthServer.remove(uuid);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        authenticatedPlayers.remove(uuid);
        playersOnAuthServer.remove(uuid);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String currentServer = event.getServer().getServerInfo().getName();
        String authServer = configManager.getAuthServer();

        if (currentServer.equalsIgnoreCase(authServer)) {
            // Player is on the auth server — track them and show hider
            playersOnAuthServer.add(uuid);
            playerHider.hidePlayer(player);
        } else {
            // Player moved away from a server
            if (playersOnAuthServer.contains(uuid)) {
                // They were on the auth server and moved off — this is the expected
                // AuthMe post-login redirect. Grant authenticated state.
                // Note: for stronger security, prefer plugin messaging from the backend.
                authenticatedPlayers.add(uuid);
                playersOnAuthServer.remove(uuid);
                logger.debug("Player left auth server, granting auth state: " + player.getUsername());
            }
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

        // Block globally blocked commands for everyone
        if (whitelistManager.isGloballyBlockedCommand(commandName)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            sendBlockedMessage(player, "command-blocked");
            return;
        }

        boolean authenticated = authenticatedPlayers.contains(player.getUniqueId());

        if (!authenticated) {
            // Always-allowed commands (e.g. /login, /register) pass through
            if (whitelistManager.isAlwaysAllowedCommand(commandName)) {
                return;
            }

            // Also allow other whitelisted auth-server commands
            if (whitelistManager.isWhitelistedCommandOnAuth(commandName)) {
                return;
            }

            // Block everything else for unauthenticated players
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            sendBlockedMessage(player, "not-logged-in");
            return;
        }

        // Authenticated players on auth server cannot use server-switching commands
        String currentServer = getCurrentServerName(player);
        if (currentServer.equalsIgnoreCase(configManager.getAuthServer())) {
            if (whitelistManager.isServerSwitchingCommand(commandName)) {
                event.setResult(CommandExecuteEvent.CommandResult.denied());
                sendBlockedMessage(player, "server-blocked");
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

    private void sendBlockedMessage(Player player, String messageKey) {
        String message = configManager.getMessage(messageKey);
        if (message != null && !message.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(message));
        }
    }
}
