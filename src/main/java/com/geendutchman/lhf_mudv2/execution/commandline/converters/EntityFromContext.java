package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityQuery.EntityQuery;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;

public class EntityFromContext extends EntityNameConverter<Entity> {
    public EntityFromContext(MessageContext ctx) {
        super(ctx);
    }

    @Override
    public String entityTypeName() {
        return "Entity";
    }

    @Override
    protected Entity singleContextCheck(String trimmed) {
        return null;
    }

    @Override
    protected ImmutableSet<Entity> manyContextCheck(String trimmed) {
        final MessageContext c = this.ctx();
        if (c == null) {
            return ImmutableSet.of();
        }
        final EntityQuery q = EntityQuery.builder().setNamePattern(Pattern.compile("^" + Pattern.quote(trimmed)))
                .build();
        final ImmutableSortedMap<IEntityID, Entity> result = c.queryEntities(q);
        return result != null ? result.values().stream().collect(ImmutableSet.toImmutableSet()) : ImmutableSet.of();
    }
}
