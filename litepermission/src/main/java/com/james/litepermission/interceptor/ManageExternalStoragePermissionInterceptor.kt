package com.james.litepermission.interceptor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.james.litepermission.PermissionUtils

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 * 文件管理权限: 特殊权限，Android 11 新增。
 * 兼容 Android 11 以下版本，需要在清单文件中注册 [Manifest.permission.READ_EXTERNAL_STORAGE] 和 [Manifest.permission.WRITE_EXTERNAL_STORAGE] 权限
 */
class ManageExternalStoragePermissionInterceptor(var context: Context) : PermissionRequestInterceptor {

    override fun interceptorType(): String {
        return "MANAGE_EXTERNAL_STORAGE"
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        //特殊权限申请包含文件管理权限
        if (chain.getSpecialPermissions().contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE)) {
            if (!canRequestManageExternalStoragePermission()) {//小于android11 不处理,默认有此权限
                onInterceptResult(chain, null, null)
                return
            }
            if (Environment.isExternalStorageManager()) {
                onInterceptResult(chain, null, null)
            } else {
                var intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                if (!PermissionUtils.resolveActivity(context, intent)) {
                    intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                }
                chain.requestPermission(intent)
            }
        } else {
            //特殊权限申请不包含文件管理权限，执行下一个
            chain.process()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        val result = hashMapOf<String, Boolean>()
        if (chain.getSpecialPermissions().contains(Manifest.permission.MANAGE_EXTERNAL_STORAGE) && canRequestManageExternalStoragePermission()) {
            result[Manifest.permission.MANAGE_EXTERNAL_STORAGE] = Environment.isExternalStorageManager()
        } else {
            result[Manifest.permission.MANAGE_EXTERNAL_STORAGE] = true
        }
        chain.handleInterceptResultPermission(result)
    }

    /**
     * 是否可以申请文件管理权限。
     * 文件管理权限是 Android11 新增的，所以在系统版本小于Android11 或者  targetSdkVersion 小于 Android11，都不可以申请该权限，此时默认有此权限。
     * @return true.系统版本和targetSdkVersion满足要求，可以申请该权限。
     */
    private fun canRequestManageExternalStoragePermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R
    }
}