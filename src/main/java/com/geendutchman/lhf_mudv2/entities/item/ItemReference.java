package com.geendutchman.lhf_mudv2.entities.item;

import java.util.Objects;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import com.geendutchman.lhf_mudv2.dice.Difficulty;
import com.geendutchman.lhf_mudv2.dice.Plain;
import com.geendutchman.lhf_mudv2.entities.EntityReference;
import com.geendutchman.lhf_mudv2.entities.EntityRepository;
import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.entities.IEntityID.EntityID;

public final class ItemReference extends EntityReference<ConcreteItem> implements Item {

    private ItemReference(ItemID id, ConcreteItem reference, EntityRepository<ConcreteItem> repo) {
        super(id, reference, repo);
    }

    @Bean
    @Scope("prototype")
    public static ItemReference ofItem(ConcreteItem item, EntityRepository<ConcreteItem> repo) {
        return new ItemReference(item.itemID(), item, repo);
    }

    @Bean
    @Scope("prototype")
    public static ItemReference ofId(ItemID id, EntityRepository<ConcreteItem> repo) {
        return new ItemReference(id, null, repo);
    }

    public boolean isDereferenced() {
        return this.entity.isPresent();
    }

    @Override
    public IEntityID identifier() {
        return super.identifier();
    }

    @Override
    public ItemID itemID() {
        if (this.identifier() instanceof ItemID itemID) {
            return itemID;
        }
        final IEntityID id = this.identifier();
        return new ItemID(new EntityID(id.entityClass(), id.uuid()));
    }

    @Override
    public Optional<String> nickname() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().nickname();
        }
        return Optional.empty();
    }

    @Override
    public Difficulty<Plain> visibility() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().visibility();
        }
        return Plain.noDifficulty();
    }

    @Override
    public ItemTag itemTag() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().itemTag();
        }
        return ItemTag.ITEM;
    }

    @Override
    public void applyDelta(Delta delta) {
        this.deref();
        if (this.entity.isPresent()) {
            this.entity.get().applyDelta(delta);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ItemReference))
            return false;
        ItemReference other = (ItemReference) obj;
        return Objects.equals(refId, other.refId);
    }

}