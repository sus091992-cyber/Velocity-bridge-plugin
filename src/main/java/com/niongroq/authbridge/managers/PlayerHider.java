package com.niongroq.authbridge.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.player.TabListEntry;
import com.velocitypowered.api.scheduler.ScheduledTask;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Hides auth-server players from every other player's tab list and vice versa.
 *
 * Problem: when a player joins a backend server, the backend sends a full
 * PlayerInfo (tab-list) packet flood AFTER Velocity fires ServerConnectedEvent.
 * A single removeEntry() call loses the race.
 *
 * Solution: start a 1-second repeating scheduler task that continuously
 * re-enforces the hidden state for every player on the auth server.
 * The task cancels itself automatically when the player leaves auth.
 */
public class PlayerHider {

    private final Object plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;

    /** UUIDs of players currently sitting on the auth server. */
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    /** Per-player repeating enforcement tasks. */
    private final Map<UUID, ScheduledTask> enforcementTasks = new ConcurrentHashMap<>();

    public PlayerHider(Object plugin, ProxyServer server,
                       ConfigManager configManager, Logger logger) {
        this.plugin        = plugin;
        this.server        = server;
        this.configManager = configManager;
        this.logger        = logger;
    }

    /**
     * Called when a player connects to the auth server.
     * Applies hiding immediately AND starts a repeating task that
     * re-enforces the hidden state every second (to win the race against
     * the backend's tab-list packet flood).
     */
    public void hidePlayer(Player player) {
        if (!configManager.isPlayerHiderEnabled()) return;

        UUID uuid = player.getUniqueId();
        hiddenPlayers.add(uuid);

        // Immediate first pass
        applyHiding(player);

        // Cancel any leftover task for this UUID
        ScheduledTask old = enforcementTasks.remove(uuid);
        if (old != null) old.cancel();

        // Repeating enforcement — wins the race with the backend packet flood
        ScheduledTask task = server.getScheduler()
            .buildTask(plugin, () -> {
                if (!player.isActive() || !hiddenPlayers.contains(uuid)) {
                    ScheduledTask self = enforcementTasks.remove(uuid);
                    if (self != null) self.cancel();
                    return;
                }
                applyHiding(player);
            })
            .delay(200L, TimeUnit.MILLISECONDS)
            .repeat(1L, TimeUnit.SECONDS)
            .schedule();

        enforcementTasks.put(uuid, task);
        logger.debug("Hiding {} from all tab lists (enforcement active)", player.getUsername());
    }

    /**
     * Called when a player successfully leaves the auth server.
     * Cancels the enforcement task and restores mutual visibility.
     */
    public void showPlayer(Player player) {
        UUID uuid = player.getUniqueId();
        boolean wasHidden = hiddenPlayers.remove(uuid);
        if (!wasHidden) return;

        // Stop enforcement
        ScheduledTask task = enforcementTasks.remove(uuid);
        if (task != null) task.cancel();

        // Restore in all non-auth players' tab lists and vice versa
        for (Player other : server.getAllPlayers()) {
            if (other.getUniqueId().equals(uuid)) continue;
            if (hiddenPlayers.contains(other.getUniqueId())) continue;

            addToTabList(other, player);
            addToTabList(player, other);
        }

        logger.debug("Restored {} in all tab lists", player.getUsername());
    }

    /**
     * Called on full proxy disconnect — cleans up state.
     * No tab-list work needed; the player is already gone.
     */
    public void cleanupPlayer(UUID uuid) {
        hiddenPlayers.remove(uuid);
        ScheduledTask task = enforcementTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    public boolean isPlayerHidden(Player player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    // ── core hiding logic ─────────────────────────────────────────────────────

    /**
     * One enforcement pass:
     *  - Remove this auth player from every other player's tab list.
     *  - Remove every other player from this auth player's tab list.
     */
    private void applyHiding(Player player) {
        UUID uuid = player.getUniqueId();
        for (Player other : server.getAllPlayers()) {
            if (other.getUniqueId().equals(uuid)) continue;

            // Remove auth player from other's tab list
            other.getTabList().removeEntry(uuid);

            // Remove other from auth player's tab list
            player.getTabList().removeEntry(other.getUniqueId());
        }
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
