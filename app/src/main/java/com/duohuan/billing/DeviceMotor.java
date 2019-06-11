package com.duohuan.billing;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.PeripheralManager;
import com.google.gson.Gson;
import com.leinardi.android.things.driver.hcsr04.Hcsr04;

import java.io.IOException;
import java.util.Arrays;

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
 * 日期: 2019/5/27
 * 时间: 19:16
 */
@SuppressLint("CheckResult")
public class DeviceMotor {

    private static DeviceMotor deviceMotor;
    private static final String TAG = "DeviceMotor";

    private static final long MAX_COUNT = (long) (3200 * 8);
    private long current_count = 0;

    private static Gson gson = new Gson();

    private static final String ENA = "BCM17";
    private static final String DIR = "BCM27";
    private static final String PUL = "BCM22";

    private static final String TRING = "BCM5";
    private static final String ECHO = "BCM6";

    private Gpio gpio_ena;
    private Gpio gpio_dir;
    private Gpio gpio_pul;

    private DeviceInitListener initListener;

    //安全机制
    private boolean isSafety;
    //是否在运行中
    private boolean runing;
    //是否是手动停止
    private boolean isManual;
    //是否初始化完成
    private boolean init;

    private RunThread myRunThread = new RunThread(false);
    private RunCountThread runCountThread = null;
    private InitThread initThread = new InitThread();
    private ReadThread readThread = new ReadThread();
    private Hcsr04 hcsr04;


    private class InitThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                gpio_dir.setValue(true);
                while (true) {
                    if (interrupted()) {
                        current_count = 0;
                        break;
                    }
                    gpio_pul.setValue(true);
                    busyWaitMicros(1);
                    gpio_pul.setValue(false);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (true) {
                if (interrupted()) {
                    break;
                }
                int[] value = new int[10];
                for (int i = 0; i < 10; i++) {
                    value[i] = (int) hcsr04.readDistance();
                }
                int distance = Arrays.stream(value).sum() / 10;
//                Log.e(TAG, "distance=" + distance);
                if (distance < 15) {
                    if (initThread != null || initThread.getState() == State.RUNNABLE) {
                        initThread.interrupt();
                    }
                    if (myRunThread != null || myRunThread.getState() == State.RUNNABLE) {
                        isSafety = true;
                        myRunThread.interrupt();
                    }
                }
                busyWaitMicros(10);
            }
        }
    }

    private class RunThread extends Thread {
        //false=TOP   true=bottom
        private boolean direction;

        public RunThread(boolean direction) {
            this.direction = direction;
            Log.e(TAG, "direction=" + direction);
        }

        @SuppressLint("CommitPrefEdits")
        @Override
        public void run() {
            super.run();
            try {
                runing = true;//启动
                isSafety = false;
                isManual = false;//手动初始
                gpio_dir.setValue(direction);
                while (true) {
                    if (interrupted()) {
                        runing = false;
                        if (!direction && !isManual && !isSafety) {//上升 未手动停止 --->复位
                            runGuide();
                        }
                        break;
                    }
                    gpio_pul.setValue(true);
                    busyWaitMicros(1);
                    gpio_pul.setValue(false);
                    if (direction) {
                        current_count -= 1;
                        if (current_count <= 0) {
                            Log.e(TAG, "MIN current_count=" + current_count);
                            myRunThread.interrupt();
                        }
                    } else {
                        current_count += 1;
                        if (current_count >= MAX_COUNT) {
                            Log.e(TAG, "MAX current_count=" + current_count);
                            myRunThread.interrupt();
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class RunCountThread extends Thread {

        private boolean direction;
        private int count;

        public RunCountThread(boolean direction, int count) {
            this.direction = direction;
            this.count = count;
        }

        @Override
        public void run() {
            super.run();
            runing = true;
            try {
                gpio_dir.setValue(direction);
                for (int i = 0; i < count; i++) {
                    gpio_pul.setValue(true);
                    busyWaitMicros(1);
                    gpio_pul.setValue(false);
                    if (direction) {
                        current_count -= 1;
                    } else {
                        current_count += 1;
                    }
                    if (current_count >= MAX_COUNT) {
                        runing = false;
                        return;
                    } else if (current_count <= 0) {
                        runing = false;
                        return;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private DeviceMotor() {
    }

    public void findFace(DeviceListener listener) {
        RequestEntity entity = new RequestEntity();
        entity.setMode(888);
        if (!init) {
            entity.setErrorCode(777);
            entity.setErrorMessage("初始化失败!");
            listener.onComplete(gson.toJson(entity));
            return;
        } else if (runing) {
            entity.setErrorCode(777);
            entity.setErrorMessage("导轨正在使用!");
            listener.onComplete(gson.toJson(entity));
            return;
        } else if (current_count == -1) {
            entity.setErrorCode(777);
            entity.setErrorMessage("导轨未回到原点!");
            listener.onComplete(gson.toJson(entity));
            return;
        }
        runGuide();
        entity.setErrorCode(700);
        entity.setErrorMessage("启动导轨");
        listener.onComplete(gson.toJson(entity));
    }

    void findFaceTop() {
        if (runing) return;
        if (myRunThread.getState() != Thread.State.NEW || myRunThread.getState() == Thread.State.RUNNABLE) {
            myRunThread.interrupt();
            myRunThread = new RunThread(false);
        }
        myRunThread.start();
    }

    void findFaceBottom() {
        if (runing) return;
        if (myRunThread.getState() != Thread.State.NEW || myRunThread.getState() == Thread.State.RUNNABLE) {
            myRunThread.interrupt();
            myRunThread = new RunThread(true);
        }
        myRunThread.start();
    }


    void reset() {//获取距离回归原点
        int[] value = new int[10];
        for (int i = 0; i < 10; i++) {
            value[i] = (int) hcsr04.readDistance();
        }
        int distance = Arrays.stream(value).sum() / 10;
        if (distance > 10) {
            initThread.start();
        } else {
            current_count = 0;
        }
    }

    public void runZero(){
        if (myRunThread.getState() != Thread.State.NEW || myRunThread.getState() == Thread.State.RUNNABLE) {
            myRunThread.interrupt();
            myRunThread = new RunThread(true);
        }
        myRunThread.start();
    }


    private void runGuide() {
        if (myRunThread.getState() != Thread.State.NEW || myRunThread.getState() == Thread.State.RUNNABLE) {
            myRunThread.interrupt();
            if (current_count >= MAX_COUNT) {
                myRunThread = new RunThread(true);
            } else {
                myRunThread = new RunThread(false);
            }
        }
        myRunThread.start();
    }

    public void stop(DeviceListener listener) {
        if (myRunThread != null) {
            isManual = true;
            myRunThread.interrupt();
            RequestEntity entity = new RequestEntity();
            entity.setErrorCode(889);
            entity.setErrorMessage("停止");
            listener.onComplete(gson.toJson(entity));
        }
    }

    private void init() {
        Observable.zip(openGpio(ENA), openGpio(DIR), openGpio(PUL), (ena, dir, pul) -> {
            gpio_ena = ena;
            gpio_ena.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            gpio_dir = dir;
            gpio_dir.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            gpio_dir.setActiveType(Gpio.ACTIVE_HIGH);
            gpio_pul = pul;
            gpio_pul.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            gpio_pul.setActiveType(Gpio.ACTIVE_HIGH);
//            hcsr04 = new Hcsr04(TRING, ECHO);
//            readThread.start();
            return true;
        })
                .subscribeOn(Schedulers.newThread())
                .subscribe(aBoolean -> {
                    init = true;
                    initListener.onInit(0, null);
                }, throwable -> {
                    init = false;
                    initListener.onInit(1, throwable.getMessage());
                });
    }

    public static DeviceMotor getInstance() {
        if (deviceMotor == null) {
            synchronized (DeviceMotor.class) {
                if (deviceMotor == null) {
                    deviceMotor = new DeviceMotor();
                }
            }
        }
        return deviceMotor;
    }

    public void setInitListener(DeviceInitListener initListener) {
        this.initListener = initListener;
        new Thread(this::init).start();
    }


    private Observable<Gpio> openGpio(String name) {
        return Observable.defer(() -> {
            PeripheralManager manager = PeripheralManager.getInstance();
            Gpio gpio = manager.openGpio(name);
            return Observable.just(gpio);
        });
    }


    private void busyWaitMicros(long micros) {
        long start = System.nanoTime();
        long waitUntil = start + (micros * 1000);
        while (true) {
            if (waitUntil <= System.nanoTime()) break;
        }
    }

}
