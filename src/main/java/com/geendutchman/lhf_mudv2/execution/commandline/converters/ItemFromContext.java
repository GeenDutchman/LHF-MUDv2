package com.geendutchman.lhf_mudv2.execution.commandline.converters;

import java.util.regex.Pattern;

import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.ItemContainer;
import com.geendutchman.lhf_mudv2.entities.item.ItemQuery;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableSet;

public class ItemFromContext extends EntityNameConverter<Item> {
    public ItemFromContext(MessageContext ctx) {
        super(ctx);
    }

    @Override
    public String entityTypeName() {
        return "Item";
    }

    @Override
    protected Item singleContextCheck(String trimmed) {
        final MessageContext c = this.ctx();
        if (c == null) {
            return null;
        }
        return c.sender().item().orElse(null);
    }

    @Override
    protected ImmutableSet<Item> manyContextCheck(String trimmed) {
        final MessageContext c = this.ctx();
        if (c == null) {
            return ImmutableSet.of();
        }
        final ItemQuery q = ItemQuery.builder()
                .adjustEntityQuery(e -> e.setNamePattern(Pattern.compile("^" + Pattern.quote(trimmed)))).build();
        final ItemContainer result = c.queryItems(q);
        return result != null ? result.items() : ImmutableSet.of();
    }
}
