package example;

import api.SchedulerApi;
import api.SimulatorApi;
import org.sim.cloudsimsdn.Log;

public class simulator {

    public static void main(String[] args) {
//        /* 有线示例 */
//        Log.printLine("启动成功");
//        SchedulerApi schedulerApi = new SchedulerApi();
//        schedulerApi.createDir();
//        //更改输出文件目录
//        schedulerApi.setSchedulerOutputPath(System.getProperty("user.dir") + "\\输出");
//        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo .xml");
//        schedulerApi.uploadHostsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\1_Input_Hosts - 3主机.xml");
//        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
//        schedulerApi.startSchedule(SchedulerApi.K8S);
//        SimulatorApi simulatorApi = new SimulatorApi();
//        simulatorApi.setSimulationTime(1000);
//        simulatorApi.setHalfduplex(Boolean.FALSE);
//        simulatorApi.uploadtopo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\615_TopoInfo_有线.xml");
//        simulatorApi.run();

        /* 无线示例 */
        Log.printLine("启动成功");
        SchedulerApi schedulerApi = new SchedulerApi();
        schedulerApi.createDir();
        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo .xml");
        schedulerApi.uploadHostsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\3_Input_Hosts - 无线8主机.xml");
        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
        schedulerApi.startSchedule(SchedulerApi.K8S);
        SimulatorApi simulatorApi = new SimulatorApi();
        simulatorApi.setSimulationTime(1000);
        simulatorApi.setHalfduplex(Boolean.FALSE);
        simulatorApi.uploadtopo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\615_TopoInfo_无线.xml");
        simulatorApi.run();
    }

}

