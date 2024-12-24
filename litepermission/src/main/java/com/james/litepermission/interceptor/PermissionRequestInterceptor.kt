package com.james.litepermission.interceptor

import android.content.Intent
import androidx.activity.result.ActivityResult

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:权限处理拦截器。
 */
interface PermissionRequestInterceptor {

    /**
     * 拦截器类型：此方法返回字段，是用来去重的。
     * 场景如：目前已经默认实现了运行时权限和几个特殊的拦截器，如果用户想要自定义实现相关的已拥有的拦截器，
     * 需要将 interceptorType 设置成当前默认拦截器中已经默认实现的字段。
     */
    fun interceptorType(): String

    /**
     * 当前拦截器权限申请处理逻辑。
     * 不同的权限申请的逻辑不一样，如部分特殊权限，需要跳转到系统设置页面进行开启。
     * 可以在此方法中加入相关弹框提示。如果需要申请权限请调用[Chain.requestPermission]方法；如果不需要申请权限，调用[Chain.process]执行下一个拦截器
     */
    fun intercept(chain: Chain)

    /**
     * 当前拦截器权限申请的结果。需要将当前拦截器权限申请的结果数据，给到[Chain]汇总统一处理。
     * 使用 [Chain.handleInterceptResultPermission] 方法将拦截器中的权限申请结果数据给到[Chain]进行汇总处理。
     * @return map ：键为当前权限名； 值为当前权限的申请结果。
     */
    fun onInterceptResult(chain: Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?)


    interface Chain {
        /**
         * 执行
         */
        fun process()

        /**
         * 正在请求的特殊权限集合。
         */
        fun getSpecialPermissions(): LinkedHashSet<String>

        /**
         * 正在请求的运行时权限集合。
         */
        fun getRunTimePermissions(): LinkedHashSet<String>

        /**
         * 将拦截器中权限申请结果交给chain统一处理（主要用于汇总所有拦截器的权限处理结果，并将汇总的结果通过回调接口返回），并通知下一个拦截器开始执行。
         * 在 [PermissionRequestInterceptor.onInterceptResult] 方法中权限申请的结果数据，通过[handleInterceptResultPermission]方法给到[Chain]统一处理。
         */
        fun handleInterceptResultPermission(permissionResult: Map<String, Boolean>?)

        /**
         * 申请权限
         * @param permissions 需要申请的权限集合
         */
        fun requestPermission(permissions: List<String>)

        /**
         * 申请权限，此方法主要用来申请部分特殊的权限，部分特殊权限的申请需要跳转到系统设置页面。
         * @param intent 部分特殊权限申请时，需要跳转到的系统设置页面意图。
         */
        fun requestPermission(intent: Intent)

        /**
         * 清除资源
         */
        fun clear()
    }
}