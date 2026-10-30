package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.result.AppResult
import com.example.data.local.AppDatabase
import com.example.data.local.dao.HistoryDao
import com.example.data.repository.RoomHistoryRepository
import com.example.modules.history.model.TranslationHistoryItem
import com.example.modules.history.model.TranslationType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TranslationHistoryRoomTest {

    private lateinit var db: AppDatabase
    private lateinit var historyDao: HistoryDao
    private lateinit var repository: RoomHistoryRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        historyDao = db.historyDao()
        repository = RoomHistoryRepository(historyDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testStoreAndRetrieveAllTranslationTypes() = runBlocking {
        val types = listOf(
            TranslationType.CAMERA,
            TranslationType.IMAGE,
            TranslationType.TEXT,
            TranslationType.VOICE,
            TranslationType.CONVERSATION,
            TranslationType.TRAVEL
        )

        types.forEachIndexed { index, type ->
            repository.insertHistoryItem(
                TranslationHistoryItem(
                    sourceText = "Source message $index",
                    translatedText = "Translated message $index",
                    sourceLanguage = "en",
                    targetLanguage = "es",
                    timestamp = System.currentTimeMillis() + index,
                    translationType = type,
                    isFavorite = index % 2 == 0,
                    explanation = if (index == 0) "{\"meaning\":\"sample\"}" else null
                )
            )
        }

        val allItems = repository.getAllHistory().first()
        assertEquals(6, allItems.size)

        // Verify ordering is descending by timestamp
        assertTrue(allItems[0].timestamp >= allItems[1].timestamp)

        // Verify type mapping
        val retrievedTypes = allItems.map { it.translationType }.toSet()
        assertEquals(types.toSet(), retrievedTypes)
    }

    @Test
    fun testSearchAndFiltering() = runBlocking {
        repository.insertHistoryItem(
            TranslationHistoryItem(
                sourceText = "Where is the subway station?",
                translatedText = "¿Dónde está la estación de metro?",
                sourceLanguage = "en",
                targetLanguage = "es",
                translationType = TranslationType.TRAVEL,
                isFavorite = true
            )
        )
        repository.insertHistoryItem(
            TranslationHistoryItem(
                sourceText = "Good morning friend",
                translatedText = "Buenos días amigo",
                sourceLanguage = "en",
                targetLanguage = "es",
                translationType = TranslationType.TEXT,
                isFavorite = false
            )
        )
        repository.insertHistoryItem(
            TranslationHistoryItem(
                sourceText = "Receipt total 50 dollars",
                translatedText = "Total del recibo 50 dólares",
                sourceLanguage = "en",
                targetLanguage = "es",
                translationType = TranslationType.CAMERA,
                isFavorite = true
            )
        )

        // Search by query "subway"
        val searchResults = repository.getFilteredHistory(query = "subway").first()
        assertEquals(1, searchResults.size)
        assertEquals("Where is the subway station?", searchResults[0].sourceText)

        // Filter by Travel type
        val travelResults = repository.getFilteredHistory(type = TranslationType.TRAVEL).first()
        assertEquals(1, travelResults.size)
        assertEquals(TranslationType.TRAVEL, travelResults[0].translationType)

        // Filter by Favorites
        val favoriteResults = repository.getFilteredHistory(favoritesOnly = true).first()
        assertEquals(2, favoriteResults.size)
        assertTrue(favoriteResults.all { it.isFavorite })
    }

    @Test
    fun testFavoriteToggleAndExplanationUpdate() = runBlocking {
        val result = repository.insertHistoryItem(
            TranslationHistoryItem(
                sourceText = "Hello",
                translatedText = "Hola",
                sourceLanguage = "en",
                targetLanguage = "es",
                translationType = TranslationType.VOICE,
                isFavorite = false
            )
        )
        assertTrue(result is AppResult.Success)
        val id = (result as AppResult.Success).data

        val beforeResult = repository.getHistoryItemById(id)
        assertTrue(beforeResult is AppResult.Success)
        val before = (beforeResult as AppResult.Success).data
        assertNotNull(before)
        assertFalse(before!!.isFavorite)

        // Toggle favorite
        repository.toggleFavorite(id, true)
        val afterFavResult = repository.getHistoryItemById(id)
        assertTrue(afterFavResult is AppResult.Success)
        val afterFav = (afterFavResult as AppResult.Success).data
        assertTrue(afterFav!!.isFavorite)

        // Update explanation
        val sampleExplanation = "{\"meaning\":\"Greeting in Spanish\"}"
        repository.updateExplanation(id, sampleExplanation)
        val afterExplResult = repository.getHistoryItemById(id)
        assertTrue(afterExplResult is AppResult.Success)
        val afterExpl = (afterExplResult as AppResult.Success).data
        assertEquals(sampleExplanation, afterExpl!!.explanation)
    }

    @Test
    fun testDeleteAndClearAll() = runBlocking {
        val res1 = repository.insertHistoryItem(
            TranslationHistoryItem(
                sourceText = "One",
                translatedText = "Uno",
                sourceLanguage = "en",
                targetLanguage = "es",
                translationType = TranslationType.TEXT
            )
        )
        val res2 = repository.insertHistoryItem(
            TranslationHistoryItem(
                sourceText = "Two",
                translatedText = "Dos",
                sourceLanguage = "en",
                targetLanguage = "es",
                translationType = TranslationType.IMAGE
            )
        )

        val id1 = (res1 as AppResult.Success).data
        val id2 = (res2 as AppResult.Success).data

        assertEquals(2, repository.getAllHistory().first().size)

        // Delete single item
        repository.deleteHistoryItem(id1)
        val remaining = repository.getAllHistory().first()
        assertEquals(1, remaining.size)
        assertEquals(id2, remaining[0].id)

        // Clear all
        repository.clearAllHistory()
        val empty = repository.getAllHistory().first()
        assertTrue(empty.isEmpty())
    }
}
