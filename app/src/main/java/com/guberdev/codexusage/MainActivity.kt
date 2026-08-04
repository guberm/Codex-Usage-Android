package com.guberdev.codexusage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.StatusBarManager
import android.appwidget.AppWidgetManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
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
    private var promotionButton: Button? = null
    private var pendingDeviceCode: DeviceCode? = null
    @Volatile private var destroyed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        requestNotificationPermission()
        render(UsageStore(this).load())
    }

    override fun onStart() {
        super.onStart()
        if (SecureTokenStore(this).load() == null) return
        RefreshCoordinator.refresh(this) { result ->
            if (!destroyed && result is RefreshResult.Success) render(result.snapshot)
        }
    }

    override fun onResume() {
        super.onResume()
        if (SecureTokenStore(this).load() != null) {
            UsageBackupJobService.schedule(this)
            NotificationHelper.showMonitor(this)
        }
        updatePromotionButton()
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
            setBackgroundColor(color(R.color.screen_background))
        }

        root.addView(
            ImageView(this).apply {
                setImageResource(R.mipmap.ic_launcher)
                contentDescription = "OpenAI Codex usage gauge"
                adjustViewBounds = true
            },
            LinearLayout.LayoutParams(dp(128), dp(128)).apply { gravity = Gravity.CENTER_HORIZONTAL },
        )
        root.addView(title("Codex Usage"))
        root.addView(
            bodyText("Unofficial companion for OpenAI Codex", color(R.color.text_secondary)).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            },
        )
        root.addView(spacer(18))

        val usageCard = card()
        percentText = TextView(this).apply {
            text = "—"
            textSize = 48f
            setTextColor(color(R.color.text_primary))
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        usageCard.addView(percentText)
        usageCard.addView(bodyText("remaining", color(R.color.text_secondary)).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
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
        resetText = bodyText("Reset: —", color(R.color.text_primary))
        updatedText = bodyText("Not updated yet", color(R.color.text_secondary))
        statusText = bodyText("Sign in with ChatGPT", color(R.color.theme_accent))
        usageCard.addView(resetText)
        usageCard.addView(updatedText)
        usageCard.addView(statusText)
        additionalContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        usageCard.addView(additionalContainer)
        root.addView(usageCard)
        root.addView(spacer(14))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            val settingsCard = card()
            settingsCard.addView(sectionTitle("Notification settings"))
            promotionButton = Button(this).apply {
                setOnClickListener { openPromotionSettings() }
            }
            settingsCard.addView(promotionButton)
            settingsCard.addView(
                bodyText(
                    "Allow promoted notifications to keep the current percentage in the status bar.",
                    color(R.color.text_secondary),
                ),
            )
            root.addView(settingsCard)
            root.addView(spacer(14))
        }

        codePanel = card().apply { visibility = View.GONE }
        codePanel.addView(sectionTitle("Sign in with ChatGPT"))
        codePanel.addView(bodyText("One-time code:", color(R.color.text_secondary)))
        codeText = TextView(this).apply {
            textSize = 30f
            setTextColor(color(R.color.text_primary))
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(0, dp(12), 0, dp(12))
        }
        codePanel.addView(codeText)
        codePanel.addView(
            Button(this).apply {
                text = "Copy code and open ChatGPT"
                setOnClickListener {
                    pendingDeviceCode?.let { openDeviceLogin(it) }
                }
            },
        )
        root.addView(codePanel)
        root.addView(spacer(14))

        loginButton = actionButton("Sign in with ChatGPT") { startDeviceLogin() }
        refreshButton = actionButton("Refresh now") { refreshNow() }
        root.addView(loginButton)
        root.addView(refreshButton)
        root.addView(actionButton("Add Quick Settings tile") { requestTile() })
        root.addView(actionButton("Add Home screen widget") { requestWidget() })
        logoutButton = actionButton("Sign out") { confirmLogout() }
        root.addView(logoutButton)

        return ScrollView(this).apply { addView(root) }
    }

    private fun startDeviceLogin() {
        setBusy("Requesting sign-in code…")
        executor.execute {
            try {
                val client = CodexAuthClient()
                val code = client.requestDeviceCode()
                pendingDeviceCode = code
                runOnUiThread {
                    codeText.text = code.userCode
                    codePanel.visibility = View.VISIBLE
                    statusText.text = "Complete sign-in in your browser"
                    openDeviceLogin(code)
                }

                val deadline = System.currentTimeMillis() + 15 * 60 * 1000L
                val pending = pollUntilAuthorized(
                    deadlineMillis = deadline,
                    intervalMillis = code.intervalSeconds * 1000L,
                    onUnknownHost = {
                        if (!destroyed) runOnUiThread {
                            statusText.text = "Waiting for network… Authorization will resume automatically."
                        }
                    },
                ) {
                    client.pollDeviceCode(code)
                }
                if (pending == null) error("The code expired. Try again.")
                val tokens = client.exchangeCode(pending)
                SecureTokenStore(this).save(tokens)
                if (!destroyed) {
                    runOnUiThread {
                        codePanel.visibility = View.GONE
                        statusText.text = "Signed in. Refreshing usage…"
                        updatePromotionButton()
                        UsageBackupJobService.schedule(this)
                        refreshNow()
                    }
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (error: Throwable) {
                if (!destroyed) runOnUiThread {
                    setBusy(error.message?.take(160) ?: "Could not sign in", busy = false)
                }
            }
        }
    }

    private fun openDeviceLogin(code: DeviceCode) {
        getSystemService(ClipboardManager::class.java)
            .setPrimaryClip(ClipData.newPlainText("Codex device code", code.userCode))
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(code.verificationUrl)))
        Toast.makeText(this, "Code copied", Toast.LENGTH_SHORT).show()
    }

    private fun refreshNow() {
        setBusy("Refreshing…")
        RefreshCoordinator.refresh(this) { result ->
            when (result) {
                is RefreshResult.Success -> render(result.snapshot)
                is RefreshResult.Error -> statusText.text = result.message
            }
            setButtonsEnabled(true)
        }
    }

    private fun render(snapshot: UsageSnapshot?) {
        val tokens = SecureTokenStore(this).load()
        val signedIn = tokens != null
        loginButton.visibility = if (signedIn) View.GONE else View.VISIBLE
        logoutButton.visibility = if (signedIn) View.VISIBLE else View.GONE
        refreshButton.isEnabled = signedIn
        statusText.text = tokens?.signedInText() ?: "Sign in with ChatGPT"
        if (snapshot == null) {
            percentText.text = "—"
            progress.progress = 0
            resetText.text = "Reset: —"
            updatedText.text = "Not updated yet"
            additionalContainer.removeAllViews()
            return
        }
        percentText.text = "${snapshot.primary.remainingPercent}%"
        progress.progress = snapshot.primary.remainingPercent
        resetText.text = "Reset: ${UsageText.resetDate(snapshot.primary.resetAtEpochSeconds)}"
        updatedText.text = "Updated ${UsageText.resetDate(snapshot.fetchedAtEpochMillis / 1000L)}"
        additionalContainer.removeAllViews()
        snapshot.additionalLimits.forEach { limit ->
            additionalContainer.addView(
                bodyText(
                    "${UsageText.featureName(limit.feature)}: ${limit.window.remainingPercent}% • " +
                        "resets ${UsageText.resetDate(limit.window.resetAtEpochSeconds)}",
                    color(R.color.text_primary),
                ),
            )
        }
        snapshot.creditBalance?.let {
            additionalContainer.addView(bodyText("Credits: $it", color(R.color.text_primary)))
        }
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
                Toast.makeText(this, "Tile request result: $result", Toast.LENGTH_SHORT).show()
            }
        } else {
            val quickSettingsIntent = Intent("android.settings.QUICK_SETTINGS_SETTINGS")
            val fallback = Intent(Settings.ACTION_SETTINGS)
            runCatching { startActivity(quickSettingsIntent) }
                .onFailure { startActivity(fallback) }
            Toast.makeText(this, "Drag the Codex Usage tile into active tiles", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestWidget() {
        val manager = getSystemService(AppWidgetManager::class.java)
        val provider = ComponentName(this, CodexUsageWidgetProvider::class.java)
        if (manager.isRequestPinAppWidgetSupported && manager.requestPinAppWidget(provider, null, null)) {
            Toast.makeText(this, "Choose where to place the widget", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Add Codex Usage from your launcher's widget picker", Toast.LENGTH_LONG).show()
        }
    }

    private fun confirmLogout() {
        AlertDialog.Builder(this)
            .setTitle("Sign out of Codex Usage?")
            .setMessage("OAuth tokens will be removed only from this phone.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Sign out") { _, _ ->
                UsageBackupJobService.cancel(this)
                NotificationHelper.cancelMonitor(this)
                SecureTokenStore(this).clear()
                UsageStore(this).clear()
                CodexUsageWidgetProvider.updateAll(this)
                render(null)
                statusText.text = "Signed out"
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 300 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            NotificationHelper.showMonitor(this)
        }
    }

    private fun openPromotionSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        NotificationHelper.showMonitor(this)
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(this, "Status bar percentage settings are unavailable", Toast.LENGTH_LONG).show()
            }
    }

    private fun updatePromotionButton() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        promotionButton?.visibility = if (SecureTokenStore(this).load() == null) View.GONE else View.VISIBLE
        val enabled = getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
        promotionButton?.text = if (enabled) {
            "Status bar percentage: enabled"
        } else {
            "Enable % in status bar"
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
        setTextColor(color(R.color.text_primary))
        setTypeface(typeface, Typeface.BOLD)
        gravity = Gravity.CENTER_HORIZONTAL
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 20f
        setTextColor(color(R.color.text_primary))
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
            setColor(color(R.color.surface))
            cornerRadius = dp(8).toFloat()
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

    private fun color(resourceId: Int): Int = getColor(resourceId)
}
