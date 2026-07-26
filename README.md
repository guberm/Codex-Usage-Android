# Codex Usage for Android

Неофициальный Android companion для отслеживания лимитов OpenAI Codex.

## Возможности

- оставшийся процент основного Codex Usage limit;
- дата и локальное время следующего reset;
- дополнительные лимиты (например GPT‑5.3‑Codex‑Spark), если они есть в аккаунте;
- Quick Settings tile: тап запускает немедленный refresh;
- постоянный foreground monitor с восстановлением после перезагрузки;
- резервная периодическая задача Android;
- уведомления при изменении оставшегося лимита;
- настройки `Check every` (1/2/4/6/12/24 часа) и `Notify every` (1/2/5/10/20%);
- безопасный ChatGPT device login без API key и без копирования browser cookies.

Значения по умолчанию: проверка каждый час, уведомление при изменении `±1%`.

## Вход и безопасность

Приложение использует device‑authorization flow Codex и получает данные из того же
ChatGPT endpoint, который использует Codex CLI:

`GET https://chatgpt.com/backend-api/wham/usage`

OAuth‑токены шифруются локально ключом из Android Keystore. Backup приложения
отключён; cookies браузера и OpenAI API key не используются.

Endpoint относится к внутреннему ChatGPT/Codex контракту и может измениться.
Приложение не является официальным продуктом OpenAI.

## Сборка

Требования: JDK 17+, Android SDK 36.

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
.\gradlew.bat test assembleDebug
```

Release‑подпись читается только из environment variables:

- `CODEX_USAGE_KEYSTORE_PATH`
- `CODEX_USAGE_KEYSTORE_PASSWORD`
- `CODEX_USAGE_KEY_ALIAS`
- `CODEX_USAGE_KEY_PASSWORD`

GitHub Actions использует соответствующие repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Ограничения Android

Foreground service остаётся активным с постоянной системной нотификацией.
Производитель телефона всё равно может принудительно остановить приложение или
ограничить сеть. Quick Settings tile и кнопка `Обновить сейчас` всегда выполняют
ручную проверку. Уведомление отражает фактическую разницу между двумя успешными
проверками.

## License

MIT
