package com.example.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * High-level connection states:
 * - ONLINE: Connected to Wi-Fi / Cellular with full internet capability
 * - LIMITED_CONNECTION: Connected to a network with captive portal, cellular low-metered, or without validated internet
 * - OFFLINE: No active network connection
 */
enum class NetworkConnectionState {
    ONLINE,
    LIMITED_CONNECTION,
    OFFLINE
}

interface NetworkConnectivityMonitor {
    val connectionState: StateFlow<NetworkConnectionState>
    val isOnline: Boolean
    val isOffline: Boolean
    val isLimited: Boolean
}

class DefaultNetworkConnectivityMonitor(
    private val context: Context
) : NetworkConnectivityMonitor {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _connectionState = MutableStateFlow(determineCurrentState())
    override val connectionState: StateFlow<NetworkConnectionState> = _connectionState.asStateFlow()

    override val isOnline: Boolean get() = _connectionState.value == NetworkConnectionState.ONLINE
    override val isOffline: Boolean get() = _connectionState.value == NetworkConnectionState.OFFLINE
    override val isLimited: Boolean get() = _connectionState.value == NetworkConnectionState.LIMITED_CONNECTION

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateState()
        }

        override fun onLost(network: Network) {
            updateState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateState()
        }
    }

    init {
        registerCallback()
    }

    private fun registerCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Safe fallback if unable to register callback
        }
    }

    private fun updateState() {
        scope.launch {
            _connectionState.value = determineCurrentState()
        }
    }

    private fun determineCurrentState(): NetworkConnectionState {
        val cm = connectivityManager ?: return NetworkConnectionState.OFFLINE
        val activeNetwork = cm.activeNetwork ?: return NetworkConnectionState.OFFLINE
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkConnectionState.OFFLINE

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        val isCaptive = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)

        return when {
            hasInternet && isValidated && !isCaptive -> NetworkConnectionState.ONLINE
            hasInternet -> NetworkConnectionState.LIMITED_CONNECTION
            else -> NetworkConnectionState.OFFLINE
        }
    }
}
