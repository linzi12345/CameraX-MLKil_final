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

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.media.Image
import android.net.Uri
import android.view.MotionEvent
import android.view.View


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
 * A ViewModel for encapsulating the data for a QR Code, including the encoded data, the bounding
 * box, and the touch behavior on the QR Code.
 *
 * As is, this class only handles displaying the QR Code data if it's a URL. Other data types
 * can be handled by adding more cases of Barcode.TYPE_URL in the init block.
 */
class QrCodeViewModel(face:Face,qrContent: String) {


    var boundingRect: Rect = face.boundingBox
    var qrContent: String = qrContent
    var qrCodeTouchCallback = { v: View, e: MotionEvent -> false } //no-op

    companion object {

        private const val LABELS = "labels.txt"
        private const val LABEL_MASK = "mask"
        private const val LABEL_NO_MASK = "no_mask"


        private fun formatString(text: String, float: Float): String =
            "$text: ${String.format("%.1f", float * 100)}%"
    }
}







