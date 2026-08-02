# Privacy Policy for Codex Usage Monitor

Last updated: August 2, 2026

Codex Usage Monitor is an independent, unofficial Android application. It is not affiliated with, endorsed by, or sponsored by OpenAI.

## Data handled by the app

The app processes the following data only to provide its usage-monitoring features:

- OpenAI device-authorization codes and OAuth access/refresh tokens;
- an account identifier and email address when they are present in the authentication response;
- Codex usage percentages, limits, and reset times;
- notification and refresh preferences.

Authentication and usage requests are sent directly from the device to `auth.openai.com` and `chatgpt.com`. The developer operates no server for this app and does not receive this data.

OAuth tokens are encrypted on the device using Android Keystore. Usage information and preferences are stored locally. Android cloud backup and device-transfer backup are disabled for the app. Data is not sold, used for advertising, or shared by the developer.

## Retention and deletion

Local data remains on the device until the user signs out, clears the app's storage, or uninstalls the app. Signing out removes the locally stored OAuth tokens and usage data. The app does not create or delete the user's OpenAI account.

## Third-party services

The app communicates with OpenAI services under the user's OpenAI account and applicable OpenAI terms and privacy policy. The app uses no advertising or analytics SDKs.

## Security

The app requires encrypted HTTPS connections and does not use copied browser cookies or OpenAI API keys. No method of storage or transmission is completely secure.

## Changes and contact

Material changes will be published in this repository. Questions may be submitted at <https://github.com/guberm/Codex-Usage-Android/issues>.
