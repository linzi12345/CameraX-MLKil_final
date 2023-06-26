package com.example.camerax_mlkit

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.camerax_mlkit.databinding.ActivityEyeMainBinding
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class eyeMainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityEyeMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var faceDetector: FaceDetector


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityEyeMainBinding.inflate(layoutInflater)
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        val cameraController = LifecycleCameraController(baseContext)
        val previewView: PreviewView = viewBinding.viewFinder
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
        val cameraSelector =
            if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA))
                CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA


        val options = FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build();
        faceDetector = FaceDetection.getClient(options);
        val analyzer = MlKitAnalyzer(
            listOf(faceDetector),
            ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888,
            ContextCompat.getMainExecutor(this)
        ) { result: MlKitAnalyzer.Result? ->

            val barcodeResults = result?.getValue(faceDetector)
            if ((barcodeResults == null) ||
                (barcodeResults.size == 0) ||
                (barcodeResults.first() == null)
            ) {
                previewView.overlay.clear()
                previewView.setOnTouchListener { _, _ -> false } //no-op
                return@MlKitAnalyzer
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // 设置图像捕获监听器
            val imageCapture = ImageCapture.Builder().build()
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val buffer = image.planes[0].buffer
                    val data = ByteArray(buffer.remaining())
                    buffer.get(data)
                    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

                    // 在这里使用获取到的bitmap进行处理

                    // 记得关闭ImageProxy
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    // 处理错误
                }
            })

            cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.IMAGE_ANALYSIS)


            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this)) { image ->
//                val bitmap = image.image?.let { toBitmap(it) }

//                    val label = bitmap?.let { predict(it) }
                //                val qrCodeViewModel = QrCodeViewModel(i, "")



                // 在这里可以对位图进行进一步的处理，例如显示到 ImageView 等操作
                // ...

                image.close()
            }
//            val bitmap=takePicture(cameraController)
            val qrCodeDrawable =
                eyeDrawable(barcodeResults,)

//                    previewView.setOnTouchListener(qrCodeViewModel.qrCodeTouchCallback)
            previewView.overlay.clear()
            qrCodeDrawable?.let { previewView.overlay.add(it) }
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)



        }



        cameraController.setImageAnalysisAnalyzer(ContextCompat.getMainExecutor(this),analyzer)



        cameraController.bindToLifecycle(this)

        previewView.controller = cameraController
    }

    private fun takePicture(cameraController: LifecycleCameraController): Bitmap? {
        var bitmap: Bitmap? = null
        cameraController.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {


                override fun onError(exc: ImageCaptureException) {
                    Log.d("OnImageSavedCallback", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    bitmap = image.image?.let { jpegtoBitmap(it) }
                }

            })
        return bitmap

    }

    private fun jpegtoBitmap(image: Image): Bitmap? {
        val ll=image.getFormat()
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer[bytes]
        val bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        return bitmapImage

    }




    private fun toBitmap(image: Image): Bitmap? {
        val ll=image.getFormat()




        // 获取YUV图像数据
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

        private const val REQUEST_CODE_PERMISSIONS = 10



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

