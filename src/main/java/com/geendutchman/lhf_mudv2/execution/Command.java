package com.geendutchman.lhf_mudv2.execution;

public sealed interface Command extends Message, Comparable<Command> permits UserCommand, LHFCommand {
    @Override
    public default int compareTo(Command o) {
        return this.tsid().compareTo(o.tsid());
    }
}
