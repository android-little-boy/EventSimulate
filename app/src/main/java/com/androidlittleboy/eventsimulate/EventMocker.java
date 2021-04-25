package com.androidlittleboy.eventsimulate;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;


public class EventMocker {
    private static final String TAG = "EventMocker";

    private static final int ACTION_TOUCH_CLICK = 1;
    private static final int ACTION_TOUCH_DOWN = 2;
    private static final int ACTION_TOUCH_MOVE = 3;
    private static final int ACTION_TOUCH_UP = 4;
    private static final int ACTION_BTN_BACK = 5;

    private static SubHandler<MyEvent> subHandler;

    public static void init() {
        if (subHandler == null) {
            subHandler = new MySubHandler();
        }
        subHandler.init();
    }

    public static void unInit() {
        subHandler.unInit();
        subHandler = null;
    }

    public static void simulateClick(int x, int y) {
        subHandler.putMessage(new MyEvent(ACTION_TOUCH_CLICK, x, y));
    }

    public static void simulatePress(int x, int y) {
        subHandler.putMessage(new MyEvent(ACTION_TOUCH_DOWN, x, y));
    }

    public static void simulateUp(int x, int y) {
        subHandler.putMessage(new MyEvent(ACTION_TOUCH_UP, x, y));
    }

    public static void simulateMove(int x, int y) {
        subHandler.putMessage(new MyEvent(ACTION_TOUCH_MOVE, x, y));
    }

    public static void simulateBtnBack() {
        subHandler.putMessage(new MyEvent(ACTION_BTN_BACK));
    }

    public static void simulateBtnHome(Context context) {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        //??
        //homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        context.startActivity(homeIntent);
    }

    public static class MyEvent {
        private int action;
        private int x, y;

        public MyEvent(int touchAction, int x, int y) {
            this.action = touchAction;
            this.x = x;
            this.y = y;
        }

        public MyEvent(int btnAction) {
            this.action = btnAction;
        }
    }


    private static class MySubHandler extends SubHandler<MyEvent> {
        @Override
        public void handleMessage(MyEvent event) {
                try {
                    Instrumentation inst = new Instrumentation();

                    switch (event.action) {
                        case ACTION_TOUCH_CLICK:
                            inst.sendPointerSync(
                                    MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                            MotionEvent.ACTION_DOWN, event.x, event.y, 0));    //x,y 即是事件的坐标
                            Thread.sleep(20);
                            inst.sendPointerSync(
                                    MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                            MotionEvent.ACTION_UP, event.x, event.y, 0));
                            return;
                        case ACTION_TOUCH_DOWN:
                            inst.sendPointerSync(
                                    MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                            MotionEvent.ACTION_DOWN, event.x, event.y, 0));
                            return;
                        case ACTION_TOUCH_MOVE:
                            inst.sendPointerSync(
                                    MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                            MotionEvent.ACTION_MOVE, event.x, event.y, 0));
                            return;
                        case ACTION_TOUCH_UP:
                            inst.sendPointerSync(
                                    MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                                            MotionEvent.ACTION_UP, event.x, event.y, 0));
                            return;
                        case ACTION_BTN_BACK:
                            inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
                            return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "handleMessage: ",e);
                    e.printStackTrace();
                }
        }
    }
}
