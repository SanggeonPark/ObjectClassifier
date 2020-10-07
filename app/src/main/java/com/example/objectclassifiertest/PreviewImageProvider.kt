package com.example.objectclassifiertest

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.lang.ref.WeakReference
import java.util.concurrent.Executors

class PreviewImageProvider {
    private lateinit var context: Context
    private lateinit var view: PreviewView
    private lateinit var bitmap: Bitmap
    private var imageRotationDegrees: Int = 0
    private val executor = Executors.newSingleThreadExecutor()
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK

    private var delegate: WeakReference<PreviewImageProviderInterface>? = null

    @SuppressLint("UnsafeExperimentalUsageError")
    fun setup(context: Context, view: PreviewView, delegate: PreviewImageProviderInterface) {
        this.context = context
        this.view = view
        this.delegate = WeakReference(delegate)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(view.display.rotation)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setTargetRotation(view.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val imageConverter = YuvToRgbConverter(context)

            imageAnalysis.setAnalyzer(executor, { image ->
                if (!::bitmap.isInitialized) {
                    imageRotationDegrees = image.imageInfo.rotationDegrees
                    bitmap = Bitmap.createBitmap(
                        image.width, image.height, Bitmap.Config.ARGB_8888)
                }
                // convert image to bitmap
                imageConverter.yuvToRgb(image = image.image!!, output = bitmap)
                delegate.previewImage(
                    bitmap = bitmap,
                    size = Size(image.width, image.height),
                    imageRotationDegrees = imageRotationDegrees
                )
                image.close()
            })

            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as LifecycleOwner, cameraSelector, preview, imageAnalysis
            )
            preview.setSurfaceProvider(view.surfaceProvider)

        }, ContextCompat.getMainExecutor(context))
    }
}