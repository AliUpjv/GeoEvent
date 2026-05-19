package com.example.geoevent.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class ConnectivityReceiver(
    private val onConnectivityChanged: (Boolean) -> Unit
) : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val isConnected = cm.activeNetworkInfo?.isConnected == true
        onConnectivityChanged(isConnected)
    }
}
