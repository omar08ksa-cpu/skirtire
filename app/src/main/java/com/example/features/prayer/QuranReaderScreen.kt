package com.example.features.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.widget.Toast
import com.example.core.prayer.QuranAyah
import com.example.core.prayer.QuranDataProvider
import com.example.core.prayer.QuranPage
import com.example.core.prayer.QuranSurahMeta
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.QuranBookmarkEntity
import com.example.data.WirdProgressEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.UthmaniFontFamily
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val WarmPaperBg = Color(0xFFFBF8EE)
private val WarmPaperBorder = Color(0xFFE2D9C2)
private val GoldAccent = Color(0xFFC59B27)
private val DeepGoldFrame = Color(0xFFB8860B)
private val IslamicGreen = Color(0xFF0F5132)
private val InkDark = Color(0xFF1C1917)
private val NightBg = Color(0xFF121417)
private val NightPaperBorder = Color(0xFF2D3748)
private val NightInk = Color(0xFFE2E8F0)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuranReaderScreen(
    initialPage: Int,
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bookmarks by viewModel.quranBookmarks.collectAsStateWithLifecycle()

    // 604 pages total. Pager index 0 corresponds to page 1.
    val initialIndex = (initialPage - 1).coerceIn(0, 603)
    val pagerState = rememberPagerState(initialPage = initialIndex) { 604 }

    var isControlsVisible by rememberSaveable { mutableStateOf(true) }
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var fontSizeSp by rememberSaveable { mutableIntStateOf(15) }
    var selectedAyahForAction by remember { mutableStateOf<QuranAyah?>(null) }

    var showIndexDialog by remember { mutableStateOf(false) }
    var showAddBookmarkDialog by remember { mutableStateOf(false) }
    var showBookmarksDrawer by remember { mutableStateOf(false) }
    var showLogReadingDialog by remember { mutableStateOf(false) }

    val currentPageNumber = pagerState.currentPage + 1
    val currentSurahName = remember(currentPageNumber) {
        QuranDataProvider.getSurahNameForPage(context, currentPageNumber)
    }
    val currentJuz = remember(currentPageNumber) {
        QuranDataProvider.getJuzNumber(currentPageNumber)
    }
    val currentHizb = remember(currentPageNumber) {
        QuranDataProvider.getHizbNumber(currentPageNumber)
    }

    // Save reading progress when page settles
    LaunchedEffect(currentPageNumber) {
        viewModel.saveCurrentQuranPage(currentPageNumber, currentSurahName)
    }

    BackHandler {
        onBack()
    }

    val bgColor = if (isNightMode) NightBg else WarmPaperBg
    val contentColor = if (isNightMode) NightInk else InkDark

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Main Horizontal Pager for 604 Madinah Pages
        // In Arabic, reading progresses right to left
        HorizontalPager(
            state = pagerState,
            reverseLayout = true,
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isControlsVisible = !isControlsVisible
                }
        ) { pageIndex ->
            val pageNum = pageIndex + 1
            val quranPage = remember(pageNum) {
                QuranDataProvider.getPage(context, pageNum)
            }

            MadinahPageContent(
                page = quranPage,
                pageNumber = pageNum,
                isNightMode = isNightMode,
                fontSizeSp = fontSizeSp,
                bookmarks = bookmarks,
                wird = uiState.wirdProgress,
                isControlsVisible = isControlsVisible,
                onAyahClick = { ayah -> selectedAyahForAction = ayah },
                onToggleControls = { isControlsVisible = !isControlsVisible },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Top Navigation Bar
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = (if (isNightMode) Color(0xFF1E242B) else Color.White).copy(alpha = 0.95f),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("quran_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "رجوع",
                            tint = contentColor
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = currentSurahName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isNightMode) GoldAccent else IslamicGreen
                        )
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("الجزء $currentJuz • الحزب $currentHizb • ص $currentPageNumber"),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.7f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Font size decrease
                        IconButton(
                            onClick = { if (fontSizeSp > 13) fontSizeSp-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A-", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = contentColor)
                        }

                        // Font size increase
                        IconButton(
                            onClick = { if (fontSizeSp < 24) fontSizeSp++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("A+", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = contentColor)
                        }

                        // Night mode toggle
                        IconButton(
                            onClick = { isNightMode = !isNightMode },
                            modifier = Modifier.size(36.dp).testTag("quran_night_mode_btn")
                        ) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "الوضع الليلي",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Fullscreen toggle
                        IconButton(
                            onClick = { isControlsVisible = false },
                            modifier = Modifier.size(36.dp).testTag("quran_fullscreen_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = "ملء الشاشة",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Index / Jump button
                        IconButton(
                            onClick = { showIndexDialog = true },
                            modifier = Modifier.size(36.dp).testTag("quran_index_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = "فهرس السور والأجزاء",
                                tint = contentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom Action Bar
        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                color = (if (isNightMode) Color(0xFF1E242B) else Color.White).copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Add Bookmark / Reflection
                    Button(
                        onClick = { showAddBookmarkDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNightMode) Color(0xFF2D3748) else IslamicGreen.copy(alpha = 0.1f),
                            contentColor = if (isNightMode) GoldAccent else IslamicGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("quran_add_bookmark_btn")
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("علامة توقف", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Bookmarks list / drawer
                    FilledTonalButton(
                        onClick = { showBookmarksDrawer = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("quran_view_bookmarks_btn")
                    ) {
                        Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("الفواصل (${bookmarks.size})"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Log Wird Reading
                    Button(
                        onClick = { showLogReadingDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNightMode) GoldAccent else IslamicGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("quran_log_wird_btn")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تسجيل الورد", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- Dialogs ---

    // 1. Index & Surahs Dialog
    if (showIndexDialog) {
        QuranIndexDialog(
            currentPage = currentPageNumber,
            onSelectPage = { selectedPage ->
                coroutineScope.launch {
                    pagerState.scrollToPage(selectedPage - 1)
                }
                showIndexDialog = false
            },
            onDismiss = { showIndexDialog = false }
        )
    }

    // 2. Add Bookmark & Reflection Dialog
    if (showAddBookmarkDialog) {
        AddBookmarkDialog(
            pageNumber = currentPageNumber,
            surahName = currentSurahName,
            onSave = { title, note, colorHex, tag ->
                viewModel.addQuranBookmark(
                    pageNumber = currentPageNumber,
                    surahName = currentSurahName,
                    ayahNumber = 1,
                    title = title,
                    note = note,
                    colorHex = colorHex,
                    tag = tag
                )
                showAddBookmarkDialog = false
            },
            onDismiss = { showAddBookmarkDialog = false }
        )
    }

    // 3. Bookmarks & Reflections List Sheet
    if (showBookmarksDrawer) {
        BookmarksListDialog(
            bookmarks = bookmarks,
            onSelectBookmark = { bookmark ->
                coroutineScope.launch {
                    pagerState.scrollToPage(bookmark.pageNumber - 1)
                }
                showBookmarksDrawer = false
            },
            onDeleteBookmark = { bookmarkId ->
                viewModel.deleteQuranBookmark(bookmarkId)
            },
            onDismiss = { showBookmarksDrawer = false }
        )
    }

    // 4. Log Wird Reading Dialog
    if (showLogReadingDialog) {
        QuickLogWirdDialog(
            currentPage = currentPageNumber,
            currentSurah = currentSurahName,
            wirdProgress = uiState.wirdProgress,
            onConfirmLog = { pagesCount ->
                viewModel.logWirdReading(
                    pagesReadNow = pagesCount,
                    bookmarkSurah = currentSurahName,
                    bookmarkAyah = 1,
                    bookmarkNote = "قراءة المصحف الشريف"
                )
                showLogReadingDialog = false
            },
            onDismiss = { showLogReadingDialog = false }
        )
    }

    // 5. Ayah Action & Stopping Dialog
    selectedAyahForAction?.let { ayah ->
        val isAyahBookmarked = bookmarks.any { it.surahName == ayah.surahName && it.ayahNumber == ayah.ayahNumber } ||
                (uiState.wirdProgress.bookmarkSurahName == ayah.surahName && uiState.wirdProgress.bookmarkAyahNumber == ayah.ayahNumber)

        AyahActionDialog(
            ayah = ayah,
            pageNumber = currentPageNumber,
            isBookmarked = isAyahBookmarked,
            onSetBookmark = { tag, colorHex, note ->
                viewModel.addQuranBookmark(
                    pageNumber = currentPageNumber,
                    surahName = ayah.surahName,
                    ayahNumber = ayah.ayahNumber,
                    title = "توقف عند ${ayah.surahName}: ${ayah.ayahNumber}",
                    note = note,
                    colorHex = colorHex,
                    tag = tag
                )
                viewModel.showBanner("تم وضع علامة التوقف عند الآية ${ayah.ayahNumber} 🔖")
                selectedAyahForAction = null
            },
            onSetWirdStop = {
                viewModel.saveCurrentQuranPage(currentPageNumber, ayah.surahName)
                viewModel.logWirdReading(
                    pagesReadNow = 0,
                    bookmarkSurah = ayah.surahName,
                    bookmarkAyah = ayah.ayahNumber,
                    bookmarkNote = "توقف عند الآية ${ayah.ayahNumber} من سورة ${ayah.surahName}"
                )
                viewModel.showBanner("تم تحديد الآية ${ayah.ayahNumber} كموضع للورد الحالي 📖")
                selectedAyahForAction = null
            },
            onCompleteWirdWithStopMark = {
                viewModel.completeTodayWirdWithStopMark(currentPageNumber, ayah.surahName, ayah.ayahNumber)
                selectedAyahForAction = null
            },
            onAddReflection = { note ->
                viewModel.addQuranBookmark(
                    pageNumber = currentPageNumber,
                    surahName = ayah.surahName,
                    ayahNumber = ayah.ayahNumber,
                    title = "تدبر: ${ayah.surahName} (${ayah.ayahNumber})",
                    note = note,
                    colorHex = "#7C3AED",
                    tag = "تدبر"
                )
                viewModel.showBanner("تم حفظ التدبر القرآني 💫")
                selectedAyahForAction = null
            },
            onDismiss = { selectedAyahForAction = null }
        )
    }
}

/**
 * Traditional Madinah Mushaf Page View
 * Shows the authentic ornate gilded border, Surah headers, and Uthmani text.
 */
@Composable
fun MadinahPageContent(
    page: QuranPage?,
    pageNumber: Int,
    isNightMode: Boolean,
    fontSizeSp: Int,
    bookmarks: List<QuranBookmarkEntity>,
    wird: WirdProgressEntity?,
    isControlsVisible: Boolean,
    onAyahClick: (QuranAyah) -> Unit,
    onToggleControls: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isNightMode) NightPaperBorder else WarmPaperBorder
    val frameGold = if (isNightMode) Color(0xFF6B5823) else DeepGoldFrame
    val textColor = if (isNightMode) NightInk else InkDark
    val surahHeaderBg = if (isNightMode) Color(0xFF1E242B) else Color(0xFFF7F2E4)

    val scrollState = rememberScrollState()
    val verticalPadding = if (isControlsVisible) 46.dp else 2.dp
    val horizontalPadding = if (isControlsVisible) 4.dp else 2.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
    ) {
        // Ornate outer border with golden frame
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val innerPadding = 5.dp.toPx()

            // Outer border
            drawRect(
                color = borderColor,
                topLeft = Offset(0f, 0f),
                size = size,
                style = Stroke(width = strokeWidth)
            )

            // Inner gold frame
            drawRect(
                color = frameGold.copy(alpha = 0.75f),
                topLeft = Offset(innerPadding, innerPadding),
                size = androidx.compose.ui.geometry.Size(
                    size.width - innerPadding * 2,
                    size.height - innerPadding * 2
                ),
                style = Stroke(width = 1.2.dp.toPx())
            )

            // Corner ornamental accents
            val cornerSize = 10.dp.toPx()
            // Top-left
            drawLine(frameGold, Offset(innerPadding, innerPadding + cornerSize), Offset(innerPadding + cornerSize, innerPadding), strokeWidth)
            // Top-right
            drawLine(frameGold, Offset(size.width - innerPadding - cornerSize, innerPadding), Offset(size.width - innerPadding, innerPadding + cornerSize), strokeWidth)
            // Bottom-left
            drawLine(frameGold, Offset(innerPadding, size.height - innerPadding - cornerSize), Offset(innerPadding + cornerSize, size.height - innerPadding), strokeWidth)
            // Bottom-right
            drawLine(frameGold, Offset(size.width - innerPadding - cornerSize, size.height - innerPadding), Offset(size.width - innerPadding, size.height - innerPadding - cornerSize), strokeWidth)
        }

        // Inner Page Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Header: Juz (Right) & Surah Name (Center) & Hizb (Left)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("الجزء ${page?.juzNumber ?: QuranDataProvider.getJuzNumber(pageNumber)}"),
                    fontFamily = UthmaniFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = frameGold
                )
                Text(
                    text = page?.surahNames?.firstOrNull() ?: "",
                    fontFamily = UthmaniFontFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = frameGold
                )
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("الحزب ${page?.hizbNumber ?: QuranDataProvider.getHizbNumber(pageNumber)}"),
                    fontFamily = UthmaniFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = frameGold
                )
            }

            Divider(color = frameGold.copy(alpha = 0.35f), thickness = 0.8.dp)

            Spacer(modifier = Modifier.height(4.dp))

            // Quran Text for the Page (fits on tall screens like S25 Ultra without scrolling)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (page == null || page.ayahs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = frameGold, modifier = Modifier.size(32.dp))
                    }
                } else {
                    var currentSurahId = -1

                    for (ayah in page.ayahs) {
                        // Check if this ayah starts a new surah on this page
                        if (ayah.surahNumber != currentSurahId && ayah.isStartOfSurah) {
                            currentSurahId = ayah.surahNumber

                            // Ornate Surah Title Banner
                            SurahHeaderBanner(
                                surahName = ayah.surahName,
                                frameGold = frameGold,
                                headerBg = surahHeaderBg,
                                textColor = textColor
                            )

                            // Bismillah Banner (All surahs except At-Tawbah 9, and Al-Fatiha 1 where Bismillah is ayah 1)
                            if (ayah.surahNumber != 9 && ayah.surahNumber != 1) {
                                BismillahBanner(frameGold = frameGold, textColor = textColor)
                            }
                        }
                    }

                    // Render Ayahs text continuously with traditional spacing and clickable ayah stopping
                    AyahsParagraph(
                        ayahs = page.ayahs,
                        fontSizeSp = fontSizeSp,
                        textColor = textColor,
                        frameGold = frameGold,
                        bookmarks = bookmarks,
                        wird = wird,
                        onAyahClick = onAyahClick,
                        onToggleControls = onToggleControls
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Divider(color = frameGold.copy(alpha = 0.35f), thickness = 0.8.dp)

            // Footer: Page Number in Arabic medallion
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(CircleShape)
                    .background(frameGold.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "﴿${UmmAlQuraCalculator.toArabicDigits(pageNumber.toString())}﴾",
                    fontFamily = UthmaniFontFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = frameGold
                )
            }
        }
    }
}

/**
 * Ornate traditional Surah banner at the start of a Surah
 */
@Composable
fun SurahHeaderBanner(
    surahName: String,
    frameGold: Color,
    headerBg: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(headerBg)
            .border(1.dp, frameGold, RoundedCornerShape(6.dp))
            .padding(vertical = 3.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("۞", color = frameGold, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "سُورَةُ $surahName",
                fontFamily = UthmaniFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("۞", color = frameGold, fontSize = 12.sp)
        }
    }
}

/**
 * Beautiful ornate Bismillah banner
 */
@Composable
fun BismillahBanner(
    frameGold: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            fontFamily = UthmaniFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = textColor.copy(alpha = 0.95f),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Continuous paragraph of Quran ayahs with authentic Uthmani script and clickable ayah stops
 */
@Composable
fun AyahsParagraph(
    ayahs: List<QuranAyah>,
    fontSizeSp: Int,
    textColor: Color,
    frameGold: Color,
    bookmarks: List<QuranBookmarkEntity>,
    wird: WirdProgressEntity?,
    onAyahClick: (QuranAyah) -> Unit,
    onToggleControls: () -> Unit
) {
    val annotatedString = buildAnnotatedString {
        for (i in ayahs.indices) {
            val ayah = ayahs[i]
            val isBookmarked = bookmarks.any { it.surahName == ayah.surahName && it.ayahNumber == ayah.ayahNumber } ||
                    (wird?.bookmarkSurahName == ayah.surahName && wird?.bookmarkAyahNumber == ayah.ayahNumber)

            pushStringAnnotation(tag = "AYAH", annotation = "${ayah.surahNumber}:${ayah.ayahNumber}")

            if (isBookmarked) {
                pushStyle(SpanStyle(background = GoldAccent.copy(alpha = 0.32f), color = GoldAccent))
            }

            append(ayah.text.trim())
            append(" ")

            if (isBookmarked) {
                append("🔖")
            }
            append("﴿")
            append(UmmAlQuraCalculator.toArabicDigits(ayah.ayahNumber.toString()))
            append("﴾")
            append(" ")

            if (isBookmarked) {
                pop()
            }
            pop()
        }
    }

    ClickableText(
        text = annotatedString,
        style = TextStyle(
            fontFamily = UthmaniFontFamily,
            fontSize = fontSizeSp.sp,
            fontWeight = FontWeight.Normal,
            color = textColor,
            lineHeight = (fontSizeSp * 1.64).sp,
            textAlign = TextAlign.Justify
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        onClick = { offset ->
            val annotation = annotatedString.getStringAnnotations(tag = "AYAH", start = offset, end = offset)
                .firstOrNull()
            if (annotation != null) {
                val parts = annotation.item.split(":")
                val sNum = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val aNum = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val foundAyah = ayahs.find { it.surahNumber == sNum && it.ayahNumber == aNum }
                if (foundAyah != null) {
                    onAyahClick(foundAyah)
                    return@ClickableText
                }
            }
            onToggleControls()
        }
    )
}

/**
 * Ayah Action & Stopping System Dialog
 */
@Composable
fun AyahActionDialog(
    ayah: QuranAyah,
    pageNumber: Int,
    isBookmarked: Boolean,
    onSetBookmark: (tag: String, colorHex: String, note: String) -> Unit,
    onSetWirdStop: () -> Unit,
    onCompleteWirdWithStopMark: () -> Unit = {},
    onAddReflection: (note: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showReflectionInput by remember { mutableStateOf(false) }
    var reflectionNote by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("ورد اليوم") }
    var selectedColor by remember { mutableStateOf("#059669") }

    val colors = listOf(
        "#059669" to "زمردي",
        "#D97706" to "ذهبي",
        "#2563EB" to "ياقوتي",
        "#DC2626" to "عقيقي",
        "#7C3AED" to "بنفسجي"
    )
    val tags = listOf("ورد اليوم", "توقف الحفظ", "تدبر", "مراجعة")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "سُورَةُ ${ayah.surahName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("الآية رقم ${ayah.ayahNumber} • صفحة $pageNumber"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Ayah Text Box in authentic Uthmani calligraphy
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${ayah.text} ﴿${UmmAlQuraCalculator.toArabicDigits(ayah.ayahNumber.toString())}﴾",
                        fontFamily = UthmaniFontFamily,
                        fontSize = 19.sp,
                        lineHeight = 32.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Text("إجراءات الآية والتوقف:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                // زر إتمام ورد اليوم ووضع علامة التوقف تلقائياً
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCompleteWirdWithStopMark() }
                        .testTag("ayah_complete_wird_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🏁 وضع علامة توقف نهاية ورد اليوم وإنجاز العادة", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text("يثبت إتمام الورد اليومي ويُنجز عادة الورد تلقائياً بالمصحف", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 1. زر علامة التوقف عند الآية
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSetBookmark(selectedTag, selectedColor, reflectionNote)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.BookmarkAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("وضع علامة توقف عند هذه الآية", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("حفظ فاصل لقراءة المصحف والرجوع إليها مباشرة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // اختيار نوع ولون الفاصل
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("لون الفاصل:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    colors.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == hex) 2.5.dp else 0.dp,
                                    color = if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                // 2. تعيين كموضع الورد الحالي
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSetWirdStop() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تحديد كموضع الورد القرآني الحالي", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("تحديث موضع القراءة وحسابات الختمة اليومية", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // 3. تدبر الآية
                if (!showReflectionInput) {
                    OutlinedButton(
                        onClick = { showReflectionInput = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة تدبر أو خواطر إيمانية حول الآية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedTextField(
                        value = reflectionNote,
                        onValueChange = { reflectionNote = it },
                        label = { Text("خواطرك وتدبراتك للآية") },
                        placeholder = { Text("اكتب ما فتح الله به عليك من تدبر...") },
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { onAddReflection(reflectionNote) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ التدبر للآية", fontWeight = FontWeight.Bold)
                    }
                }

                // 4. نسخ ومشاركة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Quran Ayah", "﴿${ayah.text}﴾ [سورة ${ayah.surahName}: ${ayah.ayahNumber}]")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "تم نسخ الآية الكريمة", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("نسخ الآية", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "﴿${ayah.text}﴾\n[سورة ${ayah.surahName}: ${ayah.ayahNumber}]")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة الآية الكريمة"))
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مشاركة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Quran Index Dialog: Surahs & Juzs quick picker
 */
@Composable
fun QuranIndexDialog(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val surahs = remember { QuranDataProvider.getAllSurahs(context) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0=السور, 1=الأجزاء, 2=رقم الصفحة
    var searchQuery by remember { mutableStateOf("") }
    var pageInputText by remember { mutableStateOf(currentPage.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "فهرس القرآن الكريم",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("السور", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("الأجزاء", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("رقم الصفحة", fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> {
                        // Surahs list with search
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("ابحث عن سورة...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val filteredSurahs = remember(searchQuery) {
                            if (searchQuery.isBlank()) surahs
                            else surahs.filter { it.name.contains(searchQuery.trim(), ignoreCase = true) }
                        }

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(filteredSurahs) { surah ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (surah.startPage == currentPage) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onSelectPage(surah.startPage) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = UmmAlQuraCalculator.toArabicDigits(surah.number.toString()),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.width(32.dp)
                                            )
                                            Text(
                                                text = "سورة ${surah.name}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("صفحة ${surah.startPage}"),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // 30 Juzs List
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items((1..30).toList()) { juz ->
                                val juzStartPage = if (juz == 1) 1 else (juz - 1) * 20 + 2
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onSelectPage(juzStartPage) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("الجزء $juz"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("يبدأ من صفحة $juzStartPage"),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Direct Page Number input & Slider
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("الانتقال المباشر لصفحة في المصحف (١ إلى ٦٠٤)", style = MaterialTheme.typography.bodyMedium)

                            OutlinedTextField(
                                value = pageInputText,
                                onValueChange = { pageInputText = it },
                                label = { Text("رقم الصفحة") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )

                            Button(
                                onClick = {
                                    val parsed = pageInputText.toIntOrNull()?.coerceIn(1, 604) ?: currentPage
                                    onSelectPage(parsed)
                                },
                                modifier = Modifier.fillMaxWidth(0.6f)
                            ) {
                                Text("انتقال", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Add Bookmark & Reflection Dialog
 */
@Composable
fun AddBookmarkDialog(
    pageNumber: Int,
    surahName: String,
    onSave: (title: String, note: String, colorHex: String, tag: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("توقف عند $surahName") }
    var note by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#059669") } // Emerald default
    var selectedTag by remember { mutableStateOf("ورد اليوم") }

    val colors = listOf(
        "#059669" to "زمردي",
        "#D97706" to "ذهبي",
        "#2563EB" to "ياقوتي أزرق",
        "#DC2626" to "عقيقي",
        "#7C3AED" to "بنفسجي"
    )

    val tags = listOf("ورد اليوم", "توقف الحفظ", "تدبر", "مراجعة")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة علامة توقف وتدبر",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("الصفحة $pageNumber • $surahName"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان العلامة") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Tags selection
                Text("نوع الفاصل:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = { selectedTag = tag },
                            label = { Text(tag, fontSize = 11.sp) }
                        )
                    }
                }

                if (selectedTag == "ورد اليوم") {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(
                                text = "حفظ الفاصل كـ «ورد اليوم» سيعتبر نهاية الورد ويُنجز عادة الورد القرآني تلقائياً بالمصحف.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Colors selection
                Text("لون العلامة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == hex) 3.dp else 1.dp,
                                    color = if (selectedColor == hex) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                // Reflection Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("الملاحظات والتدبرات القرآنية (اختياري)") },
                    placeholder = { Text("اكتب خواطرك الإيمانية أو تدبرك حول الآيات...") },
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, note, selectedColor, selectedTag) },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("حفظ الفاصل", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Bookmarks & Reflections List Dialog
 */
@Composable
fun BookmarksListDialog(
    bookmarks: List<QuranBookmarkEntity>,
    onSelectBookmark: (QuranBookmarkEntity) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "علامات التوقف والتدبرات",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                if (bookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(54.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد علامات توقف محفوظة بعد",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "اضغط على «علامة توقف» أثناء القراءة لحفظ موضعك وتدوين تدبراتك",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bookmarks, key = { it.id }) { bookmark ->
                            val color = try {
                                Color(android.graphics.Color.parseColor(bookmark.colorHex))
                            } catch (e: Exception) {
                                MaterialTheme.colorScheme.primary
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectBookmark(bookmark) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    // Colored vertical indicator
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(48.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = bookmark.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = color.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = bookmark.tag,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        val ayahLabel = if (bookmark.ayahNumber > 0) " • الآية ${bookmark.ayahNumber}" else ""
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("صفحة ${bookmark.pageNumber} • سورة ${bookmark.surahName}$ayahLabel • ${dateFormat.format(Date(bookmark.createdAt))}"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        if (bookmark.note.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = bookmark.note,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDeleteBookmark(bookmark.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "حذف",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Quick Log Wird Dialog
 */
@Composable
fun QuickLogWirdDialog(
    currentPage: Int,
    currentSurah: String,
    wirdProgress: com.example.data.WirdProgressEntity,
    onConfirmLog: (pagesCount: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var pagesReadInput by remember { mutableStateOf(wirdProgress.dailyTargetPages.toString()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "تسجيل قراءة الورد اليومي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("الموضع الحالي: صفحة $currentPage ($currentSurah)"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = pagesReadInput,
                    onValueChange = { pagesReadInput = it },
                    label = { Text("عدد الصفحات المقروءة اليوم") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "سيتم تحديث نسبة ختمتك ومزامنة عادة «الورد القرآني» في صفحة العادات تلقائياً 🌟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val count = pagesReadInput.toIntOrNull()?.coerceAtLeast(1) ?: wirdProgress.dailyTargetPages
                            onConfirmLog(count)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("تسجيل الإنجاز", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
