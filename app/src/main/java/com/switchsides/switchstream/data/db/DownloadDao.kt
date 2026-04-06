package com.switchsides.switchstream.data.db

import kotlinx.coroutines.flow.Flow

interface DownloadDao {
    fun getAll(): Flow<List<DownloadedMedia>>
    fun getCompleted(): Flow<List<DownloadedMedia>>
    suspend fun getByItemId(itemId: String): DownloadedMedia?
    fun observeByItemId(itemId: String): Flow<DownloadedMedia?>
    suspend fun insert(media: DownloadedMedia)
    suspend fun update(media: DownloadedMedia)
    suspend fun delete(media: DownloadedMedia)
    suspend fun deleteByItemId(itemId: String)
}
