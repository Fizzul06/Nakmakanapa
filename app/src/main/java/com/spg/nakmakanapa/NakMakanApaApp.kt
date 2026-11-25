package com.spg.nakmakanapa

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Application class that installs a single shared Coil [ImageLoader].
 *
 * Why this exists: before this, every AsyncImage call used Coil's bare defaults
 * (tiny memory cache, no disk cache, no HTTP cache). Google Place Photo URLs are
 * 302 redirects to short-lived signed URLs, so without caching every visit to the
 * Home screen re-downloaded every thumbnail AND re-billed a Place Photo request.
 *
 * With this loader configured:
 *  - Photos are served from disk on the second and later views (no new API call).
 *  - HTTP-level caching follows the redirect to the CDN and stores the bytes.
 *  - Thumbnails are downsampled to the view size instead of full resolution.
 */
class NakMakanApaApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        // HTTP cache shared by OkHttp so redirect targets are cached on disk.
        val httpCache = Cache(
            directory = File(cacheDir, "places_http_cache"),
            maxSize = 60L * 1024 * 1024 // 60 MB
        )

        val httpClient = OkHttpClient.Builder()
            .cache(httpCache)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(httpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20) // 20% of app memory for decoded bitmaps
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "places_image_cache"))
                    .maxSizeBytes(120L * 1024 * 1024) // 120 MB of decoded image data
                    .build()
            }
            .respectCacheHeaders(true)
            .crossfade(150)
            .build()
    }
}