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

import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.topology.NodePortTuple;

public class MyRoutingDijkstra {
	
	private long srcSwitchId=0L;
	private long destSwitchId=0L;

	
	private Map<Long, Set<Link>> switchLinks=null;
	private MyRoutingNeighbor myRoutingNeighbor=null;
	
	private List<Long> switchRoute=new ArrayList<>();
	
	public MyRoutingDijkstra(long srcSwitchId,long destSwitchId,Map<Long, Set<Link>> switchLinks,MyRoutingNeighbor myRoutingNeighbor) {
		this.srcSwitchId=srcSwitchId;
		this.destSwitchId=destSwitchId;
		this.switchLinks=switchLinks;
		this.myRoutingNeighbor=myRoutingNeighbor;
	}
	
	public List<Long> calSwitchRouteWithDijkstra() {	
		long startSwitchId=srcSwitchId;
		switchRoute.add(startSwitchId);
		
		long nextSwitchId=0;
		while(nextSwitchId!=destSwitchId) {
			//find the minimum costly neighbor
			nextSwitchId=getMinCostSwitch(startSwitchId);
			switchRoute.add(nextSwitchId);
			
			MyRoutingLog.debug("next switchId:"+nextSwitchId);
		
			//for next loop
			startSwitchId=nextSwitchId;			
		}
		
		return switchRoute;
	}
	
	public List<NodePortTuple> createNodePortTupleList(){
		List<NodePortTuple> result=new ArrayList<>();
		
		for(int i=0;i<switchRoute.size()-1;i++) {
			
			long startSwitchId=switchRoute.get(i);
			long nextSwitchId=switchRoute.get(i+1);
			
			//create nodePortTuple and add to the list
			addNodePortTuple(result, startSwitchId, nextSwitchId);
			
			MyRoutingLog.debug("create nodePortTuple:"+startSwitchId+","+nextSwitchId);
			
		}
		
		return result;	
	}
	
	/**
	 * get the minimum cost switch
	 * @param switchId
	 * @return
	 */
	private long getMinCostSwitch(long switchId) {
		long nextSwitchId=0;
		
		int minCost=Integer.MAX_VALUE;
		for(Long neighborSwitchId:myRoutingNeighbor.getNeighbors(switchId)) {
			int cost=MyRoutingUtils.calSwitchCost(switchId, neighborSwitchId);
			
			if(destSwitchId==neighborSwitchId) {
				//find the final switch
				nextSwitchId=destSwitchId;
				minCost=cost;
				break;
			}
			
			//forloop to find the min cost switchId
			if(cost<minCost) {
				//find min cost
				minCost=cost;
				nextSwitchId=neighborSwitchId;
			}
		}
		
		MyRoutingLog.debug("minimum cost is src:"+switchId+",dest:"+nextSwitchId+",cost:"+minCost);
		
		return nextSwitchId;
	}
	

	/**
	 * create nodePortTuple with to switchId and add to the target list
	 * @param nodePortTupleList
	 * @param switchId
	 * @param nextSwitchId
	 */
	private void addNodePortTuple(List<NodePortTuple> nodePortTupleList,long switchId,long nextSwitchId) {
		Link link=getLink(switchId, nextSwitchId);
		NodePortTuple nodePortTuple=null;
		if(link!=null) {
			nodePortTuple = new NodePortTuple(link.getSrc(), link.getSrcPort());
			nodePortTupleList.add(nodePortTuple);
            nodePortTuple = new NodePortTuple(link.getDst(), link.getDstPort());
            nodePortTupleList.add(nodePortTuple);
		}		
	}
	
	/**
	 * get link,direction:switchId=>destSwitchId
	 * @param switchId
	 * @param destSwitchId
	 * @return
	 */
	private Link getLink(long switchId,long destSwitchId) {
		for(Link link:switchLinks.get(switchId)) {
			if(link.getSrc()==switchId) {
				if(link.getDst()==destSwitchId) {
					return link;
				}
			}
		};
		
		return null;
	}
}
