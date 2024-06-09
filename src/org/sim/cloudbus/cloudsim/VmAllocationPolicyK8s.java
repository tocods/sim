package org.sim.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.List;

public class VmAllocationPolicyK8s extends VmAllocationPolicySimple{
    /**
     * Creates the new VmAllocationPolicySimple object.
     *
     * @param list the list
     * @pre $none
     * @post $none
     */
    public VmAllocationPolicyK8s(List<? extends Host> list) {
        super(list);
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

    /**
     * 根据 k8s 算法对物理节点打分，任务分配到分数最高的节点
     */
    private double getScore(Host host) {
        return (balancedResourceAllocation(host) + leastRequestedPriority(host)) / 2;
    }

    @Override
    public boolean scheduleAll() {
        for(Vm v: getContainerList()) {
            if(!allocateHostForVm(v))
                return false;
        }
        return true;
    }
    @Override
    public boolean allocateHostForVm(Vm vm) {
        int requiredPes = vm.getNumberOfPes();
        boolean result = false;
        int tries = 0;
        List<Integer> freePesTmp = new ArrayList<Integer>();
        for (Integer freePes : getFreePes()) {
            freePesTmp.add(freePes);
        }
        Boolean ifStatic = true;
        if (!getVmTable().containsKey(vm.getUid())) { // if this vm was not created
            do {// we still trying until we find a host or until we try all of them
                double moreFree = Double.MIN_VALUE;
                int idx = -1;
                // 只有以下情况会静态调度：输入文件 AppInfo.xml在 application 的 hardware 字段指定了物理节点
                if(vm.getHost() != null && ifStatic) {
                    idx = vm.getHost().getId();
                    Log.printLine("静态调度");
                    ifStatic = false;
                } else {
                    for (int i = 0; i < freePesTmp.size(); i++) {
                        //Log.printLine(getScore(getHostList().get(i)));
                        if (freePesTmp.get(i) == Integer.MIN_VALUE) {
                            continue;
                        }
                        if (getScore(getHostList().get(i)) > moreFree) {
                            moreFree = getScore(getHostList().get(i));
                            idx = i;
                        }
                    }
                }

                if(idx == -1) {
                    return false;
                }
                Host host = getHostList().get(idx);
                // 就算物理节点的分数最高，它依然有可能没有足够的资源承接任务
                result = host.vmCreate(vm);

                if (result) { // 创建任务成功
                    getVmTable().put(vm.getUid(), host);
                    getUsedPes().put(vm.getUid(), requiredPes);
                    getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
                    result = true;
                    break;
                } else {
                    freePesTmp.set(idx, Integer.MIN_VALUE);
                }
                tries++;
            } while (!result && tries < getFreePes().size());
        }
        return result;
    }
}
