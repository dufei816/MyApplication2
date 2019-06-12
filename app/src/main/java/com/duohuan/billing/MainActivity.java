package com.duohuan.billing;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.gson.Gson;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.leinardi.android.things.driver.hcsr04.Hcsr04;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
@SuppressLint("CheckResult")
public class MainActivity extends Activity {
    private static final String TAG = "Things";
    private static final String PATH = "/storage/emulated/0/Download/";

    private static Gson gson = new Gson();
    private static List<WebSocket> _sockets = new ArrayList<>();
    private static HashMap<String, WebSocket> _mapSockets = new HashMap<>();

    private DeviceMotor motor;


    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {//default frame rate
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startServer(5000);
        Log.e(TAG, "Start App");
        motor = DeviceMotor.getInstance();
        motor.setInitListener(((error, message) -> {
            if (error == 0) {
                Log.e(TAG, "Motor Run Success!");
//                motor.reset();
            } else {
                Log.e(TAG, "Motor Run Error!" + message);
            }
        }));
    }

    private void sendMessage(String message) {
        Log.e(TAG, "send=" + message);
        for (WebSocket socket : _sockets) {
            socket.send(message);
        }
    }

    private WebSocket.StringCallback callback = str -> {
        Log.e(TAG, str);
        RequestEntity entity;
        try {
            entity = gson.fromJson(str, RequestEntity.class);
        } catch (Exception e) {
            return;
        }
        if (entity.getMode() == Config.START_LASER) {
            DeviceMode.startDevice(entity, this::sendMessage);
        } else if (entity.getMode() == Config.START_GUIDE) {
            motor.findFace(this::sendMessage);
        } else if (entity.getMode() == Config.END_GUIDE) {
            motor.stop(this::sendMessage);
        } else if (entity.getMode() == Config.RETURN_ZERO) {
            motor.runZero();
        }
    };

    private void startServer(int port) {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.setErrorCallback(ex -> Log.e("WebSocket", "An error occurred", ex));
        httpServer.listen(AsyncServer.getDefault(), port);
        httpServer.websocket("/ws", (webSocket, request) -> {
            String protocol = request.getHeaders().get("Protocol");
            if (protocol != null && !TextUtils.isEmpty(protocol)) {
                _mapSockets.put(protocol, webSocket);
            }
            _sockets.add(webSocket);
            //Use this to clean up any references to your websocket
            webSocket.setClosedCallback(ex -> {
                try {
                    if (ex != null)
                        Log.e("WebSocket", "An error occurred", ex);
                } finally {
                    _sockets.remove(webSocket);
                }
            });
            webSocket.setStringCallback(callback);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DeviceMode.close();
    }
}
