/**
 * BetNotificationWorker is a CoroutineWorker that checks for expired bets and sends notifications to users.
 *
 * Functionality Overview:
 * - doWork():
 *     - Retrieves the user document from Firestore using the provided user ID.
 *     - Iterates over the list of rooms linked to the user.
 *     - For each room, attempts to determine the expiration time using:
 *         1) A numeric "betEndTime" field.
 *         2) A string-based "expiration" field (parsed to milliseconds).
 *     - Compares the expiration time with the current system time.
 *         - If the bet has ended (i.e., current time is past expiration), sends a notification with the bet description.
 *     - In debug mode, reschedules the work to run again in 1 minute.
 *
 * - parseExpirationToMillis(expirationString: String?): Long?
 *     - Helper function that converts an expiration date string (formatted as "dd/MM/yyyy") into milliseconds.
 *     - Returns null if the input string is null, blank, or cannot be parsed.
 *
 * - sendNotification(roomId: String, message: String)
 *     - Builds and dispatches a local notification to alert the user that a bet has ended.
 *     - Creates a notification channel for Android O and above if necessary.
 *     - Uses a unique notification ID based on the hash of the roomId.
 */

package com.example.bettertogether

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class BetNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val db = FirebaseFirestore.getInstance()
    private val userId: String? = workerParams.inputData.getString("uid")

    override suspend fun doWork(): Result {
        if (userId.isNullOrEmpty()) {
            Log.d("BetNotificationWorker", "No userId provided in inputData.")
            return Result.failure()
        }

        try {
            // Retrieve user document
            val userDoc = db.collection("users").document(userId).get().await()
            if (!userDoc.exists()) {
                Log.d("BetNotificationWorker", "User document does not exist.")
                return Result.failure()
            }

            // Get the list of rooms from the user document
            val roomsList = userDoc.get("rooms") as? List<Map<String, Any>> ?: emptyList()
            Log.d("BetNotificationWorker", "Found ${roomsList.size} rooms for user $userId")

            for (roomData in roomsList) {
                val roomId = roomData["roomId"] as? String ?: continue

                // Retrieve the actual room document from the "rooms" collection
                val roomDoc = db.collection("rooms").document(roomId).get().await()
                if (!roomDoc.exists()) {
                    Log.d("BetNotificationWorker", "Room document for $roomId does not exist.")
                    continue
                }

                // 1) Try numeric "betEndTime"
                val betEndTime = roomDoc.getLong("betEndTime")

                // 2) Or try string-based "expiration" (e.g. "15/2/2025")
                val expirationString = roomDoc.getString("expiration")
                val expirationMillis = betEndTime ?: parseExpirationToMillis(expirationString)

                val currentTime = System.currentTimeMillis()
                Log.d("BetNotificationWorker", """
                    Room $roomId:
                    betEndTime (numeric) = $betEndTime
                    expirationString     = $expirationString
                    parsed expiration    = $expirationMillis
                    currentTime          = $currentTime
                """.trimIndent())

                if (expirationMillis != null && currentTime >= expirationMillis) {
                    // If the bet (or expiration date) has passed, send a notification
                    val betDescription = roomDoc.getString("betDescription") ?: "A bet has ended."
                    Log.d("BetNotificationWorker", "Sending notification for room $roomId")
                    sendNotification(roomId, betDescription)
                } else {
                    Log.d("BetNotificationWorker", "No notification for room $roomId (not expired yet).")
                }
            }

            // For debugging, if DEBUG_MODE is true, reschedule a one-time work to run again in 1 minute.
            if (BaseActivity.DEBUG_MODE) {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val newWork = OneTimeWorkRequestBuilder<BetNotificationWorker>()
                    .setInitialDelay(1, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()
                WorkManager.getInstance(applicationContext)
                    .enqueueUniqueWork("BetNotificationWork", ExistingWorkPolicy.REPLACE, newWork)
                Log.d("BetNotificationWorker", "Rescheduled debug work to run in 1 minute.")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e("BetNotificationWorker", "Error in doWork", e)
            return Result.failure()
        }
    }

    /**
     * Helper function to parse an expiration date string (e.g. "15/2/2025") into milliseconds.
     * Adjust the date format (dd/MM/yyyy) if your date strings use a different format.
     */
    private fun parseExpirationToMillis(expirationString: String?): Long? {
        if (expirationString.isNullOrBlank()) return null
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = format.parse(expirationString)
            date?.time
        } catch (e: ParseException) {
            Log.e("BetNotificationWorker", "Failed to parse expiration date: $expirationString", e)
            null
        }
    }

    private fun sendNotification(roomId: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "bet_notifications_channel"

        // Create notification channel for Android O and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Bet Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Make sure this icon exists in your drawable resources.
            .setContentTitle("Bet Ended")
            .setContentText(message)
            .setAutoCancel(true)
            .build()

        // Use roomId.hashCode() as a unique notification ID.
        notificationManager.notify(roomId.hashCode(), notification)
    }
}
