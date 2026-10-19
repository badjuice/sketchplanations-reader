package org.rdcl.nox.sketchplanationsreader.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * "Save image": copies a sketch into the user's pictures. The file name and the PNG's own text
 * fields carry the credit the CC BY-NC licence asks for, so it travels with the picture.
 */
object Gallery {
    /** The attribution the CC BY-NC licence asks for, written into the file itself. */
    class Credit(title: String, url: String) {
        val fields = linkedMapOf(
            "Title" to title,
            "Author" to "Jono Hey",
            "Copyright" to "Jono Hey, Sketchplanations. Licence: CC BY-NC 4.0",
            "Source" to url,
            "Description" to "$title. Image: Jono Hey, Sketchplanations, $url",
        )
    }

    private fun sniff(b: ByteArray): String? = when {
        b.size > 8 && b[0] == 0x89.toByte() && b[1] == 'P'.code.toByte() -> "image/png"
        b.size > 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() -> "image/jpeg"
        b.size > 6 && String(b, 0, 3, Charsets.US_ASCII) == "GIF" -> "image/gif"
        b.size > 12 && String(b, 8, 4, Charsets.US_ASCII) == "WEBP" -> "image/webp"
        else -> null
    }

    sealed interface Result {
        data class Saved(val where: String) : Result
        /** Android 8–9 needs storage permission for the shared Pictures folder. */
        data object NeedsPermission : Result
        data class Failed(val reason: String) : Result
    }

    /** The file as it should be written: real type, credit in PNG text fields, credit in the name. */
    private class Credited(val bytes: ByteArray, val mime: String, val name: String)

    private fun credited(image: ByteArray, title: String, mime: String, credit: Credit): Credited {
        // Trust the bytes over what was asked for, so the name and type always agree.
        val actual = sniff(image) ?: mime
        val bytes = if (actual == "image/png") Png.withText(image, credit.fields) else image
        val ext = when (actual) { "image/gif" -> "gif"; "image/jpeg" -> "jpg"; "image/webp" -> "webp"; else -> "png" }
        return Credited(bytes, actual, "$title (Jono Hey, Sketchplanations).$ext".replace(Regex("[\\\\/:*?\"<>|]"), " "))
    }

    /** Writes the picture to the share cache for another app to read; the previous one is removed. */
    suspend fun shareable(context: Context, image: ByteArray, title: String, mime: String, credit: Credit): Pair<File, String> =
        withContext(Dispatchers.IO) {
            val c = credited(image, title, mime, credit)
            val dir = File(context.cacheDir, "shared").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
            File(dir, c.name).apply { writeBytes(c.bytes) } to c.mime
        }

    suspend fun save(context: Context, image: ByteArray, title: String, mime: String, credit: Credit): Result =
        withContext(Dispatchers.IO) {
            val c = credited(image, title, mime, credit)
            val bytes = c.bytes
            val name = c.name
            @Suppress("NAME_SHADOWING") val mime = c.mime
            try {
                when {
                    Build.VERSION.SDK_INT >= 29 -> {
                        val values = ContentValues().apply {
                            put(MediaStore.Images.Media.DISPLAY_NAME, name)
                            put(MediaStore.Images.Media.MIME_TYPE, mime)
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Sketchplanations")
                            put(MediaStore.Images.Media.IS_PENDING, 1)
                        }
                        val resolver = context.contentResolver
                        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: throw IOException("Gallery refused the file")
                        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: throw IOException("Couldn't write")
                        resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
                        Result.Saved("Pictures › Sketchplanations")
                    }
                    else -> {
                        if (context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                            return@withContext Result.NeedsPermission
                        }
                        @Suppress("DEPRECATION")
                        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Sketchplanations").apply { mkdirs() }
                        val f = File(dir, name)
                        f.writeBytes(bytes)
                        android.media.MediaScannerConnection.scanFile(context, arrayOf(f.path), arrayOf(mime), null)
                        Result.Saved("Pictures › Sketchplanations")
                    }
                }
            } catch (e: Exception) {
                Result.Failed(e.message ?: "Couldn't save the image")
            }
        }
}

/** Adds PNG tEXt chunks (Latin-1 keywords and text) right after the header chunk. */
internal object Png {
    private val signature = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)

    fun withText(png: ByteArray, fields: Map<String, String>): ByteArray {
        if (png.size < 33 || !png.copyOfRange(0, 8).contentEquals(signature)) return png
        val ihdrEnd = 8 + 4 + 4 + 13 + 4
        val out = java.io.ByteArrayOutputStream(png.size + 512)
        out.write(png, 0, ihdrEnd)
        fields.forEach { (k, v) -> out.write(chunk("tEXt", k.toByteArray(Charsets.ISO_8859_1) + 0 + latin1(v))) }
        out.write(png, ihdrEnd, png.size - ihdrEnd)
        return out.toByteArray()
    }

    /** tEXt is Latin-1; anything outside it is replaced so the chunk stays valid. */
    private fun latin1(s: String) = s.replace("\u2014", "-").replace("\u2013", "-").map { if (it.code < 256) it else '?' }.joinToString("").toByteArray(Charsets.ISO_8859_1)

    private fun chunk(type: String, data: ByteArray): ByteArray {
        val t = type.toByteArray(Charsets.US_ASCII)
        val crc = java.util.zip.CRC32().apply { update(t); update(data) }.value
        return java.nio.ByteBuffer.allocate(12 + data.size).putInt(data.size).put(t).put(data).putInt(crc.toInt()).array()
    }
}

private operator fun ByteArray.plus(b: Int): ByteArray = this + byteArrayOf(b.toByte())
