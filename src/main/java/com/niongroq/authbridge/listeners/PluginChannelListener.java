package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Intercepts plugin-channel messages sent from backend servers to players.
 *
 * Attack vectors covered:
 *
 *  1. minecraft:brand
 *     Backend servers (Paper, Purpur, etc.) announce their software name here.
 *     Hack clients (Meteor, Wurst, Impact, Sigma…) read this to detect the
 *     server type and infer which anti-cheat or auth plugin is running.
 *     → We block the real brand and re-send "Minecraft" to the client.
 *
 *  2. minecraft:register
 *     Backend servers register the plugin-messaging channels they support.
 *     The channel names often include the plugin ID (e.g. "authbridge:auth",
 *     "luckperms:action"). Hack clients read this list on connect.
 *     → We strip every channel whose namespace matches a known plugin/framework
 *       prefix before forwarding the registration packet.
 */
public class PluginChannelListener {

    private static final String BRAND_CHANNEL    = "minecraft:brand";
    private static final String REGISTER_CHANNEL = "minecraft:register";

    /**
     * Plugin / framework namespaces whose channel registrations must be hidden.
     * Add any new plugin you install here to keep it invisible.
     */
    private static final Set<String> SENSITIVE_NAMESPACES = Set.of(
        // Auth / security plugins
        "authbridge", "authme", "nlogin", "fastlogin",
        // Proxy infrastructure
        "velocity", "waterfall", "bungeecord", "bungee", "lilypad",
        // Common Bukkit/Spigot frameworks & plugins
        "spigot", "paper", "purpur", "pufferfish",
        "essentials", "essentialsx",
        "luckperms", "permissionsex",
        "worldguard", "worldedit", "fawe",
        "coreprotect",
        "citizens", "npc",
        "mythicmobs", "mm",
        "protocollib", "protocolsupport",
        "viaversion", "viabackwards", "viarewind",
        "geyser", "floodgate",
        "advancedserverlist",
        "imageonmap",
        // Modloaders / mod environments
        "fabric", "forge", "quilt", "neoforge",
        "fml", "mcp",
        // Generic catch-all: remove channels in the Minecraft namespace that
        // are not standard vanilla channels (standard ones are restored below)
        "minecraft"
    );

    /**
     * Standard vanilla Minecraft channels that are safe to forward even when
     * their namespace is "minecraft:". Everything else in that namespace is
     * server-side and may reveal plugin info.
     */
    private static final Set<String> ALLOWED_MINECRAFT_CHANNELS = Set.of(
        "minecraft:brand",         // we handle this separately — allow forwarding of our spoofed brand
        "minecraft:debug/paths",
        "minecraft:debug/neighbors_update",
        "minecraft:debug/caves_and_cliffs_blending"
    );

    private final Logger logger;

    public PluginChannelListener(Logger logger) {
        this.logger = logger;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onPluginMessage(PluginMessageEvent event) {
        // Only care about backend-server → player direction
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!(event.getTarget() instanceof Player))           return;

        String channel = event.getIdentifier().getId();
        Player player  = (Player) event.getTarget();

        if (channel.equals(BRAND_CHANNEL)) {
            handleBrand(event, player);
        } else if (channel.equals(REGISTER_CHANNEL)) {
            handleRegister(event, player);
        }
    }

    // ── brand spoofing ────────────────────────────────────────────────────────

    private void handleBrand(PluginMessageEvent event, Player player) {
        // Block the real brand packet from reaching the client
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        // Re-send a neutral brand — "Minecraft" looks like a vanilla server
        try {
            ChannelIdentifier brandId = MinecraftChannelIdentifier.from(BRAND_CHANNEL);
            player.sendPluginMessage(brandId, encodeBrand("Minecraft"));
        } catch (Exception e) {
            // If sending fails, the player simply gets no brand — still better
            // than leaking the real server software name.
            logger.debug("Brand spoofing send failed (non-critical): " + e.getMessage());
        }
    }

    /**
     * Encode a brand string the same way Minecraft's protocol does:
     *   VarInt(length) followed by UTF-8 bytes.
     * For brands ≤ 127 bytes the VarInt is always one byte.
     */
    private byte[] encodeBrand(String brand) {
        byte[] utf8 = brand.getBytes(StandardCharsets.UTF_8);
        byte[] out  = new byte[utf8.length + 1];
        out[0] = (byte) utf8.length;
        System.arraycopy(utf8, 0, out, 1, utf8.length);
        return out;
    }

    // ── channel registration filtering ───────────────────────────────────────

    private void handleRegister(PluginMessageEvent event, Player player) {
        // Format: null-terminated channel names concatenated
        String raw      = new String(event.getData(), StandardCharsets.UTF_8);
        String[] names  = raw.split("\0");
        String filtered = Arrays.stream(names)
            .filter(ch -> !isSensitive(ch))
            .collect(Collectors.joining("\0"));

        if (filtered.equals(raw)) {
            // Nothing was filtered — let the original packet through unchanged
            return;
        }

        // Block the original packet
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!filtered.isEmpty()) {
            // Forward only the safe subset of channels
            try {
                ChannelIdentifier regId = MinecraftChannelIdentifier.from(REGISTER_CHANNEL);
                player.sendPluginMessage(regId, filtered.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                logger.debug("Channel register filter send failed (non-critical): " + e.getMessage());
            }
        }
        // If filtered is empty we just suppress the packet entirely
    }

    /**
     * Returns true if the given channel name should be hidden from clients.
     */
    private boolean isSensitive(String channel) {
        if (channel == null || channel.isEmpty()) return true;

        String lower = channel.toLowerCase();

        // Special case: always allow whitelisted vanilla minecraft: channels
        if (lower.startsWith("minecraft:") && ALLOWED_MINECRAFT_CHANNELS.contains(lower)) {
            return false;
        }

        // Check if the channel namespace matches any sensitive prefix
        int colon = lower.indexOf(':');
        String namespace = colon != -1 ? lower.substring(0, colon) : lower;
        return SENSITIVE_NAMESPACES.contains(namespace);
    }
}
