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

    private final Map<UUID, BossBar>       playerBars  = new ConcurrentHashMap<>();
    private final Map<UUID, ScheduledTask> playerTasks = new ConcurrentHashMap<>();

    public RaidBarManager(Object plugin, ProxyServer server,
                          ConfigManager configManager, Logger logger) {
        this.plugin        = plugin;
        this.server        = server;
        this.configManager = configManager;
        this.logger        = logger;
    }

    public void startTimer(Player player) {
        if (!configManager.isRaidBarEnabled()) return;

        stopTimer(player);

        int totalSeconds = configManager.getRaidBarTimer();
        int[] remaining  = {totalSeconds};

        BossBar bar = BossBar.bossBar(
            buildTitle(remaining[0]),
            1.0f,
            resolveColor(configManager.getRaidBarColor()),
            BossBar.Overlay.PROGRESS
        );

        player.showBossBar(bar);
        playerBars.put(player.getUniqueId(), bar);

        ScheduledTask task = server.getScheduler()
            .buildTask(plugin, () -> {
                if (!player.isActive()) {
                    cleanup(player.getUniqueId());
                    return;
                }

                remaining[0]--;

                float progress = totalSeconds > 0
                    ? Math.max(0f, (float) remaining[0] / totalSeconds)
                    : 0f;

                bar.progress(progress);
                bar.name(buildTitle(remaining[0]));

                if (remaining[0] <= 0) {
                    cleanup(player.getUniqueId());
                    String kickMsg = configManager.getMessage("raidbar-timeout");
                    player.disconnect(MessageUtils.colorize(
                        kickMsg != null && !kickMsg.isEmpty()
                            ? kickMsg
                            : "&cLogin time expired! Please reconnect."));
                }
            })
            .repeat(1L, TimeUnit.SECONDS)
            .schedule();

        playerTasks.put(player.getUniqueId(), task);
        logger.debug("RaidBar timer started for {} ({}s)", player.getUsername(), totalSeconds);
    }

    public void stopTimer(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);

        ScheduledTask task = playerTasks.remove(player.getUniqueId());
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
