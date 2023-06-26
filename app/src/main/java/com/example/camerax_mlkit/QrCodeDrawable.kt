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
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.MotionEvent
import android.view.View
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
class QrCodeDrawable(faceList: MutableList<Face?>) : Drawable() {

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
    private val contentRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GREEN
        alpha = 255
    }


    private val yes_boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.GREEN
        strokeWidth = 5F
        alpha = 200
    }

    private val no_boundingRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 5F
        alpha = 200
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


    private val faceList = faceList
    private val contentPadding = 25
    private var textWidth = yes_contentTextPaint.measureText("qrContent").toInt()


    override fun draw(canvas: Canvas) {

        for (face in faceList) {



            if (face != null) {
                val rect = RectF(face.boundingBox)
                val left = face.boundingBox.left.toFloat()
                val top = face.boundingBox.top.toFloat()
                val right = face.boundingBox.right.toFloat()
                val bottom = face.boundingBox.bottom.toFloat()

                val scaledBoundingBox = Rect(left.toInt(), top.toInt() , right.toInt(), bottom.toInt())
                try{

                    if(face.getSmilingProbability()!! >0.6) {
                        canvas.drawRect(scaledBoundingBox, yes_boundingRectPaint)
                        canvas.drawText(
                            "是否微笑:是，概率:"+face.smilingProbability!!*100,
                            (scaledBoundingBox.left + contentPadding).toFloat(),
                            (scaledBoundingBox.bottom + contentPadding * 2).toFloat(),
                            yes_contentTextPaint
                        )
                    }
                    else{
                        canvas.drawRect(scaledBoundingBox, no_boundingRectPaint)
                        canvas.drawText(
                            "是否微笑:否，概率:"+(1f- face.smilingProbability!!)*100,
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
}