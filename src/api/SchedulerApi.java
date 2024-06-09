package api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.sim.cloudbus.cloudsim.Host;
import org.sim.cloudbus.cloudsim.Log;
import org.sim.controller.Result;
import org.sim.controller.ResultDTO;
import org.sim.service.Constants;
import org.sim.service.ContainerInfo;
import org.sim.service.YamlWriter;
import org.sim.workflowsim.XmlUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

import org.sim.service.service;

public class SchedulerApi {
    private service service;

    private String iPath = "";

    private String yPath = "";

    public static Integer MAXMIN = 2;
    public static Integer K8S = 7;

    public static Integer HEFT = 5;


    private void resetAllForScheduler() {
        Constants.workloads = new ArrayList<>();
        Constants.lastTime = 0.0;
        Constants.scheduleResults = new ArrayList<>();
        Constants.results = new ArrayList<>();
        Constants.logs = new ArrayList<>();
        Constants.resultPods = new ArrayList<>();
        Constants.id2Name = new HashMap<>();
        Constants.taskList = new ArrayList<>();
        Constants.nodeEnough = true;
        Constants.schedulerResult = new HashMap<>();
        Constants.faultNum = new HashMap<>();
        Constants.records = new ArrayList<>();
        Constants.ip2taskName = new HashMap<>();
        Constants.name2Ips = new HashMap<>();
        Constants.app2Con = new HashMap<>();
        Constants.repeatTime = 1;
        Constants.finishTime = 0.0;
        Constants.totalTime = 0.0;
        service = new service();
    }

    /**
     *
     * 将仿真器要用到的全局变量重置
     *
     * */
    private void resetForSimulator() {
        Constants.workloads = new ArrayList<>();
        Constants.lastTime = 0.0;
        Constants.results = new ArrayList<>();
        Constants.logs = new ArrayList<>();
        Constants.resultPods = new ArrayList<>();
        Constants.id2Name = new HashMap<>();
        Constants.taskList = new ArrayList<>();
        Constants.faultNum = new HashMap<>();
        Constants.records = new ArrayList<>();
        Constants.ip2taskName = new HashMap<>();
        Constants.name2Ips = new HashMap<>();
        Constants.finishTime = 0.0;
        Constants.totalTime = 0.0;
        service = new service();
    }

    /**
     *
     * 进行调度，调度结果<application.Name, host.Id> 存于Constants.schedulerResult中
     *
     * */
    public ResultDTO schedule(Integer a) {
        resetAllForScheduler();
        Constants.ifSimulate = false;
        try {
            service.simulate(a);
            if (!Constants.nodeEnough) {
                Log.printLine("节点资源不足");
                return ResultDTO.error("节点资源不足");
            }
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.error(e.getMessage());
        }
    }

    /**
     *
     * 进行仿真，repeatTime表示每个任务运行多少个周期
     *
     * */
    public ResultDTO simulate(Integer a, Integer repeatTime, Double lastTime) {
        resetForSimulator();
        Constants.repeatTime  = repeatTime;
        Constants.ifSimulate = true;
        Constants.lastTime = lastTime;
        try {
            service.simulate(a);
            if(!Constants.nodeEnough) {
                Log.printLine("节点资源不足");
                return ResultDTO.error("集群中节点资源不足");
            }
            return ResultDTO.success(null);
        } catch (Exception e) {
            return ResultDTO.error(e.getMessage());
        }
    }

    private ResultDTO writeJson() {
        JSONArray array = new JSONArray();
        Log.printLine("result:" + Constants.results.size());
        for(Result result: Constants.results) {
            JSONObject obj = new JSONObject().put("app", result.app).put("pausestart", result.pausestart).put("pauseend", result.pauseend).put("name", result.name).put("host", result.host).put("start", result.start).put("end", result.finish).put("size", result.size)
                    .put("mips", result.mips).put("pes", result.pes).put("type", result.type).put("datacenter", result.datacenter).put("ram", result.ram).put("containerperiod", result.period);
            array.put(obj);
        }
        try {
            String InputDir;
            if(!Objects.equals(Constants.intermediatePath, "")){
                InputDir = Constants.intermediatePath + "\\assign.json";
            }else {
                InputDir = System.getProperty("user.dir") + "\\Intermediate\\assign.json";
            }
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(InputDir), "UTF-8");

            osw.write(array.toString(4));

            osw.flush();//清空缓冲区，强制输出数据
            osw.close();//关闭输出流*/
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDTO.error(e.getMessage());
        }
        return ResultDTO.success(null);
    }


    private ResultDTO writeYaml() {
        YamlWriter writer = new YamlWriter();
        try {
            String path;
            if(!Objects.equals(Constants.outputPath,"")) {
                path = Constants.intermediatePath +  "\\yaml";
            } else {
                path = System.getProperty("user.dir") + "\\OutputFiles\\yaml";
            }
            File dir = new File(path);
            deleteDir(dir);
            dir.mkdirs();
            writer.writeYaml(path);
            return ResultDTO.success("generate successfully");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDTO.success(e.getMessage());
        }
    }

    private boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir
                        (new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        if(dir.delete()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 设置中间文件路径，默认为 System.getProperty("user.dir") + "\\Intermediate"
     * @param path 中间文件路径，不包含文件名
     */
    public void setIntermediatePath(String path) {
        File f = new File(path);
        f.mkdirs();
        Constants.intermediatePath = path;
    }

    /**
     * 设置输出目录路径，默认为 System.getProperty("user.dir")+"\\OutputFiles"
     * @param outputPath 输出目录路径，不包含文件名
     */
    public void setSchedulerOutputPath(String outputPath) {
        Constants.outputPath = outputPath;
        String path = outputPath + "\\yaml";
        deleteDir(new File(path));
        /* 新建输出结果目录 */
        File dir = new File(path);
        dir.mkdirs();
        path = outputPath + "\\jobResult";
        dir = new File(path);
        dir.mkdirs();
        path = outputPath + "\\hostUtil";
        dir = new File(path);
        dir.mkdirs();
        path = outputPath + "\\bandwidthUtil";
        dir = new File(path);
        dir.mkdirs();
        path = outputPath + "\\latency";
        dir = new File(path);
        dir.mkdirs();
        path = outputPath + "\\faultLog";
        dir = new File(path);
        dir.mkdirs();
    }

    public SchedulerApi() {
        this.service = new service();
    }


    /**
     * 创建中间文件目录和输出文件目录
     */
    public void createDir() {
        String path = System.getProperty("user.dir")+"\\OutputFiles\\yaml";
        deleteDir(new File(path));
        /* 新建输出结果目录 */
        File dir = new File(path);
        dir.mkdirs();
        path = System.getProperty("user.dir")+"\\Intermediate";
        dir = new File(path);
        dir.mkdirs();
        path = System.getProperty("user.dir")+"\\OutputFiles\\jobResult";
        dir = new File(path);
        dir.mkdirs();
        path = System.getProperty("user.dir")+"\\OutputFiles\\hostUtil";
        dir = new File(path);
        dir.mkdirs();
        path = System.getProperty("user.dir")+"\\OutputFiles\\bandwidthUtil";
        dir = new File(path);
        dir.mkdirs();
        path = System.getProperty("user.dir")+"\\OutputFiles\\latency";
        dir = new File(path);
        dir.mkdirs();
        path = System.getProperty("user.dir")+"\\OutputFiles\\faultLog";
        dir = new File(path);
        dir.mkdirs();
    }
    /**
     *
     * @param path 物理节点信息文件路径
     * @return ResultDTO.ifSuccess() 判断输入文件是否有误
     */
    public ResultDTO uploadHostsXml(String path) {
        System.out.println("hosts.xml: " + path);
        Constants.hostFile = new File(path);
        XmlUtil util = new XmlUtil(1);
        util.parseHostXml(Constants.hostFile);
        Constants.hosts = util.getHostList();
        return ResultDTO.success("上传成功");
    }

    /**
     *
     * @param path 任务信息文件路径
     * @return ResultDTO.ifSuccess() 判断输入文件是否有误
     */
    public ResultDTO uploadAppsXml(String path) {
        System.out.println("apps.xml: " + path);
        Constants.appFile = new File(path);
        XmlUtil util = new XmlUtil(1);
        util.parseHostXml(Constants.appFile);
        return ResultDTO.success("上传成功");
    }

    /**
     *
     * @param path 容器信息文件路径
     * @return ResultDTO.ifSuccess() 判断输入文件是否有误
     */
    public ResultDTO uploadContainerInfo(String path) {
        System.out.println("containerInfo: " + path);
        try{Constants.name2Container = new HashMap<>();
            XmlUtil xmlUtil = new XmlUtil(-1);
            File file = new File(path);
            String fileType = path.substring(path.lastIndexOf("."));
            //Log.printLine(fileType);
            if(fileType.equals(".xml")) {
                File containerfile = new File(path);
                Constants.containerFile = containerfile;
                xmlUtil.parseContainerInfo(containerfile);
            }else if(fileType.equals(".yml") || fileType.equals(".yaml")) {
                InputStream resource = new FileInputStream(path);
                if (Objects.nonNull(resource)) {
                    Yaml yaml = new Yaml();
                    Map<String, Object> data = yaml.load(resource);
                    ContainerInfo info = new ContainerInfo();
                    if(data.get("kind") == null) {
                        return ResultDTO.error("yaml文件缺少kind字段");
                    }else{
                        info.kind = (String) data.get("kind");
                    }
                    if(data.get("apiVersion") == null) {
                        return ResultDTO.error("yaml文件缺少apiVersion字段");
                    }else{
                        info.apiVersion = (String) data.get("apiVersion");
                    }
                    if(data.get("metadata") == null) {
                        return ResultDTO.error("yaml文件缺少metadata字段");
                    }else{
                        info.metadata = (Map<String, Object>) data.get("metadata");
                        if(info.metadata.get("name") == null) {
                            return ResultDTO.error("yaml文件的metadata字段中缺少name字段");
                        }
                    }
                    if(data.get("spec") == null) {
                        return ResultDTO.error("yaml文件缺少spec字段");
                    }else{
                        info.spec = (Map<String, Object>) data.get("spec");
                        if(info.spec.get("containers") == null || ((List<Map<String, Object>>)(info.spec.get("containers"))).isEmpty()) {
                            return ResultDTO.error("yaml文件的spec字段中缺少containers字段");
                        }
                        if(((List<Map<String, Object>>)(info.spec.get("containers"))).get(0).get("image") == null) {
                            return ResultDTO.error("yaml文件未指定容器镜像");
                        }
                    }
                    Log.printLine("解析容器信息: ");
                    Log.printLine("=====================================================");
                    Log.printLine("name:" + (String) info.metadata.get("name"));
                    Log.printLine("image:" + ((List<Map<String, Object>>)(info.spec.get("containers"))).get(0).get("image"));
                    for(Map.Entry<String, Object> i: ((List<Map<String, Object>>)(info.spec.get("containers"))).get(0).entrySet()) {
                        Log.printLine(i.getKey() + ": " + i.getValue());
                    }
                    Log.printLine("=====================================================");
                    if(Constants.containerInfoMap.get((String) info.metadata.get("name")) != null) {
                        Log.printLine((String) info.metadata.get("name") + "容器信息存在更早版本，将被当前输入版本覆盖");
                    }
                    Constants.containerInfoMap.put((String) info.metadata.get("name"), info);
                }
            }
        }catch (IOException e){
            System.out.print(e.getMessage());
            return ResultDTO.error(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDTO.error(e.getMessage());
        }
        return ResultDTO.success("上传成功");
    }

    /**
     *
     * @param path c错误注入文件路径
     * @return ResultDTO.ifSuccess() 判断输入文件是否有误
     */
    public ResultDTO uploadFault(String path) throws IOException {
        System.out.println("FaultInject.xml:" + path);
        File faultfile = new File(path);
        Constants.faultFile = faultfile;
        XmlUtil util = new XmlUtil(1);
        util.parseHostXml(faultfile);
        return ResultDTO.success("上传成功");
    }

    /**
     * 开始进行调度，流程： 调度-->根据调度结果仿真1个周期-->输出用于网络仿真的中间文件-->输出YAML文件
     * @param arithmetic 调度算法， (1)k8s--SchedulerApi.K8S  (2)maxmin--SchedulerApi.MAXMIN  (3)heft--SchedulerApi.HEFT
     * @return ResultDTO.ifSuccess() 判断调度过程是否出现错误
     */
    public ResultDTO startSchedule(Integer arithmetic) {
        try{
            ResultDTO m = new ResultDTO();
            if(Constants.hostFile == null)  {
                return ResultDTO.error("host输入文件不存在");
            }
            if(Constants.appFile == null)  {
                return ResultDTO.error("appInfo输入文件不存在");
            }
            Log.printLine("============================== 开始调度 ==============================");
            m = schedule(arithmetic);
            if(m.ifError()) {
                return m;
            }
            Log.printLine("============================== 开始仿真1个周期 ==============================");
            m = simulate(arithmetic, 1, 0.0);
            if(m.ifError()) {
                return m;
            }
            Log.printLine("============================== 开始输出中间文件 ==============================");
            m = writeJson();
            if(m.ifError()) {
                return m;
            }
            Log.printLine("============================== 开始输出YAML文件 ==============================");
            m = writeYaml();
            Constants.faultFile = null;
            DecimalFormat dft = new DecimalFormat("###.##");
            m.setData(Constants.balanceScore);
            return m;
        }catch (Exception e) {
            return ResultDTO.error(e.getMessage());
        }
    }

    /**
     *  获取调度结果
     * @return 一个  <String, Integer> 类型的 Map， 其中 String 是任务名， Integer是任务分配到的物理节点的ID, 对应物理节点在输入文件中的顺序，从 0 开始
     */
    public Map<String, Integer> getScheduleResult() {
        return Constants.schedulerResult;
    }

    /**
     * 根据任务名查找分配到的物理节点名
     * @param applicationName 任务名
     * @return 物理节点名 “”表示未找到该任务
     */
    public String getHostNameOfApplication(String applicationName) {
        if(!Constants.schedulerResult.containsKey(applicationName)) {
            return "";
        }
        Integer i =  Constants.schedulerResult.get(applicationName);
        for(Host h: Constants.hosts) {
            if(h.getId() == i) {
                return h.getName();
            }
        }
        return "";
    }

    /**
     * 根据任务名查找分配到的物理节点ID
     * @param applicationName 任务名
     * @return 物理节点ID -1表示未找到该任务
     */
    public Integer getHostIDOfApplication(String applicationName) {
        if(!Constants.schedulerResult.containsKey(applicationName)) {
            return -1;
        }
        Integer i =  Constants.schedulerResult.get(applicationName);
        return i;
    }

    /**
     *
     * @return 任务全部运行完1个周期需要的时间
     */
    public Double getFinishTime() {
        return Constants.finishTime;
    }
}
