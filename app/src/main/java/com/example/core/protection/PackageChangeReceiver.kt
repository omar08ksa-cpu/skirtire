package com.example.core.protection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.AppDatabase
import com.example.data.BlockedAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pkgUri = intent.data ?: return
        val packageName = pkgUri.schemeSpecificPart ?: return

        if (packageName.isBlank() || packageName == context.packageName) return

        val db = AppDatabase.getInstance(context)
        val protectionDao = db.protectionDao()

        when (action) {
            Intent.ACTION_PACKAGE_REMOVED -> {
                val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                if (!isReplacing) {
                    // الاحتفاظ بالتطبيق في قاعدة البيانات وعدم حذفه، فقط تعيين علامة إلغاء التثبيت
                    CoroutineScope(Dispatchers.IO).launch {
                        protectionDao.setAppUninstalledStatus(packageName, true)
                    }
                }
            }

            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val existing = protectionDao.getBlockedAppsOnce().find { it.packageName == packageName }
                    val pm = context.packageManager
                    val appLabel = try {
                        val ai = pm.getApplicationInfo(packageName, 0)
                        pm.getApplicationLabel(ai).toString()
                    } catch (_: Exception) {
                        packageName
                    }

                    if (existing != null) {
                        // إعادة الربط التلقائي وحفظ الإعدادات السابقة
                        val restored = existing.copy(
                            appName = appLabel,
                            isUninstalled = false,
                            updatedAt = System.currentTimeMillis()
                        )
                        protectionDao.upsertBlockedApp(restored)
                    } else {
                        // تطبيق جديد: تصنيف فوري ذكي
                        val classified = AppAiClassifier.classify(packageName, appLabel)
                        val newApp = BlockedAppEntity(
                            packageName = packageName,
                            appName = appLabel,
                            category = classified.genreLabel,
                            usageImpact = classified.impact,
                            isRestricted = false,
                            isUninstalled = false
                        )
                        protectionDao.upsertBlockedApp(newApp)
                    }
                }
            }
        }
    }
}
