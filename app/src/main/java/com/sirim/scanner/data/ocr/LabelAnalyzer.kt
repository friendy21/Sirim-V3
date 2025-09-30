package com.sirim.scanner.data.ocr

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.RGBLuminanceSource
import java.nio.ByteBuffer
import kotlinx.coroutines.tasks.await

class LabelAnalyzer {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
    private val multiFormatReader = MultiFormatReader()

    suspend fun analyze(imageProxy: ImageProxy): OcrResult {
        val mediaImage = imageProxy.image ?: return OcrResult.Empty
        val rotation = imageProxy.imageInfo.rotationDegrees
        val image = InputImage.fromMediaImage(mediaImage, rotation)

        val barcodeTask = barcodeScanner.process(image)
        val textTask = recognizer.process(image)

        val barcodeResults = runCatching { barcodeTask.await() }.getOrNull().orEmpty()
        val textResult = runCatching { textTask.await() }.getOrNull()

        val fields = textResult?.let { SirimLabelParser.parse(it.text) } ?: emptyMap()
        val bitmap = imageProxy.toBitmap()
        val qrCode = barcodeResults.firstOrNull()?.rawValue
            ?: bitmap?.let { decodeWithZxing(it) }

        return if (fields.isNotEmpty() || qrCode != null) {
            OcrResult.Success(
                fields = fields,
                qrCode = qrCode,
                bitmap = bitmap
            )
        } else {
            OcrResult.Empty
        }
    }

    private fun decodeWithZxing(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val binary = BinaryBitmap(HybridBinarizer(source))
        return runCatching<Result> {
            multiFormatReader.decode(binary)
        }.onSuccess {
            multiFormatReader.reset()
        }.getOrNull()?.text
    }

}

sealed interface OcrResult {
    data class Success(
        val fields: Map<String, String>,
        val qrCode: String?,
        val bitmap: Bitmap?
    ) : OcrResult

    data object Empty : OcrResult
}

private fun ImageProxy.toBitmap(): Bitmap? {
    val buffer: ByteBuffer = planes.firstOrNull()?.buffer ?: return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapUtils.nv21ToBitmap(bytes, width, height)?.rotate(imageInfo.rotationDegrees.toFloat())
}

private fun Bitmap.rotate(degrees: Float): Bitmap? = if (degrees == 0f) this else {
    val matrix = Matrix().apply { postRotate(degrees) }
    Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
