package cn.max.screenrecord.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by geyu on 17-11-8.
 */

public class Utils {

    private static final String TAG = __TAG__();
    private static final String RECORD_STATE = "record_state";

    public static String __TAG__(){

        String tag = "max";
        try {
            tag = tag + (new Exception()).getStackTrace()[1].getFileName().replace(".java","");
        }catch (Exception e){
            Log.w("max.Utils", "get Tag File name failed. ");
            e.printStackTrace();
        }

        return tag;
    }

    public static String getRecordingTime(long seconds){

        long min = seconds%3600/60;
        long sec = seconds%60;
        long hour = seconds/3600;

        return String.format("%02d:%02d:%02d", hour, min, sec);
    }

    public static String getTimeStamp(){

        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String time = dateFormat.format(date);
        Log.d(TAG, "getTimeStamp "+time);
        return time;
    }

    public static int dip2px(Context context, int dip){

        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, context.getResources().getDisplayMetrics());
        Log.d(TAG, "dip " + dip + " trans to px " + px);
        return  (int)px;
    }

    public static void setRecordState(Context context, boolean on){

        SharedPreferences sharedPreferences = context.getSharedPreferences("recordState", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(RECORD_STATE, on);
        editor.apply();
    }

    public static boolean getRecordStateIsOn(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("recordState", Context.MODE_PRIVATE);
        return sharedPreferences.getBoolean(RECORD_STATE, false);
    }
}
