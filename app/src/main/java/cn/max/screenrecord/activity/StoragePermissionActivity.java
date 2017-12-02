package cn.max.screenrecord.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import cn.max.screenrecord.ScreenRecordService;
import cn.max.screenrecord.constant.Constants;

import static cn.max.screenrecord.utils.Utils.__TAG__;

public class StoragePermissionActivity extends Activity {

    private static final String TAG = __TAG__();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "StoragePermissionActivity onCreate, request permission. ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            Log.d(TAG, "build version large M, now request permission. ");
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0x00);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        boolean permissionAuth = true;
        Intent intent = new Intent();
        for (int resultCode : grantResults){
            if (resultCode != PackageManager.PERMISSION_GRANTED){
                Log.w(TAG, "storage permission not granted, now return. ");
                permissionAuth = false;
            }
        }
        if (permissionAuth == false){
            intent.putExtra(Constants.STORAGE_PERMISSION, Constants.STORAGE_PERMISSION_DENY);
        }else {
            intent.putExtra(Constants.STORAGE_PERMISSION, Constants.STORAGE_PERMISSION_OK);
        }

        Log.w(TAG, "storage permission is granted, now return start recording. ");
        intent.setAction(Constants.START_SCREEN_RECORD);
        intent.setClass(this, ScreenRecordService.class);
        startService(intent);
        finish();
    }

    public static Intent getIntent(Context context){

        return new Intent(context, StoragePermissionActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
