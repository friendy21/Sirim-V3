package com.sirim.scanner.data.repository

import com.sirim.scanner.data.db.SirimRecord
import kotlinx.coroutines.flow.Flow

interface SirimRepository {
    val records: Flow<List<SirimRecord>>
    fun search(query: String): Flow<List<SirimRecord>>
    suspend fun upsert(record: SirimRecord): Long
    suspend fun delete(record: SirimRecord)
    suspend fun getRecord(id: Long): SirimRecord?
    suspend fun findBySerial(serial: String): SirimRecord?
    suspend fun persistImage(bytes: ByteArray, extension: String = "jpg"): String
}
