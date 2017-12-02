package cn.max.screenrecord.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import cn.max.screenrecord.R;
import cn.max.screenrecord.utils.Utils;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static cn.max.screenrecord.utils.Utils.__TAG__;

/**
 * Created by geyu on 17-11-9.
 */

public class PreviewView {

    private Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private DisplayMetrics mDisplayMetrics;
    private static final int TYPE_ANIMATE_DISMISS = 0x99;
    private static final int TYPE_DISMISS = 0x100;

    int pxMarginEnd = 12;

    private View previewView;
    private ImageView ivSnapShot;

    private float downX;
    private float downY;

    private float moveX;
    private float moveY;

    private static final float DISTANCE = 90.0f;  //  点击偏移量   在上、下、左、右这个范围之内都会触发点击事件

    private static final float TOUCH_EVENT_DISTANCE = 45.0f;

    private static final String TAG = __TAG__();

    private int screenHeight = 1080;
    private int screenWidth = 720;

    private int pxHeight;
    private int pxWidth;

    private String screenRecordFile;

    private static final int MSG_DISMISS = 0x10001;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_DISMISS:
                    if (mHandler.hasMessages(MSG_DISMISS)) {
                        mHandler.removeMessages(MSG_DISMISS);
                    }
                    mHandler.removeCallbacksAndMessages(null);

                    stopPreview(TYPE_ANIMATE_DISMISS);
                    break;
            }
        }
    };

    private Context getContext() {
        return this.mContext;
    }

    public PreviewView(Context context, String filePath) {
        this.mContext = context;
        this.screenRecordFile = filePath;

        previewView = LayoutInflater.from(context).inflate(R.layout.preview_layout, null);
        previewView.setOnTouchListener(new WindowTouchListener());
        ivSnapShot = (ImageView) previewView.findViewById(R.id.preview_snapshot);

        initWindowManager();
    }

    private void initWindowManager() {
        mWindowManager = (WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    }

    private WindowManager getWindowManager() {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    public WindowManager.LayoutParams getLayoutParams() {
        if (mLayoutParams == null) {
            mLayoutParams = new WindowManager.LayoutParams();
            initLayoutParams();
        }
        return mLayoutParams;
    }

    private int getStatusBarHeight(Context context) {
        int height = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            height = context.getResources().getDimensionPixelSize(resId);
        }
        return height;
    }

    private void initLayoutParams() {

        int width = mDisplayMetrics.widthPixels;
        int height = mDisplayMetrics.heightPixels;

        Log.d(TAG, "screen width " + width + "screen height " + height);

        getLayoutParams().flags = getLayoutParams().flags
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        getLayoutParams().dimAmount = 0.2f;
        getLayoutParams().type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        getLayoutParams().gravity = Gravity.END | Gravity.BOTTOM;
        getLayoutParams().format = PixelFormat.RGBA_8888;
        getLayoutParams().alpha = 1.0f;  //  设置整个窗口的透明度

        getLayoutParams().width = screenWidth;
        getLayoutParams().height = screenHeight;

        getLayoutParams().x = 0;//(int) (mDisplayMetrics.widthPixels - offsetX);
        getLayoutParams().y = 0;//height - getLayoutParams().height - (int)(px *2 );//(int) (mDisplayMetrics.heightPixels * 1.0f / 4 - offsetY);

        Log.d(TAG, "position x y " + getLayoutParams().x + " " + getLayoutParams().y);
    }

    public void startPreview(Bitmap snapthot) {


        mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            pxHeight = Utils.dip2px(getContext(), 96);
            pxWidth = Utils.dip2px(getContext(), 166);

            Log.d(TAG, "screen orientation is landscape, height width " + mDisplayMetrics.heightPixels + " " + mDisplayMetrics.widthPixels);
        } else {

            pxHeight = Utils.dip2px(getContext(), 166);
            pxWidth = Utils.dip2px(getContext(), 96);
            Log.d(TAG, "screen orientation is portrait, height width " + mDisplayMetrics.heightPixels + " " + mDisplayMetrics.widthPixels);
        }

        screenHeight = mDisplayMetrics.heightPixels;
        screenWidth = mDisplayMetrics.widthPixels;

        getLayoutParams().width = screenWidth;
        getLayoutParams().height = screenHeight;

        ivSnapShot.setImageBitmap(snapthot);

        getWindowManager().addView(previewView, getLayoutParams());

        ValueAnimator animator = null;

        pxMarginEnd = Utils.dip2px(getContext(), 12);

        Log.d(TAG, "pxMarginEnd is " + pxMarginEnd);

        animator = ValueAnimator.ofInt(0, pxMarginEnd);
        animator.setInterpolator(new DecelerateInterpolator());

        animator.setDuration(600);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();

                Log.d(TAG, "onAnimationUpdate value " + value);
                previewAnimatorUpdate(value);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                getLayoutParams().x = pxMarginEnd;
                getLayoutParams().y = pxMarginEnd;
                updateLocation();
                mHandler.sendEmptyMessageDelayed(MSG_DISMISS, 3000);
                Log.d(TAG, "animation end. ");
            }

            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                getLayoutParams().x = 0;
                getLayoutParams().y = 0;
                Log.d(TAG, "animation begin. ");
            }
        });
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }

    private boolean isScreenPortrait() {

        return (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    public void stopPreview(int style) {

        Log.d(TAG, "stop preview. ");
        if (previewView == null)
            return;

        if (style == TYPE_ANIMATE_DISMISS) {
            ValueAnimator animator = ValueAnimator.ofFloat((float) 1.0, (float) 0.2);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float value = (float) animation.getAnimatedValue();
                    Log.d(TAG, "dismiss alpha " + value);
                    getLayoutParams().alpha = value;
                    updateLocation();
                }
            });

            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    try {
                        getWindowManager().removeView(previewView);
                        previewView = null;
                    } catch (Exception e) {
                        Log.w(TAG, "stop Preview with animate failed. ");
                    } finally {

                    }
                }
            });
            animator.setInterpolator(new DecelerateInterpolator());
            animator.setDuration(300);
            animator.start();

        } else {
            try {
                getWindowManager().removeView(previewView);
                previewView = null;
            } catch (Exception e) {
                Log.w(TAG, "stop Preview failed.");
                e.printStackTrace();
            }
        }
    }

    private void previewAnimatorUpdate(int value) {

        int scale = 1 - value / pxMarginEnd;

        getLayoutParams().height = (screenHeight - pxHeight) * scale + pxHeight;
        getLayoutParams().width = (screenWidth - pxWidth) * scale + pxWidth;


        Log.d(TAG, "screenHeight screenWidth " + screenHeight + " " + screenWidth);
        Log.d(TAG, "window x y " + value + " " + value);

        updateLocation();
    }

    /**
     * 窗口监听类回调接口
     */
    class WindowTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    down(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    move(event);
                    break;
                case MotionEvent.ACTION_UP:
                    up(event);
                    break;
                case MotionEvent.ACTION_OUTSIDE:
                    Log.d(TAG, "out side dismiss. ");
                    stopPreview(TYPE_ANIMATE_DISMISS);
                    return true;
                default:
                    break;
            }
            return false;
        }
    }

    /**
     * 按下的事件
     *
     * @param event
     */
    private void down(MotionEvent event) {
        moveX = downX = event.getRawX();
        moveY = downY = event.getRawY();
    }

    /**
     * 移动事件
     *
     * @param event
     */
    private void move(MotionEvent event) {

        float newMoveX = event.getRawX();
        float newMoveY = event.getRawY();

        int distanceXtmp = (int) (newMoveX - moveX);
        int distanceYtmp = (int) (newMoveY - moveY);

        moveX = newMoveX;
        moveY = newMoveY;

        getLayoutParams().x -= distanceXtmp;
        getLayoutParams().y -= distanceYtmp;

        updateLocation();

        if (Math.abs(moveX - downX) < DISTANCE && Math.abs(moveY - downY) < DISTANCE) {

            Log.d(TAG, "location x y " + getLayoutParams().x + " " + getLayoutParams().y);
            updateLocation();
        } else {
            Log.d(TAG, "move too far away, now dissmiss. ");
            previewView.setOnTouchListener(null);
            stopPreview(TYPE_ANIMATE_DISMISS);
        }

        Log.d(TAG, "down x y " + event.getX() + " " + event.getY());
        Log.d(TAG, "down raw x y " + event.getRawX() + " " + event.getRawY());

    }

    /**
     * 更新窗口的位置
     */
    private void updateLocation() {
        if (getContentView() != null) {
            Log.d(TAG, "update location");
            getWindowManager().updateViewLayout(previewView, getLayoutParams());
        }
    }

    public View getContentView() {
        return previewView;
    }


    /**
     * 手指弹起的时候的事件
     *
     * @param event
     */
    private void up(MotionEvent event) {

        Log.d(TAG, "up: ");

        final float upX = event.getRawX();
        final float upY = event.getRawY();
        float endDistanceX = upX - downX;
        float endDistanceY = upY - downY;
        Log.d(TAG, "end distance x y " + endDistanceX + " " + endDistanceY);

        if (Math.abs(endDistanceX) <= TOUCH_EVENT_DISTANCE && Math.abs(endDistanceY) <= TOUCH_EVENT_DISTANCE) {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            String type = "video/mp4";
            Uri uri = Uri.parse(this.screenRecordFile);
            intent.setDataAndType(uri, type);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);

            getContext().startActivity(intent);
            stopPreview(TYPE_DISMISS);

        } else if (Math.abs(endDistanceX) <= DISTANCE && Math.abs(endDistanceY) <= DISTANCE) {
            final int preX = getLayoutParams().x + (int) endDistanceX;
            final int preY = getLayoutParams().y + (int) endDistanceY;
            final int endX = getLayoutParams().x;
            final int endY = getLayoutParams().y;
            Log.d(TAG, "up update location to back. preX preY endX endY " + preX + " " + preY + " " + endX + " " + endY);

            ValueAnimator animator = ValueAnimator.ofFloat((float) 0, (float) 1);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {

                    float value = (float) animation.getAnimatedValue();
                    Log.d(TAG, "back value " + value);
                    getLayoutParams().x = (int) ((preX - endX) * value) + endX;
                    getLayoutParams().y = (int) ((preY - endY) * value) + endY;

                    updateLocation();
                }
            });
            animator.setInterpolator(new AccelerateInterpolator());
            animator.setDuration(400);
            animator.start();

        } else {
            stopPreview(TYPE_ANIMATE_DISMISS);
        }
        Log.d(TAG, "up x y " + event.getX() + " " + event.getY());
        Log.d(TAG, "up raw x y " + event.getRawX() + " " + event.getRawY());

    }
}
