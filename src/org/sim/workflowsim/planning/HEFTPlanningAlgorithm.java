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
package org.sim.workflowsim.planning;


import org.apache.commons.math3.util.Pair;
import org.sim.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.sim.cloudbus.cloudsim.core.CloudSim;
import org.sim.controller.ScheduleResult;
import org.sim.service.Constants;
import org.sim.cloudbus.cloudsim.Consts;
import org.sim.cloudbus.cloudsim.Host;
import org.sim.cloudbus.cloudsim.Log;

import org.sim.workflowsim.CondorVM;
import org.sim.workflowsim.FileItem;
import org.sim.workflowsim.Task;
import org.sim.workflowsim.utils.Parameters;

import java.util.*;

/**
 * The HEFT planning algorithm.
 *
 * @author Pedro Paulo Vezzá Campos
 * @date Oct 12, 2013
 */
public class HEFTPlanningAlgorithm extends BasePlanningAlgorithm {

    private Map<Task, Map<Host, Double>> computationCosts;
    private Map<Task, Map<Task, Double>> transferCosts;
    private Map<Task, Double> rank;
    private Map<String, Host> sched;
    private Map<Integer, List<Event>> schedules;
    private Map<Task, Double> earliestFinishTimes;
    private List<Host> hosts;
    private double averageBandwidth;

    public class Event {

        public double start;
        public double finish;
        public int cpu;

        public Event(double start, double finish) {
            this.start = start;
            this.finish = finish;
            this.cpu = cpu;
        }
    }


    public class TaskRank implements Comparable<TaskRank> {

        public Task task;
        public Double rank;

        public TaskRank(Task task, Double rank) {
            this.task = task;
            this.rank = rank;
        }

        @Override
        public int compareTo(TaskRank o) {
            return o.rank.compareTo(rank);
        }
    }

    public HEFTPlanningAlgorithm() {
        computationCosts = new HashMap<>();
        transferCosts = new HashMap<>();
        rank = new HashMap<>();
        earliestFinishTimes = new HashMap<>();
        schedules = new HashMap<>();
        sched = new HashMap<>();
        hosts = new ArrayList<>(Constants.hosts);
    }

    /**
     * The main function
     */
    @Override
    public void run() {
        Log.printLine("HEFT planner running with " + getTaskList().size()
                + " tasks.");
        for (Host h : hosts)
            schedules.put(h.getId(), new ArrayList<>());
        averageBandwidth = calculateAverageBandwidth();
        // Prioritization phase
        calculateComputationCosts();
        calculateTransferCosts();
        calculateRanks();

        // Selection phase
        allocateTasks();
        Log.printLine("HEFT finish");
    }

    /**
     * Calculates the average available bandwidth among all VMs in Mbit/s
     *
     * @return Average available bandwidth in Mbit/s
     */
    private double calculateAverageBandwidth() {
        double avg = 0.0;
        for (Host host : Constants.hosts) {
            avg += host.getBw();
        }
        return avg / Constants.hosts.size();
    }

    /**
     * Populates the computationCosts field with the time in seconds to compute
     * a task in a vm.
     */
    private void calculateComputationCosts() {
        for (Task task : getTaskList()) {
            Map<Host, Double> costsVm = new HashMap<>();
            for (Host host : Constants.hosts) {
                if (host.getNumberOfPes() < task.getNumberOfPes()) {
                    costsVm.put(host, Double.MAX_VALUE);
                } else {
                    costsVm.put(host,
                            ((double) task.getCloudletTotalLength() / (double) (host.getPeList().get(0).getMips())));
                }
            }
            computationCosts.put(task, costsVm);
        }
    }

    /**
     * Populates the transferCosts map with the time in seconds to transfer all
     * files from each parent to each child
     */
    private void calculateTransferCosts() {
        Map<Integer, Boolean> ifFinish = new HashMap<>();
        // Initializing the matrix
        for (Task task1 : getTaskList()) {
            Map<Task, Double> taskTransferCosts = new HashMap<>();
            for (Task task2 : getTaskList()) {
                taskTransferCosts.put(task2, 0.0);
            }
            transferCosts.put(task1, taskTransferCosts);
            ifFinish.put(task1.getCloudletId(), false);
        }

        // Calculating the actual values
        for (Task parent : getTaskList()) {
            for (Task child : parent.getChildList()) {
                transferCosts.get(parent).put(child,
                        calculateTransferCost(parent, child));
            }
        }
    }

    /**
     * Accounts the time in seconds necessary to transfer all files described
     * between parent and child
     *
     * @param parent
     * @param child
     * @return Transfer cost in seconds
     */
    private double calculateTransferCost(Task parent, Task child) {
        /*List<FileItem> parentFiles = parent.getFileList();
        List<FileItem> childFiles = child.getFileList();

        double acc = 0.0;

        for (FileItem parentFile : parentFiles) {
            if (parentFile.getType() != Parameters.FileType.OUTPUT) {
                continue;
            }

            for (FileItem childFile : childFiles) {
                if (childFile.getType() == Parameters.FileType.INPUT
                        && childFile.getName().equals(parentFile.getName())) {
                    acc += childFile.getSize();
                    break;
                }
            }
        }*/
        double acc = 0.0;
        List<Pair<String, String>> ipAndSizes = Constants.name2Ips.get(child.name);
        if (ipAndSizes == null)
            return 0;
        for (Pair<String, String> ias : ipAndSizes) {
            String ip = ias.getKey();
            String mSize = ias.getValue();
            String destName = Constants.ip2taskName.get(ip);
            //Log.printLine("ip: " + ip + " size: " + mSize);
            if (destName != null && destName.equals(parent.name)) {
                acc += Double.parseDouble(mSize);
            }
        }

        //file Size is in Bytes, acc in MB
        acc = acc / Consts.MILLION;
        // acc in MB, averageBandwidth in Mb/s
        return acc * 8 / averageBandwidth;
    }

    /**
     * Invokes calculateRank for each task to be scheduled
     */
    private void calculateRanks() {
        for (Task task : getTaskList()) {
            calculateRank(task);
        }
    }

    /**
     * Populates rank.get(task) with the rank of task as defined in the HEFT
     * paper.
     *
     * @param task The task have the rank calculates
     * @return The rank
     */
    private double calculateRank(Task task) {
        if (rank.containsKey(task)) {
            return rank.get(task);
        }
        double averageComputationCost = 0.0;

        for (Double cost : computationCosts.get(task).values()) {
            averageComputationCost += cost;
        }

        averageComputationCost /= computationCosts.get(task).size();

        double max = 0.0;
        for (Task child : task.getChildList()) {
            double childCost = transferCosts.get(task).get(child)
                    + calculateRank(child);
            max = Math.max(max, childCost);
        }

        rank.put(task, averageComputationCost + max);

        return rank.get(task);
    }

    /**
     * Allocates all tasks to be scheduled in non-ascending order of schedule.
     */
    private void allocateTasks() {
        List<TaskRank> taskRank = new ArrayList<>();
        for (Task task : rank.keySet()) {
            taskRank.add(new TaskRank(task, rank.get(task)));
        }

        // Sorting in non-ascending order of rank
        Collections.sort(taskRank);
        for (TaskRank ranl : taskRank) {
            Host h = null;
            if (!Objects.equals(ranl.task.hardware, "")) {
                for (Host host : hosts) {
                    if (host.getName().equals(ranl.task.hardware)) {
                        h = host;
                        break;
                    }
                }
            }
            if (h != null) {
                CondorVM containerTmp = new CondorVM(ranl.task.getCloudletId(), 1, h.getVmScheduler().getPeCapacity(), ranl.task.getNumberOfPes(), (int) ranl.task.getRam(), 0, 0, "Xen", new CloudletSchedulerTimeShared());
                // 我们首先判断是否有足够的资源创建容器
                if (!h.vmCreate(containerTmp)) {
                    continue;
                }
                ranl.task.setVmId(h.getId());
            }
        }
        for (TaskRank rank : taskRank) {
            allocateTask(rank.task);
        }

    }

    /**
     * Schedules the task given in one of the VMs minimizing the earliest finish
     * time
     *
     * @param task The task to be scheduled
     * @pre All parent tasks are already scheduled
     */
    private void allocateTask(Task task) {
        Host chosenVM = null;
        double earliestFinishTime = Double.MAX_VALUE;
        double bestReadyTime = 0.0;
        double finishTime;
        if(task.getVmId() != -1) {
            //return;
            Host h = null;
            for (Host host : hosts) {
                if (host.getId() == task.getVmId()) {
                    h = host;
                    break;
                }
            }
            if (h != null) {
                double minReadyTime = 0.0;
                for (Task parent : task.getParentList()) {
                    double readyTime = earliestFinishTimes.get(parent);
                    List<Pair<String, String>> ipAndSizes = Constants.name2Ips.get(parent.name);
                    if (ipAndSizes != null) {
                        for (Pair<String, String> ias : ipAndSizes) {
                            String ip = ias.getKey();
                            String mSize = ias.getValue();
                            String destName = Constants.ip2taskName.get(ip);
                            if (destName.equals(h.getName()) && sched.get(parent.name).getId() != h.getId()) {
                                readyTime += Double.parseDouble(mSize) * 8 / (h.getBw() * Consts.MILLION);
                            }
                        }
                    }
                    minReadyTime = Math.max(minReadyTime, readyTime);
                }
                Log.printLine(minReadyTime);
                finishTime = findFinishimeStatic(task, h, minReadyTime).getSecond();
                if(finishTime != Double.MAX_VALUE) {
                    Log.printLine("静态分配");
                    earliestFinishTimes.put(task, earliestFinishTime);
                    Log.printLine(task.name + "被分配至节点" + h.getName());
                    task.setVmId(h.getId());
                    sched.put(task.name, h);
                    Constants.schedulerResult.put(task.name, h.getId());
                    Constants.scheduleResults.add(new ScheduleResult(h.getId(), task.name, task.getNumberOfPes(), task.getRam()));
                    return;
                }
            }
        }
        for (Host host : hosts) {
            double minReadyTime = 0.0;
            for (Task parent : task.getParentList()) {
                double readyTime = earliestFinishTimes.get(parent);
                List<Pair<String, String>> ipAndSizes = Constants.name2Ips.get(parent.name);
                if(ipAndSizes != null) {
                    for (Pair<String, String> ias : ipAndSizes) {
                        String ip = ias.getKey();
                        String mSize = ias.getValue();
                        String destName = Constants.ip2taskName.get(ip);
                        if (destName.equals(host.getName()) && sched.get(parent.name).getId() != host.getId()) {
                            readyTime += Double.parseDouble(mSize) * 8 / (host.getBw() * Consts.MILLION);
                        }
                    }
                }
                minReadyTime = Math.max(minReadyTime, readyTime);
            }

            finishTime = findFinishime(task, host, minReadyTime, false).getSecond();
            if(finishTime == Double.MAX_VALUE)
                continue;
            if (finishTime < earliestFinishTime) {
                bestReadyTime = minReadyTime;
                earliestFinishTime = finishTime;
                chosenVM = host;
            }
        }
        if(chosenVM == null) {
            Constants.nodeEnough = false;
        }
        findFinishime(task,chosenVM, bestReadyTime, true);
        earliestFinishTimes.put(task, earliestFinishTime);
        Log.printLine(task.name + "被分配至节点" + chosenVM.getName());
        //Constants.AppNum += 1;
        task.setVmId(chosenVM.getId());
        sched.put(task.name, chosenVM);
        Constants.schedulerResult.put(task.name, chosenVM.getId());
        Constants.scheduleResults.add(new ScheduleResult(chosenVM.getId(), task.name, task.getNumberOfPes(), task.getRam()));
    }

    /**
     * Finds the best time slot available to minimize the finish time of the
     * given task in the vm with the constraint of not scheduling it before
     * readyTime. If occupySlot is true, reserves the time slot in the schedule.
     *
     * @param task The task to have the time slot reserved
     * @param vm The vm that will execute the task
     * @param readyTime The first moment that the task is available to be
     * scheduled
     * @param occupySlot If true, reserves the time slot in the schedule.
     * @return The minimal finish time of the task in the vmn
     */
    private Pair<Double, Double> findFinishime(Task task, Host vm, double readyTime,
                                       boolean occupySlot) {
        CondorVM containerTmp = new CondorVM(task.getCloudletId(), 1, vm.getVmScheduler().getPeCapacity(), task.getNumberOfPes(), (int) task.getRam(), 0, 0, "Xen", new CloudletSchedulerTimeShared());
        // 我们首先判断是否有足够的资源创建容器
        if(!vm.vmCreate(containerTmp)) {
            return new Pair<>(Double.MAX_VALUE, Double.MAX_VALUE);
        }
        if(!occupySlot) {
            vm.vmDeallocate(containerTmp);
        }
        List<Event> sched = schedules.get(vm.getId());
        double computationCost = computationCosts.get(task).get(vm);
        double start, finish;
        int pos;

        if (sched.isEmpty()) {
            if (occupySlot) {
                sched.add(new Event(readyTime, readyTime + computationCost));
            }
            return new Pair<>(readyTime, readyTime + computationCost);
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return new Pair<>(start, start + computationCost);
        }
        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }
            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }
            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;

            if (occupySlot) {
                sched.add(pos, new Event(start, start + computationCost));
            }
            return new Pair<>(start, start + computationCost);
        }
        if (occupySlot) {
            sched.add(pos, new Event(start, finish));
        }
        return new Pair<>(start, finish);
    }

    private Pair<Double, Double> findFinishimeStatic(Task task, Host vm, double readyTime) {
        CondorVM containerTmp = new CondorVM(task.getCloudletId(), 1, vm.getVmScheduler().getPeCapacity(), task.getNumberOfPes(), (int) task.getRam(), 0, 0, "Xen", new CloudletSchedulerTimeShared());
        List<Event> sched = schedules.get(vm.getId());
        double computationCost = computationCosts.get(task).get(vm);
        double start, finish;
        int pos;

        if (sched.isEmpty()) {
            return new Pair<>(readyTime, readyTime + computationCost);
        }

        if (sched.size() == 1) {
            if (readyTime >= sched.get(0).finish) {
                pos = 1;
                start = readyTime;
            } else if (readyTime + computationCost <= sched.get(0).start) {
                pos = 0;
                start = readyTime;
            } else {
                pos = 1;
                start = sched.get(0).finish;
            }
            return new Pair<>(start, start + computationCost);
        }
        // Trivial case: Start after the latest task scheduled
        start = Math.max(readyTime, sched.get(sched.size() - 1).finish);
        finish = start + computationCost;
        int i = sched.size() - 1;
        int j = sched.size() - 2;
        pos = i + 1;
        while (j >= 0) {
            Event current = sched.get(i);
            Event previous = sched.get(j);

            if (readyTime > previous.finish) {
                if (readyTime + computationCost <= current.start) {
                    start = readyTime;
                    finish = readyTime + computationCost;
                }

                break;
            }
            if (previous.finish + computationCost <= current.start) {
                start = previous.finish;
                finish = previous.finish + computationCost;
                pos = i;
            }
            i--;
            j--;
        }

        if (readyTime + computationCost <= sched.get(0).start) {
            pos = 0;
            start = readyTime;
            return new Pair<>(start, start + computationCost);
        }
        return new Pair<>(start, finish);
    }
}
