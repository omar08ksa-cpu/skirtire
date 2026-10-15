package com.example.core.protection

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * مستقبل مسؤول الجهاز (Device Admin) لمنع الحذف السريع للتطبيق وقت الاندفاع.
 * شفاف للمستخدم ١٠٠٪ ويمكن إيقافه عبر مخرج الطوارئ أو إعدادات النظام.
 */
class AppDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "تم تفعيل حماية التطبيق من الحذف وقت الاندفاع (مدير يومي)",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(
            context,
            "تم إيقاف مسؤول الجهاز لتطبيق مدير يومي",
            Toast.LENGTH_SHORT
        ).show()
    }
}
