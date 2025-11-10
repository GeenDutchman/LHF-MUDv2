package com.geendutchman.lhf_mudv2.execution;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;

public interface MessageRouting {
    public IEntityID sender();

    public IEntityID destination();
}
