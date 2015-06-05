/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onos.byon;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.HostId;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Skeletal ONOS application component.
 */
@Service
@Component(immediate = true)
public class NetworkManager implements NetworkService {

    private static Logger log = LoggerFactory.getLogger(NetworkManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Activate
    protected void activate() {
        log.info("Started");
        appId = coreService.registerApplication("org.onos.byon");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void createNetwork(String network) {
        store.putNetwork(network);
    }

    @Override
    public void deleteNetwork(String network) {
        store.removeNetwork(network);
        store.removeIntents(network);
    }

    @Override
    public Set<String> getNetworks() {
        return store.getNetworks();
    }

    @Override
    public void addHost(String network, HostId hostId) {
        Set<HostId> hosts=store.addHost(network, hostId);//what if network doesn`t exist, should I fail silently
        Set<Intent> intents= addToMesh(hostId,hosts);
        store.addIntents(network,intents);
    }

    @Override
    public void removeHost(String network, HostId hostId) {
        Set<Intent> forRemoval=store.removeIntents(network,hostId);
        store.removeHost(network,hostId);
        removeFromMesh(forRemoval);
    }

    @Override
    public Set<HostId> getHosts(String network) {
        return store.getHosts(network);
    }

    private Set<Intent> addToMesh(HostId src, Set<HostId> existing) {
        if (existing.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Intent> submitted = new HashSet<>();
        existing.forEach(dst -> {
            if (!src.equals(dst)) {
                //Intent intent = new HostToHostIntent(appId, src, dst);
                Intent intent = HostToHostIntent.builder().appId(appId).one(src).two(dst).build();
                submitted.add(intent);
                intentService.submit(intent);
            }
        });
        return submitted;
    }

    private void removeFromMesh(Set<Intent> intents) {
        intents.forEach(i -> intentService.withdraw(i));
    }
}
