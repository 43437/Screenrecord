package cn.max.screenrecord.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import cn.max.screenrecord.R;
import cn.max.screenrecord.constant.Constants;
import cn.max.screenrecord.ScreenRecordService;

import static cn.max.screenrecord.utils.Utils.__TAG__;


public class MainActivity extends Activity implements View.OnClickListener{

    private static final String TAG = __TAG__();

    private Button StartButton;
    private Button StopButton;

    private Intent mServiceIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StartButton = (Button) findViewById(R.id.start_record_service);
        StopButton = (Button) findViewById(R.id.stop_record_service);

        StartButton.setOnClickListener(this);
        StopButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.start_record_service:
                mServiceIntent = new Intent(MainActivity.this, ScreenRecordService.class);
                mServiceIntent.setAction(Constants.START_SCREEN_RECORD);
                startService(mServiceIntent);
                finish();
                break;
            case R.id.stop_record_service:
                mServiceIntent = new Intent(MainActivity.this, ScreenRecordService.class);
                mServiceIntent.setAction(Constants.STOP_SCREEN_RECORD);
                startService(mServiceIntent);
                finish();
                break;
        }
    }
}
