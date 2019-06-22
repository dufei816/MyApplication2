package com.duohuan.billing;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.gson.Gson;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

/**
 * 　　　　　　　┏┛┻━━━┛┻┓ + +
 * 　　　　　　　┃　　　　　　　┃
 * 　　　　　　　┃　　　━　　　┃ ++ + + +
 * 　　　　　　 ████━████ ┃+
 * 　　　　　　　┃　　　　　　　┃ +
 * 　　　　　　　┃　　　┻　　　┃
 * 　             ┃　　　　　　　┃ + +
 * 　　　　　　　┗━┓　　　┏━┛
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┃ + + + +
 * 　　　　　　　　　┃　　　┃　　　　Code is far away from bug with the animal protecting
 * 　　　　　　　　　┃　　　┃ + 　　　　神兽保佑,代码无bug
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┃　　+
 * 　　　　　　　　　┃　 　　┗━━━┓ + +
 * 　　　　　　　　　┃ 　　　　　　　┣┓
 * 　　　　　　　　　┃ 　　　　　　　┏┛
 * 　　　　　　　　　┗┓┓┏━┳┓┏┛ + + + +
 * 　　　　　　　　　　┃┫┫　┃┫┫
 * 　　　　　　　　　　┗┻┛　┗┻┛+ + + +
 * 创建人: 杜
 * 日期: 2019/5/13
 * 时间: 16:03
 */
@SuppressLint("CheckResult")
public class DeviceMode {
    private static final String TAG = "DeviceMode";
    private static final Object obj = new Object();
    private boolean init = false;
    private boolean isOpen = false;
    private PeripheralManager manager;
    private static final String DUO_JI = "PWM1";
    private static final String JI_GUANG = "PWM0";
    private Gson gson = new Gson();
    private Pwm duo;
    private Pwm jig;

    private static final float INIT_ROTATE = 51;
    private static float rotate = INIT_ROTATE;

    private static DeviceMode deviceMode1;


    private DeviceMode() {
        init();
    }

    private void init() {
        Observable.just("")
                .subscribeOn(Schedulers.newThread())
                .subscribe(s -> {
                    manager = PeripheralManager.getInstance();
                    duo = manager.openPwm(DUO_JI);
                    duo.setPwmFrequencyHz(330);
                    duo.setPwmDutyCycle(rotate);
                    duo.setEnabled(true);

                    jig = manager.openPwm(JI_GUANG);
                    jig.setPwmFrequencyHz(330);
                    jig.setPwmDutyCycle(0);
                    jig.setEnabled(true);

                    init = true;
                    Log.e(TAG, "Init Success!");
                }, error -> {
                    init = false;
                    error.printStackTrace();
                });
    }


    public static DeviceMode getInstance() {
        if (deviceMode1 == null) {
            synchronized (obj) {
                if (deviceMode1 == null) {
                    deviceMode1 = new DeviceMode();
                }
            }
        }
        return deviceMode1;
    }

    public void close() {
        Observable.just("")
                .subscribeOn(Schedulers.newThread())
                .subscribe(str -> {
                    if (duo != null) {
                        duo.close();
                    }
                    if (jig != null) {
                        jig.close();
                    }
                }, Throwable::printStackTrace);
    }

    public synchronized void startDevice(RequestEntity entity, DeviceListener listener) {
        entity.setNumber(entity.getNumber() + 1);
        if (isOpen) {
            entity.setErrorCode(Config.LASER_RUNNING_ERROR);
            entity.setErrorMessage(Config.LASER_RUNNING_ERROR_MSG);
            listener.onComplete(gson.toJson(entity));
            return;
        }
        if (!init) {
            entity.setErrorCode(Config.LASER_ERROR);
            entity.setErrorMessage("初始化异常！");
            listener.onComplete(gson.toJson(entity));
            return;
        }
        isOpen = true;
        Observable.just("")
                .subscribeOn(Schedulers.newThread())
                .subscribe(s -> {
                    jig.setPwmDutyCycle(100);
                    startDuoJi(duo);
                    jig.setPwmDutyCycle(0);
                    entity.setErrorCode(Config.SUCCESS);
                    entity.setErrorMessage(Config.SUCCESS_MSG);
                    listener.onComplete(gson.toJson(entity));
                }, error -> {
                    entity.setErrorCode(Config.LASER_ERROR);
                    entity.setErrorMessage(error.getMessage());
                    listener.onComplete(gson.toJson(entity));
                }, () -> isOpen = false);
    }

    //    private static final float INIT_ROTATE = 47;


    private void startDuoJi(Pwm pwm) throws IOException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            rotate += 1.1;
            pwm.setPwmDutyCycle(rotate);
            Thread.sleep(50);
        }
        rotate = INIT_ROTATE;
        pwm.setPwmDutyCycle(rotate);
    }

}
