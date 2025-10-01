package com.sirim.scanner.data.db

sealed class StorageRecord {
    abstract val id: Long
    abstract val createdAt: Long

    data class Sirim(val record: SirimRecord) : StorageRecord() {
        override val id: Long = record.id
        override val createdAt: Long = record.createdAt
    }

    data class Sku(val record: SkuRecord) : StorageRecord() {
        override val id: Long = record.id
        override val createdAt: Long = record.createdAt
    }
}

fun List<SirimRecord>.asStorageRecords(): List<StorageRecord> = map(StorageRecord::Sirim)
fun List<SkuRecord>.asStorageRecords(): List<StorageRecord> = map(StorageRecord::Sku)
