package com.guberdev.codexusage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.StatusBarManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

@SuppressLint("SetTextI18n")
class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var percentText: TextView
    private lateinit var resetText: TextView
    private lateinit var updatedText: TextView
    private lateinit var statusText: TextView
    private lateinit var progress: ProgressBar
    private lateinit var additionalContainer: LinearLayout
    private lateinit var loginButton: Button
    private lateinit var refreshButton: Button
    private lateinit var logoutButton: Button
    private lateinit var codePanel: LinearLayout
    private lateinit var codeText: TextView
    private lateinit var checkSpinner: Spinner
    private lateinit var notifySpinner: Spinner
    private var pendingDeviceCode: DeviceCode? = null
    @Volatile private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        requestNotificationPermission()
        loadSettings()
        render(UsageStore(this).load())
    }

    override fun onResume() {
        super.onResume()
        if (SecureTokenStore(this).load() != null) {
            UsageMonitorService.start(this)
            UsageBackupJobService.schedule(this)
        }
        render(UsageStore(this).load())
    }

    override fun onDestroy() {
        destroyed = true
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(30), dp(20), dp(40))
            setBackgroundColor(Color.rgb(7, 18, 47))
        }

        root.addView(
            ImageView(this).apply {
                setImageResource(R.drawable.ic_launcher_foreground)
                contentDescription = "OpenAI Codex usage gauge"
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(dp(128), dp(128)).apply { gravity = Gravity.CENTER_HORIZONTAL },
        )
        root.addView(title("Codex Usage"))
        root.addView(
            bodyText("Неофициальный companion для OpenAI Codex", Color.rgb(173, 216, 230)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        root.addView(spacer(18))

        val usageCard = card()
        percentText = TextView(this).apply {
            text = "—"
            textSize = 48f
            setTextColor(Color.rgb(7, 18, 47))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        usageCard.addView(percentText)
        usageCard.addView(bodyText("осталось", Color.DKGRAY).apply { gravity = Gravity.CENTER_HORIZONTAL })
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
        }
        usageCard.addView(
            progress,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(16)).apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            },
        )
        resetText = bodyText("Сброс: —", Color.rgb(30, 40, 60))
        updatedText = bodyText("Ещё не обновлялось", Color.GRAY)
        statusText = bodyText("Войдите через ChatGPT", Color.rgb(0, 105, 115))
        usageCard.addView(resetText)
        usageCard.addView(updatedText)
        usageCard.addView(statusText)
        additionalContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        usageCard.addView(additionalContainer)
        root.addView(usageCard)
        root.addView(spacer(14))

        val settingsCard = card()
        settingsCard.addView(sectionTitle("Notification settings"))
        settingsCard.addView(bodyText("Check every", Color.DKGRAY))
        checkSpinner = Spinner(this)
        checkSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            MonitorSettingsStore.CHECK_HOUR_OPTIONS.map { "$it ч." },
        )
        settingsCard.addView(checkSpinner)
        settingsCard.addView(bodyText("Notify every", Color.DKGRAY))
        notifySpinner = Spinner(this)
        notifySpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            MonitorSettingsStore.NOTIFY_PERCENT_OPTIONS.map { "$it%" },
        )
        settingsCard.addView(notifySpinner)
        settingsCard.addView(
            Button(this).apply {
                text = "Сохранить настройки"
                setOnClickListener { saveSettings() }
            },
        )
        settingsCard.addView(
            bodyText(
                "По умолчанию: проверка каждый 1 час, уведомление при изменении ±1%.",
                Color.GRAY,
            ),
        )
        root.addView(settingsCard)
        root.addView(spacer(14))

        codePanel = card().apply { visibility = View.GONE }
        codePanel.addView(sectionTitle("Вход через ChatGPT"))
        codePanel.addView(bodyText("Одноразовый код:", Color.DKGRAY))
        codeText = TextView(this).apply {
            textSize = 30f
            setTextColor(Color.rgb(7, 18, 47))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        codePanel.addView(codeText)
        codePanel.addView(
            Button(this).apply {
                text = "Скопировать код и открыть ChatGPT"
                setOnClickListener {
                    pendingDeviceCode?.let { openDeviceLogin(it) }
                }
            },
        )
        root.addView(codePanel)
        root.addView(spacer(14))

        loginButton = actionButton("Войти через ChatGPT") { startDeviceLogin() }
        refreshButton = actionButton("Обновить сейчас") { refreshNow() }
        root.addView(loginButton)
        root.addView(refreshButton)
        root.addView(actionButton("Добавить плитку Quick Settings") { requestTile() })
        logoutButton = actionButton("Выйти") { confirmLogout() }
        root.addView(logoutButton)

        return ScrollView(this).apply { addView(root) }
    }

    private fun startDeviceLogin() {
        setBusy("Запрашиваю код входа…")
        executor.execute {
            try {
                val client = CodexAuthClient()
                val code = client.requestDeviceCode()
                pendingDeviceCode = code
                runOnUiThread {
                    codeText.text = code.userCode
                    codePanel.visibility = View.VISIBLE
                    statusText.text = "Завершите вход в браузере"
                    openDeviceLogin(code)
                }

                val deadline = System.currentTimeMillis() + 15 * 60 * 1000L
                var pending: PendingAuthorization? = null
                while (!destroyed && System.currentTimeMillis() < deadline && pending == null) {
                    Thread.sleep(code.intervalSeconds * 1000L)
                    pending = client.pollDeviceCode(code)
                }
                if (pending == null) error("Код входа истёк. Попробуйте снова.")
                val tokens = client.exchangeCode(pending)
                SecureTokenStore(this).save(tokens)
                if (!destroyed) {
                    runOnUiThread {
                        codePanel.visibility = View.GONE
                        statusText.text = "Вход выполнен. Обновляю Usage…"
                        UsageMonitorService.start(this)
                        UsageBackupJobService.schedule(this)
                        refreshNow()
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (error: Throwable) {
                if (!destroyed) runOnUiThread {
                    setBusy(error.message?.take(160) ?: "Не удалось войти", busy = false)
                }
            }
        }
    }

    private fun openDeviceLogin(code: DeviceCode) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Codex device code", code.userCode))
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUrl)))
        Toast.makeText(this, "Код скопирован", Toast.LENGTH_SHORT).show()
    }

    private fun refreshNow() {
        setBusy("Обновляю…")
        RefreshCoordinator.refresh(this) { result ->
            when (result) {
                is RefreshResult.Success -> {
                    render(result.snapshot)
                    statusText.text = result.delta?.let {
                        val sign = if (it > 0) "+" else ""
                        "Обновлено • изменение $sign$it%"
                    } ?: "Обновлено"
                }
                is RefreshResult.Error -> statusText.text = result.message
            }
            setButtonsEnabled(true)
        }
    }

    private fun render(snapshot: UsageSnapshot?) {
        val signedIn = SecureTokenStore(this).load() != null
        loginButton.visibility = if (signedIn) View.GONE else View.VISIBLE
        logoutButton.visibility = if (signedIn) View.VISIBLE else View.GONE
        refreshButton.isEnabled = signedIn
        if (snapshot == null) {
            percentText.text = "—"
            progress.progress = 0
            resetText.text = "Сброс: —"
            updatedText.text = "Ещё не обновлялось"
            additionalContainer.removeAllViews()
            return
        }
        percentText.text = "${snapshot.primary.remainingPercent}%"
        progress.progress = snapshot.primary.remainingPercent
        resetText.text = "Сброс: ${UsageText.resetDate(snapshot.primary.resetAtEpochSeconds)}"
        updatedText.text = "Обновлено " + DateUtils.getRelativeTimeSpanString(
            snapshot.fetchedAtEpochMillis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        )
        additionalContainer.removeAllViews()
        snapshot.additionalLimits.forEach { limit ->
            additionalContainer.addView(
                bodyText(
                    "${UsageText.featureName(limit.feature)}: ${limit.window.remainingPercent}% • " +
                        "сброс ${UsageText.resetDate(limit.window.resetAtEpochSeconds)}",
                    Color.rgb(30, 40, 60),
                ),
            )
        }
        snapshot.creditBalance?.let {
            additionalContainer.addView(bodyText("Credits: $it", Color.rgb(30, 40, 60)))
        }
    }

    private fun loadSettings() {
        val settings = MonitorSettingsStore(this).load()
        checkSpinner.setSelection(
            MonitorSettingsStore.CHECK_HOUR_OPTIONS.indexOf(settings.checkEveryHours),
        )
        notifySpinner.setSelection(
            MonitorSettingsStore.NOTIFY_PERCENT_OPTIONS.indexOf(settings.notifyEveryPercent),
        )
    }

    private fun saveSettings() {
        val settings = MonitorSettings(
            checkEveryHours = MonitorSettingsStore.CHECK_HOUR_OPTIONS[checkSpinner.selectedItemPosition],
            notifyEveryPercent = MonitorSettingsStore.NOTIFY_PERCENT_OPTIONS[notifySpinner.selectedItemPosition],
        )
        MonitorSettingsStore(this).save(settings)
        UsageBackupJobService.schedule(this)
        UsageMonitorService.restart(this)
        Toast.makeText(
            this,
            "Проверка: ${settings.checkEveryHours} ч., уведомление: ${settings.notifyEveryPercent}%",
            Toast.LENGTH_SHORT,
        ).show()
    }

    private fun requestTile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = getSystemService(StatusBarManager::class.java)
            manager.requestAddTileService(
                ComponentName(this, CodexUsageTileService::class.java),
                "Codex Usage",
                Icon.createWithResource(this, R.drawable.ic_stat_usage),
                mainExecutor,
            ) { result ->
                Toast.makeText(this, "Результат добавления плитки: $result", Toast.LENGTH_SHORT).show()
            }
        } else {
            val quickSettingsIntent = Intent("android.settings.QUICK_SETTINGS_SETTINGS")
            val fallback = Intent(Settings.ACTION_SETTINGS)
            runCatching { startActivity(quickSettingsIntent) }
                .onFailure { startActivity(fallback) }
            Toast.makeText(this, "Перетащите плитку Codex Usage в активные", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Выйти из Codex Usage?")
            .setMessage("OAuth‑токены будут удалены только с этого телефона.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Выйти") { _, _ ->
                UsageMonitorService.stop(this)
                UsageBackupJobService.cancel(this)
                SecureTokenStore(this).clear()
                UsageStore(this).clear()
                render(null)
                statusText.text = "Выполнен выход"
            }
            .show()
    }

    private fun requestNotificationPermission() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 300)
        }
    }

    private fun setBusy(message: String, busy: Boolean = true) {
        statusText.text = message
        setButtonsEnabled(!busy)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        loginButton.isEnabled = enabled
        refreshButton.isEnabled = enabled && SecureTokenStore(this).load() != null
    }

    private fun title(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 30f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(Color.rgb(7, 18, 47))
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, 0, 0, dp(10))
    }

    private fun bodyText(text: String, color: Int): TextView = TextView(this).apply {
        this.text = text
        textSize = 15f
        setTextColor(color)
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = GradientDrawable().apply {
            setColor(Color.rgb(246, 253, 255))
            cornerRadius = dp(18).toFloat()
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(8) }
    }

    private fun spacer(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(heightDp))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
