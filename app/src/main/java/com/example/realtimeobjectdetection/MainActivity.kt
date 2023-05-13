package com.example.realtimeobjectdetection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.Settings
import android.telephony.SmsManager
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.realtimeobjectdetection.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class MainActivity : ComponentActivity() {

    lateinit var textureView:TextureView
    lateinit var cameraManager: CameraManager
    lateinit var handler: Handler
    lateinit var cameraDevice: CameraDevice
    lateinit var model: SsdMobilenetV11Metadata1
    lateinit var bitmap: Bitmap
    lateinit var imageView: ImageView
    lateinit var imageProcessor: ImageProcessor
    lateinit var labels:List<String>
    lateinit var button: Button
    lateinit var textView: TextView
    lateinit var NotificationChannel:NotificationChannel

    val phoneNumber = "9860937580".toString() // Replace with the phone number you want to send the message to
    var message = "intrusion of unknown person detected".toString() // Replace with the message you want to send
    val startTime = System.currentTimeMillis() // store the start time

    var elapsedTime: Long = 0 // initialize the elapsed time



    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()

    private  val CHANNEL_ID = "one"
    private  val CHANNEL_NAME = "one"
    private  val CHANNEL_DESC = "notifications haru"




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.naya)
        get_permission()
        get_msg()
        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O){
//            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
//            channel.description = CHANNEL_DESC}
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                description = CHANNEL_DESC
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            if (manager.importance < NotificationManager.IMPORTANCE_HIGH) {
                val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID)
                startActivity(intent)
            }
        }
//        displayNotification()

        val smsManager = SmsManager.getDefault()
        model = SsdMobilenetV11Metadata1.newInstance(this)
        imageView = findViewById(R.id.imageView)
        button = findViewById(R.id.btn)
        textView = findViewById(R.id.textView)

        labels = FileUtil.loadLabels(this, "labels.txt")


        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        textureView = findViewById(R.id.textureView)
        cameraManager = getSystemService(Context.CAMERA_SERVICE)as CameraManager
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)


        textureView.surfaceTextureListener=object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                open_camera()
            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            bitmap = textureView.bitmap!!
// Creates inputs for reference.
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)
// Runs model inference and gets result.
                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray
                Log.d("MyApp", "classes[0]: ${outputs}")

                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)

                val h = mutable.height
                val w = mutable.width
                paint.textSize = h/30f
                paint.strokeWidth = h/150f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
//                    if(fl > 0.5){
//                        paint.setColor(colors.get(index))
//                        paint.style = Paint.Style.STROKE
//                        canvas.drawRect(RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
//                        paint.style = Paint.Style.FILL
//                        canvas.drawText(labels.get(classes.get(index).toInt())+" "+fl.toString(), locations.get(x+1)*w, locations.get(x)*h, paint)
//                    }

                    if(labels.get(classes.get(index).toInt())=="dog" && fl>0.4) {

                        var messages = "DOG"
                        displayNotification(messages)
                        updateMessage(messages)}

                    if(labels.get(classes.get(index).toInt())=="person" && fl>0.6){
//                        textView.setText("person detected")
                        var messages = "PERSON"
                        displayNotification(messages)
                        updateMessage(messages)}

                    if(labels.get(classes.get(index).toInt())=="cow" && fl>0.5){
//                        textView.setText("person detected")
                        var messages = "COW"
                        displayNotification(messages)
                        updateMessage(messages)}
//                    else{
////                        textView.setText("person detected")
//                        var messages = "NOTHING TILL NOW"
//                        updateMessage(messages)}




                }




//                        canvas.drawText("alert",locations.get(x+3)*w, locations.get(x)*h, paint)
                      //  smsManager.sendTextMessage(phoneNumber, null, message, null, null)

//                        while ( elapsedTime < 4000) { // check if confidenceFl is greater than 4 and elapsed time is less than 4 seconds
//                            elapsedTime =
//                                System.currentTimeMillis() - startTime // calculate the elapsed time
//                        }
//
//                        if (elapsedTime >= 4000) {
//                            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
//
//                        }

//                    else if(labels.get(classes.get(index).toInt())=="cow"){
//                        textView.setText("Cow intrusion")
//
//                    }




                imageView.setImageBitmap(mutable)



            }

        }
        button.setOnClickListener {
//            displayNotification()
            model.close()

        }


    }



    private fun displayNotification(messages: String) {
        val mBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setContentTitle("INTRUSION")
            .setContentText(messages)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        val notificationManagerCompat = NotificationManagerCompat.from(this)
        notificationManagerCompat.notify(1, mBuilder.build())
    }

    private fun updateMessage(messages: String) {
        textView.setText(messages)
    }
    fun get_msg() {
        ActivityCompat.requestPermissions(
            this@MainActivity,
            arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS),
            PackageManager.PERMISSION_GRANTED
        )
    }




    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }

     fun open_camera() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        cameraManager.openCamera(cameraManager.cameraIdList[0],object :CameraDevice.StateCallback(){
        override fun onOpened(p0: CameraDevice) {
            cameraDevice = p0
            var surfaceTexture = textureView.surfaceTexture
            var surface = Surface(surfaceTexture)
            var captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequest.addTarget(surface)

            cameraDevice.createCaptureSession(listOf(surface), object: CameraCaptureSession.StateCallback(){
                override fun onConfigured(p0: CameraCaptureSession) {
                    p0.setRepeatingRequest(captureRequest.build(), null, null)
                }
                override fun onConfigureFailed(p0: CameraCaptureSession) {
                }
            }, handler)



        }

        override fun onDisconnected(p0: CameraDevice) {
        }

        override fun onError(p0: CameraDevice, p1: Int) {
        }


    }, handler)
    }



    fun get_permission() {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }



}





