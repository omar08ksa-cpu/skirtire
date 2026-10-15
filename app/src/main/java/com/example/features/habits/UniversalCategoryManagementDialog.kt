package com.example.features.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity

object CategoryVisuals {
    val PALETTE = listOf(
        "#10B981", // زمردي
        "#3B82F6", // أزرق
        "#8B5CF6", // بنفسجي
        "#F59E0B", // كهرماني
        "#F43F5E", // مرجاني
        "#14B8A6", // تيل
        "#145946", // أخضر إسلامي
        "#EC4899", // وردي
        "#64748B"  // رمادي
    )

    fun getIcon(key: String): ImageVector {
        return when (key.uppercase()) {
            "FITNESS" -> Icons.Default.FitnessCenter
            "BOOK" -> Icons.Default.MenuBook
            "DEEN" -> Icons.Default.Mosque
            "WORK" -> Icons.Default.Work
            "STAR" -> Icons.Default.Star
            "CODE" -> Icons.Default.Code
            "HEART" -> Icons.Default.Favorite
            "ART" -> Icons.Default.Palette
            "LEAF" -> Icons.Default.Spa
            else -> Icons.Default.Label
        }
    }

    val ICON_KEYS = listOf(
        "FITNESS" to "لياقة",
        "BOOK" to "قراءة",
        "DEEN" to "عبادة",
        "WORK" to "عمل",
        "STAR" to "ذاتية",
        "CODE" to "برمجة",
        "HEART" to "صحة",
        "ART" to "فن",
        "LEAF" to "سكينة"
    )

    fun parseColor(hex: String, fallback: Color = Color(0xFF10B981)): Color {
        return try {
            val clean = hex.removePrefix("#")
            val colorInt = clean.toLong(16)
            if (clean.length == 6) {
                Color(0xFF000000 or colorInt)
            } else {
                Color(colorInt)
            }
        } catch (_: Exception) {
            fallback
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalCategoryManagementDialog(
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onAddCategory: (name: String, colorHex: String, iconKey: String) -> Unit,
    onUpdateCategory: (id: String, name: String, colorHex: String, iconKey: String) -> Unit,
    onDeleteCategory: (id: String) -> Unit
) {
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var isAddingNew by remember { mutableStateOf(false) }

    var inputName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(CategoryVisuals.PALETTE.first()) }
    var selectedIcon by remember { mutableStateOf("FITNESS") }

    fun resetForm() {
        editingCategory = null
        isAddingNew = false
        inputName = ""
        selectedColor = CategoryVisuals.PALETTE.first()
        selectedIcon = "FITNESS"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAddingNew) "إضافة فئة جديدة" else if (editingCategory != null) "تعديل الفئة" else "إدارة الفئات (يونيفرسل)",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isAddingNew || editingCategory != null) {
                    // نموذج الإضافة أو التعديل
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("اسم الفئة") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_name_input")
                    )

                    Text(
                        text = "اختر لون الفئة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CategoryVisuals.PALETTE) { hex ->
                            val color = CategoryVisuals.parseColor(hex)
                            val isSelected = selectedColor.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "اختر أيقونة الفئة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(CategoryVisuals.ICON_KEYS) { (key, label) ->
                            val isSelected = selectedIcon.equals(key, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedIcon = key },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(
                                        CategoryVisuals.getIcon(key),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { resetForm() }) {
                            Text("إلغاء")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (inputName.isNotBlank()) {
                                    val currentEdit = editingCategory
                                    if (currentEdit != null) {
                                        onUpdateCategory(currentEdit.id, inputName, selectedColor, selectedIcon)
                                    } else {
                                        onAddCategory(inputName, selectedColor, selectedIcon)
                                    }
                                    resetForm()
                                }
                            },
                            enabled = inputName.isNotBlank()
                        ) {
                            Text(if (editingCategory != null) "حفظ التعديل" else "إضافة الفئة")
                        }
                    }
                } else {
                    // قائمة الفئات الحالية
                    Text(
                        text = "الفئات موحدة تشمل العادات والمهام والأنشطة في كل التطبيق:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = {
                            isAddingNew = true
                            inputName = ""
                            selectedColor = CategoryVisuals.PALETTE.first()
                            selectedIcon = "FITNESS"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إنشاء فئة جديدة", fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories, key = { it.id }) { cat ->
                            val catColor = CategoryVisuals.parseColor(cat.colorHex)
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(catColor.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = CategoryVisuals.getIcon(cat.iconKey),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = cat.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingCategory = cat
                                                inputName = cat.name
                                                selectedColor = cat.colorHex
                                                selectedIcon = cat.iconKey
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "تعديل",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { onDeleteCategory(cat.id) },
                                            modifier = Modifier.size(32.dp)
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
        },
        confirmButton = {
            if (!isAddingNew && editingCategory == null) {
                TextButton(onClick = onDismiss) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
