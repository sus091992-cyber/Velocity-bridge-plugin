package com.niongroq.authbridge.bukkit;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

/**
 * Backend-side companion for the Velocity AuthBridge proxy plugin.
 *
 * Responsibilities:
 *  1. Hook AuthMe Reloaded's login/register events and forward a plugin
 *     message on the "authbridge:auth" channel so the proxy can mark the
 *     player authenticated and redirect them (see after-login in the
 *     proxy's config.yml). Without this, the proxy has no way of knowing
 *     a login/register ever happened.
 *  2. Vanish every player on this server from every other player while
 *     they wait to authenticate (real entity-level hide, not just tab
 *     list — install this jar only on the auth/login backend server).
 */
public final class AuthBridgeBukkit extends JavaPlugin {

    private String proxyChannel = "authbridge:auth";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        FileConfiguration cfg = getConfig();
        proxyChannel = cfg.getString("proxy-channel", "authbridge:auth");

        // Outgoing channel to the proxy. Velocity intercepts messages sent
        // on this channel (it registered it as a known channel) instead of
        // forwarding them to the client.
        getServer().getMessenger().registerOutgoingPluginChannel(this, proxyChannel);

        hookAuthMe();

        if (cfg.getBoolean("vanish-all.enabled", true)) {
            getServer().getPluginManager().registerEvents(new VanishListener(this), this);
            getLogger().info("Vanish-all enabled — players on this server are hidden from each other.");
        }

        getLogger().info("AuthBridge-Bukkit enabled (proxy channel: " + proxyChannel + ")");
    }

    /**
     * Hooks AuthMe Reloaded's LoginEvent/RegisterEvent reflectively (by class
     * name) instead of via a compile-time Maven dependency. AuthMe's package
     * has stayed stable ("fr.xephi.authme.events") across versions/forks for
     * years, but its Maven repository has not, so a hard dependency here
     * would be fragile. Reflection also means a missing/incompatible AuthMe
     * build degrades to a warning instead of a hard crash.
     */
    private void hookAuthMe() {
        Plugin authMe = getServer().getPluginManager().getPlugin("AuthMe");
        if (authMe == null || !authMe.isEnabled()) {
            getLogger().warning("AuthMe not found on this server — login/register notifications to the "
                + "proxy are disabled. Install AuthMe Reloaded on this server for after-login redirects to work.");
            return;
        }

        boolean loginHooked = registerAuthMeEvent("fr.xephi.authme.events.LoginEvent", "login");
        boolean registerHooked = registerAuthMeEvent("fr.xephi.authme.events.RegisterEvent", "register");

        if (loginHooked && registerHooked) {
            getLogger().info("Hooked AuthMe " + authMe.getDescription().getVersion()
                + " (login + register events)");
        } else {
            getLogger().severe("Found AuthMe, but its login/register event classes were not the expected "
                + "AuthMe Reloaded classes. Login/register notifications are disabled.");
        }
    }

    @SuppressWarnings("unchecked")
    private boolean registerAuthMeEvent(String eventClassName, String action) {
        try {
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName(eventClassName);
            Method getPlayerMethod = eventClass.getMethod("getPlayer");

            EventExecutor executor = (listener, event) -> {
                if (!eventClass.isInstance(event)) return;
                try {
                    Player player = (Player) getPlayerMethod.invoke(event);
                    notifyProxy(player, action);
                } catch (ReflectiveOperationException ex) {
                    getLogger().warning("Failed to read player from " + eventClassName + ": " + ex.getMessage());
                }
            };

            getServer().getPluginManager().registerEvent(
                eventClass, new Listener() {}, EventPriority.MONITOR, executor, this);
            return true;
        } catch (ReflectiveOperationException e) {
            getLogger().severe("Could not hook " + eventClassName + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends "action;uuid;username" to the proxy over the auth channel.
     * The proxy-side AuthChannelListener expects exactly this format.
     */
    public void notifyProxy(Player player, String action) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(action);
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(player.getName());
        player.sendPluginMessage(this, proxyChannel, out.toByteArray());
        getLogger().fine("Notified proxy: " + action + " for " + player.getName());
    }
}
