package com.sirim.scanner.data

import android.content.Context
import androidx.room.Room
import com.sirim.scanner.data.db.SirimDatabase
import com.sirim.scanner.data.export.ExportManager
import com.sirim.scanner.data.ocr.LabelAnalyzer
import com.sirim.scanner.data.ocr.TesseractManager
import com.sirim.scanner.data.repository.SirimRepository
import com.sirim.scanner.data.repository.SirimRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

interface AppContainer {
    val repository: SirimRepository
    val exportManager: ExportManager
    val labelAnalyzer: LabelAnalyzer
    val applicationScope: CoroutineScope
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    private val applicationScopeImpl = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database: SirimDatabase = Room.databaseBuilder(
        context,
        SirimDatabase::class.java,
        "sirim_records.db"
    ).fallbackToDestructiveMigration().build()

    override val repository: SirimRepository by lazy {
        SirimRepositoryImpl(
            dao = database.sirimRecordDao(),
            context = context.applicationContext
        )
    }

    override val exportManager: ExportManager by lazy {
        ExportManager(context.applicationContext)
    }

    private val tesseractManager: TesseractManager by lazy {
        TesseractManager(context.applicationContext)
    }

    override val labelAnalyzer: LabelAnalyzer by lazy { LabelAnalyzer(tesseractManager) }

    override val applicationScope: CoroutineScope
        get() = applicationScopeImpl
}
