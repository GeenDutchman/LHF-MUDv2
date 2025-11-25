package com.geendutchman.lhf_mudv2.execution;

import java.io.Serializable;

import com.github.f4b6a3.tsid.Tsid;

public interface Message extends Serializable {
    public abstract Tsid tsid();
}
