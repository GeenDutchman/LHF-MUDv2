package com.geendutchman.lhf_mudv2.item;

import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.google.common.base.Preconditions;

public final class ItemReference implements Item {
    @NonNull
    private final ItemID refID;
    @Nullable
    private transient ConcreteItem ref;

    private ItemReference(ItemID id, ConcreteItem reference) {
        Preconditions.checkNotNull(id, "reference ID should not be null");
        this.refID = id;
        this.ref = reference;
    }

    public static ItemReference ofItem(Item item) {
        return new ItemReference(item.itemID(), null);
    }

    public static ItemReference ofItem(ConcreteItem item) {
        return new ItemReference(item.itemID(), item);
    }

    public static ItemReference ofId(ItemID id) {
        return new ItemReference(id, null);
    }

    public synchronized void deref() {
        if (this.ref == null) {
            this.ref = Item.itemRepo.getOrDefault(this.refID, null);
        }
    }

    public boolean isDereferenced() {
        return this.ref != null;
    }

    @Override
    public synchronized String name() {
        this.deref();
        if (this.ref != null) {
            return this.ref.name();
        }
        return "BugRock";
    }

    @Override
    public synchronized Optional<RichOutput> description() {
        this.deref();
        if (this.ref != null) {
            return this.ref.description();
        }
        return Optional.of(RichOutput.builder().addString("Whatever this had been, it is now a statuette of an insect.")
                .addString("On the back you see the mysterious engraven phrase:")
                .addString(this.refID.uuid().toString()).build());
    }

    @Override
    public ItemID itemID() {
        return this.refID;
    }

    @Override
    public synchronized Optional<String> nickname() {
        this.deref();
        if (this.ref != null) {
            return this.ref.nickname();
        }
        return Optional.empty();
    }

    @Override
    public synchronized boolean isVisible() {
        this.deref();
        if (this.ref != null) {
            return this.ref.isVisible();
        }
        return true;
    }

    @Override
    public synchronized ItemTag itemTag() {
        this.deref();
        if (this.ref != null) {
            return this.ref.itemTag();
        }
        return ItemTag.ITEM;
    }

    @Override
    public synchronized void applyDelta(Delta delta) {
        this.deref();
        if (this.ref != null) {
            this.ref.applyDelta(delta);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(refID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof ItemReference))
            return false;
        ItemReference other = (ItemReference) obj;
        return Objects.equals(refID, other.refID);
    }

}