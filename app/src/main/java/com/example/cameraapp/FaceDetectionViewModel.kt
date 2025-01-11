package com.example.cameraapp

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class FaceDetectionViewModel(application: Application) : AndroidViewModel(application) {

    private val _faces = MutableLiveData<List<Face>>()
    val faces: LiveData<List<Face>> get() = _faces

    private var lastDetectedFaceId: Int? = null
    private var lastDetectionTime: Long = 0

    fun detectFaces(faces: List<Face>, mediaImage: Image) {
        if (faces.isNotEmpty()) {
            val currentFaceId = faces[0].trackingId
            val currentTime = SystemClock.elapsedRealtime()

            _faces.postValue(faces)

            // Guardar imagen si es un rostro nuevo o han pasado 40 segundos
            if (currentFaceId != lastDetectedFaceId || currentTime - lastDetectionTime >= 40000) {
                lastDetectedFaceId = currentFaceId
                lastDetectionTime = currentTime
                saveImageAsync(mediaImage)
            }
        } else {
            _faces.postValue(emptyList())
        }
    }

    private fun saveImageAsync(mediaImage: Image) {
        viewModelScope.launch(Dispatchers.IO) {
            saveImage(mediaImage)
            mediaImage.close() // Cerrar la imagen después de procesarla
        }
    }

    private fun Image.toBitmap(): Bitmap? {
        return try {
            val buffer = planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        } catch (e: Exception) {
            Log.e("BitmapConversionError", "Error al convertir la imagen: ${e.message}")
            null
        }
    }

    private fun saveImage(mediaImage: Image) {
        try {
            val bitmap = mediaImage.toBitmap()
            if (bitmap != null) {
                val fileName = "Rostro_${System.currentTimeMillis()}.jpg"
                val dir = File(
                    getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "Fotos Rostros"
                )
                if (!dir.exists()) dir.mkdirs()

                val file = File(dir, fileName)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }

                Log.d("ImageSaved", "Imagen guardada en: ${file.absolutePath}")
            } else {
                Log.e("SaveImageError", "Error al guardar la imagen: Bitmap es null")
            }
        } catch (e: Exception) {
            Log.e("SaveImageError", "Error al guardar la imagen: ${e.message}")
        }
    }
}