<div align="center">

# 🔐 AuthBridge

**A professional authentication bridge plugin for Velocity proxy**

![Version](https://img.shields.io/badge/version-3.1.0-blue)
![Velocity](https://img.shields.io/badge/Velocity-3.x-green)
![Java](https://img.shields.io/badge/Java-17+-orange)
![License](https://img.shields.io/badge/license-MIT-purple)

[![🇮🇷 مستندات فارسی](https://img.shields.io/badge/🇮🇷-مستندات_فارسی-brightgreen)](README_FA.md)

</div>

---

## 📋 Features

### 🔒 Authentication Gate
Players are automatically routed to the **auth server** when they first connect. Until they log in via AuthMe (or a compatible plugin), they are locked in place — no server switching, no unauthorized commands.

- Tracks authenticated vs. unauthenticated state per player
- Auth state is cleared on every disconnect, so reconnecting players always start fresh
- Supports external auth confirmation via plugin messaging channel (`authbridge:auth`)

---

### 👻 Player Hider (Tab List)
Any player on the auth server is **completely invisible** to everyone else — and sees no one themselves.

- Removed from every other player's tab list the moment they join auth
- Their own tab list is fully wiped on join
- Restored automatically after successful login and server transfer
- No player on auth can see any other player, including other auth players
- Configurable via `player-hider.enabled` and `player-hider.hide-in-tablist`

> **In-game (world) visibility:** Velocity is a proxy and cannot send entity packets directly.  
> To hide players in the game world, configure your backend AuthMe plugin to set joining players to **SPECTATOR** mode.

---

### ⏱️ RaidBar Countdown Timer
A **pink raid-style bar** counts down the seconds a player has left to log in.

- Shown immediately when a player lands on the auth server
- Title updates every second with remaining time via `%timer_bos%` placeholder
- Bar shrinks as time runs out
- Player is kicked with a configurable message when the timer reaches zero
- Cancelled automatically on successful login
- Color, duration, and message fully configurable

```yaml
raidbar:
  enabled: true
  timer: 60          # seconds before kick
  color: PINK        # BossBar color (PINK, RED, BLUE, GREEN, YELLOW, PURPLE, WHITE)
  message: "&fYou only have &c%timer_bos% &fseconds to login"
```

---

### 🚫 Blocked Servers
Certain servers can be made completely inaccessible to all players.

- Configured in `blocked-servers` list
- Connection is denied **before** it is established (pre-connect event)
- Players receive a configurable "server-blocked" message

---

### 🔗 Custom Command Aliases
Define shortcut commands that redirect to other commands.

```yaml
custom-aliases:
  "/hub": "/server lobby"
  "/l":   "/server lobby"
  "/spawn": "/server lobby"
```

---

### 🤖 Auto-Alias Registration
Automatically registers `/servername` commands for every server defined in `velocity.toml`.

- Skips the auth server and any blocked servers
- Can be disabled with `settings.auto-alias.enabled: false`
- Example: `/survival` automatically becomes an alias for `/server survival`

---

### 🛡️ Command Gating
Unauthenticated players on the auth server are restricted to a whitelist of allowed commands.

- Globally blocked commands (`/plugins`, `/pl`, `/version`, `/ver`, `/about`) are hidden from **all** players
- Unauthenticated players can only run commands listed in `whitelist.yml`
- Authenticated players on the auth server cannot use server-switching commands
- Configurable allowlist via `whitelist.yml`

---

### 🎭 Fake Plugin List
When a player runs `/plugins`, `/pl`, `/version`, or `/about`, the plugin responds with a **fake plugin list** instead of revealing the real server stack.

- Prefix, plugin name, and footer are fully configurable
- Prevents plugin fingerprinting by hack clients

```yaml
fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""
```

---

### 🔏 Tab-Complete Lockdown
Tab-complete suggestions are sanitized for all players at `PostOrder.LAST` priority.

- **Unauthenticated players:** suggestions are replaced with only the allowed command names from the whitelist
- **All players:** namespaced suggestions (e.g. `authbridge:reload`, `luckperms:user`) are always stripped — hack clients use these to detect installed plugins
- **All players:** globally blocked commands are removed from suggestions

---

### 📡 Plugin Channel Guard
Intercepts outgoing plugin channel messages from backend servers to prevent plugin fingerprinting.

- `minecraft:brand` → spoofed to `"Minecraft"`
- `minecraft:register` → plugin namespaces are stripped from the payload

---

## ⚙️ Configuration

Full `config.yml`:

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
  "/l": "/server lobby"
  "/spawn": "/server lobby"

player-hider:
  enabled: true
  hide-in-tablist: true

fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""

raidbar:
  enabled: true
  timer: 60
  color: PINK
  message: "&fYou only have &c%timer_bos% &fseconds to login"

messages:
  not-logged-in: "&cYou must login first!"
  command-blocked: "&cThis command is blocked!"
  server-blocked: "&cYou cannot connect to that server!"
  raidbar-timeout: "&cLogin time expired! Please reconnect."

settings:
  auto-alias:
    enabled: true
```

---

## 📁 whitelist.yml

Controls which commands unauthenticated players are allowed to run on the auth server.

```yaml
allowed-commands:
  - "login"
  - "l"
  - "register"
  - "reg"

always-allowed:
  - "login"
  - "register"

globally-blocked:
  - "plugins"
  - "pl"
  - "version"
  - "ver"
  - "about"
```

---

## 🚀 Installation

1. Build the plugin with Maven: `mvn clean package`
2. Copy the resulting `.jar` from `target/` into your Velocity `plugins/` folder
3. Start Velocity once to generate `config.yml` and `whitelist.yml`
4. Edit both files to match your server setup
5. Restart Velocity

---

## 🔌 Compatibility

| Software | Version |
|----------|---------|
| Velocity | 3.x |
| Java | 17+ |
| AuthMe (backend) | Any version with plugin messaging |

---

## 👤 Author

**S1MPLE** — AuthBridge v3.1.0
