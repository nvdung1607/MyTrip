package com.example.mytrip.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mytrip.MainActivity
import com.example.mytrip.R

const val CHANNEL_ID = "mytrip_schedule"
const val EXTRA_TITLE = "notif_title"
const val EXTRA_BODY = "notif_body"
const val EXTRA_TRIP_ID = "trip_id"

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "MyTrip"
        val body  = intent.getStringExtra(EXTRA_BODY)  ?: ""
        val tripId = intent.getLongExtra(EXTRA_TRIP_ID, -1L)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (idempotent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_desc)
            }
            nm.createNotificationChannel(ch)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (tripId != -1L) putExtra(EXTRA_TRIP_ID, tripId)
        }
        val pi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // On boot, reschedule pending alarms from DB
            // (simplified: app will reschedule when opened)
        }
    }
}

object NotificationScheduler {
    /**
     * Schedule a notification for [triggerAtMillis].
     * [requestCode] must be unique per alarm.
     */
    fun schedule(
        context: Context,
        requestCode: Int,
        triggerAtMillis: Long,
        title: String,
        body: String,
        tripId: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_TRIP_ID, tripId)
        }
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    fun cancel(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pi)
    }

    /**
     * Schedule a reminder X minutes before [activityTimeMillis].
     * minutesBefore: 15, 30, or 60
     */
    fun scheduleActivityReminder(
        context: Context,
        activityId: Long,
        activityName: String,
        activityTimeMillis: Long,
        tripId: Long,
        minutesBefore: Int = 15
    ) {
        val triggerAt = activityTimeMillis - minutesBefore * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return // already past

        schedule(
            context = context,
            requestCode = (activityId * 10 + minutesBefore).toInt(),
            triggerAtMillis = triggerAt,
            title = "⏰ Sắp đến giờ!",
            body = "${minutesBefore} phút nữa: $activityName",
            tripId = tripId
        )
    }
}
