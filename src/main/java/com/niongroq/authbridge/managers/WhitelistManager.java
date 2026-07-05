package com.niongroq.authbridge.managers;

import org.slf4j.Logger;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class WhitelistManager {

    private final Path dataDirectory;
    private final Logger logger;
    private final File whitelistFile;
    private ConfigurationNode whitelist;

    private Set<String> whitelistedCommandsOnAuth;
    private Set<String> serverSwitchingCommands;
    private Set<String> globallyBlockedCommands;
    private Set<String> alwaysAllowedCommands;

    public WhitelistManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.whitelistFile = dataDirectory.resolve("whitelist.yml").toFile();
        this.whitelistedCommandsOnAuth = new HashSet<>();
        this.serverSwitchingCommands = new HashSet<>();
        this.globallyBlockedCommands = new HashSet<>();
        this.alwaysAllowedCommands = new HashSet<>();
    }

    public void loadWhitelist() {
        try {
            if (!whitelistFile.exists()) {
                logger.info("Creating default whitelist.yml...");
                createDefaultWhitelist();
            }

            YamlConfigurationLoader loader = YamlConfigurationLoader.builder()
                .file(whitelistFile)
                .build();
            whitelist = loader.load();

            parseWhitelist();
            logger.info("Whitelist loaded successfully");

        } catch (IOException e) {
            logger.error("Failed to load whitelist", e);
        }
    }

    private void parseWhitelist() {
        ConfigurationNode authNode = whitelist.node("whitelisted-commands-on-auth");
        authNode.childrenList().forEach(node -> {
            whitelistedCommandsOnAuth.add(node.getString("").toLowerCase());
        });

        ConfigurationNode serverNode = whitelist.node("server-switching-commands");
        serverNode.childrenList().forEach(node -> {
            serverSwitchingCommands.add(node.getString("").toLowerCase());
        });

        ConfigurationNode blockedNode = whitelist.node("globally-blocked-commands");
        blockedNode.childrenList().forEach(node -> {
            globallyBlockedCommands.add(node.getString("").toLowerCase());
        });

        ConfigurationNode alwaysNode = whitelist.node("always-allowed-commands");
        alwaysNode.childrenList().forEach(node -> {
            alwaysAllowedCommands.add(node.getString("").toLowerCase());
        });
    }

    private void createDefaultWhitelist() {
        try {
            String defaultWhitelist = "whitelisted-commands-on-auth:\n" +
                "  - \"login\"\n" +
                "  - \"register\"\n" +
                "  - \"changepass\"\n" +
                "  - \"help\"\n\n" +
                "server-switching-commands:\n" +
                "  - \"server\"\n" +
                "  - \"survival\"\n" +
                "  - \"hub\"\n" +
                "  - \"lobby\"\n\n" +
                "globally-blocked-commands:\n" +
                "  - \"pl\"\n" +
                "  - \"plugins\"\n" +
                "  - \"version\"\n\n" +
                "always-allowed-commands:\n" +
                "  - \"login\"\n" +
                "  - \"register\"\n";

            Files.write(whitelistFile.toPath(), defaultWhitelist.getBytes());
            logger.info("Default whitelist.yml created successfully");
        } catch (IOException e) {
            logger.error("Failed to create default whitelist", e);
        }
    }

    public boolean isWhitelistedCommandOnAuth(String command) {
        return whitelistedCommandsOnAuth.contains(command.toLowerCase());
    }

    public boolean isServerSwitchingCommand(String command) {
        return serverSwitchingCommands.contains(command.toLowerCase());
    }

    public boolean isGloballyBlockedCommand(String command) {
        return globallyBlockedCommands.contains(command.toLowerCase());
    }

    public boolean isAlwaysAllowedCommand(String command) {
        return alwaysAllowedCommands.contains(command.toLowerCase());
    }

    public Set<String> getWhitelistedCommandsOnAuth() {
        return whitelistedCommandsOnAuth;
    }

    public Set<String> getServerSwitchingCommands() {
        return serverSwitchingCommands;
    }

    public Set<String> getGloballyBlockedCommands() {
        return globallyBlockedCommands;
    }

    public Set<String> getAlwaysAllowedCommands() {
        return alwaysAllowedCommands;
    }

    /**
     * Returns the union of always-allowed and whitelisted-on-auth command names.
     * Used by TabCompleteListener to build a strict allowlist for unauthenticated
     * players — only these bare command names may appear in their suggestion list.
     */
    public Set<String> getAllowedCommandNames() {
        Set<String> allowed = new HashSet<>();
        allowed.addAll(alwaysAllowedCommands);
        allowed.addAll(whitelistedCommandsOnAuth);
        return allowed;
    }
}