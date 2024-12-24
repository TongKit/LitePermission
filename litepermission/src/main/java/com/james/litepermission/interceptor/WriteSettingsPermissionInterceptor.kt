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
 * 系统设置修改权限：特殊权限 ，Android6.0 新增。
 * 当需要修改屏幕亮度等操作时，需要申请此权限。
 */
class WriteSettingsPermissionInterceptor(var context: Context) : PermissionRequestInterceptor {

    override fun interceptorType(): String {
        return "WRITE_SETTINGS"
    }

    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        //特殊权限申请包含系统设置修改权限
        if (chain.getSpecialPermissions().contains(Manifest.permission.WRITE_SETTINGS)) {
            if (!canRequestWriteSettingsWindowPermission()) {//系统小于android6 或者 targetSdkVersion小于android6， 不处理,默认有此权限
                onInterceptResult(chain, null, null)
                return
            }
            if (Settings.System.canWrite(context)) {
                onInterceptResult(chain, null, null)//已经有系统设置修改权限，将结果给到Chain，然后执行下一个拦截器
            } else {
                var intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                if (!PermissionUtils.resolveActivity(context, intent)) {
                    intent = PermissionUtils.getAppDetailIntent(context)
                }
                chain.requestPermission(intent)
            }
        } else {
            //特殊权限申请不包含安装应用权限，执行下一个
            chain.process()
        }

    }

    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        val result = hashMapOf<String, Boolean>()
        if (chain.getSpecialPermissions().contains(Manifest.permission.WRITE_SETTINGS) && canRequestWriteSettingsWindowPermission()) {
            result[Manifest.permission.WRITE_SETTINGS] = Settings.System.canWrite(context)
        } else {
            result[Manifest.permission.WRITE_SETTINGS] = true
        }
        chain.handleInterceptResultPermission(result)
    }

    /**
     * 是否可以申请系统设置修改权限。
     * 系统设置修改权限是 Android6 新增的，所以在系统版本小于Android6 或者  targetSdkVersion 小于 Android6，都不可以申请该权限，此时默认有此权限。
     * @return true.系统版本和targetSdkVersion满足要求，可以申请该权限。
     */
    private fun canRequestWriteSettingsWindowPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.M
    }
}