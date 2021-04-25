package com.androidlittleboy.eventsimulate;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class MyService extends Service {
    private MyReceiver myReceiver;
    private static final String TAG = "MyService";

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

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

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        WifiHotspot.getWifiHotspot().init(this);

        return super.onStartCommand(intent, flags, startId);
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
    }
}