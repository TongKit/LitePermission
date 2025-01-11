package com.james.litepermission.interceptor

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResult
import com.james.litepermission.PermissionUtils


/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:定位权限：定位权限是运行时权限。但当用户授权了定位权限之后，如果手机的定位服务没有开启，也是无法获取到定位数据的。需要跳转到系统设置页面打开定位服务，因此将定位权限算作特殊权限。
 * 需要再清单文件中申明一下权限： [Manifest.permission.ACCESS_FINE_LOCATION]、[Manifest.permission.ACCESS_COARSE_LOCATION]、[Manifest.permission.ACCESS_BACKGROUND_LOCATION].
 *
 * 在Android10 之前 ，只需要申请[Manifest.permission.ACCESS_FINE_LOCATION]和[Manifest.permission.ACCESS_COARSE_LOCATION]两个权限即可。申请之后，应用处于前后台都能获取定位信息。
 *
 * 在Android10版本，新增了[Manifest.permission.ACCESS_BACKGROUND_LOCATION]后台定位权限，如果应用处于后台时，依然需要定位，则必须申请后台定位权限。与
 * [Manifest.permission.ACCESS_FINE_LOCATION]和[Manifest.permission.ACCESS_COARSE_LOCATION]两个权限相互独立。可分别单独申请。
 *
 * 在Android10 之后，必须先申请前台权限后，才能申请后台权限。
 *
 * 若定位权限都已授权，但定位服务未开启，此时定位服务未开启的结果会在 回调方法的 deniedList 集合中返回出去。键为 [LOCATION_SERVICE_IS_OPEN]
 */
class LocationPermissionInterceptor(var context: Context) : PermissionRequestInterceptor {
    companion object {
        /**
         * 定位服务是否开启。
         */
        const val LOCATION_SERVICE_IS_OPEN = "LOCATION_SERVICE_IS_OPEN"
    }

    override fun interceptorType(): String {
        return "REQUEST_LOCATION_PERMISSION"
    }

    @SuppressLint("InlinedApi")
    override fun intercept(chain: PermissionRequestInterceptor.Chain) {
        //特殊权限申请包含定位权限
        if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
            chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_COARSE_LOCATION) ||
            chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        ) {
            if (isBelowAndroid6()) {//小于android6 不处理,默认有此权限
                onInterceptResult(chain, null, null)
                return
            }

            if (isBelowAndroid10()) {//大于Android6，小于Android10。ACCESS_BACKGROUND_LOCATION权限是在Android10新增的，如果小于Android10，可以移除这个权限
                chain.getSpecialPermissions().remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            val needRequestPermission = getNeedRequestPermissions(chain)
            if (needRequestPermission.isEmpty()) {//为空，表示都授权了。
                onInterceptResult(chain, null, null)//默认都有此权限。
            } else {
                if (isBelowAndroid10() || isAndroid10()) {
                    chain.requestPermission(needRequestPermission)//小于或等于Android10直接请求权限。
                } else if (isAboveAndroid10()) {
                    //大于Android10时，如果请求了
                    if (needRequestPermission.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        needRequestPermission.remove(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        chain.requestPermission(needRequestPermission)
                    } else {
                        chain.requestPermission(needRequestPermission)
                    }
                }
            }
        } else {
            chain.process()//申请的权限列表中不包含定位权限，则执行下一个。
        }
    }


    @SuppressLint("InlinedApi")
    override fun onInterceptResult(chain: PermissionRequestInterceptor.Chain, activityResult: ActivityResult?, permissionResult: Map<String, Boolean>?) {
        if (isBelowAndroid6()) {//小于android6 不处理,默认有此权限
            val tempPermissionResult = hashMapOf<String, Boolean>()
            if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
                tempPermissionResult[Manifest.permission.ACCESS_FINE_LOCATION] = true
            }
            if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                tempPermissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] = true
            }
            if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                tempPermissionResult[Manifest.permission.ACCESS_BACKGROUND_LOCATION] = true
            }
            chain.handleInterceptResultPermission(tempPermissionResult)
        } else {
            if (permissionResult == null && activityResult == null) {//都为空，是在intercept方法中的默认都有权限处理。
                isHandleInterceptResultPermissionNow(chain, parseRequestPermission(chain))
                return
            }
            if (permissionResult != null) {//权限请求结果
                if (isAboveAndroid10()) {
                    if (permissionResult.containsKey(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {//此次申请的是后台权限
                        val tempPermissionResult = parseRequestPermission(chain)
                        if (tempPermissionResult.values.contains(false)) {//如果还有未授权定位权限，则不判断定位服务是否开启
                            chain.handleInterceptResultPermission(permissionResult)
                        } else {//授权定位全部权限。
                            isHandleInterceptResultPermissionNow(chain, permissionResult)
                        }
                    } else {//此次申请的非后台权限
                        //大于Android10：如果ACCESS_FINE_LOCATION或ACCESS_COARSE_LOCATION这两个权限 其中一个或两都同意了，如果还有申请后台权限，则需要再次申请下后台权限。
                        //如果ACCESS_FINE_LOCATION或ACCESS_COARSE_LOCATION这两个权限都拒绝了，则直接返回拒绝的结果。不考虑是否有申请后台权限。
                        val isGrantedAccessFineLocation = permissionResult[Manifest.permission.ACCESS_FINE_LOCATION] == true
                        val isGrantedAccessCoarseLocation = permissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                        if (isGrantedAccessFineLocation || isGrantedAccessCoarseLocation) {
                            //ACCESS_FINE_LOCATION或ACCESS_COARSE_LOCATION这两个权限其中一个或两个同意权限申请了
                            if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                                //如果还有申请后台权限，则需要再次申请下后台权限。
                                chain.requestPermission(arrayListOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                            } else {
                                if (permissionResult.values.contains(false)) {//如果还有未授权定位权限，则不判断定位服务是否开启
                                    chain.handleInterceptResultPermission(permissionResult)
                                } else {//授权定位全部权限。
                                    isHandleInterceptResultPermissionNow(chain, permissionResult)
                                }
                            }
                        } else {
                            //ACCESS_FINE_LOCATION或ACCESS_COARSE_LOCATION这两个权限都拒绝了,直接返回false结果。
                            val tempPermissionResult = hashMapOf<String, Boolean>()
                            chain.getSpecialPermissions().forEach {
                                tempPermissionResult[it] = false
                            }
                            chain.handleInterceptResultPermission(tempPermissionResult)
                        }
                    }
                } else {
                    //小于等于Android10，直接处理结果。
                    if (permissionResult.values.contains(false)) {//如果还有未授权定位权限，则不判断定位服务是否开启
                        chain.handleInterceptResultPermission(permissionResult)
                    } else {//授权定位全部权限。
                        isHandleInterceptResultPermissionNow(chain, permissionResult)
                    }
                }
            }
            if (activityResult != null) {
                val result = parseRequestPermission(chain)
                //定位服务是否开启
                if (!isOpenLocationService()) {
                    //没有开启
                    result[LOCATION_SERVICE_IS_OPEN] = false
                }
                chain.handleInterceptResultPermission(result)
            }
        }
    }

    //是否需要现在立即处理权限请求结果。如果权限服务没有开启，不能立即处理，需要前往权限设置页面，开启权限服务。
    private fun isHandleInterceptResultPermissionNow(chain: PermissionRequestInterceptor.Chain, permissionResult: Map<String, Boolean>) {
        //判断定位服务是否开启
        if (isOpenLocationService()) {//开启
            chain.handleInterceptResultPermission(permissionResult)//处理结果
        } else {
            //没有开启，需要前往开启。
            var intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            if (!PermissionUtils.resolveActivity(context, intent)) {
                intent = PermissionUtils.getAppDetailIntent(context)
            }
            chain.requestPermission(intent)
        }
    }

    //是否开启定位服务
    private fun isOpenLocationService(): Boolean {
        var isGps = false //GPS定位是否启动
        var isNetwork = false //网络定位是否启动
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
        if (locationManager != null) {
            //通过GPS卫星定位，定位级别能够精确到街(在室外和空旷的地方定位准确、速度快)
            isGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            //通过WLAN或移动网络(3G/2G)确定的位置(也称作AGPS，辅助GPS定位。主要用于在室内或遮盖物密集的地方定位)
            isNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
        return isGps || isNetwork
    }

    //处理权限请求结果的数据。
    @SuppressLint("InlinedApi")
    private fun parseRequestPermission(chain: PermissionRequestInterceptor.Chain): HashMap<String, Boolean> {
        val tempPermissionResult = hashMapOf<String, Boolean>()

        if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_FINE_LOCATION)) {
            tempPermissionResult[Manifest.permission.ACCESS_FINE_LOCATION] = PermissionUtils.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_COARSE_LOCATION)) {
            tempPermissionResult[Manifest.permission.ACCESS_COARSE_LOCATION] = PermissionUtils.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (chain.getSpecialPermissions().contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            tempPermissionResult[Manifest.permission.ACCESS_BACKGROUND_LOCATION] = PermissionUtils.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return tempPermissionResult
    }

    private fun getNeedRequestPermissions(chain: PermissionRequestInterceptor.Chain): ArrayList<String> {
        val needRequestPermission = arrayListOf<String>()
        for (permission in chain.getSpecialPermissions()) {
            if (!PermissionUtils.checkSelfPermission(context, permission)) {
                needRequestPermission.add(permission)
            }
        }
        return needRequestPermission
    }


    /**
     * 是否小于Android6,小于Android6不用申请
     */
    private fun isBelowAndroid6(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.applicationInfo.targetSdkVersion < Build.VERSION_CODES.M
    }


    /**
     * 是否小于Android10
     */
    private fun isBelowAndroid10(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.applicationInfo.targetSdkVersion < Build.VERSION_CODES.M
    }

    /**
     * 是否是Android10
     */
    private fun isAndroid10(): Boolean {
        return Build.VERSION.SDK_INT == Build.VERSION_CODES.Q || context.applicationInfo.targetSdkVersion < Build.VERSION_CODES.Q
    }


    /**
     * 是否高于Android10
     */
    private fun isAboveAndroid10(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.R
    }

}