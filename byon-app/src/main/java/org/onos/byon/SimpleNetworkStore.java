package org.onos.byon;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.HostId;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component(immediate = true, enabled = true)
@Service
public class SimpleNetworkStore
        implements NetworkStore {

    private static Logger log = LoggerFactory.getLogger(SimpleNetworkStore.class);

    private final Map<String, Set<HostId>> networks = Maps.newHashMap();
    private final Map<String, Set<Intent>> intentsPerNet = Maps.newHashMap();

    @Activate
    protected void activate() {
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
        networks.putIfAbsent(network,new HashSet<>());
        intentsPerNet.putIfAbsent(network, new HashSet<>());
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
        Set<HostId> hosts = networks.get(network);
        if (hosts==null) return new HashSet<>();//returns empty set if network doesn`t exist
        if (hosts.add(hostId)){
            networks.put(network,hosts);
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
        Set<HostId> hosts = networks.get(network);
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
        Set<HostId> hosts=networks.get(network);
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
        Set<Intent> Intents =intentsPerNet.get(network);
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
        Set<Intent> Intents =intentsPerNet.get(network);
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
        Set<Intent> Intents =intentsPerNet.get(network);
        intentsPerNet.put(network,new HashSet<>());//is this necessary?
        return Intents;
    }
}