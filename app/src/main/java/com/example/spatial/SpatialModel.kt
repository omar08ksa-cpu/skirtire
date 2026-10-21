package com.example.spatial

import androidx.compose.ui.graphics.Color

enum class SpatialDomain(
    val title: String,
    val primaryColor: Color,
    val glowColor: Color
) {
    PRODUCTIVITY(
        title = "الإنتاجية",
        primaryColor = Color(0xFF0284C7), // Cool Cyan/Sky Blue
        glowColor = Color(0xFF38BDF8)
    ),
    CORE(
        title = "المحور الأساسي",
        primaryColor = Color(0xFF6366F1), // Modern Indigo
        glowColor = Color(0xFF818CF8)
    ),
    ISLAMIC(
        title = "القسم الإسلامي",
        primaryColor = Color(0xFF059669), // Warm Emerald Green
        glowColor = Color(0xFF34D399)
    )
}

enum class SpatialDirection {
    LEFT, RIGHT, UP, DOWN,
    UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
}

/**
 * خريطة الغرف الفراغية ثنائية الأبعاد (X, Y)
 * 10 غرف موزعة حسب المخطط المطلوب:
 * Y = +1:  (-1, +1) Focus Timer  | (0, +1) Tasks     | (+1, +1) Unified Calendar
 * Y =  0:  (-1,  0) Habits       | (0,  0) Home      | (+1,  0) Prayers         | (+2, 0) Quran (Wird)
 * Y = -1:  (-1, -1) Blocker      | (0, -1) Settings  | (+1, -1) Adhkar
 */
enum class SpatialRoom(
    val x: Int,
    val y: Int,
    val title: String,
    val subtitle: String,
    val domain: SpatialDomain
) {
    // المحور الرأسي والوسطي (X = 0)
    HOME(0, 0, "الرئيسية", "ملخص اليوم والمساعد الذكي", SpatialDomain.CORE),
    TASKS(0, 1, "المهام", "قوائم المهام اليومية والمشاريع", SpatialDomain.CORE),
    SETTINGS_STATS(0, -1, "الإعدادات والإحصائيات", "العامة والإنتاجية والإسلامية", SpatialDomain.CORE),

    // الجناح الأيسر - الإنتاجية (X = -1)
    HABITS(-1, 0, "العادات", "سجل الالتزام والإنجاز اليومي", SpatialDomain.PRODUCTIVITY),
    FOCUS_TIMER(-1, 1, "عداد التركيز", "البومودورو والمؤقت التصاعدي", SpatialDomain.PRODUCTIVITY),
    BLOCKER(-1, -1, "الحظر والمشتتات", "تقييد التطبيقات والحظر الخارق", SpatialDomain.PRODUCTIVITY),

    // الجناح الأيمن - الإسلامي (X = +1, +2)
    PRAYERS(1, 0, "الصلوات", "مواقيت الصلاة والتنبيهات", SpatialDomain.ISLAMIC),
    UNIFIED_CALENDAR(1, 1, "التقويم الموحد", "المواعيد والأحداث ومواقيت الصلاة", SpatialDomain.ISLAMIC),
    ADHKAR(1, -1, "الأذكار", "أذكار الصباح والمساء والصلوات", SpatialDomain.ISLAMIC),
    QURAN(2, 0, "القرآن والورد", "القراءة اليومية ومتابعة الختمة", SpatialDomain.ISLAMIC);

    val coordinateLabel: String
        get() {
            val xStr = if (x > 0) "+$x" else "$x"
            val yStr = if (y > 0) "+$y" else "$y"
            return "($xStr, $yStr)"
        }

    companion object {
        fun fromCoords(x: Int, y: Int): SpatialRoom? = entries.find { it.x == x && it.y == y }

        /**
         * العثور على الغرفة المجاورة بناءً على اتجاه السحب
         */
        fun getTargetRoom(
            current: SpatialRoom,
            direction: SpatialDirection,
            allowDiagonal: Boolean
        ): SpatialRoom? {
            val targetX = when (direction) {
                SpatialDirection.LEFT -> current.x + 1  // سحب لليسار يعني الانتقال لليمين
                SpatialDirection.RIGHT -> current.x - 1 // سحب لليمين يعني الانتقال لليسار
                SpatialDirection.UP_LEFT, SpatialDirection.DOWN_LEFT -> if (allowDiagonal) current.x + 1 else current.x
                SpatialDirection.UP_RIGHT, SpatialDirection.DOWN_RIGHT -> if (allowDiagonal) current.x - 1 else current.x
                else -> current.x
            }

            val targetY = when (direction) {
                SpatialDirection.UP -> current.y - 1   // سحب لأعلى يعني الانتقال لأسفل
                SpatialDirection.DOWN -> current.y + 1 // سحب لأسفل يعني الانتقال لأعلى
                SpatialDirection.UP_LEFT, SpatialDirection.UP_RIGHT -> if (allowDiagonal) current.y - 1 else current.y
                SpatialDirection.DOWN_LEFT, SpatialDirection.DOWN_RIGHT -> if (allowDiagonal) current.y + 1 else current.y
                else -> current.y
            }

            return fromCoords(targetX, targetY)
        }

        /**
         * جلب الغرف المجاورة المباشرة لعرض إشارات الحواف
         */
        fun getAdjacentRooms(room: SpatialRoom): Map<SpatialDirection, SpatialRoom> {
            val map = mutableMapOf<SpatialDirection, SpatialRoom>()
            fromCoords(room.x - 1, room.y)?.let { map[SpatialDirection.RIGHT] = it } // يسار في الإحداثيات يظهر عند السحب لليمين
            fromCoords(room.x + 1, room.y)?.let { map[SpatialDirection.LEFT] = it }
            fromCoords(room.x, room.y + 1)?.let { map[SpatialDirection.DOWN] = it }
            fromCoords(room.x, room.y - 1)?.let { map[SpatialDirection.UP] = it }
            return map
        }
    }
}
