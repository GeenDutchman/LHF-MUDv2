package com.geendutchman.lhf_mudv2.commands;

import java.net.URI;

import com.geendutchman.lhf_mudv2.commands.CommandProcessor.CommandResult;
import com.google.common.collect.ImmutableSet;

public interface CommandBus {
    public abstract void registerCommandProcessor(final CommandProcessor processor);

    public abstract void deregisterCommandProcessor(final CommandProcessor processor);

    public abstract CommandResult send(final Command command);

    public abstract ImmutableSet<URI> listCommandProcessors();
}
