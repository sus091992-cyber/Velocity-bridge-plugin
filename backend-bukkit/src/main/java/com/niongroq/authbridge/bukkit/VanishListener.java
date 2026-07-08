package com.niongroq.authbridge.bukkit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Makes every player on this server invisible to every other player on this
 * server — real entity-level hiding via the Bukkit API, not just a tab-list
 * trick. Install this jar only on the backend auth/login server: if 100
 * players are waiting to log in, none of them can see or be seen by any of
 * the other 99.
 *
 * Runs one tick after join so the joining player's own entity has finished
 * spawning before we start hiding/un-hiding.
 */
public class VanishListener implements Listener {

    private final AuthBridgeBukkit plugin;

    public VanishListener(AuthBridgeBukkit plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player joined = event.getPlayer();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player other : plugin.getServer().getOnlinePlayers()) {
                    if (other.getUniqueId().equals(joined.getUniqueId())) continue;

                    // Hide the newly joined player from everyone already online...
                    other.hidePlayer(plugin, joined);
                    // ...and hide everyone already online from the new player.
                    joined.hidePlayer(plugin, other);
                }
            }
        }.runTaskLater(plugin, 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player left = event.getPlayer();
        for (Player other : plugin.getServer().getOnlinePlayers()) {
            if (other.getUniqueId().equals(left.getUniqueId())) continue;
            other.showPlayer(plugin, left);
        }
    }
}
