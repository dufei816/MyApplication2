package com.duohuan.billing;

public class Config {

    public static final String TAKE_PIC = "TakePic";
    public static final String FACE = "Face";
    public static final String BILLING = "Billing";

    public static final int BILLING_UPDATE = 4;

    //------------start Things 100~200--------------
    //启动激光
    public static final int START_LASER = 101;
    //动作结束
    public static final int END_LASER = 102;


    //启动导轨
    public static final int START_GUIDE = 103;
    //停止导轨
    public static final int END_GUIDE = 104;
    //导轨返回原地
    public static final int RETURN_ZERO = 105;
    //人脸采集
    public static final int START_FIND_FACE = 106;
    //自动停止
    public static final int STOP_GUIDE = 107;

    //----------------error Code-----------------
    public static final int GUIDE_INIT_ERROR = 1;
    public static final String GUIDE_INIT_ERROR_MSG = "初始化失败！";
    public static final int GUIDE_RUNNING_ERROR = 2;
    public static final String GUIDE_RUNNING_ERROR_MSG = "导轨正在运行";
    public static final int GUIDE_RETURN_ZERO = 3;
    public static final String GUIDE_RETURN_ZERO_MSG = "导轨未回到原点";


    //----------------error Code-----------------

    //------------end   Things 100~200--------------


    //------------start 异常编号----------------
    public static final int SUCCESS = 0;
    public static final String SUCCESS_MSG = "完成";

    //激光正在运行
    public static final int LASER_RUNNING_ERROR = 1;
    public static final int LASER_ERROR = 2;
    public static final String LASER_RUNNING_ERROR_MSG = "激光正在运行";

    //发送消息异常
    public static final int SEND_ERROR = 3;
    //------------end 异常编号----------------


}
