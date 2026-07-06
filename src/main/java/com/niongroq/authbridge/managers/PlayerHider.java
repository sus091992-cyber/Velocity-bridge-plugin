package com.niongroq.authbridge.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;
import org.slf4j.Logger;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hides auth-server players from every other player's tab list (and vice versa).
 *
 * Rules:
 *   - Any player who connects to the auth server disappears from every other
 *     player's tab list, and every other player disappears from their tab list.
 *   - No player on auth can see any other player — not even other auth players.
 *   - When a player leaves auth (post-login), they are restored to all
 *     non-auth players' tab lists and non-auth players are restored to theirs.
 *
 * In-game (world) visibility:
 *   Velocity cannot send entity hide/show packets directly.
 *   Configure your backend AuthMe plugin to set joining players to SPECTATOR
 *   mode so they are invisible to others in the game world.
 */
public class PlayerHider {

    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;

    /** UUIDs of players currently sitting on the auth server. */
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    public PlayerHider(ProxyServer server, ConfigManager configManager, Logger logger) {
        this.server        = server;
        this.configManager = configManager;
        this.logger        = logger;
    }

    /**
     * Called when a player connects to the auth server.
     * Removes them from every other player's tab list and wipes their own.
     */
    public void hidePlayer(Player player) {
        if (!configManager.isPlayerHiderEnabled()) return;

        hiddenPlayers.add(player.getUniqueId());

        for (Player other : server.getAllPlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) continue;

            // Remove the incoming auth player from everyone else's tab list
            other.getTabList().removeEntry(player.getUniqueId());

            // Remove everyone else from the incoming auth player's tab list
            player.getTabList().removeEntry(other.getUniqueId());
        }

        logger.debug("Hidden {} from all tab lists", player.getUsername());
    }

    /**
     * Called when a player successfully leaves the auth server.
     * Restores them in all non-auth players' tab lists and vice versa.
     */
    public void showPlayer(Player player) {
        boolean wasHidden = hiddenPlayers.remove(player.getUniqueId());
        if (!wasHidden) return;

        for (Player other : server.getAllPlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) continue;
            // Don't restore against players still hiding on auth
            if (hiddenPlayers.contains(other.getUniqueId())) continue;

            addToTabList(other, player);
            addToTabList(player, other);
        }

        logger.debug("Restored {} in all tab lists", player.getUsername());
    }

    /**
     * Called on full proxy disconnect — keeps the hidden set tidy.
     * No tab-list work needed because the player is already gone.
     */
    public void cleanupPlayer(UUID uuid) {
        hiddenPlayers.remove(uuid);
    }

    public boolean isPlayerHidden(Player player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void addToTabList(Player viewer, Player target) {
        try {
            boolean alreadyPresent = viewer.getTabList().getEntries().stream()
                .anyMatch(e -> e.getProfile().getId().equals(target.getUniqueId()));
            if (alreadyPresent) return;

            TabListEntry entry = TabListEntry.builder()
                .tabList(viewer.getTabList())
                .profile(target.getGameProfile())
                .gameMode(0)
                .latency((int) target.getPing())
                .build();
            viewer.getTabList().addEntry(entry);
        } catch (Exception e) {
            logger.debug("Could not restore {} in {}'s tab list: {}",
                target.getUsername(), viewer.getUsername(), e.getMessage());
        }
    }
}
