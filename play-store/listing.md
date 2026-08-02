# Google Play listing

## App identity

- Package name: `com.guberdev.codexusage`
- App name: `Codex Usage Monitor`
- Default language: English (United States)
- Category: Tools
- Ads: No
- Target audience: 18 and over; not designed for children
- Privacy policy: <https://github.com/guberm/Codex-Usage-Android/blob/main/PRIVACY.md>

## Upload assets

- Store icon: `play-store/images/icon-512.png` (512×512 PNG)
- Feature graphic: `play-store/images/feature-graphic-1024x500.png` (1024×500 PNG)
- Phone screenshot: `play-store/screenshots/01-main.png` (1080×1920 PNG)
- Phone screenshot: `play-store/screenshots/02-widget.png` (1080×1920 PNG)

## Short description

Track remaining Codex usage, reset times, widgets, and alerts.

## Full description

Codex Usage Monitor helps you check the usage limits available to your ChatGPT/Codex account.

See your remaining percentage and reset time at a glance. Add a compact Home screen widget, use a Quick Settings tile for fast refreshes, and receive notifications when your remaining usage changes by the amount you choose.

Features:

- remaining percentage and reset time;
- optional additional usage limits when available;
- 2×1 Home screen widget;
- Quick Settings tile with tap-to-refresh;
- periodic background checks;
- configurable check interval and change alerts;
- secure ChatGPT device authorization;
- encrypted local token storage with Android Keystore;
- no ads or analytics SDKs.

The app connects directly to OpenAI services. It does not use copied browser cookies and does not require an OpenAI API key.

Important: this is an independent, unofficial app. It is not affiliated with, endorsed by, or sponsored by OpenAI. Codex, ChatGPT, and OpenAI are trademarks of their respective owner. The authenticated usage endpoint is not a public stable API and may change.

## App access instructions for review

1. Open the app and tap **Sign in with ChatGPT**.
2. Copy the displayed device code.
3. Open the displayed `auth.openai.com/codex/device` page.
4. Sign in with a reviewer-provided ChatGPT account that has Codex access and enter the code.
5. Return to the app; usage details refresh automatically.

The app does not provide or create an OpenAI account. If Play review requires reusable credentials, provide a dedicated test account only in the private **App access** field in Play Console; never add credentials to this repository.

## Data safety facts

- The developer operates no backend and cannot access app data.
- The device communicates directly with OpenAI authentication and ChatGPT usage services.
- OAuth tokens, account identifier/email when present, usage data, and settings are processed for app functionality.
- Tokens are encrypted at rest with Android Keystore; network traffic uses HTTPS.
- Data is not sold, used for ads, or shared by the developer.
- The app contains no ads or analytics SDKs.
- Signing out, clearing app storage, or uninstalling deletes local app data.
- The app does not create user accounts, so the Play account-deletion requirement does not apply.

Use these facts when completing the current Play Console Data safety wizard; its exact questions and definitions can change.

## Permissions explanation

- Internet and network state: authenticate and retrieve usage data.
- Notifications: show user-configured usage-change alerts.
- Boot completed: restore periodic background scheduling after restart.

The Play build does not declare a foreground-service permission or request special-use foreground-service approval.
