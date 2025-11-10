package com.geendutchman.lhf_mudv2.execution;

import java.util.UUID;

import com.google.common.base.Preconditions;

public sealed interface Command extends Message permits UserCommand, LHFCommand {

    @Override
    public UUID uuid();

    @Override
    public CommandRouting routing();

    static void commandPreconditions(CommandRouting routing, UUID uuid) {
        Preconditions.checkArgument(routing != null, "routing must be set");
        Preconditions.checkArgument(uuid != null, "uuid must not be null");
    }

}
