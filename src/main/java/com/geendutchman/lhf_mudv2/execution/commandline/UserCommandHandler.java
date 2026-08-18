package com.geendutchman.lhf_mudv2.execution.commandline;

import com.geendutchman.lhf_mudv2.execution.MessageContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.geendutchman.lhf_mudv2.execution.MessageBus;

public abstract class UserCommandHandler extends CommandHandler {
    protected UserCommandHandler(@Autowired MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

}
