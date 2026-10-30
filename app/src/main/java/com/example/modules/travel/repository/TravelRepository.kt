package com.example.modules.travel.repository

import com.example.core.result.AppResult
import com.example.modules.travel.model.DestinationCountry
import com.example.modules.travel.model.TravelCategory
import com.example.modules.travel.model.TravelPhrase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface TravelRepository {
    fun getSupportedDestinations(): List<DestinationCountry>
    fun getPhrasesForCategory(category: TravelCategory, targetLanguage: String): Flow<List<TravelPhrase>>
    fun getAllPhrases(targetLanguage: String): Flow<List<TravelPhrase>>
    fun getCommonPhrases(targetLanguage: String): Flow<List<TravelPhrase>>
    fun getEmergencyPhrases(targetLanguage: String): Flow<List<TravelPhrase>>
    suspend fun searchPhrases(query: String, targetLanguage: String): AppResult<List<TravelPhrase>>
    suspend fun translateCustomPhrase(englishText: String, targetLanguage: String): TravelPhrase
}

class DefaultTravelRepository : TravelRepository {

    private val supportedDestinations = listOf(
        DestinationCountry("Spain", "🇪🇸", "es", "Spanish", "EUR", "€"),
        DestinationCountry("France", "🇫🇷", "fr", "French", "EUR", "€"),
        DestinationCountry("Japan", "🇯🇵", "ja", "Japanese", "JPY", "¥"),
        DestinationCountry("Germany", "🇩🇪", "de", "German", "EUR", "€"),
        DestinationCountry("Italy", "🇮🇹", "it", "Italian", "EUR", "€"),
        DestinationCountry("China", "🇨🇳", "zh", "Chinese", "CNY", "¥"),
        DestinationCountry("Brazil", "🇧🇷", "pt", "Portuguese", "BRL", "R$"),
        DestinationCountry("Mexico", "🇲🇽", "es", "Spanish", "MXN", "$"),
        DestinationCountry("South Korea", "🇰🇷", "ko", "Korean", "KRW", "₩"),
        DestinationCountry("Saudi Arabia", "🇸🇦", "ar", "Arabic", "SAR", "﷼"),
        DestinationCountry("India", "🇮🇳", "hi", "Hindi", "INR", "₹"),
        DestinationCountry("United States", "🇺🇸", "en", "English", "USD", "$"),
        DestinationCountry("United Kingdom", "🇬🇧", "en", "English", "GBP", "£")
    )

    override fun getSupportedDestinations(): List<DestinationCountry> = supportedDestinations

    // Pre-compiled translations for user-specified common and emergency phrases across key travel languages
    private val multilingualTranslations = mapOf(
        // COMMON PHRASES:
        "Hello" to mapOf(
            "es" to ("Hola" to "OH-lah"),
            "fr" to ("Bonjour" to "bohn-ZHOOR"),
            "ja" to ("こんにちは" to "Kohn-nee-chee-wah"),
            "de" to ("Hallo" to "HAH-loh"),
            "it" to ("Ciao" to "CHAH-oh"),
            "zh" to ("你好" to "Nǐ hǎo"),
            "pt" to ("Olá" to "oh-LAH"),
            "ko" to ("안녕하세요" to "An-nyeong-ha-se-yo"),
            "ar" to ("مرحباً" to "Mar-ha-ban"),
            "hi" to ("नमस्ते" to "Na-mas-te"),
            "en" to ("Hello" to "heh-LOH")
        ),
        "Thank you" to mapOf(
            "es" to ("Gracias" to "GRAH-syahs"),
            "fr" to ("Merci" to "mehr-SEE"),
            "ja" to ("ありがとう" to "Ah-ree-gah-toh"),
            "de" to ("Danke" to "DAHN-kuh"),
            "it" to ("Grazie" to "GRAHT-syeh"),
            "zh" to ("谢谢" to "Xièxie"),
            "pt" to ("Obrigado" to "oh-bree-GAH-doo"),
            "ko" to ("감사합니다" to "Gam-sa-ham-ni-da"),
            "ar" to ("شكراً" to "Shuk-ran"),
            "hi" to ("धन्यवाद" to "Dhan-ya-vaad"),
            "en" to ("Thank you" to "thangk yoo")
        ),
        "Please" to mapOf(
            "es" to ("Por favor" to "pohr fah-VOHR"),
            "fr" to ("S'il vous plaît" to "seel voo PLEH"),
            "ja" to ("お願いします" to "Oh-neh-gai-shee-mas"),
            "de" to ("Bitte" to "BIT-tuh"),
            "it" to ("Per favore" to "pehr fah-VOH-reh"),
            "zh" to ("请" to "Qǐng"),
            "pt" to ("Por favor" to "pohr fah-VOHR"),
            "ko" to ("부탁합니다" to "Bu-tak-ham-ni-da"),
            "ar" to ("من فضلك" to "Min fad-lak"),
            "hi" to ("कृपया" to "Krip-ya"),
            "en" to ("Please" to "pleez")
        ),
        "Excuse me" to mapOf(
            "es" to ("Disculpe" to "dees-KOOL-peh"),
            "fr" to ("Excusez-moi" to "ehk-skyoo-zay MWAH"),
            "ja" to ("すみません" to "Soo-mee-mah-sehn"),
            "de" to ("Entschuldigung" to "ehnt-SHOOL-dee-goong"),
            "it" to ("Mi scusi" to "mee SKOO-zee"),
            "zh" to ("不好意思" to "Bù hǎo yì si"),
            "pt" to ("Com licença" to "kohm lee-SEHN-sah"),
            "ko" to ("실례합니다" to "Sil-lye-ham-ni-da"),
            "ar" to ("عفواً" to "Af-wan"),
            "hi" to ("माफ़ कीजिये" to "Maaf kee-ji-ye"),
            "en" to ("Excuse me" to "ek-skyooz mee")
        ),
        "Where is the bathroom?" to mapOf(
            "es" to ("¿Dónde está el baño?" to "DOHN-deh ehs-TAH ehl BAH-nyoh?"),
            "fr" to ("Où sont les toilettes ?" to "oo sohn lay twah-LET?"),
            "ja" to ("お手洗いはどこですか？" to "Oh-teh-ah-rai wa doh-koh des ka?"),
            "de" to ("Wo ist die Toilette?" to "voh ist dee twah-LET-tuh?"),
            "it" to ("Dov'è il bagno?" to "doh-VEH eel BAHN-yoh?"),
            "zh" to ("洗手间在哪里？" to "Xǐ shǒu jiān zài nǎ lǐ?"),
            "pt" to ("Onde fica a casa de banho?" to "OHN-deh FEE-kah ah KAH-zah deh BAHN-yoo?"),
            "ko" to ("화장실이 어디예요?" to "Hwa-jang-sil-i eo-di-ye-yo?"),
            "ar" to ("أين الحمام؟" to "Ayna al-hammam?"),
            "hi" to ("शौचालय कहाँ है?" to "Shau-cha-lay ka-han hai?"),
            "en" to ("Where is the bathroom?" to "wair iz thuh bath-room?")
        ),
        "Where is the station?" to mapOf(
            "es" to ("¿Dónde está la estación?" to "DOHN-deh ehs-TAH lah ehs-tah-SYOHN?"),
            "fr" to ("Où est la gare ?" to "oo eh lah GAHR?"),
            "ja" to ("駅はどこですか？" to "Eh-kee wa doh-koh des ka?"),
            "de" to ("Wo ist der Bahnhof?" to "voh ist dehr BAHN-hohf?"),
            "it" to ("Dov'è la stazione?" to "doh-VEH lah stah-TSYOH-neh?"),
            "zh" to ("火车/地铁站在哪里？" to "Chē zhàn zài nǎ lǐ?"),
            "pt" to ("Onde fica a estação?" to "OHN-deh FEE-kah ah ehs-tah-SOWNG?"),
            "ko" to ("역이 어디예요?" to "Yeok-i eo-di-ye-yo?"),
            "ar" to ("أين المحطة؟" to "Ayna al-mahatta?"),
            "hi" to ("स्टेशन कहाँ है?" to "Stay-shan ka-han hai?"),
            "en" to ("Where is the station?" to "wair iz thuh stay-shun?")
        ),
        "How much does this cost?" to mapOf(
            "es" to ("¿Cuánto cuesta esto?" to "KWAHN-toh KWEHS-tah EHS-toh?"),
            "fr" to ("Combien ça coûte ?" to "kohm-BYEHN sah KOOT?"),
            "ja" to ("これはいくらですか？" to "Koh-reh wa ee-koo-rah des ka?"),
            "de" to ("Wie viel kostet das?" to "vee feel KOS-tet dahs?"),
            "it" to ("Quanto costa questo?" to "KWAHN-toh KOH-stah KWEH-stoh?"),
            "zh" to ("这个多少钱？" to "Zhè ge duō shǎo qián?"),
            "pt" to ("Quanto custa isto?" to "KWAHN-too KOO-stah EE-stoo?"),
            "ko" to ("이거 얼마예요?" to "I-geo eol-ma-ye-yo?"),
            "ar" to ("بكم هذا؟" to "Bikam hatha?"),
            "hi" to ("यह कितने का है?" to "Yeh kit-ne ka hai?"),
            "en" to ("How much does this cost?" to "how much duz this kawst?")
        ),
        "I don't understand." to mapOf(
            "es" to ("No entiendo." to "noh ehn-TYEHN-doh."),
            "fr" to ("Je ne comprends pas." to "zhuh nuh kohm-PRAHN pah."),
            "ja" to ("分かりません。" to "Wah-kah-ree-mah-sehn."),
            "de" to ("Ich verstehe nicht." to "ikh fehr-SHTEH-uh nikht."),
            "it" to ("Non capisco." to "nohn kah-PEE-skoh."),
            "zh" to ("我听不懂。" to "Wǒ tīng bù dǒng."),
            "pt" to ("Não compreendo." to "nowng kohm-pree-EHN-doo."),
            "ko" to ("이해하지 못해요." to "I-hae-ha-ji mot-hae-yo."),
            "ar" to ("لا أفهم." to "La afham."),
            "hi" to ("मुझे समझ नहीं आया।" to "Mujhe samajh nahin aaya."),
            "en" to ("I don't understand." to "eye dont un-der-stand.")
        ),
        "Can you help me?" to mapOf(
            "es" to ("¿Puede ayudarme?" to "PWEH-deh ah-yoo-DAHR-meh?"),
            "fr" to ("Pouvez-vous m'aider ?" to "poo-VAY voo may-DAY?"),
            "ja" to ("手伝っていただけますか？" to "Teht-soo-dah-teh ee-tah-dah-keh-mas ka?"),
            "de" to ("Können Sie mir helfen?" to "KUHN-nen zee meer HEL-fen?"),
            "it" to ("Può aiutarmi?" to "pwoh ah-yoo-TAHR-mee?"),
            "zh" to ("你能帮我吗？" to "Nǐ néng bāng wǒ ma?"),
            "pt" to ("Pode ajudar-me?" to "POH-deh ah-zhoo-DAHR-meh?"),
            "ko" to ("도와주실 수 있나요?" to "Do-wa-ju-sil su in-na-yo?"),
            "ar" to ("هل يمكنك مساعدتي؟" to "Hal yumkinuka musa'adati?"),
            "hi" to ("क्या आप मेरी मदद कर सकते हैं?" to "Kya aap meri madad kar sakte hain?"),
            "en" to ("Can you help me?" to "kan yoo help mee?")
        ),
        "I need a doctor." to mapOf(
            "es" to ("Necesito un médico." to "neh-seh-SEE-toh oon MEH-dee-koh."),
            "fr" to ("J'ai besoin d'un médecin." to "zhay buh-ZWAHN duhn mayd-SAHN."),
            "ja" to ("医者が必要です。" to "Ee-shah ga hee-tsoo-yoh des."),
            "de" to ("Ich brauche einen Arzt." to "ikh BROW-khuh EYE-nen ahrtst."),
            "it" to ("Ho bisogno di un dottore." to "oh bee-ZOHN-yoh dee oon doht-TOH-reh."),
            "zh" to ("我需要医生。" to "Wǒ xū yào yī shēng."),
            "pt" to ("Preciso de um médico." to "preh-SEE-zoo deh oom MEH-dee-koo."),
            "ko" to ("의사가 필요해요." to "Ui-sa-ga pil-yo-hae-yo."),
            "ar" to ("أحتاج إلى طبيب." to "Ahtaj ila tabeeb."),
            "hi" to ("मुझे डॉक्टर की ज़रूरत है।" to "Mujhe doctor ki zaroorat hai."),
            "en" to ("I need a doctor." to "eye need uh dahk-ter.")
        ),
        "I am lost." to mapOf(
            "es" to ("Estoy perdido/a." to "ehs-TOY pehr-DEE-doh."),
            "fr" to ("Je suis perdu(e)." to "zhuh swee pair-DYOO."),
            "ja" to ("道に迷いました。" to "Mee-chee nee mah-yoh-ee-mah-shee-tah."),
            "de" to ("Ich habe mich verlaufen." to "ikh HAH-buh mikh fehr-LOW-fen."),
            "it" to ("Mi sono perso/a." to "mee SOH-noh PEHR-soh."),
            "zh" to ("我迷路了。" to "Wǒ mí lù le."),
            "pt" to ("Estou perdido/a." to "eh-STOH pehr-DEE-doo."),
            "ko" to ("길을 잃었어요." to "Gil-eul irh-eoss-eo-yo."),
            "ar" to ("أنا تائه." to "Ana ta'ih."),
            "hi" to ("मैं खो गया हूँ।" to "Main kho gaya hoon."),
            "en" to ("I am lost." to "eye am lost.")
        ),

        // EMERGENCY PHRASES:
        "I need medical help." to mapOf(
            "es" to ("Necesito asistencia médica." to "neh-seh-SEE-toh ah-sees-TEHN-syah MEH-dee-kah."),
            "fr" to ("J'ai besoin d'aide médicale." to "zhay buh-ZWAHN dehd may-dee-KAHL."),
            "ja" to ("医療的な助けが必要です。" to "Ee-ryoh-teh-kee na tas-keh ga hee-tsoo-yoh des."),
            "de" to ("Ich brauche medizinische Hilfe." to "ikh BROW-khuh may-dee-TSEE-nee-shuh HIL-fuh."),
            "it" to ("Ho bisogno di assistenza medica." to "oh bee-ZOHN-yoh dee ahs-sees-TEHN-tsah MEH-dee-kah."),
            "zh" to ("我需要医疗援助。" to "Wǒ xū yào yī liáo yuán zhù."),
            "pt" to ("Preciso de assistência médica." to "preh-SEE-zoo deh ah-sees-TEHN-syah MEH-dee-kah."),
            "ko" to ("의료 도움이 필요합니다." to "Ui-ryo do-um-i pil-yo-ham-ni-da."),
            "ar" to ("أحتاج مساعدة طبية." to "Ahtaj musa'adah tibbiyyah."),
            "hi" to ("मुझे चिकित्सकीय सहायता की आवश्यकता है।" to "Mujhe chikitsakiya sahayata ki aavashyakta hai."),
            "en" to ("I need medical help." to "eye need med-i-kul help.")
        ),
        "Call the police." to mapOf(
            "es" to ("Llame a la policía." to "YAH-meh ah lah poh-lee-SEE-ah."),
            "fr" to ("Appelez la police." to "ah-puh-LAY lah poh-LEES."),
            "ja" to ("警察を呼んでください。" to "Keh-sats oh yohn-deh koo-dah-sai."),
            "de" to ("Rufen Sie die Polizei." to "ROO-fen zee dee poh-lee-TSYE."),
            "it" to ("Chiami la polizia." to "KYAH-mee lah poh-lee-TSEE-ah."),
            "zh" to ("请报警 / 叫警察。" to "Qǐng bào jǐng / jiào jǐng chá."),
            "pt" to ("Chame a polícia." to "SHAH-mee ah poh-LEE-syah."),
            "ko" to ("경찰을 불러주세요." to "Gyeong-chal-eul bul-leo-ju-se-yo."),
            "ar" to ("اتصل بالشرطة." to "Ittasil bish-shurtah."),
            "hi" to ("पुलिस को बुलाइए।" to "Police ko bulaaiye."),
            "en" to ("Call the police." to "kawl thuh puh-lees.")
        ),
        "I need an ambulance." to mapOf(
            "es" to ("Necesito una ambulancia." to "neh-seh-SEE-toh OO-nah ahm-boo-LAHN-syah."),
            "fr" to ("J'ai besoin d'une ambulance." to "zhay buh-ZWAHN doon ahm-byoo-LAHNS."),
            "ja" to ("救急車を呼んでください。" to "Kyoo-kyoo-shah oh yohn-deh koo-dah-sai."),
            "de" to ("Ich brauche einen Krankenwagen." to "ikh BROW-khuh EYE-nen KRAHN-ken-vah-gen."),
            "it" to ("Ho bisogno di un'ambulanza." to "oh bee-ZOHN-yoh dee oon ahm-boo-LAHN-tsah."),
            "zh" to ("我需要救护车。" to "Wǒ xū yào jiù hù chē."),
            "pt" to ("Preciso de uma ambulância." to "preh-SEE-zoo deh OO-mah ahm-boo-LAHN-syah."),
            "ko" to ("구급차가 필요해요." to "Gu-geup-cha-ga pil-yo-hae-yo."),
            "ar" to ("أحتاج إلى سيارة إسعاف." to "Ahtaj ila sayyarat is'af."),
            "hi" to ("मुझे एम्बुलेंस चाहिए।" to "Mujhe ambulance chahiye."),
            "en" to ("I need an ambulance." to "eye need un am-byoo-luns.")
        ),
        "I have an allergy." to mapOf(
            "es" to ("Tengo alergia." to "TEHN-goh ah-LEHR-hyah."),
            "fr" to ("J'ai une allergie." to "zhay oon ah-lair-ZHEE."),
            "ja" to ("アレルギーがあります。" to "Ah-rehr-gee ga ah-ree-mas."),
            "de" to ("Ich habe eine Allergie." to "ikh HAH-buh EYE-nuh ah-lehr-GEE."),
            "it" to ("Ho un'allergia." to "oh oon ahl-lehr-JEE-ah."),
            "zh" to ("我有过敏症。" to "Wǒ yǒu guò mǐn zhèng."),
            "pt" to ("Tenho uma alergia." to "TEHN-yoo OO-mah ah-lehr-ZHEE-ah."),
            "ko" to ("알레르기가 있어요." to "Al-le-reu-gi-ga iss-eo-yo."),
            "ar" to ("لدي حساسية." to "Ladayya hasasiyyah."),
            "hi" to ("मुझे एलर्जी है।" to "Mujhe allergy hai."),
            "en" to ("I have an allergy." to "eye hav un al-er-jee.")
        ),
        "I need help." to mapOf(
            "es" to ("Necesito ayuda." to "neh-seh-SEE-toh ah-YOO-dah."),
            "fr" to ("J'ai besoin d'aide." to "zhay buh-ZWAHN dehd."),
            "ja" to ("助けてください。" to "Tas-keh-teh koo-dah-sai."),
            "de" to ("Ich brauche Hilfe." to "ikh BROW-khuh HIL-fuh."),
            "it" to ("Ho bisogno di aiuto." to "oh bee-ZOHN-yoh dee ah-YOO-toh."),
            "zh" to ("我需要帮助。" to "Wǒ xū yào bāng zhù."),
            "pt" to ("Preciso de ajuda." to "preh-SEE-zoo deh ah-ZHOO-dah."),
            "ko" to ("도움이 필요해요." to "Do-um-i pil-yo-hae-yo."),
            "ar" to ("أحتاج مساعدة." to "Ahtaj musa'adah."),
            "hi" to ("मुझे सहायता चाहिए।" to "Mujhe sahayata chahiye."),
            "en" to ("I need help." to "eye need help.")
        )
    )

    private val commonEnglishList = listOf(
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

    private val emergencyEnglishList = listOf(
        "I need medical help.",
        "Call the police.",
        "I need an ambulance.",
        "I am lost.",
        "I have an allergy.",
        "I need help."
    )

    private fun resolvePhrase(id: String, english: String, targetLanguage: String, category: TravelCategory): TravelPhrase {
        val langKey = targetLanguage.lowercase().take(2)
        val pair = multilingualTranslations[english]?.get(langKey)
            ?: multilingualTranslations[english]?.get("es")
            ?: (english to null)
        return TravelPhrase(
            id = id,
            originalEnglish = english,
            translatedText = pair.first,
            phoneticPronunciation = pair.second,
            category = category,
            targetLanguageCode = targetLanguage
        )
    }

    override fun getCommonPhrases(targetLanguage: String): Flow<List<TravelPhrase>> {
        val list = commonEnglishList.mapIndexed { index, en ->
            resolvePhrase("common_${index + 1}", en, targetLanguage, TravelCategory.COMMON)
        }
        return flowOf(list)
    }

    override fun getEmergencyPhrases(targetLanguage: String): Flow<List<TravelPhrase>> {
        val list = emergencyEnglishList.mapIndexed { index, en ->
            resolvePhrase("emerg_${index + 1}", en, targetLanguage, TravelCategory.EMERGENCY)
        }
        return flowOf(list)
    }

    override fun getPhrasesForCategory(
        category: TravelCategory,
        targetLanguage: String
    ): Flow<List<TravelPhrase>> {
        return when (category) {
            TravelCategory.COMMON -> getCommonPhrases(targetLanguage)
            TravelCategory.EMERGENCY -> getEmergencyPhrases(targetLanguage)
            else -> {
                // Secondary travel categories
                val additional = when (category) {
                    TravelCategory.DINING -> listOf(
                        resolvePhrase("di_1", "Could we see the menu, please?", targetLanguage, TravelCategory.DINING),
                        resolvePhrase("di_2", "The check / bill, please.", targetLanguage, TravelCategory.DINING),
                        resolvePhrase("di_3", "How much does this cost?", targetLanguage, TravelCategory.DINING)
                    )
                    TravelCategory.TRANSIT -> listOf(
                        resolvePhrase("tr_1", "Where is the station?", targetLanguage, TravelCategory.TRANSIT),
                        resolvePhrase("tr_2", "Where is the bathroom?", targetLanguage, TravelCategory.TRANSIT),
                        resolvePhrase("tr_3", "I am lost.", targetLanguage, TravelCategory.TRANSIT)
                    )
                    else -> emptyList()
                }
                flowOf(additional)
            }
        }
    }

    override fun getAllPhrases(targetLanguage: String): Flow<List<TravelPhrase>> {
        val common = commonEnglishList.mapIndexed { index, en ->
            resolvePhrase("common_${index + 1}", en, targetLanguage, TravelCategory.COMMON)
        }
        val emergency = emergencyEnglishList.mapIndexed { index, en ->
            resolvePhrase("emerg_${index + 1}", en, targetLanguage, TravelCategory.EMERGENCY)
        }
        // Deduplicate overlapping phrases (like "I am lost.")
        val seen = mutableSetOf<String>()
        val combined = mutableListOf<TravelPhrase>()
        for (item in (emergency + common)) {
            val key = "${item.originalEnglish}_${item.category}"
            if (seen.add(key)) {
                combined.add(item)
            }
        }
        return flowOf(combined)
    }

    override suspend fun searchPhrases(
        query: String,
        targetLanguage: String
    ): AppResult<List<TravelPhrase>> {
        val all = commonEnglishList.mapIndexed { index, en ->
            resolvePhrase("common_${index + 1}", en, targetLanguage, TravelCategory.COMMON)
        } + emergencyEnglishList.mapIndexed { index, en ->
            resolvePhrase("emerg_${index + 1}", en, targetLanguage, TravelCategory.EMERGENCY)
        }
        val matches = all.filter {
            it.originalEnglish.contains(query, ignoreCase = true) ||
            it.translatedText.contains(query, ignoreCase = true)
        }
        return AppResult.Success(matches)
    }

    override suspend fun translateCustomPhrase(
        englishText: String,
        targetLanguage: String
    ): TravelPhrase {
        val match = multilingualTranslations[englishText]
        val langKey = targetLanguage.lowercase().take(2)
        val pair = match?.get(langKey) ?: (englishText to null)
        return TravelPhrase(
            id = "custom_${System.currentTimeMillis()}",
            originalEnglish = englishText,
            translatedText = pair.first,
            phoneticPronunciation = pair.second,
            category = TravelCategory.COMMON,
            targetLanguageCode = targetLanguage
        )
    }
}
