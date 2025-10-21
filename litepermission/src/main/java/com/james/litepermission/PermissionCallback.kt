package com.james.litepermission

/**
 * @author: tongsiwei
 * @date: 2024/12/24
 * @Description:权限请求结果回调。
 */
fun interface PermissionCallback {
    /**
     * @param result 权限请求结果
     */
    fun onResult(result: PermissionRequestResult)
}

/**
 * @param allGranted 申请的权限是否全部允许了。true.全部允许；false：有权限被拒绝了。
 * @param grantedList 允许申请的权限列表。
 * @param deniedList 拒绝申请的权限列表。
 * @param doNotAskAgainList 拒绝申请且不在询问的权限列表。
 */
data class PermissionRequestResult(
    val allGranted: Boolean,
    val grantedList: List<String>,
    val deniedList: List<String>,
    val doNotAskAgainList: List<String>
)