package com.example.core.prayer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class QuranAyah(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val text: String,
    val isStartOfSurah: Boolean
)

data class QuranPage(
    val pageNumber: Int,
    val juzNumber: Int,
    val hizbNumber: Int,
    val surahNames: List<String>,
    val ayahs: List<QuranAyah>
)

data class QuranSurahMeta(
    val number: Int,
    val name: String,
    val startPage: Int,
    val ayahsCount: Int = 0,
    val isMakki: Boolean = true
)

object QuranDataProvider {

    @Volatile
    private var isLoaded = false
    private val pagesMap = HashMap<Int, QuranPage>(604)
    private val surahsList = ArrayList<QuranSurahMeta>(114)

    fun ensureLoaded(context: Context) {
        if (isLoaded) return
        synchronized(this) {
            if (isLoaded) return
            try {
                // 1. Load surahs
                val surahsJsonStr = context.assets.open("quran/surahs.json").bufferedReader().use { it.readText() }
                val surahsArray = JSONArray(surahsJsonStr)
                for (i in 0 until surahsArray.length()) {
                    val obj = surahsArray.getJSONObject(i)
                    surahsList.add(
                        QuranSurahMeta(
                            number = obj.getInt("number"),
                            name = obj.getString("name"),
                            startPage = obj.getInt("startPage")
                        )
                    )
                }

                // 2. Load pages
                val pagesJsonStr = context.assets.open("quran/pages.json").bufferedReader().use { it.readText() }
                val pagesObj = JSONObject(pagesJsonStr)
                val keys = pagesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val pObj = pagesObj.getJSONObject(key)
                    val pNum = pObj.getInt("page")
                    val juz = pObj.getInt("juz")
                    val hizb = pObj.getInt("hizb")

                    val sNamesArray = pObj.getJSONArray("surahNames")
                    val sNames = ArrayList<String>()
                    for (j in 0 until sNamesArray.length()) {
                        sNames.add(sNamesArray.getString(j))
                    }

                    val ayahsArray = pObj.getJSONArray("ayahs")
                    val ayahs = ArrayList<QuranAyah>()
                    for (j in 0 until ayahsArray.length()) {
                        val aObj = ayahsArray.getJSONObject(j)
                        ayahs.add(
                            QuranAyah(
                                surahNumber = aObj.getInt("s"),
                                surahName = aObj.getString("sName"),
                                ayahNumber = aObj.getInt("a"),
                                text = aObj.getString("t"),
                                isStartOfSurah = aObj.optBoolean("start", false)
                            )
                        )
                    }

                    pagesMap[pNum] = QuranPage(
                        pageNumber = pNum,
                        juzNumber = juz,
                        hizbNumber = hizb,
                        surahNames = sNames,
                        ayahs = ayahs
                    )
                }
                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getPage(context: Context, pageNumber: Int): QuranPage? {
        ensureLoaded(context)
        val clamped = pageNumber.coerceIn(1, 604)
        return pagesMap[clamped]
    }

    fun getAllSurahs(context: Context): List<QuranSurahMeta> {
        ensureLoaded(context)
        return surahsList
    }

    fun getSurahNameForPage(context: Context, pageNumber: Int): String {
        ensureLoaded(context)
        val page = pagesMap[pageNumber]
        return page?.surahNames?.firstOrNull() ?: surahsList.lastOrNull { it.startPage <= pageNumber }?.name ?: "القرآن الكريم"
    }

    fun getJuzNumber(pageNumber: Int): Int {
        val clamped = pageNumber.coerceIn(1, 604)
        if (clamped <= 21) return 1
        if (clamped >= 582) return 30
        return (((clamped - 2) / 20) + 1).coerceIn(1, 30)
    }

    fun getHizbNumber(pageNumber: Int): Int {
        val juz = getJuzNumber(pageNumber)
        val half = if ((pageNumber % 20) >= 11) 2 else 1
        return ((juz - 1) * 2 + half).coerceIn(1, 60)
    }
}
