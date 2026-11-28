
package com.tolmol.marketplace.infra.photos
import com.tolmol.marketplace.service.PhotoStorage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
@Component
class LocalPhotoStorage(@Value("\${tolmol.storage.photos-dir:./data/photos}") directory: String) : PhotoStorage {
    private val root = Path.of(directory).toAbsolutePath().normalize().also { Files.createDirectories(it) }
    private fun path(key: String): Path {
        require(key.matches(Regex("[a-f0-9-]+[.]jpg"))) { "Invalid photo key" }
        return root.resolve(key).normalize().also { require(it.parent == root) }
    }
    override fun save(bytes: ByteArray): String {
        val key = UUID.randomUUID().toString()+".jpg"
        Files.write(path(key), bytes, java.nio.file.StandardOpenOption.CREATE_NEW)
        return key
    }
    override fun read(key: String) = Files.readAllBytes(path(key))
    override fun delete(key: String) { Files.deleteIfExists(path(key)) }
}
