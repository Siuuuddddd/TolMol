
package com.tolmol.marketplace.infra.photos
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
@Component
class PhotoSanitizer : com.tolmol.marketplace.service.PhotoProcessor {
    override fun clean(bytes: ByteArray, type: String): ByteArray {
        require(bytes.size in 1..5 * 1024 * 1024) { "Photos must be at most 5 MB" }
        val actual = when {
            bytes.size > 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "image/jpeg"
            bytes.take(8).toByteArray().contentEquals(byteArrayOf(137.toByte(),80,78,71,13,10,26,10)) -> "image/png"
            bytes.size > 12 && String(bytes,0,4,Charsets.US_ASCII) == "RIFF" && String(bytes,8,4,Charsets.US_ASCII) == "WEBP" -> "image/webp"
            else -> ""
        }
        require(actual.isNotEmpty() && actual == type) { "Use a valid JPEG, PNG or WebP photo" }
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { input ->
            val readers = ImageIO.getImageReaders(input)
            require(readers.hasNext()) { "Unable to decode photo" }
            val reader = readers.next()
            try {
                reader.input = input
                val w = reader.getWidth(0)
                val h = reader.getHeight(0)
                require(w > 0 && h > 0 && w.toLong()*h <= 40_000_000) { "Photo dimensions are too large" }
                val params = reader.defaultReadParam
                val sample = maxOf(1, maxOf(w,h)/1600)
                params.setSourceSubsampling(sample,sample,0,0)
                val decoded = reader.read(0,params)
                val rgb = BufferedImage(decoded.width,decoded.height,BufferedImage.TYPE_INT_RGB)
                val g = rgb.createGraphics()
                try { g.color=java.awt.Color.WHITE; g.fillRect(0,0,rgb.width,rgb.height); g.drawImage(decoded,0,0,null) } finally { g.dispose() }
                // A fresh RGB image and writer discard all source metadata, including GPS.
                return ByteArrayOutputStream().use { out ->
                    check(ImageIO.write(rgb,"jpeg",out)); out.toByteArray()
                }
            } finally { reader.dispose() }
        }
    }
}
