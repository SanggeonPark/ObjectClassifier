package com.example.objectclassifiertest

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Size
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.random.Random

class MainActivity : AppCompatActivity(), PreviewImageProviderInterface {
    private val permissions = listOf(Manifest.permission.CAMERA)
    private val permissionsRequestCode = Random.nextInt(0, 10000)

    private lateinit var previewImageProvider: PreviewImageProvider
    private lateinit var objectClassifier: ObjectClassifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if (!hasCamera(this)) {
            return
        }
        if (hasPermissions(this)) {
            setupCameraPreview()
        } else {
            ActivityCompat.requestPermissions(
                this, permissions.toTypedArray(), permissionsRequestCode
            )
        }
    }

    // PreviewImageProvider Interface
    override fun previewImage(bitmap: Bitmap, size: Size, imageRotationDegrees: Int) {
        if (!::objectClassifier.isInitialized) {
            objectClassifier = ObjectClassifier(this, size, imageRotationDegrees)
        }
        display(objectClassifier.classifyObject(bitmap))
    }

    private fun display(text: String) = runOnUiThread {
        text_view.text = text
    }

    // Camera Setup
    private fun setupCameraPreview() {
        if(!::previewImageProvider.isInitialized) {
            previewImageProvider = PreviewImageProvider()
        }
        previewImageProvider.setup(this, view_finder, this)
    }

    // Camera Permission
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionsRequestCode && hasPermissions(this)) {
            setupCameraPreview()
        } else {
            finish()
        }
    }

    private fun hasCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    private fun hasPermissions(context: Context) = permissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}