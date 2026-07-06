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
    private boolean autoAliasEnabled;

    private boolean bossBarEnabled;
    private int     bossBarTimer;
    private String  bossBarColor;
    private String  bossBarMessage;

    // auth-server-guard
    private boolean authGuardEnabled;
    private boolean guardNoBlockInteract;
    private boolean guardNoDamage;
    private boolean guardNoHunger;
    private String  rconHost;
    private int     rconPort;
    private String  rconPassword;

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

    public boolean isBossBarEnabled()  { return bossBarEnabled; }
    public int     getBossBarTimer()   { return bossBarTimer; }
    public String  getBossBarColor()   { return bossBarColor; }
    public String  getBossBarMessage() { return bossBarMessage; }

    public boolean isAuthGuardEnabled()     { return authGuardEnabled; }
    public boolean isGuardNoBlockInteract() { return guardNoBlockInteract; }
    public boolean isGuardNoDamage()        { return guardNoDamage; }
    public boolean isGuardNoHunger()        { return guardNoHunger; }
    public String  getRconHost()            { return rconHost; }
    public int     getRconPort()            { return rconPort; }
    public String  getRconPassword()        { return rconPassword; }

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

        // bossbar
        ConfigurationNode bbNode = config.node("bossbar");
        bossBarEnabled = bbNode.node("enabled").getBoolean(true);
        bossBarTimer   = bbNode.node("timer").getInt(60);
        bossBarColor   = bbNode.node("color").getString("RED");
        bossBarMessage = bbNode.node("message").getString("&fShoma faghat &c%timer_bos% &fsanei vaght darid");

        // auth-server-guard
        ConfigurationNode guardNode = config.node("auth-server-guard");
        authGuardEnabled     = guardNode.node("enabled").getBoolean(false);
        guardNoBlockInteract = guardNode.node("no-block-interact").getBoolean(true);
        guardNoDamage        = guardNode.node("no-damage").getBoolean(true);
        guardNoHunger        = guardNode.node("no-hunger").getBoolean(true);
        ConfigurationNode rconNode = guardNode.node("rcon");
        rconHost     = rconNode.node("host").getString("127.0.0.1");
        rconPort     = rconNode.node("port").getInt(25575);
        rconPassword = rconNode.node("password").getString("");

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

            "# Auth-server player protection (requires RCON on the auth backend)\n" +
            "# Enable RCON in the auth server's server.properties:\n" +
            "#   enable-rcon=true  |  rcon.port=25575  |  rcon.password=<your-password>\n" +
            "auth-server-guard:\n" +
            "  enabled: false\n" +
            "  no-block-interact: true   # gamemode adventure (no break / place)\n" +
            "  no-damage: true           # resistance V effect\n" +
            "  no-hunger: true           # saturation 255 effect\n" +
            "  rcon:\n" +
            "    host: \"127.0.0.1\"\n" +
            "    port: 25575\n" +
            "    password: \"change-this\"\n\n" +

            "# BossBar countdown timer on the auth server\n" +
            "bossbar:\n" +
            "  enabled: true\n" +
            "  timer: 60\n" +
            "  color: RED\n" +
            "  message: \"&fYou only have &c%timer_bos% &fseconds to login\"\n\n" +

            "# Player-facing messages\n" +
            "messages:\n" +
            "  not-logged-in: \"&cYou must login first!\"\n" +
            "  command-blocked: \"&cThis command is blocked!\"\n" +
            "  server-blocked: \"&cYou cannot connect to that server!\"\n" +
            "  bossbar-timeout: \"&cLogin time expired! Please reconnect.\"\n\n" +

            "settings:\n" +
            "  auto-alias:\n" +
            "    enabled: true\n";

        Files.writeString(configFile.toPath(), cfg);
        logger.info("Default config.yml created");
    }
}
