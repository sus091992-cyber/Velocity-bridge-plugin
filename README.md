<div align="center">

# 🛡️ AuthBridge

**Professional Authentication Bridge Plugin for Velocity Proxy**

[![Version](https://img.shields.io/badge/version-3.0.0-blue.svg)](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
[![Java](https://img.shields.io/badge/java-11+-green.svg)](https://www.java.com)
[![Velocity](https://img.shields.io/badge/velocity-3.0.1+-orange.svg)](https://papermc.io/software/velocity)
[![License](https://img.shields.io/badge/license-MIT-lightgrey.svg)](LICENSE)
[![Author](https://img.shields.io/badge/author-S1MPLE-purple.svg)](https://github.com/sus091992-cyber)

---

*[فارسی 🇮🇷](#-authbridge--پلاگین-احراز-هویت-حرفه‌ای-برای-ولاسیتی) · [English 🇺🇸](#english)*

</div>

---

<a name="english"></a>

## English 🇺🇸

AuthBridge is a professional **authentication bridge plugin** for [Velocity Proxy](https://papermc.io/software/velocity). It enforces authentication on your network by holding players on a dedicated auth server until they log in, while actively defending against hack clients and plugin-fingerprinting attacks.

---

### ✨ Features

#### 🔐 Authentication System
- Players always connect to the **auth server** first and are held there until authenticated
- Authentication state is granted when AuthMe (on the backend) redirects the player to another server after a successful login
- Auth state is tracked per **UUID** and cleared on every disconnect — reconnecting players always start fresh
- External `markAuthenticated(UUID)` API for plugin-messaging integration with backend auth plugins

#### 🚫 Command Gating
- **Globally blocked** commands (`/plugins`, `/pl`, `/ver`, `/version`, `/about` and all variants) are intercepted for every player
- **Unauthenticated players** can only run `/login`, `/register`, `/changepass` and other explicitly whitelisted commands — everything else is silently denied
- **Server-switching commands** (`/server`, `/hub`, `/lobby`, `/survival` …) are blocked on the auth server even after login

#### 🎭 Fake Plugin List *(new in 3.0.0)*
- Instead of silently blocking `/plugins` and showing an error, AuthBridge **replies with a convincing fake plugin list** taken from `config.yml`
- Covers every known variant: `/plugins`, `/pl`, `/plugin`, `/version`, `/ver`, `/about`, `/icanhasbukkit`, and **all namespaced forms** (`bukkit:pl`, `spigot:pl`, `paper:pl`, `velocity:pl`, `waterfall:pl`, `bungeecord:pl`, `minecraft:plugins` …)
- The fake plugin name, colour, and message format are fully configurable

#### 🕵️ Anti-Hack-Client Tab-Complete Lockdown *(new in 3.0.0)*
Modern hack clients (Meteor, Wurst, Aristois, Sigma, Impact and others) detect installed plugins by tab-completing `/` on join — the server returns namespaced suggestions like `authbridge:reload` or `luckperms:user`, directly exposing the plugin list.

AuthBridge closes this gap entirely (runs at `PostOrder.LAST` — always the final word):
- **Unauthenticated players** receive a strict allowlist: only `/login`, `/register` and other explicitly whitelisted command names appear — nothing else
- **All players** have every `namespace:command` suggestion stripped before the packet leaves the proxy
- Globally blocked command names (`plugins`, `ver`, `about` …) are removed from suggestions for everyone

#### 📡 Plugin Channel Guard *(new in 3.0.0)*
Backend servers broadcast two packets that leak server and plugin information:

| Channel | What it leaks | AuthBridge response |
|---|---|---|
| `minecraft:brand` / `MC|Brand` | Server software (`Paper`, `Purpur` …) | Spoofed to `"Minecraft"` |
| `minecraft:register` / `REGISTER` | All registered plugin channels (e.g. `authbridge:auth`, `luckperms:action`) | Strict vanilla allowlist — only `minecraft:debug/*` channels pass through |

Legacy channel names (`MC|Brand`, `REGISTER`) used by ViaVersion and mixed-version setups are covered as well.

#### 👥 Player Hider
- Players on the auth server are **hidden from each other** (tab-list and world)
- Automatically shown again when they move to another server

#### 📌 Command Aliases
- Define custom shortcuts in `config.yml`: `/hub` → `/server lobby`
- **Auto-alias**: automatically registers a `/<serverName>` alias for every server in `velocity.toml`
- Blocked servers are excluded from auto-aliases and never appear in `/server` tab-complete

#### 🔒 Blocked Servers *(fixed in 3.0.0)*
- Servers listed in `blocked-servers` are denied at **pre-connect** time — the connection is rejected before it is established, so the player never briefly appears on the blocked server
- Blocked server names are also hidden from `/server` tab-complete suggestions

---

### 📋 Requirements

| Dependency | Version |
|---|---|
| Java | 11 or newer |
| Velocity | 3.0.1 or newer |
| Maven | 3.6+ *(build only)* |

> AuthBridge has **no runtime dependencies** — no AuthMe API, no ProtocolLib. The JAR is self-contained.

---

### 📦 Installation

1. Download `AuthBridge-3.0.0.jar` from [Releases](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases)
2. Place it in your Velocity `plugins/` folder
3. Restart the proxy — config files are generated automatically in `plugins/authbridge/`
4. Edit `config.yml` and `whitelist.yml` to match your network
5. Restart the proxy again to apply changes

---

### 🔨 Building from Source

**Requirements:** Java 11+, Maven 3.6+

```bash
git clone https://github.com/sus091992-cyber/Velocity-bridge-plugin.git
cd Velocity-bridge-plugin
mvn package
# Output: target/AuthBridge-3.0.0.jar
```

---

### ⚙️ Configuration

#### config.yml

```yaml
# Name of your authentication server (must match velocity.toml)
auth-server: "auth"

# Servers that are completely inaccessible to players (denied at pre-connect)
blocked-servers:
  - "admin"
  - "maintenance"

# Custom command shortcuts  (alias → target command)
custom-aliases:
  "/hub": "/server lobby"
  "/l":   "/server lobby"

# Hide players from each other while on the auth server
player-hider:
  enabled: true

# Fake /plugins response — shown instead of the real plugin list
fake-plugin:
  name:    "NYXCRAFT"
  color:   "&5"
  message: "&7Plugins (&a1&7): %plugin%"
  footer:  "&7There are &a1&7 plugins installed."

# Messages sent to players
messages:
  not-logged-in:   "&cYou must login first!"
  command-blocked: "&cThis command is blocked!"
  server-blocked:  "&cYou cannot connect to that server!"

settings:
  auto-alias:
    enabled: true   # Auto-register /<serverName> for every server in velocity.toml
```

#### whitelist.yml

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

# Commands blocked for ALL players everywhere
globally-blocked-commands:
  - "pl"
  - "plugins"
  - "version"
  - "ver"
  - "about"

# Commands that always bypass all blocks (login / register)
always-allowed-commands:
  - "login"
  - "register"
  - "changepass"
```

---

### 🔄 Authentication Flow

```
Player connects to proxy
        │
        ▼
 Sent to auth server ──────────────── blocked from tab-complete plugin leak
        │                             hidden from other players
   /login or /register
        │
        ▼
 AuthMe authenticates (backend)
        │
        ▼
 AuthMe redirects player → lobby
        │
        ╰── AuthBridge detects this transition
        │   (auth server → other server)
        ▼
 Player marked as authenticated
        │
        ▼
 Full access to all non-blocked servers
```

> AuthBridge detects authentication by tracking the **auth server → other server transition**. No Bukkit/AuthMe API is required on the proxy side.

---

### 🛡️ Security Summary — What hack clients see

| Detection method | Without AuthBridge | With AuthBridge |
|---|---|---|
| `/plugins` tab-complete namespace scan | All plugin names exposed | Empty / allowlist only |
| `/plugins` command | Error or plugin list | Fake single-plugin response |
| `minecraft:brand` packet | `Paper` / `Purpur` / etc. | `Minecraft` |
| `minecraft:register` channels | All plugin channels listed | Only vanilla debug channels |
| `/server` tab-complete | All servers including blocked | Only accessible servers |

---

### 📄 License

MIT License — see [LICENSE](LICENSE) for details.

### 👤 Author

**S1MPLE** — [GitHub](https://github.com/sus091992-cyber)

---
---

<a name="فارسی"></a>

## 🇮🇷 AuthBridge — پلاگین احراز هویت حرفه‌ای برای ولاسیتی

<div dir="rtl">

AuthBridge یک پلاگین **احراز هویت** حرفه‌ای برای پروکسی [Velocity](https://papermc.io/software/velocity) است. این پلاگین تمام بازیکنان را قبل از ورود به سرورهای اصلی، در سرور ورود (auth server) نگه می‌دارد — و همزمان از شناسایی پلاگین‌ها توسط کلاینت‌های هک جلوگیری می‌کند.

---

### ✨ قابلیت‌ها

#### 🔐 سیستم احراز هویت
- بازیکنان همیشه اول به **سرور ورود** متصل می‌شوند و تا زمان لاگین همانجا می‌مانند
- وضعیت احراز هویت بر اساس **UUID** ذخیره می‌شود و با هر قطع اتصال پاک می‌شود
- بازیکنانی که دوباره وصل می‌شوند همیشه از نو شروع می‌کنند
- پس از لاگین با AuthMe، پلاگین انتقال بازیکن از سرور auth به سرور دیگر را شناسایی و احراز هویت را تأیید می‌کند

#### 🚫 کنترل دستورات
- دستوراتی مثل `/plugins`، `/pl`، `/ver`، `/version`، `/about` برای **همه** بلاک هستند
- بازیکنان **لاگین‌نشده** فقط می‌توانند دستورات مجاز (مثل `/login` و `/register`) را اجرا کنند
- دستورات تغییر سرور (`/hub`، `/lobby`، `/survival` ...) روی سرور auth بلاک هستند

#### 🎭 لیست پلاگین جعلی *(جدید در نسخه ۳.۰.۰)*
- به جای بلاک کردن ساده `/plugins`، پلاگین یک **پاسخ جعلی واقع‌بینانه** ارسال می‌کند
- تمام variant‌ها پوشش داده شده‌اند:
  - `/plugins`، `/pl`، `/plugin`، `/version`، `/ver`، `/about`، `/icanhasbukkit`
  - فرم‌های namespace‌دار: `bukkit:pl`، `spigot:pl`، `paper:pl`، `velocity:pl`، `waterfall:pl`، `bungeecord:pl`، `minecraft:plugins` و ...
- نام پلاگین جعلی، رنگ و فرمت پیام کاملاً از `config.yml` قابل تنظیم است

#### 🕵️ قفل Tab-Complete ضد هک کلاینت *(جدید در نسخه ۳.۰.۰)*
کلاینت‌های هک مدرن مثل Meteor، Wurst، Aristois، Sigma و Impact با زدن `/` و Tab، لیست پلاگین‌ها را شناسایی می‌کنند — سرور پیشنهاداتی مثل `authbridge:reload` یا `luckperms:user` ارسال می‌کند که مستقیماً نام پلاگین‌ها را لو می‌دهد.

AuthBridge این روش را **کاملاً** می‌بندد (با `PostOrder.LAST` — همیشه آخرین حرف را می‌زند):
- بازیکنان **لاگین‌نشده**: فقط `/login`، `/register` و دستورات مجاز نشان داده می‌شوند
- **همه بازیکنان**: هر پیشنهادی که شامل `:` باشد (مثل `authbridge:reload`) حذف می‌شود
- دستورات بلاک‌شده از لیست پیشنهادات همه حذف می‌شوند

#### 📡 محافظت از Plugin Channel *(جدید در نسخه ۳.۰.۰)*
سرورهای بک‌اند دو packet ارسال می‌کنند که اطلاعات پلاگین‌ها و سرور را لو می‌دهند:

| Channel | چه اطلاعاتی لو می‌دهد | واکنش AuthBridge |
|---|---|---|
| `minecraft:brand` / `MC|Brand` | نرم‌افزار سرور (`Paper`، `Purpur` و ...) | جعل به `"Minecraft"` |
| `minecraft:register` / `REGISTER` | نام تمام channel‌های پلاگین (مثل `authbridge:auth`) | allowlist سخت‌گیر — فقط channel‌های vanilla |

نام‌های legacy مثل `MC|Brand` و `REGISTER` (استفاده‌شده توسط ViaVersion) هم پوشش داده شده‌اند.

#### 👥 مخفی‌کردن بازیکنان
- بازیکنان روی سرور auth از یکدیگر **مخفی** هستند
- بعد از انتقال به سرور دیگر به‌صورت خودکار نمایش داده می‌شوند

#### 📌 Alias (میانبر) برای دستورات
- میانبرهای دلخواه در `config.yml`: `/hub` ← `/server lobby`
- **Auto-alias**: به‌صورت خودکار برای هر سرور در `velocity.toml` یک alias می‌سازد
- سرورهای بلاک‌شده از auto-alias و tab-complete `/server` حذف می‌شوند

#### 🔒 بلاک کردن سرورها *(فیکس‌شده در نسخه ۳.۰.۰)*
- سرورهای موجود در `blocked-servers` در مرحله **pre-connect** بلاک می‌شوند — قبل از برقراری اتصال
- نام سرورهای بلاک‌شده در پیشنهادات tab-complete نمایش داده نمی‌شود

---

### 📋 پیش‌نیازها

| وابستگی | نسخه |
|---|---|
| Java | 11 یا بالاتر |
| Velocity | 3.0.1 یا بالاتر |
| Maven | 3.6+ (فقط برای build) |

> AuthBridge هیچ وابستگی runtime ندارد — نه AuthMe API، نه ProtocolLib. JAR کاملاً خودکفاست.

---

### 📦 نصب

1. فایل `AuthBridge-3.0.0.jar` را از [Releases](https://github.com/sus091992-cyber/Velocity-bridge-plugin/releases) دانلود کنید
2. آن را در پوشه `plugins/` پروکسی Velocity قرار دهید
3. پروکسی را ری‌استارت کنید — فایل‌های config به‌صورت خودکار در `plugins/authbridge/` ساخته می‌شوند
4. `config.yml` و `whitelist.yml` را مطابق شبکه خود ویرایش کنید
5. پروکسی را دوباره ری‌استارت کنید

---

### 🔨 Build از سورس

**پیش‌نیاز:** Java 11+، Maven 3.6+

```bash
git clone https://github.com/sus091992-cyber/Velocity-bridge-plugin.git
cd Velocity-bridge-plugin
mvn package
# خروجی: target/AuthBridge-3.0.0.jar
```

---

### ⚙️ تنظیمات

#### config.yml

```yaml
# نام سرور احراز هویت (باید با velocity.toml یکسان باشد)
auth-server: "auth"

# سرورهایی که بازیکنان اصلاً نمی‌توانند به آنها وصل شوند
blocked-servers:
  - "admin"
  - "maintenance"

# میانبرهای دلخواه
custom-aliases:
  "/hub": "/server lobby"
  "/l":   "/server lobby"

# مخفی کردن بازیکنان روی سرور auth
player-hider:
  enabled: true

# پاسخ جعلی برای /plugins
fake-plugin:
  name:    "NYXCRAFT"
  color:   "&5"
  message: "&7Plugins (&a1&7): %plugin%"
  footer:  "&7There are &a1&7 plugins installed."

# پیام‌های نمایش داده‌شده به بازیکن
messages:
  not-logged-in:   "&cابتدا باید وارد شوید!"
  command-blocked: "&cاین دستور مسدود است!"
  server-blocked:  "&cامکان اتصال به این سرور وجود ندارد!"

settings:
  auto-alias:
    enabled: true
```

#### whitelist.yml

```yaml
# دستورات مجاز روی سرور auth (قبل از لاگین)
whitelisted-commands-on-auth:
  - "login"
  - "register"
  - "changepass"
  - "help"

# دستورات تغییر سرور (بلاک روی سرور auth)
server-switching-commands:
  - "server"
  - "hub"
  - "lobby"
  - "survival"

# دستورات بلاک‌شده برای همه
globally-blocked-commands:
  - "pl"
  - "plugins"
  - "version"
  - "ver"
  - "about"

# دستوراتی که همیشه اجرا می‌شوند
always-allowed-commands:
  - "login"
  - "register"
  - "changepass"
```

---

### 🔄 مسیر احراز هویت

```
بازیکن به پروکسی متصل می‌شود
        │
        ▼
 به سرور auth فرستاده می‌شود ─── tab-complete پلاگین مسدود
        │                         از سایر بازیکنان مخفی
   /login یا /register
        │
        ▼
 AuthMe احراز هویت می‌کند (backend)
        │
        ▼
 AuthMe بازیکن را به lobby منتقل می‌کند
        │
        ╰── AuthBridge این انتقال را تشخیص می‌دهد
        │   (auth server → سرور دیگر)
        ▼
 بازیکن احراز هویت‌شده تلقی می‌شود
        │
        ▼
 دسترسی کامل به همه سرورهای مجاز
```

---

### 🛡️ خلاصه امنیت — هک کلاینت‌ها چه می‌بینند

| روش تشخیص | بدون AuthBridge | با AuthBridge |
|---|---|---|
| Tab-complete namespace scan | نام همه پلاگین‌ها | خالی / فقط allowlist |
| دستور `/plugins` | لیست واقعی | یک پلاگین جعلی |
| پکت `minecraft:brand` | `Paper` / `Purpur` / ... | `Minecraft` |
| Channel‌های `minecraft:register` | همه channel‌های پلاگین | فقط channel‌های vanilla |
| Tab-complete `/server` | همه سرورها | فقط سرورهای مجاز |

---

### 📄 لایسنس

MIT License — فایل [LICENSE](LICENSE) را ببینید.

### 👤 سازنده

**S1MPLE** — [GitHub](https://github.com/sus091992-cyber)

</div>
