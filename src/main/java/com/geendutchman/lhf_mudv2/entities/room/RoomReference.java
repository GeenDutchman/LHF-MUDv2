package com.geendutchman.lhf_mudv2.entities.room;

import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import com.geendutchman.lhf_mudv2.display.RichOutput;
import com.geendutchman.lhf_mudv2.entities.EntityReference;
import com.geendutchman.lhf_mudv2.entities.IEntityID;
import com.geendutchman.lhf_mudv2.entities.IEntityID.EntityID;
import com.geendutchman.lhf_mudv2.entities.item.ItemInventory;
import com.google.common.collect.ImmutableSortedMap;

public class RoomReference extends EntityReference<Room> implements Room {
    private RoomReference(RoomID id, ConcreteRoom reference) {
        super(id, reference, null);
    }

    public static RoomReference ofRoom(RoomReference ref) {
        return ref;
    }

    @Bean
    @Scope("prototype")
    public static RoomReference ofRoom(Room room) {
        return new RoomReference(room.roomID(), null);
    }

    @Bean
    @Scope("prototype")
    public static RoomReference ofRoom(ConcreteRoom room) {
        return new RoomReference(room.roomID(), room);
    }

    @Bean
    @Scope("prototype")
    public static RoomReference ofId(RoomID id) {
        return new RoomReference(id, null);
    }

    public boolean isDereferenced() {
        return this.entity.isPresent();
    }

    @Override
    public IEntityID identifier() {
        return super.identifier();
    }

    @Override
    public RoomID roomID() {
        if (this.identifier() instanceof RoomID roomID) {
            return roomID;
        }
        final IEntityID id = this.identifier();
        return new RoomID(new EntityID(id.entityClass(), id.uuid()));
    }

    @Override
    public ItemInventory inventory() {
        this.deref();
        if (this.entity.isPresent()) {
            return this.entity.get().inventory();
        }
        return ItemInventory.buildInventory("BugRoom Inventory", new TreeSet<>());
    }

    @Override
    public ImmutableSortedMap<String, String> attributes() {
        return super.attributes();
    }

    @Override
    public Optional<RichOutput> roomDescription() {
        return Optional.of(RichOutput.builder().addString("BugRock room").build());
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
        if (!(obj instanceof RoomReference))
            return false;
        RoomReference other = (RoomReference) obj;
        return Objects.equals(refId, other.refId);
    }
}
