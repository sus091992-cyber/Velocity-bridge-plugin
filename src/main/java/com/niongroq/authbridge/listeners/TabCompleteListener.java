package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import com.niongroq.authbridge.managers.WhitelistManager;
import org.slf4j.Logger;

import java.util.Set;

/**
 * Tab-completion lockdown — runs at PostOrder.LAST so it is always
 * the FINAL word on what the client sees, regardless of what any other
 * listener added or left in the suggestion list.
 *
 * Attack vectors closed:
 *
 *  1. Unauthenticated players
 *     Suggestion list is replaced with a strict allowlist: only the bare
 *     command names the player is permitted to type are kept.
 *     Previously the list was only cleared when the typed prefix was NOT
 *     on the allowlist — meaning an unauthenticated player typing "/login"
 *     still received all plugin-namespaced suggestions.
 *
 *  2. Namespaced commands (namespace:command)
 *     Removed for ALL players at LAST priority.
 *     "authbridge:reload", "luckperms:user", "essentials:home" etc. directly
 *     expose installed plugin names. Every modern hack client (Meteor, Wurst,
 *     Aristois, Sigma, Impact…) uses tab-complete enumeration as its primary
 *     plugin-detection method.
 *
 *  3. Globally-blocked commands
 *     /plugins, /pl, /version, /ver, /about … removed from suggestions for
 *     every player — hack clients must not even discover these command names.
 */
public class TabCompleteListener {

    private final WhitelistManager whitelistManager;
    private final AuthListener authListener;
    private final Logger logger;

    public TabCompleteListener(AuthListener authListener,
                               WhitelistManager whitelistManager,
                               Logger logger) {
        this.authListener     = authListener;
        this.whitelistManager = whitelistManager;
        this.logger           = logger;
    }

    /**
     * LAST: this is the final sanitizer — runs after every other listener so no
     * subsequent subscriber can re-introduce sensitive suggestions.
     */
    @Subscribe(order = PostOrder.LAST)
    public void onTabComplete(TabCompleteEvent event) {
        Player player = event.getPlayer();
        boolean authenticated = authListener.isAuthenticated(player.getUniqueId());

        if (!authenticated) {
            // ── Unauthenticated: keep ONLY explicitly allowed command names ──────
            // Do this unconditionally — even if the player is typing "/login",
            // any plugin-namespaced suggestion that crept into the list is removed.
            Set<String> allowed = whitelistManager.getAllowedCommandNames();
            event.getSuggestions().removeIf(text -> {
                // Always strip namespaced suggestions first
                if (text.contains(":")) return true;
                String cmd = stripLeadingSlash(text);
                return !allowed.contains(cmd.toLowerCase());
            });
            return;
        }

        // ── Authenticated: remove namespaced and globally-blocked suggestions ──
        event.getSuggestions().removeIf(text -> {
            if (text.contains(":")) return true;                   // namespace:cmd leak
            String cmd = stripLeadingSlash(text);
            int space = cmd.indexOf(' ');
            if (space != -1) cmd = cmd.substring(0, space);
            return whitelistManager.isGloballyBlockedCommand(cmd); // /pl, /ver, etc.
        });
    }

    private String stripLeadingSlash(String s) {
        return s.startsWith("/") ? s.substring(1) : s;
    }
}
