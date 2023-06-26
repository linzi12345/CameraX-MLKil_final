/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.camerax_mlkit

import android.Manifest
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.camera.view.PreviewView
import com.google.mlkit.vision.face.Face
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import org.tensorflow.lite.support.label.TensorLabel
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer


/**
 * A Drawable that handles displaying a QR Code's data and a bounding box around the QR code.
 */
class MaskCodeDrawable(faceList: MutableList<Face?>, qrContent:String, inputBitmap:Bitmap, ctx:Context, pre: PreviewView) : Drawable() {
    private val yes_boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 5F
        alpha = 200
    }

    private val no_boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 5F
        alpha = 200
    }
    companion object {
        private const val TAG = "CameraX-MLKit"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val MODEL_FILE = "model.tflite"
        private const val LABELS = "labels.txt"
        private const val LABEL_MASK = "mask"
        private const val LABEL_NO_MASK = "no_mask"


        private fun formatString(text: String, float: Float): String =
            "$text: ${String.format("%.1f", float * 100)}%"
        private val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).toTypedArray()
    }
    val modelFile = FileUtil.loadMappedFile(ctx, MODEL_FILE)
    val model = Interpreter(modelFile, Interpreter.Options())
    val labels = FileUtil.loadLabels(ctx, LABELS)
    val imageDataType = model.getInputTensor(0).dataType()
    val inputShape = model.getInputTensor(0).shape()
    val outputDataType = model.getOutputTensor(0).dataType()
    val outputShape = model.getOutputTensor(0).shape()
    var inputImageBuffer = TensorImage(imageDataType)
    private val outputBuffer = TensorBuffer.createFixedSize(outputShape, outputDataType)

    //

    private var scaleFactor = 1.0f
    // The number of horizontal pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private var postScaleWidthOffset = 0f
    // The number of vertical pixels needed to be cropped on each side to fit the image with the
    // area of overlay View after scaling.
    private var postScaleHeightOffset = 0f
    private val isImageFlipped = false
    private var needUpdateTransformation = true
    private val imageWidth = inputBitmap.width
    private val imageHeight = inputBitmap.height
    private val transformationMatrix = Matrix()


    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GREEN
        alpha = 255
    }

    private val yes_contentTextPaint = Paint().apply {
        color = Color.GREEN
        alpha = 255
        textSize = 36F
    }

    private val no_contentTextPaint = Paint().apply {
        color = Color.RED
        alpha = 255
        textSize = 36F
    }
    private val preview = pre
    private val inputBitmap = inputBitmap
    private val ctx = ctx
    private val qrContent = qrContent
    private val faceList = faceList
    private val contentPadding = 25
    private var textWidth = yes_contentTextPaint.measureText(qrContent).toInt()


    override fun draw(canvas: Canvas) {
        updateTransformationIfNeeded()

        for (face in faceList) {
            val frameWidth = preview.width

            val frameHeight = preview.height
            val scaleX = frameWidth/480f
            val scaleY =frameHeight/640f


            if (face != null) {


                // Draws a circle at the position of the detected face, with the face's track id below.
                val x = translateX(face.boundingBox.centerX().toFloat())
                val y = translateY(face.boundingBox.centerY().toFloat())
                // Calculate positions.
                val left = x - scale(face.boundingBox.width() / 2.0f)
                val top = y - scale(face.boundingBox.height() / 2.0f)
                val right = x + scale(face.boundingBox.width() / 2.0f)
                val bottom = y + scale(face.boundingBox.height() / 2.0f)


//
//                val left = face.boundingBox.left.toFloat()
//                val top = face.boundingBox.top.toFloat()
//                val right = face.boundingBox.right.toFloat()
//                val bottom = face.boundingBox.bottom.toFloat()
//                val newLeft = face.boundingBox.left * scaleX.toInt()
//                val newBottom = (face.boundingBox.bottom + contentPadding / 2) * scaleY.toInt()
//                val newRight =
//                    (face.boundingBox.left + textWidth + contentPadding * 2) * scaleX.toInt()
//                val newTop =
//                    (face.boundingBox.bottom + contentTextPaint.textSize.toInt() + contentPadding) * scaleY.toInt()
                val scaledBoundingBox = Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())

                try{
                    val bitmapCropped = Bitmap.createBitmap(
                        inputBitmap,
                        face.boundingBox.left,
                        face.boundingBox.top,
                        face.boundingBox.width(),
                        face.boundingBox.height()
                    )
                    val label = predict(ctx, bitmapCropped)
                    val with = label[LABEL_MASK] ?: 0F
                    val without = label[LABEL_NO_MASK] ?: 0F
//                    canvas.drawRect(
//                        Rect(
//                            scaledBoundingBox.left,
//                            scaledBoundingBox.bottom + contentPadding / 2,
//                            scaledBoundingBox.left + textWidth + contentPadding * 2,
//                            scaledBoundingBox.bottom + yes_contentTextPaint.textSize.toInt() + contentPadding
//                        ),
//                        contentRectPaint
//                    )


                    if (with < without) {
                        canvas.drawRect(scaledBoundingBox, no_boundingRectPaint)
                        canvas.drawText(
                            "未佩戴口罩,概率:"+without*100,
                            (scaledBoundingBox.left + contentPadding).toFloat(),
                            (scaledBoundingBox.bottom + contentPadding * 2).toFloat(),
                            yes_contentTextPaint
                        )
                    }else{
                        canvas.drawRect(scaledBoundingBox, yes_boundingRectPaint)
                        canvas.drawText(
                            "佩戴口罩,概率:"+with*100,
                            (scaledBoundingBox.left + contentPadding).toFloat(),
                            (scaledBoundingBox.bottom + contentPadding * 2).toFloat(),
                            no_contentTextPaint
                        )
                    }
                }catch (exc: Exception){
                    Log.e(MaskMainActivity.TAG, "Use case binding failed", exc)
                }



            }

        }
    }



    private fun predict(ctx:Context,input: Bitmap): MutableMap<String, Float> {





        val cropSize = kotlin.math.min(input.width, input.height)
        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeWithCropOrPadOp(cropSize, cropSize))
            .add(ResizeOp(inputShape[1], inputShape[2], ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(NormalizeOp(127.5f, 127.5f))
            .build()

        inputImageBuffer.load(input)
        inputImageBuffer = imageProcessor.process(inputImageBuffer)

        model.run(inputImageBuffer.buffer, outputBuffer.buffer.rewind())


        val labelOutput = TensorLabel(labels, outputBuffer)

        return labelOutput.mapWithFloatValue
    }



    override fun setAlpha(alpha: Int) {
        yes_boundingRectPaint.alpha = alpha
        contentRectPaint.alpha = alpha
        yes_contentTextPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        yes_boundingRectPaint.colorFilter = colorFilter
        contentRectPaint.colorFilter = colorFilter
        yes_contentTextPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return
        }
        val viewAspectRatio: Float = preview.width.toFloat() / preview.height
        val imageAspectRatio: Float = imageWidth.toFloat() / imageHeight
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = preview.width.toFloat() / imageWidth
            postScaleHeightOffset = (preview.width.toFloat() / imageAspectRatio - preview.height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = preview.height.toFloat() / imageHeight
            postScaleWidthOffset = (preview.height.toFloat() * imageAspectRatio - preview.width) / 2
        }

        transformationMatrix.reset()
        transformationMatrix.setScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)
        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, preview.width / 2f, preview.height/ 2f)
        }
        needUpdateTransformation = false
    }

    fun translateX(x: Float): Float {
        return scale(x) - postScaleWidthOffset
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    fun translateY(y: Float): Float {
        return scale(y) - postScaleHeightOffset
    }

    fun scale(imagePixel: Float): Float {
        return imagePixel * scaleFactor
    }
}