/**
 * Copyright 2012-2013 University Of Southern California
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.sim.workflowsim;

import org.sim.cloudbus.cloudsim.Cloudlet;
import org.sim.cloudbus.cloudsim.Consts;
import org.sim.cloudbus.cloudsim.UtilizationModelFull;
import org.sim.cloudsimsdn.Log;
import org.sim.cloudsimsdn.sdn.Packet;
import org.sim.cloudsimsdn.sdn.workload.Workload;
import org.sim.service.Constants;
import org.sim.service.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Task is an extention to Cloudlet in CloudSim. It supports the implementation
 * of dependencies between tasks, which includes a list of parent tasks and a
 * list of child tasks that it has. In WorkflowSim, the Workflow Engine assure
 * that a task is released to the scheduler (ready to run) when all of its
 * parent tasks have completed successfully
 *
 * @author Weiwei Chen
 * @since WorkflowSim Toolkit 1.0
 * @date Apr 9, 2013
 */
public class Task extends Cloudlet {

    /*
     * The list of parent tasks.
     */

    public String hardware = "";

    private double pauseStartTime = 0;

    private double pauseLastTime = 0;

    private boolean ifPause = false;

    private boolean ifEndPause = true;

    public void SetPause(double s, double l) {
        this.pauseStartTime = s * Constants.averageMIPS;
        this.pauseLastTime = l * Constants.averageMIPS;
        this.ifPause = true;
        this.ifEndPause = false;
    }

    /**
     *
     * 判断是否开始暂停，参数是执行长度
     */
    public boolean IfStartPause(long length) {
        if(!ifPause)
            return false;
        if(ifEndPause)
            return false;
        pauseStartTime -= length;
        return pauseStartTime <= 0;
    }

    /**
     *
     * 判断是否结束暂停，参数是执行长度
     */
    public boolean IfEndPause(long length) {
        pauseLastTime -= length;
        if(pauseLastTime <= 0) {
            ifEndPause = true;
            return true;
        }
        return false;
    }
    public List<Message> messages = new ArrayList<>();

    /**
     *
     * 判断是否在这一微秒发送消息
     */
    public void SendMessage(long length, double current) {
        for(Message m: messages) {
            if(m.IfSend(length)) {
                sendMessage(current, m);
            }
        }

    }

    private void sendMessage(double current, Message m) {
        Workload w = m.Tran2Workload(current);
        Constants.workloads.add(w);
    }

    /**
     * 每次任务执行结束时调用，重置任务状态
     */
    public void ResetMessage() {
        for(Message m: messages) {
            m.Rest();
        }
        if(Constants.pause.containsKey(getCloudletId())) {
            SetPause(Constants.pause.get(getCloudletId()).getKey(), Constants.pause.get(getCloudletId()).getValue());
        }
    }
    private List<Task> parentList;
    /*
     * The list of child tasks.
     */
    private List<Task> childList;
    /*
     * The list of all files (input data and ouput data)
     */
    private List<FileItem> fileList;
    /*
     * The priority used for research. Not used in current version.
     */
    private int priority;
    /*
     * The depth of this task. Depth of a task is defined as the furthest path
     * from the root task to this task. It is set during the workflow parsing
     * stage.
     */
    private int depth;
    /*
     * The impact of a task. It is used in research.
     */
    private double impact;

    /*
     * The type of a task.
     */
    private String type;

    /**
     * The finish time of a task (Because cloudlet does not allow WorkflowSim to
     * update finish_time)
     */
    private double taskFinishTime;

    private double numOfPes;

    private double ram;

    private long size;

    private double periodTime;

    private double compute = 0;

    public String name;
    public boolean needWait = true;

    public void setRam(double r) {
        this.ram = r;
    }

    public void setPeriodTime(double r) {this.periodTime = r;}

    public double getPeriodTime() {return this.periodTime;}

    public Boolean shouldRepeat() {return compute < Constants.repeatTime;}

    public void finishCompute() {this.compute ++;}

    public boolean ifFirstComputeTurn() {return this.compute == 0;}

    /**
     *
     */

    /**
     * Allocates a new Task object. The task length should be greater than or
     * equal to 1.
     *
     * @param taskId the unique ID of this Task
     * @param taskLength the length or size (in MI) of this task to be executed
     * in a PowerDatacenter
     * @pre taskId >= 0
     * @pre taskLength >= 0.0
     * @post $none
     */
    public Task(
            final int taskId,
            final long taskLength) {
        /**
         * We do not use cloudletFileSize and cloudletOutputSize here. We have
         * added a list to task and thus we don't need a cloudletFileSize or
         * cloudletOutputSize here The utilizationModelCpu, utilizationModelRam,
         * and utilizationModelBw are just set to be the default mode. You can
         * change it for your own purpose.
         */
        super(taskId, taskLength, 1, 0, 0, new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull());

        this.childList = new ArrayList<>();
        this.parentList = new ArrayList<>();
        this.fileList = new ArrayList<>();
        this.impact = 0.0;
        this.taskFinishTime = -1.0;
        if(Constants.pause.containsKey(getCloudletId())) {
            SetPause(Constants.pause.get(getCloudletId()).getKey(), Constants.pause.get(getCloudletId()).getValue());
        }
    }

    /**
     * Sets the type of the task
     *
     * @param type the type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the type of the task
     *
     * @return the type of the task
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the priority of the task
     *
     * @param priority the priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Sets the depth of the task
     *
     * @param depth the depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
    }

    /**
     * Gets the priority of the task
     *
     * @return the priority of the task
     * @pre $none
     * @post $none
     */
    public int getPriority() {
        return this.priority;
    }

    /**
     * Gets the depth of the task
     *
     * @return the depth of the task
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * Gets the child list of the task
     *
     * @return the list of the children
     */
    public List<Task> getChildList() {
        return this.childList;
    }

    /**
     * Sets the child list of the task
     *
     * @param list, child list of the task
     */
    public void setChildList(List<Task> list) {
        this.childList = list;
    }

    /**
     * Sets the parent list of the task
     *
     * @param list, parent list of the task
     */
    public void setParentList(List<Task> list) {
        this.parentList = list;
    }

    /**
     * Adds the list to existing child list
     *
     * @param list, the child list to be added
     */
    public void addChildList(List<Task> list) {
        this.childList.addAll(list);
    }

    /**
     * Adds the list to existing parent list
     *
     * @param list, the parent list to be added
     */
    public void addParentList(List<Task> list) {
        this.parentList.addAll(list);
    }

    /**
     * Gets the list of the parent tasks
     *
     * @return the list of the parents
     */
    public List<Task> getParentList() {
        return this.parentList;
    }

    /**
     * Adds a task to existing child list
     *
     * @param task, the child task to be added
     */
    public void addChild(Task task) {
        this.childList.add(task);
    }

    /**
     * Adds a task to existing parent list
     *
     * @param task, the parent task to be added
     */
    public void addParent(Task task) {
        this.parentList.add(task);
    }

    /**
     * Gets the list of the files
     *
     * @return the list of files
     * @pre $none
     * @post $none
     */
    public List<FileItem> getFileList() {
        return this.fileList;
    }

    /**
     * Adds a file to existing file list
     *
     * @param file, the file to be added
     */
    public void addFile(FileItem file) {
        this.fileList.add(file);
    }

    /**
     * Sets a file list
     *
     * @param list, the file list
     */
    public void setFileList(List<FileItem> list) {
        this.fileList = list;
    }

    /**
     * Sets the impact factor
     *
     * @param impact, the impact factor
     */
    public void setImpact(double impact) {
        this.impact = impact;
    }

    /**
     * Gets the impact of the task
     *
     * @return the impact of the task
     * @pre $none
     * @post $none
     */
    public double getImpact() {
        return this.impact;
    }

    /**
     * Sets the finish time of the task (different to the one used in Cloudlet)
     *
     * @param time finish time
     */
    public void setTaskFinishTime(double time) {
        this.taskFinishTime = time;
    }

    /**
     * Gets the finish time of a task (different to the one used in Cloudlet)
     *
     * @return
     */
    public double getTaskFinishTime() {
        return this.taskFinishTime;
    }

    /**
     * Gets the total cost of processing or executing this task The original
     * getProcessingCost does not take cpu cost into it also the data file in
     * Task is stored in fileList <tt>Processing Cost = input data transfer +
     * processing cost + output transfer cost</tt> .
     *
     * @return the total cost of processing Cloudlet
     * @pre $none
     * @post $result >= 0.0
     */
    @Override
    public double getProcessingCost() {
        // cloudlet cost: execution cost...

        double cost = getCostPerSec() * getActualCPUTime();

        // ...plus input data transfer cost...
        long fileSize = 0;
        for (FileItem file : getFileList()) {
            fileSize += file.getSize() / Consts.MILLION;
        }
        cost += costPerBw * fileSize;
        return cost;
    }

    public double getNumOfPes() {
        return this.numOfPes;
    }

    public double getRam() {
        return  this.ram;
    }

    public long getSize() {
        return size;
    }
}
