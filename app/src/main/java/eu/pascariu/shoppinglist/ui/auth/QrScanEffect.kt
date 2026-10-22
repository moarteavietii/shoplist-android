package eu.pascariu.shoppinglist.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanEffect(
    onQrPayload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) return

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    val lastPayload = remember { AtomicReference<String?>(null) }
    val cameraBindings = remember { CameraBindings() }
    var isDisposed by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { viewContext ->
            val previewView = PreviewView(viewContext)
            val future = ProcessCameraProvider.getInstance(viewContext)
            future.addListener({
                if (isDisposed) return@addListener
                val cameraProvider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analysisExecutor, processFrameAnalyzer(
                    barcodeScanner,
                    onPayload = { payload ->
                        if (lastPayload.compareAndSet(null, payload)) {
                            previewView.post {
                                if (!isDisposed) onQrPayload(payload)
                            }
                        }
                    },
                ))
                cameraProvider.unbindAll()
                if (isDisposed) return@addListener
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
                cameraBindings.provider = cameraProvider
                cameraBindings.useCases = listOf(preview, analysis)
            }, ContextCompat.getMainExecutor(viewContext))
            previewView
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            isDisposed = true
            cameraBindings.provider?.let { provider ->
                val useCases = cameraBindings.useCases
                if (useCases.isNotEmpty()) provider.unbind(*useCases.toTypedArray())
            }
            analysisExecutor.shutdown()
            barcodeScanner.close()
        }
    }
}

private class CameraBindings {
    var provider: ProcessCameraProvider? = null
    var useCases: List<androidx.camera.core.UseCase> = emptyList()
}

@androidx.camera.core.ExperimentalGetImage
private fun processFrameAnalyzer(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    onPayload: (String) -> Unit,
): androidx.camera.core.ImageAnalysis.Analyzer =
    ImageAnalysis.Analyzer { imageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return@Analyzer
        }
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let(onPayload)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
