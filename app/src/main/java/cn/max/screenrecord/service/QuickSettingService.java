package cn.max.screenrecord.service;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import cn.max.screenrecord.R;
import cn.max.screenrecord.ScreenRecordService;
import cn.max.screenrecord.constant.Constants;
import cn.max.screenrecord.utils.Utils;

/**
 * 这个类可以在android 7.0版本及以上添加一个下拉菜单中的图标入口，但是有些手机厂商定义了策略导致非系统级别应用添加失败
 */
@TargetApi(24)
public class QuickSettingService extends TileService {

    private final int STATE_OFF = 0;
    private final int STATE_ON = 1;
    private final String TAG = Utils.__TAG__();
    private int toggleState = STATE_OFF;

    @Override
    public void onClick() {
        // TODO Auto-generated method stub
        Log.d(TAG, "onClick state = " + getQsTile().getState());
        Icon icon;
        if (toggleState == STATE_ON) {
            toggleState = STATE_OFF;
            icon = Icon.createWithResource(getApplicationContext(), R.drawable.menu_screen_record_off);
            getQsTile().setState(Tile.STATE_INACTIVE);// 更改成非活跃状态
        } else {
            toggleState = STATE_ON;
            icon = Icon.createWithResource(getApplicationContext(), R.drawable.menu_screen_record_on);
            getQsTile().setState(Tile.STATE_ACTIVE);//更改成活跃状态
        }
        getQsTile().setIcon(icon);//设置图标
        getQsTile().updateTile();//更新Tile
        startScreenRecord();
    }

    private void startScreenRecord() {
        // TODO Auto-generated method stub
        Intent service = new Intent(this, ScreenRecordService.class);
        ;
        if (toggleState == STATE_ON) {
            service.setAction(Constants.START_SCREEN_RECORD);
        } else {
            service.setAction(Constants.STOP_SCREEN_RECORD);
        }
        startService(service);
    }

    //当用户从Edit栏添加到快速设定中调用
    @Override
    public void onTileAdded() {
        // TODO Auto-generated method stub
        super.onTileAdded();
    }

    //当用户从快速设定栏中移除的时候调用
    @Override
    public void onTileRemoved() {
        // TODO Auto-generated method stub
        super.onTileRemoved();
    }

    // 打开下拉菜单的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
    @Override
    public void onStartListening() {
        // TODO Auto-generated method stub

        Log.d(TAG, "....onStartListening....");
        Icon icon = Icon.createWithResource(getApplicationContext(), R.drawable.menu_screen_record_off);

        if (Utils.getRecordStateIsOn(getApplicationContext())) {
            icon = Icon.createWithResource(getApplicationContext(), R.drawable.menu_screen_record_on);
        }

        getQsTile().setIcon(icon);
        getQsTile().updateTile();//更新Tile
    }

    // 关闭下拉菜单的时候调用,当快速设置按钮并没有在编辑栏拖到设置栏中不会调用
    @Override
    public void onStopListening() {
        // TODO Auto-generated method stub
        Log.d(TAG, "....onStopListening....");
        super.onStopListening();
    }
}
