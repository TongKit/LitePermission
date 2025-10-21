package com.james.litepermission.sample;

import android.Manifest;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;

import com.james.litepermission.LitePermission;

/**
 * @author: tongsiwei
 * @date: 2025/10/21
 * @Description:
 */
public class TestPermissionRequest {

    public void request(FragmentActivity activity) {
        LitePermission.builder(activity)
                .permissions(Manifest.permission.RECORD_AUDIO)
                .request(result -> {
                    Log.d("TestPermissionRequest", "allGranted = " + result.getAllGranted());
                });
    }
}
