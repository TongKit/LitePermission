# LitePermission

一个由 Kotlin 语言开发的安卓权限库，使用责任链设计模式，具有很高的扩展性和兼容性。

支持**动态权限**和**特殊权限**的申请。

# 开发环境要求

Android 5.0 及以上版本。



# 快速集成



# 用法：

```kotlin
LitePermission.builder(activity)
                .permissions(Manifest.permission.CAMERA)
                .request(object : PermissionCallback {
                    override fun onResult(allGranted: Boolean, grantedList: List<String>, deniedList: List<String>, doNotAskAgainList: List<String>) {
                        Log.d(TAG, "allGranted = $allGranted")
                    }
                })
```



当前支持的特殊权限：

- **REQUEST_INSTALL_PACKAGES**：应用安装权限。在 **InstallPackagesPermissionInterceptor** 拦截器中处理权限申请逻辑。

- **MANAGE_EXTERNAL_STORAGE**：文件管理权限。在 **ManageExternalStoragePermissionInterceptor** 拦截器中处理权限申请逻辑。

- **PACKAGE_USAGE_STATS**：应用使用情况权限。在 **PackageUsageStatsPermissionInterceptor** 拦截器中处理权限申请逻辑。

- **SYSTEM_ALERT_WINDOW**：悬浮窗权限。在 **SystemAlertWindowPermissionInterceptor** 拦截器中处理权限申请逻辑。

- **WRITE_SETTINGS**：文件管理权限。在 **WriteSettingsPermissionInterceptor** 拦截器中处理权限申请逻辑。

- **定位权限**：由于定位权限需要判断定位服务是否开启，如果没有开启，需要跳转到系统设置页面打开定位服务，因此将定位权限归于特殊权限。定位服务是否开启的结果，已**LOCATION_SERVICE_IS_OPEN** 字符串做标识返回。在 **LocationPermissionInterceptor** 拦截器中处理权限申请逻辑。

  

可自行实现 ```PermissionRequestInterceptor``` 接口，自定义需要实现的权限逻辑。

自定义实现时，需要确定library中是否是已实现了该权限，如果library有默认实现，请保证自定义实现的```PermissionRequestInterceptor.interceptorType()``` 字段，与library中默认实现一致。

```kotlin
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
```



在实例代码中，有一个 ```Manifest.permission.MANAGE_EXTERNAL_STORAGE``` 权限申请的重新实现，添加了说明弹框，可参考。



# License

```
Copyright [2025] siwei tong

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

