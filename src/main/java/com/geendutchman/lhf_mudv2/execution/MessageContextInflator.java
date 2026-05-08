package com.geendutchman.lhf_mudv2.execution;

import java.util.Optional;
import java.util.function.Consumer;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.geendutchman.lhf_mudv2.entities.EntityResolver;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;
import com.geendutchman.lhf_mudv2.entities.entity.IEntityID;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.item.Item.ItemID;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.entities.room.Room.RoomID;
import com.geendutchman.lhf_mudv2.execution.MessageContext.EntityStack.EntityStackBuilder;
import com.geendutchman.lhf_mudv2.execution.MessageContext.MessageContextBuilder;

public interface MessageContextInflator {
    public Optional<Item> byItemID(ItemID id);

    public Optional<Creature> byCreatureID(CreatureID id);

    public Optional<Room> byRoomID(RoomID id);

    public default MessageContext inflate(final MessageContext context) {
        if (context == null) {
            throw new NullPointerException("cannot inflate null context");
        }

        Consumer<EntityStackBuilder> inflateStack = (stack) -> {
            IEntityID sender = stack.baseId();
            while (sender != null) {
                if (sender instanceof ItemID iid && stack.item().isEmpty()) {
                    Optional<Item> item = stack.item().or(() -> this.byItemID(iid));
                    if (item.isPresent()) {
                        sender = item.get().locale().orElse(null);
                        stack.item(item.get());
                    } else {
                        sender = null;
                    }
                } else if (sender instanceof CreatureID cid && stack.creature().isEmpty()) {
                    Optional<Creature> creature = stack.creature().or(() -> this.byCreatureID(cid));
                    if (creature.isPresent()) {
                        sender = creature.get().locale().orElse(null);
                        stack.creature(creature.get());
                    } else {
                        sender = null;
                    }
                } else if (sender instanceof RoomID rid && stack.room().isEmpty()) {
                    Optional<Room> room = stack.room().or(() -> this.byRoomID(rid));
                    if (room.isPresent()) {
                        sender = room.get().locale().orElse(null);
                        stack.room(room.get());
                    } else {
                        sender = null;
                    }
                } else {
                    if (sender != null) {
                        LoggerFactory.getLogger(this.getClass()).atDebug().addKeyValue("sender", sender)
                                .log("unknown sender");
                        sender = null;
                    }
                }

            }
        };

        MessageContextBuilder contextBuilder = context.toBuilder(true);
        EntityStackBuilder senderBuilder = contextBuilder.senderBuilder();
        inflateStack.accept(senderBuilder);
        EntityStackBuilder destinationBuilder = contextBuilder.destinationBuilder();
        inflateStack.accept(destinationBuilder);

        return contextBuilder.build();
    }

    @Service
    public static final class ContextInflator implements MessageContextInflator {
        @Autowired
        private final EntityResolver resolver;

        protected ContextInflator(@Autowired EntityResolver resolver) {
            this.resolver = resolver;
        }

        @Override
        public Optional<Creature> byCreatureID(CreatureID id) {
            if (this.resolver != null) {
                return this.resolver.byCreatureID(id);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Item> byItemID(ItemID id) {
            if (this.resolver != null) {
                return this.resolver.byItemID(id);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Room> byRoomID(RoomID id) {
            if (this.resolver != null) {
                return this.resolver.byRoomID(id);
            }
            return Optional.empty();
        }

    }
}
