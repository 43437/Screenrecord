package cn.max.screenrecord.recorder;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import static cn.max.screenrecord.utils.Utils.__TAG__;

public class MediarecorderIml {

    private static MediarecorderIml instanceIml;

    private MediaProjection mediaProjection;
    private MediaRecorder mediaRecorder;
    private VirtualDisplay virtualDisplay;

    private int videoWidth = 720;
    private int videoHeight = 1080;
    private int densityDpi;
    private boolean running;
    private CallBack mCallBack;

    private Context mContext;

    private boolean initWellDone = false;

    private Bitmap snapshot;

    private ImageReader ImageReader;

    private static final String TAG = __TAG__();

    private MediarecorderIml() {
        this.mediaRecorder = new MediaRecorder();
    }

    public static MediarecorderIml getInstance() {
        if (instanceIml == null)
            synchronized (MediarecorderIml.class) {
                if (instanceIml == null)
                    instanceIml = new MediarecorderIml();
            }
        return instanceIml;
    }

    public void initProjection(Context context, int resultCode, Intent data) {
        this.mContext = context;
        MediaProjectionManager projectionManager = (MediaProjectionManager) context.getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();

        videoWidth = metrics.widthPixels;
        videoHeight = metrics.heightPixels;

        densityDpi = metrics.densityDpi;
    }

    public interface CallBack {
        void onErro(Exception e);

        void onStop();

        void onStarted();
    }

    public void startRecord(String mVideoFilePath, CallBack callBack) {
        this.mCallBack = callBack;
        if (mediaProjection == null || running) {
            if (mCallBack != null && mediaProjection == null)
                mCallBack.onErro(new RuntimeException("mediaProjection = " + mediaProjection));
            return;
        }

        initRecorder(mVideoFilePath);

        if (initWellDone) {
            Log.d(TAG, "recorder initWellDone. ");
            createOrUpdateVirtualDisplay();
            mediaRecorder.start();

            running = true;
        }
    }

    public boolean getIsRunning() {
        return running;
    }

    private Context getContext() {
        return this.mContext;
    }

    private void initRecorder(String mVideoFilePath) {

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(mVideoFilePath);

        mediaRecorder.setVideoSize(videoWidth, videoHeight);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 768);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
            initWellDone = true;
            if (mCallBack != null){
                Log.d(TAG, "success start record. ");
                mCallBack.onStarted();
            }
        } catch (IOException e) {
            Log.w(TAG, "MediaRecord prepare failed. ");
            e.printStackTrace();
            if (mCallBack != null)
                mCallBack.onErro(e);
        }
    }

    private void createOrUpdateVirtualDisplay() {

        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen",
                videoWidth, videoHeight, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(), null, null);
    }

    public void stopRecord() {
        if (!running) {
            return;
        }
        try {
            running = false;

            mediaRecorder.setOnErrorListener(null);
            mediaRecorder.setOnInfoListener(null);

            mediaRecorder.stop();
            mediaRecorder.release();

            virtualDisplay.release();


            mediaRecorder = null;
            instanceIml = null;
        } catch (Exception e) {
            Log.w(TAG, "stop record failed. ");
            e.printStackTrace();
        }

        exitReturnSnapshot();
    }

    public void errorRelease() {
        mediaRecorder.release();
        mediaProjection.stop();
        mediaRecorder = null;
        instanceIml = null;
    }

    public Bitmap getSnapshot() {
        return this.snapshot;
    }

    private void exitReturnSnapshot() {

        ImageReader = ImageReader.newInstance(videoWidth, videoHeight, PixelFormat.RGBA_8888, 1);
        final VirtualDisplay virtualDisplaySnap = mediaProjection.createVirtualDisplay("screen-mirror", videoWidth, videoHeight, densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR
                , ImageReader.getSurface(), null, null);

        Log.d(TAG, "begin snapshot " + SystemClock.uptimeMillis());
        ImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "get snapshot " + SystemClock.uptimeMillis());
                Image image = reader.acquireLatestImage();

                Log.w(TAG, "image is null " + (image == null));
                int width = image.getWidth();
                int height = image.getHeight();
                final Image.Plane[] planes = image.getPlanes();
                final ByteBuffer buffer = planes[0].getBuffer();
                //每个像素的间距
                int pixelStride = planes[0].getPixelStride();
                //总的间距
                int rowStride = planes[0].getRowStride();
                int rowPadding = rowStride - pixelStride * width;
                Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height,
                        Bitmap.Config.ARGB_8888);
                bitmap.copyPixelsFromBuffer(buffer);
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                image.close();

                ImageReader.setOnImageAvailableListener(null, new Handler());

                snapshot = bitmap;

                virtualDisplaySnap.release();
                mediaProjection.stop();

                if (mCallBack != null) {
                    mCallBack.onStop();
                }
            }
        }, new Handler());
    }
}
