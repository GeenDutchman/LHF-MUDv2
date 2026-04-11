package com.geendutchman.lhf_mudv2.execution.commandline;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.item.Item;
import com.geendutchman.lhf_mudv2.entities.room.Room;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.geendutchman.lhf_mudv2.execution.MessageBus;

abstract class SwitchedHandler extends UserCommandHandler {

    protected SwitchedHandler(MessageBus messbus, MessageContext context) {
        super(messbus, context);
    }

    @Override
    public void run() {
        contextSplit();
    }

    protected final void contextSplit() {
        if (context.creature().map(c -> c.identifier()).filter(i -> i.equals(context.sender())).isPresent()) {
            this.onCreature(context.creature().get());
        } else if (context.item().map(item -> item.identifier()).filter(id -> id.equals(context.sender()))
                .isPresent()) {
            this.onItem(context.item().get());
        } else if (context.room().map(room -> room.identifier()).filter(id -> id.equals(context.sender()))
                .isPresent()) {
            this.onRoom(context.room().get());
        }
    }

    protected abstract void onItem(final Item item);

    protected abstract void onCreature(final Creature creature);

    protected abstract void onRoom(final Room room);

    protected abstract void unrecognized();

}