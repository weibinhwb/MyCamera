package com.weibinhwb.mycamera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val PERMISSIONS_REQUEST_CODE = 10
    private val PERMISSIONS_REQUIRED = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )


    private var mCamera: Camera? = null
    private var mPreview: CameraPreview? = null

    private val mPicture = PictureCallback { data, camera ->
        val pictureFile: File = getOutputMediaFile(MEDIA_TYPE_IMAGE) ?: run {
            Log.d(TAG, ("Error creating media file, check storage permissions"))
            return@PictureCallback
        }

        try {
            val fos = FileOutputStream(pictureFile)
            fos.write(data)
            fos.close()
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "File not found: ${e.message}")
        } catch (e: IOException) {
            Log.d(TAG, "Error accessing file: ${e.message}")
        }
        Log.d("weibin", data.size.toString())
        camera.startPreview()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        if (!hasPermissions()) {
            // Request camera-related permissions
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE)
        }

        checkCameraHardware()
        // Create an instance of Camera
        mCamera = getCameraInstance()

        mPreview = mCamera?.let {
            // Create our Preview view
            CameraPreview(this, it)
        }

        // Set the Preview view as the content of our activity.
        mPreview?.also {
            val preview: FrameLayout = findViewById(R.id.camera_preview)
            preview.addView(it)
        }


//        val captureButton: Button = findViewById(R.id.button_capture)
//        captureButton.setOnClickListener {
//            // get an image from the camera
//            mCamera?.apply {
//                takePicture(null, null, mPicture)
//            }
//        }

        var isRecording = false
        val captureButton: Button = findViewById(R.id.button_capture)
        captureButton.setOnClickListener {
            if (isRecording) {
                // stop recording and release camera
                mediaRecorder?.stop() // stop the recording
                releaseMediaRecorder() // release the MediaRecorder object
                mCamera?.lock() // take camera access back from MediaRecorder

                // inform the user that recording has stopped
                Toast.makeText(this@MainActivity, "Capture", Toast.LENGTH_SHORT).show()
                isRecording = false
            } else {
                // initialize video camera
                if (prepareVideoRecorder()) {
                    // Camera is available and unlocked, MediaRecorder is prepared,
                    // now you can start recording
                    mediaRecorder?.start()

                    // inform the user that recording has started
                    Toast.makeText(this@MainActivity, "Stop", Toast.LENGTH_SHORT).show()
                    isRecording = true
                } else {
                    // prepare didn't work, release the camera
                    releaseMediaRecorder()
                    // inform user
                }
            }
        }

        search_video.setOnClickListener {
            performVideoSearch()
        }

    }

    private fun checkCameraHardware() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "this mobile phone doesn't have a camera!!!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /** A safe way to get an instance of the Camera object. */
    private fun getCameraInstance(): Camera? {
        return try {
            Camera.open() // attempt to get a Camera instance
        } catch (e: Exception) {
            // Camera is not available (in use or does not exist)
            null // returns null if camera is unavailable
        }
    }

//    override fun onPause() {
//        super.onPause()
//        releaseMediaRecorder() // if you are using MediaRecorder, release it first
//        releaseCamera() // release the camera immediately on pause event
//    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.reset() // clear recorder configuration
        mediaRecorder?.release() // release the recorder object
        mediaRecorder = null
        mCamera?.lock() // lock camera for later use
    }

    private fun hasPermissions() = PERMISSIONS_REQUIRED.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun releaseCamera() {
        mCamera?.release() // release the camera for other applications
        mCamera = null
    }

    val MEDIA_TYPE_IMAGE = 1
    val MEDIA_TYPE_VIDEO = 2

    /** Create a file Uri for saving an image or video */
    private fun getOutputMediaFileUri(type: Int): Uri {
        return Uri.fromFile(getOutputMediaFile(type))
    }

    /** Create a File for saving an image or video */
    private fun getOutputMediaFile(type: Int): File? {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        val mediaStorageDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            "MyCameraApp"
        )
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        mediaStorageDir.apply {
            if (!exists()) {
                if (!mkdirs()) {
                    Log.d("MyCameraApp", "failed to create directory")
                    return null
                }
            }
        }

        // Create a media file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return when (type) {
            MEDIA_TYPE_IMAGE -> {
                File("${mediaStorageDir.path}${File.separator}IMG_$timeStamp.jpg")
            }
            MEDIA_TYPE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            MEDIA_TYPE_STORE_VIDEO -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.mp4")
            }
            MEDIA_TYPE_STORE_ACC -> {
                File("${mediaStorageDir.path}${File.separator}VID_$timeStamp.acc")
            }
            else -> null
        }
    }

    val MEDIA_TYPE_STORE_VIDEO = 101
    val MEDIA_TYPE_STORE_ACC = 102

    private var mediaRecorder: MediaRecorder? = null

    private fun prepareVideoRecorder(): Boolean {
        mediaRecorder = MediaRecorder()

        mCamera.let { camera ->
            // Step 1: Unlock and set camera to MediaRecorder
            camera?.unlock()

            mediaRecorder?.run {
                setCamera(camera)

                // Step 2: Set sources
                setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
                setVideoSource(MediaRecorder.VideoSource.CAMERA)

                // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//                setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH))

                // Step 4: Set output file
                setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString())

                // Step 5: Set the preview output
                setPreviewDisplay(mPreview?.holder?.surface)

                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
                setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT)


                // Step 6: Prepare configured MediaRecorder
                return try {
                    prepare()
                    true
                } catch (e: IllegalStateException) {
                    Log.d(TAG, "IllegalStateException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                    false
                } catch (e: IOException) {
                    Log.d(TAG, "IOException preparing MediaRecorder: ${e.message}")
                    releaseMediaRecorder()
                    false
                }
            }

        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseCamera()
        releaseMediaRecorder()
    }

    val VIDEO_RESULT_CODE = 1
    private fun performVideoSearch() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("video/*")
        startActivityForResult(intent, VIDEO_RESULT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == VIDEO_RESULT_CODE && resultCode == Activity.RESULT_OK) {
            val uri: Uri? = data?.data
            extractorVideo(uri!!)
            Log.d(TAG, uri.path)
        }
    }

    private fun extractorVideo(uri: Uri) {
        val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r");
        val fileDescriptor = parcelFileDescriptor?.getFileDescriptor()

        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(fileDescriptor!!)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val trackCount = extractor.trackCount
        Log.d(TAG, "trackCount = $trackCount")
        var videoTrackIndex: Int = -1
        var audioTrackIndex: Int = -1

        for (i in 0 until trackCount) {
            val mediaFormat = extractor.getTrackFormat(i)
            val mime = mediaFormat.getString(MediaFormat.KEY_MIME)
            Log.d(TAG, "mime = $mime")
            if (mime.startsWith("video/")) {
                videoTrackIndex = i
            }
            //音频信道
            if (mime.startsWith("audio/")) {
                audioTrackIndex = i
            }
        }

        Log.d(TAG, "audioTrackIndex = $audioTrackIndex")
        Log.d(TAG, "videoTrackIndex = $videoTrackIndex")

        extractor.selectTrack(videoTrackIndex)
        val inputBuffer = ByteBuffer.allocate(1024 * 1024)
        var video_len = 0

        val movieFile: File = getOutputMediaFile(MEDIA_TYPE_STORE_VIDEO)!!
        var fos = FileOutputStream(movieFile)

        while (true) {
            val readCount = extractor.readSampleData(inputBuffer, 0)
            if (readCount == -1) break
            val buffer = ByteArray(readCount)
            inputBuffer.get(buffer)
            fos.write(buffer)
            fos.flush()
            inputBuffer.clear()
            video_len += readCount
            extractor.advance()
        }
        Log.d(TAG, "video len = $video_len")

        extractor.unselectTrack(videoTrackIndex)
        extractor.selectTrack(audioTrackIndex)
        var audio_len = 0

        val audioFile: File = getOutputMediaFile(MEDIA_TYPE_STORE_ACC)!!
        fos = FileOutputStream(audioFile, true)
        inputBuffer.clear()

        var i = 0
        val sb = StringBuffer()

        while (true) {
            val readCount = extractor.readSampleData(inputBuffer, 0)
            if (readCount == -1) break
            i++

            val aac_audio_buffer = ByteArray(readCount + 7)
            addADTStoPacket(aac_audio_buffer, readCount + 7)
            inputBuffer.get(aac_audio_buffer, 7, readCount)

            fos.write(aac_audio_buffer)
            fos.flush()
            inputBuffer.clear()
            audio_len += readCount
            extractor.advance()
        }

        val fin = FileInputStream(audioFile)
        var len = -1
        sb.delete(0, sb.length)
        val readByte = ByteArray(1024)
        do {
            len = fin.read(readByte)
            readByte.forEach {
                sb.append("$it ")
            }
        } while (len != -1)
        Log.d(TAG, "ans: $sb")
        Log.d(TAG, "i = $i")
        Log.d(TAG, "audio len = $audio_len")
        Log.d(TAG, "len = $audio_len")
        fos.close()
        fin.close()
        parcelFileDescriptor?.close()
        extractor.release()

    }

    private fun addADTStoPacket(packet: ByteArray, packetLen: Int) {
        val profile = 2  //AAC LC
        val freqIdx = 4  //44.1KHz
        val chanCfg = 2  //CPE
        packet[0] = 0xFF.toByte()
        packet[1] = 0xF9.toByte()
        packet[2] = ((profile - 1 shl 6) + (freqIdx shl 2) + (chanCfg shr 2)).toByte()
        packet[3] = ((chanCfg and 3 shl 6) + (packetLen shr 11)).toByte()
        packet[4] = (packetLen and 0x7FF shr 3).toByte()
        packet[5] = ((packetLen and 7 shl 5) + 0x1F).toByte()
        packet[6] = 0xFC.toByte()
        Log.d(TAG, "${packet[0]} ${packet[1]} ${packet[2]} ${packet[3]} ${packet[4]} ${packet[5]} ${packet[6]}")
    }
}

