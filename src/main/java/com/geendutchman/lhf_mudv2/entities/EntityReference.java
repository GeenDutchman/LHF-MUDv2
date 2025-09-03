package com.geendutchman.lhf_mudv2.entities;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.google.common.base.Preconditions;

public abstract class EntityReference<E extends Entity> implements Entity {
    @NonNull
    protected transient Optional<E> entity;
    @NonNull
    protected final IEntityID refId;
    @Autowired
    @NonNull
    protected transient EntityRepository<E> repo;
    private final Lock lock = new ReentrantLock();

    protected EntityReference(IEntityID id, E entity, EntityRepository<E> repo) {
        Preconditions.checkNotNull(id, "reference ID should not be null");
        Preconditions.checkNotNull(repo, "repository must not be null");
        this.refId = id;
        this.entity = Optional.ofNullable(entity);
        this.repo = repo;
    }

    public final void deref() {
        if (this.repo != null && this.entity.isEmpty() && this.lock.tryLock()) {
            try {
                if (this.entity.isEmpty() && this.repo != null) {
                    this.entity = repo.byID(refId);
                }
            } finally {
                this.lock.unlock();
            }
        }
    }

    @Override
    public IEntityID identifier() {
        return this.refId;
    }

    @Override
    public Optional<RichOutput> description() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().description();
        }
        return Optional
                .of(RichOutput.builder().addString("Whatever this had been, it is now a statuette of an insect or bug.")
                        .addString("On the back you see the mysterious engraven phrase:")
                        .addString(this.refId.toString()).build());
    }

    @Override
    public String name() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().name();
        }
        return "BugRock";
    }

    @Override
    public String content() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().content();
        }
        return "BugRock";
    }

    @Override
    public String tag() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().tag();
        }
        return "BugRock";
    }

    @Override
    public Optional<URI> locale() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().locale();
        }
        return Optional.empty();
    }

    @Override
    public void processEvent(Event event, EventBus bus) {
        this.deref();
        if (this.entity.isPresent()) {
            this.entity.get().processEvent(event, bus);
            return;
        }
        Logger.getLogger("bugRock").log(Level.WARNING,
                String.format("Entity '%s' not found for event '%s'", this.refId, event));
    }

    @Override
    public URI processorURI() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().processorURI();
        }
        return this.refId.uri();
    }

    @Override
    public String toString() {
        if (this.entity.isPresent()) {
            return String.format("Reference to: %s", this.entity.get().toString());
        }
        return String.format("Unresolved reference to: %s", this.refId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(refId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof EntityReference))
            return false;
        EntityReference<?> other = (EntityReference<?>) obj;
        return Objects.equals(refId, other.refId);
    }

}
