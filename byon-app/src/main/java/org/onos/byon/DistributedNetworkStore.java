package org.onos.byon;

import com.google.common.collect.*;

import org.apache.felix.scr.annotations.*;
import org.onlab.packet.*;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.HostId;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.serializers.MacAddressSerializer;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;

@Component(immediate = true, enabled = true)
@Service
public class DistributedNetworkStore
        implements NetworkStore {

    private static Logger log = LoggerFactory.getLogger(DistributedNetworkStore.class);

    @Reference(cardinality = MANDATORY_UNARY)//Doubt I`m not sure this should be here
    protected StorageService storageService;

    private ConsistentMap<String, Set<HostId>> networks;
    private ConsistentMap<String, Set<Intent>> intentsPerNet;

    private static final Serializer SERIALIZER = Serializer.using(KryoNamespaces.API);

        /*private static final Serializer SERIALIZER = new Serializer() {

        KryoNamespace kryo = new KryoNamespace.Builder()

                .register(HashSet.class)
                .register(HostId.class)
                .register(MacAddress.class)
                .register(byte[].class)
                .register(VlanId.class)
                .register(HostToHostIntent.class)
                .register(DefaultApplicationId.class)
//                .register(com.google.common.collect.RegularImmutableList)
                .build();*

        @Override
        public <T> byte[] encode(T object) {
            return kryo.serialize(object);
        }

        @Override
        public <T> T decode(byte[] bytes) {
            return kryo.deserialize(bytes);
        }

    };*/
    //private String listenerId;
    @Activate
    protected void activate() {
        networks=storageService.<String, Set<HostId>>consistentMapBuilder()
                .withName("onos-byon-net")// Copied from onos appdb
                .withSerializer(SERIALIZER)// why is a serializer needed?
                .build();
        intentsPerNet=storageService.<String, Set<Intent>>consistentMapBuilder()
                .withName("onos-byon-intents")// Copied from onos appdb
                .withSerializer(SERIALIZER)// why is a serializer needed?
                .build();
        //EntryListener<String,Set<HostId>> listener = new RemoteListener();
        //listenerId = networks.
        log.info("Started");
    }


    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    /**
     * Create a named network.
     *
     * @param network network name
     */
    @Override
    public void putNetwork(String network) {
        networks.putIfAbsent(network,Sets.newHashSet());
        intentsPerNet.putIfAbsent(network, Sets.newHashSet());
    }

    /**
     * Removes a named network.
     *
     * @param network network name
     */
    @Override
    public void removeNetwork(String network) {
        networks.remove(network);
        intentsPerNet.remove(network);
    }

    /**
     * Returns a set of network names.
     *
     * @return a set of network names
     */
    @Override
    public Set<String> getNetworks() {
        return networks.keySet();
    }

    /**
     * Adds a host to the given network.
     *
     * @param network network name
     * @param hostId  host id
     * @return updated set of hosts in the network (or an empty set if the host
     * has already been added to the network)
     */
    @Override
    public Set<HostId> addHost(String network, HostId hostId) {
        Set<HostId> hosts = networks.get(network).value();
        if (hosts==null) return Sets.newHashSet();//returns empty set if network doesn`t exist
        if (hosts.add(hostId)){
            networks.put(network,hosts);
            return hosts;
        }else return Sets.newHashSet();
    }

    /**
     * Removes a host from the given network.
     *
     * @param network network name
     * @param hostId  host id
     */
    @Override
    public void removeHost(String network, HostId hostId) {
        Set<HostId> hosts = networks.get(network).value();
        if (hosts!=null) hosts.remove(hostId);
    }

    /**
     * Returns all the hosts in a network.
     *
     * @param network network name
     * @return set of host ids
     */
    @Override
    public Set<HostId> getHosts(String network) {
        Set<HostId> hosts= networks.get(network).value();
        return (hosts!=null) ? hosts: Sets.newHashSet();
    }

    /**
     * Adds a set of intents to a network
     *
     * @param network network name
     * @param intents set of intents
     */
    @Override
    public void addIntents(String network, Set<Intent> intents) {
        Set<Intent> Intents = intentsPerNet.get(network).value();
        if (Intents==null) {
            Intents = Sets.newHashSet();//is it going to break if null?
        }
        Intents.addAll(intents);
        intentsPerNet.put(network,Intents);//is this necessary
    }

    /**
     * Returns a set of intents given a network and a host.
     *
     * @param network network name
     * @param hostId  host id
     * @return set of intents
     */
    @Override
    public Set<Intent> removeIntents(String network, HostId hostId) {
        Set<Intent> Intents = intentsPerNet.get(network).value();
        if (Intents==null) {
            return Sets.newHashSet();
        }
        Set<Intent> forRemoval = Sets.newHashSet();
        for (Intent item: Intents){
            HostToHostIntent intent = (HostToHostIntent) item;
            if (hostId.equals(intent.one()) || hostId.equals(intent.two())){
                forRemoval.add(item);
            }
        }
        Intents.removeAll(forRemoval);
        intentsPerNet.put(network,Intents);//is this necessary?
        return forRemoval;
    }

    /**
     * Returns a set of intents given a network.
     *
     * @param network network name
     * @return set of intents
     */
    @Override
    public Set<Intent> removeIntents(String network) {
        Set<Intent> Intents = intentsPerNet.get(network).value();
        intentsPerNet.put(network,Sets.newHashSet());//is this necessary?
        return Intents;
    }
}