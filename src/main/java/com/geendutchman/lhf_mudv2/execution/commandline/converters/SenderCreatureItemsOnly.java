package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableSet;

public class SenderCreatureItemsOnly extends EntityNameConverter<Item> {

    public SenderCreatureItemsOnly(MessageContext ctx) {
        super(ctx);
    }

    @Override
    public String entityTypeName() {
        return "Item";
    }

    @Override
    protected Item singleContextCheck(String trimmed) {
        return null;
    }

    @Override
    protected ImmutableSet<Item> manyContextCheck(String trimmed) {
        final MessageContext context = this.ctx();
        if (context == null || context.sender().creature().isEmpty()) {
            return ImmutableSet.of();
        }
        final ItemQuery q = ItemQuery.builder().adjustEntityQuery(e -> e.setNamePattern("^" + Pattern.quote(trimmed)))
                .build();
        final ItemContainer result = context.sender().creature().get().queryItems(q);
        return result != null ? result.items() : ImmutableSet.of();
    }

}
