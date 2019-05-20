package com.duohuan.billing;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.Pwm;
import com.google.gson.Gson;

import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

    private static boolean isOpen = false;
    private static PeripheralManager manager = PeripheralManager.getInstance();
    private static final String DUO_JI = "PWM1";
    private static final String JI_GUANG = "PWM0";
    private static Gson gson = new Gson();

    public interface PwmListener {
        void onComplete(String str);
    }

    public synchronized static void startDevice(PwmListener listener) {
        RequestEntity entity = new RequestEntity();
        entity.setMode(999);
        if (isOpen) {
            entity.setErrorCode(1);
            entity.setErrorMessage("设备正在运行");
            listener.onComplete(gson.toJson(entity));
            return;
        }
        isOpen = true;
        Observable.zip(openDuoJi(), openJiGuang(), (duoji, jiguang) -> {
            jiguang.setPwmDutyCycle(0);
            startDuoJi(duoji);
            jiguang.setPwmDutyCycle(100);
            entity.setErrorCode(0);
            entity.setErrorMessage("运行完成");
            return entity;
        }).subscribeOn(Schedulers.newThread())
                .subscribe(str -> {
                    if (listener != null) {
                        listener.onComplete(gson.toJson(entity));
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (listener != null) {
                        entity.setErrorCode(2);
                        entity.setErrorMessage(throwable.getMessage());
                        listener.onComplete(gson.toJson(entity));
                    }
                }, () -> {
                    Log.i(TAG, "运行完毕");
                    isOpen = !isOpen;
                });
    }

    private static final float INIT_ROTATE = 47;
    private static float rotate = INIT_ROTATE;

    private static void startDuoJi(Pwm pwm) throws IOException, InterruptedException {
        for (int i = 0; i < 20; i++) {
            rotate += 1.1;
            pwm.setPwmDutyCycle(rotate);
            Thread.sleep(50);
        }
        rotate = INIT_ROTATE;
        pwm.setPwmDutyCycle(rotate);
    }

    private static Pwm duo;
    private static Pwm jig;

    private static Observable<Pwm> openDuoJi() {
        if (duo == null) {
            return Observable.defer(() -> {
                duo = manager.openPwm(DUO_JI);
                duo.setPwmFrequencyHz(330);
                duo.setPwmDutyCycle(rotate);
                duo.setEnabled(true);
                return Observable.just(duo);
            });
        } else {
            return Observable.just(duo);
        }
    }

    private static Observable<Pwm> openJiGuang() {
        if (jig == null) {
            return Observable.defer(() -> {
                jig = manager.openPwm(JI_GUANG);
                jig.setPwmFrequencyHz(330);
                jig.setPwmDutyCycle(10);
                jig.setEnabled(true);
                return Observable.just(jig);
            });
        } else {
            return Observable.just(jig);
        }
    }
}
