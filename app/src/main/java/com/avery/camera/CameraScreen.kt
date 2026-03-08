package com.example.camera

import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CameraScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var photoUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val previewView = remember { PreviewView(context) }

    // Mode carousel
    var cameraMode by remember { mutableStateOf("Photo") }
    val modes = listOf("Photo", "Video", "Pro")

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera preview
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize()) { view ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val capture = ImageCapture.Builder().build()
                imageCapture = capture

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        capture
                    )
                } catch (exc: Exception) {
                    Log.e("Camera", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }

        // Top mode carousel
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color(0x55000000), shape = RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            modes.forEach { mode ->
                Text(
                    text = mode,
                    color = if (cameraMode == mode) Color.White else Color.LightGray,
                    style = MaterialTheme.typography.body1,
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .clickable { cameraMode = mode }
                )
            }
        }

        // Bottom bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Gallery preview
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Gray, shape = CircleShape)
                    .clickable {
                        photoUri?.let { uri ->
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.setDataAndType(uri, "image/*")
                            intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            context.startActivity(intent)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("📷")
            }

            // Shutter button
            Button(
                onClick = {
                    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg"
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera")
                        }
                    }
                    val outputOptions = ImageCapture.OutputFileOptions
                        .Builder(context.contentResolver,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues)
                        .build()
                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Camera", "Photo capture failed: ${exc.message}", exc)
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                photoUri = output.savedUri
                                Log.d("Camera", "Photo saved: $photoUri")
                            }
                        }
                    )
                },
                shape = CircleShape,
                modifier = Modifier
                    .size(72.dp)
            ) {}

            // Switch camera
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.Gray, shape = CircleShape)
                    .clickable {
                        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        else CameraSelector.DEFAULT_BACK_CAMERA
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("🔄")
            }
        }
    }
}