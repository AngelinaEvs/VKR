package com.nefrit.common.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun Context.saveImage(image: Bitmap, fileName: String): Uri? {
    val imagesFolder = File(cacheDir, "images")
    var uri: Uri? = null
    try {
        imagesFolder.mkdirs()
        val file = File(imagesFolder, fileName)
        val stream = FileOutputStream(file)
        image.compress(Bitmap.CompressFormat.PNG, 90, stream)
        stream.flush()
        stream.close()
        uri = FileProvider.getUriForFile(this, "wounds", file)
    } catch (e: IOException) {
    }
    return uri
}

fun getFile(uri: String): File {
    return File(uri)
}