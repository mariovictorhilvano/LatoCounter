package com.hilvano.latocounter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.view.Surface
import android.view.TextureView
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.hilvano.latocounter.databinding.ActivityMainBinding
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val CAMERA_PERMISSION_REQUEST_CODE = 1001
    private val VIDEO_FILE_NAME = "video.mp4"
    private val AUDIO_FILE_NAME = "audio.3gp"
    private lateinit var textureView: TextureView
    private lateinit var recordButton: ImageButton
    private lateinit var stopButton: ImageButton
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraCaptureSession: CameraCaptureSession
    private lateinit var mediaRecorder: MediaRecorder
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var recordingTimer: CountDownTimer
    private var isRecording: Boolean = false
    private var isFrontCamera = true
    private var videoFilePath: String? = null
    private var audioFilePath: String? = null
    private var elapsedTime = 0L
    private var toggle35000: Boolean = false
    private var toggle60000: Boolean = false
    private var toggle75000: Boolean = false
    private var sensitivity: Int = 35000
    private var clickCount = 0
    private lateinit var tvClicks: TextView
    private lateinit var context: Context
    private var selectedItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        context = applicationContext

        textureView = findViewById(R.id.textureView)

        recordButton = binding.btnRecord
        stopButton = binding.btnStop

        mediaRecorder = MediaRecorder()

        tvClicks = findViewById<TextView>(R.id.tvClicks)
        val tv35000 = findViewById<TextView>(R.id.tv35000)
        val tv60000 = findViewById<TextView>(R.id.tv60000)
        val tv75000 = findViewById<TextView>(R.id.tv75000)

        toggle35000 = true
        toggle60000 = false
        toggle75000 = false

        tv35000.setOnClickListener {
            updateToggleState()
        }

        tv60000.setOnClickListener {
            updateToggleState()
        }

        tv75000.setOnClickListener {
            updateToggleState()
        }
        tvClicks.setOnClickListener {
            clickCount++
            updateClicksText()
        }
        val seekBar = findViewById<SeekBar>(R.id.sensitivity_slider)
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedItemIndex = progress
                updateToggleTextColors()
                updateClicksText()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // No action needed
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // No action needed
            }
        })

        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        val playButton: ImageButton = findViewById(R.id.btnPlayRecord)
        playButton.setOnClickListener {
            playRecordedVideo()
        }

        val switchButton: ImageButton = findViewById(R.id.btnSwitch)
        switchButton.setOnClickListener { switchCamera() }
        val openFolderButton: ImageButton = findViewById(R.id.btnOpenFolder)
        openFolderButton.setOnClickListener {
            openVideoFolder()
        }
    }

    private fun updateToggleState() {
        toggle35000 = selectedItemIndex == 0
        toggle60000 = selectedItemIndex == 1
        toggle75000 = selectedItemIndex == 2

        sensitivity = when (selectedItemIndex) {
            0 -> 35000
            1 -> 60000
            2 -> 75000
            else -> 0
        }

        updateClicksText()
        updateToggleTextColors()
    }

    private fun updateClicksText() {
        tvClicks.text = "$clickCount"
    }

    private fun updateToggleTextColors() {
        val tv35000 = binding.tv35000
        val tv60000 = binding.tv60000
        val tv75000 = binding.tv75000

        tv35000.setTextColor(
            if (selectedItemIndex == 0) ContextCompat.getColor(this, R.color.black)
            else ContextCompat.getColor(this, R.color.white)
        )

        tv60000.setTextColor(
            if (selectedItemIndex == 1) ContextCompat.getColor(this, R.color.black)
            else ContextCompat.getColor(this, R.color.white)
        )

        tv75000.setTextColor(
            if (selectedItemIndex == 2) ContextCompat.getColor(this, R.color.black)
            else ContextCompat.getColor(this, R.color.white)
        )
    }

    override fun onResume() {
        super.onResume()
        openCamera()
    }

    override fun onPause() {
        super.onPause()
        closeCamera()
    }

    private fun hasCamera(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    private fun openCamera() {
        if (!hasCamera()) {
            showToast("This device does not have a camera")
            return
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = getCameraId()
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                // Open the camera only if both camera and audio permissions are granted
                cameraManager.openCamera(cameraId, cameraStateCallback, null)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.CAMERA,
                        android.Manifest.permission.RECORD_AUDIO
                    ),
                    CAMERA_PERMISSION_REQUEST_CODE
                )
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getCameraId(): String {
        val cameraIds = cameraManager.cameraIdList
        for (id in cameraIds) {
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (isFrontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id
            } else if (!isFrontCamera && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return id
            }
        }
        return cameraIds[0] // Default to the first camera if no matching camera found
    }

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraDevice = camera
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraDevice.close()
        }
    }

    private fun createCaptureSession() {
        val surfaces = arrayListOf<Surface>()
        surfaceTexture = textureView.surfaceTexture!!
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val previewSurface = Surface(surfaceTexture)
        surfaces.add(previewSurface)

        try {
            val captureRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder.addTarget(previewSurface)

            val outputConfigurations = surfaces.map { OutputConfiguration(it) }

            cameraDevice.createCaptureSessionByOutputConfigurations(
                outputConfigurations,
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        startPreview()
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        showToast("Camera configuration failed")
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        surfaceTexture.let {
            it.setDefaultBufferSize(textureView.width, textureView.height)
            val previewSurface = Surface(it)
            surfaces.add(previewSurface)

            try {
                val captureRequestBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequestBuilder.addTarget(previewSurface)

                // ...
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }
    }

    private fun startRecording() {
        if (isRecording) {
            return
        }

        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                CAMERA_PERMISSION_REQUEST_CODE
            )
            return
        }

        try {
            mediaRecorder = MediaRecorder()
            setUpMediaRecorder()
            val recordingSurface = mediaRecorder.surface
            val captureBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
            captureBuilder.addTarget(recordingSurface)
            val previewSurface = Surface(surfaceTexture)
            captureBuilder.addTarget(previewSurface)

            cameraCaptureSession.stopRepeating()
            cameraCaptureSession.abortCaptures()
            cameraCaptureSession.setRepeatingRequest(
                captureBuilder.build(),
                null,
                null
            )

            videoFilePath = getOutputFilePath(VIDEO_FILE_NAME)
            audioFilePath = getOutputFilePath(AUDIO_FILE_NAME)
            mediaRecorder.setOutputFile(videoFilePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            startRecordingTimer()
            updateRecordButtonState(true)
            isRecording = true
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    private fun setUpMediaRecorder() {
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setVideoEncodingBitRate(10000000)
        mediaRecorder.setVideoFrameRate(30)
        mediaRecorder.setVideoSize(textureView.width, textureView.height)

        // Set the preview surface as the input surface for the MediaRecorder
        mediaRecorder.setPreviewDisplay(Surface(surfaceTexture))

        // Set the audio source and encoder separately if needed
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        // Set the output file path
        videoFilePath = getOutputFilePath(VIDEO_FILE_NAME)
        mediaRecorder.setOutputFile(videoFilePath)
    }

    private fun startRecordingTimer() {
        recordingTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                elapsedTime += 1000
                updateDurationTextView()
            }

            override fun onFinish() {
                // Do nothing
            }
        }
        recordingTimer.start()
    }

    private fun stopRecording() {
        if (!isRecording) {
            return
        }

        try {
            mediaRecorder.stop()
            mediaRecorder.reset()
            mediaRecorder.release()
            recordingTimer.cancel()
            elapsedTime = 0
            updateDurationTextView()
            showToast("Recording saved: $videoFilePath")
            updateRecordButtonState(false)
            isRecording = false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cameraCaptureSession.close()
        startPreview()
    }

    private fun startPreview() {
        try {
            val previewRequestBuilder =
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val previewSurface = Surface(surfaceTexture)
            previewRequestBuilder.addTarget(previewSurface)
            cameraCaptureSession.setRepeatingRequest(
                previewRequestBuilder.build(),
                null,
                null
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun updateRecordButtonState(isRecording: Boolean) {
        if (isRecording) {
            recordButton.setImageResource(R.drawable.ic_stop_button)
        } else {
            recordButton.setImageResource(R.drawable.ic_rec_button)
        }
    }

    private fun updateDurationTextView() {

        val seconds = elapsedTime / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        val durationText = String.format("%02d:%02d", minutes, remainingSeconds)
        val tvDuration = findViewById<TextView>(R.id.tvDuration)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun getOutputFilePath(fileName: String): String {
        val directory = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val file = File(directory, fileName)
        return file.absolutePath
    }

    private fun playRecordedVideo() {
        videoFilePath?.let { path ->
            val videoUri = FileProvider.getUriForFile(
                this,
                "com.hilvano.latocounter.fileprovider",
                File(path)
            )
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(videoUri, "video/mp4")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        }
    }

    private fun switchCamera() {
        isFrontCamera = !isFrontCamera
        closeCamera()
        openCamera()
    }

    private fun closeCamera() {
        if (isRecording) {
            stopRecording()
        }
        if (::cameraCaptureSession.isInitialized) {
            cameraCaptureSession.close()
        }
        if (::cameraDevice.isInitialized) {
            cameraDevice.close()
        }
        if (::mediaRecorder.isInitialized) {
            mediaRecorder.release()
        }
    }

    private fun openVideoFolder() {
        val directory = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.setDataAndType(Uri.parse(directory?.path), "*/*")
        startActivity(intent)
    }
}