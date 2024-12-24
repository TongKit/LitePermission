package com.james.litepermission.interceptor

import android.content.Context
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.annotation.RequiresApi
import com.james.litepermission.PermissionUtils


/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 * 运行时权限,正常申请，不需要跳转到系统设置页。
 */
class RunTimePermissionInterceptor(var context: Context) : PermissionRequestInterceptor {

    override fun interceptorType(): String {
        return "RunTimePermission"
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        val needRequestPermission = arrayListOf<String>()
        val requestRunTimePermission = chain.getRunTimePermissions()
        for (permissoin in requestRunTimePermission) {
            if (!PermissionUtils.checkSelfPermission(context, permissoin)) {
                needRequestPermission.add(permissoin)
            }
        }
        if (needRequestPermission.isEmpty()) {//权限都已授权。
            val permissionResult = hashMapOf<String, Boolean>()
            requestRunTimePermission.forEach { permission ->
                permissionResult[permission] = true
            }
            onInterceptResult(chain, null, permissionResult)//此处将所有授权的权限交给Chain统一处理。
        } else {//没有都授权，需要请求授权逻辑。
            chain.requestPermission(requestRunTimePermission.toMutableList())
        }
    }

    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        val requestRunTimePermission = chain.getRunTimePermissions()
        if (requestRunTimePermission.size != permissionResult?.size) {//申请的权限列表和返回的权限列表不一样：此处表示申请的多个权限中，已有权限在之前已被授权。
            requestRunTimePermission.forEach { permission ->
                if (permissionResult?.contains(permission) == false) {
                    if (permissionResult is MutableMap) {
                        permissionResult.put(permission, true)//将之前已被授权的权限放到结果中，进行统一处理并返回。
                    }
                }
            }
        }
        chain.handleInterceptResultPermission(permissionResult ?: hashMapOf())
    }
}