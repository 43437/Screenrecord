package cn.max.screenrecord.view;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.max.screenrecord.R;
import cn.max.screenrecord.utils.Utils;

import static cn.max.screenrecord.utils.Utils.__TAG__;


public class FloatingView {

    private static final String TAG = __TAG__();

    public static final int MSG_START_STOP = 0x1001;
    public static final int MSG_VOICE_MUTE = 0x1002;

    private static final int MSG_DRAWING_HIDE = 0x1003;
    private static final int MSG_TIME_UPDATE = 0x1004;


    private WindowManager.LayoutParams mLayoutParams;

    private WindowManager mWindowManager;

    private Context mContext;
    private View mContentView;

    private static final float DISTANCE = 45.0f;  //  点击偏移量   在上、下、左、右这个范围之内都会触发点击事件

    private float offsetX;
    private float offsetY;

    private boolean mIsShowing;
    private float downX;
    private float downY;

    private float downInX;
    private float downInY;

    private boolean mIsOpen;

    private View mFloatingView;
    private View mPopupView;


    private ImageView mStartStopRecord;
    private ImageView mAudioRecord;
    private ImageView mMarkRecord;

    private View btnStartStopRecord;
    private View btnAudioRecord;
    private View btnMarkRecord;

    private TextView txtStartStop;
    private TextView txtMic;
    private TextView txtMark;

    private DrawingBoardView drawingBoardView;

    private boolean isMarkOn = false;

    private CallBack callBack;

    private TextView mTvShowShotTime;
    private ImageView imgDetailSign;
    private LinearLayout floatViewLayout;
    private View detailView;

    private long seconds = 0;

    private long startMillions;

    private static final int ALIGN_NONE = 0x1000;
    private static final int ALIGN_END = 0x1001;
    private static final int ALIGN_BOTOOM = 0x1002;

    private int alignBoundary = ALIGN_NONE;


    /**
     * 无参构造方法
     *
     * @param context
     */
    public FloatingView(Context context) {
        this(context, null);
    }


    /**
     * 带参数的构造方法
     *
     * @param context
     * @param floatingView
     */
    public FloatingView(Context context, View floatingView) {
        this.mContext = context;
        setFloatingView(floatingView);
        initWindowManager();
        initLayoutParams();

        seconds = 0;
    }


    /**
     * 设置开始的视图
     *
     * @param floatingView
     */
    public void setFloatingView(View floatingView) {
        if(floatingView != null) {
            this.mFloatingView = floatingView;
            mTvShowShotTime = (TextView) floatingView.findViewById(R.id.id_show_shot_time);
            imgDetailSign = (ImageView) floatingView.findViewById(R.id.detail_sign);
            detailView = floatingView.findViewById(R.id.detail_view);
            floatViewLayout = (LinearLayout) floatingView.findViewById(R.id.floating_view_container);
            Log.d(TAG, "detailView is null "+(detailView == null));
            setContentView(mFloatingView);
        }
    }

    /**
     * 初始化窗口管理器
     */
    private void initWindowManager() {
        mWindowManager = (WindowManager) getContext().getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
    }

    private DisplayMetrics getDisplayMetrics(){
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);
        return mDisplayMetrics;
    }

    /**
     * 初始化WindowManager.LayoutParams参数
     */
    private void initLayoutParams() {
        getLayoutParams().flags = getLayoutParams().flags
                | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;

        getLayoutParams().dimAmount = 0.2f;
        getLayoutParams().type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;

        getLayoutParams().height = WindowManager.LayoutParams.WRAP_CONTENT;
        getLayoutParams().width = Utils.dip2px(getContext(), 114);

        getLayoutParams().gravity = Gravity.START | Gravity.TOP;
        getLayoutParams().format = PixelFormat.RGBA_8888;
        getLayoutParams().alpha = 1.0f;  //  设置整个窗口的透明度
        offsetX = 0;
        offsetY =  getStatusBarHeight(getContext());

        DisplayMetrics displayMetrics = getDisplayMetrics();

        getLayoutParams().x = (int) ( displayMetrics.widthPixels - offsetX);
        getLayoutParams().y = (int) (displayMetrics.heightPixels * 1.0f / 4 - offsetY);

    }

    /**
     * 设置当前窗口布局
     *
     * @param contentView
     */
    private void setContentView(View contentView) {
        if(contentView != null) {
            if(getIsShowing()) {
                getWindowManager().removeView(mContentView);
                createContentView(contentView);
                getWindowManager().addView(mContentView, getLayoutParams());

                DisplayMetrics displayMetrics = getDisplayMetrics();

                getLayoutParams().x = displayMetrics.widthPixels / 2;
                getLayoutParams().y = displayMetrics.heightPixels / 2;
                updateLocation();
            } else {
                createContentView(contentView);
            }
        }
    }

    /**
     * 创建一个窗口显示的内容
     *
     * @param contentView
     */
    private void createContentView(View contentView) {
        this.mContentView = contentView;
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED); // 主动计算视图View的宽高信息
        contentView.setOnTouchListener(new WindowTouchListener());

        mPopupView = contentView.findViewById(R.id.detail_view);


        mStartStopRecord = (ImageView) mPopupView.findViewById(R.id.id_pop_start_stop);
        mAudioRecord = (ImageView) mPopupView.findViewById(R.id.id_pop_audio);
        mMarkRecord = (ImageView) mPopupView.findViewById(R.id.id_pop_mark);

        txtStartStop = (TextView) mPopupView.findViewById(R.id.txt_start_stop);
        txtMic = (TextView) mPopupView.findViewById(R.id.txt_mic);
        txtMark = (TextView) mPopupView.findViewById(R.id.txt_mark);

        int stopId = Resources.getSystem().getIdentifier("lockscreen_transport_stop_description", "string", "android");
        String strStop = getContext().getString(stopId);
        txtStartStop.setText(strStop);

        txtStartStop.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        txtStartStop.setSingleLine(true);
        txtStartStop.setSelected(true);
        txtStartStop.setFocusable(true);
        txtStartStop.setFocusableInTouchMode(true);

        int micId = Resources.getSystem().getIdentifier("permgrouplab_microphone", "string", "android");
        String strMic = getContext().getString(micId);
        txtMic.setText(strMic);

        txtMic.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        txtMic.setSingleLine(true);
        txtMic.setSelected(true);
        txtMic.setFocusable(true);
        txtMic.setFocusableInTouchMode(true);

        txtMark.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        txtMark.setSingleLine(true);
        txtMark.setSelected(true);
        txtMark.setFocusable(true);
        txtMark.setFocusableInTouchMode(true);

        btnStartStopRecord = mPopupView.findViewById(R.id.btn_start_stop);
        btnAudioRecord = mPopupView.findViewById(R.id.btn_voice_control);
        btnMarkRecord = mPopupView.findViewById(R.id.btn_mark_control);

        btnStartStopRecord.setOnClickListener(clickListener);
        btnAudioRecord.setOnClickListener(clickListener);
        btnMarkRecord.setOnClickListener(clickListener);
    }

    public interface CallBack{

        void onPopClicked(int msg);
    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()) {

                case R.id.btn_mark_control:
                    Log.d(TAG, "mark button clicked.");
                    if (isMarkOn){
                        hideDrawingBoard();
                    }
                    else {
                        showDrawingBoard();
                    }
                    break;
                case R.id.btn_start_stop:
                    callBack.onPopClicked(MSG_START_STOP);
                    break;

                case R.id.btn_voice_control:
                    callBack.onPopClicked(MSG_VOICE_MUTE);
                    break;
                default:

                    break;
            }
        }
    };

    public void setVoiceMuteDrawableOff(boolean flag){
        if (flag){
            mAudioRecord.setImageResource(R.drawable.record_voice_off);
        }else {
            mAudioRecord.setImageResource(R.drawable.record_voice_on);
        }
    }

    public void setStartStopDrawableOn(boolean flag){
        if (flag){
            mStartStopRecord.setImageResource(R.drawable.record_off_on);
        }else {
            mStartStopRecord.setImageResource(R.drawable.record_off);
        }
    }



    public void setCallBack(CallBack callBack){

        this.callBack = callBack;
    }

    private void showDrawingBoard(){

        WindowManager.LayoutParams drawingBoardViewLayoutPara = new WindowManager.LayoutParams();

        drawingBoardViewLayoutPara.type = WindowManager.LayoutParams.TYPE_PHONE;
        drawingBoardViewLayoutPara.height = WindowManager.LayoutParams.MATCH_PARENT;
        drawingBoardViewLayoutPara.width = WindowManager.LayoutParams.MATCH_PARENT;
        drawingBoardViewLayoutPara.flags = drawingBoardViewLayoutPara.flags
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        drawingBoardViewLayoutPara.alpha=1F;
        drawingBoardViewLayoutPara.format=PixelFormat.RGBA_8888;   //let window background transparent.
        drawingBoardViewLayoutPara.screenBrightness=1F;
        drawingBoardViewLayoutPara.gravity = Gravity.START | Gravity.TOP;

        Log.d(TAG, "display drawing board. ");

        if (drawingBoardView == null)
            drawingBoardView = new DrawingBoardView(this.getContext());

        getWindowManager().addView(drawingBoardView, drawingBoardViewLayoutPara);
        mMarkRecord.setImageResource(R.drawable.mark_on);

        drawingBoardView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
        isMarkOn = true;

        mHandler.sendEmptyMessageDelayed(MSG_DRAWING_HIDE, 3000);  // remove drawing board 3 seconds after, let drawing lines clear.
    }

    private void hideDrawingBoard(){

        Log.d(TAG, "hide drawing board.");
        if (isMarkOn == true) {
            try {
                getWindowManager().removeView(drawingBoardView);
                mMarkRecord.setImageResource(R.drawable.mark_off);
            } catch (Exception e) {
                Log.w(TAG, "remove drawing board failed. ");
                e.printStackTrace();
            }
        }
        drawingBoardView = null;
        isMarkOn = false;
    }

    /**
     * 窗口监听类回调接口
     */
    class WindowTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch(event.getAction()) {
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
                        turnMini();
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
        downX = event.getRawX();
        downY = event.getRawY();

        downInX = event.getX();
        downInY = event.getY();

        getLayoutParams().alpha = 1.0f;

        getWindowManager().updateViewLayout(getContentView(), getLayoutParams());

    }

    /**
     * 移动事件
     *
     * @param event
     */
    private void move(MotionEvent event){
        float newMoveX = event.getRawX();
        float newMoveY = event.getRawY();

        getLayoutParams().x = (int)(newMoveX - downInX);
        getLayoutParams().y = (int)(newMoveY - downInY - offsetY);

        getWindowManager().updateViewLayout(getContentView(), getLayoutParams());

        Log.d(TAG, "down x y "+ event.getX()+ " " + event.getY());
        Log.d(TAG, "down raw x y "+event.getRawX()+" "+event.getRawY());
    }

    /**
     * 手指弹起的时候的事件
     *
     * @param event
     */
    private void up(MotionEvent event) {

        Log.d(TAG, "up: ");

        float x = event.getRawX();
        float y = event.getRawY();

        if(x >= downX - DISTANCE && x <= downX + DISTANCE && y >= downY - DISTANCE &&
                y <= downY + DISTANCE) {
            Log.d(TAG, "up: Show popupView");
            if (mIsOpen){
                Log.d(TAG, "mIsOpen true, now turnMini. ");
                turnMini();
            }else {
                Log.d(TAG, "mIsOpen false, now turnDetail. ");
                turnDetail();
            }
        } else {
            //给一个动画去贴边
            Log.d(TAG, "up: go back");
            ValueAnimator animator = alignAnimator(x, y);
            animator.start();
        }
    }

    /**
     *
     */
    public void turnDetail(){
        detailView.setVisibility(View.VISIBLE);
        imgDetailSign.setImageResource(R.drawable.detail_on);
        getLayoutParams().width = Utils.dip2px(getContext(), 180);
        floatViewLayout.setBackgroundResource(R.drawable.floating_view_bg);
        getWindowManager().updateViewLayout(getContentView(), getLayoutParams());
        mIsOpen = true;

        Log.d(TAG, "getMeasuredWidth getMeasuredHeight "+getContentView().getMeasuredWidth() + " "+getContentView().getMeasuredHeight());
        Log.d(TAG, "getWidth getHeight "+getContentView().getWidth()+" "+getContentView().getHeight());
    }

    public void turnMini(){
        detailView.setVisibility(View.GONE);
        imgDetailSign.setImageResource(R.drawable.detail_off);
        getLayoutParams().width = Utils.dip2px(getContext(), 114);
        floatViewLayout.setBackgroundResource(R.drawable.floating_view_bg_small);

        DisplayMetrics displayMetrics = getDisplayMetrics();
        int width = displayMetrics.widthPixels;
        int height = displayMetrics.heightPixels;

        if (alignBoundary == ALIGN_END){
            getLayoutParams().x = width;
            Log.d(TAG, "up to width "+width);
            Log.d(TAG, "align end. ");
        }else if (alignBoundary == ALIGN_BOTOOM){
            getLayoutParams().y = height;
            Log.d(TAG, "up to height "+height);
            Log.d(TAG, "align bottom. ");
        }

        getWindowManager().updateViewLayout(getContentView(), getLayoutParams());
        mIsOpen = false;

        Log.d(TAG, "getMeasuredWidth getMeasuredHeight "+getContentView().getMeasuredWidth() + " "+getContentView().getMeasuredHeight());
        Log.d(TAG, "getWidth getHeight "+getContentView().getWidth()+" "+getContentView().getHeight());
    }

    /**
     * 更新窗口的位置
     */
    private void updateLocation() {

        if(getContentView() != null) {
            getWindowManager().updateViewLayout(mContentView, getLayoutParams());
        }
    }


    /**
     * 自动对齐的一个小动画（自定义属性动画），使自动贴边的时候显得不那么生硬
     */
    private ValueAnimator alignAnimator(float x, final float y) {
        ValueAnimator animator = null;

        int width, height;

        DisplayMetrics displayMetrics = getDisplayMetrics();

        width = displayMetrics.widthPixels;
        height = displayMetrics.heightPixels;

        int nowPosX = getLayoutParams().x;
        int nowPosY = getLayoutParams().y;

        offsetX = getContentView().getMeasuredWidth() /2;

        Log.d(TAG, "width height "+width + " "+ height);

        if (y <= height * 0.15) {
            animator = ValueAnimator.ofObject(new PointEvaluator(), new Point(nowPosX, nowPosY), new Point(nowPosX, 0));
            alignBoundary = ALIGN_NONE;

            Log.d(TAG, "align top. ");
        }else if (y >= height * 0.85){
            alignBoundary = ALIGN_BOTOOM;

            animator = ValueAnimator.ofObject(new PointEvaluator(), new Point(nowPosX, nowPosY), new Point(nowPosX , height));
            Log.d(TAG, "align bottom. ");
        }else if(x < width / 2) {

            alignBoundary = ALIGN_NONE;
            animator = ValueAnimator.ofObject(new PointEvaluator(), new Point(nowPosX, nowPosY), new Point(0, nowPosY));
            Log.d(TAG, "align start. ");
        } else {

            alignBoundary = ALIGN_END;
            animator = ValueAnimator.ofObject(new PointEvaluator(), new Point(nowPosX, nowPosY), new Point(width, nowPosY));
            Log.d(TAG, "align end. ");
        }
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Point point = (Point) animation.getAnimatedValue();
//                Log.d(TAG, "update point x y" +point.x+" "+point.y);
                getLayoutParams().x = point.x;
                getLayoutParams().y = point.y;
                updateLocation();
            }
        });

        animator.setDuration(160);
        return animator;
    }


    /**
     * 动画差值器
     */
    public class PointEvaluator implements TypeEvaluator {

        @Override
        public Object evaluate(float fraction, Object from, Object to) {
            Point startPoint = (Point) from;
            Point endPoint = (Point) to;
            float x = startPoint.x + fraction * (endPoint.x - startPoint.x);
            float y = startPoint.y + fraction * (endPoint.y - startPoint.y);
            Point point = new Point((int) x, (int) y);
            return point;
        }
    }

    /**
     * 获取状态栏的高度
     */
    private int getStatusBarHeight(Context context) {
        int height = 0;
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(resId > 0) {
            height = context.getResources().getDimensionPixelSize(resId);
        }
        return height;
    }


    /**
     * 显示窗口
     */
    public void show() {
        if(getContentView() != null && !getIsShowing()) {
            getWindowManager().addView(getContentView(), getLayoutParams());
            mIsShowing = true;

            startMillions = SystemClock.uptimeMillis();
            mHandler.sendEmptyMessageDelayed(MSG_TIME_UPDATE, 900);
        }
    }

    /**
     * 隐藏当前显示窗口
     */
    public void dismiss() {
        if(getContentView() != null && getIsShowing()) {
            getWindowManager().removeView(getContentView());
            mIsShowing = false;
        }
    }

    /**
     * 获取上下文
     *
     * @return
     */
    private Context getContext() {
        return this.mContext;
    }

    /**
     * 获取WindowManager
     *
     * @return
     */
    private WindowManager getWindowManager() {
        if(mWindowManager == null) {
            mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

    /**
     * 获取当前正在显示的视频
     *
     * @return 当前视图
     */
    public View getContentView() {
        return mContentView;
    }


    /**
     * 判断当前是否有显示窗口
     *
     * @return 有true/没有false
     */
    public boolean getIsShowing() {
        return mIsShowing;
    }

    /**
     * 获取 WindowManager.LayoutParams 参数
     *
     * @return
     */
    public WindowManager.LayoutParams getLayoutParams() {
        if(mLayoutParams == null) {
            mLayoutParams = new WindowManager.LayoutParams();
            initLayoutParams();
        }
        return mLayoutParams;
    }

    /*
            Handler ------------------------------------------------------------------------
     */
    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case MSG_DRAWING_HIDE:
                    if (mHandler.hasMessages(MSG_DRAWING_HIDE)){
                        mHandler.removeMessages(MSG_DRAWING_HIDE);
                    }
                    hideDrawingBoard();
                    break;
                case MSG_TIME_UPDATE:
                    seconds = (SystemClock.uptimeMillis() - startMillions)/1000;
                    mTvShowShotTime.setText(Utils.getRecordingTime(seconds));
                    mHandler.sendEmptyMessageDelayed(MSG_TIME_UPDATE, 900);
                    break;
            }
        }
    };

}
