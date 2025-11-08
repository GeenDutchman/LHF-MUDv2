package com.geendutchman.lhf_mudv2.execution;

import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.google.common.base.Preconditions;

public record CommandRouting(IEntityID sender, IEntityID destination) implements MessageRouting {
    public CommandRouting {
        Preconditions.checkNotNull(sender, "send must not be null");
        Preconditions.checkNotNull(destination, "destination must not be null");
    }
}