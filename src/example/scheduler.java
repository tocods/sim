package example;

import api.SchedulerApi;
import org.sim.cloudbus.cloudsim.Log;

public class scheduler {
    public static void main(String[] args) {
        SchedulerApi schedulerApi = new SchedulerApi();
        // 创建输出文件目录和中间文件目录
        schedulerApi.createDir();
        schedulerApi.setSchedulerOutputPath(System.getProperty("user.dir") + "\\输出");
        // 传入应用信息输入文件
        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo .xml");
        // 传入物理节点信息输入文件
        schedulerApi.uploadHostsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_Hosts .xml");
        // 传入容器信息输入文件
        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
        // 设置调度算法
        schedulerApi.startSchedule(SchedulerApi.HEFT);
        //schedulerApi.startSchedule(SchedulerApi.K8S);
        //schedulerApi.startSchedule(SchedulerApi.MAXMIN);

        // 获取应用所在物理节点
        Log.printLine(schedulerApi.getHostNameOfApplication("HF_GPU1-CPU1"));
        // 获取当前调度算法下所有应用运行一周期需要的时间
        Log.printLine("完成1周期所需时间；" + schedulerApi.getFinishTime());
    }
}
