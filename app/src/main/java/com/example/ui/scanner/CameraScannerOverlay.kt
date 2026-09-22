package com.example.ui.scanner

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.ui.ScannerMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@kotlin.OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScannerOverlay(
    mode: ScannerMode,
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    var isTorchOn by remember { mutableStateOf(false) }
    var cameraControl: Camera? by remember { mutableStateOf(null) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }
    var lastScannedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("camera_scanner_container")
        ) {
            if (cameraPermissionState.status.isGranted) {
                // Camera Preview using CameraX
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val barcodeScanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            val executor = Executors.newSingleThreadExecutor()

                            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                val now = System.currentTimeMillis()
                                                if (!rawValue.isNullOrBlank() && (now - lastScannedTime > 1500L)) {
                                                    lastScannedTime = now
                                                    triggerHapticFeedback(ctx)
                                                    onBarcodeScanned(rawValue)
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                val cam = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = cam
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    }
                )

                // Viewfinder scanning animation
                ScannerViewfinder()
            } else {
                // Permission not granted fallback
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Camera Permission Needed",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "The camera is used for instant scanning of product barcodes and inventory tracking.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { cameraPermissionState.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("request_camera_perm_button")
                    ) {
                        Text("Grant Camera Access")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(
                        onClick = { showManualInputDialog = true },
                        modifier = Modifier.testTag("manual_barcode_perm_fallback")
                    ) {
                        Text("Enter Barcode Manually", color = Color.White)
                    }
                }
            }

            // Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("close_scanner_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Scanner",
                        tint = Color.White
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = if (mode == ScannerMode.SCAN_TO_CART) "POS: Scan to Add" else "Inventory: Scan Item",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                IconButton(
                    onClick = {
                        isTorchOn = !isTorchOn
                        cameraControl?.cameraControl?.enableTorch(isTorchOn)
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("toggle_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Torch",
                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                    )
                }
            }

            // Bottom Floating Bar: Manual Barcode fallback button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showManualInputDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.9f),
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("manual_barcode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Manual Code Entry",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enter Barcode Manually",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Manual Barcode Dialog
            if (showManualInputDialog) {
                Dialog(onDismissRequest = { showManualInputDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Manual Barcode Entry",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = manualBarcodeText,
                                onValueChange = { manualBarcodeText = it },
                                label = { Text("Barcode / SKU") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("manual_barcode_input")
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showManualInputDialog = false }) {
                                    Text("Cancel")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (manualBarcodeText.isNotBlank()) {
                                            onBarcodeScanned(manualBarcodeText.trim())
                                            showManualInputDialog = false
                                        }
                                    },
                                    modifier = Modifier.testTag("submit_manual_barcode")
                                ) {
                                    Text("Submit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScannerViewfinder() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserYPercent by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxWidth = size.width * 0.75f
            val boxHeight = boxWidth * 0.65f
            val left = (size.width - boxWidth) / 2f
            val top = (size.height - boxHeight) / 2f

            // Darkened translucent overlay surrounding the scan rect
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = size
            )

            // Cutout target area border
            drawRoundRect(
                color = Color(0xFF0D9488),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 3.dp.toPx())
            )

            // Animated laser sweep line
            val currentLaserY = top + (boxHeight * laserYPercent)
            drawLine(
                color = Color(0xFF14B8A6),
                start = Offset(left + 16.dp.toPx(), currentLaserY),
                end = Offset(left + boxWidth - 16.dp.toPx(), currentLaserY),
                strokeWidth = 3.dp.toPx()
            )
        }

        // Instructional Text above target box
        Text(
            text = "Align barcode within frame",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 260.dp)
                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

private fun triggerHapticFeedback(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(80)
        }
    } catch (e: Exception) {
        // Safe fallback
    }
}
