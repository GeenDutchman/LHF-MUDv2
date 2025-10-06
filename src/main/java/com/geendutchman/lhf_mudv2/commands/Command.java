package com.geendutchman.lhf_mudv2.commands;

import java.net.URI;
import java.util.UUID;

import com.google.common.base.Preconditions;

public sealed interface Command permits UserCommand, LHFCommand {

    public UUID uuid();

    public CommandRouting routing();

    static void commandPreconditions(CommandRouting routing, UUID uuid) {
        Preconditions.checkArgument(routing != null, "routing must be set");
        Preconditions.checkArgument(routing.destination().getQuery() == null,
                "commands are direct, should not have a query");
        Preconditions.checkArgument(uuid != null, "uuid must not be null");
    }

    public static record CommandRouting(URI sender, URI destination) {
        public CommandRouting {
            Preconditions.checkNotNull(sender, "send must not be null");
            Preconditions.checkNotNull(destination, "destination must not be null");
            Preconditions.checkArgument(sender.getQuery() == null || sender.getQuery() == "",
                    "sender must not have a query");
            Preconditions.checkArgument(destination.getQuery() == null || sender.getQuery() == "",
                    "destination must not have a query");
        }
    }

}
