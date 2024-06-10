package api;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.sim.cloudsimsdn.core.CloudSim;
import org.sim.cloudsimsdn.sdn.LogWriter;
import org.sim.cloudsimsdn.sdn.main.SimpleExampleInterCloud;
import org.sim.cloudsimsdn.sdn.workload.Workload;
import org.sim.controller.*;
import org.sim.service.Constants;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.sim.cloudsimsdn.core.CloudSim.assignInfoMap;
import static org.sim.cloudsimsdn.sdn.Configuration.monitoringTimeInterval;
import static org.sim.controller.MyPainter.*;
import static org.sim.controller.MyPainter.paintMultiLinkGraph;

public class SimulatorApi {
    private SimpleExampleInterCloud simulator;
//    private String input_topo = "./InputFiles/Input_TopoInfo.xml";
    private String input_host = "./InputFiles/Input_Hosts.xml";
    private String input_container = "./Intermediate/assign.json";
    public static String input_app = "./InputFiles/Input_AppInfo.xml";
    private String physicalf = "./Intermediate/physical.json";
    private String virtualf = "./Intermediate/virtual.json";
    private String latency_result = "./OutputFiles/latency/output_latency.xml";
    private String bwutil_result = "./OutputFiles/bandwidthUtil/link_utilization.xml";
    private Map<String, Long> wirelessChan_bw = new HashMap<>();
    public static long ethernetSpeed = 10000000; //10G
    public static double simulationStopTime = 1000.0; //仿真持续时间，微秒
    public static double contractRate = 0.000001; // 容器调度的单位为 1 微秒。
    public static JSONArray pure_msgs = new JSONArray();
    List<Workload> wls = new ArrayList<>();
    /**
     * 设置仿真持续时间
     */
    public void setSimulationTime(int time) {
        simulationStopTime = time;
        System.out.println("设置仿真持续时间："+simulationStopTime+"微秒");
    }

    /**
     * 设置全双工/半双工
     * @param state
     */
    public void setHalfduplex(Boolean state) {
        if (state) {
            System.out.println("半双工");
        } else {
            System.out.println("全双工");
        }
        CloudSim.HalfDuplex = state;
    }

    /**
     * 上传文件到系统
     * @param fullFilePath 文件原路径
     * @param sysFilePath 文件在系统中的路径
     */
    private static void uploadFileToSys(String fullFilePath, String sysFilePath) {
        try {
            File file = new File(fullFilePath);
            File sysfile = new File(sysFilePath);
            boolean dr = sysfile.getParentFile().mkdirs(); //创建系统目录
            // 拷贝文件到系统目录下
            FileChannel inputChannel = new FileInputStream(file).getChannel();
            FileChannel outputChannel = new FileOutputStream(sysfile).getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            inputChannel.close();
            outputChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 上传拓扑文件到系统
     * @param fullFilePath 文件原路径
     */
    public static void uploadtopo(String fullFilePath) {
//        String sysFilePath = System.getProperty("user.dir")+"\\InputFiles" + "\\Input_TopoInfo.xml";
//        System.out.println("上传topo.xml文件到系统："+sysFilePath + " : " + fullFilePath);
//        uploadFileToSys(fullFilePath, sysFilePath);
        Constants.topoFile = new File(fullFilePath);
    }

    /**
     * Checktopo()子函数，用于报错
     */
    public void PrintInvalidName(String indexstr, String filepath) {
        try{
            File file = new File(filepath);
            BufferedReader topo = new BufferedReader(new FileReader(file));//读文件
            String topoline = null;//临时的每行数据
            int linenum = 1;
            while ((topoline = topo.readLine()) != null) {
                //打印该字符串是否在此行，否则输出-1
                if (topoline.indexOf(indexstr) != -1) { //或者!r1.startsWith(indexstr)
                    System.out.println("拓扑文件第"+linenum+"行，第"+topoline.indexOf(indexstr)+"位，"+indexstr+"非法");
                }
                ++linenum;
            }
        } catch (Exception ignored){
        }
    }

    /**
     * 检查拓扑文件是否有错误
     */
    public boolean Checktopo() throws Exception {
        System.out.println("开始文件数据关联性检测");
        // 所有的valid主机名
        String xml = Files.lines(Constants.hostFile.toPath()).reduce("", String::concat);// Files.readString(Path.of(input_host));
        JSONObject hostjson = XML.toJSONObject(xml);
        JSONArray hosts = hostjson.getJSONObject("adag").getJSONArray("node");
        Set<String> hostnames = new HashSet<>();
        for(Object obj : hosts) {
            JSONObject host = (JSONObject) obj;
            hostnames.add(host.getString("name"));
        }
        // 所有的valid交换机名
        xml = Files.lines(Constants.topoFile.toPath()).reduce("", String::concat);
        JSONObject topojson = XML.toJSONObject(xml).getJSONObject("NetworkTopo");
        JSONObject swes = topojson.getJSONObject("Switches");
        JSONArray swches = new JSONArray();
        try {
            swches = swes.getJSONArray("Switch");
        } catch (Exception e){
            swches.clear();
            swches.put(swes.getJSONObject("Switch"));
        }
        for(Object obj : swches) {
            JSONObject swch = (JSONObject) obj;
            hostnames.add(swch.getString("Name"));
        }
        // 检查link有无非法名字
        JSONArray links = topojson.getJSONObject("Links").getJSONArray("Link");
        boolean flag = true;
        for(Object obj : links) {
            JSONObject link = (JSONObject) obj;
            if(!hostnames.contains(link.getString("Src"))){//非法主机名
                PrintInvalidName(link.getString("Src"), Constants.topoFile.getPath());
                flag = false;
            }
            if(!hostnames.contains(link.getString("Dst"))){//非法主机名
                PrintInvalidName(link.getString("Dst"), Constants.topoFile.getPath());
                flag = false;
            }
        }
        return flag;
    }

    /**
     * 将主机文件、拓扑文件转换为json中间文件
     */
    public void convertphytopo() throws IOException {
        String xml = Files.lines(Constants.topoFile.toPath()).reduce("", String::concat);
        Set<String> linkNameSet = new HashSet<>();
        JSONObject topojson = XML.toJSONObject(xml).getJSONObject("NetworkTopo");
        JSONObject swes = topojson.getJSONObject("Switches");
        JSONArray swches = new JSONArray();
        try {
            swches = swes.getJSONArray("Switch");
        } catch (Exception e){
            swches.clear();
            swches.put(swes.getJSONObject("Switch"));
        }
//        JSONArray links = topojson.getJSONObject("Links").getJSONArray("Link");
        //新建所有的平台
        Set<String> dcnames = new HashSet<>();
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            String dcname = swch.getString("Group");
            dcnames.add(dcname);
        }
        JSONObject topo = new JSONObject();
        // 新建wirelessnetwork dc、interswitch
        topo.accumulate("datacenters", new JSONObject()
                .put("name","net").put("type", "wirelessnetwork"));
        topo.accumulate("nodes", new JSONObject()
                .put("upports", 0)
                .put("downports", 0)
                .put("iops", 1000000000)
                .put("name","inter")
                .put("type","intercloud")
                .put("datacenter","net")
                .put("bw", 100000000) //100M
        );
        // 新建普通dc、gateways
        for(String dcname : dcnames){
            topo.accumulate("datacenters", new JSONObject()
                    .put("name",dcname).put("type", "cloud"));
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name","gw"+dcname)
                    .put("type","gateway")
                    .put("datacenter","net")
                    .put("bw", 100000000) //100M
            );
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name","gw"+dcname)
                    .put("type","gateway")
                    .put("datacenter",dcname)
                    .put("bw", 100000000) //100M
            );
        }
        // 新建所有的交换机
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            String swchname = swch.getString("Name");
            // 该交换机的类型，普通交换机 or 无线接入点
            if(swch.getString("Type").equals("WirelessAp")){
                swch.put("Type", "core");
            } else {
                swch.put("Type", "edge");
            }
            // 该交换机的带宽
            JSONArray ports = swch.getJSONObject("AswPhysPorts").getJSONArray("AswPhysPort");
            double bw = ((JSONObject)ports.get(0)).getDouble("Speed");
            // 创建交换机节点
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name",swch.getString("Name"))
                    .put("type",swch.getString("Type"))
                    .put("datacenter",swch.getString("Group"))
                    .put("ports",8)
                    .put("bw",(long)bw * 1000000));
            // 创建 交换机的链路连接
            for(Object obj1 : ports){
                JSONObject port = (JSONObject)obj1;
                //如果没有连接，跳过
                if(port.getString("LinkedHwName").equals("")){
                    continue;
                }
                //如果有重复，跳过；
                if(linkNameSet.contains(swchname+"@"+port.getString("LinkedHwName")) || linkNameSet.contains(port.getString("LinkedHwName")+"@"+swchname)){
                    continue;
                }
                topo.accumulate("links", new JSONObject()
                    .put("source",swchname)
                    .put("destination",port.getString("LinkedHwName"))
                    .put("latency", "0")
                    .put("name", "None")
                );
                linkNameSet.add(swchname+"@"+port.getString("LinkedHwName"));
                linkNameSet.add(port.getString("LinkedHwName")+"@"+swchname);
            }
        }
        // 补建links：gateway<->interswitch
        for(String dcname : dcnames){
            topo.accumulate("links", new JSONObject()
                    .put("source","inter")
                    .put("destination","gw"+dcname)
                    .put("latency","0")
                    .put("name", "gw"+dcname+"-inter")
            );
        }
        // 补建links：core<->gateway
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            if(swch.getString("Type").equals("core") || swch.getString("Type").equals("WirelessAp")){
                topo.accumulate("links", new JSONObject()
                        .put("source",swch.getString("Name"))
                        .put("destination","gw"+swch.getString("Group"))
                        .put("latency","0")
                        .put("name", "gw"+swch.getString("Group")+"-core")
                );
            }
        }
        // 新建所有的主机
        xml = Files.lines(Constants.hostFile.toPath()).reduce("", String::concat);//Files.readString(Path.of(input_host));
        JSONObject hostjson = XML.toJSONObject(xml);
        JSONArray hosts = hostjson.getJSONObject("adag").getJSONArray("node");
        for(Object obj : hosts){
            JSONObject host = (JSONObject) obj;
            topo.accumulate("nodes", new JSONObject()
                    .put("name",host.getString("name"))
                    .put("type","host")
                    .put("datacenter",host.getString("network"))
                    .put("bw",(long) host.getDouble("bandwidth")*1000000)
                    .put("pes",host.getInt("cores"))
                    .put("mips",host.getLong("mips"))
                    .put("ram", host.getInt("memory"))
                    .put("storage",host.getLong("storage")));
        }
        String jsonPrettyPrintString = topo.toString(4);
        //保存格式化后的json
        FileWriter writer = new FileWriter(Constants.intermediatePath+"\\physical.json");
        writer.write(jsonPrettyPrintString);
        writer.close();
        try {
            JSONObject apjson = topojson
                    .getJSONObject("Aps");
            JSONArray aps = apjson.getJSONArray("Ap");
            for (Object obj : aps) {
                JSONObject wirelesschan = (JSONObject) obj;
                String name = wirelesschan.getString("Group");
                long bw = (long) (wirelesschan.getDouble("Speed") * 1000); //MB
                wirelessChan_bw.put(name, bw);
            }
        }catch (Exception e) {
        }
        CloudSim.bwLimit = 1.0;

        try {
            JSONArray ups = hostjson.getJSONObject("adag").getJSONArray("utilization");
            for (Object obj : ups) {
                JSONObject up = (JSONObject) obj;
                String name = up.getString("type");
                double upvalue =  up.getDouble("up");
                if(name.equals("bandwidth")){
                    CloudSim.bwLimit = upvalue;
                }
            }
        } catch (Exception e){
            try {
                JSONArray ups = new JSONArray();
                ups.put(hostjson.getJSONObject("adag").getJSONObject("utilization"));
                for (Object obj : ups) {
                    JSONObject up = (JSONObject) obj;
                    String name = up.getString("type");
                    double upvalue =  up.getDouble("up");
                    if(name.equals("bandwidth")){
                        CloudSim.bwLimit = upvalue;
                    }
                }
            }
            catch (Exception e1){
            }
        }
        System.out.println("带宽利用率约束:"+CloudSim.bwLimit*100 +"%");
    }

    /**
     * 将app任务文件、容器分配文件转换为json中间文件
     */
    public void convertvirtopo() throws IOException{
        String content = Files.lines(Paths.get(Constants.intermediatePath+"\\assign.json")).reduce("", String::concat);// Files.readString(Path.of(input_container));
        JSONArray json = new JSONArray(content);
        JSONObject vir = new JSONObject();
        for(Object obj : json){
            JSONObject vm = (JSONObject) obj;
            vir.accumulate("nodes", vm);
        }
        JSONArray vms = vir.getJSONArray("nodes");
        for(int i=0; i<vms.length(); ++i){
            for(int j=0; j<vms.length(); ++j){
                if(j==i) {
                    continue;
                }
                vir.accumulate("flows", new JSONObject()
                        .put("name", "default")
                        .put("source", vms.getJSONObject(j).getString("name"))
                        .put("destination",vms.getJSONObject(i).getString("name"))
                );
            }
        }
        vir.put("policies", new JSONArray());
        String jsonPrettyPrintString = vir.toString(4);
        //保存格式化后的json
        FileWriter writer = new FileWriter(Constants.intermediatePath+"\\virtual.json");
        writer.write(jsonPrettyPrintString);
        writer.close();
    }

    /**
     * 使用assignInfoMap全局结构记录下每个app任务
     */
    public void convertAPP() throws IOException{
        //读result1制作ip->starttime/endtime的字典
        String content = Files.lines(Paths.get(Constants.intermediatePath+"\\assign.json")).reduce("", String::concat);// Files.readString(Path.of(input_container));
        JSONArray json = new JSONArray(content);
        assignInfoMap = new HashMap<>();
        for(Object obj : json) {
            JSONObject appinfo = (JSONObject) obj;
            AssignInfo ai = new AssignInfo(
                    appinfo.getString("app"),
                    appinfo.getString("name"), //ip
                    Double.parseDouble(appinfo.getString("start"))*contractRate,
                    Double.parseDouble(appinfo.getString("end"))*contractRate,
                    appinfo.getDouble("pausestart")*contractRate,
                    appinfo.getDouble("pauseend")*contractRate,
                    appinfo.getDouble("containerperiod")*contractRate,
                    appinfo.getString("datacenter")
            );
            assignInfoMap.put(appinfo.getString("name"), ai);
        }
    }

    /**
     * 仿真主函数，开始仿真模拟
     */
    public void run() {
        CloudSim.wirelesschan_bw = wirelessChan_bw;
        // 检查关联性数据
        try {
            if (!Checktopo()) {
                System.err.println("检测到输入文件错误");
            } else {
                System.out.println("文件数据关联性检测通过");
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        try {
            try{
                Helper h = new Helper();
                h.simForCpu(simulationStopTime);
                MyPainter p = new MyPainter("as");
                p.paintCPU();
            }catch (Exception e){
                //Do nothing
            }
            //将输入host.xml、topo.xml文件转换为中间JSON文件
            convertphytopo();
            //将输入容器调度模块的结果assign.json文件转换为中间JSON文件
            convertvirtopo();
            //将输入appinfo.xml文件转换为中间JSON文件
            convertAPP();
            String args[] = {"", Constants.intermediatePath+"\\physical.json", Constants.intermediatePath+"\\virtual.json", ""};
            LogWriter.resetLogger(Constants.outputPath+"\\link_utilization.xml");
            //带宽利用率writer
            LogWriter log = new LogWriter(Constants.outputPath+"\\link_utilization.xml");
            log.printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            log.printLine("<Links Timespan=\"" + monitoringTimeInterval + "\">");
            /* 新建一个仿真器 */
            simulator = new SimpleExampleInterCloud();
            wls.clear();
            CloudSim.linkUtilMap = new HashMap<>();
            pure_msgs.clear();
            /* 仿真的主函数 */
            wls.addAll( simulator.main(args) );
            log = LogWriter.getLogger(Constants.outputPath+"\\link_utilization.xml");
            log.printLine("</Links>");
            //延迟结果写入输出文件
            outputdelay(wls);
            //画图像。延迟、带宽
            System.out.println("绘制图像");
            paintMultiLatencyGraph(wls, true);
//            completeLinkUtil();
            paintMultiLinkGraph(CloudSim.linkUtilMap, true);
            System.out.println("仿真结束");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }

    /**
     * 打印消息时延输出文件
     */
    public void outputdelay(List<Workload> wls) throws IOException{
        //创建xml
        File file = new File(Constants.outputPath+"\\output_latency.xml");
        file.createNewFile();
        // 写入
        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<NetworkDelay>\n<Messages>\n");
        int count = 0;
        double totaltime = 0.0;

        for(int i=0; i<wls.size(); ++i){
            Workload msg = wls.get(i);
            try {
                System.out.println("容器" + msg.destVmId + "接收到容器" + msg.submitVmId + "发送的消息，网络传输时间:" + msg.networktransmissiontime);
                bw.write("\t<Message MessageName=\"" + msg.msgName + "\" Src=\"" + msg.submitVmName + "\" Dst=\"" + msg.destVmName + "\" StartTime=\"" + msg.time + "\" EndTime=\"" + msg.end2endfinishtime
                        + "\" NetworkTime=\"" + (msg.networktransmissiontime)
                        + "\" WaitingTime=\"" + (msg.dagschedulingtime)
                        + "\" EndtoEndTime=\"" + (msg.end2endfinishtime-msg.time)
                        + "\" PkgSizeKB=\"" + msg.submitPktSize + "\">\n\t</Message>\n");
                ++count;
                totaltime += msg.networktransmissiontime;
            }
            catch (Exception e) {
                bw.write("\t<Message MessageName=\"" + msg.msgName + "\" Src=\"" + msg.submitVmName + "\" Dst=\"" + msg.destVmName + "\" StartTime=\"" + msg.time + "\" EndTime=\"TimeOut\" " +
                        "NetworkTime=\"TimeOut\" WaitingTime=\"X\" EndtoEndTime=\"X\" " +
                        "PkgSizeKB=\"" + msg.submitPktSize + "\">\n\t</Message>\n");
            }
        }
        bw.write("</Messages>\n");
        bw.write("<TotalNetworkTime Time=\""+String.valueOf(totaltime)+"\"/>\n");
        bw.write("<AvgNetworkTime Time=\""+String.valueOf(totaltime/count)+"\"/>\n");
        bw.write("</NetworkDelay>");
        bw.close();
    }

    /**
     * 查看指定的多条消息
     * @param names 举例：msg1,msg2,msg3
     */
    public ResultDTO specifyMsg(String names) {
        System.out.println("查看多条消息："+names);
        String[] list = names.split("\\s+"); // 使用正则表达式\\s+表示一个或多个空格字符
        List<String> namelist = Arrays.asList(list);
        try {
            paintMultiMsgGraph(wls, names, namelist);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultDTO.success("ok");
    }

    /**
     * 查看指定的多条链路
     * @param names 举例：host1-edge1,egde1-core1
     */
    public void specifyLink(String names) {
        System.out.println("查看多条链路："+names);
        String[] list = names.split("\\s+"); // 使用正则表达式\\s+表示一个或多个空格字符
        List<String> namelist = Arrays.asList(list);
        try {
            paintOptionLinkGraph(CloudSim.linkUtilMap, names, namelist);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
