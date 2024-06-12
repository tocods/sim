package org.sim.controller;

public class Result {
    // 任务名
    public String app;
    public Double pausestart = 0.0;
    public Double pauseend = 0.0;
    // 任务的IP地址
    public String name;
    // 任务所在物理节点名
    public String host;
    // 第1个周期任务开始运行的时间
    public String start;
    // 第1个周期任务运行完成的时间
    public String finish;
    public String type="cn";
    public String datacenter="A";
    public Integer mips=500;
    // 任务占用的核
    public Double pes=1.0;
    public Double ram=512.0;
    public Integer size=1000;
    public Double period;

    public Result getNewResult() {
        Result ret = new Result();
        ret.app = app;
        ret.pauseend = pauseend;
        ret.pausestart = pausestart;
        ret.name = name;
        ret.host = host;
        ret.start = start;
        ret.finish = finish;
        ret.type = type;
        ret.datacenter = datacenter;
        ret.mips = mips;
        ret.pes = pes;
        ret.ram = ram;
        ret.period = period;
        return ret;
    }
}
