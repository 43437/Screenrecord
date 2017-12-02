package cn.max.screenrecord.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.util.Log;

import cn.max.screenrecord.constant.Constants;
import cn.max.screenrecord.ScreenRecordService;

import static cn.max.screenrecord.utils.Utils.__TAG__;

/**
 * Created by geyu on 17-11-8.
 */

public class RecordPermissionActivity extends Activity {

    private static final String TAG = __TAG__();

    MediaProjectionManager mediaProjectionManager;
    private static final int REQUEST_CODE = 0x101;

    public static Intent getIntent(Context context){

        return new Intent(context, RecordPermissionActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent intent = mediaProjectionManager.createScreenCaptureIntent();
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null) {
            Log.w(TAG, "onActivityResult intent is null, now new it. ");
            data = new Intent();
        }

        data.setClass(this, ScreenRecordService.class);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            data.putExtra(Constants.RESULTCODE, RESULT_OK);
            data.setAction(Constants.START_SCREEN_RECORD);
        }else if (requestCode == REQUEST_CODE && resultCode != RESULT_OK){
            data.putExtra(Constants.RESULTCODE, 0);
            data.putExtra(Constants.RESULTACTION, Constants.RESULTACTION_CODE_DENY);
            data.setAction(Constants.START_SCREEN_RECORD);
        }

        startService(data);
        finish();
    }
}
