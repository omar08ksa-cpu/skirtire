package com.example.spatial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * الخريطة المصغرة التفاعلية (Minimap)
 * تعرض جميع الغرف الـ 10 وفق توزيعها الفراغي (X, Y)
 * مع تمييز الغرفة الحالية وإمكانية الانتقال الفوري باللمس (Tap-to-teleport)
 */
@Composable
fun SpatialMinimapModal(
    currentRoom: SpatialRoom,
    onTeleportTo: (SpatialRoom) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .wrapContentHeight()
                .padding(vertical = 24.dp)
                .testTag("spatial_minimap_modal"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // شريط العنوان مع زر الإغلاق
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "خريطة الغرف الفراغية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "الموقع الحالي: ${currentRoom.title} ${currentRoom.coordinateLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = currentRoom.domain.primaryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Explore,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                // المخطط الشبكي الفراغي للـ 10 غرف
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // الصف العلوي Y = +1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimapCell(
                            room = SpatialRoom.FOCUS_TIMER,
                            isCurrent = currentRoom == SpatialRoom.FOCUS_TIMER,
                            onClick = { onTeleportTo(SpatialRoom.FOCUS_TIMER) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.TASKS,
                            isCurrent = currentRoom == SpatialRoom.TASKS,
                            onClick = { onTeleportTo(SpatialRoom.TASKS) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.UNIFIED_CALENDAR,
                            isCurrent = currentRoom == SpatialRoom.UNIFIED_CALENDAR,
                            onClick = { onTeleportTo(SpatialRoom.UNIFIED_CALENDAR) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // الصف الأوسط Y = 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        MinimapCell(
                            room = SpatialRoom.HABITS,
                            isCurrent = currentRoom == SpatialRoom.HABITS,
                            onClick = { onTeleportTo(SpatialRoom.HABITS) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.HOME,
                            isCurrent = currentRoom == SpatialRoom.HOME,
                            onClick = { onTeleportTo(SpatialRoom.HOME) },
                            modifier = Modifier.weight(1.1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.PRAYERS,
                            isCurrent = currentRoom == SpatialRoom.PRAYERS,
                            onClick = { onTeleportTo(SpatialRoom.PRAYERS) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.QURAN,
                            isCurrent = currentRoom == SpatialRoom.QURAN,
                            onClick = { onTeleportTo(SpatialRoom.QURAN) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // الصف السفلي Y = -1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinimapCell(
                            room = SpatialRoom.BLOCKER,
                            isCurrent = currentRoom == SpatialRoom.BLOCKER,
                            onClick = { onTeleportTo(SpatialRoom.BLOCKER) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.SETTINGS_STATS,
                            isCurrent = currentRoom == SpatialRoom.SETTINGS_STATS,
                            onClick = { onTeleportTo(SpatialRoom.SETTINGS_STATS) },
                            modifier = Modifier.weight(1f)
                        )
                        MinimapCell(
                            room = SpatialRoom.ADHKAR,
                            isCurrent = currentRoom == SpatialRoom.ADHKAR,
                            onClick = { onTeleportTo(SpatialRoom.ADHKAR) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // دليل الألوان الفراغية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpatialLegendItem(
                        color = SpatialDomain.PRODUCTIVITY.primaryColor,
                        title = "الإنتاجية (يسار)"
                    )
                    SpatialLegendItem(
                        color = SpatialDomain.CORE.primaryColor,
                        title = "الأساس (وسط)"
                    )
                    SpatialLegendItem(
                        color = SpatialDomain.ISLAMIC.primaryColor,
                        title = "إسلامي (يمين)"
                    )
                }

                Text(
                    text = "المس أي غرفة للانتقال إليها فوراً",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MinimapCell(
    room: SpatialRoom,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isCurrent) {
        room.domain.glowColor
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }

    val backgroundColor = if (isCurrent) {
        room.domain.primaryColor.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("minimap_room_${room.name.lowercase()}"),
        shape = RoundedCornerShape(14.dp),
        color = backgroundColor,
        border = BorderStroke(if (isCurrent) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // شارة الإحداثي
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isCurrent) room.domain.primaryColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = room.coordinateLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCurrent) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }

            // اسم الغرفة
            Text(
                text = room.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = if (isCurrent) room.domain.primaryColor else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // نقطة المؤشر للحالة الحالية
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isCurrent) room.domain.primaryColor else Color.Transparent)
            )
        }
    }
}

@Composable
private fun SpatialLegendItem(color: Color, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
