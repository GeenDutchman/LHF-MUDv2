package com.geendutchman.lhf_mudv2.execution;

import java.util.UUID;

public interface Message {
    public UUID uuid();

    public MessageRouting routing();
}
