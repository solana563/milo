package com.ubayanda.milonative.push

import com.google.firebase.messaging.FirebaseMessagingService

/**
 * Notifications that arrive while the app is closed are shown by the system from the message itself.
 * Here we only need to hear about a new address for this phone.
 */
class MiloMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        if (Push.enabled(this)) Push.save(this, token)
    }
}
