package com.divoom.navigationarrow.navigation

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class GoogleMapsNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeNotifications
            ?.filter { it.packageName == GOOGLE_MAPS_PACKAGE }
            ?.forEach(::processNotification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn?.packageName != GOOGLE_MAPS_PACKAGE) return
        processNotification(sbn)
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val values = mutableListOf<CharSequence?>(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        )
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.forEach { values.add(it) }
        val parts = values
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotEmpty() }
        GoogleMapsInstructionParser.parse(parts)
            ?.let(NavigationInstructionBus::publish)
    }

    companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}
