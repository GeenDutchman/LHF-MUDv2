package com.geendutchman.lhf_mudv2.execution;

import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;

public interface EventProcessor {
    public abstract MessageProcessingResult process(MessageContext context, Event event);

    public interface Itemized extends EventProcessor {

        public static void distribute(Itemized itemized, MessageContext context, Event event) {
            if (itemized == null || context == null || event == null) {
                return;
            }

            switch (event) {
            case Event.PlainEvent plainEvent -> itemized.onPlainEvent(context, plainEvent);
            case Event.ItemChangedEvent itemChanged -> itemized.onItemChangedEvent(context, itemChanged);
            case Event.CreatureChangedEvent creatureChanged -> itemized.onCreatureChangedEvent(context,
                    creatureChanged);
            case Event.InventoryEvent inventory -> itemized.onInventoryEvent(context, inventory);
            case Event.RoomChangedEvent roomChanged -> itemized.onRoomChangedEvent(context, roomChanged);
            case Event.RoomSeenEvent roomSeen -> itemized.onRoomSeenEvent(context, roomSeen);
            case Event.CreatureSeenEvent creatureSeen -> itemized.onCreatureSeenEvent(context, creatureSeen);
            case Event.ItemSeenEvent itemSeen -> itemized.onItemSeenEvent(context, itemSeen);
            case Event.SpokenEvent speaking -> itemized.onSpokenEvent(context, speaking);
            }
        }

        @Override
        default MessageProcessingResult process(MessageContext context, Event event) {
            Itemized.distribute(this, context, event);
            return MessageProcessingResult.HANDLED;
        }

        void onPlainEvent(MessageContext context, Event.PlainEvent event);

        void onItemChangedEvent(MessageContext context, Event.ItemChangedEvent event);

        void onCreatureChangedEvent(MessageContext context, Event.CreatureChangedEvent event);

        void onInventoryEvent(MessageContext context, Event.InventoryEvent event);

        void onRoomChangedEvent(MessageContext context, Event.RoomChangedEvent event);

        void onRoomSeenEvent(MessageContext context, Event.RoomSeenEvent event);

        void onCreatureSeenEvent(MessageContext context, Event.CreatureSeenEvent event);

        void onItemSeenEvent(MessageContext context, Event.ItemSeenEvent event);

        void onSpokenEvent(MessageContext context, Event.SpokenEvent event);

    }
}
