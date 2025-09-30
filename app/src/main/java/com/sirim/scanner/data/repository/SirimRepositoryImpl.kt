package com.sirim.scanner.data.repository

import android.content.Context
import com.sirim.scanner.data.db.SirimRecord
import com.sirim.scanner.data.db.SirimRecordDao
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SirimRepositoryImpl(
    private val dao: SirimRecordDao,
    private val context: Context
) : SirimRepository {

    private val fileMutex = Mutex()

    override val records: Flow<List<SirimRecord>> = dao.getAllRecords()

    override fun search(query: String): Flow<List<SirimRecord>> =
        dao.searchRecords("%$query%")

    override suspend fun upsert(record: SirimRecord): Long = dao.upsert(record)

    override suspend fun delete(record: SirimRecord) {
        record.imagePath?.let { path ->
            runCatching { File(path).takeIf(File::exists)?.delete() }
        }
        dao.delete(record)
    }

    override suspend fun getRecord(id: Long): SirimRecord? = dao.getRecordById(id)
    override suspend fun findBySerial(serial: String): SirimRecord? = dao.findBySerial(serial)
    override suspend fun persistImage(bytes: ByteArray, extension: String): String {
        val directory = File(context.filesDir, "captured")
        if (!directory.exists()) directory.mkdirs()
        return fileMutex.withLock {
            val file = File(directory, "sirim_${System.currentTimeMillis()}.$extension")
            FileOutputStream(file).use { output ->
                output.write(bytes)
            }
            file.absolutePath
        }
    }
}
