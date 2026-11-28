
package com.tolmol.app.feature.listing.data
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.tolmol.app.feature.listing.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID
class ListingPhotoRepositoryImpl(private val context: Context,private val api: ListingApi) : ListingPhotoRepository {
    override suspend fun prepare(source: String): PreparedPhoto = withContext(Dispatchers.IO) {
        val uri=Uri.parse(source)
        fun stream() = requireNotNull(context.contentResolver.openInputStream(uri)) { "Unable to open photo" }
        val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        stream().use { BitmapFactory.decodeStream(it,null,bounds) }
        require(bounds.outWidth>0 && bounds.outHeight>0) { "Unable to decode photo" }
        var sample=1
        while(maxOf(bounds.outWidth,bounds.outHeight)/sample>2560) sample*=2
        val decoded=stream().use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample }) }
            ?: error("Unable to decode photo")
        val exif=stream().use { ExifInterface(it) }
        val matrix=Matrix().apply { if(exif.isFlipped) postScale(-1f,1f); postRotate(exif.rotationDegrees.toFloat()) }
        val rotated=Bitmap.createBitmap(decoded,0,0,decoded.width,decoded.height,matrix,true)
        if(rotated!==decoded) decoded.recycle()
        val factor=minOf(1f,1280f/maxOf(rotated.width,rotated.height))
        var bitmap=Bitmap.createScaledBitmap(rotated,maxOf(1,(rotated.width*factor).toInt()),maxOf(1,(rotated.height*factor).toInt()),true)
        if(bitmap!==rotated) rotated.recycle()
        try {
            var quality=80
            var bytes: ByteArray
            while(true) {
                bytes=ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG,quality,it);it.toByteArray() }
                if(bytes.size<=300*1024) break
                if(quality>50) quality-=10 else {
                    val smaller=Bitmap.createScaledBitmap(bitmap,maxOf(1,bitmap.width*3/4),maxOf(1,bitmap.height*3/4),true)
                    bitmap.recycle();bitmap=smaller;quality=80
                }
            }
            PreparedPhoto(UUID.randomUUID().toString(),source,bytes)
        } finally { bitmap.recycle() }
    }
    override suspend fun upload(listing: String,photo: PreparedPhoto) {
        api.uploadPhoto(listing,photo.key,listOf(MultipartBody.Part.createFormData("files","photo.jpg",photo.bytes.toRequestBody("image/jpeg".toMediaType()))))
    }
}
