package org.onos.byon.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onos.byon.NetworkService;
import org.onosproject.cli.AbstractShellCommand;

/**
 * CLI to create network
 */
@Command(scope = "byon", name = "remove-network", description = "Remove a network")
public class RemoveNetworkCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "network", description = "Network name",
            required = true, multiValued = false)
    String network = null;

    @Override
    protected void execute() {
        NetworkService networkService = get(NetworkService.class);

        networkService.deleteNetwork(network);

        print("Removed network %s", network);

    }
}
