package com.geendutchman.lhf_mudv2.execution;

import java.net.URI;

public interface MessageRouting {
    public URI sender();

    public URI destination();
}
