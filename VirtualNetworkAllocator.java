package net.floodlightcontroller.virtualnetworkallocator;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import javafx.util.Pair;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.LinkInfo;
import net.floodlightcontroller.multipathrouting.IMultiPathRoutingService;
import net.floodlightcontroller.queuepusher.QueuePusherResponse;
import net.floodlightcontroller.queuepusher.Utils;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.routing.Link;
import net.floodlightcontroller.routing.Route;
import net.floodlightcontroller.topology.NodePortTuple;

public class VirtualNetworkAllocator implements Runnable, IFloodlightModule,
		IOFSwitchListener, ILinkDiscoveryListener {

	protected static IFloodlightProviderService floodlightProvider;
	protected static IOFSwitchService switchService;
	protected static ILinkDiscoveryService linkService;
	protected static IMultiPathRoutingService multipathService;
	protected static HashMap<String, Double> hostsFreeCPU;
	protected static HashMap<String, Pair<String, Integer>> hostToSwitchLink;
	protected static HashMap<String, Integer> hostToSwitchPort;
	protected static HashMap<String, String> nextHostMap;
	protected static ArrayList<String> commandsToSWs;
	protected static HashMap<Pair<Link,Link>, Integer> changesToLinks;
	protected static HashMap<String, Integer> changesToHostSWLinks;
	protected static HashMap<String, Integer> virtualHostsToIPAddr;
	protected static HashMap<String, Integer> virtualHostToID;
	protected static Logger logger;
	protected static Double highestCPUAvailable;
	protected static Double sumCPUforReqSplit;
	protected static int currentId;
	protected static int tempCurrentId;
	protected static Integer VNRHopCount;
	protected static Set<String> zeroToTen;
	protected static Set<String> tenToTwenty;
	protected static Set<String> twentyToThirty;
	protected static Set<String> thirtyToForty;
	protected static Set<String> fortyToFifty;
	protected static Set<String> fiftyToSixty;
	protected static Set<String> sixtyToSeventy;
	protected static Set<String> seventyToEighty;
	protected static Set<String> eightyToNinety;
	protected static Set<String> ninetyToOneHundred;
	
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
		l.add(IOFSwitchService.class);
		l.add(ILinkDiscoveryService.class);
		l.add(IRoutingService.class);
		l.add(IMultiPathRoutingService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context)
			throws FloodlightModuleException {
		floodlightProvider = context
				.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		linkService = context.getServiceImpl(ILinkDiscoveryService.class);
		multipathService = context.getServiceImpl(IMultiPathRoutingService.class);
		hostsFreeCPU = new HashMap<String, Double>();
		hostToSwitchLink = new HashMap<String, Pair<String, Integer>>();
		hostToSwitchPort = new HashMap<String, Integer>();
		nextHostMap = new HashMap<String, String>();
		zeroToTen = new HashSet<String>();
		tenToTwenty = new HashSet<String>();
		twentyToThirty = new HashSet<String>();
		thirtyToForty = new HashSet<String>();
		fortyToFifty = new HashSet<String>();
		fiftyToSixty = new HashSet<String>();
		sixtyToSeventy = new HashSet<String>();
		seventyToEighty = new HashSet<String>();
		eightyToNinety = new HashSet<String>();
		ninetyToOneHundred = new HashSet<String>();
		highestCPUAvailable = new Double(-1);
		sumCPUforReqSplit = new Double(-1);
		currentId = 1;
		tempCurrentId = 1;
		VNRHopCount = 0;
		commandsToSWs = new ArrayList<String>();
		changesToLinks = new HashMap<Pair<Link,Link>, Integer>();
		changesToHostSWLinks = new HashMap<String, Integer>();
		virtualHostsToIPAddr = new HashMap<String, Integer>();
		virtualHostToID = new HashMap<String, Integer>();
		logger = LoggerFactory.getLogger(VirtualNetworkAllocator.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		switchService.addOFSwitchListener(this);
		linkService.addListener(this);
	}

	@Override
	public void run() {
		Scanner in = new Scanner(System.in);
		
		@SuppressWarnings("unused")
		String test = in.nextLine();
		
	//************************************************************************************************//
	//************** THE FOLLOWING LINES ARE FOR THE ADAPTIVE BANDWIDTH PROOF OF CONCEPT *************//
	//************************************************************************************************//
		
//		OFFactory my13Factory = OFFactories.getFactory(OFVersion.OF_13);
//		QueuePusherResponse rsp = Utils.createSlice("00:00:00:00:00:00:00:01", "s1-eth5", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		IOFSwitch currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:01"));
//		Match myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.1"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.5"))
//			    .build();
//		OFActionSetQueue enq = null;
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		OFActionOutput output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(5))
//			    .build();
//		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		OFFlowAdd flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:01", "s1-eth5", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:01"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.2"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.6"))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(5))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:01", "s1-eth5", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:01"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.3"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.7"))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(5))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:01", "s1-eth5", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:01"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.4"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.8"))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(5))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		
//		
//		
//		rsp = Utils.createMaxSlice("00:00:00:00:00:00:00:02", "s2-eth2", 100, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:02"));
//		myMatch = my13Factory.buildMatch()
//				.setExact(MatchField.IN_PORT, OFPort.of(1))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(2))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		
//		
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:03", "s3-eth2", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:03"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.1"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.5"))
//			    .build();
//		enq = null;
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(2))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:03", "s3-eth3", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:03"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.2"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.6"))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(3))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:03", "s3-eth4", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:03"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.3"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.7"))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(4))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionLfatist.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
//		
//		rsp = Utils.createSlice("00:00:00:00:00:00:00:03", "s3-eth5", 25, false);
//		System.out.println("Slice creation response is: (rsp.err) " + rsp.err + " | andddd: (rsp.out) " + rsp.out + " | andddd: (rsp.qid) " + rsp.qid);
//		currentSwitchObject = switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:03"));
//		myMatch = my13Factory.buildMatch()
//			    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
//			    .setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.4"))
//			    .setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.8"))
//			    .build();
//		enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
//		output = my13Factory.actions().buildOutput()
//			    .setPort(OFPort.of(5))
//			    .build();
//		actionList = new ArrayList<OFAction>();
//		actionList.add(enq);
//		actionList.add(output);
//		flowAdd = my13Factory.buildFlowAdd()
//			    .setBufferId(OFBufferId.NO_BUFFER)
//			    .setHardTimeout(0)
//			    .setIdleTimeout(0)
//			    .setPriority(32768)
//			    .setMatch(myMatch)
//			    .setActions(actionList)
//			    .setTableId(TableId.of(0))
//			    .build();
//		currentSwitchObject.write(flowAdd);
		
	//************************************************************************************************//
	//************************************************************************************************//
	//************************************************************************************************//
		
		test = in.nextLine();
		
//		System.out.println("BEFORE initNetwork! DUMPING STATE!");
//		dumpState();
//		initNetwork();
//		System.out.println("AFTER initNetwork! DUMPING STATE!");
//		dumpState();
//		updatehighestCPUAvailable();
//		
//		test = in.nextLine();
		
		int processedReqs = 0;
		Double serverUtil = null;
		Double networkUtil = null;
		Double edgeNetworkUtil = null;
		Double finalNetworkUtil = null;
		ArrayList<Double> times = null;
		ArrayList<Integer> hopCountList = null;
		ArrayList<Double> serverUtilization = new ArrayList<Double>();
		ArrayList<Double> networkUtilization = new ArrayList<Double>();
		ArrayList<Integer> numberOfRequestsCount = new ArrayList<Integer>();
		
		for(int i = 2; i < 41; i++) {
			initNetwork();
			updatehighestCPUAvailable();
			times = new ArrayList<Double>();
			hopCountList = new ArrayList<Integer>();
			processedReqs = 0;
			for(int k = 0; k < 50000; k++) {
				long startTime = System.currentTimeMillis();
				boolean result = processVirtualNetwork("size_" + i + "_files/" + k );
				long estimatedTimeMilliseconds = System.currentTimeMillis() - startTime;
				Double estimatedTime = (double) estimatedTimeMilliseconds / 1000;
				if (result == false) {
					break;
				} else {
					times.add(estimatedTime);
					hopCountList.add(VNRHopCount);
					processedReqs++;
				}
			}
			Double acumHostCPU = new Double(0);
			for (Double cpuFree : hostsFreeCPU.values()) {
			    acumHostCPU += (100 - cpuFree);
			}
			serverUtil = acumHostCPU / hostsFreeCPU.size();
			
			Map<Link, LinkInfo> allLinks = linkService.getLinks();
			Double acumBW = new Double(0);
			for(Link currLink : allLinks.keySet()) {
				acumBW += (double) ((double) (currLink.getInitialBanwidth() - currLink.getFreeBandwidth()) / currLink.getInitialBanwidth()) * 100;
			}
			networkUtil = acumBW / allLinks.size();
			
			Double acumBWEdge = new Double(0);
			for(Pair<String, Integer> currentLinkk : hostToSwitchLink.values()) {
				Integer currentLinkkBW = currentLinkk.getValue();
				acumBWEdge += (double) (((double) 1000 - currentLinkkBW) / 1000) * 100;
			}
			edgeNetworkUtil =(double) (acumBWEdge / hostToSwitchLink.size());
			
			finalNetworkUtil = (networkUtil + edgeNetworkUtil) / 2;
			
			clearNetworkObjects();
			
			PrintWriter myPW = null;
			try {
			     File file = new File("/home/dcaixinha/Desktop/results/results_" + i + "_alloc_time.txt");
			     FileWriter fw = new FileWriter(file, true);
			     myPW = new PrintWriter(fw);    
			} catch (IOException e) {
				e.printStackTrace();
			}
			myPW.println(times);
			myPW.close();
			myPW = null;
			try {
			     File file = new File("/home/dcaixinha/Desktop/results/results_" + i + "_hop_count.txt");
			     FileWriter fw = new FileWriter(file, true);
			     myPW = new PrintWriter(fw);    
			} catch (IOException e) {
				e.printStackTrace();
			}
			myPW.println(hopCountList);
			myPW.close();
			
			numberOfRequestsCount.add(processedReqs);
			PrintWriter otherMyPW = null;
			try {
			     File file = new File("/home/dcaixinha/Desktop/results/results_" + i + "_number_of_proc_requests.txt");
			     FileWriter fw = new FileWriter(file, true);
			     otherMyPW = new PrintWriter(fw);    
			} catch (IOException e) {
				e.printStackTrace();
			}
			otherMyPW.println(numberOfRequestsCount);
			otherMyPW.close();
			
			serverUtilization.add(serverUtil);
			PrintWriter otherMyPW2 = null;
			try {
			     File file2 = new File("/home/dcaixinha/Desktop/results/results_" + i + "_server_util.txt");
			     FileWriter fw2 = new FileWriter(file2, true);
			     otherMyPW2 = new PrintWriter(fw2);    
			} catch (IOException e) {
				e.printStackTrace();
			}
			otherMyPW2.println(serverUtilization);
			otherMyPW2.close();
			
			networkUtilization.add(finalNetworkUtil);
			PrintWriter otherMyPW3 = null;
			try {
			     File file3 = new File("/home/dcaixinha/Desktop/results/results_" + i + "_network_util.txt");
			     FileWriter fw3 = new FileWriter(file3, true);
			     otherMyPW3 = new PrintWriter(fw3);    
			} catch (IOException e) {
				e.printStackTrace();
			}
			otherMyPW3.println(networkUtilization);
			otherMyPW3.close();
		}
		
		test = in.nextLine();

		System.out.println("\n\nExiting...\n\n");
		in.close();
	}

	public void initNetwork() {
		try {
			File fXmlFile = new File(
					"/home/dcaixinha/Desktop/physical_network.xml");
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);
			doc.getDocumentElement().normalize();

			NodeList hostsList = doc.getElementsByTagName("host");
			for (int currentHost = 0; currentHost < hostsList.getLength(); currentHost++) {
				Node hostNode = hostsList.item(currentHost);
				if (hostNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) hostNode;
					String hostName = eElement.getElementsByTagName("name")
							.item(0).getTextContent();
					Double hostCPU = Double.parseDouble(eElement
							.getElementsByTagName("CPU").item(0)
							.getTextContent());
					String nextHost = eElement.getElementsByTagName("next")
							.item(0).getTextContent();
					if (hostCPU <= 0) {
						System.err.println("The Physical Network File has a host with null or negative CPU in " + hostName + "! Please correct it and try again!");
						System.exit(-1);
					} else if (hostCPU > 10000) {
						System.err.println("The Physical Network File has a host with CPU higher than 100% in " + hostName + "! Please correct this and try again!");
						System.exit(-1);
					}
					hostsFreeCPU.put(hostName, hostCPU);
					addHostToAppropriateList(hostName, hostCPU);
					nextHostMap.put(hostName, nextHost);
					
				}
			}
			NodeList HSLinksList = doc.getElementsByTagName("HSLink");
			for (int currentLink = 0; currentLink < HSLinksList.getLength(); currentLink++) {
				Node HSLinkNode = HSLinksList.item(currentLink);
				if (HSLinkNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) HSLinkNode;
					String linkFrom = eElement.getElementsByTagName("from")
							.item(0).getTextContent();
					String linkTo = eElement.getElementsByTagName("to").item(0)
							.getTextContent();
					Integer linkBW = Integer.parseInt(eElement
							.getElementsByTagName("bandwidth").item(0)
							.getTextContent());
					Integer linkSWPort = Integer.parseInt(eElement
							.getElementsByTagName("port").item(0)
							.getTextContent());
					if (linkBW <= 0) {
						System.err.println("The Physical Network File has a link with null or negative bandwidth in " + linkFrom + "! Please correct it and try again!");
						System.exit(-1);
					} else if (linkBW > 10000) {
						System.err.println("The Physical Network File has a link with bandwidth higher than 10 Gbps in " + linkFrom + " (max. BW is 10 Gbps). Please correct this and try again!");
						System.exit(-1);
					}
					hostToSwitchLink.put(linkFrom, new Pair<String, Integer>(
								linkTo, linkBW));
					hostToSwitchPort.put(linkFrom, linkSWPort);
				}
			}
			NodeList ISLinksList = doc.getElementsByTagName("ISLink");
			for (int currentLink = 0; currentLink < ISLinksList.getLength(); currentLink++) {
				Node ISLinkNode = ISLinksList.item(currentLink);
				if (ISLinkNode.getNodeType() == Node.ELEMENT_NODE) {
					Element eElement = (Element) ISLinkNode;
					String linkFromSW = eElement.getElementsByTagName("from")
							.item(0).getTextContent();
					String linkToSW = eElement.getElementsByTagName("to")
							.item(0).getTextContent();
					Integer linkSWBandwidth = Integer.parseInt(eElement
							.getElementsByTagName("bandwidth").item(0)
							.getTextContent());
					Pair<Link, Link> linksOfSW = linkService
							.getBidirectionalLink(DatapathId.of(linkFromSW),
									DatapathId.of(linkToSW));
					if (linksOfSW != null) {
						linksOfSW.getKey().setInitialBandwidth(linkSWBandwidth);
						linksOfSW.getKey().setFreeBandwidth(linkSWBandwidth);
						linksOfSW.getValue().setInitialBandwidth(linkSWBandwidth);
						linksOfSW.getValue().setFreeBandwidth(linkSWBandwidth);
					} else {
						throw new NullPointerException(
								"Attempted to get the bidirectional link of a non-existent Switch! linkFromSW IS " + linkFromSW + " AND linkToSW IS " + linkToSW);
					}
				}
			}
			
			updatehighestCPUAvailable();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void updatehighestCPUAvailable() {
		Double availableCPU = Collections.max(hostsFreeCPU.values());
		highestCPUAvailable = availableCPU;
	}
	
	public static void clearNetworkObjects() {
		hostsFreeCPU = new HashMap<String, Double>();
		hostToSwitchLink = new HashMap<String, Pair<String, Integer>>();
		hostToSwitchPort = new HashMap<String, Integer>();
		nextHostMap = new HashMap<String, String>();
		zeroToTen = new HashSet<String>();
		tenToTwenty = new HashSet<String>();
		twentyToThirty = new HashSet<String>();
		thirtyToForty = new HashSet<String>();
		fortyToFifty = new HashSet<String>();
		fiftyToSixty = new HashSet<String>();
		sixtyToSeventy = new HashSet<String>();
		seventyToEighty = new HashSet<String>();
		eightyToNinety = new HashSet<String>();
		ninetyToOneHundred = new HashSet<String>();
		highestCPUAvailable = new Double(-1);
		sumCPUforReqSplit = new Double(-1);
		currentId = 1;
		tempCurrentId = 1;
		VNRHopCount = 0;
		commandsToSWs = new ArrayList<String>();
		changesToLinks = new HashMap<Pair<Link,Link>, Integer>();
		changesToHostSWLinks = new HashMap<String, Integer>();
		virtualHostsToIPAddr = new HashMap<String, Integer>();
		virtualHostToID = new HashMap<String, Integer>();
	}
	
	public Map.Entry<String, Double> getMostFreeServer() {
		Map.Entry<String, Double> maxEntry = null;

		for (Map.Entry<String, Double> entry : hostsFreeCPU.entrySet())
		{
		    if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
		    {
		        maxEntry = entry;
		    }
		}
		return maxEntry;
	}
	
	public boolean processVirtualNetwork(String fileName) {
		
		HashMap<String, Double> virtualNetworkCPUs = new HashMap<String, Double>();
		HashMap<String, Integer> virtualNetworkLinks =new HashMap<String, Integer>();
		HashMap<String, String> virtualHostsToHosts = new HashMap<String, String>();
		virtualHostsToIPAddr = new HashMap<String, Integer>();
		commandsToSWs = new ArrayList<String>();
		changesToLinks = new HashMap<Pair<Link, Link>, Integer>();
		changesToHostSWLinks = new HashMap<String, Integer>();
		virtualHostToID = new HashMap<String, Integer>();
		Double totalVHostLoad = new Double(0);
		tempCurrentId = currentId;
		VNRHopCount = 0;
		
		try {
			File myXmlFile = new File(
					"/home/dcaixinha/Desktop/NetworkFiles/" + fileName + ".xml");
			DocumentBuilderFactory myDbFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder myDBuilder = myDbFactory.newDocumentBuilder();
			Document myDoc = myDBuilder.parse(myXmlFile);
			myDoc.getDocumentElement().normalize();
		
			NodeList vHostsList = myDoc.getElementsByTagName("virtualHost");
			for (int currentVHost = 0; currentVHost < vHostsList.getLength(); currentVHost++) {
				Node vHostNode = vHostsList.item(currentVHost);
				if (vHostNode.getNodeType() == Node.ELEMENT_NODE) {
					Element myElement = (Element) vHostNode;
					String vHostName = myElement.getElementsByTagName("name")
							.item(0).getTextContent();
					Double vHostCPU = Double.parseDouble(myElement
							.getElementsByTagName("CPU").item(0)
							.getTextContent());
					if (vHostCPU <= 0 || vHostCPU == null) {
						System.err.println("This Virtual Network Request has a virtualHost with null or negative CPU in " + vHostName + "! Please correct it and try again!");
						System.exit(-1);
					} else if (vHostCPU > 100) {
						System.err.println("This Virtual Network Request has a virtualHost with CPU higher than 100% in " + vHostName + "! Please correct this and try again!");
						System.exit(-1);
					}
					
					virtualNetworkCPUs.put(vHostName, vHostCPU);
				}
			}
			
			NodeList vLinksList = myDoc.getElementsByTagName("virtualLink");
			for (int currentVLink = 0; currentVLink < vLinksList.getLength(); currentVLink++) {
				Node vLinkNode = vLinksList.item(currentVLink);
				if (vLinkNode.getNodeType() == Node.ELEMENT_NODE) {
					Element myElem = (Element) vLinkNode;
					String linkFrom = myElem.getElementsByTagName("from")
							.item(0).getTextContent();
					String linkTo = myElem.getElementsByTagName("to").item(0)
							.getTextContent();
					Integer linkBW = Integer.parseInt(myElem
							.getElementsByTagName("bandwidth").item(0)
							.getTextContent());
					if (linkBW <= 0) {
						System.err.println("This Virtual Network Request has a link with null or negative bandwidth in " + linkFrom + "! Please correct it and try again!");
						System.exit(-1);
					} else if (linkBW > 1000) {
						System.err.println("This Virtual Network Request has a link with bandwidth higher than 1 Gbps in " + linkFrom + " (max. BW is 1 Gbps). Please correct this and try again!");
						System.exit(-1);
					}
					int compare = linkFrom.compareTo(linkTo);
					if (compare < 0){
						Integer linkBWKey = virtualNetworkLinks.get(linkFrom + "---" + linkTo);
						if (linkBWKey != null) {
							virtualNetworkLinks.put(linkFrom + "---" + linkTo, virtualNetworkLinks.get(linkFrom + "---" + linkTo) + linkBW);
						} else {
							virtualNetworkLinks.put(linkFrom + "---" + linkTo, linkBW);
						}
					}
					else if (compare > 0) {
						Integer linkBWKey = virtualNetworkLinks.get(linkTo + "---" + linkFrom);
						if (linkBWKey != null) {
							virtualNetworkLinks.put(linkTo + "---" + linkFrom, virtualNetworkLinks.get(linkTo + "---" + linkFrom) + linkBW);
						} else {
							virtualNetworkLinks.put(linkTo + "---" + linkFrom, linkBW);
						}	
					}
					else {
						System.err.println("This Virtual Network Request has a loopback link in the node " + linkFrom + "! Please remove it and try again!");
						System.exit(-1);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		
		for (Double myDouble : virtualNetworkCPUs.values()) {
			totalVHostLoad += myDouble;
		}
		
		if(totalVHostLoad <= highestCPUAvailable) {
			
			Double delta = new Double(Double.MAX_VALUE);
			String chosenHost = "";
			Double chosenCPU = new Double(-1);
			
			Double fakevHostLoad = totalVHostLoad;
			Set<String> mySet = getAppropriateHostList(fakevHostLoad);
			
			while(true) {
			
				mySet = getAppropriateHostList(fakevHostLoad);
				
				for (String hstName : mySet) {
				    Double freeCPUOnHost = hostsFreeCPU.get(hstName);
				    if (((freeCPUOnHost - totalVHostLoad) < delta) && ((freeCPUOnHost - totalVHostLoad) > 0)) {
				    	chosenHost = hstName;
				    	chosenCPU = freeCPUOnHost;
				    	delta = freeCPUOnHost - totalVHostLoad;
				    }
				}
				
				if (chosenHost.equals("") || chosenCPU.equals(-1)) {
					fakevHostLoad += 10;
				} else {
					break;
				}
			}
			
			if (chosenHost.equals("") || chosenCPU.equals(-1)) {
				System.err.println("Unexpected error in processVirtualNetworkRequest! No host was chosen to allocate this request...");
				System.exit(-1);
			}
			
			Double newFreeCPU = new Double(chosenCPU - totalVHostLoad);
			hostsFreeCPU.put(chosenHost, newFreeCPU);
			mySet.remove(chosenHost);
			addHostToAppropriateList(chosenHost, newFreeCPU);
			updatehighestCPUAvailable();
			return true;
			
		} else {
			Set<String> alreadyAllocatedVMs;
			
			Map.Entry<String, Double> mostFreeServer = getMostFreeServer();
			Map.Entry<String, Double> currentServer = mostFreeServer;
			Map.Entry<String, Double> firstServer = mostFreeServer;
			Map<String, Integer> sortedVNRLinks = sortByComparator(virtualNetworkLinks, false);
			Set<String> sortedByBWVNRvHosts = sortedVNRLinks.keySet();

			Pair<Set<String>, Set<String>> splittedVMs = splitRequest(sortedByBWVNRvHosts, virtualNetworkCPUs, mostFreeServer, new TreeSet<String>());
			Set<String> preAllocatedVMs = splittedVMs.getKey();
			alreadyAllocatedVMs = preAllocatedVMs;
			Set<String> remainingVMs = splittedVMs.getValue();
			Set<String> apprSet = getAppropriateHostList(sumCPUforReqSplit);
			Double newHostFreeCPU = new Double(hostsFreeCPU.get(mostFreeServer.getKey()) - sumCPUforReqSplit);
			hostsFreeCPU.put(mostFreeServer.getKey(), newHostFreeCPU);
			apprSet.remove(mostFreeServer.getKey());
			addHostToAppropriateList(mostFreeServer.getKey(), newHostFreeCPU);
			for (String vHost : preAllocatedVMs) {
				virtualHostsToHosts.put(vHost, mostFreeServer.getKey());
			}
			
			while(true) {

				currentServer = getNextHost(currentServer);
				if(currentServer == firstServer){
					System.out.println("\n\nReturning false on a virtual network request...\n\n");
					return false;
				}
				Set<String> linksBetweenRemainingVMs = linksForRemainingVMs(remainingVMs, sortedVNRLinks);
				Pair<Set<String>, Set<String>> spllitedAllocatedVMs = splitRequest(linksBetweenRemainingVMs, virtualNetworkCPUs, currentServer, alreadyAllocatedVMs);
				preAllocatedVMs = spllitedAllocatedVMs.getKey();
				remainingVMs = spllitedAllocatedVMs.getValue();
				Pair<Set<String>, Set<String>> checkBWResult = checkVNRBandwidth(preAllocatedVMs, alreadyAllocatedVMs, virtualNetworkLinks, virtualHostsToHosts, currentServer);
				
				preAllocatedVMs = checkBWResult.getKey();
				Set<String> tmpRemainingVMs = checkBWResult.getValue();
				preAllocatedVMs.removeAll(tmpRemainingVMs);
				remainingVMs.addAll(tmpRemainingVMs);
				
				if (!preAllocatedVMs.isEmpty()) {
					VNRHopCount += 1;
				}
				
				Double totalCPUAllocated = getTotalCPUOfPreAllocs(virtualNetworkCPUs, preAllocatedVMs);
				
				apprSet = getAppropriateHostList(totalCPUAllocated);
				newHostFreeCPU = new Double(hostsFreeCPU.get(currentServer.getKey()) - totalCPUAllocated);
				hostsFreeCPU.put(currentServer.getKey(), newHostFreeCPU);
				apprSet.remove(currentServer.getKey());
				addHostToAppropriateList(currentServer.getKey(), newHostFreeCPU);
				for (String vHost : preAllocatedVMs) {
					virtualHostsToHosts.put(vHost, currentServer.getKey());
					alreadyAllocatedVMs.add(vHost);
				}
				if(remainingVMs.isEmpty()){					
					OFFactory my13Factory = OFFactories.getFactory(OFVersion.OF_13);
										
					for (String currentCommand : commandsToSWs) {
						String[] splittedCommand = currentCommand.split("\\$");
						String currentSwitch = splittedCommand[0];
						Integer currentOutputPort = Integer.parseInt(splittedCommand[1]);
						Integer currentSourceID = Integer.parseInt(splittedCommand[2]);
						Integer currentDestID = Integer.parseInt(splittedCommand[3]);
						Integer currentVirtualBW = Integer.parseInt(splittedCommand[4]);
						String correctCurrentSourceID = convertIntToIPAddr(currentSourceID);
						String correctCurrentDestID = convertIntToIPAddr(currentDestID);
						String SwAndPortForQueueCreation = convertSwitchAndPortToQueueFormat(currentSwitch, currentOutputPort);
						
						QueuePusherResponse rsp = Utils.createSlice(currentSwitch, SwAndPortForQueueCreation, currentVirtualBW, false);
						if(rsp.err != null) {
							System.err.println("Queue creation was not possible! Check communication with OVS and try again!");
							System.exit(-1);
						}
						
						IOFSwitch currentSwitchObject = switchService.getSwitch(DatapathId.of(currentSwitch));
						Match myMatch = my13Factory.buildMatch()
							    .setExact(MatchField.ETH_TYPE, EthType.IPv4)
							    .setExact(MatchField.IPV4_SRC, IPv4Address.of(correctCurrentSourceID))
							    .setExact(MatchField.IPV4_DST, IPv4Address.of(correctCurrentDestID))
							    .build();
						OFActionSetQueue enq = my13Factory.actions().setQueue(Integer.parseInt(rsp.qid));
						OFActionOutput output = my13Factory.actions().buildOutput()
							    .setPort(OFPort.of(currentOutputPort))
							    .build();
						
						ArrayList<OFAction> actionList = new ArrayList<OFAction>();
						actionList.add(enq);
						actionList.add(output);
						
						OFFlowAdd flowAdd = my13Factory.buildFlowAdd()
							    .setBufferId(OFBufferId.NO_BUFFER)
							    .setHardTimeout(0)
							    .setIdleTimeout(0)
							    .setPriority(32768)
							    .setMatch(myMatch)
							    .setActions(actionList)
							    .setTableId(TableId.of(0))
							    .build();
						
						currentSwitchObject.write(flowAdd);
					}

					System.out.println("\n\nReturning true on a virtual network request...\n\n");
					currentId = tempCurrentId;
					updatehighestCPUAvailable();

					return true;
				}
			}
			
		}
	}
	
	public static Set<String> linksForRemainingVMs(Set<String> remVMs, Map<String, Integer> sortedVNRLinks) {
		
		Set<String> returnSet = new TreeSet<String>();
		
		for (Map.Entry<String, Integer> entry : sortedVNRLinks.entrySet()) {
		    String currentLink = entry.getKey();
		    String[] linksList = currentLink.split("---");
    		String currLinkFrom = linksList[0];
    		String currLinkTo = linksList[1];
    		if (remVMs.size() == 1) {
    			if ( (remVMs.contains(currLinkFrom)) || (remVMs.contains(currLinkTo)) ) {
        			returnSet.add(currentLink);
        		}
    		} else {
	    		if ( (remVMs.contains(currLinkFrom)) && (remVMs.contains(currLinkTo)) ) {
	    			returnSet.add(currentLink);
	    		}
    		}
		}
		
		return returnSet;
	}
	
	public static double getTotalCPUOfPreAllocs(HashMap<String, Double> virtNetCPUs, Set<String> preAllcVMs) {
		
		Double finalResut = new Double(0);
		
		for(String currentAllcVM : preAllcVMs) {
			Double currentAllcCPU = virtNetCPUs.get(currentAllcVM);
			if(currentAllcCPU != null)
				finalResut += currentAllcCPU;
			else
				System.err.println("This should never happen! Please check getTotalCPUOfPreAllocs method!");
		}
	
		return finalResut;
	}
	
	public static Pair<Set<String>, Set<String>> checkVNRBandwidth(Set<String> preAllocVMs, Set<String> alreadyAllocVMs, HashMap<String, Integer> virtualNetworkLinks, HashMap<String, String> virtualHostsToHosts, Map.Entry<String, Double> currentServer) {
		Integer vLinksBW = null;
		Set<String> preAlloccVMs = new TreeSet<String>();
		Set<String> remmVMs = new TreeSet<String>();
		HashMap<Pair<Link,Link>, Integer> tempAllocatedLinks = new HashMap<Pair<Link,Link>, Integer>();
		HashMap<String, Integer> tempHostToSWLinks = new HashMap<String, Integer>();
		ArrayList<String> tempCommandsToSW = new ArrayList<String>();
		for(String vHost : preAllocVMs) {
			allocsLoop:
			for(String alreadyAllocvHost : alreadyAllocVMs) {
				vLinksBW = null;
				int compare = vHost.compareTo(alreadyAllocvHost);
				if (compare < 0){
					vLinksBW = virtualNetworkLinks.get(vHost + "---" + alreadyAllocvHost);
				}
				else if (compare >		 0) {
					vLinksBW = virtualNetworkLinks.get(alreadyAllocvHost + "---" + vHost);
				}
				else {
					System.err.println("alreadyAllocatedVMs and preAllocatedVMs have the same VM!!! Please check this and try again!\n\nalreadyAllocatedVMs: " + alreadyAllocVMs + "\n\npreAllocatedVMs: " + preAllocVMs);
					System.exit(-1);
				}
				if (vLinksBW == null) {
					continue;
				} else {
					Integer vHostTestIPAddr = Integer.parseInt(currentServer.getKey().substring(1));
					Integer alreadyAllocvHostTestIPAddr = Integer.parseInt(virtualHostsToHosts.get(alreadyAllocvHost).substring(1));
					if(alreadyAllocvHostTestIPAddr == null) {
						alreadyAllocvHostTestIPAddr = new Integer(tempCurrentId++);
						virtualHostsToIPAddr.put(alreadyAllocvHost, alreadyAllocvHostTestIPAddr);
					}
					Integer allocedvHostBWtoSW = hostToSwitchLink.get(virtualHostsToHosts.get(alreadyAllocvHost)).getValue();
					if (allocedvHostBWtoSW < vLinksBW) {
						tempAllocatedLinks = new HashMap<Pair<Link,Link>, Integer>();
						tempHostToSWLinks = new HashMap<String, Integer>();
						tempCommandsToSW = new ArrayList<String>();						
						remmVMs.add(vHost);
						break;
					} else {
						Integer otherTestVal1 = tempHostToSWLinks.get(alreadyAllocvHost);
					    if (otherTestVal1 != null){
					    	tempHostToSWLinks.put(virtualHostsToHosts.get(alreadyAllocvHost), tempHostToSWLinks.get(virtualHostsToHosts.get(alreadyAllocvHost)) + vLinksBW); 
					    } else {
					    	tempHostToSWLinks.put(virtualHostsToHosts.get(alreadyAllocvHost), vLinksBW);
					    }
					}
					Integer currentvHostBWtoSW = hostToSwitchLink.get(currentServer.getKey()).getValue();
					if (currentvHostBWtoSW < vLinksBW) {
						tempAllocatedLinks = new HashMap<Pair<Link,Link>, Integer>();
						tempHostToSWLinks = new HashMap<String, Integer>();
						tempCommandsToSW = new ArrayList<String>();
						remmVMs.add(vHost);
						break;
					} else {
						Integer otherTestVal2 = tempHostToSWLinks.get(currentServer.getKey());
					    if (otherTestVal2 != null){
					    	tempHostToSWLinks.put(currentServer.getKey(), tempHostToSWLinks.get(currentServer.getKey()) + vLinksBW); 
					    } else {
					    	tempHostToSWLinks.put(currentServer.getKey(), vLinksBW);
					    }
					}
					String allocedvHostSwitch = hostToSwitchLink.get(virtualHostsToHosts.get(alreadyAllocvHost)).getKey();
					String currentvHostSwitch = hostToSwitchLink.get(currentServer.getKey()).getKey();
					ArrayList<Route> myRoutes = multipathService.getRoutes(DatapathId.of(allocedvHostSwitch), DatapathId.of(currentvHostSwitch));
					DatapathId previousDp = null;
					boolean gotToEnd = false;
					if (myRoutes.isEmpty()) {
						Integer secondvHostPort = hostToSwitchPort.get(currentServer.getKey());
						tempCommandsToSW.add(currentvHostSwitch + "$" + secondvHostPort.toString() + "$" + alreadyAllocvHostTestIPAddr.toString() + "$" + vHostTestIPAddr.toString() + "$" + vLinksBW);
					} else {
						for(Route rt : myRoutes) {
							List<NodePortTuple> swPorts = rt.getPath();
							int z = 1;
							for(NodePortTuple npt : swPorts) {
								gotToEnd = false;
								DatapathId currentDp = npt.getNodeId();
								if(previousDp == null) {
									tempCommandsToSW.add(currentDp.toString() + "$" + npt.getPortId().toString() + "$" + alreadyAllocvHostTestIPAddr.toString() + "$" + vHostTestIPAddr.toString() + "$" + vLinksBW);
									previousDp = currentDp;
									z++;
									continue;
								} else if((previousDp.toString().equals(currentDp.toString()))) {
									tempCommandsToSW.add(currentDp.toString() + "$" + npt.getPortId().toString() + "$" + alreadyAllocvHostTestIPAddr.toString() + "$" + vHostTestIPAddr.toString() + "$" + vLinksBW);
									z++;
								} else {
									if (z++ == swPorts.size()) {
										Integer currentvHostPort = hostToSwitchPort.get(currentServer.getKey());
										tempCommandsToSW.add(currentDp.toString() + "$" + currentvHostPort.toString() + "$" + alreadyAllocvHostTestIPAddr.toString() + "$" + vHostTestIPAddr.toString() + "$" + vLinksBW);
								    }
									Pair<Link, Link> linksBetweenSWs = linkService.getBidirectionalLink(currentDp, previousDp);
									Link firstLink = linksBetweenSWs.getKey();
									Link secondLink = linksBetweenSWs.getValue();
									Pair<Link, Link> newLink = new Pair<Link,Link>(firstLink, secondLink);
									if(firstLink.getFreeBandwidth() < vLinksBW) {
										break;
									} else {
										Integer otherTestLinkk = tempAllocatedLinks.get(newLink);
									    if (otherTestLinkk != null){
									    	tempAllocatedLinks.put(newLink, tempAllocatedLinks.get(newLink) + vLinksBW); 
									    } else {
									    	tempAllocatedLinks.put(newLink, vLinksBW);
									    }
									}
								}
								previousDp = currentDp;
								gotToEnd = true;
							}
							if(!gotToEnd) {
								tempAllocatedLinks = new HashMap<Pair<Link,Link>, Integer>();
								tempHostToSWLinks = new HashMap<String, Integer>();
								tempCommandsToSW = new ArrayList<String>();
								remmVMs.add(vHost);
								break allocsLoop;
							} else {
								break;
							}
						}
					}
				}		
			}
			commandsToSWs.addAll(tempCommandsToSW);
			
			for (Map.Entry<Pair<Link,Link>, Integer> entry : tempAllocatedLinks.entrySet()) {
			    Pair<Link, Link> linksKey = entry.getKey();
			    Link fLink = linksKey.getKey();
				Link sLink = linksKey.getValue();
			    Integer BWValue = entry.getValue();
			    fLink.subtractBandwidth(BWValue);
			    sLink.subtractBandwidth(BWValue);
			    Integer testDoubleBW = changesToLinks.get(linksKey);
			    if (testDoubleBW != null){
			    	changesToLinks.put(linksKey, testDoubleBW + BWValue); 
			    } else {
			    	changesToLinks.put(linksKey, BWValue);
			    }
			}
			for (Map.Entry<String, Integer> myEntry : tempHostToSWLinks.entrySet()) {
				String currHst = myEntry.getKey();
				Integer BWtoRem = myEntry.getValue();
				Integer testVall = hostToSwitchLink.get(currHst).getValue();
				if (testVall != null){
					hostToSwitchLink.put(currHst, new Pair<String, Integer>(hostToSwitchLink.get(currHst).getKey(), testVall - BWtoRem)); 
			    } else {
			    	hostToSwitchLink.put(currHst, new Pair<String, Integer>(hostToSwitchLink.get(currHst).getKey(), BWtoRem)); 
			    }
			}
			tempAllocatedLinks = new HashMap<Pair<Link,Link>, Integer>();
			tempHostToSWLinks = new HashMap<String, Integer>();
			tempCommandsToSW = new ArrayList<String>();
			preAlloccVMs.add(vHost);
		}
		
		return new Pair<Set<String>, Set<String>>(preAlloccVMs, remmVMs);
	}
		
	private static Map<String, Integer> sortByComparator(Map<String, Integer> unsortMap, final boolean order)
    {

        List<Entry<String, Integer>> list = new LinkedList<Entry<String, Integer>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Entry<String, Integer>>()
        {
            public int compare(Entry<String, Integer> o1,
                    Entry<String, Integer> o2)
            {
                if (order)
                {
                    return o1.getValue().compareTo(o2.getValue());
                }
                else
                {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
        for (Entry<String, Integer> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static void printMap(Map<String, Integer> map)
    {
        for (Map.Entry<String, Integer> entry : map.entrySet())
        {
            System.out.println("Key : " + entry.getKey() + " | Value : " + entry.getValue());
        }
    }
    
    public Pair<Set<String>, Set<String>> splitRequest(Set<String> sortedVNRvHosts, Map<String, Double> virtualNetworkCPUs, Map.Entry<String, Double> mostFreeServer, Set<String> alreadyAllcVMs) {
    	Set<String> preAllocatedVMs = new TreeSet<String>();
    	Set<String> remainingVMs = new TreeSet<String>();
    	sumCPUforReqSplit = new Double(0);
    	Double acumCPULoad = new Double(0);

    	for(String entry : sortedVNRvHosts){
    		String[] vHostsList = entry.split("---");
    		String vHostFrom = vHostsList[0];
    		String vHostTo = vHostsList[1];
    		Double vHostFromCPU = virtualNetworkCPUs.get(vHostFrom);
    		Double vHostToCPU = virtualNetworkCPUs.get(vHostTo);
    		
    		if( (!alreadyAllcVMs.contains(vHostFrom)) && (!alreadyAllcVMs.contains(vHostTo)) && (!preAllocatedVMs.contains(vHostFrom)) && (!remainingVMs.contains(vHostFrom)) && (!preAllocatedVMs.contains(vHostTo)) && (!remainingVMs.contains(vHostTo)) && (vHostFromCPU + vHostToCPU) < (mostFreeServer.getValue() - acumCPULoad) ) {
    			acumCPULoad += vHostFromCPU;
    			acumCPULoad += vHostToCPU;
    			preAllocatedVMs.add(vHostFrom);
    			preAllocatedVMs.add(vHostTo);
     		}
    		if( (!alreadyAllcVMs.contains(vHostFrom)) && (!preAllocatedVMs.contains(vHostFrom)) && (!remainingVMs.contains(vHostFrom)) && (vHostFromCPU < (mostFreeServer.getValue() - acumCPULoad)) ) {
    			acumCPULoad += vHostFromCPU;
    			preAllocatedVMs.add(vHostFrom);
    		}
    		if( (!alreadyAllcVMs.contains(vHostTo)) && (!preAllocatedVMs.contains(vHostTo)) && (!remainingVMs.contains(vHostTo)) && (vHostToCPU < (mostFreeServer.getValue() - acumCPULoad)) ) {
    			acumCPULoad += vHostToCPU;
    			preAllocatedVMs.add(vHostTo);
    		}
			if (!alreadyAllcVMs.contains(vHostFrom) && (!preAllocatedVMs.contains(vHostFrom)) && (!remainingVMs.contains(vHostFrom))) {
				remainingVMs.add(vHostFrom);
			}
			if (!alreadyAllcVMs.contains(vHostTo) && (!preAllocatedVMs.contains(vHostTo)) && (!remainingVMs.contains(vHostTo))) {
				remainingVMs.add(vHostTo);
			}
    	}
    	sumCPUforReqSplit = acumCPULoad;
    	return new Pair<Set<String>, Set<String>>(preAllocatedVMs, remainingVMs);
    }
	
    public Map.Entry<String, Double> getNextHost(Map.Entry<String, Double> currentServer) {
    	String nextHostName = nextHostMap.get(currentServer.getKey());
    	
    	Map.Entry<String, Double> returnEntry = null;

		for (Map.Entry<String, Double> entry : hostsFreeCPU.entrySet())
		{
		    if (entry.getKey().equals(nextHostName))
		    {
		        returnEntry = entry;
		    }
		}
    	return returnEntry;
    }
    
	public Set<String> getAppropriateHostList(Double totalVMLoad) {
		
		boolean hasTried = false;
		
		if (totalVMLoad >= 0 && totalVMLoad < 10) {
			if (zeroToTen.size() <= 0) {
				hasTried = true;
			} else {
				return zeroToTen;
			}
		}
		if ((totalVMLoad >= 10 && totalVMLoad < 20) || hasTried) {
			if (tenToTwenty.size() <= 0) {
				hasTried = true;
			} else {
				return tenToTwenty;
			}
		}
		if ((totalVMLoad >= 20 && totalVMLoad < 30) || hasTried) {
			if (twentyToThirty.size() <= 0) {
				hasTried = true;
			} else {
				return twentyToThirty;
			}
		}
		if ((totalVMLoad >= 30 && totalVMLoad < 40) || hasTried) {
			if (thirtyToForty.size() <= 0) {
				hasTried = true;
			} else {
				return thirtyToForty;
			}
		}
		if ((totalVMLoad >= 40 && totalVMLoad < 50) || hasTried) {
			if (fortyToFifty.size() <= 0) {
				hasTried = true;
			} else {
				return fortyToFifty;
			}
		}
		if ((totalVMLoad >= 50 && totalVMLoad < 60) || hasTried) {
			if (fiftyToSixty.size() <= 0) {
				hasTried = true;
			} else {
				return fiftyToSixty;
			}
		}
		if ((totalVMLoad >= 60 && totalVMLoad < 70) || hasTried) {
			if (sixtyToSeventy.size() <= 0) {
				hasTried = true;
			} else {
				return sixtyToSeventy;
			}
		}
		if ((totalVMLoad >= 70 && totalVMLoad < 80) || hasTried) {
			if (seventyToEighty.size() <= 0) {
				hasTried = true;
			} else {
				return seventyToEighty;
			}
		}
		if ((totalVMLoad >= 80 && totalVMLoad < 90) || hasTried) {
			if (eightyToNinety.size() <= 0) {
				hasTried = true;
			} else {
				return eightyToNinety;
			}
		}
		if ((totalVMLoad >= 90 && totalVMLoad <= 100) || hasTried) {
			if (ninetyToOneHundred.size() <= 0) {
				hasTried = true;
			} else {
				return ninetyToOneHundred;
			}
		} else {
			System.err.println("Invalid totalVMLoad on findAppropriateSet! Please correct this and try again!");
			System.exit(-1);
			return null;
		}
		System.err.println("Invalid totalVMLoad on findAppropriateSet! Please correct this and try again!");
		System.exit(-1);
		return null;
	}
	
	public void addHostToAppropriateList(String hostName, Double hostFreeCPU) {
		if (hostFreeCPU >= 0 && hostFreeCPU < 10) {
			zeroToTen.add(hostName);
		} else if (hostFreeCPU >= 10 && hostFreeCPU < 20) {
			tenToTwenty.add(hostName);
		} else if (hostFreeCPU >= 20 && hostFreeCPU < 30) {
			twentyToThirty.add(hostName);
		} else if (hostFreeCPU >= 30 && hostFreeCPU < 40) {
			thirtyToForty.add(hostName);
		} else if (hostFreeCPU >= 40 && hostFreeCPU < 50) {
			fortyToFifty.add(hostName);
		} else if (hostFreeCPU >= 50 && hostFreeCPU < 60) {
			fiftyToSixty.add(hostName);
		} else if (hostFreeCPU >= 60 && hostFreeCPU < 70) {
			sixtyToSeventy.add(hostName);
		} else if (hostFreeCPU >= 70 && hostFreeCPU < 80) {
			seventyToEighty.add(hostName);
		} else if (hostFreeCPU >= 80 && hostFreeCPU < 90) {
			eightyToNinety.add(hostName);
		} else if (hostFreeCPU >= 90 && hostFreeCPU <= 100) {
			ninetyToOneHundred.add(hostName);
		} else {
			System.err.println("Invalid totalVMLoad on findAppropriateSet! Please correct this and try again!");
			System.exit(-1);
		}
	}
	
	public void dumpState() {
		System.out.println("hostsFreeCPU     - " + hostsFreeCPU);
		System.out.println("hostToSwitchLink - " + hostToSwitchLink);
		System.out.println("SET [0-10%[      - " + zeroToTen);
		System.out.println("SET [10-20%[     - " + tenToTwenty);
		System.out.println("SET [20-30%[     - " + twentyToThirty);
		System.out.println("SET [30-40%[     - " + thirtyToForty);
		System.out.println("SET [40-50%[     - " + fortyToFifty);
		System.out.println("SET [50-60%[     - " + fiftyToSixty);
		System.out.println("SET [60-70%[     - " + sixtyToSeventy);
		System.out.println("SET [70-80%[     - " + seventyToEighty);
		System.out.println("SET [80-90%[     - " + eightyToNinety);
		System.out.println("SET [90-100%]    - " + ninetyToOneHundred);
	}

	public static String convertIntToIPAddr(int toIP) {
		
		if(toIP > 10000000) {
			System.err.println("The assigned IP numbers (i.e. IDs) are overflowing (max. is 10 million)! Please check this!");
			System.exit(-1);
		}
		
		String ipStr = String.format("%d.%d.%d.%d",
				(10),
				(toIP >> 16 & 0xff),
				(toIP >> 8 & 0xff),
				(toIP & 0xff)
				         );
		return ipStr;
	}

	public static String convertSwitchAndPortToQueueFormat(String switchID, int portNumber) {
		String macAddrWithoutColon = switchID.replace(":", "");
		
		Integer sWInDecimal = Integer.parseInt(macAddrWithoutColon, 16);
		return "s" + sWInDecimal + "-eth" + portNumber;
	}
	
	@Override
	public void switchAdded(DatapathId switchId) {
	}

	@Override
	public void switchRemoved(DatapathId switchId) {
	}

	@Override
	public void switchActivated(DatapathId switchId) {
	}

	@Override
	public void switchPortChanged(DatapathId switchId, OFPortDesc port,
			PortChangeType type) {
	}

	@Override
	public void switchChanged(DatapathId switchId) {
	}

	@Override
	public void linkDiscoveryUpdate(LDUpdate update) {
	}

	@Override
	public void linkDiscoveryUpdate(List<LDUpdate> updateList) {
	}

}