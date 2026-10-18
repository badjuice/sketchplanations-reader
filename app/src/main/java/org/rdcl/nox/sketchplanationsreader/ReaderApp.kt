package org.rdcl.nox.sketchplanationsreader

import android.app.Application
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache

class ReaderApp : Application(), ImageLoaderFactory {
    /**
     * Images are cached on disk generously and served from that cache regardless of HTTP headers,
     * so anything already seen keeps showing with no connection. Animated sketches play.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(if (Build.VERSION.SDK_INT >= 28) ImageDecoderDecoder.Factory() else GifDecoder.Factory()) }
        .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.2).build() }
        .diskCache { DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(400L * 1024 * 1024).build() }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}
