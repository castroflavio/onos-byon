package org.onos.byon;

import com.hazelcast.core.EntryAdapter;
import com.hazelcast.core.EntryEvent;
import org.apache.felix.scr.annotations.*;
import org.onosproject.net.HostId;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.store.AbstractStore;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;

@Component(immediate = false, enabled = false)
@Service
public class DistributedNetworkStore extends AbstractStore<NetworkEvent, NetworkStoreDelegate>
        implements NetworkStore {

    private static Logger log = LoggerFactory.getLogger(DistributedNetworkStore.class);

    @Reference(cardinality = MANDATORY_UNARY)//Doubt I`m not sure this should be here
    protected StorageService storageService;

    private ConsistentMap<String, Set<HostId>> networks;
    private ConsistentMap<String, Set<Intent>> intentsPerNet;

    //private String listenerId;
    @Activate
    protected void activate() {
        networks=storageService.<String, Set<HostId>>consistentMapBuilder()
                //.withName("onos-app-database-perf-test-map") Copied from onos appdb
                //.withSerializer(SERIALIZER) why is a serializer needed?
                .build();
        intentsPerNet=storageService.<String, Set<Intent>>consistentMapBuilder()
                .build();
        //EntryListener<String,Set<HostId>> listener = new RemoteListener();
        //listenerId = networks.
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        //networks.removeEntryListener(listenerId);
        log.info("Stopped");
    }
    /**
     * Create a named network.
     *
     * @param network network name
     */
    @Override
    public void putNetwork(String network) {
        intentsPerNet.putIfAbsent(network, new HashSet<>());
        if (networks.putIfAbsent(network,new HashSet<>())==null)
            notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_ADDED, network));
    }
    /**
     * Removes a named network.
     *
     * @param network network name
     */
    @Override
    public void removeNetwork(String network) {

        intentsPerNet.remove(network);
        if ( networks.remove(network) != null)
            notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_REMOVED, network));
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
        Set<HostId> hosts = (Set<HostId>) networks.get(network);
        if (hosts==null) return new HashSet<>();//returns empty set if network doesn`t exist
        if (hosts.add(hostId)){
            if (networks.put(network,hosts)!= null)
                notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_UPDATED,network));
            return hosts;
        }else return new HashSet<>();
    }

    /**
     * Removes a host from the given network.
     *
     * @param network network name
     * @param hostId  host id
     */
    @Override
    public void removeHost(String network, HostId hostId) {
        Set<HostId> hosts = (Set<HostId>) networks.get(network);
        if (hosts!=null)
            if (hosts.remove(hostId))
                notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_UPDATED,network));
    }

    /**
     * Returns all the hosts in a network.
     *
     * @param network network name
     * @return set of host ids
     */
    @Override
    public Set<HostId> getHosts(String network) {
        Set<HostId> hosts= (Set<HostId>) networks.get(network);
        return (hosts!=null) ? hosts: new HashSet<>();
    }

    /**
     * Adds a set of intents to a network
     *
     * @param network network name
     * @param intents set of intents
     */
    @Override
    public void addIntents(String network, Set<Intent> intents) {
        Set<Intent> Intents = (Set<Intent>) intentsPerNet.get(network);
        if (Intents==null) {
            Intents = new HashSet<>();//is it going to break if null?
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
        Set<Intent> Intents = (Set<Intent>) intentsPerNet.get(network);
        if (Intents==null) {
            return new HashSet<>();
        }
        Set<Intent> forRemoval = new HashSet<>();
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
        Set<Intent> Intents = (Set<Intent>) intentsPerNet.get(network);
        intentsPerNet.put(network,new HashSet<>());//is this necessary?
        return Intents;
    }

    private class RemoteListener extends EntryAdapter<String, Set<HostId>> {
        @Override
        public void entryAdded(EntryEvent<String, Set<HostId>> event) {
            notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_ADDED, event.getKey()));
        }

        @Override
        public void entryUpdated(EntryEvent<String, Set<HostId>> event) {
            notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_UPDATED, event.getKey()));
        }

        @Override
        public void entryRemoved(EntryEvent<String, Set<HostId>> event) {
            notifyDelegate(new NetworkEvent(NetworkEvent.Type.NETWORK_REMOVED, event.getKey()));
        }
    }

}