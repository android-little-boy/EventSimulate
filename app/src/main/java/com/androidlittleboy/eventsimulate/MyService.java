package com.androidlittleboy.eventsimulate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class MyService extends Service {
    private MyReceiver myReceiver;
    private static final String TAG = "MyService";
    private MediaProjection mediaProjection;
    private int width;
    private int height;
    private int mScreenDensity;
    private ImageReader mImageReader;

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        WifiHotspot.getWifiHotspot().init(this,callback);
        return new MyBinder();
    }

    public class MyBinder extends Binder {
        public void setMediaProjection(MediaProjection mediaProjection) {
            MyService.this.mediaProjection = mediaProjection;
            initScreenHelper();
            mediaProjection.createVirtualDisplay("AnyChatScreenHelper",
                    width, height, mScreenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        }
    }

    private WifiHotspot.Callback callback = new WifiHotspot.Callback() {
        @Override
        public void onEvent(String event) {
           String[] eventSt = event.split(";");
           switch (eventSt[0]){
               case "1":
                   EventMocker.simulateClick(Integer.parseInt(eventSt[1]),Integer.parseInt(eventSt[2]));
                   break;
               case "2":
                   EventMocker.simulatePress(Integer.parseInt(eventSt[1]),Integer.parseInt(eventSt[2]));
                   break;
               case "3":
                   EventMocker.simulateUp(Integer.parseInt(eventSt[1]),Integer.parseInt(eventSt[2]));
                   break;
               case "4":
                   EventMocker.simulateMove(Integer.parseInt(eventSt[1]),Integer.parseInt(eventSt[2]));
                   break;
               case "5":
                   EventMocker.simulateBtnBack();
                   break;
               case "6":
                   EventMocker.simulateBtnHome(MyService.this);
                   break;
           }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        myReceiver = new MyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON); //亮屏
        filter.addAction(Intent.ACTION_SCREEN_OFF); //锁屏、黑屏
        this.registerReceiver(myReceiver, filter);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel mChannel = null;
            mChannel = new NotificationChannel("1232", "远程投屏控制服务", NotificationManager.IMPORTANCE_HIGH);
            //使通知静音
            mChannel.setSound(null, null);
            notificationManager.createNotificationChannel(mChannel);
            Notification notification = new Notification.Builder(getApplicationContext(), "1232")
                    .setContentTitle("投屏控制服务")
                    .build();
            startForeground(1, notification);
        }
        EventMocker.init();
    }


    public void initScreenHelper() {
//获取到屏幕的宽高
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
//整个屏幕长宽
        windowManager.getDefaultDisplay().getRealSize(point);

        width = point.x;
        height = point.y;
        Log.d(TAG, "initScreenHelper: w:"+width+",h:"+height);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        this.mScreenDensity = displayMetrics.densityDpi / 3;


        this.mImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 5);
        this.mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Log.d(TAG, "onImageAvailable: " + reader.getHeight());
                handleCapture(reader);
            }
        }, null);

    }

    private void handleCapture(ImageReader reader) {

        Image image = reader.acquireLatestImage();
        if (image == null) {
            return;
        }
        int width = image.getWidth();
        int height = image.getHeight();
        Log.d(TAG, "handleCapture: w:"+width+",h:"+height);
        final Image.Plane[] planes = image.getPlanes();
        if (planes == null) {
            image.close();
            return;
        }
        final ByteBuffer buffer = planes[0].getBuffer();
        if (buffer == null) {
            image.close();
            return;
        }
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * width;
        Bitmap mBitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
        mBitmap.copyPixelsFromBuffer(buffer);
        mBitmap = Bitmap.createBitmap(mBitmap, 0, 0, width, height);
        image.close();
        if (mBitmap != null) {//将数据转换为数据流
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            byte[] datas = bos.toByteArray();
//            int bytes = mBitmap.getByteCount();
//            ByteBuffer buf = ByteBuffer.allocate(bytes);
//            mBitmap.copyPixelsToBuffer(buf);
            if (!mBitmap.isRecycled()) {
                mBitmap.recycle();
                mBitmap = null;
            }
//            if (buf == null) {
//                return;
//            }
//            byte[] datas = buf.array();
//            if (datas == null || datas.length == 0) {
//                return;
//            }
            WifiHotspot.getWifiHotspot().write(datas);
        }
    }


    class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_SCREEN_OFF)) //锁屏、黑屏
            {
                //启动保活Activity
                Log.d(TAG, "onReceive:ACTION_SCREEN_OFF ");
//                Intent aIntent = new Intent(MyService.this, KeepAlive_Activity.class);
//                aIntent.addFlags(FLAG_ACTIVITY_NEW_TASK);
//                MyService.this.startActivity(aIntent);
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) //亮屏
            {
                Log.d(TAG, "onReceive: ACTION_SCREEN_ON");
                //移除保活Activity
//                EventBus.getDefault().post(new KeepAlive_Activity.MessageEvent(Event_Finish_KeepAliveActivity));
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (myReceiver != null) {
            this.unregisterReceiver(myReceiver);
            myReceiver = null;
        }
        stopForeground(true);
        EventMocker.unInit();
        WifiHotspot.getWifiHotspot().unInit();
    }
}