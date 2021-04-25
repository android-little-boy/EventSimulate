package com.androidlittleboy.eventsimulate;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class KeepAlive_Activity extends AppCompatActivity {
public static final int Event_Finish_KeepAliveActivity = 111;
    private static final String TAG = "KeepAlive_Activity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //只有左上角的一个点，主要为了使用户无感知
        Window window = getWindow();
        window.setGravity(Gravity.LEFT | Gravity.TOP);

        WindowManager.LayoutParams params = window.getAttributes();
        params.x = 0;
        params.y = 0;
        params.height = 1;
        params.width = 1;
        window.setAttributes(params);

        //通过EventBus来接收消息
        EventBus.getDefault().register(this);
        Log.d(TAG, "onCreate: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        Log.d(TAG, "onDestroy: ");
    }

    // EventBus ------------------------------
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(MessageEvent msg) {
        int msgCode = msg.msgCode;
        switch (msgCode) {
            case Event_Finish_KeepAliveActivity:
                KeepAlive_Activity.this.finish();
                break;

            default:
                break;
        }
    }


    public static class MessageEvent {
        int msgCode;

        public MessageEvent(int msgCode) {
            this.msgCode = msgCode;
        }
    }
}