/*******************


Team members and IDs:
Junjie Yang 6138470
Chun Tat Sham 6161063
Yuhan Qin 5595982

Github link:
https://github.com/ChuntatSham/CNT4713Project3


*******************/

package net.floodlightcontroller.myrouting;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openflow.protocol.OFFlowMod;
import org.openflow.protocol.OFMatch;
import org.openflow.protocol.OFMessage;
import org.openflow.protocol.OFPacketIn;
import org.openflow.protocol.OFType;
import org.openflow.protocol.action.OFAction;
import org.openflow.protocol.action.OFActionOutput;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.SwitchPort;

import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;

import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.routing.RouteId;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.topology.NodePortTuple;

import org.openflow.util.HexString;
import org.openflow.util.U8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jnr.ffi.Struct.int16_t;

public class MyRouting implements IOFMessageListener, IFloodlightModule {

	protected IFloodlightProviderService floodlightProvider;
	protected Set<Long> macAddresses;
	protected static Logger logger;
	protected IDeviceService deviceProvider;
	protected ILinkDiscoveryService linkProvider;

	protected Map<Long, IOFSwitch> switches;
	protected Map<Link, LinkInfo> links;
	protected Collection<? extends IDevice> devices;

	protected static int uniqueFlow;
	protected ILinkDiscoveryService lds;
	protected IStaticFlowEntryPusherService flowPusher;
	protected boolean printedTopo = false;
	
	protected MyRoutingNeighbor myRoutingNeighbor = null;
	protected Map<String, Boolean> alreadyInstalled = new HashMap<String, Boolean>();


	@Override
	public String getName() {
		return MyRouting.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		return (type.equals(OFType.PACKET_IN) && (name.equals("devicemanager") || name.equals("topology"))
				|| name.equals("forwarding"));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		return false;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IDeviceService.class);
		l.add(ILinkDiscoveryService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		deviceProvider = context.getServiceImpl(IDeviceService.class);
		linkProvider = context.getServiceImpl(ILinkDiscoveryService.class);
		flowPusher = context.getServiceImpl(IStaticFlowEntryPusherService.class);
		lds = context.getServiceImpl(ILinkDiscoveryService.class);

	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {

		// Print the topology if not yet.
		if (!printedTopo) {
			System.out.println("*** Print topology");
			
			switches = floodlightProvider.getAllSwitchMap();
			myRoutingNeighbor = new MyRoutingNeighbor(lds.getSwitchLinks(), switches);
			
			// For each switch, print its neighbor switches.
			for(Map.Entry<Long, List<Long>> entry:myRoutingNeighbor.getNeighBorMap().entrySet()) {
				Long swichId=entry.getKey();
				List<Long> neighborList=entry.getValue();
				
				//format the string to print
				Collections.sort(neighborList);
				String neighborStr=neighborList.toString();
				neighborStr=neighborStr.substring(1,neighborStr.length()-1);
				System.out.println("switch "+swichId+" neighbors: "+neighborStr);	
			}

			printedTopo = true;
		}

		// eth is the packet sent by a switch and received by floodlight.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		// We process only IP packets of type 0x0800.
		if (eth.getEtherType() != 0x0800) {
			return Command.CONTINUE;
		} else {
			
			// Parse the incoming packet.
			OFPacketIn pi = (OFPacketIn) msg;
			OFMatch match = new OFMatch();
			match.loadFromPacket(pi.getPacketData(), pi.getInPort());

			// Obtain source and destination IPs.
			int srcAdd = match.getNetworkSource();
			int destAdd = match.getNetworkDestination();
			
			if(alreadyInstalled.containsKey(srcAdd+""+destAdd)) {
				//prevent repeated print
				return Command.STOP;
			}
			
			System.out.println("*** New flow packet");
			System.out.println("srcIP: " + IPv4.fromIPv4Address(srcAdd));
			System.out.println("dstIP: " + IPv4.fromIPv4Address(destAdd));

			Long srcSwitchId = getSwDpid(srcAdd);
			Long destSwitchId = getSwDpid(destAdd);

			MyRoutingLog.debug("switchId: " + srcSwitchId + "," + destSwitchId);

			// Calculate the path using Dijkstra's algorithm.
			MyRoutingDijkstra myRoutingDijkstra=new MyRoutingDijkstra(srcSwitchId, destSwitchId,lds.getSwitchLinks(),myRoutingNeighbor);
			List<Long> switchRouteList=myRoutingDijkstra.calSwitchRouteWithDijkstra();
			
			//format the string to print
			String routeStr=switchRouteList.toString();
			routeStr=routeStr.substring(1,routeStr.length()-1).replace(",", "");
			System.out.println("route "+routeStr);
			
			//create Route
			List<NodePortTuple> ports=myRoutingDijkstra.createNodePortTupleList();
			Route route = new Route(new RouteId(srcSwitchId, destSwitchId), ports);

			// add head and tail
			addHeadAndTail(route, match, cntx);

			// Write the path into the flow tables of the switches on the path.
			if (route != null) {
				installRoute(route.getPath(), match);
				MyRoutingLog.debug("route install successfully");
				
				//mark the route
				alreadyInstalled.put(srcAdd+""+destAdd, true);
			}
			

			return Command.STOP;
		}
	}

	// Install routing rules on switches.
	private void installRoute(List<NodePortTuple> path, OFMatch match) {

		OFMatch m = new OFMatch();

		m.setDataLayerType(Ethernet.TYPE_IPv4).setNetworkSource(match.getNetworkSource())
				.setNetworkDestination(match.getNetworkDestination());

		for (int i = 0; i <= path.size() - 1; i += 2) {
			short inport = path.get(i).getPortId();
			m.setInputPort(inport);
			List<OFAction> actions = new ArrayList<OFAction>();
			OFActionOutput outport = new OFActionOutput(path.get(i + 1).getPortId());
			actions.add(outport);

			OFFlowMod mod = (OFFlowMod) floodlightProvider.getOFMessageFactory().getMessage(OFType.FLOW_MOD);
			mod.setCommand(OFFlowMod.OFPFC_ADD).setIdleTimeout((short) 0).setHardTimeout((short) 0).setMatch(m)
					.setPriority((short) 105).setActions(actions)
					.setLength((short) (OFFlowMod.MINIMUM_LENGTH + OFActionOutput.MINIMUM_LENGTH));
			flowPusher.addFlow("routeFlow" + uniqueFlow, mod, HexString.toHexString(path.get(i).getNodeId()));
			uniqueFlow++;
		}
	}

	// Get switch Id(long) based on the host IP address(10.0.5.1) return the third
	// byte
	private Long getSwDpid(int is) {
		String s = IPv4.fromIPv4Address(is);
		StringTokenizer st = new StringTokenizer(s, ".");
		String des = new String();
		ArrayList<String> a = new ArrayList<String>();
		while (st.hasMoreElements()) {
			des = (String) st.nextToken();
			a.add(des);
		}
		return Long.valueOf(a.get(2));
	}

	//add head and tail the route
	private void addHeadAndTail(Route route, OFMatch match, FloodlightContext cntx) {
		List<NodePortTuple> pathNode = route.getPath();

		IDevice dstHost = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_DST_DEVICE);
		IDevice srcHost = IDeviceService.fcStore.get(cntx, IDeviceService.CONTEXT_SRC_DEVICE);

		SwitchPort[] srcDaps = srcHost.getAttachmentPoints();
		SwitchPort[] dstDaps = dstHost.getAttachmentPoints();

		NodePortTuple srcSwithport = new NodePortTuple(srcDaps[0].getSwitchDPID(), srcDaps[0].getPort());
		NodePortTuple dstSwithport = new NodePortTuple(dstDaps[0].getSwitchDPID(), dstDaps[0].getPort());

		pathNode.add(0, srcSwithport);
		pathNode.add(dstSwithport);
	}
}
