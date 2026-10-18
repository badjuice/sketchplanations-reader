package com.sketchplanations.reader.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** The three states that drive real behaviour: images, playback and downloads all key off this. */
enum class Conn { Wifi, Mobile, Off }

fun connectivity(context: Context): Flow<Conn> = callbackFlow {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    fun current(): Conn {
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return Conn.Off
        if (!caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return Conn.Off
        // Metered is what the user cares about: a phone hotspot is "mobile data" even over Wi-Fi.
        val metered = !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) && !metered) Conn.Wifi
        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) Conn.Wifi
        else Conn.Mobile
    }
    val cb = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(current()) }
        override fun onLost(network: Network) { trySend(current()) }
        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
    }
    trySend(current())
    cm.registerDefaultNetworkCallback(cb)
    awaitClose { cm.unregisterNetworkCallback(cb) }
}.distinctUntilChanged()
