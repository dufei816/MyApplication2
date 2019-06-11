package com.duohuan.billing;

public class Config {

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

    //------------end   Things 100~200--------------


    //------------start 异常编号----------------
    public static final int SUCCESS = 0;
    public static final String SUCCESS_MSG = "完成";

    //激光正在运行
    public static final int LASER_RUNNING_ERROR = 1;
    public static final int LASER_ERROR = 2;
    public static final String LASER_RUNNING_ERROR_MSG = "激光正在运行";
    //------------end 异常编号----------------


}
