package com.example.objectclassifiertest

import android.graphics.Bitmap
import android.util.Size

interface PreviewImageProviderInterface {
    fun previewImage(bitmap: Bitmap, size: Size, imageRotationDegrees: Int) {
        // Override this
    }
}