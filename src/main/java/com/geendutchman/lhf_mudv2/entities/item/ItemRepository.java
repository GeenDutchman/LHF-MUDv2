package com.geendutchman.lhf_mudv2.entities.item;

import java.util.concurrent.ConcurrentSkipListSet;

import org.springframework.stereotype.Repository;

import com.geendutchman.lhf_mudv2.entities.EntityRepository;
import com.google.common.collect.ImmutableSortedMap;

@Repository
public final class ItemRepository
        implements ItemContainer.MutableItemContainer<ConcreteItem>, EntityRepository<ConcreteItem> {

    private final ConcurrentSkipListSet<ConcreteItem> cargo = new ConcurrentSkipListSet<>(Item.getItemComparator());

    @Override
    public String name() {
        return "ItemRepository";
    }

    @Override
    public ConcurrentSkipListSet<ConcreteItem> cargo() {
        return this.cargo;
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return ImmutableSortedMap.of();
    }

}
