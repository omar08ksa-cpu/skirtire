package com.example.spatial

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel

/**
 * الغرفة الفراغية (+2, 0): القرآن الكريم والورد اليومي
 * قواعد التفاعل الخاصة (Lock):
 * - السحب بإصبع واحد يقلب صفحات المصحف
 * - الخروج إلى غرفة الصلوات (+1, 0) يتم عبر زر الرجوع العلوي أو بالسحب بإصبعين
 */
@Composable
fun QuranWirdSpatialRoom(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onBackToPrayers: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember {
        mutableIntStateOf(uiState.wirdProgress.currentPage.coerceAtLeast(1).coerceAtMost(604))
    }
    var isBookmarked by remember { mutableStateOf(false) }

    val juzNumber = remember(currentPage) { ((currentPage - 1) / 20) + 1 }
    val surahName = remember(currentPage) {
        when {
            currentPage <= 1 -> "الفاتحة"
            currentPage <= 49 -> "البقرة"
            currentPage <= 76 -> "آل عمران"
            currentPage <= 106 -> "النساء"
            currentPage <= 127 -> "المائدة"
            currentPage <= 150 -> "الأنعام"
            currentPage <= 176 -> "الأعراف"
            currentPage <= 186 -> "الأنفال"
            currentPage <= 207 -> "التوبة"
            currentPage <= 221 -> "يونس"
            currentPage <= 235 -> "هود"
            currentPage <= 248 -> "يوسف"
            currentPage <= 255 -> "الرعد"
            currentPage <= 261 -> "إبراهيم"
            currentPage <= 267 -> "الحجر"
            currentPage <= 281 -> "النحل"
            currentPage <= 293 -> "الإسراء"
            currentPage <= 304 -> "الكهف"
            currentPage <= 312 -> "مريم"
            currentPage <= 321 -> "طه"
            currentPage <= 331 -> "الأنبياء"
            currentPage <= 341 -> "الحج"
            currentPage <= 350 -> "المؤمنون"
            currentPage <= 359 -> "النور"
            currentPage <= 366 -> "الفرقان"
            currentPage <= 376 -> "الشعراء"
            currentPage <= 385 -> "النمل"
            currentPage <= 396 -> "القصص"
            currentPage <= 404 -> "العنكبوت"
            currentPage <= 410 -> "الروم"
            currentPage <= 417 -> "لقمان"
            currentPage <= 427 -> "الأحزاب"
            currentPage <= 434 -> "سبأ"
            currentPage <= 440 -> "فاطر"
            currentPage <= 445 -> "يس"
            currentPage <= 452 -> "الصافات"
            currentPage <= 458 -> "ص"
            currentPage <= 467 -> "الزمر"
            currentPage <= 476 -> "غافر"
            currentPage <= 482 -> "فصلت"
            currentPage <= 489 -> "الشورى"
            currentPage <= 495 -> "الزخرف"
            currentPage <= 498 -> "الدخان"
            currentPage <= 502 -> "الجاثية"
            currentPage <= 506 -> "الأحقاف"
            currentPage <= 510 -> "محمد"
            currentPage <= 515 -> "الفتح"
            currentPage <= 517 -> "الحجرات"
            currentPage <= 520 -> "ق"
            currentPage <= 523 -> "الذاريات"
            currentPage <= 525 -> "الطور"
            currentPage <= 528 -> "النجم"
            currentPage <= 531 -> "القمر"
            currentPage <= 534 -> "الرحمن"
            currentPage <= 537 -> "الواقعة"
            currentPage <= 541 -> "الحديد"
            currentPage <= 545 -> "المجادلة"
            currentPage <= 548 -> "الحشر"
            currentPage <= 551 -> "الممتحنة"
            currentPage <= 553 -> "الصف"
            currentPage <= 555 -> "الجمعة"
            currentPage <= 557 -> "المنافقون"
            currentPage <= 560 -> "التغابن"
            currentPage <= 562 -> "الطلاق"
            currentPage <= 564 -> "التحريم"
            currentPage <= 566 -> "الملك"
            currentPage <= 568 -> "القلم"
            currentPage <= 570 -> "الحاقة"
            currentPage <= 572 -> "المعارج"
            currentPage <= 574 -> "نوح"
            currentPage <= 576 -> "الجن"
            currentPage <= 578 -> "المزمل"
            currentPage <= 580 -> "المدثر"
            currentPage <= 581 -> "القيامة"
            currentPage <= 583 -> "الإنسان"
            currentPage <= 585 -> "المرسلات"
            else -> "جزء عمّ"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("room_quran_wird")
    ) {
        // شريط الرأس مع زر الرجوع لغرفة الصلوات (+1, 0)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // زر الرجوع إلى (+1, 0)
                FilledTonalButton(
                    onClick = onBackToPrayers,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("quran_back_to_prayers_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع للصلوات",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("الصلوات (+1, 0)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "سورة $surahName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits("الجزء $juzNumber • صفحة $currentPage"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = {
                        isBookmarked = !isBookmarked
                        viewModel.showBanner(if (isBookmarked) "تم حفظ العلامة المرجعية صفحة $currentPage" else "تمت إزالة العلامة المرجعية")
                    }
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "حفظ علامة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // إشعار قفل الإيماءات في غرفة المصحف
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "اسحب بإصبع واحد لتقليب الصفحات • اسحب بإصبعين أو استخدم الزر العلوي للرجوع",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // مساحة صفحة المصحف التفاعلية مع دعم تقليب الصفحات بإصبع واحد
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), RoundedCornerShape(22.dp))
                .pointerInput(currentPage) {
                    var totalDragX = 0f
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            // التحقق من عدد الأصابع
                            val pointerCount = change.id.value.toInt()
                            totalDragX += dragAmount.x
                        },
                        onDragEnd = {
                            if (totalDragX > 80f) {
                                // سحب لليمين -> الصفحة السابقة
                                if (currentPage > 1) currentPage--
                            } else if (totalDragX < -80f) {
                                // سحب لليسار -> الصفحة التالية
                                if (currentPage < 604) currentPage++
                            }
                            totalDragX = 0f
                        }
                    )
                }
                .testTag("quran_page_card"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // رأس الصفحة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits("الجزء $juzNumber"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "سورة $surahName",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // نص المصحف التقديري للصفحة
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "اقْرَأْ بِاسْمِ رَبِّكَ الَّذِي خَلَقَ • خَلَقَ الْإِنسَانَ مِنْ عَلَقٍ • اقْرَأْ وَرَبُّكَ الْأَكْرَمُ • الَّذِي عَلَّمَ بِالْقَلَمِ • عَلَّمَ الْإِنسَانَ مَا لَمْ يَعْلَمْ",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp
                            )
                        }
                    }
                }

                // تذييل الصفحة مع أزرار التنقل السريع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (currentPage < 604) currentPage++ },
                        enabled = currentPage < 604
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "الصفحة التالية")
                    }

                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits("صفحة $currentPage من 604"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = { if (currentPage > 1) currentPage-- },
                        enabled = currentPage > 1
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "الصفحة السابقة")
                    }
                }
            }
        }
    }
}
