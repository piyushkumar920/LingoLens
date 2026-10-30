package com.example

import com.example.modules.travel.model.DestinationCountry
import com.example.modules.travel.model.TravelCategory
import com.example.modules.travel.repository.DefaultTravelRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TravelModeTest {

    private lateinit var travelRepository: DefaultTravelRepository

    @Before
    fun setup() {
        travelRepository = DefaultTravelRepository()
    }

    @Test
    fun testDestinationCountrySelection() {
        val destinations = travelRepository.getSupportedDestinations()
        assertTrue(destinations.isNotEmpty())
        assertTrue(destinations.any { it.primaryLanguageCode == "es" })
        assertTrue(destinations.any { it.primaryLanguageCode == "ja" })
        assertTrue(destinations.any { it.primaryLanguageCode == "fr" })

        val spain = destinations.find { it.primaryLanguageCode == "es" }
        assertNotNull(spain)
        assertEquals("Spain", spain!!.countryName)
        assertEquals("EUR", spain.currencyCode)
    }

    @Test
    fun testCommonPhrasesList() = runBlocking {
        val commonPhrases = travelRepository.getCommonPhrases("es").first()

        val expectedPhrases = listOf(
            "Hello",
            "Thank you",
            "Please",
            "Excuse me",
            "Where is the bathroom?",
            "Where is the station?",
            "How much does this cost?",
            "I don't understand.",
            "Can you help me?",
            "I need a doctor.",
            "I am lost."
        )

        for (expected in expectedPhrases) {
            val found = commonPhrases.find { it.originalEnglish.equals(expected, ignoreCase = true) }
            assertNotNull("Missing required common phrase: $expected", found)
            assertTrue("Translation must not be empty for $expected", found!!.translatedText.isNotBlank())
        }
    }

    @Test
    fun testEmergencyPhrasesList() = runBlocking {
        val emergencyPhrases = travelRepository.getEmergencyPhrases("es").first()

        val expectedEmergency = listOf(
            "I need medical help.",
            "Call the police.",
            "I need an ambulance.",
            "I am lost.",
            "I have an allergy.",
            "I need help."
        )

        for (expected in expectedEmergency) {
            val found = emergencyPhrases.find { it.originalEnglish.equals(expected, ignoreCase = true) }
            assertNotNull("Missing required emergency phrase: $expected", found)
            assertEquals(TravelCategory.EMERGENCY, found!!.category)
            assertTrue("Translation must not be empty for $expected", found.translatedText.isNotBlank())
        }
    }

    @Test
    fun testDynamicLanguageAdaptation() = runBlocking {
        // Test Japanese translations
        val jaPhrases = travelRepository.getCommonPhrases("ja").first()
        val jaHello = jaPhrases.find { it.originalEnglish == "Hello" }
        assertNotNull(jaHello)
        assertEquals("こんにちは", jaHello!!.translatedText)

        // Test French translations
        val frPhrases = travelRepository.getCommonPhrases("fr").first()
        val frHello = frPhrases.find { it.originalEnglish == "Hello" }
        assertNotNull(frHello)
        assertEquals("Bonjour", frHello!!.translatedText)
    }
}
