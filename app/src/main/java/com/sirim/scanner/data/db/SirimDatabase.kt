package com.sirim.scanner.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SirimRecord::class],
    version = 1,
    exportSchema = false
)
abstract class SirimDatabase : RoomDatabase() {
    abstract fun sirimRecordDao(): SirimRecordDao
}
