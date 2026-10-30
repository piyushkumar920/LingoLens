package com.example.modules.travel.model

enum class TravelCategory(val displayName: String, val iconName: String) {
    COMMON("Common Phrases", "chat"),
    EMERGENCY("Emergency & Medical", "medical_services"),
    SAVED("Saved Phrases", "star"),
    DINING("Food & Dining", "restaurant"),
    TRANSIT("Transport & Directions", "directions_bus"),
    HOTEL("Lodging & Hotel", "hotel"),
    SHOPPING("Shopping & Money", "shopping_bag"),
    CUSTOMS("Airport & Customs", "flight_land"),
    SOCIAL("Greetings & Social", "people")
}

data class TravelPhrase(
    val id: String,
    val originalEnglish: String,
    val translatedText: String,
    val phoneticPronunciation: String? = null,
    val category: TravelCategory,
    val targetLanguageCode: String
)

data class DestinationCountry(
    val countryName: String,
    val flagEmoji: String,
    val primaryLanguageCode: String,
    val primaryLanguageName: String,
    val currencyCode: String? = null,
    val currencySymbol: String? = null
)
