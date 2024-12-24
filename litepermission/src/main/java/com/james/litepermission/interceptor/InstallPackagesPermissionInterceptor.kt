package com.james.litepermission.interceptor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.james.litepermission.PermissionUtils

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 * 应用安装权限: 特殊权限，Android 8.0 新增。
 */
class InstallPackagesPermissionInterceptor(var context: Context) : PermissionRequestInterceptor {

    override fun interceptorType(): String {
        return "REQUEST_INSTALL_PACKAGES"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        //特殊权限申请包含安装应用权限
        if (chain.getSpecialPermissions().contains(Manifest.permission.REQUEST_INSTALL_PACKAGES)) {
            if (!canRequestInstallPackagesPermission()) {//系统小于android8 或者 targetSdkVersion小于android8， 不处理,默认有此权限
                onInterceptResult(chain, null, null)
                return
            }
            if (context.packageManager.canRequestPackageInstalls()) {//允许安装
                onInterceptResult(chain, null, null)//已经有安装权限，将结果给到Chain，然后执行下一个拦截器
            } else {
                var intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        val result = hashMapOf<String, Boolean>()
        if (chain.getSpecialPermissions().contains(Manifest.permission.REQUEST_INSTALL_PACKAGES) && canRequestInstallPackagesPermission()) {
            result[Manifest.permission.REQUEST_INSTALL_PACKAGES] = context.packageManager.canRequestPackageInstalls()
        } else {
            result[Manifest.permission.REQUEST_INSTALL_PACKAGES] = true
        }
        chain.handleInterceptResultPermission(result)
    }

    /**
     * 是否可以申请应用安装权限。
     * 应用安装权限是 Android8 新增的，所以在系统版本小于Android8 或者  targetSdkVersion 小于 Android8，都不可以申请该权限，此时默认有此权限。
     * @return true.系统版本和targetSdkVersion满足要求，可以申请该权限。
     */
    private fun canRequestInstallPackagesPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.O
    }
}