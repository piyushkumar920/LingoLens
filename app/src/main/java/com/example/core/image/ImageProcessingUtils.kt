package com.example.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

/**
 * Image enhancement modes for low-quality, low-light, or degraded images/receipts.
 */
enum class ImageEnhanceMode(val displayName: String) {
    ORIGINAL("Original"),
    HIGH_CONTRAST("High Contrast"),
    DOCUMENT_GRAYSCALE("B&W Document"),
    BRIGHTEN("Brighten (Low Light)")
}

object ImageProcessingUtils {

    private const val TAG = "ImageProcessingUtils"
    const val DEFAULT_MAX_DIMENSION = 2048

    /**
     * Efficiently loads and downsamples a bitmap from a Content URI to prevent OutOfMemory errors.
     * Automatically applies EXIF orientation correction.
     */
    suspend fun loadBitmapSafely(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // 1. Decode bounds only to determine dimensions without allocating full memory
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val originalWidth = options.outWidth
            val originalHeight = options.outHeight

            if (originalWidth <= 0 || originalHeight <= 0) {
                Log.e(TAG, "Failed to decode image bounds for URI: $uri")
                return@withContext null
            }

            // 2. Calculate optimal sample size (power of 2)
            options.inSampleSize = calculateInSampleSize(originalWidth, originalHeight, maxDimension, maxDimension)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.ARGB_8888

            // 3. Decode actual sampled bitmap
            val sampledBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            // 4. Extract and apply EXIF orientation
            val exifRotation = getExifOrientationDegrees(context, uri)
            if (exifRotation != 0) {
                rotateBitmap(sampledBitmap, exifRotation.toFloat())
            } else {
                sampledBitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error safely decoding bitmap from URI: $uri", e)
            null
        }
    }

    /**
     * Calculates the inSampleSize for BitmapFactory based on requested max dimensions.
     */
    fun calculateInSampleSize(
        rawWidth: Int,
        rawHeight: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            val halfHeight = rawHeight / 2
            val halfWidth = rawWidth / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return max(1, inSampleSize)
    }

    /**
     * Reads EXIF orientation from the image URI and converts it to degrees (0, 90, 180, 270).
     */
    fun getExifOrientationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Could not read EXIF orientation: ${e.message}")
            0
        }
    }

    /**
     * Rotates a bitmap by the given degrees.
     */
    fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated != source && !source.isRecycled) {
            // Note: only recycle if a new instance was generated
        }
        return rotated
    }

    /**
     * Applies image enhancements for OCR enhancement (e.g. low-light contrast boost, B&W conversion).
     */
    suspend fun applyEnhancement(
        source: Bitmap,
        mode: ImageEnhanceMode
    ): Bitmap = withContext(Dispatchers.Default) {
        if (mode == ImageEnhanceMode.ORIGINAL) return@withContext source

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        when (mode) {
            ImageEnhanceMode.ORIGINAL -> return@withContext source

            ImageEnhanceMode.HIGH_CONTRAST -> {
                // Boost contrast by 1.5x and increase brightness slightly
                val contrast = 1.6f
                val brightness = 10f
                val colorMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, brightness,
                        0f, contrast, 0f, 0f, brightness,
                        0f, 0f, contrast, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            ImageEnhanceMode.DOCUMENT_GRAYSCALE -> {
                // High-contrast Grayscale / B&W document filter
                val grayMatrix = ColorMatrix().apply {
                    setSaturation(0f)
                }
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        2.0f, 0f, 0f, 0f, -40f,
                        0f, 2.0f, 0f, 0f, -40f,
                        0f, 0f, 2.0f, 0f, -40f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                grayMatrix.postConcat(contrastMatrix)
                paint.colorFilter = ColorMatrixColorFilter(grayMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }

            ImageEnhanceMode.BRIGHTEN -> {
                // Gamma / brightness boost for low light captures
                val brightness = 45f
                val colorMatrix = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, brightness,
                        0f, 1.2f, 0f, 0f, brightness,
                        0f, 0f, 1.2f, 0f, brightness,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(source, 0f, 0f, paint)
            }
        }

        output
    }
}
