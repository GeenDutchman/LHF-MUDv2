package com.geendutchman.lhf_mudv2.execution.commandline;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageProcessor.MessageProcessingResult;
import com.geendutchman.lhf_mudv2.execution.MessageBus;

abstract class SwitchedHandler extends UserCommandHandler {

    protected SwitchedHandler(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Override
    public MessageProcessingResult call() {
        return contextSplit();
    }

    protected final MessageProcessingResult contextSplit() {
        if (context.sender().creature().map(c -> c.identifier()).filter(i -> i.equals(context.sender().baseId()))
                .isPresent()) {
            return this.onCreature(context.sender().creature().get());
        } else if (context.sender().item().map(item -> item.identifier())
                .filter(id -> id.equals(context.sender().baseId())).isPresent()) {
            return this.onItem(context.sender().item().get());
        } else if (context.sender().room().map(room -> room.identifier())
                .filter(id -> id.equals(context.sender().baseId())).isPresent()) {
            return this.onRoom(context.sender().room().get());
        }
        return this.unrecognized();
    }

    protected abstract MessageProcessingResult onItem(final Item item);

    protected abstract MessageProcessingResult onCreature(final Creature creature);

    protected abstract MessageProcessingResult onRoom(final Room room);

    protected abstract MessageProcessingResult unrecognized();

}