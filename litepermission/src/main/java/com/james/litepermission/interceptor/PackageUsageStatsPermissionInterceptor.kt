package com.james.litepermission.interceptor

import android.Manifest
import android.app.AppOpsManager
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
 * @Description:应用使用情况权限: 特殊权限，Android 6 新增。
 * 如需要获取最近应用使用的列表，就需要申请该权限，否则获取到的结果是空。
 */
class PackageUsageStatsPermissionInterceptor(var context: Context) : PermissionRequestInterceptor {
    override fun interceptorType(): String {
        return "PACKAGE_USAGE_STATS"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        //特殊权限申请包含应用使用情况权限
        if (chain.getSpecialPermissions().contains(Manifest.permission.PACKAGE_USAGE_STATS)) {
            if (!canRequestPackageUsageStatsPermission()) {//小于android6 不处理,默认有此权限
                onInterceptResult(chain, null, null)
                return
            }
            if (isGrantedPackageUsageStatsPermission()) {
                onInterceptResult(chain, null, null)
            } else {
                val intent = getPackageUsageStatsPermissionIntent()
                chain.requestPermission(intent)
            }
        } else {
            //特殊权限申请不包含应用使用情况权限，执行下一个
            chain.process()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        val result = hashMapOf<String, Boolean>()
        if (chain.getSpecialPermissions().contains(Manifest.permission.PACKAGE_USAGE_STATS) && canRequestPackageUsageStatsPermission()) {
            result[Manifest.permission.PACKAGE_USAGE_STATS] = isGrantedPackageUsageStatsPermission()
        } else {
            result[Manifest.permission.PACKAGE_USAGE_STATS] = true
        }
        chain.handleInterceptResultPermission(result)
    }

    /**
     *获取跳转到使用统计权限设置界面的意图
     */
    private fun getPackageUsageStatsPermissionIntent(): Intent {
        var intent: Intent? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {//兼容Android10.在Android10以上需要加上包名；Android10 以下不能加上包名。
                intent.data = Uri.parse("package:${context.packageName}")
            }
        }
        if (intent == null || !PermissionUtils.resolveActivity(context, intent)) {
            intent = PermissionUtils.getAppDetailIntent(context)
        }
        return intent
    }

    /**
     * 应用使用情况权限 是否被允许申请。
     * @return true.表示允许；false.表示被拒绝
     */
    @Suppress("DEPRECATION")
    private fun isGrantedPackageUsageStatsPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, context.applicationInfo.uid, context.packageName)
            } else {
                appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, context.applicationInfo.uid, context.packageName)
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return true
    }

    /**
     * 是否可以申请应用使用情况权限。
     * 应用使用情况权限是 Android6 新增的，所以在系统版本小于Android6 或者  targetSdkVersion 小于 Android6，都不可以申请该权限，此时默认有此权限。
     * @return true.系统版本和targetSdkVersion满足要求，可以申请该权限。
     */
    private fun canRequestPackageUsageStatsPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.M
    }
}