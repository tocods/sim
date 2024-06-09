/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.sim.cloudbus.cloudsim.power.models;

/**
 * The power model of an IBM server x3550 (2 x [Xeon X5670 2933 MHz, 6 cores], 12GB).
 * http://www.spec.org/power_ssj2008/results/res2010q2/power_ssj2008-20100315-00239.html
 * 
 * If you are using any algorithms, policies or workload included in the power package, please cite
 * the following paper:
 * 
 * Anton Beloglazov, and Rajkumar Buyya, "Optimal Online Deterministic Algorithms and Adaptive
 * Heuristics for Energy and Performance Efficient Dynamic Consolidation of Virtual Machines in
 * Cloud Data Centers", Concurrency and Computation: Practice and Experience (CCPE), Volume 24,
 * Issue 13, Pages: 1397-1420, John Wiley & Sons, Ltd, New York, USA, 2012
 * 
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 3.0
 */
public class PowerModelSpecPowerIbmX3550XeonX5670 extends PowerModelSpecPower {

	/** The power. */
	private final double[] power = { 66, 107, 120, 131, 143, 156, 173, 191, 211, 229, 247 };

	/*
	 * (non-Javadoc)
	 * @see PowerModelSpecPower#getPowerData(int)
	 */
	@Override
	protected double getPowerData(int index) {
		return power[index];
	}

}
