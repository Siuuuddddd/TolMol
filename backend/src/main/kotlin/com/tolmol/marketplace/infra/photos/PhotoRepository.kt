
package com.tolmol.marketplace.infra.photos
import com.tolmol.marketplace.service.*
import com.tolmol.marketplace.domain.model.ListingPhoto
import com.tolmol.platform.infra.config.Databases
import org.springframework.stereotype.Repository
import org.springframework.scheduling.annotation.Scheduled
import java.util.UUID
@Repository
class PhotoRepository(private val db: Databases, private val storage: PhotoStorage) : PhotoStore {
    private val sql get() = db.marketplace.sql
    override fun participant(listing: UUID,user: UUID) = sql.sql("select count(*) from deals where listing=? and (buyer=? or seller=?)").params(listing,user,user).query(Long::class.java).single()>0L
    override fun list(listing: UUID) = sql.sql("select * from listing_photo where listing=? order by is_cover desc,position")
        .param(listing).query { rs, _ -> ListingPhoto(rs.getObject("id",UUID::class.java),listing,
            rs.getInt("position"),rs.getLong("size"),rs.getString("storage_key"),rs.getBoolean("is_cover")) }.list()
    override fun <T> edit(listing: UUID, action: () -> T): T = db.marketplace.transactions.execute {
        sql.sql("select id from listings where id=? for update").param(listing).query(UUID::class.java).single()
        action()
    }!!
    override fun findRequest(listing: UUID,key: String): ListingPhoto? {
        val id = sql.sql("select id from listing_photo where listing=? and request_key=?").params(listing,key)
            .query(UUID::class.java).optional().orElse(null)
        return list(listing).find { it.id == id }
    }
    override fun insert(photo: ListingPhoto, request: String) {
        sql.sql("insert into listing_photo(id,listing,position,content_type,size,storage_key,is_cover,request_key) values(?,?,?,'image/jpeg',?,?,?,?)")
            .params(photo.id,photo.listing,photo.position,photo.size,photo.storageKey,photo.isCover,request).update()
    }
    override fun remove(listing: UUID, photo: UUID) {
        sql.sql("delete from listing_photo where listing=? and id=?").params(listing,photo).update()
        val remaining = list(listing)
        if(remaining.isNotEmpty() && remaining.none { it.isCover }) cover(listing,remaining.first().id)
    }
    override fun cover(listing: UUID, photo: UUID) {
        require(list(listing).any { it.id == photo }) { "Photo not found" }
        sql.sql("update listing_photo set is_cover=false where listing=?").param(listing).update()
        sql.sql("update listing_photo set is_cover=true where listing=? and id=?").params(listing,photo).update()
    }
    @Scheduled(fixedDelay=60000) fun cleanup() {
        db.marketplace.transactions.execute {
            val keys=sql.sql("select storage_key from photo_file_cleanup for update skip locked").query(String::class.java).list()
            keys.forEach { key ->
                if(sql.sql("select count(*) from listing_photo where storage_key=?").param(key).query(Long::class.java).single()==0L) storage.delete(key)
                sql.sql("delete from photo_file_cleanup where storage_key=?").param(key).update()
            }
        }
    }
}
