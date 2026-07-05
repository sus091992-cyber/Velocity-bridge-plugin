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
 * Intercepts plugin-channel messages that travel from backend servers to players.
 *
 * Runs at PostOrder.LAST — this listener has the final say on what reaches
 * the client; no subsequent subscriber can re-introduce filtered data.
 *
 * Attack vectors covered:
 *
 *  1. minecraft:brand / MC|Brand (legacy)
 *     Backend servers (Paper, Purpur, etc.) broadcast their software name here.
 *     Hack clients read this to infer the server stack and which plugins may
 *     be installed. We block the real brand and re-send "Minecraft" so the
 *     server appears to be a vanilla instance.
 *
 *  2. minecraft:register / REGISTER (legacy)
 *     Backend servers register all their plugin-messaging channels here.
 *     Channel names are namespaced (e.g. "authbridge:auth", "luckperms:action")
 *     and directly expose the plugin list.
 *
 *     Strategy: strict ALLOWLIST — only explicit vanilla Minecraft channels may
 *     pass through. Everything else is stripped. This is more robust than a
 *     blacklist because any unknown plugin channel is blocked by default.
 */
public class PluginChannelListener {

    // Modern channel names
    private static final String BRAND_CHANNEL    = "minecraft:brand";
    private static final String REGISTER_CHANNEL = "minecraft:register";

    // Legacy channel names used by older backends / mixed-version setups via ViaVersion
    private static final String BRAND_LEGACY    = "MC|Brand";
    private static final String REGISTER_LEGACY = "REGISTER";

    /**
     * STRICT ALLOWLIST: the only minecraft:register entries that may reach clients.
     * Every other channel — regardless of namespace — is stripped before forwarding.
     *
     * Standard vanilla debug channels are kept for compatibility with vanilla debug
     * tools; they carry no plugin-identification information.
     */
    private static final Set<String> SAFE_CHANNELS = Set.of(
        "minecraft:debug/paths",
        "minecraft:debug/neighbors_update",
        "minecraft:debug/caves_and_cliffs_blending",
        "minecraft:debug/structures",
        "minecraft:debug/worldgen_attempt",
        "minecraft:debug/poi_ticket_count",
        "minecraft:debug/poi_added",
        "minecraft:debug/poi_removed",
        "minecraft:debug/village_sections",
        "minecraft:debug/goal_selector",
        "minecraft:debug/brain",
        "minecraft:debug/bee",
        "minecraft:debug/hive",
        "minecraft:debug/game_test_add_marker",
        "minecraft:debug/game_test_clear"
    );

    private final Logger logger;

    public PluginChannelListener(Logger logger) {
        this.logger = logger;
    }

    /**
     * LAST: runs after all other listeners so no subsequent subscriber can
     * re-introduce filtered channel data before the packet leaves for the client.
     */
    @Subscribe(order = PostOrder.LAST)
    public void onPluginMessage(PluginMessageEvent event) {
        // Only intercept backend → player direction
        if (!(event.getSource() instanceof ServerConnection)) return;
        if (!(event.getTarget() instanceof Player))           return;

        String channel = event.getIdentifier().getId();
        Player player  = (Player) event.getTarget();

        if (isBrandChannel(channel)) {
            handleBrand(event, player);
        } else if (isRegisterChannel(channel)) {
            handleRegister(event, player);
        }
    }

    // ── brand spoofing ────────────────────────────────────────────────────────

    private void handleBrand(PluginMessageEvent event, Player player) {
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        try {
            ChannelIdentifier id = MinecraftChannelIdentifier.from(BRAND_CHANNEL);
            player.sendPluginMessage(id, encodeBrand("Minecraft"));
        } catch (Exception e) {
            logger.debug("Brand spoof failed (non-critical): " + e.getMessage());
        }
    }

    /**
     * Encode brand as Minecraft protocol: VarInt(len) + UTF-8 bytes.
     * Safe for brands ≤ 127 bytes (single-byte VarInt).
     */
    private byte[] encodeBrand(String brand) {
        byte[] utf8 = brand.getBytes(StandardCharsets.UTF_8);
        byte[] out  = new byte[utf8.length + 1];
        out[0] = (byte) utf8.length;
        System.arraycopy(utf8, 0, out, 1, utf8.length);
        return out;
    }

    // ── channel-registration filtering (strict allowlist) ─────────────────────

    private void handleRegister(PluginMessageEvent event, Player player) {
        String raw     = new String(event.getData(), StandardCharsets.UTF_8);
        String[] names = raw.split("\0");

        // Keep ONLY channels in the safe vanilla allowlist
        String filtered = Arrays.stream(names)
            .filter(ch -> SAFE_CHANNELS.contains(ch.toLowerCase()))
            .collect(Collectors.joining("\0"));

        // Block the original packet regardless
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!filtered.isEmpty()) {
            try {
                ChannelIdentifier id = MinecraftChannelIdentifier.from(REGISTER_CHANNEL);
                player.sendPluginMessage(id, filtered.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                logger.debug("Channel register forward failed (non-critical): " + e.getMessage());
            }
        }
        // Empty filtered → packet suppressed entirely
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private boolean isBrandChannel(String ch) {
        return BRAND_CHANNEL.equals(ch) || BRAND_LEGACY.equals(ch);
    }

    private boolean isRegisterChannel(String ch) {
        return REGISTER_CHANNEL.equals(ch) || REGISTER_LEGACY.equals(ch);
    }
}
