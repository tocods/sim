package org.sim.service;

import org.jfree.data.xy.XYSeries;
import org.sim.cloudbus.cloudsim.*;
import org.sim.cloudbus.cloudsim.*;
import org.sim.cloudbus.cloudsim.core.CloudSim;
import org.sim.controller.MyPainter;
import org.sim.controller.Result;
import org.sim.service.result.FaultRecord;
import org.sim.workflowsim.*;
import org.sim.workflowsim.failure.FailureGenerator;
import org.sim.workflowsim.failure.FailureMonitor;
import org.sim.workflowsim.failure.FailureParameters;
import org.sim.workflowsim.utils.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.sim.workflowsim.*;
import org.sim.workflowsim.utils.*;
import org.sim.cloudbus.cloudsim.*;
import org.sim.workflowsim.*;
import org.sim.workflowsim.utils.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;


public class service {
    public List<Host> hostList;

    public List<Host> hostToBalance;

    protected List<CondorVM> createVM(int userId, int vms) {
        //Creates a container to store VMs. This list is passed to the broker later
        LinkedList<CondorVM> list = new LinkedList<>();

        //VM Parameters
        String vmm = "Xen"; //VMM name

        //create VMs
        CondorVM[] vm = new CondorVM[vms];
        for (int i = 0; i < vms; i++) {
            double ratio = 1.0;
            vm[i] = new CondorVM(i, userId, 0, 0, 0, 0, 0, vmm, new CloudletSchedulerSpaceShared());
            list.add(vm[i]);
        }
        return list;
    }

    ////////////////////////// STATIC METHODS ///////////////////////
    /**
     * Creates main() to run this example This example has only one datacenter
     * and one storage
     */
    public void simulate(Integer arithmetic) {
        DistributionGenerator.DistributionFamily f = DistributionGenerator.DistributionFamily.WEIBULL;
        Double s = 100.0;
        Double shape = 1.0;
        try {
            XmlUtil util = new XmlUtil(6);
            // 解析错误注入文件
            if(Constants.faultFile != null) {
                util.parseHostXml(Constants.faultFile);
                f = util.distributionFamily;
                s = util.scale;
                shape = util.shape;
                Log.printLine("FaultInject: || "  + "type: " + f.name() + "  scale: " + s + " shape: " + shape + " ||");
            }
            // 解析物理节点文件
            util.parseHostXml(Constants.hostFile);
            hostList = util.getHostList();
            hostToBalance = new ArrayList<>(hostList);
            int vmNum = 1;//number of vms;
            FailureParameters.FTCMonitor ftc_monitor = FailureParameters.FTCMonitor.MONITOR_ALL;
            FailureParameters.FTCFailure ftc_failure = FailureParameters.FTCFailure.FAILURE_ALL;
            FailureParameters.FTCluteringAlgorithm ftc_method = util.ftCluteringAlgorithm;
            DistributionGenerator[][] failureGenerators = new DistributionGenerator[1][1];
            failureGenerators[0][0] = new DistributionGenerator(f,
                    s, shape, 30, 300, 0.78);

            Parameters.SchedulingAlgorithm sch_method = Parameters.SchedulingAlgorithm.STATIC;
            Parameters.PlanningAlgorithm pln_method = Parameters.PlanningAlgorithm.HEFT;
            ReplicaCatalog.FileSystem file_system = ReplicaCatalog.FileSystem.SHARED;
            Log.printLine(arithmetic + "----------");
            // 解析调度算法类型
            switch (arithmetic) {
                case 1:
                    sch_method = Parameters.SchedulingAlgorithm.ROUNDROBIN;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 2:
                    sch_method = Parameters.SchedulingAlgorithm.MAXMIN;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 3:
                    sch_method = Parameters.SchedulingAlgorithm.FCFS;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 4:
                    sch_method = Parameters.SchedulingAlgorithm.MCT;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 6:
                    sch_method = Parameters.SchedulingAlgorithm.MIGRATE;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 7:
                    sch_method = Parameters.SchedulingAlgorithm.K8S;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
                case 8:
                    sch_method = Parameters.SchedulingAlgorithm.USER;
                    pln_method = Parameters.PlanningAlgorithm.INVALID;
                    break;
            }


            FailureMonitor.init();
            FailureGenerator.init();
            OverheadParameters op = new OverheadParameters(0, null, null, null, null, 0);
            ClusteringParameters.ClusteringMethod method = ClusteringParameters.ClusteringMethod.NONE;
            ClusteringParameters cp = new ClusteringParameters(0, 0, method, null);
            FailureParameters.init(ftc_method, ftc_monitor, ftc_failure, failureGenerators);
            Parameters.init(vmNum, "", null,
                    null, op, cp, sch_method, pln_method,
                    null, 0);
            ReplicaCatalog.init(file_system);
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events
            CloudSim.init(num_user, calendar, trace_flag);
            // 创建工作流数据中心
            WorkflowDatacenter datacenter0 = createDatacenter("Datacenter_0", arithmetic);
            WorkflowPlanner wfPlanner = new WorkflowPlanner("planner_0", 1);
            //wfPlanner.setAppPath("D:/WorkflowSim-1.0/config/tmp/app10.xml");
            wfPlanner.setAppPath("");
            WorkflowEngine wfEngine = wfPlanner.getWorkflowEngine();
            List<CondorVM> vmlist0 = createVM(wfEngine.getSchedulerId(0), 1);
            Constants.Scheduler_Id = wfEngine.getSchedulerId(0);
            Constants.LOG_PATH = System.getProperty("user.dir")+"\\OutputFiles\\hostUtil\\hostUtilization.xml";
            Constants.FAULT_LOG_PATH = System.getProperty("user.dir") + "\\OutputFiles\\faultLog\\faultLog.xml";
            Constants.ERROR_TIME_PATH = System.getProperty("user.dir") + "\\OutputFiles\\faultLog\\faultTime.xml";
            /**
             * Submits this list of vms to this WorkflowEngine.
             */
            wfEngine.submitVmList(vmlist0, 0);
            //wfEngine.submitVmList(new ArrayList<>(), 0);

            wfEngine.submitHostList(hostList, 0);
            Constants.hosts = hostList;

            /**
             * Binds the data centers with the scheduler.
             */
            wfEngine.bindSchedulerDatacenter(datacenter0.getId(), 0);
            CloudSim.startSimulation();
            Log.printLine("开始输出结果");
            List<Job> outputList0 = wfEngine.getJobsReceivedList();
            Constants.resultPods = new ArrayList<>(outputList0);
            CloudSim.stopSimulation();
            printJobList(outputList0);
            wfPlanner.rewriteAppXml();
        } catch (Exception e) {
            Log.printLine("The simulation has been terminated due to an unexpected error" );
            e.printStackTrace();
        }
    }


    protected WorkflowDatacenter createDatacenter(String name, Integer a) {
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.1;	// the cost of using storage in this resource
        double costPerBw = 0.1;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<>();	//we are not adding SAN devices by now
        WorkflowDatacenter datacenter = null;

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 5. Finally, we need to create a storage object.
        /**
         * The bandwidth within a data center in MB/s.
         */
        int maxTransferRate = 15;// the number comes from the futuregrid site, you can specify your bw

        try {
            // Here we set the bandwidth to be 15MB/s
            HarddriveStorage s1 = new HarddriveStorage(name, 1e12);
            s1.setMaxTransferRate(maxTransferRate);
            storageList.add(s1);
            VmAllocationPolicy v = new VmAllocationPolicySimple(hostList);
            switch (a) {
                case 1:
                    v = new VmAllocationPolicyRR(hostList);
                    break;
                case 2:
                    //v = new VmAllocationPolicyMaxMin(hostList);
                    break;
                case 3:
                    v = new VmAllocationPolicyFCFS(hostList);
                    break;
                case 4:
                case 6:
                    v = new VmAllocationPolicyMaxMin(hostList);
                    break;
                case 7:
                    //v = new VmAllocationPolicyK8s(hostList);
                    break;
                case 8:
                    break;
            }

            datacenter = new WorkflowDatacenter(name, characteristics, v, storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return datacenter;
    }

    private double leastRequestedPriority(Host host) {
        double cpu_score = (double) (host.getVmScheduler().getAvailableMips()) / (double) (host.getNumberOfPes() * host.getVmScheduler().getPeCapacity());
        //Log.printLine("cpu_score: " + cpu_score);
        double ram_score = (double) (host.getRamProvisioner().getAvailableRam()) / (double) host.getRamProvisioner().getRam();
        //Log.printLine("ram_score: " + ram_score);
        return 10 * (cpu_score + ram_score) / 2;
    }

    private double balancedResourceAllocation(Host host) {
        double cpu_fraction = 1 -  (host.getVmScheduler().getAvailableMips()) / (double) (host.getNumberOfPes() * host.getVmScheduler().getPeCapacity());
        //Log.printLine("cpu_: " + cpu_fraction);
        double ram_fraction = 1 - (double) (host.getRamProvisioner().getAvailableRam()) / (double) host.getRamProvisioner().getRam();
        //Log.printLine("ram: " + ram_fraction);
        double mean = (cpu_fraction + ram_fraction) / 2;
        //Log.printLine("mean: " + mean);
        double variance = ((cpu_fraction - mean)*(cpu_fraction - mean)
                + (ram_fraction - mean)*(ram_fraction - mean)
        ) / 2;
        //Log.printLine("variance: " + variance);
        return 10 - variance * 10;
    }

    private double getScore(Host host) {
        return (balancedResourceAllocation(host) + leastRequestedPriority(host)) / 2;
    }

    /**
     * 输出仿真结果，包括CPU 利用率文件、错误时间点文件
     */
    protected void printJobList(List<Job> list) {
        String indent = "    ";
        Log.printLine();
        Map<Integer, Boolean> jobs = new HashMap<>();
        for(int i = 1; i <= 101; i++) {
            jobs.put(i, false);
        }
        for(Task t: Constants.taskList) {
            Result r = new Result();
        }

        String lastTime = "";
        Map<String, Boolean> ifLog = new HashMap<>();
        Constants.ifSimulate = Constants.ifSimulate && (Constants.lastTime == 0.0);
        if(Constants.ifSimulate) {
            Log.printLine("========== OUTPUT ==========");
            Log.printLine("Task Name" + indent + "Task ID" + indent + "STATUS" + indent
                    + "Host ID" + indent + indent
                    + "Time" + indent + "Start Time" + indent + "Finish Time" + indent + "Depth");
        }
        DecimalFormat dft = new DecimalFormat("###.##");
        Double totalTime = 0.0;
        for (Job job : list) {
            jobs.put(job.getCloudletId(), true);
            if(job.getTaskList().size() == 0)
                continue;
            if(Constants.ifSimulate)
                for(Task t: job.getTaskList())
                    Log.print(indent + t.name + indent + indent);
            if (job.getClassType() == Parameters.ClassType.STAGE_IN.value) {
                if(Constants.ifSimulate)
                    Log.print("Stage-in");
            }
            for (Task task : job.getTaskList()) {
                if(Constants.ifSimulate)
                    Log.print(task.getCloudletId() + ",");

            }
            if(Constants.ifSimulate)
                Log.print(indent);

            if (job.getCloudletStatus() == Cloudlet.SUCCESS) {
                if(Constants.ifSimulate) {
                    Log.print("SUCCESS");
                    totalTime += job.getFinishTime() - job.getExecStartTime();
                    Log.printLine(indent + indent + indent + job.getVmId()
                            + indent + indent + indent + dft.format(job.getActualCPUTime())
                            + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                            + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth());
                }
                lastTime = dft.format(job.getFinishTime());
                if(!job.getTaskList().isEmpty() && !ifLog.containsKey(job.getTaskList().get(0).name)) {
                    Result r = new Result();
                    double t1 = job.getFinishTime();

                    r.finish = dft.format(t1);
                    r.start = dft.format(job.getExecStartTime());
                    r.app = job.getTaskList().get(0).name;
                    if(Constants.pause.containsKey(job.getCloudletId())) {
                        Double start = Constants.pause.get(job.getCloudletId()).getKey();
                        Double last = Constants.pause.get(job.getCloudletId()).getValue();
                        if(t1 - job.getExecStartTime() < start) {

                        } else {
                            r.pausestart = start;
                            r.pauseend = last;
                        }
                    }
                    //r.host = "host" + job.getVmId();
                    Host host = null;
                    for(Host h: Constants.hosts) {
                        if(h.getId() == job.getVmId()) {
                            host = h;
                            break;
                        }
                    }
                    if(host == null) {
                        continue;
                    }
                    r.host = host.getName();
                    r.name = job.getTaskList().get(0).getType();
                    r.pes = Double.valueOf(job.getTaskList().get(0).getNumberOfPes()) / 1000;
                    r.ram = job.getTaskList().get(0).getRam();
                    r.period = job.getTaskList().get(0).getPeriodTime();
                    r.datacenter = host.datacenterName;
                    Task task = job.getTaskList().get(0);
                    if(Constants.pause.get(job.getTaskList().get(0).getCloudletId()) != null) {
                        r.pausestart = Constants.pause.get(job.getTaskList().get(0).getCloudletId()).getKey();
                        r.pauseend = Constants.pause.get(job.getTaskList().get(0).getCloudletId()).getValue();
                    }
                    CondorVM containerTmp = new CondorVM(task.getCloudletId(), 1, 0, task.getNumberOfPes(), (int) task.getRam(), 0, 0, "Xen", new CloudletSchedulerTimeShared());
                    for(Host h: hostToBalance) {
                        if(h.getName().equals(r.host)) {
                            h.vmCreate(containerTmp);
                        }
                    }
                    Constants.results.add(r);
                    ifLog.put(job.getTaskList().get(0).name, true);
                }
            } else if (job.getCloudletStatus() == Cloudlet.FAILED) {
                if(Constants.ifSimulate) {
                    Log.print("FAILED");
                    Log.printLine(indent + indent + indent + job.getVmId()
                            + indent + indent + indent + dft.format(job.getActualCPUTime())
                            + indent + indent + dft.format(job.getExecStartTime()) + indent + indent + indent
                            + dft.format(job.getFinishTime()) + indent + indent + indent + job.getDepth());
                }
            }
        }
        if(Constants.ifSimulate) {
            Log.printLine(Constants.repeatTime + "周期下，任务群总完成时间为：" + lastTime );
            Constants.score = Double.valueOf(lastTime) / (Constants.totalTime);
            if (Constants.repeatTime > 0)
                Constants.score /= Constants.repeatTime;
            Constants.score = 1 - Constants.score;
            Constants.score *= 100;
            Double leastRequestScore = 0.0;
            Double balanceScore = 0.0;
            for(Host h: hostToBalance) {
                leastRequestScore += leastRequestedPriority(h);
                balanceScore += balancedResourceAllocation(h);
            }
            balanceScore /= hostToBalance.size();
            balanceScore *= 10;
            leastRequestScore /= hostToBalance.size();
            leastRequestScore *= 10;
            Double totalScore = (leastRequestScore + balanceScore) / 2;
            Constants.balanceScore = "\r\n 资源均衡度打分:\r\n BalanceResourceAllocation: " + dft.format(balanceScore) + "\r\n LeastRequestPriority: " + dft.format(leastRequestScore)
                    + "\r\n 总得分: " + dft.format(totalScore) + "\r\n 运行时间打分\r\n" + "任务群仿真运行" + Constants.repeatTime + "周期总时间：" + lastTime + "\r\n 得分: " + dft.format(Constants.score);
        }
        try {
            int size_T = 0;
            if(!Objects.equals(Constants.outputPath, "")) {
                Constants.LOG_PATH = Constants.outputPath + "\\hostUtil\\hostUtilization.xml";
                Constants.FAULT_LOG_PATH = Constants.outputPath + "\\faultLog\\faultLog.xml";
                Constants.ERROR_TIME_PATH = Constants.outputPath + "\\faultLog\\faultTime.xml";
            }
            File file = new File(Constants.LOG_PATH);
            if(!file.exists()) {
                file.getParentFile().mkdir();
            }
            int num = Constants.hosts.size();
            List<Double> cpuUtil = new ArrayList<>();
            List<Double> ramUtil = new ArrayList<>();
            for(int i = 0; i < num; i++) {
                cpuUtil.add(0.0);
                ramUtil.add(0.0);
            }
            XYSeries[] xySeries = new XYSeries[Constants.hosts.size()];
            for(int i = 0; i < Constants.hosts.size(); i++) {
                xySeries[i] = new XYSeries(Constants.hosts.get(i).getName());
            }
            Element root = new Element("root");
            Document doc = new Document(root);
            int hostSize = Constants.hosts.size();
            Element r = null;
            for(int i = 0; i < Constants.logs.size(); i++) {
                if(i % hostSize == 0) {
                    if(Double.parseDouble(Constants.logs.get(i).time) >= Constants.finishTime) {
                        break;
                    }
                    r = new Element("Utilization");
                    r.setAttribute("time", Constants.logs.get(i).time);
                }
                Element t = new Element("Host");
                t.setAttribute("name", Constants.hosts.get(i % hostSize).getName());
                t.setAttribute("cpuUtilization", Constants.logs.get(i).cpuUtilization);
                t.setAttribute("ramUtilization", Constants.logs.get(i).ramUtilization);
                r.addContent(t);
                xySeries[i % hostSize].add(Double.parseDouble(Constants.logs.get(i).time), Double.parseDouble(Constants.logs.get(i).cpuUtilization));
                Double cpu = cpuUtil.get(Constants.logs.get(i).hostId);
                Double ram = ramUtil.get(Constants.logs.get(i).hostId);
                cpu += Double.parseDouble(Constants.logs.get(i).cpuUtilization);
                ram += Double.parseDouble(Constants.logs.get(i).ramUtilization);
                cpuUtil.add(Constants.logs.get(i).hostId, cpu);
                ramUtil.add(Constants.logs.get(i).hostId, ram);
                if(i % hostSize == hostSize - 1)
                {
                    size_T++;
                    doc.getRootElement().addContent(r);
                }

            }
            List<Double> cpuAverage = new ArrayList<>();
            List<Double> ramAverage = new ArrayList<>();
            for(Integer i = 0; i < hostSize; i++){
                cpuAverage.add(cpuUtil.get(i) / size_T);
                ramAverage.add(ramUtil.get(i) / size_T);
            }
            DecimalFormat dfs = new DecimalFormat("#.000");
            Double meanCpu = 0.0;
            Double meanRam = 0.0;
            Double c = 0.0;
            Double ra = 0.0;
            r = new Element("Average");
            Integer cs = 0;
            Double ras = 0.0;
            for(Integer i = 0; i < hostSize; i++) {
                Element t = new Element("AverageUtil");
                t.setAttribute("host", Constants.hosts.get(i).getName());
                t.setAttribute("cpu", dfs.format(cpuAverage.get(i)));
                t.setAttribute("ram", dfs.format(ramAverage.get(i)));
                c += (Constants.hosts.get(i).getNumberOfPes() / 1000 )* cpuAverage.get(i);
                ra += Constants.hosts.get(i).getRam() * ramAverage.get(i);
                meanCpu += cpuAverage.get(i);
                meanRam += ramAverage.get(i);
                cs += Constants.hosts.get(i).getNumberOfPes() / 1000;
                ras += Constants.hosts.get(i).getRam();
                r.addContent(t);
            }
            meanCpu /= hostSize;
            meanRam /= hostSize;
            Double varianceCpu = 0.0;
            Double varianceRam = 0.0;
            for(Integer i = 0; i < hostSize; i++) {
                varianceCpu += (cpuAverage.get(i) - meanCpu) * (cpuAverage.get(i) - meanCpu);
                varianceRam += (ramAverage.get(i) - meanRam) * (ramAverage.get(i) - meanRam);
            }
            varianceCpu /= hostSize;
            varianceRam /= hostSize;
            Element t = new Element("cpuUtilization");
            t.setAttribute("value", dfs.format(c / cs));
            r.addContent(t);
            t = new Element("ramUtilization");
            t.setAttribute("value", dfs.format(ra / ras));
            r.addContent(t);
            doc.getRootElement().addContent(r);
            r = new Element("Variance");
            r.setAttribute("cpu", dfs.format(varianceCpu));
            r.setAttribute("ram", dfs.format(varianceRam));
            doc.getRootElement().addContent(r);
            XMLOutputter xmlOutput = new XMLOutputter();
            Format f = Format.getRawFormat();
            f.setIndent("  "); // 文本缩进
            f.setTextMode(Format.TextMode.TRIM_FULL_WHITE);
            xmlOutput.setFormat(f);

            // 把xml文件输出到指定的位置
            xmlOutput.output(doc, new FileOutputStream(file));

            file = new File(Constants.FAULT_LOG_PATH);
            root = new Element("root");
            doc = new Document(root);
            for(FaultRecord rs: Constants.records) {
                Element e = new Element("FaultRecord");
                e.setAttribute("time", dfs.format(rs.time));
                e.setAttribute("jobName", rs.name);
                doc.getRootElement().addContent(e);
            }
            xmlOutput.output(doc, new FileOutputStream(file));
            /*MyPainter painter = new MyPainter("CPU Usage");
            painter.paintCPU(xySeries, "cpu_usage");*/
            file = new File(Constants.ERROR_TIME_PATH);
            root = new Element("root");
            doc = new Document(root);
            for(Double err: Constants.errors) {
                if(err > Constants.finishTime)
                    break;
                Element e = new Element("FaultTime");
                e.setAttribute("time", dfs.format(err));
                doc.getRootElement().addContent(e);
            }
            xmlOutput.output(doc, new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
