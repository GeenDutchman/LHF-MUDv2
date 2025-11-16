package com.geendutchman.lhf_mudv2.execution;

import java.io.Serializable;

public sealed interface Command extends Serializable permits UserCommand, LHFCommand {

}
