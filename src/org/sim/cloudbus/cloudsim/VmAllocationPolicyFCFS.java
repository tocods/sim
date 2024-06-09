package org.sim.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.List;

public class VmAllocationPolicyFCFS extends VmAllocationPolicySimple{
    /**
     * Creates the new VmAllocationPolicySimple object.
     *
     * @param list the list
     * @pre $none
     * @post $none
     */
    public VmAllocationPolicyFCFS(List<? extends Host> list) {
        super(list);
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

        if (!getVmTable().containsKey(vm.getUid())) { // if this vm was not created
            do {// we still trying until we find a host or until we try all of them
                double moreFree = Double.MIN_VALUE;
                int idx = -1;
                //Log.printLine(freePesTmp.size());
                // we want the host with less pes in use
                for (int i = 0; i < freePesTmp.size(); i++) {
                    //Log.printLine(getScore(getHostList().get(i)));
                    if(freePesTmp.get(i) == Integer.MIN_VALUE) {
                        continue;
                    }
                    idx = i;
                    break;
                }
                if(idx == -1) {
                    return false;
                }
                Host host = getHostList().get(idx);
                result = host.vmCreate(vm);

                if (result) { // if vm were succesfully created in the host
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
