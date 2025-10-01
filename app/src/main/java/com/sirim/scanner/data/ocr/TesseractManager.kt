package com.sirim.scanner.data.ocr

import android.content.Context
import android.graphics.Bitmap
import cz.adaptech.tesseract4android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TesseractManager(context: Context) {

    private val appContext = context.applicationContext

    private val baseDir: File = File(context.filesDir, "tesseract").apply { mkdirs() }
    private val tessDataDir: File = File(baseDir, "tessdata").apply { mkdirs() }
    private val mutex = Mutex()

    private var tessBaseApi: TessBaseAPI? = null
    private var initialised = false

    suspend fun recognise(bitmap: Bitmap): String? = mutex.withLock {
        if (!ensureEngine()) return null
        val engine = tessBaseApi ?: return null
        return try {
            engine.setImage(bitmap)
            engine.utF8Text?.takeIf { it.isNotBlank() }
        } catch (error: Exception) {
            null
        } finally {
            engine.clear()
        }
    }

    private fun ensureEngine(): Boolean {
        if (initialised) {
            return tessBaseApi != null
        }

        val engine = TessBaseAPI()
        val hasTrainedData = ensureTrainedDataExists()
        val success = hasTrainedData && runCatching {
            engine.init(baseDir.absolutePath, "eng")
        }.getOrDefault(false)

        if (success) {
            engine.pageSegMode = TessBaseAPI.PageSegMode.PSM_AUTO_OSD
            engine.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-/. ")
            engine.setVariable("textord_heavy_nr", "1")
            tessBaseApi = engine
        } else {
            engine.end()
            tessBaseApi = null
        }

        initialised = true
        return tessBaseApi != null
    }

    private fun ensureTrainedDataExists(): Boolean {
        val trainedData = File(tessDataDir, "eng.traineddata")
        if (trainedData.exists()) {
            return true
        }

        return copyBundledModel(trainedData)
    }

    private fun copyBundledModel(destination: File): Boolean {
        val assetName = "tessdata/eng.traineddata"
        return runCatching {
            appContext.assets.open(assetName)
        }.mapCatching { stream ->
            stream.use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
            destination.exists()
        }.getOrElse { false }
    }
}
