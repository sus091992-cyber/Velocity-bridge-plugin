package com.niongroq.authbridge.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.niongroq.authbridge.rcon.RconClient;
import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Applies game-mechanic protections to players on the auth server via RCON:
 *
 *   - No block break / place  →  gamemode adventure
 *   - No damage               →  resistance effect (level V = 100 % reduction)
 *   - No hunger               →  saturation effect (level 255, hidden particles)
 *
 * Requires RCON to be enabled on the auth backend server.
 * server.properties on the auth server must have:
 *     enable-rcon=true
 *     rcon.port=25575
 *     rcon.password=<same as config>
 */
public class AuthServerGuard {

    private final Object        plugin;
    private final ProxyServer   server;
    private final ConfigManager configManager;
    private final Logger        logger;

    public AuthServerGuard(Object plugin, ProxyServer server,
                           ConfigManager configManager, Logger logger) {
        this.plugin        = plugin;
        this.server        = server;
        this.configManager = configManager;
        this.logger        = logger;
    }

    /**
     * Call when a player lands on the auth server.
     * Runs RCON commands asynchronously after a short delay to ensure the
     * player is fully loaded into the backend world before commands fire.
     */
    public void protect(Player player) {
        if (!configManager.isAuthGuardEnabled()) return;

        server.getScheduler()
            .buildTask(plugin, () -> applyProtection(player.getUsername()))
            .delay(20L, TimeUnit.MILLISECONDS)
            .schedule();
    }

    // ── internals ─────────────────────────────────────────────────────────────

    private void applyProtection(String playerName) {
        try (RconClient rcon = new RconClient()) {
            rcon.connect(
                configManager.getRconHost(),
                configManager.getRconPort(),
                configManager.getRconPassword()
            );

            if (configManager.isGuardNoBlockInteract()) {
                rcon.execute("gamemode adventure " + playerName);
            }
            if (configManager.isGuardNoDamage()) {
                // Resistance V = 100 % damage reduction; hidden particles
                rcon.execute("effect give " + playerName
                    + " minecraft:resistance 99999 4 true");
            }
            if (configManager.isGuardNoHunger()) {
                // Saturation 255 refills hunger every tick; hidden particles
                rcon.execute("effect give " + playerName
                    + " minecraft:saturation 99999 255 true");
            }

            logger.debug("Auth guard applied for {}", playerName);

        } catch (Exception e) {
            logger.warn("Auth guard RCON failed for {}: {}", playerName, e.getMessage());
            logger.debug("Enable RCON on the auth server and set rcon.* in config.yml");
        }
    }
}
