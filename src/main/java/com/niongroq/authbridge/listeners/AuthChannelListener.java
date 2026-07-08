package com.niongroq.authbridge.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
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
    private final Logger logger;

    public AuthChannelListener(AuthListener authListener, Logger logger) {
        this.authListener = authListener;
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
            authListener.markAuthenticated(uuid, username);
            logger.debug("Received '{}' confirmation for {} ({}) from backend", action, username, uuidStr);
        } catch (Exception e) {
            logger.error("Failed to parse message on {}: {}", CHANNEL, e.getMessage());
        }
    }
}
