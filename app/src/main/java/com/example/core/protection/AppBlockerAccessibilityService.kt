package com.example.core.protection

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.MainActivity
import com.example.core.study.StudyTimerManager

/**
 * خدمة إمكانية الوصول لحظر التطبيقات ومنع التحايل:
 * 1) إجبار البقاء داخل شاشة المنبه الخارق أثناء الرنين ومنع الخروج منها
 * 2) منع الدخول إلى إدارة التطبيقات وإعدادات الحذف أثناء تفعيل الحماية
 * 3) حظر التطبيقات في جلسات التركيز ووضع النوم والحد اليومي (StayFree)
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        AppUsageMonitor.recordLiveForegroundSwitch(pkg, packageName)

        if (pkg == packageName || pkg in ProtectionManager.permanentExemptPackages || pkg.startsWith("com.android.settings")) return

        val protectionState = ProtectionManager.state.value

        // أثناء رنين المنبه الخارق: يمنع منعاً باتاً فتح أي تطبيق أو الانتقال للواجهة الرئيسية
        if (protectionState.isWakeAlarmRinging) {
            try {
                val wakeIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                }
                startActivity(wakeIntent)
            } catch (_: Exception) {
            }
            return
        }

        val isStudyActive = StudyTimerManager.timerState.value.isActive
        val isSleepActive = protectionState.isSleepModeActive
        val specificReason = protectionState.enforcedBlockReasonsByPackage[pkg]

        val hasAnyActiveBlock = isStudyActive || isSleepActive || specificReason != null || protectionState.isStrictProtectionEnabled
        if (!hasAnyActiveBlock) return

        val isUninstallPackage = pkg.contains("packageinstaller") && (pkg.contains("uninstaller") || pkg.contains("uninstall"))
        if (protectionState.isStrictProtectionEnabled && isUninstallPackage) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            launchBlockerScreen("إلغاء التثبيت", "حماية منع الحذف والتحايل مفعلة")
            return
        }

        val reason = when {
            specificReason != null -> specificReason
            isStudyActive && pkg in protectionState.blockedStudyPackages -> "جلسة التركيز نشطة"
            isSleepActive && pkg in protectionState.blockedSleepPackages -> "الحظر الليلي مفعل"
            else -> null
        }

        if (reason != null) {
            launchBlockerScreen(pkg, reason)
        }
    }

    private fun launchBlockerScreen(blockedLabel: String, reason: String) {
        val appLabel = try {
            val ai = packageManager.getApplicationInfo(blockedLabel, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            blockedLabel
        }
        ProtectionManager.triggerBlockerOverlayPreview(appLabel, reason)
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("intercepted_app_name", appLabel)
                putExtra("intercepted_block_reason", reason)
            }
            startActivity(intent)
        } catch (_: Exception) {
        }
    }

    override fun onInterrupt() = Unit
}
