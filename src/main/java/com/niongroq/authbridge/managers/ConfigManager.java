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
    private Set<String> blockedServers;        // fix: was never loaded before
    private boolean playerHiderEnabled;
    private boolean autoAliasEnabled;

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger        = logger;
        this.configFile    = dataDirectory.resolve("config.yml").toFile();
        this.messages      = new HashMap<>();
        this.customAliases = new HashMap<>();
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

    public String getAuthServer()         { return authServer; }
    public String getFakePluginPrefix()   { return fakePluginPrefix; }
    public String getPluginMessage()      { return pluginMessage; }
    public String getPluginFooter()       { return pluginFooter; }
    public Map<String, String> getMessages()      { return messages; }
    public Map<String, String> getCustomAliases() { return customAliases; }
    public boolean isPlayerHiderEnabled()         { return playerHiderEnabled; }
    public boolean isAutoAliasEnabled()           { return autoAliasEnabled; }

    /**
     * Lower-cased set of server names that players should never be able to reach.
     * Was previously parsed but never stored — fix for ServerCommand blocked-server check.
     */
    public Set<String> getBlockedServers() { return blockedServers; }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    // ── private ───────────────────────────────────────────────────────────────

    private void parseConfiguration() {
        authServer = config.node("auth-server").getString("auth");

        // blocked-servers list (fix: was missing from previous implementation)
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
        pluginFooter    = fakeNode.node("footer").getString("&7There are &a1&7 plugins installed.");

        // player-hider
        playerHiderEnabled = config.node("player-hider", "enabled").getBoolean(true);

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

            "# Hide players from each other while on the auth server\n" +
            "player-hider:\n" +
            "  enabled: true\n\n" +

            "# Fake /plugins response\n" +
            "fake-plugin:\n" +
            "  prefix: \"&a&lNYX&f&lCORE\"\n" +
            "  message: \"&7Plugins (&a1&7): %plugin%\"\n" +
            "  footer: \"\"\n\n" +

            "# Player-facing messages\n" +
            "messages:\n" +
            "  not-logged-in: \"&cYou must login first!\"\n" +
            "  command-blocked: \"&cThis command is blocked!\"\n" +
            "  server-blocked: \"&cYou cannot connect to that server!\"\n\n" +

            "settings:\n" +
            "  auto-alias:\n" +
            "    enabled: true\n";

        Files.writeString(configFile.toPath(), cfg);
        logger.info("Default config.yml created");
    }
}
