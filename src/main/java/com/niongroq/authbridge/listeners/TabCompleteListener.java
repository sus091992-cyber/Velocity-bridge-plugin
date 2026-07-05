package com.niongroq.authbridge.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.TabCompleteEvent;
import com.velocitypowered.api.proxy.Player;
import com.niongroq.authbridge.managers.WhitelistManager;
import org.slf4j.Logger;

import java.util.Collections;

public class TabCompleteListener {

    private final WhitelistManager whitelistManager;
    private final Logger logger;

    public TabCompleteListener(WhitelistManager whitelistManager, Logger logger) {
        this.whitelistManager = whitelistManager;
        this.logger = logger;
    }

    @Subscribe
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSource() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getSource();
        String partialCommand = event.getPartialCommand().toLowerCase();
        String commandName = extractCommandName(partialCommand);

        if (whitelistManager.isGloballyBlockedCommand(commandName)) {
            event.setSuggestions(Collections.emptyList());
        }
    }

    private String extractCommandName(String command) {
        command = command.trim();
        if (command.startsWith("/")) {
            command = command.substring(1);
        }
        
        int spaceIndex = command.indexOf(' ');
        if (spaceIndex != -1) {
            command = command.substring(0, spaceIndex);
        }
        
        return command.toLowerCase();
    }
}