package com.guberdev.codexusage

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.service.quicksettings.TileService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

sealed class RefreshResult {
    data class Success(val snapshot: UsageSnapshot, val delta: Int?) : RefreshResult()
    data class Error(val message: String) : RefreshResult()
}

object RefreshCoordinator {
    private val executor = Executors.newSingleThreadExecutor()
    private val refreshing = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun refresh(context: Context, callback: ((RefreshResult) -> Unit)? = null) {
        val appContext = context.applicationContext
        if (!refreshing.compareAndSet(false, true)) {
            callback?.let { mainHandler.post { it(RefreshResult.Error("Обновление уже выполняется")) } }
            return
        }
        executor.execute {
            val result = runCatching { refreshBlocking(appContext) }
                .getOrElse { RefreshResult.Error(friendlyMessage(it)) }
            refreshing.set(false)
            requestTileUpdate(appContext)
            callback?.let { mainHandler.post { it(result) } }
        }
    }

    private fun refreshBlocking(context: Context): RefreshResult {
        val tokenStore = SecureTokenStore(context)
        val originalTokens = tokenStore.load()
            ?: return RefreshResult.Error("Войдите через ChatGPT")
        val authClient = CodexAuthClient()
        var tokens = authClient.ensureFresh(originalTokens)
        if (tokens != originalTokens) tokenStore.save(tokens)

        val snapshot = try {
            CodexUsageApi().fetch(tokens)
        } catch (error: HttpStatusException) {
            if (error.statusCode != 401) throw error
            tokens = authClient.refresh(tokens)
            tokenStore.save(tokens)
            CodexUsageApi().fetch(tokens)
        }

        val settings = MonitorSettingsStore(context).load()
        val delta = UsageStore(context).save(snapshot, settings.notifyEveryPercent)
        NotificationHelper.updateMonitor(context, snapshot)
        if (delta != null) NotificationHelper.notifyChange(context, snapshot, delta)
        UsageBackupJobService.schedule(context)
        return RefreshResult.Success(snapshot, delta)
    }

    private fun requestTileUpdate(context: Context) {
        TileService.requestListeningState(
            context,
            ComponentName(context, CodexUsageTileService::class.java),
        )
    }

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is HttpStatusException -> when (error.statusCode) {
            401, 403 -> "Сессия ChatGPT истекла. Войдите снова."
            429 -> "Слишком много запросов. Попробуйте позже."
            else -> "Codex Usage недоступен (HTTP ${error.statusCode})"
        }
        is java.net.SocketTimeoutException -> "Codex Usage не ответил вовремя"
        is java.io.IOException -> "Нет соединения с Codex Usage"
        else -> error.message?.take(160) ?: "Не удалось обновить Codex Usage"
    }
}
