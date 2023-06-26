package com.example.camerax_mlkit


import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.YuvImage
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseIntArray
import android.view.Surface
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax_mlkit.databinding.ActivityMaskMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MaskMainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMaskMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var previewView: PreviewView

    private val ORIENTATIONS = SparseIntArray()

    init {
        ORIENTATIONS.append(Surface.ROTATION_0, 0)
        ORIENTATIONS.append(Surface.ROTATION_90, 90)
        ORIENTATIONS.append(Surface.ROTATION_180, 180)
        ORIENTATIONS.append(Surface.ROTATION_270, 270)
    }

//    private val yuvToRgbConverter = YuvToRgbConverter(this)





    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMaskMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            preview = Preview.Builder()
                .build()
            val cameraController = LifecycleCameraController(baseContext)
            val previewView: PreviewView = viewBinding.viewFinder




            val options = FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build();
            faceDetector = FaceDetection.getClient(options);

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setTargetRotation(Surface.ROTATION_0)
                .setOutputImageRotationEnabled(true)
                .build()
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val cameraSelector =
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                    CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
//                val a=getRotationCompensation()
                var bitmap =  image.image?.let { jpegtoBitmap(it) }
//            val bitmap=BitmapUtils.getBitmap(image)
//            val bitmap=  Bitmap.createBitmap(
//                image.width, image.height, Bitmap.Config.ARGB_8888
//            )
//            image.image?.let { yuvToRgbConverter.yuvToRgb(it, bitmap) }

//            val bitmap= init_bitmap ?.let { rotateBitmap(it) }
                val rotationDegrees: Int = image.getImageInfo().getRotationDegrees()
                val frame= bitmap?.let { InputImage.fromBitmap(it,0) }
                if (image.imageInfo.timestamp % 15 == 0.toLong()) {
                    val faces = frame?.let {
                        faceDetector.process(it).addOnSuccessListener { faces ->
                            if (faces.size == 0) {
                                previewView.overlay.clear()
                            } else {
                                previewView.overlay.clear()
                                val qrCodeDrawable =
                                    bitmap?.let { MaskCodeDrawable(faces, "123", it, this, previewView) }
                                //                    previewView.setOnTouchListener(qrCodeViewModel.qrCodeTouchCallback)
                                previewView.overlay.clear()
                                qrCodeDrawable?.let { previewView.overlay.add(it) }

                            }
                        }
                    }
                }
//            previewView.overlay.clear()





                // 在这里可以对位图进行进一步的处理，例如显示到 ImageView 等操作
                // ...

                image.close()
            }
//            previewView.controller = cameraController



//        cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),analyzer)
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalysis
                )
                preview?.setSurfaceProvider(previewView.surfaceProvider)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))



//        cameraController.bindToLifecycle(this)

    }

    private fun jpegtoBitmap(image: Image): Bitmap? {
        val a=image.getFormat()
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        // 创建一个与图像大小相匹配的 Bitmap 对象
        val bitmap = Bitmap.createBitmap(image.width + rowPadding / pixelStride, image.height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)

        // 如果图像实际大小与 Bitmap 对象的大小不匹配，可以使用以下代码进行裁剪
        val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)

        image.close() // 关闭 Image 对象

        return croppedBitmap

    }


    private fun toBitmap(image: Image): Bitmap? {


        // 获取YUV图像数据
        val a=image.getFormat()
        val planes = image.planes
        val bufferY = planes[0].buffer // Y平面
        val bufferU = planes[1].buffer // U平面
        val bufferV = planes[2].buffer // V平面
        val dataY = ByteArray(bufferY.remaining())
        val dataU = ByteArray(bufferU.remaining())
        val dataV = ByteArray(bufferV.remaining())
        bufferY.get(dataY)
        bufferU.get(dataU)
        bufferV.get(dataV)

        // 创建YUV数据字节数组
        val data = ByteArray(dataY.size + dataU.size + dataV.size)
        System.arraycopy(dataY, 0, data, 0, dataY.size)
        System.arraycopy(dataU, 0, data, dataY.size, dataU.size)
        System.arraycopy(dataV, 0, data, dataY.size + dataU.size, dataV.size)

        // 创建YUVImage对象
        val yuvImage = YuvImage(data, ImageFormat.NV21, image.width, image.height, null)

        // 将YUVImage转换为Bitmap
        val outputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, outputStream)
        val bitmapData = outputStream.toByteArray()
        val bitmap= BitmapFactory.decodeByteArray(bitmapData, 0, bitmapData.size)
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun rotateBitmap(bitmap: Bitmap): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(90f)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }


    @Throws(CameraAccessException::class)
    private fun getRotationCompensation(): Int {
        // Get the device's current rotation relative to its "native" orientation.
        // Then, from the ORIENTATIONS table, look up the angle the image must be
        // rotated to compensate for the device's rotation.
        val deviceRotation = this.windowManager.defaultDisplay.rotation
        var rotationCompensation = ORIENTATIONS.get(deviceRotation)

        // Get the device's sensor orientation.
        val cameraManager = this.getSystemService(CAMERA_SERVICE) as CameraManager

        val cameraIds = cameraManager.cameraIdList
        val sensorOrientation = cameraManager
            .getCameraCharacteristics(cameraIds[0])
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        val facing: Int = cameraManager.getCameraCharacteristics(cameraIds[0]).get(CameraCharacteristics.LENS_FACING)!!

        if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
            rotationCompensation = (sensorOrientation + rotationCompensation) % 360
        } else { // back-facing
            rotationCompensation = (sensorOrientation - rotationCompensation + 360) % 360
        }
        return rotationCompensation
    }


    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
//        barcodeScanner.close()
    }

    companion object {
        const val TAG = "CameraX-MLKit"
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



    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}