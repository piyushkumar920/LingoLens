package com.example.modules.image_translation.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.core.image.ImageEnhanceMode
import com.example.core.result.AppResult
import com.example.modules.image_translation.model.ImageTranslationUiState
import com.example.modules.language.model.Language
import com.example.modules.ocr.model.OcrScript
import com.example.modules.translation.model.OverlayMode
import com.example.modules.translation.model.TranslatedBlock
import kotlinx.coroutines.flow.StateFlow

interface ImageTranslationRepository {
    val uiState: StateFlow<ImageTranslationUiState>

    suspend fun loadImage(context: Context, uri: Uri): AppResult<Unit>
    suspend fun setBitmap(bitmap: Bitmap, autoProcess: Boolean = true): AppResult<Unit>
    suspend fun reprocess(
        context: Context? = null,
        script: OcrScript? = null,
        enhanceMode: ImageEnhanceMode? = null,
        rotateDegrees: Float = 0f
    ): AppResult<Unit>
    suspend fun changeLanguages(sourceLanguage: Language, targetLanguage: Language): AppResult<Unit>
    fun selectBlock(block: TranslatedBlock?)
    fun setOverlayMode(mode: OverlayMode)
    fun updateTransform(scale: Float, panX: Float, panY: Float)
    fun resetTransform()
    suspend fun rotateImage(degrees: Float): AppResult<Unit>
    suspend fun setEnhancementMode(mode: ImageEnhanceMode): AppResult<Unit>
    fun clear()
}
