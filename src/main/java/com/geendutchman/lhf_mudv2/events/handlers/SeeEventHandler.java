package com.geendutchman.lhf_mudv2.events.handlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.geendutchman.lhf_mudv2.display.Examinable;
import com.geendutchman.lhf_mudv2.events.Event;
import com.geendutchman.lhf_mudv2.events.EventBus;
import com.geendutchman.lhf_mudv2.events.EventHandler;
import com.geendutchman.lhf_mudv2.events.EventHandlerRegistry;
import com.geendutchman.lhf_mudv2.events.EventProcessor;
import com.geendutchman.lhf_mudv2.events.Events;
import com.geendutchman.lhf_mudv2.events.Events.SeeEvent;

import jakarta.annotation.PostConstruct;

@Component
public class SeeEventHandler implements EventHandler<Events.SeeEvent> {

    @Override
    public Class<SeeEvent> handledType() {
        return Events.SeeEvent.class;
    }

    @Autowired
    private EventHandlerRegistry registry;

    @PostConstruct
    public void register() {
        this.registry.register(this);
    }

    @Override
    public EventProcessor.ProcessingResult handle(SeeEvent event, EventProcessor processor, EventBus bus) {
        if (processor == null) {
            return new EventProcessor.ProcessingResult.Unhandled();
        }
        if (processor instanceof Examinable examinable) {
            Event reply = Events.addressed().reply(event.routing()).viewedEvent().setObserved(examinable).build();
            bus.publish(reply);
            return new EventProcessor.ProcessingResult.Handled();
        }
        return new EventProcessor.ProcessingResult.Unhandled();
    }

}
