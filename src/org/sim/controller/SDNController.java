package org.sim.controller;

import org.sim.cloudsimsdn.core.CloudSim;
import org.sim.cloudsimsdn.sdn.LogWriter;
import org.sim.cloudsimsdn.sdn.main.SimpleExampleInterCloud;
import org.sim.cloudsimsdn.sdn.workload.Workload;
import org.sim.cloudsimsdn.sdn.workload.WorkloadResultWriter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.sim.service.Constants;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.sim.cloudsimsdn.core.CloudSim.assignInfoMap;
import static org.sim.cloudsimsdn.sdn.Configuration.monitoringTimeInterval;
import static org.sim.controller.MyPainter.*;


public class SDNController {
    private SimpleExampleInterCloud simulator;
    private String input_topo = "./InputFiles/Input_TopoInfo.xml";
    private String input_host = "./InputFiles/Input_Hosts.xml";
    private String input_container = Constants.intermediatePath+"\\assign.json";
//    public static String input_app = "./InputFiles/Input_AppInfo.xml";
    private String physicalf = Constants.intermediatePath+"\\physical.json";
    private String virtualf = Constants.intermediatePath+"\\virtual.json";
//    public static String workloadf = "./Intermediate/messages.json";
    public static String workload_result = Constants.outputPath+"\\result_messages.csv";
    private String latency_result = Constants.outputPath+"\\latency\\output_latency.xml";
    private String bwutil_result = Constants.outputPath+"\\bandwidthUtil\\link_utilization.xml";
    private Map<String, Long> wirelessChan_bw = new HashMap<>();
//    public static Map<String, LinkUtil> linkUtilMap = new HashMap<>();
    private boolean halfDuplex = false;
    public int containerPeriodCount = 3;
    public double latencyScore = 0;
    public static long ethernetSpeed = 10000000; //10G
    public static double simulationStopTime = 1000.0; //仿真持续时间，微秒
    public static double contractRate = 0.000001; // 容器调度的单位为 1 微秒。
    public static JSONArray pure_msgs = new JSONArray();
    List<Workload> wls = new ArrayList<>();

    public String hello(){
        System.out.println("simulator可访问");
        return "This is simulator backend";
    }


    public ResultDTO setSimulationTime(String req) {
        JSONObject content = new JSONObject(req);
        int time = content.getInt("time");
        simulationStopTime = time;
        System.out.println("设置仿真持续时间："+simulationStopTime+"微秒");
        return ResultDTO.success("ok");
    }



    public ResultDTO halfduplex(String req) {
        JSONObject state = new JSONObject(req);
        halfDuplex = !state.getBoolean("switchstate");
        if (halfDuplex) {
            System.out.println("半双工");
        } else {
            System.out.println("全双工");
        }
        CloudSim.HalfDuplex = halfDuplex;
        return ResultDTO.success("ok");
    }

    public ResultDTO specifySingleMsg(String req) {
        JSONObject content = new JSONObject(req);
        String name = content.getString("msgname");
        System.out.println("查看多条消息："+name);
        String[] list = name.split("\\s+"); // 使用正则表达式\\s+表示一个或多个空格字符

        List<String> namelist = Arrays.asList(list);

        try {
            paintMultiMsgGraph(wls, name, namelist);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultDTO.success("ok");
    }

    /**
     * 将离散的数据补全为等间隔数据点
     */
    private void completeLinkUtil() {
        Set<String> keys = CloudSim.linkUtilMap.keySet();
        for (String key : keys) {
            LinkUtil lu = CloudSim.linkUtilMap.get(key);
            if(!lu.printable)
                continue;
            // 补全 recordTimes
            ListIterator<Double> Times_it = lu.recordTimes.listIterator();
            ListIterator<Double> Forward_it = lu.UnitUtilForward.listIterator();
            ListIterator<Double> Backward_it = lu.UnitUtilBackward.listIterator();
            double pre_time = Times_it.next(); //0*12345...
            Forward_it.next();
            Backward_it.next();
            while (Times_it.hasNext()){
                double next_time = Times_it.next(); //01*2345...
                Forward_it.next();
                Backward_it.next();
                if(next_time - pre_time > monitoringTimeInterval){
                    Times_it.previous(); //0*12345...
                    Forward_it.previous();
                    Backward_it.previous();
                    Times_it.add(pre_time + monitoringTimeInterval); //0(Added)*12345...
                    Forward_it.add(0.0); //新利用率0
                    Backward_it.add(0.0); //新利用率0
                    pre_time = pre_time + monitoringTimeInterval;
                    continue;
                }
                pre_time = next_time;
            }

        }
    }

    public ResultDTO specifySingleLink(String req) {
        JSONObject content = new JSONObject(req);
        String name = content.getString("linkname");
        System.out.println("查看多条链路："+name);
        String[] list = name.split("\\s+"); // 使用正则表达式\\s+表示一个或多个空格字符

        List<String> namelist = Arrays.asList(list);
        try {
            paintOptionLinkGraph(CloudSim.linkUtilMap, name, namelist);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ResultDTO.success("ok");
    }


    public ResultDTO getassign(String req) throws IOException {
        String content = Files.lines(Paths.get(input_container)).reduce("", String::concat);//Files.readString(Path.of(input_container));
        JSONArray array = new JSONArray(content);
        return ResultDTO.success(array.toString());
    }

    public ResultDTO modifyassign(String req) throws IOException {
        JSONArray array = new JSONArray(req);
        System.out.println("用户修改容器分配方案:");
        for (Object obj:array){
            JSONObject container = (JSONObject)obj;
            System.out.printf("\tapp: %s | ", container.getString("app"));
            System.out.printf("host: %s | ", container.getString("host"));
            System.out.printf("container: %s\n", container.getString("name"));
        }
        String jsonPrettyPrintString = array.toString(4);
        FileWriter writer = new FileWriter(input_container);
        writer.write(jsonPrettyPrintString);
        writer.close();
        return ResultDTO.success(array.toString());
    }
    public void convertphytopo() throws IOException {
        String xml = Files.lines(Paths.get(input_topo)).reduce("", String::concat);//Files.readString(Path.of(input_topo));
        JSONObject topojson = XML.toJSONObject(xml).getJSONObject("NetworkTopo");
        JSONObject swes = topojson.getJSONObject("Switches");
        JSONArray swches = new JSONArray();
        try {
            swches = swes.getJSONArray("Switch");
        } catch (Exception e){
            swches.clear();
            swches.put(swes.getJSONObject("Switch"));
        }
        JSONArray links = topojson.getJSONObject("Links").getJSONArray("Link");
        Set<String> dcnames = new HashSet<>();
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            String dcname = swch.getString("Network");
//            (long) swch.getDouble("Speed")*1000000)
            if(swch.getDouble("Speed") >= 100) {
                ethernetSpeed = 100000000; //100G
                monitoringTimeInterval = 0.000010; //10微秒
            }
            else if(swch.getDouble("Speed") >= 40) {
                ethernetSpeed = 40000000;
                monitoringTimeInterval = 0.000010; //10微秒
            }
            else {
                ethernetSpeed = 10000000;
                monitoringTimeInterval = 0.000010; //10微秒
            }
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
        // 新建所有的交换机、links
        for(Object obj : swches){
            JSONObject swch = (JSONObject) obj;
            if(swch.getString("Type").equals("wirelessAP")){
                swch.put("Type", "core");
            }
            topo.accumulate("nodes", new JSONObject()
                    .put("upports", 0)
                    .put("downports", 0)
                    .put("iops", 1000000000)
                    .put("name",swch.getString("Name"))
                    .put("type",swch.getString("Type"))
                    .put("datacenter",swch.getString("Network"))
                    .put("ports",swch.getInt("PortNum"))
                    .put("bw",(long) swch.getDouble("Speed")*1000000));
        }
        for(Object obj : links){
            JSONObject link = (JSONObject) obj;
            topo.accumulate("links", new JSONObject()
                    .put("source",link.getString("Src"))
                    .put("destination",link.getString("Dst"))
                    .put("latency",String.valueOf(link.getDouble("Latency")))
                    .put("name", link.getString("Name"))
            );
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
            if( swch.getString("Type").equals("core")){
                topo.accumulate("links", new JSONObject()
                        .put("source",swch.getString("Name"))
                        .put("destination","gw"+swch.getString("Network"))
                        .put("latency","0")
                        .put("name", "gw"+swch.getString("Network")+"-core")
                );
            }
        }
        // 新建所有的主机
        xml = Files.lines(Paths.get(input_host)).reduce("", String::concat);//Files.readString(Path.of(input_host));
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
        FileWriter writer = new FileWriter(physicalf);
        writer.write(jsonPrettyPrintString);
        writer.close();
        try {
            JSONObject endsys = topojson
                    .getJSONObject("EndSystems")
                    .getJSONObject("EndSystem")
                    .getJSONObject("AesPhysPorts");
            JSONArray sys = endsys.getJSONArray("AesPhysPort");
            for (Object obj : sys) {
                JSONObject wirelesschan = (JSONObject) obj;
                String name = wirelesschan.getString("Network");
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

    // 必需保持 hostname = “host” + hostid 对应关系。flows字段在解析workload文件时添加
    public void convertvirtopo() throws IOException{
        String content = Files.lines(Paths.get(input_container)).reduce("", String::concat);// Files.readString(Path.of(input_container));
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
        FileWriter writer = new FileWriter(virtualf);
        writer.write(jsonPrettyPrintString);
        writer.close();
    }
    public void convertworkload() throws IOException{
        //读result1制作ip->starttime/endtime的字典
        String content = Files.lines(Paths.get(input_container)).reduce("", String::concat);// Files.readString(Path.of(input_container));
        JSONArray json = new JSONArray(content);
        Map<String, Double> startmap = new HashMap<>();
        Map<String, Double> endmap = new HashMap<>();
        Map<String, Double> pausestartmap = new HashMap<>();
        Map<String, Double> pauseendmap = new HashMap<>();

        assignInfoMap = new HashMap<>();

        for(Object obj : json) {
            JSONObject appinfo = (JSONObject) obj;
            startmap.put(appinfo.getString("name"), Double.parseDouble(appinfo.getString("start")));
            endmap.put(appinfo.getString("name"), Double.parseDouble(appinfo.getString("end")));
            pausestartmap.put(appinfo.getString("name"), appinfo.getDouble("pausestart"));
            pauseendmap.put(appinfo.getString("name"), appinfo.getDouble("pauseend"));

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
    public ResultDTO outputdelay(List<Workload> wls) throws IOException{
        //创建xml
        File file = new File(latency_result);
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
        return ResultDTO.success("ok");
    }
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
    public boolean Checktopo() throws Exception {
        System.out.println("开始文件数据关联性检测");
        // 所有的valid主机名
        String xml = Files.lines(Paths.get(input_host)).reduce("", String::concat);// Files.readString(Path.of(input_host));
        JSONObject hostjson = XML.toJSONObject(xml);
        JSONArray hosts = hostjson.getJSONObject("adag").getJSONArray("node");
        Set<String> hostnames = new HashSet<>();
        for(Object obj : hosts) {
            JSONObject host = (JSONObject) obj;
            hostnames.add(host.getString("name"));
        }
        // 所有的valid交换机名
        xml = Files.lines(Paths.get(input_topo)).reduce("", String::concat);//Files.readString(Path.of(input_topo));
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
                PrintInvalidName(link.getString("Src"), input_topo);
                flag = false;
            }
            if(!hostnames.contains(link.getString("Dst"))){//非法主机名
                PrintInvalidName(link.getString("Dst"), input_topo);
                flag = false;
            }
        }
        return flag;
    }

    public double getLatencyTime(Workload workload){
        double finishTime = WorkloadResultWriter.getWorkloadFinishTime(workload);
        double startTime = WorkloadResultWriter.getWorkloadStartTime(workload);
        if (finishTime > 0)
            return finishTime - startTime;
        else
            return -1;
    }

    public ResultDTO run() throws IOException {
        CloudSim.wirelesschan_bw = wirelessChan_bw;
        // 检查关联性数据
        try {
            if (!Checktopo()) {
                return ResultDTO.error("检测到输入文件错误");
            } else {
                System.out.println("文件数据关联性检测通过");
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
            convertworkload();
            String args[] = {"", physicalf, virtualf, ""};
            LogWriter.resetLogger(bwutil_result);
            //带宽利用率writer
            LogWriter log = LogWriter.getLogger(bwutil_result);
            log.printLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            log.printLine("<Links Timespan=\"" + monitoringTimeInterval + "\">");
            /* 新建一个仿真器 */
            simulator = new SimpleExampleInterCloud();
            wls.clear();
            CloudSim.linkUtilMap = new HashMap<>();
            pure_msgs.clear();
            /* 仿真的主函数 */
            wls.addAll( simulator.main(args) );
            log = LogWriter.getLogger(bwutil_result);
            log.printLine("</Links>");
            //延迟结果写入输出文件
            outputdelay(wls);
            //画图像。延迟、带宽
            System.out.println("绘制图像");
            paintMultiLatencyGraph(wls, true);
//            completeLinkUtil();
            paintMultiLinkGraph(CloudSim.linkUtilMap, true);
            return ResultDTO.success("仿真结束");
        } catch (Exception e) {
            e.printStackTrace();
            return ResultDTO.error(e.getMessage());
        }
    }

}
