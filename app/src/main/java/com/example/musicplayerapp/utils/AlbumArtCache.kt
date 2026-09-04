package com.example.musicplayerapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

object AlbumArtCache {
    // Sentinel object to cache negative results (files with no album art)
    val NO_ARTWORK: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8)

    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(1024)

    // Concurrency limiter to prevent CPU thrashing during fast scrolling
    private val decodeSemaphore = Semaphore(3)

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return (bitmap.byteCount / 1024).coerceAtLeast(1)
        }
    }

    fun get(filePath: String): Bitmap? {
        if (filePath.isEmpty()) return NO_ARTWORK
        return memoryCache.get(filePath)
    }

    suspend fun loadAlbumArt(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
        if (filePath.isEmpty()) return@withContext NO_ARTWORK

        memoryCache.get(filePath)?.let { return@withContext it }

        decodeSemaphore.withPermit {
            // Double check cache after acquiring permit
            memoryCache.get(filePath)?.let { return@withPermit it }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                val art = retriever.embeddedPicture
                if (art != null) {
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2 // Downsample list thumbnails to save RAM & CPU
                    }
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size, options)
                    val resultBitmap = bitmap ?: NO_ARTWORK
                    memoryCache.put(filePath, resultBitmap)
                    resultBitmap
                } else {
                    memoryCache.put(filePath, NO_ARTWORK)
                    NO_ARTWORK
                }
            } catch (e: Exception) {
                memoryCache.put(filePath, NO_ARTWORK)
                NO_ARTWORK
            } finally {
                try {
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
    }
}
