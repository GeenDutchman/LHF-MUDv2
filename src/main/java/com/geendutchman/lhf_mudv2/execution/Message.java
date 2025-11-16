package com.geendutchman.lhf_mudv2.execution;

import java.io.Serializable;
import java.util.UUID;

public interface Message extends Serializable {
    public abstract UUID uuid();
}
