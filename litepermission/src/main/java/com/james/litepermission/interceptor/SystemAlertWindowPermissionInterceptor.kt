package com.james.litepermission.interceptor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import com.james.litepermission.PermissionUtils

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 * 悬浮窗权限: 特殊权限，Android 6.0 新增。
 * 在 Android 10 及之前的版本能跳转到应用悬浮窗设置页面，在 Android 11 及之后的版本只能跳转到系统设置悬浮窗管理列表。
 * 官方文档：https://developer.android.google.cn/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION
 */
class SystemAlertWindowPermissionInterceptor(var context: Context) : PermissionRequestInterceptor {
    override fun interceptorType(): String {
        return "SYSTEM_ALERT_WINDOW"
    }

    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        //特殊权限申请包含悬浮窗权限
        if (chain.getSpecialPermissions().contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {

            if (!canRequestSystemAlertWindowPermission()) {//小于android6 不处理,默认有此权限
                onInterceptResult(chain, null, null)
                return
            }

            if (Settings.canDrawOverlays(context)) {
                onInterceptResult(chain, null, null)
            } else {
                var intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                if (!PermissionUtils.resolveActivity(context, intent)) {
                    intent = PermissionUtils.getAppDetailIntent(context)
                }
                chain.requestPermission(intent)
            }
        } else {
            chain.process()
        }
    }

    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        val result = hashMapOf<String, Boolean>()
        if (chain.getSpecialPermissions().contains(Manifest.permission.SYSTEM_ALERT_WINDOW) && canRequestSystemAlertWindowPermission()) {
            result[Manifest.permission.SYSTEM_ALERT_WINDOW] = Settings.canDrawOverlays(context)
        } else {
            result[Manifest.permission.SYSTEM_ALERT_WINDOW] = true
        }
        chain.handleInterceptResultPermission(result)
    }

    /**
     * 是否可以申请悬浮窗权限。
     * 悬浮窗权限是 Android6 新增的，所以在系统版本小于Android6 或者  targetSdkVersion 小于 Android6，都不可以申请该权限，此时默认有此权限。
     * @return true.系统版本和targetSdkVersion满足要求，可以申请该权限。
     */
    private fun canRequestSystemAlertWindowPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.M
    }
}