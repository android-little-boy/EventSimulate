package com.androidlittleboy.eventsimulate;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.ResultReceiver;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import static android.content.Context.WIFI_SERVICE;

public class WifiHotspot {
    public static final int DEVICE_CONNECTING = 1;//有设备正在连接热点
    public static final int DEVICE_CONNECTED = 2;//有设备连上热点
    public static final int SEND_MSG_SUCCSEE = 3;//发送消息成功
    public static final int SEND_MSG_ERROR = 4;//发送消息失败
    public static final int GET_MSG = 6;//获取新消息
    private WifiManager wifiManager;
    private static final String TAG = "WifiHotspot";
    private Handler handler;
    private Callback mCallback;
    private final Handler workerHandler;
    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case DEVICE_CONNECTING:
                    connectThread = new ConnectThread(listenerThread.getSocket(), handler);
                    connectThread.start();
                    break;
                case DEVICE_CONNECTED:
                    Log.d(TAG, "设备连接成功");
                    break;
                case SEND_MSG_SUCCSEE:
                    Log.d(TAG, "发送消息成功:");
                    break;
                case SEND_MSG_ERROR:
                    Log.d(TAG, "发送消息失败:" + msg.getData().getString("MSG"));
                    break;
                case GET_MSG:
                    if (msg.getData().getString("event") != null) {
                        String event = msg.getData().getString("event");
                        if (mCallback != null)
                            mCallback.onEvent(event);
                        Log.d(TAG, "收到event消息:" + event);
                    }
                    break;
            }
            return false;
        }
    };

    private WifiHotspot() {
        HandlerThread handlerThread = new HandlerThread("worker");
        handlerThread.start();
        workerHandler = new Handler(handlerThread.getLooper());
    }

    public static WifiHotspot getWifiHotspot() {
        return WifiHotspotFactory.wifiHotspot;
    }

    private static class WifiHotspotFactory {
        static WifiHotspot wifiHotspot = new WifiHotspot();
    }

    public void write(byte[] data) {
        if (connectThread != null) {
            workerHandler.post(() -> connectThread.write(data));
        }
    }

    /**
     * 连接线程
     */
    private ConnectThread connectThread;

    /**
     * 监听线程
     */
    private ListenerThread listenerThread;

    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "被控制者";
    /**
     * 端口号
     */
    private static final int PORT = 54321;

    public void init(Context context, Callback mCallback) {
        this.mCallback = mCallback;
        handler = new Handler(callback);
        wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        /**
         * 先开启监听线程，在开启连接
         */
        listenerThread = new ListenerThread(PORT, handler);
        listenerThread.start();
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    Log.i("ip", "getWifiApIpAddress()" + getWifiApIpAddress());
//                    //本地路由开启通信
//                    String ip = getWifiApIpAddress();
//                    if (ip != null) {
//                    } else {
//                        ip = "192.168.43.1";
//                    }
//                    Socket socket = new Socket(ip, PORT);
//                    connectThread = new ConnectThread(socket, handler);
//                    connectThread.start();
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    Log.e(TAG, "创建通信失败");
//                }
//            }
//        }).start();
//        createWifiHotspot();
    }

    ConnectivityManager mConnectivityManager;

    public void unInit() {
        connectThread.interrupt();
        listenerThread.interrupt();
        workerHandler.getLooper().quit();
        connectThread = null;
        listenerThread = null;
        mCallback = null;
    }


    /**
     * 创建Wifi热点
     */
    private void createWifiHotspot() {
        if (wifiManager.isWifiEnabled()) {
            //如果wifi处于打开状态，则关闭wifi,
            wifiManager.setWifiEnabled(false);
        }
        final WifiConfiguration config = new WifiConfiguration();
        config.SSID = WIFI_HOTSPOT_SSID;
        config.preSharedKey = "123456789";
        config.hiddenSSID = false;
        config.allowedAuthAlgorithms
                .set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        //通过反射调用设置热点

        //192.168.43.59
        //        Log.i("ip", "getWifiApIpAddress()" + getWifiApIpAddress() +
        //                "\n");
        try {
            Method method = wifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManager, config, true);
            if (enable) {
                Log.d(TAG, "热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:123456789");


                //        开启连接线程
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Log.i("ip", "getWifiApIpAddress()" + getWifiApIpAddress()
//                            );
//                            String ip = getWifiApIpAddress();
//                            if (ip != null) {
//                            } else {
//                                //一般Android手机默认路由是
//                                ip = "192.168.43.1";
//                            }
//                            //本地路由开启通信
//                            Socket socket = new Socket(ip, PORT);
//                            connectThread = new ConnectThread(socket, handler);
//                            connectThread.start();
//
//
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                            Log.e(TAG, "创建通信失败");
//                        }
//                    }
//                }).start();
//                Thread.sleep(1000);

                //                listenerThread = new ListenerThread(PORT, handler);
                //                listenerThread.start();
            } else {
                Log.e(TAG, "创建热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "创建热点失败");
        }
    }

    public String getWifiApIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                if (intf.getName().contains("wlan")) {
                    for (Enumeration<InetAddress> enumIpAddr = intf
                            .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress()
                                && (inetAddress.getAddress().length == 4)) {
                            Log.d(TAG, inetAddress.getHostAddress());
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("Main", ex.toString());
        }
        return null;
    }

    /**
     * 关闭WiFi热点
     */
    public void closeWifiHotspot() {
        try {
            Method method = wifiManager.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManager);
            Method method2 = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(wifiManager, config, false);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "热点已关闭");
    }

    public interface Callback {
        void onEvent(String event);
    }
}
