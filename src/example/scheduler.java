package example;

import api.SchedulerApi;
import org.sim.cloudbus.cloudsim.Log;

public class scheduler {
    public static void main(String[] args) {
        System.out.println("启动成功");
        SchedulerApi schedulerApi = new SchedulerApi();
        schedulerApi.createDir();
        schedulerApi.uploadAppsXml("D:\\SimulatorBackend\\SimulatorBackend\\test\\TC2-6\\Input_AppInfo .xml");
        schedulerApi.uploadHostsXml("D:\\SimulatorBackend\\SimulatorBackend\\test\\TC2-6\\Input_Hosts .xml");
        schedulerApi.uploadContainerInfo("D:\\SimulatorBackend\\SimulatorBackend\\test\\TC2-6\\ContainerInfo8.xml");
        schedulerApi.startSchedule(SchedulerApi.HEFT);
        Log.printLine(schedulerApi.getHostNameOfApplication("HF_SM_4"));
        Log.printLine("完成1周期所需时间；" + schedulerApi.getFinishTime());
    }
}
