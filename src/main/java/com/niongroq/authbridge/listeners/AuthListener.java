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
import com.niongroq.authbridge.managers.AuthServerGuard;
import com.niongroq.authbridge.managers.BossBarManager;
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
    private final BossBarManager bossBarManager;
    private final AuthServerGuard authServerGuard;
    private final Logger logger;

    /**
     * Players whose authentication state has been confirmed.
     * Cleared on every disconnect so a reconnecting player always starts fresh.
     */
    private final Set<UUID> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    /**
     * Players currently sitting on the auth server (unauthenticated).
     * A transition from this set → another server is the signal that AuthMe
     * has redirected them after a successful login/register.
     */
    private final Set<UUID> playersOnAuthServer = ConcurrentHashMap.newKeySet();

    public AuthListener(ProxyServer server, ConfigManager configManager,
                        WhitelistManager whitelistManager, PlayerHider playerHider,
                        BossBarManager bossBarManager, AuthServerGuard authServerGuard,
                        Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.whitelistManager = whitelistManager;
        this.playerHider = playerHider;
        this.bossBarManager = bossBarManager;
        this.authServerGuard = authServerGuard;
        this.logger = logger;
    }

    // ── public API used by TabCompleteListener ────────────────────────────────

    /** Returns true if the given player UUID is currently authenticated. */
    public boolean isAuthenticated(UUID uuid) {
        return authenticatedPlayers.contains(uuid);
    }

    /**
     * Externally mark a player as authenticated (e.g. via plugin messaging from
     * the backend AuthMe server).
     */
    public void markAuthenticated(UUID playerUuid, String playerName) {
        authenticatedPlayers.add(playerUuid);
        logger.debug("Player marked authenticated externally: " + playerName);
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
        bossBarManager.stopTimer(player);
    }

    // ── server connection events ──────────────────────────────────────────────

    /**
     * PRE-connect: block access to servers that are in the blocked-servers list.
     * Using the Pre event means the connection is denied before it is established,
     * not after — so the player never briefly appears on the blocked server.
     */
    @Subscribe(order = PostOrder.EARLY)
    public void onServerPreConnect(ServerPreConnectEvent event) {
        String target = event.getOriginalServer().getServerInfo().getName();

        // Always allow the auth server itself
        if (target.equalsIgnoreCase(configManager.getAuthServer())) return;

        if (configManager.getBlockedServers().contains(target.toLowerCase())) {
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            String msg = configManager.getMessage("server-blocked");
            event.getPlayer().sendMessage(MessageUtils.colorize(
                msg != null && !msg.isEmpty() ? msg : "&cYou cannot connect to that server."));
        }
    }

    /**
     * POST-connect: track auth-server presence and update player visibility.
     */
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        String current = event.getServer().getServerInfo().getName();
        String authSrv = configManager.getAuthServer();

        if (current.equalsIgnoreCase(authSrv)) {
            playersOnAuthServer.add(uuid);
            playerHider.hidePlayer(player);
            bossBarManager.startTimer(player);
            authServerGuard.protect(player);
        } else {
            if (playersOnAuthServer.contains(uuid)) {
                // Legitimate AuthMe post-login redirect: auth server → other server
                authenticatedPlayers.add(uuid);
                playersOnAuthServer.remove(uuid);
                bossBarManager.stopTimer(player);
                logger.debug("Auth state granted (auth-server transition): "
                        + player.getUsername());
            }
            playerHider.showPlayer(player);
        }
    }

    // ── command gating ────────────────────────────────────────────────────────

    /**
     * Runs at DEFAULT order — FakePluginListener (EARLY) runs first, so when it
     * has already denied a plugin-info command this handler sees result ≠ allowed
     * and exits immediately, preventing a duplicate "command-blocked" message.
     */
    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        if (!(event.getCommandSource() instanceof Player)) return;

        // If another listener (e.g. FakePluginListener) already handled this, stop here
        if (!event.getResult().isAllowed()) return;

        Player player = (Player) event.getCommandSource();
        String rawCommand = event.getCommand().toLowerCase();
        String commandName = extractCommandName(rawCommand);

        // ── globally blocked (plugins, ver, about …) — blocked for everyone ──
        if (whitelistManager.isGloballyBlockedCommand(commandName)) {
            event.setResult(CommandExecuteEvent.CommandResult.denied());
            // No message here: FakePluginListener sends the fake plugin list.
            // For any non-plugin-info command that ends up here, send generic block msg.
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

        // Authenticated players on the auth server cannot switch servers via commands
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

        // Strip args
        int space = command.indexOf(' ');
        if (space != -1) command = command.substring(0, space);

        // Strip namespace prefix (bukkit:pl → pl, spigot:plugins → plugins)
        // so namespaced variants hit the same whitelist / blocked checks
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
