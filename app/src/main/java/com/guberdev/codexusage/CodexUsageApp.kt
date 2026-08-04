package com.guberdev.codexusage

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network

class NetworkRecoveryTracker(initiallyConnected: Boolean) {
    private var connected = initiallyConnected

    @Synchronized
    fun onAvailable(): Boolean = (!connected).also { connected = true }

    @Synchronized
    fun onLost() {
        connected = false
    }
}

class CodexUsageApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val connectivity = getSystemService(ConnectivityManager::class.java)
        val tracker = NetworkRecoveryTracker(connectivity.activeNetwork != null)
        connectivity.registerDefaultNetworkCallback(
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    if (tracker.onAvailable() && SecureTokenStore(this@CodexUsageApp).load() != null) {
                        RefreshCoordinator.refresh(this@CodexUsageApp)
                    }
                }

                override fun onLost(network: Network) {
                    tracker.onLost()
                }
            },
        )
    }
}
