package org.sim.cloudsimsdn.sdn;

import org.sim.cloudsimsdn.Pe;
import org.sim.cloudsimsdn.VmScheduler;
import org.sim.cloudsimsdn.provisioners.*;
import org.sim.cloudsimsdn.sdn.physicalcomponents.SDNHost;

import java.util.LinkedList;

public class HostFactorySimple implements HostFactory {

	@Override
	public SDNHost createHost(int ram, long bw, long storage, long pes, double mips, String name) {
		LinkedList<Pe> peList = new LinkedList<Pe>();
		int peId=0;
		for(int i=0;i<pes;i++) {
			PeProvisioner pp =  new PeProvisionerSimple(mips);
			peList.add(new Pe(peId++, pp));
		}

		RamProvisioner ramPro = new RamProvisionerSimple(ram);
		BwProvisioner bwPro = new BwProvisionerSimple(bw);
		VmScheduler vmScheduler = new VmSchedulerTimeSharedEnergy(peList);
		SDNHost newHost = new SDNHost(ramPro, bwPro, storage, peList, vmScheduler, name);

		return newHost;
	}
}
