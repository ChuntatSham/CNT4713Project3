/*******************


Team members and IDs:
Junjie Yang 6138470
Chun Tat Sham 6161063
Yuhan Qin 5595982

Github link:
https://github.com/ChuntatSham/CNT4713Project3


*******************/

package net.floodlightcontroller.myrouting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.routing.Link;

public class MyRoutingNeighbor {
	
	//key:switchId value:neighbor swichId list
	private Map<Long, List<Long>> neighborMap = null;

	private Map<Long, Set<Link>> switchLinks;
	private Map<Long, IOFSwitch> switches;

	public MyRoutingNeighbor(Map<Long, Set<Link>> switchLinks, Map<Long, IOFSwitch> switches) {
		this.switchLinks = switchLinks;
		this.switches = switches;

		init();
	}

	private void init() {
		neighborMap=new HashMap<>();
		
		for (Long switchId : switches.keySet()) {
			List<Long> neighborList = new ArrayList<>();
			for (Link link : switchLinks.get(switchId)) {
				if (switchId == link.getSrc()) {
					// src swichId equals this switchId
					long neighborId = link.getDst();
					// add neighbor
					neighborList.add(neighborId);
				}
			}

			if (!neighborList.isEmpty()) {
				neighborMap.put(switchId, neighborList);
			}
		}
	}
	
	public List<Long> getNeighbors(long switchId) {
		return neighborMap.get(switchId);
	}

	public Map<Long, List<Long>> getNeighBorMap() {
		return neighborMap;
	}
	
}
