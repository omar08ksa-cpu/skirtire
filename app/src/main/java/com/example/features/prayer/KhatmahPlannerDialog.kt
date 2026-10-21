package com.example.features.prayer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.WirdProgressEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KhatmahPlannerDialog(
    wirdProgress: WirdProgressEntity,
    onSavePlan: (dailyTargetPages: Int, planType: String, khatmahTitle: String, startPage: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTargetPages by remember { mutableIntStateOf(wirdProgress.dailyTargetPages) }
    var customPagesText by remember { mutableStateOf(wirdProgress.dailyTargetPages.toString()) }
    var khatmahTitle by remember { mutableStateOf(wirdProgress.khatmahTitle.ifBlank { "الختمة الحالية" }) }
    var isCustom by remember { mutableStateOf(wirdProgress.dailyTargetPages !in listOf(2, 4, 10, 20)) }
    var startPageText by remember { mutableStateOf(wirdProgress.currentPage.toString()) }

    // Pre-calculated preset plans
    val presetPlans = listOf(
        Triple(20, "جزء كامل يومياً", "ختمة في شهر (٣٠ يوماً)"),
        Triple(10, "نصف جزء يومياً", "ختمة في شهرين (٦٠ يوماً)"),
        Triple(4, "٤ صفحات يومياً", "ختمة في ٥ أشهر (١٥١ يوماً)"),
        Triple(2, "صفحتان يومياً", "ختمة في سنة تقريباً (٣٠٢ يوماً)")
    )

    // Calculate duration in days
    val effectiveTarget = if (isCustom) {
        customPagesText.toIntOrNull()?.coerceIn(1, 100) ?: selectedTargetPages
    } else {
        selectedTargetPages
    }

    val effectiveStartPage = startPageText.toIntOrNull()?.coerceIn(1, 604) ?: wirdProgress.currentPage
    val totalPagesRemaining = (604 - effectiveStartPage + 1).coerceAtLeast(1)
    val remainingDaysToComplete = ((totalPagesRemaining + effectiveTarget - 1) / effectiveTarget).coerceAtLeast(1)
    val fullKhatmahDays = ((604 + effectiveTarget - 1) / effectiveTarget).coerceAtLeast(1)

    // Projected completion date
    val projectedCompletionMillis = System.currentTimeMillis() + remainingDaysToComplete * 86_400_000L
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()) }
    val projectedDateStr = dateFormat.format(Date(projectedCompletionMillis))

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "خطة الختمة والورد اليومي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // Khatmah Title Field
                OutlinedTextField(
                    value = khatmahTitle,
                    onValueChange = { khatmahTitle = it },
                    label = { Text("اسم الختمة") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. تحديد مكان بدأ الختمة (الصفحة الحالية)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "مكان بدأ الختمة (رقم الصفحة الحالية):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "إذا كنت موصلاً في القراءة سابقاً (مثلاً صفحة ٣١٢)، اكتب رقم الصفحة هنا للبدء منها مباشرة:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = startPageText,
                                onValueChange = { input ->
                                    val digits = input.filter { it.isDigit() }
                                    startPageText = digits
                                },
                                label = { Text("ابدأ من صفحة") },
                                placeholder = { Text("مثال: 312") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )

                            // Quick reset to page 1 chip
                            OutlinedButton(
                                onClick = { startPageText = "1" },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("صفحة ١", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        val parsedPage = startPageText.toIntOrNull()?.coerceIn(1, 604) ?: 1
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("المتبقي للختم من صفحة $parsedPage: ${604 - parsedPage + 1} صفحة"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Text(
                    text = "اختر وتيرة القراءة اليومية:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Presets list
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presetPlans.forEach { (pages, label, durationDesc) ->
                        val isSelected = !isCustom && selectedTargetPages == pages
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    isCustom = false
                                    selectedTargetPages = pages
                                    customPagesText = pages.toString()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits("$label ($pages صفحة)"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = durationDesc,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        isCustom = false
                                        selectedTargetPages = pages
                                        customPagesText = pages.toString()
                                    }
                                )
                            }
                        }
                    }

                    // Custom pages option
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isCustom) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = if (isCustom) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCustom = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تحديد عدد صفحات مخصص",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (isCustom) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = customPagesText,
                                        onValueChange = {
                                            customPagesText = it
                                            it.toIntOrNull()?.let { v -> selectedTargetPages = v }
                                        },
                                        label = { Text("عدد الصفحات يومياً") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth(0.7f)
                                    )
                                }
                            }
                            RadioButton(
                                selected = isCustom,
                                onClick = { isCustom = true }
                            )
                        }
                    }
                }

                // Calculation Summary Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "حسابات الختمة التقديرية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المدة الإجمالية للختمة:", fontSize = 13.sp)
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("$fullKhatmahDays يوماً"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("المتبقي لختم القرآن الحالي:", fontSize = 13.sp)
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("$remainingDaysToComplete يوماً ($totalPagesRemaining صفحة)"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("تاريخ الختم التقريبي:", fontSize = 13.sp)
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits(projectedDateStr),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // تنبيه ربط عادة الورد اليومي
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(
                            text = "ترتبط عادة الورد القرآني بهذه الخطة اليومية، وتكتمل آلياً فقط عند وضع علامة التوقف عند نهاية الورد بالمصحف (التعبئة اليدوية ممنوعة).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Action Buttons
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
                            onSavePlan(effectiveTarget, "PAGES", khatmahTitle, effectiveStartPage)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_khatmah_plan_btn")
                    ) {
                        Text("حفظ الخطة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
