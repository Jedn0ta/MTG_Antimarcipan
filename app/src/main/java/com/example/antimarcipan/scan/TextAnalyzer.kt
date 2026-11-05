package com.example.antimarcipan.scan

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * CameraX analyzer that runs on-device OCR (ML Kit) on each frame and reports
 * the recognised text. Throttled so we don't hammer the recogniser (or the
 * Scryfall lookup that follows) on every single frame.
 */
class TextAnalyzer(
    private val minIntervalMs: Long = 700L,
    private val onText: (String) -> Unit,
) : ImageAnalysis.Analyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastRun = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastRun < minIntervalMs) {
            imageProxy.close()
            return
        }
        lastRun = now

        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val input = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees,
        )
        recognizer.process(input)
            .addOnSuccessListener { result ->
                val text = result.text
                if (text.isNotBlank()) onText(text)
            }
            .addOnCompleteListener { imageProxy.close() }
    }
}
