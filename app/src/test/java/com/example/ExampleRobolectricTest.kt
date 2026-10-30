package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.modules.language.repository.DefaultLanguageRepository
import com.example.modules.travel.model.TravelCategory
import com.example.modules.travel.repository.DefaultTravelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("LingoLens", appName)
    }

    @Test
    fun `verify language repository contains core languages`() = runBlocking {
        val langRepo = DefaultLanguageRepository(FakeLanguageIdentifier(), FakeTranslationService())
        val languages = langRepo.getSupportedLanguages().first()
        assertTrue(languages.isNotEmpty())
        assertTrue(languages.any { it.code == "en" })
        assertTrue(languages.any { it.code == "es" })
        assertTrue(languages.any { it.code == "ja" })
    }

    @Test
    fun `verify travel repository returns categorized phrases`() = runBlocking {
        val travelRepo = DefaultTravelRepository()
        val emergencyPhrases = travelRepo.getPhrasesForCategory(TravelCategory.EMERGENCY, "es").first()
        assertTrue(emergencyPhrases.isNotEmpty())
        assertTrue(emergencyPhrases.all { it.category == TravelCategory.EMERGENCY })
    }
}
