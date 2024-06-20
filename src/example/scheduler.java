package example;

import api.SchedulerApi;
import api.SimulatorApi;
import org.sim.cloudbus.cloudsim.Log;
import org.sim.controller.Result;

import java.util.ArrayList;
import java.util.List;

public class scheduler {
    public static void main(String[] args) {
        SchedulerApi schedulerApi = new SchedulerApi();
        SimulatorApi simulatorApi = new SimulatorApi();
        // 创建输出文件目录和中间文件目录
        schedulerApi.createDir();
        // 更改输出文件目录
        //schedulerApi.setSchedulerOutputPath(System.getProperty("user.dir") + "\\输出");
        // 传入应用信息输入文件
        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo.xml");
        // 传入物理节点信息输入文件
        schedulerApi.uploadHostsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_Hosts.xml");
        // 传入容器信息输入文件
        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
        // 传入错误注入文件
        // schedulerApi.uploadFault(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_Fault.xml");
        // 设置暂停
        // simulatorApi.pauseContainer(8, 1.0, 10.0);
        // 设置调度算法
        schedulerApi.startSchedule(SchedulerApi.HEFT);
        //schedulerApi.startSchedule(SchedulerApi.K8S);
        //schedulerApi.startSchedule(SchedulerApi.MAXMIN);

        // 获取应用所在物理节点
        Log.printLine(schedulerApi.getHostNameOfApplication("HF_GPU1-CPU1"));
        // 获取当前调度算法下所有应用运行一周期需要的时间
        Log.printLine("完成1周期所需时间；" + schedulerApi.getFinishTime());
        // 获取调度结果
        List<Result> results = new ArrayList<>(schedulerApi.getScheduleResults());
        for(Result r: results) {
            Log.printLine("任务 " + r.app + " =====> " + "物理节点 " + r.host);
        }
        // 修改调度结果
        List<Result> resultToChange = new ArrayList<>();
        for(Result r: results) {
            Result r2 = r.getNewResult();
            if(r.app.equals("HF_SM_2")) {
                r2.host = "host6";
            }
            resultToChange.add(r2);
        }
        schedulerApi.staticSimulate(resultToChange);
        // 对比修改前后的调度结果
        List<Result> resultAfterChange = schedulerApi.getScheduleResults();
        for(Result r: results) {
            for(Result r2: resultAfterChange)
            {
                if(r2.app.equals(r.app))
                    Log.printLine("任务 " + r.app + " =====> " + "物理节点(修改前） " + r.host + " / 物理节点(修改后） " + r2.host);
            }

        }
    }
}
