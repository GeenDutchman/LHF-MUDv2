package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.entity.Entity;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.google.auto.value.AutoBuilder;
import com.google.common.base.Preconditions;

public record CommandContext(IEntityID sender, Optional<Item> item, Optional<Creature> creature, Optional<Room> room) {

    public CommandContext {
        Preconditions.checkNotNull(sender, "sender must not be null");
        Preconditions.checkNotNull(item, "item may be empty but must not be null");
        Preconditions.checkNotNull(creature, "creature may be empty but must not be null");
        Preconditions.checkNotNull(room, "room may be empty but must not be null");
    }

    @AutoBuilder
    public interface CommandContextBuilder {
        public abstract CommandContextBuilder setSender(IEntityID entity);

        public default CommandContextBuilder setSender(Entity entity) {
            return this.setSender(entity.identifier());
        }

        public abstract CommandContextBuilder setItem(Item item);

        public abstract CommandContextBuilder setCreature(Creature creature);

        public abstract CommandContextBuilder setRoom(Room room);

        public abstract CommandContext build();
    }

    public static CommandContextBuilder builder() {
        return new AutoBuilder_CommandContext_CommandContextBuilder();
    }

}
