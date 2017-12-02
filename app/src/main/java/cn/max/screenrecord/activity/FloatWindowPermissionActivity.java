package cn.max.screenrecord.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import cn.max.screenrecord.constant.Constants;
import cn.max.screenrecord.ScreenRecordService;

import static cn.max.screenrecord.utils.Utils.__TAG__;

public class FloatWindowPermissionActivity extends Activity {

    private static final String TAG = __TAG__();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.d(TAG, "can not DrawOverlays, now request it. ");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 12);
        }else {
            Intent intent = new Intent(this, ScreenRecordService.class);
            intent.putExtra(Constants.FLOAT_WINDOW_PERMISSION, Constants.FLOAT_WINDOW_PERMISSION_OK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startService(intent);
            finish();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            Log.w(TAG, "onActivityResult intent is null, now new it. ");
            data = new Intent();
        }

        if (resultCode!= RESULT_OK){
            data.putExtra(Constants.FLOAT_WINDOW_PERMISSION, Constants.FLOAT_WINDOW_PERMISSION_DENY);
        }else {
            data.putExtra(Constants.FLOAT_WINDOW_PERMISSION, Constants.FLOAT_WINDOW_PERMISSION_OK);
        }
        data.setClass(this, ScreenRecordService.class);
        data.setAction(Constants.START_SCREEN_RECORD);

        startService(data);
        finish();
    }

    public static Intent getIntent(Context context) {

        return new Intent(context, FloatWindowPermissionActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
}
