package com.niongroq.authbridge.managers;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerHider {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;
    private final ConcurrentHashMap<String, Boolean> hiddenPlayers;

    public PlayerHider(ProxyServer server, ConfigManager configManager, Logger logger) {
        this.server = server;
        this.configManager = configManager;
        this.logger = logger;
        this.hiddenPlayers = new ConcurrentHashMap<>();
    }

    public void hidePlayer(Player player) {
        if (!configManager.isPlayerHiderEnabled()) {
            return;
        }

        try {
            Optional<ServerConnection> connection = player.getCurrentServer();
            if (connection.isPresent()) {
                String serverName = connection.get().getServerInfo().getName();
                if (serverName.equals(configManager.getAuthServer())) {
                    hiddenPlayers.put(player.getUsername(), true);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to hide player: " + player.getUsername(), e);
        }
    }

    public void showPlayer(Player player) {
        try {
            hiddenPlayers.remove(player.getUsername());
        } catch (Exception e) {
            logger.error("Failed to show player: " + player.getUsername(), e);
        }
    }

    public boolean isPlayerHidden(Player player) {
        return hiddenPlayers.containsKey(player.getUsername());
    }

    public void removeHiddenPlayer(String playerName) {
        hiddenPlayers.remove(playerName);
    }

    public ConcurrentHashMap<String, Boolean> getHiddenPlayers() {
        return hiddenPlayers;
    }
}