package com.example.features.protection

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.core.protection.PlacePhotoHelper
import com.example.core.protection.ProtectionRuntimeState

/**
 * شاشة رنين المنبه الخارق الكاملة:
 * لا يتوقف الرنين إلا بعد تصوير المكان المحدد بالكاميرا ومطابقته للصورة المرجعية أو إدخال رمز QR المخصص.
 */
@Composable
fun WakeAlarmRingingFullScreen(
    runtimeState: ProtectionRuntimeState,
    onDismissVerified: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // منع الرجوع نهائياً أثناء رنين المنبه الخارق (Anti-Cheat)
    BackHandler(enabled = true) {
        // محاولة الرجوع محظورة حتى التحقق
    }

    val isPhotoPlaceMode = runtimeState.verificationMethod != "QR"
    val referenceBitmap: Bitmap? = remember(runtimeState.activeAlarmId) {
        PlacePhotoHelper.loadReferencePlaceBitmap(context, runtimeState.activeAlarmId)
    }

    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }
    var manualCodeInput by remember { mutableStateOf("") }

    val systemCameraFallbackLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { capturedBitmap: Bitmap? ->
        if (capturedBitmap != null) {
            val result = PlacePhotoHelper.compareWithReferencePlace(context, capturedBitmap, runtimeState.activeAlarmId)
            if (result.isMatch) {
                isSuccess = true
                onDismissVerified("تم التحقق بنجاح (${result.similarityPercent}%) وإيقاف المنبه")
            } else {
                feedbackMessage = result.feedbackMessage
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14090D))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFEF4444).copy(alpha = 0.2f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.AlarmOn,
                        contentDescription = null,
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Text(
                text = runtimeState.wakeAlarmTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            if (isPhotoPlaceMode) {
                Text(
                    text = "وجه الكاميرا نحو ${runtimeState.placeName} والتقط صورة لإيقاف المنبه",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )

                if (referenceBitmap != null) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                bitmap = referenceBitmap.asImageBitmap(),
                                contentDescription = "الصورة المرجعية للمكان",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    text = "المكان المطلوب تصويره",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = runtimeState.placeName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                    ) {
                        LivePlaceCameraPreview(
                            onPreviewReady = { previewViewRef = it }
                        )
                    }

                    Button(
                        onClick = {
                            val captured = previewViewRef?.bitmap
                            if (captured != null) {
                                val result = PlacePhotoHelper.compareWithReferencePlace(context, captured, runtimeState.activeAlarmId)
                                if (result.isMatch) {
                                    isSuccess = true
                                    onDismissVerified("تم التحقق بنجاح (${result.similarityPercent}%) وإيقاف المنبه")
                                } else {
                                    feedbackMessage = result.feedbackMessage
                                }
                            } else {
                                systemCameraFallbackLauncher.launch(null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("capture_place_verify_btn"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصوير المكان وإيقاف المنبه", fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("منح صلاحية الكاميرا")
                    }

                    OutlinedButton(
                        onClick = { systemCameraFallbackLauncher.launch(null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("فتح الكاميرا للتصوير", color = Color.White)
                    }
                }
            } else {
                Text(
                    text = "أدخل رمز QR المخصص لإيقاف المنبه: ${runtimeState.expectedQrCode}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                if (hasCameraPermission) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black)
                    ) {
                        LivePlaceCameraPreview(
                            onPreviewReady = { previewViewRef = it }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        label = { Text("رمز التحقق", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (manualCodeInput.trim().equals(runtimeState.expectedQrCode.trim(), ignoreCase = true)) {
                                onDismissVerified("تم التحقق من الرمز وإيقاف المنبه")
                            } else {
                                feedbackMessage = "الرمز المدخل غير مطابق"
                            }
                        }
                    ) {
                        Text("تأكيد")
                    }
                }
            }

            feedbackMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSuccess) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LivePlaceCameraPreview(
    onPreviewReady: (PreviewView) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            onPreviewReady(previewView)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview
                    )
                } catch (_: Exception) {
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * شاشة حظر التطبيقات التي تظهر فور محاولة فتح تطبيق محظور
 */
@Composable
fun AppBlockedFullScreenOverlay(
    appName: String,
    blockReason: String = "",
    isSleepMode: Boolean,
    onReturnHome: () -> Unit,
    onEmergencyUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1120))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Icon(
                    imageVector = if (isSleepMode) Icons.Default.Bedtime else Icons.Default.Block,
                    contentDescription = null,
                    tint = Color(0xFFF87171),
                    modifier = Modifier.size(52.dp)
                )
                Text(
                    text = if (isSleepMode) "وضع النوم مفعل" else "التطبيق محظور حالياً",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "تم إيقاف الوصول إلى تطبيق $appName",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (blockReason.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.18f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = blockReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Button(
                    onClick = onReturnHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("العودة إلى الشاشة الرئيسية", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = onEmergencyUnlock) {
                    Text("فك الحظر الاضطراري", color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}
