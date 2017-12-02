package cn.max.screenrecord.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;

import cn.max.screenrecord.ScreenRecordService;
import cn.max.screenrecord.constant.Constants;

public class AudioPermissionActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 0x01);
        }
    }

    public static Intent getIntent(Context context){

        return new Intent(context, AudioPermissionActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        Intent intent = new Intent();
        intent.setClass(this, ScreenRecordService.class);

        intent.setAction(Constants.START_SCREEN_RECORD);

        if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
            intent.putExtra(Constants.AUDIO_PERMISSION, Constants.AUDIO_PERMISSION_OK);
        }else {
            intent.putExtra(Constants.AUDIO_PERMISSION, Constants.AUDIO_PERMISSION_DENY);
        }

        startService(intent);
        finish();
    }
}
