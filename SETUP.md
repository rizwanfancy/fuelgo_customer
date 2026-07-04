# FuelGo Customer — Android App Setup

Native Android customer portal for FuelGo (Kotlin + Jetpack Compose), wired to the real
`FuelGo.Api` .NET backend. Covers: Splash, Login/Signup, Dashboard, Place Order, Live
Tracking, My Orders (history, cancel, rate), Profile, Logout — plus an in-app **API
Configuration** screen so the server address is never baked into the APK.

## 0. Build a signed release APK via GitHub Actions (recommended)

`.github/workflows/build-release.yml` builds a **signed release** APK (not debug)
automatically on every push, once you push this `FuelGoCustomer` folder to its own
GitHub repo (repo root = this folder):

1. **First, add the signing secrets** (one-time setup) — see `KEYSTORE-INFO.txt` in the
   same delivery as this project for the exact keystore file and passwords. In your repo:
   **Settings → Secrets and variables → Actions → New repository secret**, add:
   - `RELEASE_KEYSTORE_BASE64` — contents of `fuelgo-release.keystore.base64.txt`
   - `RELEASE_KEYSTORE_PASSWORD`
   - `RELEASE_KEY_ALIAS`
   - `RELEASE_KEY_PASSWORD`

   Without these four secrets, the workflow fails fast with a clear error instead of
   silently producing an unsigned build.
2. Push to any branch → the workflow runs automatically, decodes the keystore, and runs
   `gradle assembleRelease`.
3. Download the signed APK from the finished run's **Summary** page, artifact name
   `fuelgo-customer-release-apk`.
4. **Optional:** push a tag like `v1.0.0` (`git tag v1.0.0 && git push origin v1.0.0`) and
   the workflow also publishes the signed APK as a GitHub Release with a direct download
   link — handy for sharing with testers/supervisors without them needing repo access.

Because the server address is now configured **inside the app** (see below), you do
**not** need to edit any file or rebuild before pushing — the same APK works against any
backend URL you type into the app later.

There's also `.github/workflows/build-apk.yml`, kept only for manually grabbing an
unsigned **debug** build (Actions tab → "Build debug APK (manual, legacy)" → Run
workflow) — it no longer runs automatically on push, to avoid duplicate CI runs.

Other cloud options if you want something more full-featured: **Codemagic**
(codemagic.io) has a free tier purpose-built for mobile CI/CD with a no-YAML setup
wizard — connect your GitHub repo and it detects the Gradle project automatically.
**GitHub Codespaces** or **Gitpod** give you a full cloud Linux dev environment with a
terminal, where you'd install the Android SDK once and run Gradle commands directly —
closer to a "remote Android Studio" than a one-shot build.

## 1. Or open it locally in Android Studio

1. Install **Android Studio** (Koala/2024.1 or newer recommended).
2. `File > Open` and select the `MobileApp/FuelGoCustomer` folder.
3. This project has no `gradlew`/Gradle wrapper jar (it couldn't be generated in the
   sandbox that built it — no internet access to download the wrapper binary).
   Android Studio will detect this and offer **"Create Gradle Wrapper"** — accept it,
   or run `gradle wrapper` yourself if you have Gradle installed locally. Either way,
   first sync may take a few minutes while dependencies download.

## 2. Point the app at your backend (no rebuild needed)

The server address is **not** compiled into the APK — it's entered on-device and saved
locally (DataStore), so the exact same APK works against any backend URL. On first
launch (or whenever it isn't configured yet), the app opens **API Configuration**
automatically; you can also reach it any time via the **⋮** menu on the login screen or
the top bar once logged in.

Enter:
- **Server base URL** — e.g. `http://192.168.1.42:5086/api` (your PC's LAN IP; run
  `ipconfig` on the machine running the backend, look for "IPv4 Address" under your
  Wi-Fi/Ethernet adapter). Your phone and PC must be on the same Wi-Fi network.
- **Tenant slug** — defaults to `fuelgo`.

Tap **Test connection** (calls the anonymous `GET /orders/fuel-types` endpoint) before
**Save** to confirm the app can actually reach your backend.

- **Using the Android Emulator instead of a real phone?** Use `10.0.2.2` instead of
  your LAN IP — the emulator maps that address to your host machine's localhost.
- Plain `http://` (not `https://`) is intentional — the backend's HTTPS profile uses a
  local self-signed dev certificate your phone won't trust. `network_security_config.xml`
  (debug build only) permits cleartext HTTP for this reason. Don't ship this to
  production as-is.
- Make sure **Windows Firewall** allows inbound connections on port 5086, and that the
  backend is actually running with the `http` launch profile (or `https` profile, which
  also opens `http://localhost:5086`):
  ```
  cd Project/Backend
  dotnet run --project FuelGo.Api
  ```

## 3. Run it

Select a device (physical phone via USB debugging, or an emulator) in Android Studio
and hit Run. First launch shows a splash screen, then (if not yet configured) API
Configuration, then Login/Create Account.

**Demo login** (seeded automatically on backend startup):
- Tenant: `fuelgo` (hardcoded — this build is single-tenant)
- Username: `admin@fuelgo.local`
- Password: `FuelGo@12345`

Note: the seeded admin account is a `SUPER_ADMIN`, not a customer, so it can log in but
`GET /customers/me` will fail for it (no Customer record exists). Use **Create Account**
in the app to register a real customer — new customers are activated immediately
(`IsActive = true`) so you can sign in right after registering, even though the
confirmation message says "you can sign in after approval" (that text is legacy copy —
the actual current backend doesn't gate login on admin approval).

## 4. What's implemented

| Module | Screen(s) | Backend endpoints |
|---|---|---|
| Splash | `ui/splash/*` | — (routes to Login or Home based on saved session) |
| Login / Signup | `ui/auth/*` | `POST /auth/login`, `POST /customers/register` |
| API Configuration | `ui/config/*` | `GET /orders/fuel-types` (used as "Test connection") |
| Dashboard | `ui/dashboard/*` | `GET /orders/my`, `GET /customers/me` |
| Order | `ui/order/*` | `GET /orders/fuel-types`, `GET/POST /customers/me/addresses`, `POST /orders/place` |
| Tracking | `ui/tracking/*` | `GET /orders/my` (polled every 15s), `PATCH /orders/{id}/cancel` |
| My Orders | `ui/history/*` | `GET /orders/my`, `PATCH /orders/{id}/cancel`, `POST /orders/{id}/rate` |
| Profile / Logout | `ui/profile/*` | `GET /customers/me`, `POST /auth/change-password` |

Bottom navigation (once logged in): **Dashboard · Order · Tracking · My Orders ·
Profile**. The top bar's **⋮** menu opens **API Configuration** from any tab, so the
server address can be changed without logging out.

The Dashboard shows: a greeting, a quick-access card for any active order (with a Track
button), a 2×2 stats grid (total orders, total spent, delivered, active), a horizontal
bar chart of orders by status, and a vertical bar chart of spend over the last 6 months
— all built from `GET /orders/my` + `GET /customers/me`, no extra backend endpoints
needed.

JWT access + refresh tokens are stored in DataStore (`data/local/TokenManager.kt`) and
attached automatically to every request. A 401 triggers an automatic silent refresh via
`POST /auth/refresh-token` (see `data/remote/NetworkModule.kt`); if the refresh token is
also expired, the app clears the session and the next screen load naturally routes back
to Login.

## 5. Known limitations (matches current backend behavior, not app bugs)

- **Order placement requires a saved address.** The Angular frontend also supports
  picking a raw map location instead, but the backend's `PlaceOrderRequest.DeliveryAddressId`
  is a required (non-nullable) field, so that alternate path is unreliable there too —
  this app only implements the saved-address flow, plus an in-app "Add new address" form.
- **"Already rated" state isn't shown** on delivered orders — the backend's `OrderDto`
  doesn't currently return `customerRating` in `GET /orders/my`, even though rating
  submission (`POST /orders/{id}/rate`) works. The Rate button is always available on
  delivered orders.
- **No live map view.** Tracking shows a step-by-step delivery timeline (matches the
  web portal's left panel) plus an "Open in Maps" button that launches the phone's
  native Maps app via an intent — this avoids requiring a Google Maps SDK API key for
  a student project, while still giving real navigation.
- **Inter font not bundled** — the brand typeface used on the web portal isn't included
  as font files, so the app falls back to the platform default sans-serif
  (`ui/theme/Type.kt`). Drop `.ttf` files into `app/src/main/res/font/` and update
  `Type.kt` to use the real typeface if desired.

## 6. Verification note

This project was written without access to Android Studio, Gradle, or the Android SDK,
so it has **not been compiled**. It went through two passes of careful manual review
(including a fresh independent read-through) checking imports, braces/parens balance,
Compose API usage, and DTO field alignment against the actual backend C# records. Please
run a Gradle sync + build as your first step and report back if anything doesn't
compile — most likely culprits for any remaining issue would be a minor import or a
Compose API signature that shifted between library versions.
