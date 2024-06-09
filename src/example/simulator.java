package example;

import api.SchedulerApi;
import api.SimulatorApi;
import org.sim.cloudsimsdn.Log;

public class simulator {

    public static void main(String[] args) {
        Log.printLine("启动成功");
        SchedulerApi schedulerApi = new SchedulerApi();
        schedulerApi.createDir();
        schedulerApi.uploadAppsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_AppInfo .xml");
        schedulerApi.uploadHostsXml(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\Input_Hosts .xml");
        schedulerApi.uploadContainerInfo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\TC\\ContainerInfo8.xml");
        schedulerApi.startSchedule(SchedulerApi.HEFT);
        SimulatorApi simulatorApi = new SimulatorApi();
        simulatorApi.setSimulationTime(1000);
        simulatorApi.setHalfduplex(Boolean.FALSE);
        simulatorApi.uploadtopo(System.getProperty("user.dir") + "\\测试文档\\TestFile\\1_Input_TopoInfo - 10G以太网.xml");
        simulatorApi.run();
    }

}

