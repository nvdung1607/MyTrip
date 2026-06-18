package com.example.mytrip.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.ExifInterface
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

                    val bitmap = decodeAndRotateBitmap(file.absolutePath) ?: continue
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

    suspend fun exportSelectedPhotos(
        context: Context,
        tripName: String,
        selectedPhotos: List<Pair<Note, String>>,
        includeWatermark: Boolean
    ): Int {
        return withContext(Dispatchers.IO) {
            var count = 0
            for ((note, photoPath) in selectedPhotos) {
                val file = File(photoPath)
                if (!file.exists()) continue

                val bitmap = decodeAndRotateBitmap(file.absolutePath) ?: continue
                val finalBitmap = if (includeWatermark) {
                    drawWatermark(context, bitmap, note)
                } else {
                    bitmap
                }

                saveBitmapToGallery(context, finalBitmap, tripName, note.id)
                if (finalBitmap != bitmap) finalBitmap.recycle()
                bitmap.recycle()
                count++
            }
            count
        }
    }

    private fun decodeAndRotateBitmap(path: String): Bitmap? {
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        return try {
            val exif = ExifInterface(path)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.preScale(1.0f, -1.0f)
                    matrix.postRotate(180f)
                }
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun drawWatermark(context: Context, original: Bitmap, note: Note): Bitmap {
        val config = original.config ?: Bitmap.Config.ARGB_8888
        val result = original.copy(config, true)
        val canvas = Canvas(result)

        // Text setup
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = original.width * 0.03f // 3% of image width
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }

        val padding = original.width * 0.03f
        val lineSpacing = paint.textSize * 1.2f

        val lines = mutableListOf<String>()
        val timeStr = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("vi", "VN")).format(java.util.Date(note.timestamp))
        lines.add("${note.tag.icon} $timeStr")
        
        val nameText = note.name.ifBlank { note.tag.label }
        lines.add(nameText)
        
        val starText = "⭐".repeat(note.rating)
        lines.add(starText)
        
        if (note.cost > 0) {
            lines.add("Chi phí: ${MoneyUtils.formatVnd(note.cost)}")
        }
        if (note.gpsLat != null && note.gpsLng != null) {
            lines.add("GPS: ${String.format(java.util.Locale.US, "%.5f, %.5f", note.gpsLat, note.gpsLng)}")
        }

        // Calculate box dimensions
        var maxWidth = 0f
        for (line in lines) {
            val width = paint.measureText(line)
            if (width > maxWidth) maxWidth = width
        }
        
        val boxPadding = padding * 0.5f
        val boxHeight = lines.size * lineSpacing + boxPadding
        val boxWidth = maxWidth + boxPadding * 2
        
        val startX = padding
        val startY = original.height - padding - boxHeight
        
        // Draw semi-transparent background box
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = 100 // ~40% opacity
            style = Paint.Style.FILL
        }
        val rect = RectF(startX, startY, startX + boxWidth, startY + boxHeight)
        canvas.drawRoundRect(rect, 16f, 16f, bgPaint)

        // Draw text from top to bottom inside the box
        var currentY = startY + boxPadding + paint.textSize * 0.9f
        for (line in lines) {
            canvas.drawText(line, startX + boxPadding, currentY, paint)
            currentY += lineSpacing
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
