package com.example.mytrip.widget.actions

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.example.mytrip.MainActivity

/** Key used to pass tripId through ActionParameters. */
val tripIdKey = ActionParameters.Key<Long>("trip_id")

/**
 * Opens the AddNote screen for the active trip when the "+" button is tapped.
 * Uses a deep-link Intent so Navigation handles routing correctly.
 */
class AddNoteAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val tripId = parameters[tripIdKey] ?: return
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("mytrip://add_note/$tripId")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}
