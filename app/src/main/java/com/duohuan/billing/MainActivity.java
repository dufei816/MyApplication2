package com.duohuan.billing;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.gson.Gson;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
public class MainActivity extends Activity {
    private static final String TAG = "Things";
    private static final String PATH = "/storage/emulated/0/Download/";
    private static final String DUO_JI = "PWM1";

    private static Gson gson = new Gson();

//    private PlayerView playerView1;

    private static List<WebSocket> _sockets = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {//default frame rate
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        playerView1 = findViewById(R.id.player_view1);
//        JzvdStd jzvdStd = findViewById(R.id.videoplayer);
//        jzvdStd.setUp("http://jzvd.nathen.cn/c6e3dc12a1154626b3476d9bf3bd7266/6b56c5f0dc31428083757a45764763b0-5287d2089db37e62345123a1be272f8b.mp4"
//                , "饺子闭眼睛" , Jzvd.SCREEN_WINDOW_NORMAL);
//        jzvdStd.thumbImageView.setImage("http://p.qpic.cn/videoyun/0/2449_43b6f696980311e59ed467f22794e792_1/640");

//
//        SimpleExoPlayer player = ExoPlayerFactory.newSimpleInstance(this);
//        playerView1.setPlayer(player);
//        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this,
//                Util.getUserAgent(this, "App"));
//        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
//                .createMediaSource(Uri.fromFile(new File(PATH, "test2.mp4")));
//        player.prepare(videoSource);
//
//        player.setPlayWhenReady(true);
        startServer(5000);
    }

    private WebSocket.StringCallback callback = str -> {
        RequestEntity entity = gson.fromJson(str, RequestEntity.class);
        if (entity.getMode() == 0) {
            DeviceMode.startDevice(message -> {
                for (WebSocket socket : _sockets) {
                    socket.send(message);
                }
            });
        }
    };

    private void startServer(int port) {
        AsyncHttpServer httpServer = new AsyncHttpServer();
        httpServer.setErrorCallback(ex -> Log.e("WebSocket", "An error occurred", ex));
        httpServer.listen(AsyncServer.getDefault(), port);

        httpServer.websocket("/ws", (webSocket, request) -> {
            _sockets.add(webSocket);
            webSocket.send("收到");
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
    }
}
