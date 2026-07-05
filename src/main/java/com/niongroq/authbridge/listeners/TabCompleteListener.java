package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import com.niongroq.authbridge.managers.WhitelistManager;
import org.slf4j.Logger;

/**
 * Hardens tab-completion against plugin-detection attacks.
 *
 * Covered attack vectors:
 *
 *  1. Unauthenticated players
 *     Their suggestion list is cleared entirely (except for the commands they
 *     are explicitly allowed to type). Hack clients that tab-complete "/" on
 *     join to enumerate available commands get an empty result.
 *
 *  2. Namespaced commands  (namespace:command)
 *     Even for authenticated players, all suggestions containing ":" are
 *     removed. Suggestions like "authbridge:reload", "luckperms:user",
 *     "essentials:eco" directly expose installed plugin names. Every modern
 *     hack client (Meteor, Wurst, Aristois, Sigma, Impact …) uses this as its
 *     primary plugin-detection method.
 *
 *  3. Globally-blocked commands
 *     /plugins, /pl, /version, /ver, /about … are removed from suggestions
 *     for everyone so hack clients cannot even discover that these commands
 *     exist on the proxy.
 *
 * Runs at PostOrder.EARLY so it fires before FakePluginListener and any other
 * subscriber that might re-add suggestions.
 */
public class TabCompleteListener {

    private final WhitelistManager whitelistManager;
    private final AuthListener authListener;   // shared source of auth truth
    private final Logger logger;

    public TabCompleteListener(AuthListener authListener,
                               WhitelistManager whitelistManager,
                               Logger logger) {
        this.authListener    = authListener;
        this.whitelistManager = whitelistManager;
        this.logger           = logger;
    }

    @Subscribe(order = PostOrder.EARLY)
    public void onTabComplete(TabCompleteEvent event) {
        Player player  = event.getPlayer();
        String partial = event.getPartialMessage().toLowerCase().trim();
        String cmdName = extractCommandName(partial);

        boolean authenticated = authListener.isAuthenticated(player.getUniqueId());

        if (!authenticated) {
            // ── Unauthenticated: clear everything unless the partial input is
            //    already one of the explicitly allowed commands (login / register …)
            if (!whitelistManager.isAlwaysAllowedCommand(cmdName)
                    && !whitelistManager.isWhitelistedCommandOnAuth(cmdName)) {
                event.getSuggestions().clear();
            }
            return;
        }

        // ── Authenticated: strip namespaced and globally-blocked suggestions ──
        // In Velocity 3.x, getSuggestions() returns List<String>
        event.getSuggestions().removeIf(text -> {
            // 1. Remove anything with ":" — these are plugin:command namespaces
            if (text.contains(":")) return true;

            // 2. Remove globally blocked commands (/pl, /ver, /about …)
            String cleaned = text.startsWith("/") ? text.substring(1) : text;
            int space = cleaned.indexOf(' ');
            if (space != -1) cleaned = cleaned.substring(0, space);
            return whitelistManager.isGloballyBlockedCommand(cleaned);
        });
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String extractCommandName(String command) {
        if (command.startsWith("/")) command = command.substring(1);
        int space = command.indexOf(' ');
        return space != -1 ? command.substring(0, space) : command;
    }
}
