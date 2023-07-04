package com.hilvano.latocounter

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.hilvano.latocounter.databinding.ActivityPermissionsBinding

class PermissionsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionsBinding
    private val PERMISSION_REQUEST_CODE = 1001
    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPermissions()
        setupListeners()
    }

    private fun setupListeners() {
        binding.allowCamera.setOnClickListener {
            if (isCameraPermissionGranted()) {
                Toast.makeText(this, "Camera permission already granted.", Toast.LENGTH_SHORT).show()
            } else {
                requestCameraPermission()
            }
        }

        binding.allowAudio.setOnClickListener {
            if (isAudioPermissionGranted()) {
                Toast.makeText(this, "Audio permission already granted.", Toast.LENGTH_SHORT).show()
            } else {
                requestAudioPermission()
            }
        }

        binding.allowStorage.setOnClickListener {
            if (isStoragePermissionGranted()) {
                Toast.makeText(this, "Storage permission already granted.", Toast.LENGTH_SHORT).show()
            } else {
                requestStoragePermission()
            }
        }

        binding.btnContinue.setOnClickListener {
            if (checkPermissions()) {
                // Permissions granted, proceed with your app logic
                startMainActivity()
            } else {
                // Permissions not granted, show a toast or handle accordingly
                Toast.makeText(this, "Please grant all the required permissions.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun isAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    private fun isStoragePermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun checkPermissions(): Boolean {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission)
            }
        }

        return permissionsToRequest.isEmpty()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}