package com.james.litepermission

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 */
class PermissionFragment : Fragment() {

    private var requestPermissions = arrayListOf<String>()
    private var permissionCallback: ActivityResultCallback? = null
    private var isCreate = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCreate = true
    }

    //运行时权限申请。
    private val runTimePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (it.isNotEmpty()) {
            permissionCallback?.onActivityResult(null, it)
        }
    }

    //特殊权限申请。
    private val specialPermissionLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionCallback?.onActivityResult(it, null)
    }

    fun requestPermission(permissions: List<String>) {
        if (isCreate) {
            requestPermissions.clear()
            requestPermissions.addAll(permissions)
            runTimePermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    fun requestPermission(intent: Intent) {
        if (isCreate) {
            specialPermissionLauncher.launch(intent)
        }
    }

    fun setOnRequestPermissionResultListener(permissionCallback: ActivityResultCallback?) {
        this.permissionCallback = permissionCallback
    }

    interface ActivityResultCallback {

        fun onActivityResult(activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?)

        fun onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        permissionCallback?.onDestroy()
        permissionCallback = null
    }
}