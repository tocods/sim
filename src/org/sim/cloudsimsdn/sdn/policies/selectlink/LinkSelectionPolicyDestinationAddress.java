/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.sim.cloudsimsdn.sdn.policies.selectlink;

import org.sim.cloudsimsdn.sdn.physicalcomponents.Link;
import org.sim.cloudsimsdn.sdn.physicalcomponents.Node;

import java.util.List;

public class LinkSelectionPolicyDestinationAddress implements LinkSelectionPolicy {
	public LinkSelectionPolicyDestinationAddress() {
	}

	// Choose a random link regardless of the flow

	public Link selectLink(List<Link> links, int flowId, Node src, Node dest, Node prevNode) {
		int numLinks = links.size();
		int linkid = dest.getAddress() % numLinks;
		Link link = links.get(linkid);
		return link;
	}

	@Override
	public boolean isDynamicRoutingEnabled() {
		return false;
	}
}
