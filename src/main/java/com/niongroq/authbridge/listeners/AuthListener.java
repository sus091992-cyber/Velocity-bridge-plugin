package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.niongroq.authbridge.managers.RaidBarManager;
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
    private final RaidBarManager raidBarManager;
    private final Logger logger;

    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> playersOnAuthServer   = ConcurrentHashMap.newKeySet();

    public AuthListener(ProxyServer server, ConfigManager configManager,
                        WhitelistManager whitelistManager, PlayerHider playerHider,
                        RaidBarManager raidBarManager, Logger logger) {
        this.server           = server;
        this.configManager    = configManager;
        this.whitelistManager = whitelistManager;
        this.playerHider      = playerHider;
        this.raidBarManager   = raidBarManager;
        this.logger           = logger;
    }

    // ── public API ────────────────────────────────────────────────────────────

    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    /**
     * Called via plugin messaging (authbridge:auth channel) when the backend
     * AuthMe confirms a login or register. Marks the player authenticated and,
     * if after-login.send is enabled, transfers them to the configured server.
     */
    public void markAuthenticated(UUID playerUuid, String playerName) {
        authenticatedPlayers.add(playerUuid);
        playersOnAuthServer.remove(playerUuid);
        logger.debug("Player marked authenticated externally: " + playerName);

        if (configManager.isAfterLoginSend()) {
            String target = configManager.getAfterLoginServer();
            server.getPlayer(playerUuid).ifPresent(player ->
                server.getServer(target).ifPresent(dest -> {
                    raidBarManager.stopTimer(player);
                    player.createConnectionRequest(dest).fireAndForget();
                    logger.debug("After-login redirect → " + target + " for " + playerName);
                })
            );
        }
    }

    // ── lifecycle events ──────────────────────────────────────────────────────

    @Subscribe
    public void onLogin(LoginEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        authenticatedPlayers.remove(uuid);
        playersOnAuthServer.remove(uuid);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        authenticatedPlayers.remove(uuid);
        playersOnAuthServer.remove(uuid);
        raidBarManager.stopTimer(player);
        playerHider.cleanupPlayer(uuid);
    }

    // ── server connection events ──────────────────────────────────────────────

    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        String target = event.getOriginalServer().getServerInfo().getName();

        if (target.equalsIgnoreCase(configManager.getAuthServer())) return;

        if (configManager.getBlockedServers().contains(target.toLowerCase())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            String msg = configManager.getMessage("server-blocked");
            event.getPlayer().sendMessage(MessageUtils.colorize(
                msg != null && !msg.isEmpty() ? msg : "&cYou cannot connect to that server."));
        }
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID   uuid   = player.getUniqueId();
        String current = event.getServer().getServerInfo().getName();
        String authSrv = configManager.getAuthServer();

        if (current.equalsIgnoreCase(authSrv)) {
            // Player arrived on auth server
            playersOnAuthServer.add(uuid);
            playerHider.hidePlayer(player);
            raidBarManager.startTimer(player);

        } else {
            if (playersOnAuthServer.contains(uuid)) {
                // Player transitioned auth → another server (AuthMe-driven redirect)
                authenticatedPlayers.add(uuid);
                playersOnAuthServer.remove(uuid);
                raidBarManager.stopTimer(player);
                logger.debug("Auth state granted (auth-server transition): "
                        + player.getUsername());

                // If after-login.send is on and they landed on the wrong server, redirect
                if (configManager.isAfterLoginSend()) {
                    String target = configManager.getAfterLoginServer();
                    if (!current.equalsIgnoreCase(target)) {
                        server.getServer(target).ifPresent(dest ->
                            player.createConnectionRequest(dest).fireAndForget()
                        );
                        logger.debug("After-login redirect → " + target
                                + " for " + player.getUsername());
                    }
                }
            }
            playerHider.showPlayer(player);
        }
    }

    // ── command gating ────────────────────────────────────────────────────────

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) return;
        if (!event.getResult().isAllowed()) return;

        Player player      = (Player) event.getCommandSource();
        String rawCommand  = event.getCommand().toLowerCase();
        String commandName = extractCommandName(rawCommand);

        if (whitelistManager.isGloballyBlockedCommand(commandName)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            return;
        }

        boolean authenticated = authenticatedPlayers.contains(player.getUniqueId());

        if (!authenticated) {
            if (whitelistManager.isAlwaysAllowedCommand(commandName)) return;
            if (whitelistManager.isWhitelistedCommandOnAuth(commandName)) return;

            event.setResult(CommandExecuteEvent.CommandResult.denied());
            sendMessage(player, "not-logged-in");
            return;
        }

        String currentServer = getCurrentServerName(player);
        if (currentServer.equalsIgnoreCase(configManager.getAuthServer())) {
            if (whitelistManager.isServerSwitchingCommand(commandName)) {
                event.setResult(CommandExecuteEvent.CommandResult.denied());
                sendMessage(player, "server-blocked");
            }
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String extractCommandName(String command) {
        command = command.trim();
        if (command.startsWith("/")) command = command.substring(1);

        int space = command.indexOf(' ');
        if (space != -1) command = command.substring(0, space);

        int colon = command.indexOf(':');
        if (colon != -1) command = command.substring(colon + 1);

        return command;
    }

    private String getCurrentServerName(Player player) {
        Optional<ServerConnection> conn = player.getCurrentServer();
        return conn.map(c -> c.getServerInfo().getName()).orElse("unknown");
    }

    private void sendMessage(Player player, String key) {
        String msg = configManager.getMessage(key);
        if (msg != null && !msg.isEmpty()) {
            player.sendMessage(MessageUtils.colorize(msg));
        }
    }
}
