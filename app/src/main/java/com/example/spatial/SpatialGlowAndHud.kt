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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * توهج حواف الشاشة التلميحي (Edge Hints):
 * - أخضر زمردي / ذهبي دافئ للجهة اليمنى (القسم الإسلامي)
 * - أزرق سماوي بارد للجهة اليسرى (قسم الإنتاجية)
 * - تلميح علوي وسفلي ناعم عند توفر غرف في الأعلى أو الأسفل
 */
@Composable
fun SpatialEdgeHintsOverlay(
    currentRoom: SpatialRoom,
    modifier: Modifier = Modifier
) {
    val adjacentRooms = remember(currentRoom) { SpatialRoom.getAdjacentRooms(currentRoom) }

    val hasRight = adjacentRooms.containsKey(SpatialDirection.LEFT) // سحب يسار ينتقل يمين
    val hasLeft = adjacentRooms.containsKey(SpatialDirection.RIGHT) // سحب يمين ينتقل يسار
    val hasTop = adjacentRooms.containsKey(SpatialDirection.DOWN)   // سحب أسفل ينتقل لأعلى
    val hasBottom = adjacentRooms.containsKey(SpatialDirection.UP)  // سحب أعلى ينتقل لأسفل

    // نبض ضوئي هادئ جداً
    val infiniteTransition = rememberInfiniteTransition(label = "edge_pulse")
    val alphaGlow by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha_glow"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // حافة اليمين (القسم الإسلامي)
        if (hasRight) {
            val rightRoom = adjacentRooms[SpatialDirection.LEFT]
            val glowColor = rightRoom?.domain?.glowColor ?: SpatialDomain.ISLAMIC.glowColor
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, glowColor.copy(alpha = alphaGlow))
                        )
                    )
            )
        }

        // حافة اليسار (قسم الإنتاجية)
        if (hasLeft) {
            val leftRoom = adjacentRooms[SpatialDirection.RIGHT]
            val glowColor = leftRoom?.domain?.glowColor ?: SpatialDomain.PRODUCTIVITY.glowColor
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(10.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(glowColor.copy(alpha = alphaGlow), Color.Transparent)
                        )
                    )
            )
        }

        // حافة الأعلى
        if (hasTop) {
            val topRoom = adjacentRooms[SpatialDirection.DOWN]
            val glowColor = topRoom?.domain?.glowColor ?: MaterialTheme.colorScheme.primary
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(glowColor.copy(alpha = alphaGlow * 0.7f), Color.Transparent)
                        )
                    )
            )
        }

        // حافة الأسفل
        if (hasBottom) {
            val bottomRoom = adjacentRooms[SpatialDirection.UP]
            val glowColor = bottomRoom?.domain?.glowColor ?: MaterialTheme.colorScheme.secondary
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, glowColor.copy(alpha = alphaGlow * 0.7f))
                        )
                    )
            )
        }
    }
}

/**
 * شريط الرأس العائم الأنيق (HUD):
 * يعرض اسم الغرفة الحالية مع الإحداثي (X, Y)
 * ويمكّن المستخدم من لمس الإحداثي لفتح الخريطة المصغرة (Minimap)
 */
@Composable
fun SpatialTopHud(
    currentRoom: SpatialRoom,
    onOpenMinimap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // شارة الغرفة والإحداثي التفاعلية لفتح الخريطة
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            tonalElevation = 3.dp,
            border = BorderStroke(1.dp, currentRoom.domain.primaryColor.copy(alpha = 0.35f)),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = onOpenMinimap)
                .testTag("spatial_hud_room_pill")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(currentRoom.domain.primaryColor)
                )
                Text(
                    text = currentRoom.coordinateLabel,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = currentRoom.domain.primaryColor
                )
                Text(
                    text = "•",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    text = currentRoom.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // زر فتح الخريطة المصغرة
        IconButton(
            onClick = onOpenMinimap,
            modifier = Modifier
                .size(36.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    CircleShape
                )
                .testTag("open_minimap_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Grid4x4,
                contentDescription = "الخريطة المصغرة",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * مؤشر مقاومة السحب الرأسي (Vertical List Protection)
 * يظهر عند الوصول لنهاية القائمة والسحب الإضافي للانتقال للغرفة السفلية أو العلوية
 */
@Composable
fun VerticalPullResistanceIndicator(
    progress: Float, // من 0.0 إلى 1.0
    targetRoom: SpatialRoom,
    isPullingUp: Boolean,
    modifier: Modifier = Modifier
) {
    if (progress <= 0.05f) return

    val label = if (isPullingUp) {
        "اسحب أكثر للانتقال إلى: ${targetRoom.title} ${targetRoom.coordinateLabel}"
    } else {
        "اسحب لأسفل للانتقال إلى: ${targetRoom.title} ${targetRoom.coordinateLabel}"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = progress)),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
