package com.example.objectclassifiertest

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.nnapi.NnApiDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.image.ops.Rot90Op

class ObjectClassifier(
    private val context: Context,
    private val imageSize: Size,
    private val imageRotationDegrees: Int
) {
    private val tfImageBuffer = TensorImage(DataType.UINT8)
    private val tflite by lazy {
        Interpreter(
            FileUtil.loadMappedFile(context, "classification_model.tflite"),
            Interpreter.Options().addDelegate(NnApiDelegate())
        )
    }

    private val labels: List<String> by lazy {
        FileUtil.loadLabels(context, "classification_model_label.txt")
    }

    private val tfInputSize by lazy {
        val inputIndex = 0
        val inputShape = tflite.getInputTensor(inputIndex).shape()
        Size(inputShape[2], inputShape[1]) // Order of axis is: {1, height, width, 3}
    }

    private val tfImageProcessor by lazy {
        val cropSize = minOf(imageSize.width, imageSize.height)
        ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(
                ResizeOp(
                    tfInputSize.height, tfInputSize.width, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR
                )
            )
            .add(Rot90Op(-imageRotationDegrees / 90))
            .add(NormalizeOp(0f, 1f))
            .build()
    }

    private val rawValue by lazy {
        arrayOf(ByteArray(labels.count()))
    }

    private val outputBuffer by lazy {
        mapOf(0 to rawValue)
    }

    fun classifyObject(bitmap: Bitmap): String  {
        val tfImage =  tfImageProcessor.process(tfImageBuffer.apply { load(bitmap) })
        tflite.runForMultipleInputsOutputs(arrayOf(tfImage.buffer), outputBuffer)

        // find the index of max value in outputBuffer
        var maxValue: Byte = 0
        var targetIndex: Int = -1
        for ((index, value) in rawValue[0].withIndex()) {
            if (maxValue < value) {
                maxValue = value
                targetIndex = index
            }
        }

        if (targetIndex >= 0 && maxValue > 42) { // if there is an Object and accuracy is above 33%
            return labels[targetIndex]
        }
        return ""
    }
}
