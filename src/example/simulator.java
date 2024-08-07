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
//        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo.xml");
//        schedulerApi.uploadLimitsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_Limits.xml");
//        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
//        schedulerApi.uploadTopoXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\TopoInfo有线.xml");
//        schedulerApi.startSchedule(SchedulerApi.K8S);
//        SimulatorApi simulatorApi = new SimulatorApi();
//        simulatorApi.setSimulationTime(1000);
//        simulatorApi.setHalfduplex(Boolean.FALSE);
//        simulatorApi.run();

        /* 无线示例 */
        Log.printLine("启动成功");
        SchedulerApi schedulerApi = new SchedulerApi();
        schedulerApi.createDir();
        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo-无线.xml");
        schedulerApi.uploadLimitsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_Limits.xml");
        schedulerApi.uploadTopoXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\TopoInfo无线.xml");
        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
        schedulerApi.startSchedule(SchedulerApi.K8S);
        SimulatorApi simulatorApi = new SimulatorApi();
        simulatorApi.setSimulationTime(1000);
        simulatorApi.setHalfduplex(Boolean.FALSE);
        simulatorApi.run();
    }

}

