package com.geendutchman.lhf_mudv2.execution;

public sealed interface Command extends Message permits UserCommand, LHFCommand {

}
