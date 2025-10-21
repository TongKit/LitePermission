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

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        LitePermission.addPermissionInterceptor(CustomManageExternalStoragePermissionInterceptor(this))
        LitePermission.addPermissionInterceptor(CustomManageExternalStoragePermissionInterceptor(this))
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<Button>(R.id.btCamera).setOnClickListener {
            LitePermission.builder(this)
                .permissions(Manifest.permission.CAMERA)
                .request {
                    Log.d(TAG, "allGranted = ${it.allGranted}")
                }
        }
        findViewById<Button>(R.id.btAllFile).setOnClickListener {
            LitePermission.builder(this)
                .permissions(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                .request {
                    Log.d(TAG, "allGranted = ${it.allGranted}")
                }
        }

        findViewById<Button>(R.id.btLocation).setOnClickListener {
            LitePermission.builder(this)
                .permissions(
                    Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )
                .request {
                    Log.d(TAG, "allGranted = ${it.allGranted}")
                }
        }

        findViewById<Button>(R.id.btJava).setOnClickListener {
            TestPermissionRequest().request(this)
        }

    }
}