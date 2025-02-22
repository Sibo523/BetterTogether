package com.example.bettertogether

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyRewardReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)
        notificationHelper.sendNotification(
            title = "Daily Reward",
            message = "Your daily reward is ready! Open the app to claim it."
        )
    }
}
