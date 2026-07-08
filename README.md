<div align="center">

# 🔐 AuthBridge

**Professional Authentication Bridge Plugin for Velocity Proxy**

[![Version](https://img.shields.io/badge/version-3.5.0-blue?style=for-the-badge)](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
[![Velocity](https://img.shields.io/badge/Velocity-3.5.0-green?style=for-the-badge)](https://papermc.io/software/velocity)
[![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge)](https://adoptium.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)

</div>

---

<details open>
<summary><b>🇬🇧 English Documentation</b></summary>

## Overview

AuthBridge is a Velocity proxy plugin that enforces authentication before players can access any game server. It intercepts every player connection, holds them in a dedicated auth server, runs a visual countdown timer (RaidBar), hides auth-server players from the tab list, and redirects them to the lobby once authenticated.

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🔐 **Authentication Gate** | Players cannot leave the auth server until whitelisted |
| 👁️ **Player Hider** | Auth-server players are hidden from everyone's tab list |
| ⏱️ **RaidBar Timer** | Segmented countdown bar (raid-style) with kick on timeout |
| 🚀 **After-Login Redirect** | Auto-sends players to a target server after login |
| 🚫 **Blocked Servers** | Certain servers are permanently inaccessible |
| 🔗 **Custom Aliases** | Map `/hub`, `/l` etc. to `/server lobby` |
| 🤖 **Auto-Alias** | Automatically registers `/servername` commands from `velocity.toml` |
| 🛡️ **Command Gate** | Blocks all commands for unauthenticated players |
| 🎭 **Fake Plugin List** | `/plugins` returns a custom single-plugin response |
| ⌨️ **Tab-Complete Lock** | Hides command suggestions for unauthenticated players |
| 📡 **Plugin Channel Guard** | Blocks plugin channel traffic on the auth server |
| 📢 **`/sayvelo` Broadcast** | Send gradient/color messages to **all players on all servers** |

---

## 📢 /sayvelo — Global Broadcast Command

Sends a message to every player connected to the proxy, regardless of which server they are on.

### Permission
```
authbridge.sayvelo
```
Console always has permission.

### Usage

```
/sayvelo <message>
```

### Formats

**Legacy color codes** (`&` codes — any Minecraft color/format):

```
/sayvelo &c&lSERVER RESTART &r&fin 5 minutes!
/sayvelo &a&lWelcome &f%player% to the network!
```

**Gradient text** (custom `GRADINT` tag):

```
/sayvelo <GRADINT:RED:WHITE:GREEN>ANNOUNCEMENT» Server is back online!
/sayvelo <GRADINT:GOLD:YELLOW:WHITE>ANNOUNCEMENT» Event starts in 10 minutes!
/sayvelo <GRADINT:DARK_RED:RED:GOLD>WARNING» Do not spam chat!
/sayvelo <GRADINT:#FF0000:#FFFFFF:#00FF00>ANNOUNCEMENT» Custom hex colors!
```

### Available Colors for GRADINT

| Named Color | Named Color |
|-------------|-------------|
| `RED` | `DARK_RED` |
| `GREEN` | `DARK_GREEN` |
| `BLUE` | `DARK_BLUE` |
| `AQUA` | `DARK_AQUA` |
| `YELLOW` | `GOLD` |
| `WHITE` | `GRAY` |
| `DARK_GRAY` | `BLACK` |
| `LIGHT_PURPLE` | `DARK_PURPLE` |

Or use any **hex color**: `#FF5500`, `#00AAFF`, etc.

You can chain **2 or more** colors: `<GRADINT:RED:GOLD:YELLOW:WHITE>`

### Legacy Color & Format Codes (`&` codes)

| Code | Result | Code | Result |
|------|--------|------|--------|
| `&0` | Black | `&8` | Dark Gray |
| `&1` | Dark Blue | `&9` | Blue |
| `&2` | Dark Green | `&a` | Green |
| `&3` | Dark Aqua | `&b` | Aqua |
| `&4` | Dark Red | `&c` | Red |
| `&5` | Dark Purple | `&d` | Light Purple |
| `&6` | Gold | `&e` | Yellow |
| `&7` | Gray | `&f` | White |
| `&l` | **Bold** | `&o` | *Italic* |
| `&n` | Underline | `&m` | ~~Strikethrough~~ |
| `&k` | Obfuscated | `&r` | Reset |

---

## ⏱️ RaidBar — Configuration Guide

The RaidBar is the segmented countdown boss bar shown to players while they are on the auth server.

```yaml
raidbar:
  enabled: true
  timer: 60           # seconds before kicking (1–3600)
  color: RED          # bar color (see table below)
  overlay: NOTCHED_6  # bar style (see table below)
  message: "&fYou only have &c%timer_bos% &fseconds to login"
```

### 🎨 Color Options

| Value | Appearance |
|-------|-----------|
| `RED` | 🔴 Red |
| `GREEN` | 🟢 Green |
| `BLUE` | 🔵 Blue |
| `YELLOW` | 🟡 Yellow |
| `PURPLE` | 🟣 Purple |
| `PINK` | 🩷 Pink |
| `WHITE` | ⬜ White |

### 📊 Overlay (Style) Options

| Value | Description | Visual |
|-------|-------------|--------|
| `PROGRESS` | Solid bar (no segments) | `────────────────────` |
| `NOTCHED_6` | 6 segments — **Raid style** ⭐ | `██ ██ ██ ██ ██ ██` |
| `NOTCHED_10` | 10 segments | `█ █ █ █ █ █ █ █ █ █` |
| `NOTCHED_12` | 12 segments | `█ █ █ █ █ █ █ █ █ █ █ █` |
| `NOTCHED_20` | 20 segments | `▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌` |

> **Recommended**: `color: RED` + `overlay: NOTCHED_6` replicates the exact look of Minecraft's native raid boss bar.

The `%timer_bos%` placeholder in `message` is replaced with the remaining seconds.

---

## ⚙️ Full config.yml

```yaml
# Name of your auth server (must match velocity.toml)
auth-server: "auth"

# Servers permanently inaccessible to all players
blocked-servers:
  - "admin"
  - "maintenance"

# Custom command shortcuts
custom-aliases:
  "/hub": "/server lobby"
  "/l": "/server lobby"
  "/spawn": "/server lobby"

# Hide auth-server players from tab list
player-hider:
  enabled: true
  hide-in-tablist: true

# Fake /plugins output
fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""

# RaidBar countdown timer
# color   : PINK | BLUE | RED | GREEN | YELLOW | PURPLE | WHITE
# overlay : PROGRESS | NOTCHED_6 | NOTCHED_10 | NOTCHED_12 | NOTCHED_20
raidbar:
  enabled: true
  timer: 60
  color: RED
  overlay: NOTCHED_6
  message: "&fYou only have &c%timer_bos% &fseconds to login"

# Redirect players after successful login/register
after-login:
  send: true
  server: "lobby"

# Player-facing messages
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

## 📋 whitelist.yml

Authenticated players are stored here automatically by your auth plugin. Each entry is a UUID:username pair.

```yaml
players:
  - uuid: "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
    username: "Steve"
```

---

## 📦 Installation

1. Download `AuthBridge-3.4.0.jar` from [Releases](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
2. Place it in your Velocity `plugins/` folder
3. Start your proxy once to generate `plugins/AuthBridge/config.yml`
4. Edit `config.yml` — set `auth-server` to match your `velocity.toml` server name
5. Restart the proxy
6. **Install the backend companion plugin (`AuthBridge-Bukkit-1.0.0.jar`, built from `backend-bukkit/`) on your auth server only** — see below. Without it, `after-login` will never fire and players will never be vanished from each other.

---

## 🔌 Backend companion plugin (`backend-bukkit/`)

AuthBridge (the proxy plugin) cannot see login/register events by itself — that only happens on the backend server where AuthMe runs, and it cannot make players truly invisible to each other — Velocity doesn't have entity-level control over what a backend server renders. Both problems are solved by a small companion plugin, `AuthBridge-Bukkit`, that you install **only on your auth/login server** (the one named in `auth-server:`).

It does two things:

1. **Reports login/register to the proxy.** It hooks AuthMe Reloaded's `LoginEvent`/`RegisterEvent` and sends a plugin message on the `authbridge:auth` channel. The proxy listens for this and only then marks the player authenticated and performs the `after-login` redirect — so the player is moved **only after AuthMe confirms success**, never before and never on a failed attempt.
2. **Vanishes players from each other.** Every player connected to this server is hidden (real entity-level hide, not just tab list) from every other player on the same server, and vice versa. If 100 players are waiting to log in, none of them can see any of the others — only themselves.

### Build

```bash
cd backend-bukkit
mvn package
```

Produces `target/AuthBridge-Bukkit-1.0.0.jar`.

### Install

1. Place `AuthBridge-Bukkit-1.0.0.jar` in the `plugins/` folder of your **auth server only** (not your lobby/game servers).
2. Make sure AuthMe Reloaded is installed on that same server.
3. Restart the server — it generates `plugins/AuthBridgeBukkit/config.yml`.
4. `proxy-channel` in that config must match the proxy's channel (`authbridge:auth` by default — do not change unless you also change the proxy source).
5. `vanish-all.enabled: true` turns on mutual invisibility; set to `false` if you only want the login/register bridge.

> If AuthMe is not detected on the backend server, the plugin logs a warning and disables the login/register bridge (vanish-all still works). Check your server console after installing.

### Version compatibility

`AuthBridge-Bukkit` targets **Paper 1.17 and newer** (up to and including the latest release) with a single build — you do not need to rebuild it per Minecraft version:

- It only uses Bukkit API that has been stable since 1.17 (plugin-messaging channels, `Player#hidePlayer`/`showPlayer`, `PlayerJoinEvent`/`PlayerQuitEvent`) — nothing tied to a newer version, and nothing removed on current Paper.
- The jar is compiled to Java 8 bytecode, so it loads under whatever JVM your Paper build already runs on (Java 8 through Java 21) — the compile target is not what limits the supported floor.
- `plugin.yml` intentionally has no `api-version` pin. Paper still loads plugins without one (legacy compatibility mode); this avoids the server rejecting the jar over a version mismatch, but it is a loadability choice, not proof the plugin behaves correctly on every historical Paper release.

The 1.17 floor comes from PaperMC's own Maven repository, which no longer publishes API artifacts older than 1.17 (older snapshots, e.g. 1.13.x/1.16.x, have been purged) — so that's the oldest version this plugin has actually been built and can be verified against. Compiling against very old Spigot/CraftBukkit API from another source and testing on a real 1.13–1.16 server would be needed before claiming support further back than that.

---

## 🔧 Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/server <name>` | — | Connect to a server (gated by whitelist) |
| `/sayvelo <msg>` | `authbridge.sayvelo` | Broadcast to all players on all servers |

---

## 🧩 Compatibility

- **Velocity**: 3.3.0+ (API 3)
- **Java**: 21+
- **Auth plugins**: AuthMe Velocity, nLogin, JPremium, etc.

</details>

---

<details>
<summary><b>🇮🇷 مستندات فارسی</b></summary>

## درباره پلاگین

AuthBridge یک پلاگین پروکسی Velocity است که قبل از ورود بازیکنان به هر سروری، احراز هویت را اجباری می‌کند. این پلاگین هر اتصال بازیکن را رهگیری کرده، او را در سرور Auth نگه می‌دارد، یک تایمر شمارش معکوس بصری (RaidBar) اجرا می‌کند، بازیکنان سرور Auth را از لیست Tab مخفی می‌کند و پس از احراز هویت، آن‌ها را به لابی هدایت می‌کند.

---

## ✨ قابلیت‌ها

| قابلیت | توضیح |
|--------|--------|
| 🔐 **دروازه احراز هویت** | بازیکنان تا زمان وایت‌لیست شدن نمی‌توانند از سرور Auth خارج شوند |
| 👁️ **مخفی‌کردن بازیکنان** | بازیکنان سرور Auth از لیست Tab همه مخفی می‌شوند |
| ⏱️ **تایمر RaidBar** | نوار شمارش معکوس تقسیم‌بندی‌شده با اخراج در صورت اتمام وقت |
| 🚀 **ریدایرکت پس از لاگین** | بازیکن پس از لاگین به‌صورت خودکار به سرور مشخص فرستاده می‌شود |
| 🚫 **سرورهای مسدود** | برخی سرورها برای همیشه غیرقابل دسترس هستند |
| 🔗 **Alias سفارشی** | نگاشت `/hub`، `/l` و غیره به `/server lobby` |
| 🤖 **Auto-Alias** | ثبت خودکار دستورات `/servername` از `velocity.toml` |
| 🛡️ **فیلتر دستورات** | بلاک کردن تمام دستورات برای بازیکنان احراز هویت‌نشده |
| 🎭 **لیست پلاگین جعلی** | `/plugins` پاسخ سفارشی تک‌پلاگین برمی‌گرداند |
| ⌨️ **قفل Tab-Complete** | مخفی‌کردن پیشنهادات دستور برای بازیکنان احراز هویت‌نشده |
| 📡 **محافظ کانال پلاگین** | بلاک کردن ترافیک کانال‌های پلاگین در سرور Auth |
| 📢 **دستور `/sayvelo`** | ارسال پیام گرادیانت/رنگی به **تمام بازیکنان در همه سرورها** |

---

## 📢 دستور /sayvelo — پخش سراسری

این پیام را به تمام بازیکنان متصل به پروکسی، صرف نظر از اینکه در کدام سرور هستند، ارسال می‌کند.

### مجوز
```
authbridge.sayvelo
```
کنسول همیشه مجوز دارد.

### فرمت‌های دستور

**کدهای رنگ قدیمی** (کدهای `&`):

```
/sayvelo &c&lاعلان مهم &r&fمتن پیام
/sayvelo &a&lخوش آمدید &fبه سرور!
```

**متن گرادیانت** (تگ سفارشی `GRADINT`):

```
/sayvelo <GRADINT:RED:WHITE:GREEN>ANNOUNCEMENT» سرور آنلاین شد!
/sayvelo <GRADINT:GOLD:YELLOW:WHITE>ANNOUNCEMENT» رویداد ۱۰ دقیقه دیگر شروع می‌شود!
/sayvelo <GRADINT:DARK_RED:RED:GOLD>WARNING» اسپم نکنید!
/sayvelo <GRADINT:#FF0000:#FFFFFF:#00FF00>ANNOUNCEMENT» رنگ‌های هگزادسیمال!
```

### رنگ‌های موجود برای GRADINT

| رنگ | رنگ |
|-----|-----|
| `RED` — قرمز | `DARK_RED` — قرمز تیره |
| `GREEN` — سبز | `DARK_GREEN` — سبز تیره |
| `BLUE` — آبی | `DARK_BLUE` — آبی تیره |
| `AQUA` — فیروزه‌ای | `DARK_AQUA` — فیروزه تیره |
| `YELLOW` — زرد | `GOLD` — طلایی |
| `WHITE` — سفید | `GRAY` — خاکستری |
| `DARK_GRAY` — خاکستری تیره | `BLACK` — مشکی |
| `LIGHT_PURPLE` — بنفش روشن | `DARK_PURPLE` — بنفش تیره |

یا هر **رنگ هگز**: `#FF5500`، `#00AAFF` و غیره.
می‌توانید **۲ یا بیشتر** رنگ زنجیر کنید: `<GRADINT:RED:GOLD:YELLOW:WHITE>`

---

## ⏱️ راهنمای تنظیمات RaidBar

```yaml
raidbar:
  enabled: true
  timer: 60           # ثانیه‌های قبل از اخراج (۱–۳۶۰۰)
  color: RED          # رنگ نوار (جدول زیر)
  overlay: NOTCHED_6  # سبک نوار (جدول زیر)
  message: "&fفقط &c%timer_bos% &fثانیه برای لاگین داری"
```

### 🎨 رنگ‌های نوار

| مقدار | رنگ |
|-------|-----|
| `RED` | 🔴 قرمز |
| `GREEN` | 🟢 سبز |
| `BLUE` | 🔵 آبی |
| `YELLOW` | 🟡 زرد |
| `PURPLE` | 🟣 بنفش |
| `PINK` | 🩷 صورتی |
| `WHITE` | ⬜ سفید |

### 📊 سبک نوار (Overlay)

| مقدار | توضیح | ظاهر |
|-------|--------|------|
| `PROGRESS` | نوار یکپارچه (بدون تقسیم) | `────────────────────` |
| `NOTCHED_6` | ۶ بخش — **سبک Raid اصلی** ⭐ | `██ ██ ██ ██ ██ ██` |
| `NOTCHED_10` | ۱۰ بخش | `█ █ █ █ █ █ █ █ █ █` |
| `NOTCHED_12` | ۱۲ بخش | `█ █ █ █ █ █ █ █ █ █ █ █` |
| `NOTCHED_20` | ۲۰ بخش | `▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌▌` |

> **پیشنهاد**: `color: RED` + `overlay: NOTCHED_6` دقیقاً ظاهر نوار Raid اصلی Minecraft را شبیه‌سازی می‌کند.

متغیر `%timer_bos%` در پیام با ثانیه‌های باقیمانده جایگزین می‌شود.

---

## ⚙️ فایل config.yml کامل

```yaml
# نام سرور Auth (باید با velocity.toml یکسان باشد)
auth-server: "auth"

# سرورهایی که برای همیشه غیرقابل دسترس هستند
blocked-servers:
  - "admin"
  - "maintenance"

# میانبرهای دستور سفارشی
custom-aliases:
  "/hub": "/server lobby"
  "/l": "/server lobby"

# مخفی کردن بازیکنان سرور Auth از لیست Tab
player-hider:
  enabled: true
  hide-in-tablist: true

# خروجی جعلی /plugins
fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""

# تایمر شمارش معکوس RaidBar
# color   : PINK | BLUE | RED | GREEN | YELLOW | PURPLE | WHITE
# overlay : PROGRESS | NOTCHED_6 | NOTCHED_10 | NOTCHED_12 | NOTCHED_20
raidbar:
  enabled: true
  timer: 60
  color: RED
  overlay: NOTCHED_6
  message: "&fفقط &c%timer_bos% &fثانیه برای لاگین داری"

# ریدایرکت بازیکنان پس از لاگین
after-login:
  send: true
  server: "lobby"

# پیام‌های نمایشی
messages:
  not-logged-in: "&cابتدا باید لاگین کنی!"
  command-blocked: "&cاین دستور مسدود است!"
  server-blocked: "&cنمی‌توانی به آن سرور متصل شوی!"
  raidbar-timeout: "&cزمان لاگین به اتمام رسید! دوباره متصل شو."

settings:
  auto-alias:
    enabled: true
```

---

## 📦 نصب

1. دانلود `AuthBridge-3.5.0.jar` از [Releases](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
2. فایل را در پوشه `plugins/` پروکسی Velocity قرار دهید
3. پروکسی را یک‌بار راه‌اندازی کنید تا `plugins/AuthBridge/config.yml` ساخته شود
4. `config.yml` را ویرایش کنید — مقدار `auth-server` باید با نام سرور در `velocity.toml` یکسان باشد
5. پروکسی را ریستارت کنید
6. **پلاگین کمکی بک‌اند (`AuthBridge-Bukkit-1.0.0.jar` از پوشه `backend-bukkit/`) را فقط روی سرور Auth نصب کنید** — بدون آن، ریدایرکت پس از لاگین اجرا نمی‌شود و بازیکنان از هم مخفی نخواهند شد. جزئیات در بخش انگلیسی (Backend companion plugin) بالا آمده است.

---

## 🔧 دستورات

| دستور | مجوز | توضیح |
|-------|------|--------|
| `/server <name>` | — | اتصال به سرور (کنترل شده توسط وایت‌لیست) |
| `/sayvelo <msg>` | `authbridge.sayvelo` | پخش پیام به همه بازیکنان در همه سرورها |

---

## 🧩 سازگاری

- **Velocity**: نسخه ۳.۳.۰ به بالا (API نسخه ۳)
- **Java**: نسخه ۲۱ به بالا
- **پلاگین‌های Auth**: AuthMe Velocity، nLogin، JPremium و غیره

</details>

---

<div align="center">
Made with ❤️ by <b>S1MPLE</b>
</div>
