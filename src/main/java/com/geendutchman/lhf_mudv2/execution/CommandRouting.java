package com.geendutchman.lhf_mudv2.execution;

import java.net.URI;

import com.google.common.base.Preconditions;

public record CommandRouting(URI sender, URI destination) implements MessageRouting {
    public CommandRouting {
        Preconditions.checkNotNull(sender, "send must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
        Preconditions.checkArgument(sender.getQuery() == null || sender.getQuery() == "",
                "sender must not have a query");
        Preconditions.checkArgument(destination.getQuery() == null || sender.getQuery() == "",
                "destination must not have a query");
    }
}