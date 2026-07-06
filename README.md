<div align="center">

# 🔐 AuthBridge

**A professional authentication bridge plugin for Velocity proxy**
**پلاگین حرفه‌ای احراز هویت برای پروکسی Velocity**

![Version](https://img.shields.io/badge/version-3.1.0-blue)
![Velocity](https://img.shields.io/badge/Velocity-3.x-green)
![Java](https://img.shields.io/badge/Java-17+-orange)
![License](https://img.shields.io/badge/license-MIT-purple)

</div>

---

<details open>
<summary>🇬🇧 English</summary>

## 📋 Features

### 🔒 Authentication Gate
Players are automatically routed to the **auth server** when they first connect. Until they log in via AuthMe (or a compatible plugin), they are completely locked in place — no server switching, no unauthorized commands.

- Tracks authenticated vs. unauthenticated state per player
- Auth state is cleared on every disconnect so reconnecting players always start fresh
- Supports external auth confirmation via plugin messaging channel (`authbridge:auth`)

---

### 👻 Player Hider (Tab List)
Any player on the auth server is **completely invisible** to everyone else — and sees no one themselves.

- Removed from every other player's tab list the moment they join the auth server
- Their own tab list is fully wiped on join
- Restored automatically after a successful login and server transfer
- No player on auth can see any other player, including other players also on auth
- Configurable via `player-hider.enabled` and `player-hider.hide-in-tablist`

> **In-game (world) visibility:** Velocity is a proxy and cannot send entity packets directly.
> To hide players in the game world, configure your backend AuthMe plugin to set joining players to **SPECTATOR** mode.

---

### ⏱️ RaidBar Countdown Timer
A **pink raid-style bar** counts down the seconds a player has left to log in.

- Shown immediately when a player lands on the auth server
- Title updates every second with remaining time via `%timer_bos%`
- Bar shrinks as time runs out
- Player is kicked with a configurable message when the timer reaches zero
- Cancelled automatically on successful login
- Color, duration, and message are fully configurable

```yaml
raidbar:
  enabled: true
  timer: 60          # seconds before kick
  color: PINK        # PINK, RED, BLUE, GREEN, YELLOW, PURPLE, WHITE
  message: "&fYou only have &c%timer_bos% &fseconds to login"
```

---

### 🚫 Blocked Servers
Certain servers can be made completely inaccessible to all players.

- Configured in the `blocked-servers` list
- Connection is denied **before** it is established (pre-connect event)
- Players receive a configurable `server-blocked` message

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
Automatically registers a `/servername` command for every server defined in `velocity.toml`.

- Skips the auth server and any blocked servers
- Can be disabled with `settings.auto-alias.enabled: false`

---

### 🛡️ Command Gating
Unauthenticated players on the auth server are restricted to a whitelist of allowed commands.

- Globally blocked commands (`/plugins`, `/pl`, `/version`, `/ver`, `/about`) are hidden from **all** players
- Unauthenticated players can only run commands listed in `whitelist.yml`
- Authenticated players on the auth server cannot use server-switching commands

---

### 🎭 Fake Plugin List
Responds to `/plugins`, `/pl`, `/version`, or `/about` with a **fake plugin list** instead of revealing the real server stack.

```yaml
fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""
```

---

### 🔏 Tab-Complete Lockdown
Tab-complete suggestions are sanitized for all players at `PostOrder.LAST` priority.

- **Unauthenticated players:** suggestions are replaced with only the allowed commands from the whitelist
- **All players:** namespaced suggestions (e.g. `authbridge:reload`) are stripped
- **All players:** globally blocked commands are removed from suggestions

---

### 📡 Plugin Channel Guard
Intercepts outgoing plugin channel messages from backend servers.

- `minecraft:brand` → spoofed to `"Minecraft"`
- `minecraft:register` → plugin namespaces stripped from payload

---

## ⚙️ Configuration

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

## 📁 whitelist.yml

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

## 🚀 Installation

1. Build: `mvn clean package`
2. Copy `target/AuthBridge-3.1.0.jar` into Velocity `plugins/`
3. Start Velocity once to generate config files
4. Edit `config.yml` and `whitelist.yml` to match your setup
5. Restart Velocity

## 🔌 Compatibility

| Software | Version |
|----------|---------|
| Velocity | 3.x |
| Java | 17+ |
| AuthMe (backend) | Any version with plugin messaging |

## 👤 Author
**S1MPLE** — AuthBridge v3.1.0

</details>

---

<details>
<summary>🇮🇷 فارسی</summary>

## ✨ قابلیت‌ها

### 🔒 دروازه احراز هویت
پلیرها به محض اتصال به پروکسی، به طور خودکار وارد **سرور auth** می‌شوند. تا زمانی که از طریق AuthMe (یا پلاگین سازگار دیگری) لاگین نکنند، کاملاً محدود هستند — هیچ جابه‌جایی سرور و هیچ دستور غیرمجازی امکان‌پذیر نیست.

- وضعیت احراز هویت هر پلیر به صورت جداگانه ردیابی می‌شود
- با هر disconnect، وضعیت پاک می‌شود — پلیر مجبور است دوباره لاگین کند
- پشتیبانی از تأیید خارجی احراز هویت از طریق plugin messaging (`authbridge:auth`)

---

### 👻 مخفی‌سازی پلیر (تب‌لیست)
هر پلیری که وارد سرور auth شود **کاملاً از دید همه پنهان می‌شود** — و خودش هم هیچ‌کس را نمی‌بیند.

- به محض ورود به auth، از تب‌لیست تمام پلیرهای دیگر حذف می‌شود
- تب‌لیست خود پلیر نیز کاملاً پاک می‌شود
- بعد از لاگین موفق و انتقال به سرور دیگر، به صورت خودکار بازمی‌گردد
- هیچ پلیری در auth نمی‌تواند پلیر دیگری را ببیند، حتی پلیرهای دیگری که در auth هستند
- قابل تنظیم از طریق `player-hider.enabled` و `player-hider.hide-in-tablist`

> **مخفی‌سازی در دنیای بازی:** Velocity یک پروکسی است و نمی‌تواند مستقیماً packet موجودیت ارسال کند.
> برای مخفی‌کردن پلیرها در دنیا، پلاگین AuthMe در بک‌اند را طوری تنظیم کنید که پلیرهای وارد شده را روی حالت **SPECTATOR** بگذارد.

---

### ⏱️ تایمر RaidBar (نوار رید)
یک **نوار رید صورتی** ثانیه‌شماری می‌کند تا پلیر چقدر وقت برای لاگین دارد.

- بلافاصله پس از ورود به سرور auth نمایش داده می‌شود
- هر ثانیه عنوان با زمان باقی‌مانده بروز می‌شود (`%timer_bos%`)
- نوار با گذشت زمان کوچک می‌شود
- وقتی تایمر به صفر رسید، پلیر با پیام قابل تنظیم کیک می‌شود
- با لاگین موفق خودکار متوقف می‌شود

```yaml
raidbar:
  enabled: true
  timer: 60          # ثانیه تا کیک
  color: PINK        # PINK، RED، BLUE، GREEN، YELLOW، PURPLE، WHITE
  message: "&fYou only have &c%timer_bos% &fseconds to login"
```

---

### 🚫 سرورهای مسدود
برخی سرورها می‌توانند برای همه پلیرها کاملاً غیرقابل دسترس باشند.

- در لیست `blocked-servers` تنظیم می‌شود
- اتصال **قبل از برقراری** رد می‌شود (pre-connect event)
- پلیر پیام قابل تنظیم `server-blocked` دریافت می‌کند

---

### 🔗 دستورات میانبر سفارشی
دستورات کوتاه‌نویسی تعریف کنید که به دستورات دیگر هدایت می‌شوند.

```yaml
custom-aliases:
  "/hub": "/server lobby"
  "/l": "/server lobby"
  "/spawn": "/server lobby"
```

---

### 🤖 ثبت خودکار Alias
به طور خودکار دستور `/servername` برای هر سروری که در `velocity.toml` تعریف شده ثبت می‌کند.

- سرور auth و سرورهای مسدود را رد می‌کند
- با `settings.auto-alias.enabled: false` قابل غیرفعال‌کردن است

---

### 🛡️ محدودیت دستورات
پلیرهای لاگین‌نشده در سرور auth فقط به لیست سفید دستورات دسترسی دارند.

- دستورات بلاک شده جهانی (`/plugins`، `/pl`، `/version`، `/ver`، `/about`) از **همه** پنهان است
- پلیرهای لاگین‌نشده فقط دستورات موجود در `whitelist.yml` را می‌توانند اجرا کنند
- پلیرهای احراز هویت شده در auth نمی‌توانند دستورات جابه‌جایی سرور را اجرا کنند

---

### 🎭 لیست پلاگین جعلی
وقتی پلیری `/plugins`، `/pl`، `/version` یا `/about` را اجرا می‌کند، به جای افشای استک واقعی سرور، **لیست پلاگین جعلی** ارسال می‌شود.

```yaml
fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""
```

---

### 🔏 قفل Tab-Complete
پیشنهادات tab-complete برای همه پلیرها با اولویت `PostOrder.LAST` پاکسازی می‌شود.

- **پلیرهای لاگین‌نشده:** فقط دستورات مجاز از whitelist پیشنهاد می‌شود
- **همه پلیرها:** پیشنهادات namespace‌دار (مثلاً `authbridge:reload`) همیشه حذف می‌شوند
- **همه پلیرها:** دستورات بلاک شده جهانی از پیشنهادات حذف می‌شوند

---

### 📡 محافظ کانال پلاگین
پیام‌های کانال پلاگین ارسال شده از سرورهای بک‌اند را رهگیری می‌کند.

- `minecraft:brand` ← جعل می‌شود به `"Minecraft"`
- `minecraft:register` ← namespace های پلاگین از payload حذف می‌شوند

---

## ⚙️ تنظیمات

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

## 📁 whitelist.yml

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

## 🚀 نصب

1. پلاگین را بیلد کنید: `mvn clean package`
2. فایل `target/AuthBridge-3.1.0.jar` را در پوشه `plugins/` سرور Velocity کپی کنید
3. یک بار Velocity را راه‌اندازی کنید تا فایل‌های تنظیمات ساخته شوند
4. `config.yml` و `whitelist.yml` را مطابق سرور خود ویرایش کنید
5. Velocity را ری‌استارت کنید

## 🔌 سازگاری

| نرم‌افزار | نسخه |
|----------|------|
| Velocity | 3.x |
| Java | 17+ |
| AuthMe (بک‌اند) | هر نسخه با پشتیبانی plugin messaging |

## 👤 سازنده
**S1MPLE** — AuthBridge v3.1.0

</details>
