package cn.max.screenrecord.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import static cn.max.screenrecord.utils.Utils.__TAG__;

/**
 * Created by geyu on 17-11-6.
 */

public class DrawingBoardView extends View {

    private static final String TAG = __TAG__();
    public float currentx=40;
    public float currenty=50;
    public float prex;
    public float prey;
    //之前坐标
    private Path mpath; //定义路径
    public Paint p; //定义画笔

    private Context mContext;
    private boolean inDraw = false;


    public DrawingBoardView(Context context) {
        super(context);
        Log.d(TAG, "construction DrawingBoardView0");
        init(context);
    }

    public DrawingBoardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "construction DrawingBoardView1");
        init(context);
    }

    public DrawingBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.d(TAG, "construction DrawingBoardView2");
        init(context);
    }

    public void init(Context context){

        mContext=context;
        mpath = new Path();
        mpath.reset();
        p= new Paint();
        p.setColor(Color.RED);
        p.setStyle(Paint.Style.STROKE);//将画笔设置为空心，才会画成曲线
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeWidth(12);//设置画笔宽度
        p.setAntiAlias(true);
    }


    @Override
    public void onDraw(Canvas canvas){
        super.onDraw(canvas);

        if (inDraw){
            Paint m=new Paint();
            m.setColor(Color.BLUE);
            canvas.drawCircle(currentx,currenty,15,m);
            canvas.drawPath(mpath,p);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {   //不断触发触摸屏事件，将监听到的信息都传入DrawView 组件
        inDraw = true;
        currentx=event.getX();
        currenty=event.getY();
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mpath.moveTo(currentx,currenty);
                prex = currentx;
                prey = currenty;
                break;
            case MotionEvent.ACTION_MOVE:
                mpath.quadTo(prex,prey,currentx,currenty);
                prex = currentx;
                prey = currenty;
                break;
            case MotionEvent.ACTION_UP:
//                mpath.reset();
                break;
        }
        invalidate();
        return true;

    }
}
