/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.meter_intent.app;


import org.glassfish.jersey.internal.guava.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.*;
import org.onosproject.net.flow.*;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkAdminService;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkStore;
import org.onosproject.net.meter.*;
import org.onosproject.netconf.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.core.CoreService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.channels.Selector;
import java.sql.Timestamp;
import java.util.*;

import static org.onlab.util.Tools.get;
import static org.onosproject.net.Link.Type.OPTICAL;

/**
 * Skeletal ONOS application component.
 */
@Component(immediate = true, service = {SomeInterface.class})
public class AppComponent implements SomeInterface {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MeterService meterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetconfController netconfController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkStore linkStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkAdminService linkService;


    /** Some configurable property. */
    private DeviceId deviceId = DeviceId.deviceId("of:01308030e084c740");
    private ApplicationId appId;
    private NetconfDeviceListener deviceListener = new InnerDeviceListener();
    private FlowRuleListener flowRuleListener = new FlowListener();
    private Iterable <Link> links;
    private String time = "";
    private String port= "";
    private String status= "";
    private String type = "";
    private String host = "Controller";
    private HashMap<String, ArrayList<ConnectPoint>> removed= new HashMap <>();
    private HashMap<DeviceId, HashMap<Set<PortNumber>,Set<PortNumber>>> deviceport = new HashMap <>(); // dev, pot_in port_out


    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.meter_intent");
        netconfController.addDeviceListener(deviceListener);
        flowRuleService.addListener(flowRuleListener);
        log.info("********************ATTIVAZIONE APP*************************************");
        ////////////////////////////////////////////////////////////////////////////////////
        // Device of:01308030e084c740 N1

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .meter(MeterId.meterId(51L))
                .setOutput(PortNumber.portNumber("49"));

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("51"))
                .matchVlanId(VlanId.vlanId((short)1000))
                .matchEthType(Ethernet.TYPE_IPV4);

        ForwardingObjective.Builder flowObj = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        installMeters(DeviceId.deviceId("of:01308030e084c740"),createBand(1000000),51L);

        flowObjectiveService.apply(DeviceId.deviceId("of:01308030e084c740"),flowObj.add());

        TrafficTreatment.Builder treatment1 = DefaultTrafficTreatment.builder()
                .meter(MeterId.meterId(12L))
                .setOutput(PortNumber.portNumber("49"));

        TrafficSelector.Builder selector1 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("12"))
                .matchVlanId(VlanId.vlanId((short)2000))
                .matchEthType(Ethernet.TYPE_IPV4);

        ForwardingObjective.Builder flowObj1 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector1.build())
                .withTreatment(treatment1.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        installMeters(DeviceId.deviceId("of:01308030e084c740"),createBand(1000000),12L);
        flowObjectiveService.apply(DeviceId.deviceId("of:01308030e084c740"),flowObj1.add());

        //////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012d8030e084c740 N2

        TrafficTreatment.Builder treatment2 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("52"));

        TrafficSelector.Builder selector2 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("22"))
                 .matchEthType(Ethernet.TYPE_IPV4);
                //.matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObj2 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector2.build())
                .withTreatment(treatment2.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012d8030e084c740"),flowObj2.add());

        //////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012e8030e084c740 N3

        TrafficTreatment.Builder treatment3 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("36"));

        TrafficSelector.Builder selector3 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("50"))
                .matchEthType(Ethernet.TYPE_IPV4);
                //.matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObj3 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector3.build())
                .withTreatment(treatment3.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012e8030e084c740"),flowObj3.add());

        //////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012e8030e084c740 N3

        TrafficTreatment.Builder treatment7 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("35"));

        TrafficSelector.Builder selector7 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("34"))
                 .matchEthType(Ethernet.TYPE_IPV4);
                //.matchVlanId(VlanId.vlanId((short)2000));

        ForwardingObjective.Builder flowObj7 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector7.build())
                .withTreatment(treatment7.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012e8030e084c740"),flowObj7.add());

        //////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012dd4c9ef848c40 N4

        TrafficTreatment.Builder treatment4 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("26"));

        TrafficSelector.Builder selector4 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("25"))
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VlanId.vlanId((short)1000));

        ForwardingObjective.Builder flowObj4 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector4.build())
                .withTreatment(treatment4.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012dd4c9ef848c40"),flowObj4.add());

        //////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012dd4c9ef848c40 N4

        TrafficTreatment.Builder treatment5 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("12"));

        TrafficSelector.Builder selector5 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("25"))
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VlanId.vlanId((short)2000));

        ForwardingObjective.Builder flowObj5 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector5.build())
                .withTreatment(treatment5.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012dd4c9ef848c40"),flowObj5.add());
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    void installMeters(DeviceId deviceId, Set<Band> band, Long meterId){

      MeterRequest.Builder meterBuilder = DefaultMeterRequest.builder()
                .forDevice(deviceId)
                .withUnit(Meter.Unit.KB_PER_SEC)
                .withBands(band)
                .fromApp(appId);

       meterService.submit(meterBuilder.add(), MeterId.meterId(meterId));

    }

    public Set<Band> createBand(Integer rate){
        Set<Band> bands = Sets.newHashSet();
        Band bandBuilder = DefaultBand.builder()
                .ofType(Band.Type.DROP)
                .withRate(rate)
                .build();

        bands.add(bandBuilder);
        return bands;
    }

    public void parseResponse(String eventMessage){
        try {
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            InputSource is = new InputSource();
            is.setCharacterStream(new StringReader(eventMessage));
            Document el = db.parse(is);
            //Node timeNode =  el.getElementsByTagName("eventTime").item(0);
            //time = timeNode.getTextContent();
            Node portNode =  el.getElementsByTagName("element-name").item(0);
            port = portNode.getTextContent();
            Node statusNode =  el.getElementsByTagName("status").item(0);
            status = statusNode.getTextContent();
            Node typeNode = el.getElementsByTagName("element-type").item(0);
            type = typeNode.getTextContent();

        }
        catch (Exception e) {
            log.info("Parsing failed");
        }

    }

    public void getLink( DeviceId deviceId){

        log.info("FUNCTION GET LINK");
        ArrayList<ConnectPoint> cpList = new ArrayList<>();
        String portChanged="";
        portChanged=port.replace("-","");

        links = linkService.getDeviceLinks(deviceId);

        for (Link link : links) {


            if (deviceId.equals(link.src().deviceId())){


                if (((link.src().port().toString()).equals(portChanged)) &&
                        (status.equals("DOWN"))) {
                    //sleep before port down
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    cpList.add(link.src());
                    cpList.add(link.dst());
                    removed.put(portChanged,cpList);

                    linkStore.removeOrDownLink(link.src(), link.dst());
                    linkStore.removeOrDownLink(link.dst(),link.src());
                    Timestamp tsRemove = new Timestamp(System.currentTimeMillis());
                    log.info("Link removed at {}", tsRemove);

                    //TODO parte che modifica  flow rule e meters
                    try {
                        changeOperationalMode();
                    } catch (NetconfException e) {
                        e.printStackTrace();
                    }

                    setupRecovery();

                }
                if (status.equals("UP")) {
                    if (removed.containsKey(portChanged)) {
                        log.info("PORT UP");
                        ConnectPoint cp1=removed.get(portChanged).get(0);
                        ConnectPoint cp2=removed.get(portChanged).get(1);
                        DefaultAnnotations DA =
                                DefaultAnnotations.builder().set("metric", "1.0").build();

                        LinkDescription linkDesc1 = new DefaultLinkDescription(cp1, cp2,
                                OPTICAL, false, DA);
                        LinkDescription linkDesc2 = new DefaultLinkDescription(cp2, cp1,
                                OPTICAL, false, DA);
                        //log.info(linkDesc1.toString());
                        linkStore.createOrUpdateLink(link.providerId(), linkDesc1);
                        linkStore.createOrUpdateLink(link.providerId(), linkDesc2);
                        removed.get(portChanged).clear();
                        removed.remove(portChanged);
                        log.info(removed.toString());

                    }
                }
            }
        }

    }

    public void setupRecovery(){


        // Device of:01308030e084c740 N1

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .meter(MeterId.meterId(11L))
                .setOutput(PortNumber.portNumber("49"));

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("11"))
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObj = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        installMeters(DeviceId.deviceId("of:01308030e084c740"),createBand(1000000),11L);
        installMeters(DeviceId.deviceId("of:01308030e084c740"),createBand(500000),51L);
        installMeters(DeviceId.deviceId("of:01308030e084c740"),createBand(500000),12L);

        flowObjectiveService.apply(DeviceId.deviceId("of:01308030e084c740"),flowObj.add());


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012d8030e084c740 N2

        TrafficTreatment.Builder treatment2 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("21"));

        TrafficSelector.Builder selector2 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("22"))
                .matchEthType(Ethernet.TYPE_IPV4);
                //.matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObj2 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector2.build())
                .withTreatment(treatment2.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012d8030e084c740"),flowObj2.add());


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Device f:012dd4c9ef848c40 N3

        TrafficTreatment.Builder treatment4 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("12"));

        TrafficSelector.Builder selector4 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("25"))
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObj4 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector4.build())
                .withTreatment(treatment4.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012dd4c9ef848c40"),flowObj4.add());

        //////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Device of:012e8030e084c740 N4

        TrafficTreatment.Builder treatment3 = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("36"));

        TrafficSelector.Builder selector3 = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("34"))
                .matchEthType(Ethernet.TYPE_IPV4);
                //.matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObj3 = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selector3.build())
                .withTreatment(treatment3.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012e8030e084c740"),flowObj3.add());


        //remove flow rule in  Device of:012e8030e084c740 N4
        TrafficTreatment.Builder treatmentrem = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber("36"));

        TrafficSelector.Builder selectorrem = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber("50"))
                .matchEthType(Ethernet.TYPE_IPV4);
                //.matchVlanId(VlanId.vlanId((short)3000));

        ForwardingObjective.Builder flowObjrem = DefaultForwardingObjective.builder()
                .makePermanent()
                .withPriority(40000)
                .withSelector(selectorrem.build())
                .withTreatment(treatmentrem.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId);

        flowObjectiveService.apply(DeviceId.deviceId("of:012e8030e084c740"),flowObjrem.remove());

    }

    private class InternalNotificationListener
            extends FilteringNetconfDeviceOutputEventListener
            implements NetconfDeviceOutputEventListener {

        InternalNotificationListener(NetconfDeviceInfo deviceInfo) {
            super(deviceInfo);
        }

        @Override
        public void event(NetconfDeviceOutputEvent event) {
            DeviceId deviceId = event.getDeviceInfo().getDeviceId();

            switch (event.type()) {
                case DEVICE_REPLY:
                    log.info("Device {} has reply", deviceId);
                    break;
                case DEVICE_NOTIFICATION:
                    //sleep before reporting notif
                    /*try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }*/
                    log.info("\n Device {} has notification: {}", deviceId, event.getMessagePayload());

                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    time=timestamp.toString();
                    parseResponse(event.getMessagePayload());

                    if (type.equals("port")){
                        log.info("DENTRO INIZIO OPERAZIONI STACCO PORTA");
                        Timestamp ChangeOperModesrc = new Timestamp(System.currentTimeMillis());
                        log.info("*************************************************" +
                                "Time Netconf notification recive {} *************************************************",
                                ChangeOperModesrc);

                        getLink(deviceId);
                    }
                    break;
                case DEVICE_UNREGISTERED:
                    log.warn("Device has closed session");
                    break;
                case DEVICE_ERROR:
                    log.warn("Device has error");
                    break;
                case SESSION_CLOSED:
                    log.warn("Device has closed Session");
                    break;
                default:
                    log.warn("Wrong event type {} ", event.type());
            }

        }
    }


    private void changeOperationalMode()
            throws NetconfException {
        StringBuilder sb = new StringBuilder();

        sb.append("<components xmlns='http://openconfig.net/yang/platform'>");
        sb.append("<component>");
        sb.append("<name>channel-11811</name>");
        sb.append("<oc-opt-term:optical-channel xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>");
        sb.append("<oc-opt-term:config>");
        sb.append("<oc-opt-term:operational-mode>1</oc-opt-term:operational-mode>");
        sb.append("</oc-opt-term:config>");
        sb.append("</oc-opt-term:optical-channel>");
        sb.append("</component>");
        sb.append("</components>");


        NetconfDevice src = netconfController.getNetconfDevice( (IpAddress.valueOf("10.100.101.2")),  2022);
        NetconfSession ss = src.getSession();


        boolean oks =
                ss.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!oks) {
            throw new NetconfException("error changing operational mode on device 10.100.101.2" );
        }
        Timestamp ChangeOperModesrc = new Timestamp(System.currentTimeMillis());

        NetconfDevice dst = netconfController.getNetconfDevice( IpAddress.valueOf("10.100.101.3"),  2022);
        NetconfSession sd = dst.getSession();

        boolean okd =
                sd.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!okd) {
            throw new NetconfException("error changing operational mode on device 10.100.101.3" );
        }

    }


    private class InnerDeviceListener implements NetconfDeviceListener {

        @Override
        public void deviceAdded(DeviceId deviceId) {
            log.info("CIOPOLANEK-ADDED");

            try {
                //Open netconf session
                NetconfDevice device = netconfController.getNetconfDevice(deviceId);
                NetconfSession session = device.getSession();

                //Initialize listener
                InternalNotificationListener listener = new InternalNotificationListener(device.getDeviceInfo());
                session.addDeviceOutputListener(listener);

                //Send subscription
                //session.get(createSubscriptionString(filter, deviceId), null);
                //session.startSubscription(createSubscriptionString(null,deviceId));
                session.startSubscription(null);
                Timestamp tsSubscription = new Timestamp(System.currentTimeMillis());
            } catch (NetconfException e) {
                log.error("Device add fail {}", e.getMessage());
            }

        }

        @Override
        public void deviceRemoved(DeviceId deviceId) {
            log.info("CIOPOLANEK-REMOVED");
        }
    }

    class FlowListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event){

            if(event.type().equals(FlowRuleEvent.Type.RULE_ADD_REQUESTED)){
                Timestamp tsSubscription = new Timestamp(System.currentTimeMillis());

                log.info("*************************************************" +
                                "Time FlowRule ADD REQUEST {} Device Id{}*************************************************",
                        tsSubscription, event.subject().deviceId());
            }


            if(event.type().equals(FlowRuleEvent.Type.RULE_ADDED)){
                Timestamp tsSubscription = new Timestamp(System.currentTimeMillis());

                log.info("*************************************************" +
                        "Time FlowRule ADDED {} Device Id{}*************************************************",
                        tsSubscription, event.subject().deviceId() );
            }

        }

    }

    void initialize(){

        // Device of:01308030e084c740 N1
        HashMap <Set<PortNumber>,Set<PortNumber>> portInOut = new HashMap <>();
        Set<PortNumber> portIn =  Sets.newHashSet();
        Set<PortNumber> portOut =  Sets.newHashSet();

        portIn.add(PortNumber.portNumber(51L));
        portIn.add(PortNumber.portNumber(12L));
        portIn.add(PortNumber.portNumber(11L));
        portOut.add(PortNumber.portNumber(49L));
        portInOut.put(portIn,portOut);

        deviceport.put(DeviceId.deviceId("of:01308030e084c740"),portInOut);
        portIn.clear();
        portOut.clear();
        portInOut.clear();

        // Device of:012d8030e084c740 N2

        portIn.add(PortNumber.portNumber(22L));
        portOut.add(PortNumber.portNumber(21L));
        portOut.add(PortNumber.portNumber(52L));
        portInOut.put(portIn,portOut);

        deviceport.put(DeviceId.deviceId("of:012d8030e084c740 "),portInOut);
        portIn.clear();
        portOut.clear();
        portInOut.clear();

        // of:012e8030e084c740 N3

        portIn.add(PortNumber.portNumber(25L));
        portOut.add(PortNumber.portNumber(12L));
        portOut.add(PortNumber.portNumber(26L));
        portInOut.put(portIn,portOut);

        deviceport.put(DeviceId.deviceId("of:012e8030e084c740 "),portInOut);
        portIn.clear();
        portOut.clear();
        portInOut.clear();

        // Device of:012e8030e084c740 N4

        portIn.add(PortNumber.portNumber(50L));
        portIn.add(PortNumber.portNumber(34L));
        portOut.add(PortNumber.portNumber(35L));
        portOut.add(PortNumber.portNumber(36L));
        portInOut.put(portIn,portOut);

        deviceport.put(DeviceId.deviceId("of:012e8030e084c740"),portInOut);
        portIn.clear();
        portOut.clear();
        portInOut.clear();

    }

    void setFlow(){

        // Device of:01308030e084c740 N1
        deviceport.forEach((deviceId1, setSetHashMap) -> {

            if(deviceId1.equals(DeviceId.deviceId("of:01308030e084c740"))){

                setSetHashMap.forEach((portIN, portOUT) -> {

                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                        .meter(MeterId.meterId(51L));
                        //.setOutput();

                TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                        .matchInPort(PortNumber.portNumber("51"))
                        .matchVlanId(VlanId.vlanId((short)1000))
                        .matchEthType(Ethernet.TYPE_IPV4);

                ForwardingObjective.Builder flowObj = DefaultForwardingObjective.builder()
                        .makePermanent()
                        .withPriority(40000)
                        .withSelector(selector.build())
                        .withTreatment(treatment.build())
                        .withFlag(ForwardingObjective.Flag.SPECIFIC)
                        .fromApp(appId);

                installMeters(deviceId1,createBand(1000000),Long.valueOf(portIN.toString()));

                flowObjectiveService.apply(DeviceId.deviceId("of:01308030e084c740"),flowObj.add());

                });
            }

        });

        // Device of:012d8030e084c740 N2


        // Device of:012e8030e084c740 N3


        // Device of:012e8030e084c740 N4



    }


}


