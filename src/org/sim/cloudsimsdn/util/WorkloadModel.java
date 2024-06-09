/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2012, The University of Melbourne, Australia
 */

package org.sim.cloudsimsdn.util;

import org.sim.cloudsimsdn.Cloudlet;

import java.util.List;

/**
 * Defines what a workload model should provide. A workload model generates a list of
 * jobs ({@link Cloudlet Cloudlets}) that can be dispatched to a resource by {@link Workload}.
 *
 * @author Marcos Dias de Assuncao
 * @since 5.0
 *
 * @see Workload
 * @see WorkloadFileReader
 */
public interface WorkloadModel {

	/**
	 * Generates a list of jobs ({@link Cloudlet Cloudlets}) to be executed.
	 *
	 * @return a list with the jobs ({@link Cloudlet Cloudlets})
         * generated by the workload or null in case of failure.
	 */
	List<Cloudlet> generateWorkload();

}