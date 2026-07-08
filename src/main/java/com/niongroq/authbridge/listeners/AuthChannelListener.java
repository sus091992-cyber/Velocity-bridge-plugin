package com.niongroq.authbridge.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.niongroq.authbridge.managers.ConfigManager;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Listens on the "authbridge:auth" plugin-messaging channel for notifications
 * sent by the backend auth server (e.g. a small AuthMe-hook plugin) when a
 * player successfully logs in or registers.
 *
 * Wire format (BungeeCord-style DataOutput/DataInput, UTF-8 strings):
 *   writeUTF(action)   -- "login" or "register"
 *   writeUTF(uuid)     -- player's UUID, as returned by UUID#toString()
 *   writeUTF(username) -- player's username
 *
 * Without this listener, nothing ever calls {@link AuthListener#markAuthenticated},
 * so successful logins/registers on the backend never propagate to the proxy and
 * players are never redirected to the after-login server.
 */
public class AuthChannelListener {

    public static final String CHANNEL = "authbridge:auth";

    private final AuthListener authListener;
    private final ConfigManager configManager;
    private final Logger logger;

    public AuthChannelListener(AuthListener authListener, ConfigManager configManager, Logger logger) {
        this.authListener = authListener;
        this.configManager = configManager;
        this.logger = logger;
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!event.getIdentifier().getId().equals(CHANNEL)) return;

        // Only trust messages coming from a backend server, never from a client.
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!(event.getTarget() instanceof Player)) return;

        // Consume the message — it is internal proxy<->backend protocol and
        // must never reach the client.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ServerConnection source = (ServerConnection) event.getSource();
        Player targetPlayer = (Player) event.getTarget();

        // Only the configured auth server (where AuthMe actually runs) is
        // trusted to confirm authentication. A compromised/rogue backend
        // server cannot forge a confirmation just by sending on this
        // channel — it must also be the auth server the player is
        // currently connected through.
        String sourceServerName = source.getServerInfo().getName();
        if (!sourceServerName.equalsIgnoreCase(configManager.getAuthServer())) {
            logger.warn("Ignoring {} message from non-auth server '{}' (expected '{}')",
                CHANNEL, sourceServerName, configManager.getAuthServer());
            return;
        }

        try {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String action = in.readUTF();
            String uuidStr = in.readUTF();
            String username = in.readUTF();

            if (!action.equalsIgnoreCase("login") && !action.equalsIgnoreCase("register")) {
                logger.warn("Unknown action '{}' received on {} from backend", action, CHANNEL);
                return;
            }

            UUID uuid = UUID.fromString(uuidStr);

            // The payload UUID must match the player this message is actually
            // addressed to (Velocity resolves the target from the physical
            // connection the backend sent it on). This prevents the auth
            // server from ever confirming authentication for a UUID other
            // than the one the message channel is bound to.
            if (!uuid.equals(targetPlayer.getUniqueId())) {
                logger.warn("Ignoring {} message: payload UUID {} does not match message target {} ({})",
                    CHANNEL, uuidStr, targetPlayer.getUsername(), targetPlayer.getUniqueId());
                return;
            }

            authListener.markAuthenticated(uuid, username);
            logger.debug("Received '{}' confirmation for {} ({}) from backend", action, username, uuidStr);
        } catch (Exception e) {
            logger.error("Failed to parse message on {}: {}", CHANNEL, e.getMessage());
        }
    }
}
