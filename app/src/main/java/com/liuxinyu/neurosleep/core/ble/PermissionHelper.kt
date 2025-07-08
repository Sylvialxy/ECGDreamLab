package com.liuxinyu.neurosleep.core.ble

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class PermissionHelper(private val activity: AppCompatActivity) {
    private val requestPermissionsLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { handlePermissionsResult(it) }

    fun checkAndRequestPermissions() {
        val requiredPermissions = arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_ADVERTISE,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (!hasAllPermissions(requiredPermissions)) {
            requestPermissionsLauncher.launch(requiredPermissions)
        } else {
            onAllPermissionsGranted?.invoke()
        }
    }

    private fun hasAllPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    var onAllPermissionsGranted: (() -> Unit)? = null
    var onPermissionsDenied: ((deniedPermissions: List<String>) -> Unit)? = null

    private fun handlePermissionsResult(result: Map<String, Boolean>) {
        val granted = result.filterValues { it }.keys
        val denied = result.filterValues { !it }.keys

        if (denied.isEmpty()) {
            onAllPermissionsGranted?.invoke()
        } else {
            onPermissionsDenied?.invoke(denied.toList())
        }
    }
}