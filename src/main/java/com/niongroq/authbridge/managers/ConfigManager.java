package com.niongroq.authbridge.managers;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {

    private final Path dataDirectory;
    private final Logger logger;
    private final File configFile;
    private ConfigurationNode config;

    private String authServer;
    private String fakePluginName;
    private String fakePluginColor;
    private String pluginMessage;
    private String pluginFooter;
    private Map<String, String> messages;
    private Map<String, String> customAliases;
    private boolean playerHiderEnabled;
    private int loginCacheDuration;
    private boolean autoAliasEnabled;

    public ConfigManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.configFile = dataDirectory.resolve("config.yml").toFile();
        this.messages = new HashMap<>();
        this.customAliases = new HashMap<>();
    }

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

    private void parseConfiguration() {
        authServer = config.node("auth-server").getString("auth");

        ConfigurationNode fakePluginNode = config.node("fake-plugin");
        fakePluginName = fakePluginNode.node("name").getString("NYXCRAFT");
        fakePluginColor = fakePluginNode.node("color").getString("&5");
        pluginMessage = fakePluginNode.node("message").getString("&7Plugins (&a1&7): %plugin%");
        pluginFooter = fakePluginNode.node("footer").getString("&7There are &a1&7 plugins installed.");

        ConfigurationNode playerHiderNode = config.node("player-hider");
        playerHiderEnabled = playerHiderNode.node("enabled").getBoolean(true);

        ConfigurationNode messagesNode = config.node("messages");
        messagesNode.childrenMap().forEach((key, node) -> {
            messages.put(key.toString(), node.getString(""));
        });

        ConfigurationNode aliasesNode = config.node("custom-aliases");
        aliasesNode.childrenMap().forEach((key, node) -> {
            String aliasKey = key.toString();
            if (!aliasKey.startsWith("/")) {
                aliasKey = "/" + aliasKey;
            }
            customAliases.put(aliasKey.toLowerCase(), node.getString(""));
        });

        loginCacheDuration = config.node("settings", "login-cache-duration").getInt(3600);
        autoAliasEnabled = config.node("settings", "auto-alias", "enabled").getBoolean(true);
    }

    private void createDefaultConfig() {
        try {
            String defaultConfig = "auth-server: \"auth\"\n\n" +
                "blocked-servers:\n" +
                "  - \"admin\"\n" +
                "  - \"maintenance\"\n\n" +
                "auth-required-servers:\n" +
                "  - \"lobby\"\n" +
                "  - \"survival\"\n\n" +
                "custom-aliases:\n" +
                "  \"/hub\": \"/server lobby\"\n" +
                "  \"/l\": \"/server lobby\"\n\n" +
                "player-hider:\n" +
                "  enabled: true\n" +
                "  hide-in-world: true\n" +
                "  hide-in-tablist: true\n" +
                "  game-mode: \"ADVENTURE\"\n\n" +
                "fake-plugin:\n" +
                "  name: \"NYXCRAFT\"\n" +
                "  color: \"&5\"\n" +
                "  message: \"&7Plugins (&a1&7): %plugin%\"\n" +
                "  footer: \"&7There are &a1&7 plugins installed.\"\n\n" +
                "messages:\n" +
                "  not-logged-in: \"&cYou must login first!\"\n" +
                "  command-blocked: \"&cThis command is blocked!\"\n\n" +
                "settings:\n" +
                "  enabled: true\n" +
                "  debug: false\n" +
                "  login-cache-duration: 3600\n" +
                "  auto-alias:\n" +
                "    enabled: true\n";

            Files.write(configFile.toPath(), defaultConfig.getBytes());
            logger.info("Default config.yml created successfully");
        } catch (IOException e) {
            logger.error("Failed to create default config", e);
        }
    }

    public String getAuthServer() {
        return authServer;
    }

    public String getFakePluginName() {
        return fakePluginName;
    }

    public String getFakePluginColor() {
        return fakePluginColor;
    }

    public String getPluginMessage() {
        return pluginMessage;
    }

    public String getPluginFooter() {
        return pluginFooter;
    }

    public Map<String, String> getMessages() {
        return messages;
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "&cMessage not found: " + key);
    }

    public Map<String, String> getCustomAliases() {
        return customAliases;
    }

    public boolean isPlayerHiderEnabled() {
        return playerHiderEnabled;
    }

    public int getLoginCacheDuration() {
        return loginCacheDuration;
    }

    public boolean isAutoAliasEnabled() {
        return autoAliasEnabled;
    }
}