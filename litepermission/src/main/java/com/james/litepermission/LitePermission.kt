@file:Suppress("unused")

package com.james.litepermission

import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.james.litepermission.interceptor.PermissionRequestInterceptor

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description: 权限管理类。
 *
 * 使用实例代码如下：
 * LitePermission.builder(this)
 *                  .permissions(Manifest.permission.CAMERA)
 *                  .request(object : PermissionCallback {
 *                      @Override
 *                      public void onResult(allGranted: Boolean, grantedList: List<String>, deniedList: List<String>, doNotAskAgainList: List<String>) {
 *
 *                      }
 *                  })
 */
object LitePermission {

    //特殊权限的集合
    private val specialPermissions = arrayListOf(
        "android.permission.SYSTEM_ALERT_WINDOW",//允许应用在其他应用之上显示内容，例如悬浮窗
        "android.permission.WRITE_SETTINGS",//允许应用修改系统设置，例如改变屏幕亮度、音量等
        "android.permission.MANAGE_EXTERNAL_STORAGE",//允许应用访问设备上的所有文件，而不仅限于媒体文件
        "android.permission.SCHEDULE_EXACT_ALARM",//允许应用设置精确的闹钟，以确保在特定时间点触发操作
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",//允许应用忽略系统的电池优化策略，以确保在后台持续运行。
        "android.permission.REQUEST_INSTALL_PACKAGES",//允许应用安装来自未知来源的 APK 文件
        "android.permission.BIND_ACCESSIBILITY_SERVICE",//允许应用提供无障碍服务，帮助有特殊需求的用户使用设备
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",//允许应用访问设备的通知，以便进行相关处理
        "android.permission.BIND_VPN_SERVICE",//允许应用提供虚拟专用网络（VPN）服务
        "android.permission.PACKAGE_USAGE_STATS",//允许应用获取其他应用的使用情况统计信息
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_COARSE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",//定位权限需要开启定位服务，所以将定位权限也放到特殊权限中了。
    )

    //自定义权限拦截器
    private val customPermissionInterceptors = arrayListOf<PermissionRequestInterceptor>()

    /**
     * 添加自定义的权限拦截器
     */
    @JvmStatic
    fun addPermissionInterceptor(permissionInterceptor: PermissionRequestInterceptor) {
        val listIterator = customPermissionInterceptors.listIterator()
        while (listIterator.hasNext()) {
            val interceptor = listIterator.next()
            if (interceptor.interceptorType() == permissionInterceptor.interceptorType()) {//移除相同功能类型的拦截器
                listIterator.remove()
            }
        }
        customPermissionInterceptors.add(permissionInterceptor)
    }

    /**
     * 添加特殊权限。
     * 由于系统更新而框架没有及时更新等原因。如果此时新增了一个特殊权限，需要将这个特殊权限放到[LitePermission.specialPermissions]集合中。
     * 在请求特殊权限时，会根据判断，将此请求的特殊权限放到[PermissionBuilder.specialPermissions]中，便于在特殊权限拦截器中使用。
     */
    fun addSpecialPermission(permission: String) {
        if (!specialPermissions.contains(permission)) {
            specialPermissions.add(permission)
        }
    }

    /**
     * 是否是特殊权限。
     */
    fun isSpecialPermission(permission: String): Boolean {
        return specialPermissions.contains(permission)
    }

    @JvmStatic
    fun builder(activity: FragmentActivity): PermissionBuilder {
        return PermissionBuilder(activity, customPermissionInterceptors)
    }

    @JvmStatic
    fun builder(fragment: Fragment): PermissionBuilder {
        return PermissionBuilder(fragment, customPermissionInterceptors)
    }

    @JvmStatic
    fun isGranted(context: Context, permission: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PermissionUtils.checkSelfPermission(context, permission)
        } else {
            true
        }
    }
}