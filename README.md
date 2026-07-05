# 🛡️ AuthBridge - Professional Authentication Bridge Plugin

[![Version](https://img.shields.io/badge/version-3.0.0-blue.svg)](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
[![Java](https://img.shields.io/badge/java-11+-green.svg)](https://www.java.com)
[![Velocity](https://img.shields.io/badge/velocity-3.0.1+-orange.svg)](https://papermc.io/software/velocity)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Author](https://img.shields.io/badge/author-S1MPLE-blue.svg)](https://github.com/sus091992-cyber)

AuthBridge is a professional authentication bridge plugin for **Velocity Proxy** that secures your network by blocking players from accessing game servers until they have logged in on the authentication server.

---

## ✨ Features

### 🔐 Authentication System
- Dedicated authentication server — players must log in before joining any other server
- Players who leave the auth server are automatically granted authenticated status
- Auth state is tracked per UUID and cleared on disconnect
- Only `/login`, `/register`, `/changepass` (and configurable commands) are allowed before authentication

### 🚫 Command Blocking
- **Globally blocked commands** (blocked for everyone, everywhere):
  - `/pl`, `/plugins`, `/plugin`, `/version`, `/ver`, `/about`
  - Namespaced variants: `/bukkit:pl`, `/spigot:plugins`, etc.
- **Server-switching commands** (blocked on auth server for unauthenticated players):
  - `/server`, `/hub`, `/lobby`, `/survival`, `/creative`, and many more

### 👥 Player Hider
- Players on the auth server are hidden from each other
- Fully toggleable via `config.yml`

### 🎭 Fake Plugin System
- Replace the `/plugins` output with a custom plugin name
- Supports Minecraft color codes (`&0`–`&f`, `&l`, `&o`, `&r`, etc.)
- Customizable message and footer

### 📌 Command Aliases
- Define custom shortcuts: `/hub` → `/server lobby`
- **Auto-alias**: automatically creates shortcuts for every server defined in `velocity.toml`
- Fully configurable in `config.yml`

### ⚙️ Whitelist System
- Fine-grained control over which commands are allowed before authentication
- Separate lists for: always-allowed, whitelisted-on-auth, server-switching, globally-blocked

---

## 📋 Requirements

| Dependency | Version |
|------------|---------|
| Java | 11+ |
| Velocity | 3.0.1+ |
| Maven | 3.6+ *(build only)* |

---

## 📦 Installation

1. Download `AuthBridge-3.0.0.jar` from [Releases](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
2. Place it in your Velocity `plugins/` folder
3. Restart the proxy — config files are generated automatically
4. Edit `plugins/authbridge/config.yml` and `plugins/authbridge/whitelist.yml`
5. Restart the proxy again to apply your configuration

---

## 🔨 Building from Source

**Requirements:** Java 11+, Maven 3.6+

```bash
git clone https://github.com/sus091992-cyber/Velocity-bridge-plugin.git
cd Velocity-bridge-plugin
mvn package
# Output: target/AuthBridge-3.0.0.jar
```

---

## ⚙️ Configuration

### config.yml

```yaml
# Name of your authentication server (must match velocity.toml)
auth-server: "auth"

# Servers that are completely inaccessible (e.g. admin, maintenance)
blocked-servers:
  - "admin"
  - "maintenance"

# Custom command shortcuts
custom-aliases:
  "/hub": "/server lobby"
  "/s": "/server survival"

# Hide players from each other on the auth server
player-hider:
  enabled: true

# Replace /plugins output with a fake plugin name
fake-plugin:
  name: "NYXCRAFT"
  color: "&5"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: "&7There are &a1&7 plugins installed."

# Messages sent to players
messages:
  not-logged-in: "&cYou must login first!"
  command-blocked: "&cThis command is blocked!"
  server-blocked: "&cYou cannot switch servers from here!"

settings:
  auto-alias:
    enabled: true   # Auto-create /serverName aliases for all velocity.toml servers
```

### whitelist.yml

```yaml
# Commands allowed on the auth server (before login)
whitelisted-commands-on-auth:
  - "login"
  - "register"
  - "changepass"
  - "help"

# Commands treated as server-switching (blocked on auth server)
server-switching-commands:
  - "server"
  - "hub"
  - "lobby"
  - "survival"
  # ... add more as needed

# Commands blocked globally for all players
globally-blocked-commands:
  - "pl"
  - "plugins"
  - "version"
  - "ver"
  - "about"

# Commands that always pass through (override all blocks)
always-allowed-commands:
  - "login"
  - "register"
  - "changepass"
```

---

## 🔄 How Authentication Works

```
Player connects to proxy
        │
        ▼
 Sent to auth server
        │
   /login or /register
        │
        ▼
 AuthMe authenticates player
        │
        ▼
 Player redirected to lobby/hub
        │  ← AuthBridge detects this transition
        ▼
 Player marked as authenticated
        │
        ▼
 Full access to all servers
```

> **Note:** AuthBridge detects authentication by tracking when a player transitions from the auth server to another server. No direct AuthMe API dependency is required on the proxy side.

---

## 📄 License

MIT License — see [LICENSE](LICENSE) for details.

## 👤 Author

**S1MPLE** — [GitHub](https://github.com/sus091992-cyber)
