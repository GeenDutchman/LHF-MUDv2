package com.geendutchman.lhf_mudv2.execution.commandline;

import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageBus;

public abstract class UserCommandHandler extends CommandHandler {
    protected UserCommandHandler(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

}
