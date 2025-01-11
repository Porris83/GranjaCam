package com.example.cameraapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private var previewView: PreviewView? = null
    private var overlay: Overlay? = null
    private val isDetectorActive = AtomicBoolean(true)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var faceDetector: FaceDetector? = null
    private var mediaPlayer: MediaPlayer? = null
    private val faceDetectionViewModel: FaceDetectionViewModel by viewModels()
    private var imageCapture: ImageCapture? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastDetectedFaceId: Int? = null
    private var lastDetectionTime: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        overlay = findViewById(R.id.overlay)
        mediaPlayer = MediaPlayer.create(this, R.raw.camera_sound)

        // Initialize face detector
        faceDetector = FaceDetection.getClient()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                101
            )
        } else {
            startCamera()
            checkStoragePermission()
        }

        // Observer to update face overlay
        faceDetectionViewModel.faces.observe(this) { faces ->
            if (faces != null && faces.isNotEmpty()) {
                val currentFaceId = faces[0].trackingId
                val currentTime = System.currentTimeMillis()

                if (currentFaceId != lastDetectedFaceId) {
                    takePhoto()
                    lastDetectedFaceId = currentFaceId
                    lastDetectionTime = currentTime
                    Log.d("FaceDetected", "Face detected, photo taken.")
                } else if (currentTime - lastDetectionTime >= 40000) {
                    takePhoto()
                    lastDetectionTime = currentTime
                    Log.d("FaceDetected", "Face detected after 40 seconds, photo taken.")
                }

                handler.postDelayed({
                    if (currentFaceId == lastDetectedFaceId) {
                        takePhoto()
                        lastDetectionTime = System.currentTimeMillis()
                        Log.d("FaceDetected", "Face detected after 40 seconds, photo taken.")
                    }
                }, 40000)

                overlay?.setFaces(faces)
            } else {
                overlay?.clear()
            }
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                102
            )
        } else {
            createImageDirectory()
        }
    }

    private fun createImageDirectory() {
        val dir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Fotos Rostros")
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Preview configuration
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView?.surfaceProvider)
                }

                // Camera selection
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Image analysis configuration
                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalyzer.setAnalyzer(cameraExecutor) { imageProxy ->
                    analyzeImage(imageProxy)
                }

                // Image capture configuration
                imageCapture = ImageCapture.Builder().build()

                // Bind to lifecycle
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer, imageCapture)
            } catch (e: Exception) {
                Log.e("CameraApp", "Error starting camera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyzeImage(imageProxy: ImageProxy) {
        if (!isDetectorActive.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            faceDetector?.process(image)
                ?.addOnSuccessListener { faces ->
                    faceDetectionViewModel.detectFaces(faces.filterNotNull(), mediaImage)
                }
                ?.addOnFailureListener { e ->
                    Log.e("FaceDetection", "Error detecting faces: ${e.message}")
                }
                ?.addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "Fotos Rostros/Rostro_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(this@MainActivity, "Photo saved: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()
                    Log.d("ImageSaved", "Photo saved: ${photoFile.absolutePath}")
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("ImageCapture", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        isDetectorActive.set(false)
        faceDetector?.close()
        cameraExecutor.shutdown()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            101 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
                    startCamera()
                    checkStoragePermission()
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            102 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                    createImageDirectory()
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}