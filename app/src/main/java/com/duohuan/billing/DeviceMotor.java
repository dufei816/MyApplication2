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

    private static final Object obj = new Object();
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
    private FindFaceListener findFaceListener;


    public void close() {
        Observable.just("")
                .subscribeOn(Schedulers.newThread())
                .subscribe(str -> {
                    if (gpio_ena != null) {
                        gpio_ena.close();
                    }
                    if (gpio_dir != null) {
                        gpio_dir.close();
                    }
                    if (gpio_pul != null) {
                        gpio_pul.close();
                    }
                    if (myRunThread.getState() == Thread.State.RUNNABLE) {
                        myRunThread.interrupt();
                    }
                }, Throwable::printStackTrace);
    }

    public void setFindFaceListener(FindFaceListener findFaceListener) {
        this.findFaceListener = findFaceListener;
    }

    public interface FindFaceListener {
        void onStop();
    }

    public void test() {
        runGuide();
    }

    //安全机制
    private boolean isSafety;
    //是否在运行中
    private boolean runing;
    //是否是手动停止
    private boolean isManual;
    //是否初始化完成
    private boolean init = false;

    private RunThread myRunThread = new RunThread(false);
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

        @Override
        public void run() {
            super.run();
            try {
                runing = true;//启动
                isManual = false;//手动初始
                gpio_dir.setValue(direction);
                while (true) {
                    if (interrupted()) {
                        runing = false;
                        if (!direction) {//上升
                            if (!isManual) {//自动停止
                                runGuide();//--->复位
                            } else {
                                findFaceListener = null;
                            }
                        } else {//下降
                            if (!isManual) {//自动停止
                                if (findFaceListener != null) {
                                    findFaceListener.onStop();
                                }
                            }
                            findFaceListener = null;
                        }
                        runing = false;
                        break;
                    }
                    oneStep();
                    if (!safety(direction)) {//不安全
                        this.interrupt();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private DeviceMotor() {

    }

    public void findFace(RequestEntity entity, DeviceListener listener) {
        entity.setNumber(entity.getNumber() + 1);
        if (!init) {
            entity.setErrorCode(Config.GUIDE_INIT_ERROR);
            entity.setErrorMessage(Config.GUIDE_INIT_ERROR_MSG);
            listener.onComplete(gson.toJson(entity));
            return;
        } else if (runing) {
            entity.setErrorCode(Config.GUIDE_RUNNING_ERROR);
            entity.setErrorMessage(Config.GUIDE_RUNNING_ERROR_MSG);
            listener.onComplete(gson.toJson(entity));
            return;
        } else if (current_count == -1) {
            entity.setErrorCode(Config.GUIDE_RETURN_ZERO);
            entity.setErrorMessage(Config.GUIDE_RETURN_ZERO_MSG);
            listener.onComplete(gson.toJson(entity));
            return;
        }
        runGuide();
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

    public void runZero() {
        synchronized (obj) {
            if (myRunThread.getState() != Thread.State.NEW || myRunThread.getState() == Thread.State.RUNNABLE) {
                myRunThread.interrupt();
                isManual = true;
                myRunThread = new RunThread(true);
            }
            myRunThread.start();
        }
    }


    /**
     * 启动导轨
     */
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

    public void stop() {
        synchronized (obj) {
            if (myRunThread != null) {
                isManual = true;
                myRunThread.interrupt();
            }
        }
    }


    /**
     * 安全判断
     *
     * @param direction
     * @return
     */
    private boolean safety(boolean direction) {
        if (direction) {
            current_count -= 1;
            return current_count > 0;
        } else {
            current_count += 1;
            return current_count < MAX_COUNT;
        }
    }

    /**
     * 前进1步
     */
    private void oneStep() {
        if (init) {
            try {
                gpio_pul.setValue(true);
                busyWaitMicros(1);
                gpio_pul.setValue(false);
            } catch (IOException e) {
                e.printStackTrace();
            }
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

    /**
     * 初始化
     *
     * @param initListener
     */
    public void setInitListener(DeviceInitListener initListener) {
        this.initListener = initListener;
        new Thread(this::init).start();
    }


    /**
     * 打开io口
     *
     * @param name
     * @return
     */
    private Observable<Gpio> openGpio(String name) {
        return Observable.defer(() -> {
            PeripheralManager manager = PeripheralManager.getInstance();
            Gpio gpio = manager.openGpio(name);
            return Observable.just(gpio);
        });
    }


    /**
     * 延时
     *
     * @param micros
     */
    private void busyWaitMicros(long micros) {
        long start = System.nanoTime();
        long waitUntil = start + (micros * 1000);
        while (true) {
            if (waitUntil <= System.nanoTime()) break;
        }
    }

}
