package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureContainer;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureQuery;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableSet;

public class SenderRoomCreaturesOnly extends EntityNameConverter<Creature> {
    public SenderRoomCreaturesOnly(MessageContext ctx) {
        super(ctx);
    }

    @Override
    public String entityTypeName() {
        return "Creature";
    }

    @Override
    protected Creature singleContextCheck(String trimmed) {
        final MessageContext c = this.ctx();
        if (c == null) {
            return null;
        }
        return c.sender().creature().orElse(null);
    }

    @Override
    protected ImmutableSet<Creature> manyContextCheck(String trimmed) {
        final MessageContext context = this.ctx();
        if (context == null || context.sender().room().isEmpty()) {
            return ImmutableSet.of();
        }
        final CreatureQuery q = CreatureQuery.builder()
                .adjustEntityQuery(e -> e.setNamePattern("^" + Pattern.quote(trimmed))).build();
        final CreatureContainer result = context.sender().room().get().queryCreatures(q);
        return result != null ? result.creatures() : ImmutableSet.of();
    }
}
