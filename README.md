# OptionPulse Android

Native Android alert client for an NSE F&O naked-option momentum scanner. The included demo repository makes the UI immediately previewable without credentials.

## What is implemented

- Live-style dashboard for ~210 NSE F&O underlyings, feed health, India VIX, alert cap, and latency.
- Ranked Call/Put setup cards and complete signal detail: Gann trigger/target, VWAP/volume/pattern/astro/OI validation, liquidity, spot stop, premium stop.
- Dynamic strike-step and ATM/OTM1 selection logic with unit tests.
- Square-of-9 engine for 45°, 90°, 135°, 180°, 225°, 270°, 315°, 360°, 540° and 720° projections, reverse-angle calculation, support clamping, and trade-plan mapping.
- Alert-only/paper-mode language and risk warnings.
- Secure client boundary: Upstox, Telegram, Pushover, Redis, and ephemeris secrets are intentionally not embedded in the APK.

## Open in Android Studio

1. Open the `NakedOptionScanner` folder.
2. Let Android Studio create/download the Gradle wrapper when prompted and sync the project.
3. Run the `app` configuration on Android 8.0+.

## Build the APK with GitHub Actions

1. Create a GitHub repository and push the contents of this `NakedOptionScanner` directory to its `main` branch.
2. Open the repository's **Actions** tab and select **Android CI**.
3. Choose **Run workflow**, or simply push a commit to `main`.
4. After the build succeeds, open the workflow run and download **OptionPulse-debug-apk** from **Artifacts**.

The workflow uses Java 17 and Gradle 8.9, runs `testDebugUnitTest`, builds `assembleDebug`, and retains the debug APK for 14 days. No Upstox, Telegram, Pushover, or signing credentials belong in this mobile repository.

For a Play Store/release APK, use a GitHub Environment with approval protection and encrypted Actions secrets for a dedicated Android signing key. Do not reuse Upstox or notification credentials as signing secrets.

## Production API contract

Replace `DemoScannerRepository` with a Retrofit repository calling your VPS over HTTPS:

- `GET /v1/status` → `MarketStatus`
- `GET /v1/signals?min_score=80` → array of `Signal`
- `GET /v1/signals/stream` → authenticated WebSocket/SSE updates
- `POST /v1/devices/register` → FCM registration token

### Gann convention used

The app implements the supplied convention exactly:

- Resistance: `(sqrt(pivot) + angle/180)^2`
- Support: `max(0, sqrt(pivot) - angle/180)^2`
- Reverse angle: `abs(sqrt(current) - sqrt(pivot)) * 180`

For a ₹1,400 low pivot, the Call plan is 90° trigger ₹1,437.67, 45° protective level ₹1,418.77, 180° target ₹1,475.83, and 360° target ₹1,553.67. The scanner must only confirm a trigger after a completed 5-minute candle; a transient tick above the level is not a close.

Authenticate mobile requests with short-lived user access tokens. Keep Upstox API secret/access token, Telegram token, Pushover token, Swiss Ephemeris calculations and Redis on the VPS. Pin production TLS and reject cleartext traffic.

## Important corrections before live use

- Do not generate Upstox OAuth tokens unattended unless the current Upstox flow explicitly permits it; use the supported interactive authorization flow and refresh/re-authorize as required.
- Derive strikes from the live instrument master/option chain. Price-tier rounding is a fallback only; NSE strike intervals can differ by contract.
- Define an explicit maximum spread formula, e.g. `(ask-bid)/mid*100 <= 1%`; the source blueprint omitted the threshold.
- The Moon/Saturn filter is included as an optional rule, not a validated market edge. Backtest it separately and keep it disabled unless evidence supports it.
- “Under 10 ms to NSE” cannot be guaranteed through a retail broker API; monitor end-to-end tick and alert latency instead.
- Pushover emergency priority may repeat alerts but cannot universally bypass Android Do Not Disturb settings.

This project does not place orders. Naked options can lose 100% of premium; paper-test, account for slippage/fees, and validate out-of-sample before using real capital.

## Background alerts deployment

The Android client now uses the live HTTPS API and includes an FCM messaging service, a high-priority notification channel, Android 13+ permission handling, offline/error state, and system-bar inset handling. Scanning remains on the VPS, so alerts can arrive while the app is minimized or closed.

The `backend/` FastAPI service supplies status/signals, registers FCM devices, enforces score/cooldown/daily caps in Redis, and dispatches Telegram plus FCM concurrently. `docker-compose.yml` runs the API and persistent Redis on Ubuntu.

Before production use:

1. Add your Firebase Android app's `google-services.json` to `app/` locally or inject it from an encrypted GitHub Actions secret. It is intentionally ignored by Git. Supply the registration-only token as Gradle property `OPTIONPULSE_DEVICE_TOKEN`; it is separate from the internal signal-ingestion token.
2. Put the Firebase service-account JSON only on the VPS and set `FIREBASE_CREDENTIALS_PATH`.
3. Copy `.env.example` to `.env`, generate a long random `INTERNAL_API_TOKEN`, and add Upstox/Telegram values. Never commit `.env`.
4. Put an HTTPS reverse proxy in front of port 8080 and change `API_BASE_URL` to that domain.
5. Connect the Upstox WebSocket/candle worker to authenticated `POST /internal/signals`. The API refuses unauthenticated ingestion.

The repository deliberately does not automate live orders. Alerts are advisory and manual-execution only.
