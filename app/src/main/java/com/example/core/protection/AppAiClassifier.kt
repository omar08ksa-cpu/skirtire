package com.example.core.protection

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

data class ClassifiedAppResult(
    val packageName: String,
    val appName: String,
    val impact: String, // "POSITIVE", "NEUTRAL", "NEGATIVE", "EXCLUDED"
    val genreLabel: String // Cosmetic tag
)

object AppAiClassifier {

    private val negativeKeywords = listOf(
        "tiktok", "musically", "instagram", "snapchat", "twitter", "x.corp",
        "facebook", "pubg", "freefire", "roblox", "candycrush", "netflix",
        "twitch", "tinder", "badoo", "clashofclans", "brawlstars", "genshin",
        "reels", "shorts"
    )

    private val positiveKeywords = listOf(
        "quran", "quraan", "mushaf", "prayer", "athan", "adhan", "salat", "azkar",
        "athkar", "hisn", "tafsir", "hadith", "duolingo", "anki", "coursera",
        "edx", "udemy", "notion", "obsidian", "forest", "pomodoro", "study",
        "health", "fitness", "fit", "shealth", "strava", "khan", "quizlet"
    )

    private val excludedKeywords = listOf(
        "dialer", "telecom", "systemui", "launcher", "trebuchet", "clock",
        "packageinstaller", "settings", "emergency", "service", "carrier",
        "incallui", "keyguard", "setupwizard", "permissioncontroller"
    )

    fun classify(packageName: String, appName: String): ClassifiedAppResult {
        val lowerPkg = packageName.lowercase()
        val lowerName = appName.lowercase()

        // 1. Excluded check
        if (excludedKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) } ||
            lowerPkg.startsWith("com.android.internal") ||
            lowerPkg.startsWith("com.google.android.inputmethod")
        ) {
            return ClassifiedAppResult(packageName, appName, "EXCLUDED", "أدوات النظام")
        }

        // 2. Positive check
        if (positiveKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            val genre = if (lowerPkg.contains("quran") || lowerPkg.contains("mushaf") || lowerPkg.contains("prayer") || lowerPkg.contains("athan")) {
                "ديني"
            } else if (lowerPkg.contains("health") || lowerPkg.contains("fit")) {
                "صحة ورياضة"
            } else {
                "تعليم وإنتاجية"
            }
            return ClassifiedAppResult(packageName, appName, "POSITIVE", genre)
        }

        // 3. Negative check
        if (negativeKeywords.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            val genre = if (lowerPkg.contains("pubg") || lowerPkg.contains("game") || lowerPkg.contains("roblox") || lowerPkg.contains("genshin")) {
                "ألعاب"
            } else if (lowerPkg.contains("netflix") || lowerPkg.contains("twitch")) {
                "ترفيه"
            } else {
                "شبكات تواصل"
            }
            return ClassifiedAppResult(packageName, appName, "NEGATIVE", genre)
        }

        // 4. Neutral fallback
        return ClassifiedAppResult(packageName, appName, "NEUTRAL", "خدمات وتطبيقات عامة")
    }

    fun scanAllInstalledApps(context: Context): List<ClassifiedAppResult> {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val results = mutableListOf<ClassifiedAppResult>()

        for (app in installed) {
            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            if (isSystem && !app.packageName.contains("shealth") && !app.packageName.contains("health")) {
                continue
            }
            val label = try {
                pm.getApplicationLabel(app).toString()
            } catch (_: Exception) {
                app.packageName
            }
            results.add(classify(app.packageName, label))
        }
        return results
    }
}
