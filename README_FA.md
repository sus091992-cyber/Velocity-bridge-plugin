<div align="center">

# 🔐 AuthBridge

**پلاگین حرفه‌ای احراز هویت برای پروکسی Velocity**

![Version](https://img.shields.io/badge/نسخه-3.1.0-blue)
![Velocity](https://img.shields.io/badge/Velocity-3.x-green)
![Java](https://img.shields.io/badge/Java-17+-orange)

</div>

---

## ✨ قابلیت‌ها

### 🔒 دروازه احراز هویت
پلیرها به محض اتصال به پروکسی، به طور خودکار وارد **سرور auth** می‌شوند. تا زمانی که از طریق AuthMe (یا پلاگین سازگار دیگری) لاگین نکنند، کاملاً محدود هستند.

- وضعیت احراز هویت هر پلیر به صورت جداگانه ردیابی می‌شود
- با هر disconnect، وضعیت پاک می‌شود — پلیر مجبور است دوباره لاگین کند
- پشتیبانی از تأیید خارجی احراز هویت از طریق plugin messaging (`authbridge:auth`)

---

### 👻 مخفی‌سازی پلیر (تب‌لیست)
هر پلیری که وارد سرور auth شود **کاملاً از دید همه پنهان می‌شود** — و خودش هم کسی را نمی‌بیند.

- به محض ورود به auth، از تب‌لیست تمام پلیرهای دیگر حذف می‌شود
- تب‌لیست خود پلیر نیز کاملاً پاک می‌شود
- بعد از لاگین موفق و انتقال به سرور دیگر، به صورت خودکار بازمی‌گردد
- هیچ پلیری در auth نمی‌تواند پلیر دیگری را ببیند — حتی پلیرهای دیگری که در auth هستند
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
- رنگ، مدت، و پیام کاملاً قابل تنظیم است

```yaml
raidbar:
  enabled: true
  timer: 60          # ثانیه تا کیک
  color: PINK        # رنگ (PINK، RED، BLUE، GREEN، YELLOW، PURPLE، WHITE)
  message: "&fYou only have &c%timer_bos% &fseconds to login"
```

---

### 🚫 سرورهای مسدود
برخی سرورها می‌توانند برای همه پلیرها کاملاً غیرقابل دسترس باشند.

- در لیست `blocked-servers` تنظیم می‌شود
- اتصال **قبل از برقراری** رد می‌شود (pre-connect event)
- پلیر پیام قابل تنظیم "server-blocked" دریافت می‌کند

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
- مثال: `/survival` به طور خودکار alias برای `/server survival` می‌شود

---

### 🛡️ محدودیت دستورات
پلیرهای لاگین‌نشده در سرور auth فقط به لیست سفید دستورات دسترسی دارند.

- دستورات بلاک شده جهانی (`/plugins`، `/pl`، `/version`، `/ver`، `/about`) از **همه** پنهان است
- پلیرهای لاگین‌نشده فقط دستورات موجود در `whitelist.yml` را می‌توانند اجرا کنند
- پلیرهای احراز هویت شده در auth نمی‌توانند دستورات جابه‌جایی سرور را اجرا کنند
- لیست سفید از طریق `whitelist.yml` قابل تنظیم است

---

### 🎭 لیست پلاگین جعلی
وقتی پلیری `/plugins`، `/pl`، `/version` یا `/about` را اجرا می‌کند، پلاگین به جای افشای استک واقعی سرور، **لیست پلاگین جعلی** ارسال می‌کند.

- پیشوند، نام پلاگین و footer کاملاً قابل تنظیم است
- از شناسایی پلاگین توسط کلاینت‌های هک جلوگیری می‌کند

```yaml
fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""
```

---

### 🔏 قفل Tab-Complete
پیشنهادات tab-complete برای همه پلیرها با اولویت `PostOrder.LAST` پاکسازی می‌شود.

- **پلیرهای لاگین‌نشده:** پیشنهادات فقط شامل دستورات مجاز از whitelist است
- **همه پلیرها:** پیشنهادات namespace‌دار (مثلاً `authbridge:reload`) همیشه حذف می‌شوند — کلاینت‌های هک از این برای شناسایی پلاگین‌ها استفاده می‌کنند
- **همه پلیرها:** دستورات بلاک شده جهانی از پیشنهادات حذف می‌شوند

---

### 📡 محافظ کانال پلاگین
پیام‌های کانال پلاگین ارسال شده از سرورهای بک‌اند را رهگیری می‌کند تا از شناسایی پلاگین جلوگیری شود.

- `minecraft:brand` ← جعل می‌شود به `"Minecraft"`
- `minecraft:register` ← namespace های پلاگین از payload حذف می‌شوند

---

## ⚙️ تنظیمات

فایل کامل `config.yml`:

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

# مخفی‌سازی پلیرهای سرور auth از دید همه (تب‌لیست)
player-hider:
  enabled: true
  hide-in-tablist: true

fake-plugin:
  prefix: "&a&lNYX&f&lCORE"
  message: "&7Plugins (&a1&7): %plugin%"
  footer: ""

# تایمر RaidBar برای پلیرهای در سرور auth
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

کنترل می‌کند که پلیرهای لاگین‌نشده در سرور auth چه دستوراتی می‌توانند اجرا کنند.

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

## 🚀 نصب

1. پلاگین را با Maven بیلد کنید: `mvn clean package`
2. فایل `.jar` از پوشه `target/` را در پوشه `plugins/` سرور Velocity کپی کنید
3. یک بار Velocity را راه‌اندازی کنید تا `config.yml` و `whitelist.yml` ساخته شوند
4. هر دو فایل را مطابق با تنظیمات سرور خود ویرایش کنید
5. Velocity را ری‌استارت کنید

---

## 🔌 سازگاری

| نرم‌افزار | نسخه |
|----------|------|
| Velocity | 3.x |
| Java | 17+ |
| AuthMe (بک‌اند) | هر نسخه با پشتیبانی plugin messaging |

---

## 👤 سازنده

**S1MPLE** — AuthBridge v3.1.0
