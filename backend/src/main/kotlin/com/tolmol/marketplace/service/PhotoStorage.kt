
package com.tolmol.marketplace.service
interface PhotoStorage {
    fun save(bytes: ByteArray): String
    fun read(key: String): ByteArray
    fun delete(key: String)
}
