package com.kyzn.photofindai

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class GalleryScanner(private val context: Context) {

    fun getAllImagePaths(): List<String> {
        val paths = mutableListOf<String>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )
        
        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, 
                    id
                )
                paths.add(contentUri.toString())
            }
        }
        return paths
    }
}
