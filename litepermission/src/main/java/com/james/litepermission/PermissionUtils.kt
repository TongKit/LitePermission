package com.james.litepermission

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 */
object PermissionUtils {

    /**
     * 检查权限是否被授予
     */
    @TargetApi(value = Build.VERSION_CODES.M)
    fun checkSelfPermission(context: Context, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * 判断这个意图的 Activity 是否存在。
     * @return true.这个Activity存在；false.这个Activity不存在
     */
    @SuppressLint("QueryPermissionsNeeded")
    fun resolveActivity(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }

    /**
     * 获取应用详情的意图
     */
    fun getAppDetailIntent(context: Context): Intent {
        var intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        if (!resolveActivity(context, intent)) {
            intent = Intent(Settings.ACTION_APPLICATION_SETTINGS)
            if (!resolveActivity(context, intent)) {
                intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            }
        }
        return intent
    }

}