package com.niongroq.authbridge.managers;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final Path dataDirectory;
    private final Logger logger;
    private final File configFile;
    private ConfigurationNode config;

    private String authServer;
    private String fakePluginPrefix;
    private String pluginMessage;
    private String pluginFooter;
    private Map<String, String> messages;
    private Map<String, String> customAliases;
    private Set<String> blockedServers;
    private boolean playerHiderEnabled;
    private boolean hideInTabList;
    private boolean autoAliasEnabled;

    private boolean raidBarEnabled;
    private int     raidBarTimer;
    private String  raidBarColor;
    private String  raidBarMessage;

    private boolean afterLoginSend;
    private String  afterLoginServer;

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory  = dataDirectory;
        this.logger         = logger;
        this.configFile     = dataDirectory.resolve("config.yml").toFile();
        this.messages       = new HashMap<>();
        this.customAliases  = new HashMap<>();
        this.blockedServers = new HashSet<>();
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void loadConfig() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            if (!configFile.exists()) {
                logger.info("Creating default config.yml...");
                createDefaultConfig();
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .file(configFile)
                .build();
            config = loader.load();

            parseConfiguration();
            logger.info("Configuration loaded successfully");

        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }

    // ── getters ───────────────────────────────────────────────────────────────

    public String getAuthServer()                 { return authServer; }
    public String getFakePluginPrefix()           { return fakePluginPrefix; }
    public String getPluginMessage()              { return pluginMessage; }
    public String getPluginFooter()               { return pluginFooter; }
    public Map<String, String> getMessages()      { return messages; }
    public Map<String, String> getCustomAliases() { return customAliases; }
    public Set<String> getBlockedServers()        { return blockedServers; }
    public boolean isPlayerHiderEnabled()         { return playerHiderEnabled; }
    public boolean isHideInTabList()              { return hideInTabList; }
    public boolean isAutoAliasEnabled()           { return autoAliasEnabled; }

    public boolean isRaidBarEnabled()  { return raidBarEnabled; }
    public int     getRaidBarTimer()   { return raidBarTimer; }
    public String  getRaidBarColor()   { return raidBarColor; }
    public String  getRaidBarMessage() { return raidBarMessage; }

    public boolean isAfterLoginSend()   { return afterLoginSend; }
    public String  getAfterLoginServer(){ return afterLoginServer; }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void parseConfiguration() {
        authServer = config.node("auth-server").getString("auth");

        // blocked-servers
        blockedServers.clear();
        try {
            List<String> rawBlocked = config.node("blocked-servers").getList(String.class);
            if (rawBlocked != null) {
                rawBlocked.forEach(s -> blockedServers.add(s.toLowerCase()));
            }
        } catch (Exception e) {
            logger.warn("Could not parse blocked-servers list: " + e.getMessage());
        }

        // fake-plugin section
        ConfigurationNode fakeNode = config.node("fake-plugin");
        fakePluginPrefix = fakeNode.node("prefix").getString("&a&lNYX&f&lCORE");
        pluginMessage    = fakeNode.node("message").getString("&7Plugins (&a1&7): %plugin%");
        pluginFooter     = fakeNode.node("footer").getString("");

        // player-hider
        ConfigurationNode hiderNode = config.node("player-hider");
        playerHiderEnabled = hiderNode.node("enabled").getBoolean(true);
        hideInTabList      = hiderNode.node("hide-in-tablist").getBoolean(true);

        // raidbar
        ConfigurationNode rbNode = config.node("raidbar");
        raidBarEnabled = rbNode.node("enabled").getBoolean(true);
        raidBarTimer   = rbNode.node("timer").getInt(60);
        raidBarColor   = rbNode.node("color").getString("PINK");
        raidBarMessage = rbNode.node("message").getString("&fYou only have &c%timer_bos% &fseconds to login");

        // after-login
        ConfigurationNode alNode = config.node("after-login");
        afterLoginSend   = alNode.node("send").getBoolean(false);
        afterLoginServer = alNode.node("server").getString("lobby");

        // messages
        messages.clear();
        config.node("messages").childrenMap().forEach((key, node) ->
            messages.put(key.toString(), node.getString("")));

        // custom aliases
        customAliases.clear();
        config.node("custom-aliases").childrenMap().forEach((key, node) -> {
            String k = key.toString();
            if (!k.startsWith("/")) k = "/" + k;
            customAliases.put(k.toLowerCase(), node.getString(""));
        });

        // settings
        autoAliasEnabled = config.node("settings", "auto-alias", "enabled").getBoolean(true);
    }

    private void createDefaultConfig() throws IOException {
        String cfg =
            "# Name of your authentication server (must match velocity.toml)\n" +
            "auth-server: \"auth\"\n\n" +

            "# Servers completely inaccessible to players\n" +
            "blocked-servers:\n" +
            "  - \"admin\"\n" +
            "  - \"maintenance\"\n\n" +

            "# Custom command shortcuts  (alias → target command)\n" +
            "custom-aliases:\n" +
            "  \"/hub\": \"/server lobby\"\n" +
            "  \"/l\": \"/server lobby\"\n\n" +

            "# Hide players on the auth server from everyone (tab list)\n" +
            "player-hider:\n" +
            "  enabled: true\n" +
            "  hide-in-tablist: true\n\n" +

            "# Fake /plugins response\n" +
            "fake-plugin:\n" +
            "  prefix: \"&a&lNYX&f&lCORE\"\n" +
            "  message: \"&7Plugins (&a1&7): %plugin%\"\n" +
            "  footer: \"\"\n\n" +

            "# RaidBar countdown timer shown to players on the auth server\n" +
            "raidbar:\n" +
            "  enabled: true\n" +
            "  timer: 60\n" +
            "  color: PINK\n" +
            "  message: \"&fYou only have &c%timer_bos% &fseconds to login\"\n\n" +

            "# Send player to a specific server after successful login/register\n" +
            "after-login:\n" +
            "  send: true\n" +
            "  server: \"lobby\"\n\n" +

            "# Player-facing messages\n" +
            "messages:\n" +
            "  not-logged-in: \"&cYou must login first!\"\n" +
            "  command-blocked: \"&cThis command is blocked!\"\n" +
            "  server-blocked: \"&cYou cannot connect to that server!\"\n" +
            "  raidbar-timeout: \"&cLogin time expired! Please reconnect.\"\n\n" +

            "settings:\n" +
            "  auto-alias:\n" +
            "    enabled: true\n";

        Files.writeString(configFile.toPath(), cfg);
        logger.info("Default config.yml created");
    }
}
