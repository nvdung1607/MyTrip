package com.example.mytrip.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.mytrip.data.db.entities.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object PhotoExportUtils {

    suspend fun exportTripPhotos(context: Context, tripName: String, notes: List<Note>): Int {
        return withContext(Dispatchers.IO) {
            var count = 0
            for (note in notes) {
                val photos = if (note.photoPaths.isNotEmpty()) note.photoPaths else if (note.photoPath != null) listOf(note.photoPath!!) else emptyList()
                for (photoPath in photos) {
                    val file = File(photoPath)
                    if (!file.exists()) continue

                    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue
                    val watermarkedBitmap = drawWatermark(context, bitmap, note)

                    saveBitmapToGallery(context, watermarkedBitmap, tripName, note.id)
                    watermarkedBitmap.recycle()
                    if (bitmap != watermarkedBitmap) bitmap.recycle()
                    count++
                }
            }
            count
        }
    }

    private fun drawWatermark(context: Context, original: Bitmap, note: Note): Bitmap {
        val config = original.config ?: Bitmap.Config.ARGB_8888
        val result = original.copy(config, true)
        val canvas = Canvas(result)

        // Text setup
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = original.width * 0.04f // 4% of image width
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(4f, 2f, 2f, Color.BLACK)
        }

        val padding = original.width * 0.05f
        var currentY = original.height - padding

        val lines = mutableListOf<String>()
        if (note.gpsLat != null && note.gpsLng != null) {
            lines.add("GPS: ${String.format("%.5f", note.gpsLat)}, ${String.format("%.5f", note.gpsLng)}")
        }
        if (note.cost > 0) {
            lines.add("Chi phí: ${MoneyUtils.formatVnd(note.cost)}")
        }
        lines.add("${note.rating} ⭐")
        lines.add(note.name.ifBlank { note.tag.label })
        lines.add("ID: ${note.id}")

        // Draw from bottom to top
        for (line in lines) {
            canvas.drawText(line, padding, currentY, paint)
            currentY -= (paint.textSize * 1.5f)
        }

        return result
    }

    private fun saveBitmapToGallery(context: Context, bitmap: Bitmap, tripName: String, noteId: Long) {
        val filename = "MyTrip_${tripName}_Note${noteId}_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyTrip")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            resolver.openOutputStream(it)?.use { os ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, os)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
        }
    }
}
