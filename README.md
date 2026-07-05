# 🛡️ AuthBridge - Professional Authentication Bridge Plugin

[![Version](https://img.shields.io/badge/version-3.0.0-blue.svg)](https://github.com/sus091992-cyber/Velocity-bridge-plugin)
[![Java](https://img.shields.io/badge/java-11+-green.svg)](https://www.java.com)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)
[![Author](https://img.shields.io/badge/author-S1MPLE-blue.svg)](https://github.com/sus091992-cyber)

AuthBridge is a professional authentication bridge plugin for **Velocity Proxy** that provides secure authentication system with advanced command blocking, player management, and customizable configuration.

## ✨ Features

### 🔐 Authentication System
- Integration with **AuthMe API** for secure authentication
- Designate an authentication server for login operations
- Block access to other servers until authenticated
- Only allow `/login`, `/register`, `/changepass` commands before authentication

### 🚫 Command Blocking System
- **Globally blocked commands** (always and everywhere):
  - `/pl`, `/plugins`, `/plugin`, `/version`, `/ver`, `/about`
  - Server command variants: `/bukkit:pl`, `/spigot:plugins`, etc.
  
- **Server-switching commands** (blocked on auth server only):
  - `/server`, `/survival`, `/creative`, `/skyblock`, `/hub`, `/lobby`
  - `/goto`, `/gserv`, `/sv`, `/spawn`, `/factions`, `/prison`, `/kitpvp`, `/pvp`
  - `/minigames`, `/sky`, `/plot`, `/build`, `/mini`, `/game`, `/join`, `/connect`

### 👥 Player Hider System
- Hide players from each other on the auth server
- Change game mode to `ADVENTURE` automatically
- Hide players from tab list and world
- Fully customizable messages

### 🎭 Fake Plugin System
- Display custom plugin name for `/pl` and `/plugins` commands
- **NEW**: Apply single color code to entire plugin name
- Customizable plugin message and footer
- Supports all vanilla color codes (`&0-&f`, `&k`, `&l`, `&m`, `&n`, `&o`, `&r`)

### 📌 Command Aliases
- Create custom command shortcuts
- **Auto-alias**: Automatically creates shortcuts for all servers from velocity.toml
- Example: `/hub` → `/server lobby`
- Example: `/survival` → `/server survival`
- Fully configurable in `config.yml`

### ⚙️ Whitelist System
- Separate configuration for different command types
- Manage whitelisted commands before authentication
- Control server-switching and globally-blocked commands
- Always-allowed commands that bypass all restrictions

## 📋 Requirements

- **Java 11+**
- **Velocity 3.0.1+**
- **AuthMe 5.6.0+** (on backend servers)
- **Maven 3.6+** (for building)

## 📦 Installation

### Step 1: Download
Download the latest release from [GitHub Releases](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)

### Step 2: Install
1. Place `AuthBridge-3.0.0.jar` in your `plugins` folder
2. Restart your Velocity proxy

### Step 3: Configure
Edit the generated configuration files:
- `plugins/authbridge/config.yml`
- `plugins/authbridge/whitelist.yml`

### Step 4: Restart
Restart your Velocity proxy to apply the configuration

## 🎯 Configuration

### config.yml

```yaml
auth-server: "auth"

blocked-servers:
  - "admin"
  - "maintenance"

auth-required-servers:
  - "lobby"
  - "survival"
  - "creative"

custom-aliases:
  "/hub": "/server lobby"
  "/s": "/server survival"

player-hider:
  enabled: true
  hide-in-world: true
  hide-in-tablist: true
  game-mode: "ADVENTURE"

fake-plugin:
  name: "NYXCRAFT"
  color: "&5"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: "&7There are &a1&7 plugins installed."

messages:
  not-logged-in: "&cYou must login first!"
  command-blocked: "&cThis command is blocked!"
  server-blocked: "&cYou cannot connect to this server!"

settings:
  enabled: true
  debug: false
  login-cache-duration: 3600
  auto-alias:
    enabled: true
```

### whitelist.yml

```yaml
whitelisted-commands-on-auth:
  - "login"
  - "register"
  - "changepass"
  - "help"

server-switching-commands:
  - "server"
  - "survival"
  - "hub"

globally-blocked-commands:
  - "pl"
  - "plugins"
  - "version"

always-allowed-commands:
  - "login"
  - "register"
```

## 🎨 Color Codes

| Code | Color |
|------|-------|
| `&0` | Black |
| `&1` | Dark Blue |
| `&2` | Dark Green |
| `&3` | Dark Aqua |
| `&4` | Dark Red |
| `&5` | Dark Purple |
| `&6` | Gold |
| `&7` | Gray |
| `&8` | Dark Gray |
| `&9` | Blue |
| `&a` | Green |
| `&b` | Aqua |
| `&c` | Red |
| `&d` | Light Purple |
| `&e` | Yellow |
| `&f` | White |

## 🔧 Build

### Build with Maven

```bash
git clone https://github.com/sus091992-cyber/Velocity-bridge-plugin.git
cd Velocity-bridge-plugin
mvn clean package
```

The compiled JAR will be in `target/authbridge-3.0.0.jar`

## 🏗️ Architecture

### Project Structure

```
AuthBridge/
├── src/main/java/com/niongroq/authbridge/
│   ├── AuthBridge.java
│   ├── commands/
│   │   ├── ServerCommand.java
│   │   └── AliasCommand.java
│   ├── listeners/
│   │   ├── AuthListener.java
│   │   └── TabCompleteListener.java
│   ├── managers/
│   │   ├── ConfigManager.java
│   │   ├── WhitelistManager.java
│   │   └── PlayerHider.java
│   └── utils/
│       └── MessageUtils.java
└── src/main/resources/
    ├── plugin.json
    ├── config.yml
    └── whitelist.yml
```

## 📊 Performance

- 🚀 Lightweight and efficient
- 💾 Minimal memory footprint
- ⚡ Async command processing
- 🔄 ConcurrentHashMap for thread-safe operations
- 📈 Scalable for large server networks

## 🤝 Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**S1MPLE**
- GitHub: [@sus091992-cyber](https://github.com/sus091992-cyber)

## 📞 Support

For issues, questions, or suggestions:
- 🐛 GitHub Issues: [Create an Issue](https://github.com/sus091992-cyber/Velocity-bridge-plugin/issues)
- 💬 Discussions: [Start a Discussion](https://github.com/sus091992-cyber/Velocity-bridge-plugin/discussions)

---

**Made with ❤️ by S1MPLE**

Last Updated: 2026-07-05
Version: 3.0.0