package com.androidlittleboy.eventsimulate;

import android.os.Handler;
import android.os.HandlerThread;

public abstract class SubHandler<T extends EventMocker.MyEvent> {
    private Handler eventHandler;

    public void init() {
        HandlerThread handlerThread = new HandlerThread("eventWorker");
        handlerThread.start();
        eventHandler = new Handler(handlerThread.getLooper());
    }

    abstract void handleMessage(T t);


    public void putMessage(T t) {
        eventHandler.post(() -> handleMessage(t));
    }

    public void unInit() {
        eventHandler.getLooper().quit();
    }
}