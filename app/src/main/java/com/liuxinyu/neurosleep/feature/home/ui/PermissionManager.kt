package com.liuxinyu.neurosleep.feature.home.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class PermissionManager(private val activity: AppCompatActivity) {

    // 定义蓝牙所需权限
    private val bluetoothPermissions = arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    // 权限请求结果回调
    private var permissionCallback: ((Boolean) -> Unit)? = null

    // 初始化权限请求启动器
    private val requestPermissionsLauncher: ActivityResultLauncher<Array<String>> = 
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionsResult ->
            handlePermissionsResult(permissionsResult)
        }

    /**
     * 检查并请求蓝牙相关权限
     * @param callback 权限请求结果回调
     */
    fun checkAndRequestBluetoothPermissions(callback: (Boolean) -> Unit) {
        permissionCallback = callback
        
        // 检查是否已经拥有所有权限
        val allPermissionsGranted = bluetoothPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }

        if (allPermissionsGranted) {
            // 所有权限已经授予
            permissionCallback?.invoke(true)
        } else {
            // 请求权限
            requestPermissionsLauncher.launch(bluetoothPermissions)
        }
    }

    /**
     * 处理权限请求结果
     */
    private fun handlePermissionsResult(permissionsResult: Map<String, Boolean>) {
        val allPermissionsGranted = permissionsResult.all { it.value }
        
        if (allPermissionsGranted) {
            // 所有权限已经授予
            Toast.makeText(activity, "所有权限都已授予", Toast.LENGTH_SHORT).show()
            permissionCallback?.invoke(true)
        } else {
            // 部分或全部权限被拒绝
            Toast.makeText(activity, "部分权限未授予，蓝牙功能可能受限", Toast.LENGTH_SHORT).show()
            permissionCallback?.invoke(false)
        }
    }

    /**
     * 检查是否拥有蓝牙权限
     */
    fun hasBluetoothPermissions(): Boolean {
        return bluetoothPermissions.all { permission ->
            ActivityCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
} 