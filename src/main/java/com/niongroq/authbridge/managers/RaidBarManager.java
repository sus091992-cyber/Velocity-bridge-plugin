package com.niongroq.authbridge.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.bossbar.BossBar;
import com.niongroq.authbridge.utils.MessageUtils;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class RaidBarManager {

    private final Object plugin;
    private final ProxyServer server;
    private final ConfigManager configManager;
    private final Logger logger;

    private final Map<UUID, BossBar>       playerBars    = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> playerTasks   = new ConcurrentHashMap<>();
    /** Pending delayed-start tasks so stopTimer() can cancel them before they fire. */
    private final Map<UUID, ScheduledTask> pendingStarts = new ConcurrentHashMap<>();

    public RaidBarManager(Object plugin, ProxyServer server,
                          ConfigManager configManager, Logger logger) {
        this.plugin        = plugin;
        this.server        = server;
        this.configManager = configManager;
        this.logger        = logger;
    }

    /**
     * Start the countdown for a player.
     * We delay 600 ms before showing the bar so the player is fully connected
     * and the backend's initial packet flood has settled.
     */
    public void startTimer(Player player) {
        if (!configManager.isRaidBarEnabled()) return;

        // Cancel any previous timer or pending start
        stopTimer(player);

        int totalSeconds    = configManager.getRaidBarTimer();
        int[] remaining     = {totalSeconds};
        UUID uuid           = player.getUniqueId();

        // Delay the actual bar creation by 600 ms
        ScheduledTask pending = server.getScheduler()
            .buildTask(plugin, () -> {
                pendingStarts.remove(uuid);

                if (!player.isActive()) return;

                BossBar bar = BossBar.bossBar(
                    buildTitle(remaining[0]),
                    1.0f,
                    resolveColor(configManager.getRaidBarColor()),
                    BossBar.Overlay.PROGRESS
                );

                player.showBossBar(bar);
                playerBars.put(uuid, bar);

                ScheduledTask tick = server.getScheduler()
                    .buildTask(plugin, () -> {
                        if (!player.isActive()) {
                            cleanup(uuid);
                            return;
                        }

                        remaining[0]--;

                        float progress = totalSeconds > 0
                            ? Math.max(0f, (float) remaining[0] / totalSeconds)
                            : 0f;

                        bar.progress(progress);
                        bar.name(buildTitle(remaining[0]));

                        if (remaining[0] <= 0) {
                            cleanup(uuid);
                            String kickMsg = configManager.getMessage("raidbar-timeout");
                            player.disconnect(MessageUtils.colorize(
                                kickMsg != null && !kickMsg.isEmpty()
                                    ? kickMsg
                                    : "&cLogin time expired! Please reconnect."));
                        }
                    })
                    .repeat(1L, TimeUnit.SECONDS)
                    .schedule();

                playerTasks.put(uuid, tick);
                logger.debug("RaidBar started for {} ({}s)", player.getUsername(), totalSeconds);
            })
            .delay(600L, TimeUnit.MILLISECONDS)
            .schedule();

        pendingStarts.put(uuid, pending);
    }

    public void stopTimer(Player player) {
        UUID uuid = player.getUniqueId();

        // Cancel pending delayed start
        ScheduledTask pending = pendingStarts.remove(uuid);
        if (pending != null) pending.cancel();

        BossBar bar = playerBars.remove(uuid);
        if (bar != null) player.hideBossBar(bar);

        ScheduledTask task = playerTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private void cleanup(UUID uuid) {
        ScheduledTask task = playerTasks.remove(uuid);
        if (task != null) task.cancel();
        playerBars.remove(uuid);
    }

    private net.kyori.adventure.text.Component buildTitle(int seconds) {
        String raw = configManager.getRaidBarMessage()
            .replace("%timer_bos%", String.valueOf(seconds));
        return MessageUtils.colorize(raw);
    }

    private BossBar.Color resolveColor(String name) {
        try {
            return BossBar.Color.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown raidbar color '{}', falling back to PINK", name);
            return BossBar.Color.PINK;
        }
    }
}
