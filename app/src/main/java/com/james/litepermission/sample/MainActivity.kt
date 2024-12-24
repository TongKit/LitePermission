package com.james.litepermission.sample

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.james.litepermission.LitePermission
import com.james.litepermission.PermissionCallback

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.btCamera).setOnClickListener {
            LitePermission.builder(this).permissions(Manifest.permission.CAMERA).request(object : PermissionCallback {
                    override fun onResult(allGranted: Boolean, grantedList: List<String>, deniedList: List<String>, doNotAskAgainList: List<String>) {
                        Log.d(TAG, "allGranted = $allGranted")
                    }
                })
        }
        findViewById<Button>(R.id.btAllFile).setOnClickListener {
            LitePermission.builder(this).permissions(Manifest.permission.MANAGE_EXTERNAL_STORAGE).request(object : PermissionCallback {
                    override fun onResult(allGranted: Boolean, grantedList: List<String>, deniedList: List<String>, doNotAskAgainList: List<String>) {
                        Log.d(TAG, "allGranted = $allGranted")
                    }
                })
        }
    }
}