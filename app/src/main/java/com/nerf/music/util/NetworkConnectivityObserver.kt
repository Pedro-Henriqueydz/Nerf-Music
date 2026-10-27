package com.nerf.music.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Monitor em tempo real do estado de conectividade de rede do dispositivo.
 * Implementa [ConnectivityObserver] emitindo [ConnectivityObserver.Status].
 */
class NetworkConnectivityObserver(
    private val context: Context
) : ConnectivityObserver {

    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun observe(): Flow<ConnectivityObserver.Status> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                launch { send(ConnectivityObserver.Status.Available) }
            }

            override fun onLosing(network: Network, maxMsToLive: Int) {
                super.onLosing(network, maxMsToLive)
                launch { send(ConnectivityObserver.Status.Losing) }
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                launch { send(ConnectivityObserver.Status.Lost) }
            }

            override fun onUnavailable() {
                super.onUnavailable()
                launch { send(ConnectivityObserver.Status.Unavailable) }
            }
        }

        // Emite o estado inicial imediatamente
        val initialStatus = if (NetworkUtils.isNetworkAvailable(context)) {
            ConnectivityObserver.Status.Available
        } else {
            ConnectivityObserver.Status.Unavailable
        }
        trySend(initialStatus)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            try {
                connectivityManager.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged()
}
