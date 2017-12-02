package cn.max.screenrecord;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import cn.max.screenrecord.activity.AudioPermissionActivity;
import cn.max.screenrecord.activity.FloatWindowPermissionActivity;
import cn.max.screenrecord.activity.RecordPermissionActivity;
import cn.max.screenrecord.activity.StoragePermissionActivity;
import cn.max.screenrecord.constant.Constants;
import cn.max.screenrecord.recorder.MediarecorderIml;
import cn.max.screenrecord.service.QuickSettingService;
import cn.max.screenrecord.utils.Utils;
import cn.max.screenrecord.view.FloatingView;
import cn.max.screenrecord.view.PreviewView;

import java.io.File;

import static android.app.Activity.RESULT_OK;
import static cn.max.screenrecord.utils.Utils.__TAG__;
import static cn.max.screenrecord.utils.Utils.getTimeStamp;


public class ScreenRecordService extends Service {

    private static final String TAG = __TAG__();
    /**
     * 控件window
     */
    private FloatingView mFloatingWindow;

    private AudioManager audioManager;

    /**
     * 两个状态的View
     */
    private View mFloatView;

    private boolean isStarted = false;

    private boolean isMuted = false;

    private String saveFile;

    private static final String SAVE_DIR = "/sdcard/DCIM/ScreenRecord/";

    private PreviewView previewView;
    /**
     * 相关控件
     */

    private MediarecorderIml mediarecorderIml;

    private Context mContext;


    @Override
    public IBinder onBind(Intent intent) {
        return new PopupBinder();
    }

    public class PopupBinder extends Binder {
        public ScreenRecordService getService() {
            return ScreenRecordService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initFloatingWindow();

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String action = "";
        if (intent != null) {
            action = intent.getAction();
            Log.d(TAG, "intent is not null, get action " + action);
        }


        if (!isStarted && action.contains(Constants.START_SCREEN_RECORD)) {

            if (intent.getIntExtra(Constants.STORAGE_PERMISSION, Constants.STORAGE_PERMISSION_OK) == Constants.STORAGE_PERMISSION_DENY) {
                Log.w(TAG, "storage permission deny, now destroy. ");

                errorPermissionExit();

                return START_NOT_STICKY;
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "build version is large M, check permission. ");
                if ((checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        || checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)) {
                    Log.w(TAG, "large M, permission not granted, now request storage permission. ");
                    requestStoragePermission();
                } else {
                    Log.d(TAG, "large M, storage permission is granted. ");
                }
            }
            if (intent.getIntExtra(Constants.AUDIO_PERMISSION, Constants.AUDIO_PERMISSION_OK) == Constants.AUDIO_PERMISSION_DENY) {

                Log.w(TAG, "audio permission is deny, now exit. ");
                errorPermissionExit();
                return START_NOT_STICKY;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
                    Log.w(TAG, "audio record permission not granted. ");

                    requestAudioRecordPermission();

                    return START_NOT_STICKY;
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                if (intent.getIntExtra(Constants.FLOAT_WINDOW_PERMISSION, Constants.FLOAT_WINDOW_PERMISSION_OK) != Constants.FLOAT_WINDOW_PERMISSION_DENY) {
                    requestFloatWindowPermission();
                } else {
                    Log.w(TAG, "float window permission deny, now destroy. ");
                    errorPermissionExit();
                }
                return START_NOT_STICKY;
            }

            int result = intent.getIntExtra(Constants.RESULTCODE, 0);
            Log.d(TAG, "result code is " + result);

            if (result == RESULT_OK) {
                Log.d(TAG, "start record");
                startRecord(result, intent);

            } else if (intent.getIntExtra(Constants.RESULTACTION, 0) == Constants.RESULTACTION_CODE_DENY) {
                Log.w(TAG, "request permission deny, now exit. ");
                errorPermissionExit();

            } else {
                Log.d(TAG, "start record, need permission, now request it. ");
                requestRecordPermission();
            }
        } else if (isStarted && action.contains(Constants.STOP_SCREEN_RECORD)) {
            Log.d(TAG, "start is true,  now stop. ");
            stopRecord();
        }

       /* orientationEventListener = new OrientationEventListener(this.getApplicationContext(), SensorManager.SENSOR_DELAY_NORMAL) {
            @Override
            public void onOrientationChanged(int orientation) {

                if (orientation == ORIENTATION_UNKNOWN){
//                    Log.d(TAG, "orientation unknown, now return. ");
                    return;
                }

//                Log.d(TAG, "orientation change "+orientation);
                if (screenDirection != ORIENTATION_PORTRAIT && isScreenPortrait()){  //screen change to portrait

                    screenDirection = ORIENTATION_PORTRAIT;
                    Log.d(TAG, "isPortrait "+isScreenPortrait());
                    if (previewView != null){
                        previewView.onOrientationChanged(Configuration.ORIENTATION_PORTRAIT);
                    }

                }else if (screenDirection != ORIENTATION_LANDSCAPE && !isScreenPortrait()){  //screen change to landscape

                    screenDirection = ORIENTATION_LANDSCAPE;
                    Log.d(TAG, "isPortrait "+isScreenPortrait());
                    if (previewView != null){
                        previewView.onOrientationChanged(Configuration.ORIENTATION_LANDSCAPE);
                    }

                }
            }
        };

        if (orientationEventListener.canDetectOrientation()){
            Log.w(TAG, "orientation event listener enable. ");
            orientationEventListener.enable();
        }else {
            Log.w(TAG, "orientation event listener disable. ");
            orientationEventListener.disable();
        }*/

        return START_NOT_STICKY;
    }

    private MediarecorderIml.CallBack callBack = new MediarecorderIml.CallBack() {
        @Override
        public void onErro(Exception e) {

            Log.w(TAG, "screen record error occurred. ");
            e.printStackTrace();

            Toast.makeText(getContext(), getResources().getString(R.string.screenshot_failed_title), Toast.LENGTH_SHORT).show();

            Utils.setRecordState(getContext(), false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                QuickSettingService.requestListeningState(getContext(), new ComponentName(getContext(), QuickSettingService.class));

            MediarecorderIml.getInstance().errorRelease();
            stopSelf();
        }

        @Override
        public void onStop() {

            Toast.makeText(ScreenRecordService.this, getResources().getString(R.string.screenshot_stop_screenrecord), Toast.LENGTH_SHORT).show();

            Utils.setRecordState(getContext(), false);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                QuickSettingService.requestListeningState(getContext(), new ComponentName(getContext(), QuickSettingService.class));

            Bitmap bitmap = mediarecorderIml.getSnapshot();

            previewView.startPreview(bitmap);

            getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + saveFile)));

            Log.d(TAG, "screen record stop. ");
            stopSelf();
        }

        @Override
        public void onStarted() {
            Log.d(TAG, "screen record started. ");
            Toast.makeText(getContext(), getResources().getString(R.string.screenshot_start_screenrecord), Toast.LENGTH_SHORT).show();

            Utils.setRecordState(getContext(), true);


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                QuickSettingService.requestListeningState(getContext(), new ComponentName(getContext(), QuickSettingService.class));
        }
    };

    public Context getContext() {
        if (mContext == null) {
            mContext = getApplicationContext();
        }
        return mContext;
    }

    private void initFloatingWindow() {
        mFloatView = LayoutInflater.from(this).inflate(R.layout.folating_view, null);

        mFloatingWindow = new FloatingView(this);
        mFloatingWindow.setCallBack(new FloatingView.CallBack() {
            @Override
            public void onPopClicked(int msg) {
                switch (msg) {
                    case FloatingView.MSG_VOICE_MUTE:
                        if (!isMuted) {
                            Log.d(TAG, "setMicrophoneMute true");
                            audioManager.setMicrophoneMute(true);
                            isMuted = true;
                        } else {
                            Log.d(TAG, "setMicrophoneMute false");
                            audioManager.setMicrophoneMute(false);
                            isMuted = false;
                        }
                        mFloatingWindow.setVoiceMuteDrawableOff(isMuted);

                        break;

                    case FloatingView.MSG_START_STOP:
                        Log.d(TAG, "button id_pop_start_stop clicked.");
                        Log.d(TAG, "isStarted " + isStarted);
                        if (!isStarted) {
                            isStarted = true;
                        } else {
                            Log.d(TAG, "button click stop record. ");
                            stopRecord();
                        }
                        mFloatingWindow.setStartStopDrawableOn(isStarted);

                        break;
                }
            }
        });
        mFloatingWindow.setFloatingView(mFloatView);
    }

    public void requestRecordPermission() {

        Log.d(TAG, "start record. ");
        startActivity(RecordPermissionActivity.getIntent(ScreenRecordService.this));
    }

    public void requestStoragePermission() {
        startActivity(StoragePermissionActivity.getIntent(getContext()));
    }

    public void requestAudioRecordPermission(){
        startActivity(AudioPermissionActivity.getIntent(getContext()));
    }

    public void requestFloatWindowPermission() {

        startActivity(FloatWindowPermissionActivity.getIntent(getContext()));
    }

    public void stopRecord() {
        Log.d(TAG, "stop record.");

        mediarecorderIml.stopRecord();

        Utils.setRecordState(getContext(), false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            QuickSettingService.requestListeningState(getContext(), new ComponentName(getContext(), QuickSettingService.class));

        isStarted = false;
        dimiss();
    }

    private void startRecord(int result, Intent intent) {

        File saveDir = new File(SAVE_DIR);
        if (!saveDir.exists()) {
            if (false == saveDir.mkdirs()) {
                Log.w(TAG, "save dir " + SAVE_DIR + " not exist, and make it failed, now exit. ");
                return;
            }
        }

        saveFile = SAVE_DIR + "ScreenRecord_" + getTimeStamp() + ".mp4";
        Log.d(TAG, "saveFile " + saveFile);

        MediarecorderIml.getInstance().initProjection(this, result, intent);
        mediarecorderIml = MediarecorderIml.getInstance();

        previewView = new PreviewView(getContext(), saveFile);

        mediarecorderIml.startRecord(saveFile, callBack);

        if (mediarecorderIml.getIsRunning()) {
            show();
            isStarted = true;
            Log.d(TAG, "isStarted " + isStarted);
        }
    }

    public void show() {
        if (null != mFloatingWindow)
            mFloatingWindow.show();
    }

    public void dimiss() {
        if (null != mFloatingWindow)
            mFloatingWindow.dismiss();
    }

    public void errorPermissionExit() {

        Toast.makeText(getContext(), getResources().getString(R.string.screenshot_failed_title), Toast.LENGTH_SHORT).show();
        Log.w(TAG, "float window permission is deny, now stop service and record process. ");
        if (isStarted) {
            stopRecord();
        }
        stopSelf();
    }
}
