package com.geendutchman.lhf_mudv2.execution.controllers;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature.CreatureID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import com.geendutchman.lhf_mudv2.entities.creatures.Creature;
import com.geendutchman.lhf_mudv2.entities.creatures.CreatureRepository;
import com.geendutchman.lhf_mudv2.execution.Event;
import com.geendutchman.lhf_mudv2.execution.MessageBus;
import com.geendutchman.lhf_mudv2.execution.MessageContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;

@TestComponent
public class TestCreatureController extends CreatureController {
    protected ListMultimap<CreatureID, Event> events = Multimaps
            .synchronizedListMultimap(MultimapBuilder.linkedHashKeys().arrayListValues().build());

    public TestCreatureController(MessageBus bus, CreatureRepository repo) {
        super(bus, repo);
    }

    @Autowired
    @Override
    protected void processEvent(MessageContext context, Event event, Creature creature) {
        if (context == null || event == null || creature == null) {
            return;
        }

        super.processEvent(context, event, creature);

        this.events.put(creature.creatureID(), event);
    }

    /**
     * Retrieves a snapshot of events for a creature
     * 
     * @param id
     * @return
     */
    public ImmutableList<Event> eventsFor(CreatureID id) {
        if (id == null) {
            return ImmutableList.of();
        }
        ImmutableList.Builder<Event> compiled = ImmutableList.builder();
        synchronized (this.events) {
            compiled.addAll(this.events.get(id));
        }
        return compiled.build();
    }

}