@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package com.james.litepermission

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.james.litepermission.interceptor.*

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 */
class PermissionBuilder {
    private var activity: FragmentActivity? = null
    private var fragment: Fragment? = null

    /**
     * 自定义权限拦截器集合
     */
    private val customPermissionInterceptors = arrayListOf<PermissionRequestInterceptor>()

    /**
     * 请求的特殊权限集合
     */
    private val specialPermissions = LinkedHashSet<String>()

    /**
     * 请求的运行时权限集合
     */
    private val runTimePermissions = LinkedHashSet<String>()


    private var requestChain: PermissionRequestInterceptor.Chain? = null

    private var callback: PermissionCallback? = null

    constructor(activity: FragmentActivity, customPermissionInterceptors: ArrayList<PermissionRequestInterceptor>) {
        this.activity = activity
        this.customPermissionInterceptors.addAll(customPermissionInterceptors)
    }

    constructor(fragment: Fragment, customPermissionInterceptors: ArrayList<PermissionRequestInterceptor>) {
        this.fragment = fragment
        this.activity = fragment.requireActivity()
        this.customPermissionInterceptors.addAll(customPermissionInterceptors)
    }

    private fun getFragmentManager(): FragmentManager {
        return fragment?.childFragmentManager ?: activity!!.supportFragmentManager
    }

    /**
     * 需要请求的权限
     */
    fun permissions(permissions: List<String>): PermissionBuilder {
        for (permission in permissions) {
            if (LitePermission.isSpecialPermission(permission)) {
                specialPermissions.add(permission)
            } else {
                runTimePermissions.add(permission)
            }
        }
        return this
    }

    /**
     * 需要请求的权限
     */
    fun permissions(vararg permissions: String): PermissionBuilder {
        return permissions(listOf(*permissions))
    }

    /**
     * 请求权限。
     * @param callback 请求权限的结果回调。
     */
    fun request(callback: PermissionCallback) {
        this.callback = callback
        requestChain?.clear()
        requestChain = getRequestChain()
        requestChain!!.process()
    }

    private fun getRequestChain(): PermissionRequestInterceptor.Chain {
        val context = fragment?.context ?: activity!!
        val interceptors = arrayListOf<PermissionRequestInterceptor>()
        //添加默认实现的权限拦截器
        interceptors.add(RunTimePermissionInterceptor(context))//运行时权限拦截器
        interceptors.add(ManageExternalStoragePermissionInterceptor(context))
        interceptors.add(InstallPackagesPermissionInterceptor(context))
        interceptors.add(SystemAlertWindowPermissionInterceptor(context))
        interceptors.add(WriteSettingsPermissionInterceptor(context))
        interceptors.add(PackageUsageStatsPermissionInterceptor(context))
        interceptors.add(LocationPermissionInterceptor(context))

        val defaultPermissionsIterator = interceptors.listIterator()
        val customPermissionsIterator = customPermissionInterceptors.listIterator()
        while (customPermissionsIterator.hasNext()) {
            val customInterceptor = customPermissionsIterator.next()
            while (defaultPermissionsIterator.hasNext()) {
                val defaultInterceptor = defaultPermissionsIterator.next()
                //如果外部自定义拦截器实现了与默认拦截器相同的功能，则移除默认的实现
                if (defaultInterceptor.interceptorType() == customInterceptor.interceptorType()) {
                    defaultPermissionsIterator.remove()
                }
            }
        }
        interceptors.addAll(customPermissionInterceptors)
        return RealPermissionRequestChain(
            activity,
            getFragmentManager(),
            interceptors,
            runTimePermissions,
            specialPermissions,
            callback
        )
    }
}