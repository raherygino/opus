package com.gsoft.opus.presentation.photo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.gsoft.opus.data.signature.QrPayload
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCaptureScreen(
    qrPayload: QrPayload? = null,
    pairingIp: String? = null,
    pairingPort: Int? = null,
    pairingCode: String? = null,
    onNavigateBack: () -> Unit,
    viewModel: PhotoCaptureViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Connect when screen opens
    LaunchedEffect(qrPayload, pairingIp, pairingCode) {
        if (qrPayload != null) {
            viewModel.connectByQrPayload(qrPayload)
        } else if (pairingIp != null && pairingPort != null && pairingCode != null) {
            viewModel.connectByPairingCode(pairingIp, pairingPort, pairingCode)
        }
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture photo") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.disconnect()
                        onNavigateBack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Connection status
            PhotoConnectionStatusBar(
                isConnected = uiState.isConnected,
                isConnecting = uiState.isConnecting,
                desktopIp = uiState.desktopIp,
                errorMessage = uiState.errorMessage,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.isFinished) {
                // Finished state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Photo envoyée",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "En attente de validation sur le poste desktop...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = {
                            viewModel.disconnect()
                            onNavigateBack()
                        }) {
                            Text("Retour")
                        }
                    }
                }
            } else if (!uiState.isConnected && !uiState.isConnecting) {
                // Error / disconnected state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage ?: "Connexion échouée",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = onNavigateBack) {
                            Text("Retour")
                        }
                    }
                }
            } else if (uiState.isConnecting) {
                // Connecting state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connexion en cours...",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            } else if (hasCameraPermission) {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black, RoundedCornerShape(16.dp)),
                ) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        lensFacing = lensFacing,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.disconnect()
                            onNavigateBack()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Annuler")
                    }
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK)
                                CameraSelector.LENS_FACING_FRONT
                            else
                                CameraSelector.LENS_FACING_BACK
                        },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.Filled.FlipCameraAndroid,
                            contentDescription = "Changer caméra",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Button(
                        onClick = {
                            takePhoto(
                                imageCapture = imageCapture,
                                context = context,
                                onPhotoCaptured = { base64 ->
                                    viewModel.sendPhoto(base64)
                                },
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Camera, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Prendre la photo")
                    }
                }
            } else {
                // No camera permission
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Permission caméra requise",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Accorder la permission")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoConnectionStatusBar(
    isConnected: Boolean,
    isConnecting: Boolean,
    desktopIp: String?,
    errorMessage: String?,
) {
    val (color, text) = when {
        isConnected -> MaterialTheme.colorScheme.primary to "Connecté${desktopIp?.let { " à $it" } ?: ""}"
        isConnecting -> MaterialTheme.colorScheme.tertiary to "Connexion..."
        errorMessage != null -> MaterialTheme.colorScheme.error to errorMessage
        else -> MaterialTheme.colorScheme.outline to "Non connecté"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp, color = color)
    }
}

@Composable
private fun CameraPreview(
    imageCapture: ImageCapture,
    lensFacing: Int,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    key(lensFacing) {
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture,
                        )
                    } catch (e: Exception) {
                        // Ignore
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = modifier,
        )
    }
}

private fun takePhoto(
    imageCapture: ImageCapture,
    context: android.content.Context,
    onPhotoCaptured: (String) -> Unit,
) {
    val photoFile = java.io.File(context.cacheDir, "photo_capture.jpg")
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                var bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                if (bitmap != null) {
                    // Apply EXIF rotation so portrait photos are not landscape
                    bitmap = rotateBitmapByExif(bitmap, photoFile.absolutePath)
                    // Resize to max 1024px wide to keep base64 manageable
                    val scaledBitmap = resizeBitmap(bitmap, maxWidth = 1024)
                    val outputStream = ByteArrayOutputStream()
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                    val byteArray = outputStream.toByteArray()
                    val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64"
                    onPhotoCaptured(dataUrl)
                }
            }

            override fun onError(exception: ImageCaptureException) {
                // Ignore — user can retry
            }
        },
    )
}

private fun rotateBitmapByExif(bitmap: Bitmap, filePath: String): Bitmap {
    try {
        val exif = ExifInterface(filePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        }
        if (!matrix.isIdentity) {
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
    } catch (_: Exception) {
    }
    return bitmap
}

private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
    if (bitmap.width <= maxWidth) return bitmap
    val ratio = maxWidth.toFloat() / bitmap.width.toFloat()
    val newHeight = (bitmap.height * ratio).toInt()
    return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
}
