package com.james.litepermission

import android.content.Intent
import android.os.Build
import androidx.activity.result.ActivityResult
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.james.litepermission.interceptor.PermissionRequestInterceptor

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:
 */
class RealPermissionRequestChain(
    private var activity: FragmentActivity?,
    private var fragmentManager: FragmentManager,
    private var interceptors: ArrayList<PermissionRequestInterceptor>,//拦截器，包含默认实现和外部自定义实现。
    private var runTimePermissionList: LinkedHashSet<String>,//运行时权限集合
    private var specialPermissionList: LinkedHashSet<String>,//特殊权限集合
    private var callback: PermissionCallback?,//结果回调
) : PermissionRequestInterceptor.Chain {

    @Suppress("PrivatePropertyName")
    private val FRAGMENT_TAG = "PermissionFragment"

    //当前拦截器脚本
    private var interceptorIndex = -1

    /**
     * 所有权限拦截器的权限申请结果集合。
     */
    private val requestPermissionResults = hashMapOf<String, Boolean>()

    //当前正在处理的拦截器。
    private var nowInterceptor: PermissionRequestInterceptor? = null

    private var permissionFragment: PermissionFragment? = null

    private fun getPermissionFragment(): PermissionFragment {
        if (permissionFragment == null) {
            val exitedFragment = fragmentManager.findFragmentByTag(FRAGMENT_TAG)
            permissionFragment = if (exitedFragment != null) {
                val pFragment = exitedFragment as PermissionFragment
                pFragment.setOnRequestPermissionResultListener(null)
                setOnRequestPermissionResultListener(pFragment)
                pFragment
            } else {
                val pFragment = PermissionFragment()
                setOnRequestPermissionResultListener(pFragment)
                fragmentManager.beginTransaction()
                    .add(pFragment, FRAGMENT_TAG)
                    .commitNowAllowingStateLoss()
                pFragment
            }
        }
        return permissionFragment!!
    }

    private fun setOnRequestPermissionResultListener(permissionFragment: PermissionFragment) {
        permissionFragment.setOnRequestPermissionResultListener(object : PermissionFragment.ActivityResultCallback {

            override fun onActivityResult(activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
                nowInterceptor?.onInterceptResult(this@RealPermissionRequestChain, activityResult, permissionResult)
            }

            override fun onDestroy() {
                clear()
            }
        })
    }

    /**
     * 清除对象实例。
     */
    override fun clear() {
        try {
            if (permissionFragment != null) {
                fragmentManager.beginTransaction().remove(permissionFragment!!).commitNowAllowingStateLoss()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        callback = null
        activity = null
        permissionFragment = null
    }

    override fun process() {
        interceptorIndex++
        if (interceptorIndex >= interceptors.size) {
            val grantedList = arrayListOf<String>()
            val deniedList = arrayListOf<String>()
            val doNotAskAgainList = arrayListOf<String>()
            val iterator = requestPermissionResults.iterator()
            var isAllGranted = true
            while (iterator.hasNext()) {
                val entity = iterator.next()
                val isGranted = entity.value //权限是否允许；true.允许
                val permission = entity.key
                if (isGranted) {
                    grantedList.add(permission)
                } else {
                    isAllGranted = false
                    deniedList.add(permission)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && runTimePermissionList.contains(permission)) {
                        val isDeniedForever = activity?.shouldShowRequestPermissionRationale(permission)
                        if (isDeniedForever == false) {//不在询问，永久拒绝
                            doNotAskAgainList.add(permission)
                        }
                    }
                }
            }
            callback?.onResult(isAllGranted, grantedList, deniedList, doNotAskAgainList)
            return
        }
        nowInterceptor = interceptors[interceptorIndex]
        nowInterceptor?.intercept(this)
    }

    override fun getSpecialPermissions(): LinkedHashSet<String> {
        return specialPermissionList
    }

    override fun getRunTimePermissions(): LinkedHashSet<String> {
        return runTimePermissionList
    }


    override fun handleInterceptResultPermission(permissionResult: Map<String, Boolean>?) {
        requestPermissionResults.putAll(permissionResult ?: hashMapOf())
        process()
    }

    override fun requestPermission(permissions: List<String>) {
        getPermissionFragment().requestPermission(permissions)
    }

    override fun requestPermission(intent: Intent) {
        getPermissionFragment().requestPermission(intent)
    }
}