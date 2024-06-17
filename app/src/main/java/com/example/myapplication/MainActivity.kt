package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var takeVideoLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private var photoUri: Uri? = null
    private var videoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register the launchers before setting the content
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                photoUri?.let {
                    Log.d("CameraApp", "Picture saved at $it")
                    // Handle the saved picture here
                }
            } else {
                Log.e("CameraApp", "Failed to save picture")
            }
        }

        takeVideoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                videoUri?.let {
                    Log.d("CameraApp", "Video saved at $it")
                    // Handle the saved video here
                }
            } else {
                Log.e("CameraApp", "Failed to capture video, resultCode: ${result.resultCode}")
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true &&
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true &&
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true) {
                Log.d("CameraApp", "All permissions granted")
            } else {
                Log.d("CameraApp", "Permissions denied")
            }
        }

        setContent {
            MyApplicationTheme {
                var showOptions by remember { mutableStateOf(false) }
                var hasPermissions by remember { mutableStateOf(false) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    hasPermissions = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED

                    if (!hasPermissions) {
                        requestPermissions()
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    IconButton(onClick = { showOptions = true }) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add",
                            modifier = Modifier.size(64.dp)
                        )
                    }
                    if (showOptions) {
                        CameraOptions(
                            onCapturePhoto = {
                                Log.d("CameraApp", "Take Photo button clicked")
                                if (hasPermissions) {
                                    photoUri = createImageFileUri()
                                    photoUri?.let {
                                        Log.d("CameraApp", "Launching camera to take photo at URI: $it")
                                        takePictureLauncher.launch(it)
                                    }
                                } else {
                                    Log.d("CameraApp", "Permissions not granted, requesting permissions")
                                    requestPermissions()
                                }
                            },
                            onCaptureVideo = {
                                Log.d("CameraApp", "Record Video button clicked")
                                if (hasPermissions) {
                                    startVideoCapture()
                                } else {
                                    Log.d("CameraApp", "Permissions not granted, requesting permissions")
                                    requestPermissions()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        Log.d("CameraApp", "Requesting permissions")
        requestPermissionLauncher.launch(arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ))
    }

    private fun createImageFileUri(): Uri? {
        val externalCacheDir = externalCacheDir
        return if (externalCacheDir != null) {
            val imageFile = File(externalCacheDir, "photo_${System.currentTimeMillis()}.jpg")
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", imageFile)
        } else {
            Log.e("CameraApp", "External cache directory is null")
            null
        }
    }

    private fun startVideoCapture() {
        val videoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        videoUri = createVideoFileUri()
        videoUri?.let {
            videoIntent.putExtra(MediaStore.EXTRA_OUTPUT, it)
            try {
                takeVideoLauncher.launch(videoIntent)
            } catch (e: Exception) {
                Log.e("CameraApp", "Error starting video capture intent: ${e.message}")
            }
        }
    }

    private fun createVideoFileUri(): Uri? {
        val externalCacheDir = externalCacheDir
        return if (externalCacheDir != null) {
            val videoFile = File(externalCacheDir, "video_${System.currentTimeMillis()}.mp4")
            FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", videoFile)
        } else {
            Log.e("CameraApp", "External cache directory is null")
            null
        }
    }
}

@Composable
fun CameraOptions(onCapturePhoto: () -> Unit, onCaptureVideo: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = onCapturePhoto) {
            Text(text = "Take Photo", fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onCaptureVideo) {
            Text(text = "Record Video", fontSize = 18.sp)
        }
    }
}
